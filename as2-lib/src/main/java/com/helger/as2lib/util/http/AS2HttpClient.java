/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2019 Philip Helger philip[at]helger[dot]com
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
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;
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
import com.helger.as2lib.util.dump.IHTTPOutgoingDumper;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.http.EHttpMethod;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.stream.CountingInputStream;
import com.helger.mail.cte.EContentTransferEncoding;

/**
 * Http connection, Implemented as HttpClient.
 *
 * @author Ziv Harpaz
 */
public class AS2HttpClient
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS2HttpClient.class);

  private final RequestBuilder m_aRequestBuilder;
  private PipedInputStream m_aPipedInputStream;
  private PipedOutputStream m_aPipedOutputStream;
  private final CloseableHttpClient m_aCloseableHttpClient;
  private CloseableHttpResponse m_aCloseableHttpResponse;

  public AS2HttpClient (@Nonnull @Nonempty final String sUrl,
                        final int nConnectTimeout,
                        final int nReadTimeout,
                        @Nonnull final EHttpMethod eRequestMethod,
                        @Nullable final Proxy aProxy,
                        @Nullable final SSLContext aSSLContext,
                        @Nullable final HostnameVerifier aHV)
  {
    // set configuration
    final RequestConfig.Builder aConfBuilder = RequestConfig.custom ()
                                                            .setConnectionRequestTimeout (nConnectTimeout)
                                                            .setConnectTimeout (nConnectTimeout)
                                                            .setSocketTimeout (nReadTimeout);
    // add proxy if exists
    _setProxyToRequestConfig (aConfBuilder, aProxy);
    final RequestConfig aConf = aConfBuilder.build ();
    final HttpClientBuilder aClientBuilder = HttpClientBuilder.create ();
    if (aSSLContext != null)
      aClientBuilder.setSSLContext (aSSLContext);
    if (aHV != null)
      aClientBuilder.setSSLHostnameVerifier (aHV);

    m_aCloseableHttpClient = aClientBuilder.build ();
    m_aRequestBuilder = RequestBuilder.create (eRequestMethod.getName ()).setUri (sUrl).setConfig (aConf);
  }

  /**
   * Set an HTTP header (replacing existing value). No modification or check on
   * name or value happens.
   *
   * @param sName
   *        Header name. May not be <code>null</code>.
   * @param sValue
   *        Header value. May not be <code>null</code>.
   */
  public void setHttpHeader (@Nonnull final String sName, @Nonnull final String sValue)
  {
    ValueEnforcer.notNull (sName, "Name");
    ValueEnforcer.notNull (sValue, "Value");

    m_aRequestBuilder.setHeader (sName, sValue);
  }

  /**
   * @return URL
   * @throws OpenAS2Exception
   *         in case of error
   */
  @Nonnull
  public URL getURL () throws OpenAS2Exception
  {
    URI uri = null;
    try
    {
      uri = m_aRequestBuilder.getUri ();
      return uri.toURL ();
    }
    catch (final MalformedURLException ex)
    {
      if (LOGGER.isErrorEnabled ())
        LOGGER.error ("Failed to get URL from connection, URI: " + uri.toASCIIString (), ex);
      throw new OpenAS2Exception (ex.getCause ());
    }
  }

  /**
   * Provides an output stream so user can write message content to it. Starts
   * the sending to caller can start write to the received stream
   *
   * @return OutputStream to write message body to
   * @throws IOException
   *         in case of error
   */
  @Nonnull
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
        LOGGER.info ("Runnable: calling execute");
        m_aCloseableHttpResponse = m_aCloseableHttpClient.execute (aHttpUriRequest);
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Runnable: Error in background sending", ex);
      }
      LOGGER.info ("Runnable: Thread exiting");
    });
    aSenderThread.start ();
  }

  /**
   * @param aISToSend
   *        InputStream to send. May not be <code>null</code>.
   * @param eCTE
   *        Content-Transfer-Encoding to be used. May not be <code>null</code>.
   * @param aOutgoingDumper
   *        Optional outgoing dumper
   * @return bytes sent. Must be &ge; 0.
   * @throws IOException
   *         In case of error
   */
  @Nonnegative
  public long send (@Nonnull final InputStream aISToSend,
                    @Nonnull final EContentTransferEncoding eCTE,
                    @Nullable final IHTTPOutgoingDumper aOutgoingDumper) throws IOException
  {
    final CountingInputStream aCIS = new CountingInputStream (aISToSend);
    final InputStreamEntity aISE = new InputStreamEntity (aCIS)
    {
      @Override
      public InputStream getContent () throws IOException
      {
        // Only writeTo should be used
        throw new UnsupportedOperationException ();
      }

      @Override
      public void writeTo (@Nonnull final OutputStream aOS) throws IOException
      {
        // Use MIME encoding here
        try (final OutputStream aDebugOS = aOutgoingDumper != null ? aOutgoingDumper.getDumpOS (aOS) : aOS;
            final OutputStream aEncodedOS = MimeUtility.encode (aDebugOS, eCTE.getID ()))
        {
          super.writeTo (aEncodedOS);
        }
        catch (final MessagingException ex)
        {
          throw new IllegalStateException ("Failed to encode OutputStream with " + eCTE, ex);
        }
      }
    };
    m_aRequestBuilder.setEntity (aISE);
    final HttpUriRequest aHttpUriRequest = m_aRequestBuilder.build ();
    m_aCloseableHttpResponse = m_aCloseableHttpClient.execute (aHttpUriRequest);
    return aCIS.getBytesRead ();
  }

  /**
   * Get InputStream
   *
   * @return InputStream to read response body from
   * @throws OpenAS2Exception
   *         in case of error
   * @throws IOException
   *         in case of IO error
   */
  public InputStream getInputStream () throws OpenAS2Exception, IOException
  {
    // message was not sent yet, not response
    if (m_aCloseableHttpResponse == null)
      throw new OpenAS2Exception ("No response as message was yet sent");

    return m_aCloseableHttpResponse.getEntity ().getContent ();
  }

  /**
   * @return response HTTP Status as int
   * @throws OpenAS2Exception
   *         in case of error
   */
  public int getResponseCode () throws OpenAS2Exception
  {
    // message was not sent yet, not response
    if (m_aCloseableHttpResponse == null)
      throw new OpenAS2Exception ("No response as message was yet sent");

    try
    {
      final StatusLine aStatusLine = m_aCloseableHttpResponse.getStatusLine ();
      return aStatusLine.getStatusCode ();
    }
    catch (final Exception ex)
    {
      throw new OpenAS2Exception (ex);
    }
  }

  /**
   * @return the response message
   * @throws OpenAS2Exception
   *         in case of error
   */
  public String getResponseMessage () throws OpenAS2Exception
  {
    // message was not sent yet, not response
    if (m_aCloseableHttpResponse == null)
      throw new OpenAS2Exception ("No response as message was yet sent");

    final StatusLine aStatusLine = m_aCloseableHttpResponse.getStatusLine ();
    return aStatusLine.getReasonPhrase ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public HttpHeaderMap getResponseHeaderFields () throws OpenAS2Exception
  {
    // message was not sent yet, not response
    if (m_aCloseableHttpResponse == null)
      throw new OpenAS2Exception ("No response as message was yet sent");

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
   *        My by <code>null</code>, in such case nothing is done.
   */
  private static void _setProxyToRequestConfig (@Nonnull final RequestConfig.Builder aConfBuilder,
                                                @Nullable final Proxy aProxy)
  {
    try
    {
      if (null != aProxy)
      {
        final SocketAddress aSocketAddress = aProxy.address ();
        if (aSocketAddress instanceof InetSocketAddress)
        {
          final InetSocketAddress aISocketAdress = (InetSocketAddress) aSocketAddress;
          final InetAddress aInetAddr = aISocketAdress.getAddress ();
          if (null != aInetAddr)
          {
            final HttpHost aHost = new HttpHost (aInetAddr, aISocketAdress.getPort ());
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
    catch (final RuntimeException ex)
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
