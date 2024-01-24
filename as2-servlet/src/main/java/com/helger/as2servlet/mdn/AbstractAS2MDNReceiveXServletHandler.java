/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.as2servlet.mdn;

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.AS2Exception;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.processor.receiver.net.AS2MDNReceiverHandler;
import com.helger.as2lib.session.AS2Session;
import com.helger.as2lib.util.AS2HttpHelper;
import com.helger.as2lib.util.http.IAS2HttpResponseHandler;
import com.helger.as2servlet.AbstractAS2ReceiveBaseXServletHandler;
import com.helger.as2servlet.util.AS2ServletMDNReceiverModule;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.http.CHttpHeader;

import jakarta.activation.DataSource;
import jakarta.servlet.ServletException;

/**
 * This is the main XServlet handler that takes async MDNs messages and
 * processes them. This class contains a lot of methods that may be overridden.
 *
 * @author Philip Helger
 * @since 4.6.4
 */
public abstract class AbstractAS2MDNReceiveXServletHandler extends AbstractAS2ReceiveBaseXServletHandler
{
  /**
   * The name of the Servlet's init-parameter from which the absolute path to
   * the configuration file is read.
   */
  public static final String SERVLET_INIT_PARAM_AS2_SERVLET_CONFIG_FILENAME = "as2-servlet-config-filename";

  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractAS2MDNReceiveXServletHandler.class);

  private AS2ServletMDNReceiverModule m_aReceiver;

  /**
   * Create the AS2 session to be used based on the provided configuration file.
   *
   * @param aInitParams
   *        The init params of the servlet to use. Never <code>null</code>.
   * @return The created session. May not be <code>null</code>.
   * @throws AS2Exception
   *         In case something goes wrong when initializing the session
   * @throws ServletException
   *         In case an overriding methods wants to throw a different exception
   */
  @Override
  @Nonnull
  @OverrideOnDemand
  protected abstract AS2Session createAS2Session (@Nonnull ICommonsMap <String, String> aInitParams) throws AS2Exception,
                                                                                                     ServletException;

  @Override
  public void onServletInit (@Nonnull final ICommonsMap <String, String> aInitParams) throws ServletException
  {
    super.onServletInit (aInitParams);

    try
    {
      m_aReceiver = getSession ().getMessageProcessor ().getModuleOfClass (AS2ServletMDNReceiverModule.class);
      if (m_aReceiver == null)
        throw new ServletException ("Failed to retrieve 'AS2ServletMDNReceiverModule' which is a mandatory module! Please ensure your configuration file contains at least the module '" +
                                    AS2ServletMDNReceiverModule.class.getName () +
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
  protected final AS2ServletMDNReceiverModule getMDNReceiverModule ()
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
    final AS2MDNReceiverHandler aReceiverHandler = getMDNReceiverModule ().createHandler ();
    aReceiverHandler.handleIncomingMessage (sClientInfo, aMsgData, aMsg, aResponseHandler);
  }
}
