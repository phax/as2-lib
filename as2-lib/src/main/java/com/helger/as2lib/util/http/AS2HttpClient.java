/*
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2018 Philip Helger philip[at]helger[dot]com
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE FREEBSD PROJECT ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE FREEBSD PROJECT OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the FreeBSD Project.
 */
package com.helger.as2lib.util.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.processor.sender.AbstractHttpSenderModule;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.http.EHttpMethod;
import com.helger.commons.http.HttpHeaderMap;

/**
 * Http connection, Implemented as HttpClient.
 *
 * @author Ziv Harpaz
 */
public class AS2HttpClient implements IAS2HttpConnection
{
  private RequestBuilder m_aRequestBuilder;
  private PipedInputStream m_aPipedInputStream;
  private PipedOutputStream m_aPipedOutputStream;
  private CloseableHttpClient m_aCloseableHttpClient;
  private CloseableHttpResponse m_aCloseableHttpResponse;
  private static final Logger LOGGER = LoggerFactory.getLogger (AS2HttpClient.class);

  public AS2HttpClient (@Nonnull @Nonempty final String sUrl,
                        final int nConnectTimeout,
                        final int nReadTimeout,
                        @Nonnull final EHttpMethod eRequestMethod,
                        @Nullable final Proxy aProxy) throws OpenAS2Exception
  {
    try
    {
      // set configuration
      final RequestConfig.Builder aConfBuilder = RequestConfig.custom ()
                                                              .setConnectionRequestTimeout (nConnectTimeout)
                                                              .setConnectTimeout (nConnectTimeout)
                                                              .setSocketTimeout (nReadTimeout);
      // add proxy if exists
      _setProxyToRequestConfig (aConfBuilder, aProxy);
      final RequestConfig aConf = aConfBuilder.build ();
      final URI aUri = new URI (sUrl);
      final HttpClientBuilder aClientBuilder = HttpClientBuilder.create ();
      if (aUri.getScheme ().toLowerCase (Locale.ROOT).equals ("https"))
      {
        // Create SSL context
        final SSLContext aSSLCtx = AbstractHttpSenderModule.createSSLContext ();
        if (aSSLCtx != null)
          aClientBuilder.setSSLContext (aSSLCtx);

        // Get hostname verifier
        final HostnameVerifier aHV = AbstractHttpSenderModule.createHostnameVerifier ();
        if (aHV != null)
          aClientBuilder.setSSLHostnameVerifier (aHV);
      }
      m_aCloseableHttpClient = aClientBuilder.build ();
      m_aRequestBuilder = RequestBuilder.create (eRequestMethod.getName ()).setUri (aUri).setConfig (aConf);
    }
    catch (final URISyntaxException | GeneralSecurityException e)
    {
      LOGGER.error ("Exception in AS2HttpClient constructor", e);
      throw new OpenAS2Exception (e.getMessage ());
    }
  }

  /**
   * Set an HTTP header (replacing existing value
   *
   * @param sName
   *        Header name
   * @param sValue
   *        Header value
   */
  public void setHttpHeader (@Nonnull final String sName, @Nonnull final String sValue)
  {
    m_aRequestBuilder.setHeader (sName, sValue);
  }

  /**
   * Get URL
   */
  public URL getURL () throws OpenAS2Exception
  {
    URI uri = null;
    try
    {
      uri = m_aRequestBuilder.getUri ();
      return uri.toURL ();
    }
    catch (final Exception e)
    {
      if (LOGGER.isErrorEnabled ())
        LOGGER.error ("Failed to get URL from connection, URI: " + (uri == null ? "null" : uri.toASCIIString ()));
      throw new OpenAS2Exception (e.getCause ());
    }
  }

  /**
   * Provides an output stream so user can write message content to it. Starts
   * the sending to caller can start write to the received stream
   *
   * @return OutputStream to write message body to
   */
  public OutputStream getOutputStream () throws IOException
  {
    if (m_aPipedOutputStream == null)
    {
      m_aPipedInputStream = new PipedInputStream ();
      m_aPipedOutputStream = new PipedOutputStream (m_aPipedInputStream);
    }
    m_aRequestBuilder.setEntity (new InputStreamEntity (m_aPipedInputStream));
    _sendInBackground ();
    return m_aPipedOutputStream;
  }

  /**
   * send the request in background, allowing the foreground to write to the
   * OutputStream
   */
  private void _sendInBackground ()
  {
    final Thread aSenderThread = new Thread ( () -> {
      try
      {
        final HttpUriRequest aHttpUriRequest = m_aRequestBuilder.build ();
        System.out.println ("Runnable: calling execute");
        m_aCloseableHttpResponse = m_aCloseableHttpClient.execute (aHttpUriRequest);
      }
      catch (final Exception e)
      {
        e.printStackTrace ();
      }
      System.out.println ("Runnable: Thread exiting");
    });
    aSenderThread.start ();
  }

  public void send (final InputStream toSend) throws IOException
  {
    m_aRequestBuilder.setEntity (new InputStreamEntity (toSend));
    final HttpUriRequest aHttpUriRequest = m_aRequestBuilder.build ();
    m_aCloseableHttpResponse = m_aCloseableHttpClient.execute (aHttpUriRequest);

  }

  /**
   * Get InputStream
   *
   * @return InputStream to read response body from
   */
  public InputStream getInputStream () throws OpenAS2Exception, IOException
  {
    // message was not sent yet, not response
    if (m_aCloseableHttpResponse == null)
    {
      throw new OpenAS2Exception ("No response as message was not yet sent");
    }
    return m_aCloseableHttpResponse.getEntity ().getContent ();
  }

  /**
   * Get response HTTP Status as integer
   */
  public int getResponseCode () throws OpenAS2Exception
  {
    // message was not sent yet, not response
    if (m_aCloseableHttpResponse == null)
    {
      throw new OpenAS2Exception ("No response as message was not yet sent");
    }
    try
    {
      final StatusLine aStatusLine = m_aCloseableHttpResponse.getStatusLine ();
      return aStatusLine.getStatusCode ();
    }
    catch (final Exception e)
    {
      throw new OpenAS2Exception (e.getCause ());
    }
  }

  /**
   * Get the response message
   */
  public String getResponseMessage () throws OpenAS2Exception
  {
    // message was not sent yet, not response
    if (m_aCloseableHttpResponse == null)
    {
      throw new OpenAS2Exception ("No response as message was not yet sent");
    }
    final StatusLine aStatusLine = m_aCloseableHttpResponse.getStatusLine ();
    return aStatusLine.getReasonPhrase ();
  }

  /**
   * @return the headers of the request
   */
  @Nonnull
  @ReturnsMutableCopy
  public HttpHeaderMap getHeaderFields () throws OpenAS2Exception
  {
    // message was not sent yet, not response
    if (m_aCloseableHttpResponse == null)
    {
      throw new OpenAS2Exception ("No response as message was not yet sent");
    }

    final Header [] aHeaders = m_aCloseableHttpResponse.getAllHeaders ();
    final HttpHeaderMap ret = new HttpHeaderMap ();
    for (final Header aHeader : aHeaders)
      ret.addHeader (aHeader.getName (), aHeader.getValue ());
    return ret;
  }

  /**
   * Close the connection
   */
  public void disconnect ()
  {
    try
    {
      if (m_aCloseableHttpResponse != null)
        m_aCloseableHttpResponse.close ();
      if (m_aCloseableHttpClient != null)
        m_aCloseableHttpClient.close ();
    }
    catch (final Exception ex)
    {
      if (LOGGER.isErrorEnabled ())
        LOGGER.error ("Exception while closing HttpClient connection: " + this.toString (), ex);
    }
  }

  /**
   * Set {@link Proxy} into {@link RequestConfig.Builder}
   *
   * @param aConfBuilder
   *        {@link RequestConfig.Builder} to set
   * @param aProxy
   *        My by null, in such case nothing is done.
   */
  private static void _setProxyToRequestConfig (@Nonnull final RequestConfig.Builder aConfBuilder,
                                                @Nullable final Proxy aProxy)
  {
    try
    {
      if (null != aProxy)
      {
        final SocketAddress aSocketAddress = aProxy.address ();
        if (null != aSocketAddress && aSocketAddress instanceof InetSocketAddress)
        {
          final InetSocketAddress aISocketAdress = (InetSocketAddress) aSocketAddress;
          final InetAddress aInetAddr = aISocketAdress.getAddress ();
          if (null != aInetAddr)
          {
            final HttpHost aHost = new HttpHost (aInetAddr);
            aConfBuilder.setProxy (aHost);
          }
          else
          {
            if (LOGGER.isDebugEnabled ())
              LOGGER.debug ("No address in proxy:" +
                            aProxy.address () +
                            "-" +
                            (null != aProxy.type () ? aProxy.type ().name () : "null"));
          }
        }
      }
    }
    catch (final Exception ex)
    {
      if (LOGGER.isErrorEnabled ())
        LOGGER.error ("Exception while setting proxy. Continue without proxy. aProxy:" +
                      aProxy.address () +
                      "-" +
                      (null != aProxy.type () ? aProxy.type ().name () : "null"),
                      ex);
    }
  }
}
