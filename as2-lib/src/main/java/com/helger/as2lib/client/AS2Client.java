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
package com.helger.as2lib.client;

import java.net.Proxy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.cert.CertificateExistsException;
import com.helger.as2lib.cert.CertificateFactory;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.params.MessageParameters;
import com.helger.as2lib.partner.CPartnershipIDs;
import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.partner.SelfFillingPartnershipFactory;
import com.helger.as2lib.processor.DefaultMessageProcessor;
import com.helger.as2lib.processor.IMessageProcessor;
import com.helger.as2lib.processor.resender.IProcessorResenderModule;
import com.helger.as2lib.processor.resender.ImmediateResenderModule;
import com.helger.as2lib.processor.sender.AS2SenderModule;
import com.helger.as2lib.processor.sender.AbstractHttpSenderModule;
import com.helger.as2lib.processor.sender.IProcessorSenderModule;
import com.helger.as2lib.session.AS2Session;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.collection.attr.StringMap;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.factory.FactoryNewInstance;
import com.helger.commons.functional.ISupplier;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.timing.StopWatch;

/**
 * A simple client that allows for sending AS2 Messages and retrieving of
 * synchronous MDNs.
 *
 * @author Philip Helger
 */
public class AS2Client
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS2Client.class);
  private ISupplier <AS2SenderModule> m_aAS2SenderModuleFactory = FactoryNewInstance.create (AS2SenderModule.class,
                                                                                             true);
  // proxy is not serializable
  private Proxy m_aHttpProxy;

  public AS2Client ()
  {}

  /**
   * Set the factory to create {@link AS2SenderModule} objects internally. By
   * default a new instance of {@link AS2SenderModule} is created so you don't
   * need to call this method.
   *
   * @param aAS2SenderModuleFactory
   *        The factory to be used. May not be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public AS2Client setAS2SenderModuleFactory (@Nonnull final ISupplier <AS2SenderModule> aAS2SenderModuleFactory)
  {
    m_aAS2SenderModuleFactory = ValueEnforcer.notNull (aAS2SenderModuleFactory, "AS2SenderModuleFactory");
    return this;
  }

  /**
   * @return The current HTTP proxy used. Defaults to <code>null</code>.
   */
  @Nullable
  public Proxy getHttpProxy ()
  {
    return m_aHttpProxy;
  }

  /**
   * Set the proxy server for transmission.
   *
   * @param aHttpProxy
   *        The proxy to use. May be <code>null</code> to indicate no proxy.
   * @return this for chaining
   */
  @Nonnull
  public AS2Client setHttpProxy (@Nullable final Proxy aHttpProxy)
  {
    m_aHttpProxy = aHttpProxy;
    return this;
  }

  /**
   * Create a new {@link Partnership} object that is later used for message
   * creation. If you override this method, please ensure to call this class'
   * version of the method first.
   *
   * @param aSettings
   *        The current client settings. Never <code>null</code>.
   * @return Non-<code>null</code> Partnership.
   */
  @Nonnull
  @OverrideOnDemand
  @OverridingMethodsMustInvokeSuper
  protected Partnership buildPartnership (@Nonnull final AS2ClientSettings aSettings)
  {
    final Partnership aPartnership = new Partnership (aSettings.getPartnershipName ());

    aPartnership.setSenderAS2ID (aSettings.getSenderAS2ID ());
    aPartnership.setSenderX509Alias (aSettings.getSenderKeyAlias ());
    aPartnership.setSenderEmail (aSettings.getSenderEmailAddress ());

    aPartnership.setReceiverAS2ID (aSettings.getReceiverAS2ID ());
    aPartnership.setReceiverX509Alias (aSettings.getReceiverKeyAlias ());

    aPartnership.setAS2URL (aSettings.getDestinationAS2URL ());
    aPartnership.setEncryptAlgorithm (aSettings.getCryptAlgo ());
    aPartnership.setSigningAlgorithm (aSettings.getSignAlgo ());
    aPartnership.setProtocol (AS2Message.PROTOCOL_AS2);
    aPartnership.setMessageIDFormat (aSettings.getMessageIDFormat ());

    if (aSettings.isMDNRequested ())
    {
      // We want an MDN

      // This field must be set to a valid email address
      aPartnership.setAS2MDNTo (aSettings.getSenderEmailAddress ());

      // signed MDN requested?
      aPartnership.setAS2MDNOptions (aSettings.getMDNOptions ());

      if (aSettings.isAsyncMDNRequested ())
      {
        // We want an async MDN
        aPartnership.setAS2ReceiptDeliveryOption (aSettings.getAsyncMDNUrl ());
      }
      else
      {
        // We want an sync MDN
        // This field must be null - otherwise async MDN!
        aPartnership.setAS2ReceiptDeliveryOption (null);
      }
    }
    else
    {
      // We don't want an MDN
      aPartnership.setAS2MDNTo (null);
      aPartnership.setAS2ReceiptDeliveryOption (null);
      aPartnership.setAS2MDNOptions (null);
    }

    if (aSettings.getCompressionType () != null)
    {
      aPartnership.setCompressionType (aSettings.getCompressionType ());
      aPartnership.setCompressionMode (aSettings.isCompressBeforeSigning () ? CPartnershipIDs.COMPRESS_BEFORE_SIGNING
                                                                            : CPartnershipIDs.COMPRESS_AFTER_SIGNING);
    }

    return aPartnership;
  }

  @Nonnull
  @OverrideOnDemand
  protected AS2Message createAS2MessageObj ()
  {
    return new AS2Message ();
  }

  @Nonnull
  @OverrideOnDemand
  @OverridingMethodsMustInvokeSuper
  protected AS2Message createMessage (@Nonnull final Partnership aPartnership,
                                      @Nonnull final AS2ClientRequest aRequest) throws MessagingException
  {
    final AS2Message aMsg = createAS2MessageObj ();
    aMsg.setContentType (aRequest.getContentType ());
    aMsg.setSubject (aRequest.getSubject ());
    aMsg.setPartnership (aPartnership);
    aMsg.setMessageID (aMsg.generateMessageID ());

    aMsg.attrs ().putIn (CPartnershipIDs.PA_AS2_URL, aPartnership.getAS2URL ());
    aMsg.attrs ().putIn (CPartnershipIDs.PID_AS2, aPartnership.getReceiverAS2ID ());
    aMsg.attrs ().putIn (CPartnershipIDs.PID_EMAIL, aPartnership.getSenderEmail ());

    // Build message content
    final MimeBodyPart aPart = new MimeBodyPart ();
    aRequest.applyDataOntoMimeBodyPart (aPart);
    aMsg.setData (aPart);

    return aMsg;
  }

  /**
   * @return The certificate factory instance to be used. May not be
   *         <code>null</code>.
   */
  @Nonnull
  @OverrideOnDemand
  protected CertificateFactory createCertificateFactory ()
  {
    return new CertificateFactory ();
  }

  @OverrideOnDemand
  protected void initCertificateFactory (@Nonnull final AS2ClientSettings aSettings,
                                         @Nonnull final AS2Session aSession) throws OpenAS2Exception
  {
    final StringMap aParams = new StringMap ();
    // TYPE is the only parameter that must be present in initDynamicComponents
    aParams.putIn (CertificateFactory.ATTR_TYPE, aSettings.getKeyStoreType ().getID ());

    final CertificateFactory aCertFactory = createCertificateFactory ();
    aCertFactory.initDynamicComponent (aSession, aParams);

    if (aSettings.getKeyStoreFile () != null)
    {
      aCertFactory.setFilename (aSettings.getKeyStoreFile ().getAbsolutePath ());
      aCertFactory.setPassword (aSettings.getKeyStorePassword ());
      aCertFactory.setSaveChangesToFile (aSettings.isSaveKeyStoreChangesToFile ());
      aCertFactory.load ();
    }
    else
      if (aSettings.getKeyStoreBytes () != null && aSettings.getKeyStorePassword () != null)
      {
        aCertFactory.setSaveChangesToFile (false);
        aCertFactory.load (new NonBlockingByteArrayInputStream (aSettings.getKeyStoreBytes ()),
                           aSettings.getKeyStorePassword ().toCharArray ());
      }
      else
      {
        // No file provided - no storage
        aCertFactory.setSaveChangesToFile (false);
      }

    if (aSettings.getReceiverCertificate () != null)
    {
      // Dynamically add recipient certificate if provided
      try
      {
        aCertFactory.addCertificate (aSettings.getReceiverKeyAlias (), aSettings.getReceiverCertificate (), false);
      }
      catch (final CertificateExistsException ex)
      {
        // ignore
      }
    }
    aSession.setCertificateFactory (aCertFactory);
  }

  @OverrideOnDemand
  protected void initPartnershipFactory (@Nonnull final AS2Session aSession) throws OpenAS2Exception
  {
    // Use a self-filling in-memory partnership factory
    final SelfFillingPartnershipFactory aPartnershipFactory = new SelfFillingPartnershipFactory ();
    aSession.setPartnershipFactory (aPartnershipFactory);
  }

  @OverrideOnDemand
  protected void initMessageProcessor (@Nonnull final AS2Session aSession) throws OpenAS2Exception
  {
    final IMessageProcessor aMessageProcessor = new DefaultMessageProcessor ();
    aSession.setMessageProcessor (aMessageProcessor);
  }

  /**
   * Create an empty response object that is to be filled.
   *
   * @return The empty response object and never <code>null</code>.
   */
  @Nonnull
  @OverrideOnDemand
  protected AS2ClientResponse createResponse ()
  {
    return new AS2ClientResponse ();
  }

  /**
   * Create the AS2 session to be used. This method must ensure an eventually
   * needed proxy is set.
   *
   * @return The new AS2 session and never <code>null</code>.
   */
  @Nonnull
  @OverrideOnDemand
  protected AS2Session createSession ()
  {
    final AS2Session ret = new AS2Session ();
    ret.setHttpProxy (m_aHttpProxy);
    return ret;
  }

  /**
   * Callback method that is invoked before the main sending. This may be used
   * to customize the message.
   *
   * @param aSettings
   *        Client settings. Never <code>null</code>.
   * @param aSession
   *        Current session. Never <code>null</code>.
   * @param aMsg
   *        Current message. Never <code>null</code>.
   */
  @OverrideOnDemand
  protected void beforeSend (@Nonnull final AS2ClientSettings aSettings,
                             @Nonnull final AS2Session aSession,
                             @Nonnull final IMessage aMsg)
  {}

  /**
   * Send the AS2 message synchronously
   *
   * @param aSettings
   *        The settings to be used. May not be <code>null</code>.
   * @param aRequest
   *        The request data to be send. May not be <code>null</code>.
   * @return The response object. Never <code>null</code>.
   */
  @Nonnull
  public AS2ClientResponse sendSynchronous (@Nonnull final AS2ClientSettings aSettings,
                                            @Nonnull final AS2ClientRequest aRequest)
  {
    ValueEnforcer.notNull (aSettings, "ClientSettings");
    ValueEnforcer.notNull (aRequest, "ClientRequest");

    final AS2ClientResponse aResponse = createResponse ();
    IMessage aMsg = null;
    final StopWatch aSW = StopWatch.createdStarted ();
    try
    {
      final Partnership aPartnership = buildPartnership (aSettings);

      aMsg = createMessage (aPartnership, aRequest);
      aResponse.setOriginalMessageID (aMsg.getMessageID ());

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("MessageID to send: " + aMsg.getMessageID ());

      final boolean bHasRetries = aSettings.getRetryCount () > 0;

      // Start a new session
      final AS2Session aSession = createSession ();

      initCertificateFactory (aSettings, aSession);
      initPartnershipFactory (aSession);
      initMessageProcessor (aSession);

      if (bHasRetries)
      {
        // Use synchronous no-delay resender
        final IProcessorResenderModule aResender = new ImmediateResenderModule ();
        aResender.initDynamicComponent (aSession, null);
        aSession.getMessageProcessor ().addModule (aResender);
      }

      aSession.getMessageProcessor ().startActiveModules ();
      try
      {
        // Invoke callback
        beforeSend (aSettings, aSession, aMsg);

        // Build options map for "handle"
        final ICommonsMap <String, Object> aHandleOptions = new CommonsHashMap <> ();
        if (bHasRetries)
          aHandleOptions.put (IProcessorResenderModule.OPTION_RETRIES, Integer.toString (aSettings.getRetryCount ()));

        // Content-Transfer-Encoding is a partnership property
        aPartnership.setContentTransferEncodingSend (aRequest.getContentTransferEncoding ());
        aPartnership.setContentTransferEncodingReceive (aRequest.getContentTransferEncoding ());

        // And create a sender module that directly sends the message
        // The message processor registration is required for the resending
        // feature
        final AS2SenderModule aSender = m_aAS2SenderModuleFactory.get ();
        aSender.initDynamicComponent (aSession, null);
        // Set connect and read timeout
        aSender.attrs ().putIn (AbstractHttpSenderModule.ATTR_CONNECT_TIMEOUT, aSettings.getConnectTimeoutMS ());
        aSender.attrs ().putIn (AbstractHttpSenderModule.ATTR_READ_TIMEOUT, aSettings.getReadTimeoutMS ());
        // Register large file support from caller
        aSender.attrs ().putIn (MessageParameters.ATTR_LARGE_FILE_SUPPORT_ON, aSettings.isLargeFileSupport ());

        // Add all custom headers
        aMsg.headers ().setAllHeaders (aSettings.customHeaders ());

        aSession.getMessageProcessor ().addModule (aSender);

        aSender.handle (IProcessorSenderModule.DO_SEND, aMsg, aHandleOptions);
      }
      finally
      {
        aSession.getMessageProcessor ().stopActiveModules ();
      }
    }
    catch (final Throwable t)
    {
      LOGGER.error ("Error sending AS2 message", t);
      aResponse.setException (t);
    }
    finally
    {
      if (aMsg != null && aMsg.getMDN () != null)
      {
        // May be present, even in case of an exception
        aResponse.setMDN (aMsg.getMDN ());
      }
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Response retrieved: " + aResponse.getAsString ());

    aResponse.setExecutionDuration (aSW.stopAndGetDuration ());
    return aResponse;
  }
}
