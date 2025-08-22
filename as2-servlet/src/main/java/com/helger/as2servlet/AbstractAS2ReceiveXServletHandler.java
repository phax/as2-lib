/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.OverridingMethodsMustInvokeSuper;
import com.helger.annotation.style.OverrideOnDemand;
import com.helger.as2lib.exception.AS2Exception;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.processor.receiver.net.AS2ReceiverHandler;
import com.helger.as2lib.util.AS2HttpHelper;
import com.helger.as2lib.util.http.IAS2HttpResponseHandler;
import com.helger.as2servlet.util.AS2ServletReceiverModule;
import com.helger.collection.commons.ICommonsMap;
import com.helger.http.CHttpHeader;

import jakarta.activation.DataSource;
import jakarta.annotation.Nonnull;
import jakarta.servlet.ServletException;

/**
 * This is the main XServlet handler that takes AS2 messages and processes them.
 * This class contains a lot of methods that may be overridden. In v4.6.4 the
 * abstract base class {@link AbstractAS2ReceiveBaseXServletHandler} was
 * extracted to contain the common parts for async MDNs.<br>
 * It expects a module of class {@link AS2ServletReceiverModule} to be
 * registered in the messages processor of the created session.
 *
 * @author Philip Helger
 */
public abstract class AbstractAS2ReceiveXServletHandler extends AbstractAS2ReceiveBaseXServletHandler
{
  /**
   * The name of the Servlet's init-parameter from which the absolute path to
   * the configuration file is read.
   */
  public static final String SERVLET_INIT_PARAM_AS2_SERVLET_CONFIG_FILENAME = "as2-servlet-config-filename";

  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractAS2ReceiveXServletHandler.class);

  private AS2ServletReceiverModule m_aReceiver;

  @Override
  public void onServletInit (@Nonnull final ICommonsMap <String, String> aInitParams) throws ServletException
  {
    super.onServletInit (aInitParams);

    try
    {
      m_aReceiver = getSession ().getMessageProcessor ().getModuleOfClass (AS2ServletReceiverModule.class);
      if (m_aReceiver == null)
        throw new ServletException ("Failed to retrieve 'AS2ServletReceiverModule' which is a mandatory module! Please ensure your configuration file contains at least the module '" +
                                    AS2ServletReceiverModule.class.getName () +
                                    "'");
    }
    catch (final AS2Exception ex)
    {
      throw new ServletException ("Failed to init AS2 configuration", ex);
    }

    LOGGER.info ("Successfully initialized AS2 configuration");
  }

  /**
   * @return The AS2 receiver module that was created in initialization. Never
   *         <code>null</code>.
   * @throws IllegalStateException
   *         In case initialization failed
   */
  @Nonnull
  protected final AS2ServletReceiverModule getReceiverModule ()
  {
    if (m_aReceiver == null)
      throw new IllegalStateException ("This servlet was not initialized properly! No receiver is present.");
    return m_aReceiver;
  }

  @Override
  protected final boolean isQuoteHeaderValues ()
  {
    return m_aReceiver.isQuoteHeaderValues ();
  }

  @Override
  @OverrideOnDemand
  @OverridingMethodsMustInvokeSuper
  protected void handleIncomingMessage (@Nonnull final String sClientInfo,
                                        @Nonnull final DataSource aMsgData,
                                        @Nonnull final AS2Message aMsg,
                                        @Nonnull final IAS2HttpResponseHandler aResponseHandler) throws ServletException
  {
    // for large file support, handleIncomingMessage takes DataSource
    final String sReceivedContentType = AS2HttpHelper.getCleanContentType (aMsg.getHeader (CHttpHeader.CONTENT_TYPE));
    if (sReceivedContentType == null)
      throw new ServletException ("Incoming message does not contain a valid Content-Type: '" +
                                  aMsg.getHeader (CHttpHeader.CONTENT_TYPE) +
                                  "'");

    // This call internally invokes the AS2ServletSBDModule
    final AS2ReceiverHandler aReceiverHandler = getReceiverModule ().createHandler ();
    aReceiverHandler.handleIncomingMessage (sClientInfo, aMsgData, aMsg, aResponseHandler);
  }
}
