/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.as2servlet;

import java.io.DataInputStream;
import java.io.IOException;

import javax.activation.DataSource;
import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.processor.CNetAttribute;
import com.helger.as2lib.processor.receiver.AS2ReceiverModule;
import com.helger.as2lib.session.AS2Session;
import com.helger.as2lib.util.AS2HttpHelper;
import com.helger.as2lib.util.dump.IHTTPIncomingDumper;
import com.helger.as2lib.util.http.HTTPHelper;
import com.helger.as2servlet.util.AS2OutputStreamCreatorHttpServletResponse;
import com.helger.as2servlet.util.AS2ServletReceiverModule;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.http.EHttpMethod;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.http.EHttpVersion;
import com.helger.mail.datasource.ByteArrayDataSource;
import com.helger.servlet.ServletHelper;
import com.helger.web.scope.IRequestWebScope;
import com.helger.xservlet.handler.IXServletHandler;

/**
 * This is the main XServlet handler that takes AS2 messages and processes them.
 * This class contains a lot of methods that may be overridden. So simply
 * subclass this class and create your own Servlet as in
 * {@link AS2ReceiveServlet}.
 *
 * @author Philip Helger
 */
public abstract class AbstractAS2ReceiveXServletHandler implements IXServletHandler
{
  /**
   * The name of the Servlet's init-parameter from which the absolute path to
   * the configuration file is read.
   */
  public static final String SERVLET_INIT_PARAM_AS2_SERVLET_CONFIG_FILENAME = "as2-servlet-config-filename";

  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractAS2ReceiveXServletHandler.class);

  private AS2Session m_aSession;
  private AS2ReceiverModule m_aReceiver;

  /**
   * Create the AS2 session to be used based on the provided configuration file.
   *
   * @param aInitParams
   * @return The created session. May not be <code>null</code>.
   * @throws OpenAS2Exception
   *         In case something goes wrong when initializing the session
   * @throws ServletException
   *         In case an overriding methods wants to throw a different exception
   */
  @Nonnull
  @OverrideOnDemand
  protected abstract AS2Session createAS2Session (@Nonnull ICommonsMap <String, String> aInitParams) throws OpenAS2Exception,
                                                                                                     ServletException;

  @Override
  public void onServletInit (@Nonnull final ICommonsMap <String, String> aInitParams) throws ServletException
  {
    try
    {
      // E.g. read the configuration from a file
      m_aSession = createAS2Session (aInitParams);
      // Don't start active modules to avoid connecting to a port!

      m_aReceiver = m_aSession.getMessageProcessor ().getModuleOfClass (AS2ServletReceiverModule.class);
      if (m_aReceiver == null)
        throw new ServletException ("Failed to retrieve AS2ReceiverModule which is a mandatory module! Please ensure your configuration file contains at least the module '" +
                                    AS2ServletReceiverModule.class.getName () +
                                    "'");
    }
    catch (final OpenAS2Exception ex)
    {
      throw new ServletException ("Failed to init AS2 configuration", ex);
    }

    LOGGER.info ("Successfully initialized AS2 configuration");
  }

  /**
   * @return The AS2 session that was created in initialization. Never
   *         <code>null</code>.
   * @throws IllegalStateException
   *         In case initialization failed
   */
  @Nonnull
  protected AS2Session getSession ()
  {
    if (m_aSession == null)
      throw new IllegalStateException ("This servlet was not initialized properly! No AS2 session is present.");
    return m_aSession;
  }

  /**
   * @return The AS2 receiver module that was created in initialization. Never
   *         <code>null</code>.
   * @throws IllegalStateException
   *         In case initialization failed
   */
  @Nonnull
  protected AS2ReceiverModule getReceiverModule ()
  {
    if (m_aReceiver == null)
      throw new IllegalStateException ("This servlet was not initialized properly! No receiver is present.");
    return m_aReceiver;
  }

  /**
   * Main handling method
   *
   * @param aHttpRequest
   *        HTTP request
   * @param aHttpResponse
   *        HTTP response
   * @param aRequestScope
   *        Current request scope
   * @param aMsgData
   *        Message content
   * @param aMsg
   *        AS2 message object
   * @param aResponseHandler
   *        The response handler for sending back the MDN
   * @throws ServletException
   *         In case of an error
   */
  @OverrideOnDemand
  @OverridingMethodsMustInvokeSuper
  protected void handeIncomingMessage (@Nonnull final HttpServletRequest aHttpRequest,
                                       @Nonnull final HttpServletResponse aHttpResponse,
                                       @Nonnull final IRequestWebScope aRequestScope,
                                       @Nonnull final byte [] aMsgData,
                                       @Nonnull final AS2Message aMsg,
                                       @Nonnull final AS2OutputStreamCreatorHttpServletResponse aResponseHandler) throws ServletException
  {
    // Handle the incoming message, and return the MDN if necessary
    final String sClientInfo = aHttpRequest.getRemoteAddr () + ":" + aHttpRequest.getRemotePort ();

    // for large file support, handleIncomingMessage takes DataSource
    final String sReceivedContentType = AS2HttpHelper.getCleanContentType (aMsg.getHeader (CHttpHeader.CONTENT_TYPE));
    if (sReceivedContentType == null)
      throw new ServletException ("Incoming message does not contain a valid Content-Type: '" +
                                  aMsg.getHeader (CHttpHeader.CONTENT_TYPE) +
                                  "'");

    // Put received data in a MIME body part
    final DataSource aPayload = new ByteArrayDataSource (aMsgData, sReceivedContentType, null);

    // This call internally invokes the AS2ServletSBDModule
    getReceiverModule ().createHandler ().handleIncomingMessage (sClientInfo, aPayload, aMsg, aResponseHandler);
  }

  public final void onRequest (@Nonnull final HttpServletRequest aHttpRequest,
                               @Nonnull final HttpServletResponse aHttpResponse,
                               @Nonnull final EHttpVersion eHttpVersion,
                               @Nonnull final EHttpMethod eHttpMethod,
                               @Nonnull final IRequestWebScope aRequestScope) throws ServletException, IOException
  {
    // Create empty message
    final AS2Message aMsg = new AS2Message ();
    aMsg.attrs ().putIn (CNetAttribute.MA_SOURCE_IP, aHttpRequest.getRemoteAddr ());
    aMsg.attrs ().putIn (CNetAttribute.MA_SOURCE_PORT, aHttpRequest.getRemotePort ());
    aMsg.attrs ().putIn (CNetAttribute.MA_DESTINATION_IP, aHttpRequest.getLocalAddr ());
    aMsg.attrs ().putIn (CNetAttribute.MA_DESTINATION_PORT, aHttpRequest.getLocalPort ());

    // Request type (e.g. "POST")
    aMsg.attrs ().putIn (HTTPHelper.MA_HTTP_REQ_TYPE, aHttpRequest.getMethod ());
    // Request URL (e.g. "/as2")
    aMsg.attrs ().putIn (HTTPHelper.MA_HTTP_REQ_URL, ServletHelper.getRequestRequestURI (aHttpRequest));

    // Add all request headers to the AS2 message
    aMsg.headers ().setAllHeaders (aRequestScope.headers ());

    // Build the handler that performs the response handling
    final AS2OutputStreamCreatorHttpServletResponse aResponseHandler = new AS2OutputStreamCreatorHttpServletResponse (aHttpResponse);

    // Read the S/MIME content in a byte array - memory!
    // Chunked encoding was already handled, so read "as-is"
    final long nContentLength = aHttpRequest.getContentLengthLong ();
    if (nContentLength > Integer.MAX_VALUE)
      throw new IllegalStateException ("Currently only payload with up to 2GB can be handled!");

    final byte [] aMsgData;
    if (nContentLength >= 0)
    {
      // Length is known
      aMsgData = new byte [(int) nContentLength];
      // Closes the HTTP request InputStream afterwards
      try (final DataInputStream aDataIS = new DataInputStream (aHttpRequest.getInputStream ()))
      {
        aDataIS.readFully (aMsgData);
      }
    }
    else
    {
      // Length is unknown
      // Closes the HTTP request InputStream afterwards
      aMsgData = StreamHelper.getAllBytes (aHttpRequest.getInputStream ());
    }

    // Dump on demand
    final IHTTPIncomingDumper aIncomingDumper = HTTPHelper.getHTTPIncomingDumper ();
    if (aIncomingDumper != null)
      aIncomingDumper.dumpIncomingRequest (aMsg.headers ().getAllHeaderLines (), aMsgData, aMsg);

    // Call main handling method
    handeIncomingMessage (aHttpRequest, aHttpResponse, aRequestScope, aMsgData, aMsg, aResponseHandler);
  }
}
