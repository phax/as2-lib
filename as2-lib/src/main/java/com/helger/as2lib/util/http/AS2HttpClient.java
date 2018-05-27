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

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.http.EHttpMethod;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
	private RequestBuilder        aRequestBuilder;
	private PipedInputStream      aPipedInputStream;
	private PipedOutputStream     aPipedOutputStream;
	@SuppressWarnings("CanBeFinal")
	private CloseableHttpClient   aCloseableHttpClient;
	private CloseableHttpResponse aCloseableHttpResponse;
	private static final Logger s_aLogger = LoggerFactory.getLogger (AS2HttpClient.class);
	private boolean               bSendComplete=false;
	private Thread                aSenderThread=null;

	public AS2HttpClient (@Nonnull @Nonempty final String sUrl,
	                      final int iConnectTimeout,
	                      final int iReadTimeout,
	                      @Nonnull final EHttpMethod eRequestMethod,
	                      //TODO handle proxy
	                      @Nullable final Proxy aProxy)
		throws OpenAS2Exception {
		try {
			aCloseableHttpClient = HttpClients.createDefault();
			// set configuration
			RequestConfig conf = RequestConfig.custom()
				.setConnectionRequestTimeout(iConnectTimeout)
				.setConnectTimeout(iConnectTimeout)
				.setSocketTimeout(iReadTimeout)
				.build();
			//TODO handle SSL
			URI aUri = new URI(sUrl);
			aRequestBuilder = RequestBuilder.create(eRequestMethod.getName())
				.setUri(aUri)
				.setConfig(conf);
		}catch (java.net.URISyntaxException e) {
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
		aRequestBuilder.setHeader(sName, sValue);
	}

	/**
	 * Get URL
	 *
	 */
	public URL getURL() throws OpenAS2Exception
	{
		URI uri = null;
		try {
			uri = aRequestBuilder.getUri();
			URL url = uri.toURL();
			return url;
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
		if (aPipedOutputStream == null){
			aPipedInputStream  = new PipedInputStream();
			aPipedOutputStream = new PipedOutputStream(aPipedInputStream);
		}
		aRequestBuilder.setEntity(new InputStreamEntity(aPipedInputStream));
		sendInBackground();
		return aPipedOutputStream;
	}

	/**
	 * send the request in background, allowing the forground to write to the OutputStream
	 */
	private void sendInBackground(){
		aSenderThread = new Thread(() -> {
			try {
				HttpUriRequest aHttpUriRequest = aRequestBuilder.build();
				System.out.println("Runnable: calling execute");
				aCloseableHttpResponse = aCloseableHttpClient.execute(aHttpUriRequest);
				bSendComplete = true;
			}catch (Exception e){
				e.printStackTrace();
			}
			System.out.println("Runnable: Thread exiting");
		});
		aSenderThread.start();
	}

	public void send(InputStream toSend) throws IOException {
		aRequestBuilder.setEntity(new InputStreamEntity(toSend));
		HttpUriRequest aHttpUriRequest = aRequestBuilder.build();
		aCloseableHttpResponse = aCloseableHttpClient.execute(aHttpUriRequest);

	}

	/**
	 * isSendComplete
	 *
	 * @return true if sending has completed
	 */
	private boolean isbSendComplete(){ return bSendComplete;}

	/**
	 * Get InputStream
	 *
	 * @return InputStream to read response body from
	 */
	public InputStream getInputStream() throws OpenAS2Exception, IOException{
		// message was not sent yet, not response
		if (aCloseableHttpResponse == null) {
			throw new OpenAS2Exception("No response as message was not yet sent");
		}
		return aCloseableHttpResponse.getEntity().getContent();
	}

	/**
	 * Get response HTTP Status as integer
	 *
	 */
	public int getResponseCode() throws OpenAS2Exception {
		try {
			//aSenderThread.wait();
			// message was not sent yet, not response
			if (aCloseableHttpResponse == null) {
				throw new OpenAS2Exception("No response as message was not yet sent");
			}
			StatusLine status = aCloseableHttpResponse.getStatusLine();
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
		if (aCloseableHttpResponse == null) {
			throw new OpenAS2Exception("No response as message was not yet sent");
		}
		StatusLine status = aCloseableHttpResponse.getStatusLine();
		return status.getReasonPhrase();
	}

	/**
	 * Get the headers of the request
	 *
	 */
	public Map<String,List<String>> getHeaderFields()throws OpenAS2Exception{
		// message was not sent yet, not response
		if (aCloseableHttpResponse == null) {
			throw new OpenAS2Exception("No response as message was not yet sent");
		}
		Header[] headers = aCloseableHttpResponse.getAllHeaders();
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
			if (aCloseableHttpResponse != null) aCloseableHttpResponse.close();
			if (aCloseableHttpClient   != null) aCloseableHttpClient.close();
		} catch (Exception e){
			if (s_aLogger.isErrorEnabled ())
				s_aLogger.error ("Exception while closing HttpClient connection: " + this.toString());
		}
	}
}
