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

import com.helger.as2lib.processor.sender.AbstractHttpSenderModule;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;

import java.io.*;
import java.net.*;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.http.EHttpMethod;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


//import static com.helger.as2lib.client.AS2ClientSettings.DEFAULT_CONNECT_TIMEOUT_MS;
//import static com.helger.as2lib.client.AS2ClientSettings.DEFAULT_READ_TIMEOUT_MS;
//import static com.helger.as2lib.processor.sender.AbstractHttpSenderModule.ATTR_CONNECT_TIMEOUT;
//import static com.helger.as2lib.processor.sender.AbstractHttpSenderModule.ATTR_READ_TIMEOUT;


/**
 * Http connection, Implemented as HttpClient.
 *
 * @author Ziv Harpaz
 */
public class AS2HttpClient implements IAS2HttpConnection{
	private RequestBuilder        m_aRequestBuilder;
	private PipedInputStream      m_aPipedInputStream;
	private PipedOutputStream     m_aPipedOutputStream;
	@SuppressWarnings("CanBeFinal")
	private CloseableHttpClient   m_aCloseableHttpClient;
	private CloseableHttpResponse m_aCloseableHttpResponse;
	private static final Logger s_aLogger = LoggerFactory.getLogger (AS2HttpClient.class);

	public AS2HttpClient (@Nonnull @Nonempty final String sUrl,
	                      final int iConnectTimeout,
	                      final int iReadTimeout,
	                      @Nonnull final EHttpMethod eRequestMethod,
	                      @Nullable final Proxy aProxy)
		throws OpenAS2Exception {
		try {
			// set configuration
      RequestConfig.Builder aConfBuilder = RequestConfig.custom()
        .setConnectionRequestTimeout(iConnectTimeout)
        .setConnectTimeout(iConnectTimeout)
        .setSocketTimeout(iReadTimeout);
      //add proxy if exists
      setProxyToRequestConfig(aConfBuilder, aProxy);
			RequestConfig aConf = aConfBuilder.build();
			URI aUri = new URI(sUrl);
			HttpClientBuilder aClientBuilder = HttpClientBuilder.create();
			if (aUri.getScheme().toLowerCase().equals("https")){
				// Create SSL context
				final SSLContext aSSLCtx = AbstractHttpSenderModule.createSSLContext ();
				// Get hostname verifier
				final HostnameVerifier aHV = AbstractHttpSenderModule.createHostnameVerifier ();
				aClientBuilder.setSSLContext(aSSLCtx)
					.setSSLHostnameVerifier(aHV);
			}
			m_aCloseableHttpClient = aClientBuilder.build();
			m_aRequestBuilder = RequestBuilder.create(eRequestMethod.getName())
				.setUri(aUri)
				.setConfig(aConf);
		}catch (java.net.URISyntaxException|GeneralSecurityException e) {
			s_aLogger.error("Exception in AS2HttpClient constructor: ", e.getMessage());
			throw new OpenAS2Exception(e.getMessage());
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
	public void setHttpHeader(@Nonnull String sName, @Nonnull String sValue){
		m_aRequestBuilder.setHeader(sName, sValue);
	}

	/**
	 * Get URL
	 *
	 */
	public URL getURL() throws OpenAS2Exception
	{
		URI uri = null;
		try {
			uri = m_aRequestBuilder.getUri();
			return uri.toURL();
		} catch (IllegalArgumentException|MalformedURLException e){
			if (s_aLogger.isErrorEnabled ())
				s_aLogger.error ("Failed to get URL from connection, URI: " +
					(uri == null ? "null" : uri.toASCIIString()));
			throw new OpenAS2Exception(e.getCause());
		}
	}

	/**
	 * Provides an output stream so user can write message content to it. Starts the sending to caller can start write to the received stream
	 *
	 * @return OutputStream to write message body to
	 */
	public OutputStream getOutputStream() throws IOException {
		if (m_aPipedOutputStream == null){
			m_aPipedInputStream = new PipedInputStream();
			m_aPipedOutputStream = new PipedOutputStream(m_aPipedInputStream);
		}
		m_aRequestBuilder.setEntity(new InputStreamEntity(m_aPipedInputStream));
		sendInBackground();
		return m_aPipedOutputStream;
	}

	/**
	 * send the request in background, allowing the forground to write to the OutputStream
	 */
	private void sendInBackground(){
		Thread aSenderThread = new Thread(() -> {
			try {
				HttpUriRequest aHttpUriRequest = m_aRequestBuilder.build();
				System.out.println("Runnable: calling execute");
				m_aCloseableHttpResponse = m_aCloseableHttpClient.execute(aHttpUriRequest);
			}catch (Exception e){
				e.printStackTrace();
			}
			System.out.println("Runnable: Thread exiting");
		});
		aSenderThread.start();
	}

	public void send(InputStream toSend) throws IOException {
		m_aRequestBuilder.setEntity(new InputStreamEntity(toSend));
		HttpUriRequest aHttpUriRequest = m_aRequestBuilder.build();
		m_aCloseableHttpResponse = m_aCloseableHttpClient.execute(aHttpUriRequest);

	}

	/**
	 * Get InputStream
	 *
	 * @return InputStream to read response body from
	 */
	public InputStream getInputStream() throws OpenAS2Exception, IOException{
		// message was not sent yet, not response
		if (m_aCloseableHttpResponse == null) {
			throw new OpenAS2Exception("No response as message was not yet sent");
		}
		return m_aCloseableHttpResponse.getEntity().getContent();
	}

	/**
	 * Get response HTTP Status as integer
	 *
	 */
	public int getResponseCode() throws OpenAS2Exception {
		try {
			// message was not sent yet, not response
			if (m_aCloseableHttpResponse == null) {
				throw new OpenAS2Exception("No response as message was not yet sent");
			}
			StatusLine status = m_aCloseableHttpResponse.getStatusLine();
			return status.getStatusCode();
		}catch (Exception e){
			throw new OpenAS2Exception(e.getCause());
		}
	}

	/**
	 * Get the response message
	 *
	 */
	public String getResponseMessage() throws OpenAS2Exception {
		// message was not sent yet, not response
		if (m_aCloseableHttpResponse == null) {
			throw new OpenAS2Exception("No response as message was not yet sent");
		}
		StatusLine status = m_aCloseableHttpResponse.getStatusLine();
		return status.getReasonPhrase();
	}

	/**
	 * Get the headers of the request
	 *
	 */
	public Map<String,List<String>> getHeaderFields()throws OpenAS2Exception{
		// message was not sent yet, not response
		if (m_aCloseableHttpResponse == null) {
			throw new OpenAS2Exception("No response as message was not yet sent");
		}
		Header[] headers = m_aCloseableHttpResponse.getAllHeaders();
		Map<String,List<String>> res = new HashMap<>();
		for (Header h :headers){
			List<String> values = new LinkedList<>();
			values.add(h.getValue());
			res.put(h.getName(),values);
		}
		return res;
	}

	/**
	 * Close the connection
	 *
	 */
	public void disconnect() {
		try {
			if (m_aCloseableHttpResponse != null) m_aCloseableHttpResponse.close();
			if (m_aCloseableHttpClient != null) m_aCloseableHttpClient.close();
		} catch (Exception e){
			if (s_aLogger.isErrorEnabled ())
				s_aLogger.error ("Exception while closing HttpClient connection: " + this.toString());
		}
	}

  /**
   * Set {@link Proxy} into {@link RequestConfig.Builder}
   *
   * @param aConfBuilder
   *        {@link RequestConfig.Builder} to set
   * @param aProxy
   *        My by null, in such case nothing is done.
   *
   */
  private static void setProxyToRequestConfig(@Nonnull RequestConfig.Builder aConfBuilder, @Nullable Proxy aProxy)
  {
    try {
      if (null != aProxy) {
        SocketAddress aSocketAddress = aProxy.address();
        if (null != aSocketAddress &&
          aSocketAddress instanceof InetSocketAddress) {
          InetSocketAddress aISocketAdress = (InetSocketAddress) aSocketAddress;
          InetAddress aInetAddr = aISocketAdress.getAddress();
          if (null != aInetAddr) {
            HttpHost aHost = new HttpHost(aInetAddr);
            aConfBuilder.setProxy(aHost);
          } else {
            s_aLogger.debug("No address in proxy:{}-{}",
              aProxy.address(),
              (null != aProxy.type()? aProxy.type().name():"null"));
          }
        }
      }
    }
    catch (Exception e)
    {
      String aMessage = String.format("Exception while setting proxy. Continue without proxy. aProxy:%s-%s",
        aProxy.address(),
        (null != aProxy.type()? aProxy.type().name():"null"));
      s_aLogger.error(aMessage, e);
    }
  }
}
