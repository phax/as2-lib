/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2014 Philip Helger philip[at]helger[dot]com
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

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.cert.CertificateExistsException;
import com.helger.as2lib.cert.PKCS12CertificateFactory;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.partner.CPartnershipIDs;
import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.partner.SelfFillingPartnershipFactory;
import com.helger.as2lib.processor.sender.AS2SenderModule;
import com.helger.as2lib.processor.sender.IProcessorSenderModule;
import com.helger.as2lib.session.AS2Session;
import com.helger.as2lib.util.StringMap;
import com.helger.commons.annotations.OverrideOnDemand;

/**
 * A simple client that allows for sending AS2 Messages and retrieving of
 * synchronous MDNs.
 *
 * @author Philip Helger
 */
public class AS2Client
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS2Client.class);

  public AS2Client ()
  {}

  @Nonnull
  @OverrideOnDemand
  @OverridingMethodsMustInvokeSuper
  protected Partnership buildPartnership (@Nonnull final AS2ClientSettings aSettings)
  {
    final Partnership aPartnership = new Partnership (aSettings.getPartnershipName ());

    aPartnership.setReceiverID (CPartnershipIDs.PID_AS2, aSettings.getReceiverAS2ID ());
    aPartnership.setReceiverID (CPartnershipIDs.PID_X509_ALIAS, aSettings.getReceiverKeyAlias ());

    aPartnership.setSenderID (CPartnershipIDs.PID_AS2, aSettings.getSenderAS2ID ());
    aPartnership.setSenderID (CPartnershipIDs.PID_X509_ALIAS, aSettings.getSenderKeyAlias ());
    aPartnership.setSenderID (CPartnershipIDs.PID_EMAIL, aSettings.getSenderEmailAddress ());

    aPartnership.setAttribute (CPartnershipIDs.PA_AS2_URL, aSettings.getDestinationAS2URL ());
    aPartnership.setAttribute (CPartnershipIDs.PA_ENCRYPT, aSettings.getCryptAlgoID ());
    aPartnership.setAttribute (CPartnershipIDs.PA_SIGN, aSettings.getSignAlgoID ());
    aPartnership.setAttribute (CPartnershipIDs.PA_PROTOCOL, AS2Message.PROTOCOL_AS2);
    // We want a sync MDN:
    aPartnership.setAttribute (CPartnershipIDs.PA_AS2_MDN_OPTIONS, aSettings.getMDNOptions ());
    if (false)
      aPartnership.setAttribute (CPartnershipIDs.PA_AS2_MDN_TO, "http://localhost:10080");
    // We don't want an async MDN:
    aPartnership.setAttribute (CPartnershipIDs.PA_AS2_RECEIPT_OPTION, null);
    aPartnership.setAttribute (CPartnershipIDs.PA_MESSAGEID_FORMAT, aSettings.getMessageIDFormat ());
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
  protected AS2Message createMessage (@Nonnull final Partnership aPartnership, @Nonnull final AS2ClientRequest aRequest) throws MessagingException,
                                                                                                                        OpenAS2Exception
  {
    final AS2Message aMsg = createAS2MessageObj ();
    aMsg.setContentType (aRequest.getContentType ());
    aMsg.setSubject (aRequest.getSubject ());
    aMsg.setPartnership (aPartnership);
    aMsg.setMessageID (aMsg.generateMessageID ());

    aMsg.setAttribute (CPartnershipIDs.PA_AS2_URL, aPartnership.getAttribute (CPartnershipIDs.PA_AS2_URL));
    aMsg.setAttribute (CPartnershipIDs.PID_AS2, aPartnership.getReceiverID (CPartnershipIDs.PID_AS2));
    aMsg.setAttribute (CPartnershipIDs.PID_EMAIL, aPartnership.getSenderID (CPartnershipIDs.PID_EMAIL));

    // Build message content
    final MimeBodyPart aPart = new MimeBodyPart ();
    aRequest.applyDataOntoMimeBodyPart (aPart);
    aMsg.setData (aPart);

    return aMsg;
  }

  @OverrideOnDemand
  protected void initCertificateFactory (@Nonnull final AS2ClientSettings aSettings, @Nonnull final AS2Session aSession) throws OpenAS2Exception
  {
    // Dynamically add certificate factory
    final StringMap aParams = new StringMap ();
    aParams.setAttribute (PKCS12CertificateFactory.ATTR_FILENAME, aSettings.getKeyStoreFile ().getAbsolutePath ());
    aParams.setAttribute (PKCS12CertificateFactory.ATTR_PASSWORD, aSettings.getKeyStorePassword ());

    final PKCS12CertificateFactory aCertFactory = new PKCS12CertificateFactory ();
    aCertFactory.initDynamicComponent (aSession, aParams);
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
  protected void initPartnershipFactory (final AS2Session aSession) throws OpenAS2Exception
  {
    // Use a self-filling in-memory partnership factory
    final SelfFillingPartnershipFactory aPartnershipFactory = new SelfFillingPartnershipFactory ();
    aSession.setPartnershipFactory (aPartnershipFactory);
  }

  @Nonnull
  @OverrideOnDemand
  protected AS2ClientResponse createResponse ()
  {
    return new AS2ClientResponse ();
  }

  @Nonnull
  @OverrideOnDemand
  protected AS2Session createSession ()
  {
    return new AS2Session ();
  }

  /**
   * @param aSettings
   *        Client settings
   * @param aSession
   *        Current session
   * @param aMsg
   *        Current message
   */
  @OverrideOnDemand
  protected void beforeSend (@Nonnull final AS2ClientSettings aSettings,
                             @Nonnull final AS2Session aSession,
                             @Nonnull final IMessage aMsg)
  {}

  @Nonnull
  public AS2ClientResponse sendSynchronous (@Nonnull final AS2ClientSettings aSettings,
                                            @Nonnull final AS2ClientRequest aRequest)
  {
    final AS2ClientResponse aResponse = createResponse ();
    IMessage aMsg = null;
    try
    {
      final Partnership aPartnership = buildPartnership (aSettings);

      aMsg = createMessage (aPartnership, aRequest);
      aResponse.setOriginalMessageID (aMsg.getMessageID ());

      if (false)
        s_aLogger.info ("msgId to send: " + aMsg.getMessageID ());

      // Start a new session
      final AS2Session aSession = createSession ();

      initCertificateFactory (aSettings, aSession);
      initPartnershipFactory (aSession);

      beforeSend (aSettings, aSession, aMsg);

      // And create a sender module that directly sends the message
      // No need for a message processor, as the sending is exactly one module
      final AS2SenderModule aSender = new AS2SenderModule ();
      aSender.initDynamicComponent (aSession, null);
      aSender.handle (IProcessorSenderModule.DO_SEND, aMsg, null);
    }
    catch (final Throwable t)
    {
      s_aLogger.error ("Error sending message", t);
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

    if (false)
      s_aLogger.info (aResponse.getAsString ());

    return aResponse;
  }
}
