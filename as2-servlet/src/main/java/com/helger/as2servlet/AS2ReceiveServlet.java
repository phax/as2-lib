/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.processor.CNetAttribute;
import com.helger.as2lib.processor.receiver.AS2ReceiverModule;
import com.helger.as2lib.util.http.HTTPHelper;
import com.helger.as2servlet.util.AS2OutputStreamCreatorHttpServletResponse;
import com.helger.as2servlet.util.AS2ServletReceiverModule;
import com.helger.as2servlet.util.AS2ServletSession;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.string.StringHelper;

/**
 * This is the main servlet that takes AS2 messages and processes them. This
 * servlet is configured to accept only POST requests. This class contains a lot
 * of methods that may be overridden. So simply subclass this class and
 * reference the subclasses class in your web.xml file.
 *
 * @author Philip Helger
 */
public class AS2ReceiveServlet extends HttpServlet
{
  /**
   * The name of the Servlet's init-parameter from which the absolute path to
   * the configuration file is read.
   */
  public static final String SERVLET_INIT_PARAM_AS2_SERVLET_CONFIG_FILENAME = "as2-servlet-config-filename";

  private static final Logger s_aLogger = LoggerFactory.getLogger (AS2ReceiveServlet.class);

  private AS2ServletSession m_aSession;
  private AS2ReceiverModule m_aReceiver;

  /**
   * Get the AS2 configuration file to be used. By default this method reads it
   * from the Servlet init-param called
   * {@link #SERVLET_INIT_PARAM_AS2_SERVLET_CONFIG_FILENAME}. You may override
   * this method to use another way of retrieving the configuration file. <br>
   * Note: it must be a {@link File} because the configuration file allows for
   * "%home%" parameter substitution which uses the directory of the
   * configuration file as the base directory.
   *
   * @return The configuration file to be used. MUST not be <code>null</code>.
   * @throws ServletException
   *         If no or an invalid configuration file was provided.
   */
  @OverrideOnDemand
  @Nonnull
  protected File getConfigurationFile () throws ServletException
  {
    final String sConfigurationFilename = getServletConfig ().getInitParameter (SERVLET_INIT_PARAM_AS2_SERVLET_CONFIG_FILENAME);
    if (StringHelper.hasNoText (sConfigurationFilename))
      throw new ServletException ("Servlet Init-Parameter '" +
                                  SERVLET_INIT_PARAM_AS2_SERVLET_CONFIG_FILENAME +
                                  "' is missing or empty!");

    try
    {
      return new File (sConfigurationFilename).getCanonicalFile ();
    }
    catch (final IOException ex)
    {
      throw new ServletException ("Failed to get the canonical file from '" + sConfigurationFilename + "'", ex);
    }
  }

  /**
   * Create the AS2 session to be used based on the provided configuration file.
   *
   * @param aConfigurationFile
   *        Configuration file to be used. Never <code>null</code>.
   * @return The created session. May not be <code>null</code>.
   * @throws OpenAS2Exception
   *         In case something goes wrong when initializing the session
   * @throws ServletException
   *         In case an overriding methods wants to throw a different exception
   */
  @Nonnull
  @OverrideOnDemand
  protected AS2ServletSession createAS2ServletSession (@Nonnull final File aConfigurationFile) throws OpenAS2Exception,
                                                                                               ServletException
  {
    return new AS2ServletSession (aConfigurationFile);
  }

  @Override
  public void init () throws ServletException
  {
    // Get configuration file
    final File aConfigurationFile = getConfigurationFile ();
    if (aConfigurationFile == null)
      throw new ServletException ("No configuration file provided!");

    try
    {
      // Read the configuration from a file
      m_aSession = createAS2ServletSession (aConfigurationFile);
      // Don't start active modules to avoid connecting to a port!

      m_aReceiver = m_aSession.getMessageProcessor ().getModuleOfClass (AS2ServletReceiverModule.class);
      if (m_aReceiver == null)
        throw new ServletException ("Failed to retrieve AS2ReceiverModule which is a mandatory module!");
    }
    catch (final OpenAS2Exception ex)
    {
      throw new ServletException ("Failed to init AS2 configuration", ex);
    }

    s_aLogger.info ("Successfully initialized AS2 configuration from file '" +
                    aConfigurationFile.getAbsolutePath () +
                    "'");
  }

  /**
   * @return The AS2 session that was created in {@link #init()}. Never
   *         <code>null</code>.
   * @throws IllegalStateException
   *         In case initialization failed
   */
  @Nonnull
  protected AS2ServletSession getSession ()
  {
    if (m_aSession == null)
      throw new IllegalStateException ("This servlet was not initialized properly! No AS2 session is present.");
    return m_aSession;
  }

  /**
   * @return The AS2 receiver module that was created in {@link #init()}. Never
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
                                       @Nonnull final byte [] aMsgData,
                                       @Nonnull final AS2Message aMsg,
                                       @Nonnull final AS2OutputStreamCreatorHttpServletResponse aResponseHandler) throws ServletException
  {
    // Handle the incoming message, and return the MDN if necessary
    final String sClientInfo = aHttpRequest.getRemoteAddr () + ":" + aHttpRequest.getRemotePort ();

    // This call internally invokes the AS2ServletSBDModule
    getReceiverModule ().createHandler ().handleIncomingMessage (sClientInfo, aMsgData, aMsg, aResponseHandler);
  }

  @Override
  protected void doPost (@Nonnull final HttpServletRequest aHttpRequest,
                         @Nonnull final HttpServletResponse aHttpResponse) throws ServletException, IOException
  {
    // Create empty message
    final AS2Message aMsg = new AS2Message ();
    aMsg.setAttribute (CNetAttribute.MA_SOURCE_IP, aHttpRequest.getRemoteAddr ());
    aMsg.setAttribute (CNetAttribute.MA_SOURCE_PORT, Integer.toString (aHttpRequest.getRemotePort ()));
    aMsg.setAttribute (CNetAttribute.MA_DESTINATION_IP, aHttpRequest.getLocalAddr ());
    aMsg.setAttribute (CNetAttribute.MA_DESTINATION_PORT, Integer.toString (aHttpRequest.getLocalPort ()));

    // Request type (e.g. "POST")
    aMsg.setAttribute (HTTPHelper.MA_HTTP_REQ_TYPE, aHttpRequest.getMethod ());
    // Request URL (e.g. "/as2")
    aMsg.setAttribute (HTTPHelper.MA_HTTP_REQ_URL, aHttpRequest.getRequestURI ());

    // Add all request headers to the AS2 message
    final Enumeration <?> eHeaders = aHttpRequest.getHeaderNames ();
    while (eHeaders.hasMoreElements ())
    {
      final String sName = (String) eHeaders.nextElement ();
      final Enumeration <?> eHeaderValues = aHttpRequest.getHeaders (sName);
      while (eHeaderValues.hasMoreElements ())
      {
        final String sValue = (String) eHeaderValues.nextElement ();
        aMsg.addHeader (sName, sValue);
      }
    }

    // Build the handler that performs the response handling
    final AS2OutputStreamCreatorHttpServletResponse aResponseHandler = new AS2OutputStreamCreatorHttpServletResponse (aHttpResponse);

    // Read the S/MIME content
    final byte [] aMsgData = HTTPHelper.readHttpPayload (aHttpRequest.getInputStream (), aResponseHandler, aMsg);

    // Dump on demand
    HTTPHelper.dumpHttpRequest (HTTPHelper.getAllHTTPHeaderLines (aMsg.getHeaders ()), aMsgData);

    // Call main handling method
    handeIncomingMessage (aHttpRequest, aHttpResponse, aMsgData, aMsg, aResponseHandler);
  }
}
