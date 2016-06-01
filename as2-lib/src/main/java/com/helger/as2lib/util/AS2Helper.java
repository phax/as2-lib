/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2016 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.util;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.CAS2Info;
import com.helger.as2lib.cert.CertificateNotFoundException;
import com.helger.as2lib.cert.ECertificatePartnershipType;
import com.helger.as2lib.cert.ICertificateFactory;
import com.helger.as2lib.cert.KeyNotFoundException;
import com.helger.as2lib.crypto.BCCryptoHelper;
import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.as2lib.crypto.ICryptoHelper;
import com.helger.as2lib.disposition.DispositionOptions;
import com.helger.as2lib.disposition.DispositionType;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.AS2MessageMDN;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.params.MessageParameters;
import com.helger.as2lib.partner.PartnershipNotFoundException;
import com.helger.as2lib.processor.CNetAttribute;
import com.helger.as2lib.session.IAS2Session;
import com.helger.as2lib.util.http.HTTPHelper;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.state.ETriState;

@Immutable
public final class AS2Helper
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS2Helper.class);
  private static final String HEADER_RECEIVED_CONTENT_MIC = "Received-Content-MIC";
  private static final String HEADER_DISPOSITION = "Disposition";
  private static final String HEADER_ORIGINAL_MESSAGE_ID = "Original-Message-ID";
  private static final String HEADER_FINAL_RECIPIENT = "Final-Recipient";
  private static final String HEADER_ORIGINAL_RECIPIENT = "Original-Recipient";
  private static final String HEADER_REPORTING_UA = "Reporting-UA";

  private static final class SingletonHolder
  {
    static final BCCryptoHelper s_aInstance = new BCCryptoHelper ();
  }

  private AS2Helper ()
  {}

  @Nonnull
  public static ICryptoHelper getCryptoHelper ()
  {
    return SingletonHolder.s_aInstance;
  }

  /**
   * Create and fill the Mdn parameter
   *
   * @param aSession
   *        Session to retrieve the certificate factory for signing
   * @param aMdn
   *        The MDN object to be filled
   * @param bSignMDN
   *        <code>true</code> to sign the MDN
   * @param bIncludeCertificateInSignedContent
   *        <code>true</code> if the passed certificate should be part of the
   *        signed content, <code>false</code> if the certificate should not be
   *        put in the content. E.g. for PEPPOL this must be <code>true</code>.
   * @param eMICAlg
   *        The MIC algorithm to be used. Must be present if bSignMDN is
   *        <code>true</code>.
   * @param bUseOldRFC3851MicAlgs
   *        <code>true</code> to use the old RFC 3851 MIC algorithm names (e.g.
   *        <code>sha1</code>), <code>false</code> to use the new RFC 5751 MIC
   *        algorithm names (e.g. <code>sha-1</code>).
   * @throws Exception
   *         In case something internally goes wrong
   */
  public static void createMDNData (@Nonnull final IAS2Session aSession,
                                    @Nonnull final IMessageMDN aMdn,
                                    final boolean bSignMDN,
                                    final boolean bIncludeCertificateInSignedContent,
                                    @Nullable final ECryptoAlgorithmSign eMICAlg,
                                    final boolean bUseOldRFC3851MicAlgs) throws Exception
  {
    ValueEnforcer.notNull (aSession, "AS2Session");
    ValueEnforcer.notNull (aMdn, "MDN");
    if (bSignMDN)
      ValueEnforcer.notNull (eMICAlg, "MICAlg");

    // Create the report and sub-body parts
    final MimeMultipart aReportParts = new MimeMultipart ();

    // Create the text part
    final MimeBodyPart aTextPart = new MimeBodyPart ();
    final String sText = aMdn.getText () + HTTPHelper.EOL;
    aTextPart.setContent (sText, CMimeType.TEXT_PLAIN.getAsString ());
    aTextPart.setHeader (CAS2Header.HEADER_CONTENT_TYPE, CMimeType.TEXT_PLAIN.getAsString ());
    aReportParts.addBodyPart (aTextPart);

    // Create the report part
    final MimeBodyPart aReportPart = new MimeBodyPart ();
    {
      final InternetHeaders aReportValues = new InternetHeaders ();
      aReportValues.setHeader (HEADER_REPORTING_UA, aMdn.getAttribute (AS2MessageMDN.MDNA_REPORTING_UA));
      aReportValues.setHeader (HEADER_ORIGINAL_RECIPIENT, aMdn.getAttribute (AS2MessageMDN.MDNA_ORIG_RECIPIENT));
      aReportValues.setHeader (HEADER_FINAL_RECIPIENT, aMdn.getAttribute (AS2MessageMDN.MDNA_FINAL_RECIPIENT));
      aReportValues.setHeader (HEADER_ORIGINAL_MESSAGE_ID, aMdn.getAttribute (AS2MessageMDN.MDNA_ORIG_MESSAGEID));
      aReportValues.setHeader (HEADER_DISPOSITION, aMdn.getAttribute (AS2MessageMDN.MDNA_DISPOSITION));
      aReportValues.setHeader (HEADER_RECEIVED_CONTENT_MIC, aMdn.getAttribute (AS2MessageMDN.MDNA_MIC));

      final Enumeration <?> aReportEn = aReportValues.getAllHeaderLines ();
      final StringBuilder aReportData = new StringBuilder ();
      while (aReportEn.hasMoreElements ())
        aReportData.append ((String) aReportEn.nextElement ()).append (HTTPHelper.EOL);
      aReportData.append (HTTPHelper.EOL);
      aReportPart.setContent (aReportData.toString (), "message/disposition-notification");
    }

    aReportPart.setHeader (CAS2Header.HEADER_CONTENT_TYPE, "message/disposition-notification");
    aReportParts.addBodyPart (aReportPart);

    // Convert report parts to MimeBodyPart
    final MimeBodyPart aReport = new MimeBodyPart ();
    aReportParts.setSubType ("report; report-type=disposition-notification");
    aReport.setContent (aReportParts);
    aReport.setHeader (CAS2Header.HEADER_CONTENT_TYPE, aReportParts.getContentType ());

    // Sign the MDN data if needed
    if (bSignMDN)
    {
      final ICertificateFactory aCertFactory = aSession.getCertificateFactory ();
      try
      {
        final X509Certificate aSenderCert = aCertFactory.getCertificate (aMdn, ECertificatePartnershipType.SENDER);
        final PrivateKey aSenderKey = aCertFactory.getPrivateKey (aMdn, aSenderCert);
        final MimeBodyPart aSignedReport = getCryptoHelper ().sign (aReport,
                                                                    aSenderCert,
                                                                    aSenderKey,
                                                                    eMICAlg,
                                                                    bIncludeCertificateInSignedContent,
                                                                    bUseOldRFC3851MicAlgs);
        aMdn.setData (aSignedReport);
      }
      catch (final CertificateNotFoundException ex)
      {
        ex.terminate ();
        aMdn.setData (aReport);
      }
      catch (final KeyNotFoundException ex)
      {
        ex.terminate ();
        aMdn.setData (aReport);
      }
    }
    else
    {
      // No signing needed
      aMdn.setData (aReport);
    }

    // Update the MDN headers with content information
    final MimeBodyPart aData = aMdn.getData ();
    aMdn.setHeader (CAS2Header.HEADER_CONTENT_TYPE, aData.getContentType ());

    // final int size = getSize (aData);
    // aMdn.setHeader (CAS2Header.HEADER_CONTENT_LENGTH, Integer.toString
    // (size));
  }

  /**
   * Create a new MDN
   *
   * @param aSession
   *        AS2 session to be used. May not be <code>null</code>.
   * @param aMsg
   *        The source AS2 message for which the MDN is to be created. May not
   *        be <code>null</code>.
   * @param aDisposition
   *        The disposition - either success or error. May not be
   *        <code>null</code>.
   * @param sText
   *        The text to be send. May not be <code>null</code>.
   * @return The created MDN object which is already attached to the passed
   *         source AS2 message.
   * @throws Exception
   *         In case of an error
   */
  @Nonnull
  public static IMessageMDN createMDN (@Nonnull final IAS2Session aSession,
                                       @Nonnull final AS2Message aMsg,
                                       @Nonnull final DispositionType aDisposition,
                                       @Nonnull final String sText) throws Exception
  {
    ValueEnforcer.notNull (aSession, "AS2Session");
    ValueEnforcer.notNull (aMsg, "AS2Message");
    ValueEnforcer.notNull (aDisposition, "Disposition");
    ValueEnforcer.notNull (sText, "Text");

    final AS2MessageMDN aMDN = new AS2MessageMDN (aMsg);
    aMDN.setHeader (CAS2Header.HEADER_AS2_VERSION, CAS2Header.DEFAULT_AS2_VERSION);
    aMDN.setHeader (CAS2Header.HEADER_DATE, DateHelper.getFormattedDateNow (CAS2Header.DEFAULT_DATE_FORMAT));
    aMDN.setHeader (CAS2Header.HEADER_SERVER, CAS2Info.NAME_VERSION);
    aMDN.setHeader (CAS2Header.HEADER_MIME_VERSION, CAS2Header.DEFAULT_MIME_VERSION);
    aMDN.setHeader (CAS2Header.HEADER_AS2_FROM, aMsg.getPartnership ().getReceiverAS2ID ());
    aMDN.setHeader (CAS2Header.HEADER_AS2_TO, aMsg.getPartnership ().getSenderAS2ID ());

    // get the MDN partnership info
    aMDN.getPartnership ().setSenderAS2ID (aMDN.getHeader (CAS2Header.HEADER_AS2_FROM));
    aMDN.getPartnership ().setReceiverAS2ID (aMDN.getHeader (CAS2Header.HEADER_AS2_TO));
    // Set the appropriate keystore aliases
    aMDN.getPartnership ().setSenderX509Alias (aMsg.getPartnership ().getReceiverX509Alias ());
    aMDN.getPartnership ().setReceiverX509Alias (aMsg.getPartnership ().getSenderX509Alias ());
    // Update the partnership
    try
    {
      aSession.getPartnershipFactory ().updatePartnership (aMDN, true);
    }
    catch (final PartnershipNotFoundException ex)
    {
      // This would block sending an MDN in case a PartnershipNotFoundException
      // was the reason for sending the MDN :)
    }

    aMDN.setHeader (CAS2Header.HEADER_FROM, aMsg.getPartnership ().getReceiverEmail ());
    final String sSubject = aMDN.getPartnership ().getMDNSubject ();
    if (sSubject != null)
    {
      aMDN.setHeader (CAS2Header.HEADER_SUBJECT, new MessageParameters (aMsg).format (sSubject));
    }
    else
    {
      aMDN.setHeader (CAS2Header.HEADER_SUBJECT, "Your Requested MDN Response");
    }
    aMDN.setText (new MessageParameters (aMsg).format (sText));
    aMDN.setAttribute (AS2MessageMDN.MDNA_REPORTING_UA, CAS2Info.NAME_VERSION +
                                                        "@" +
                                                        aMsg.getAttribute (CNetAttribute.MA_DESTINATION_IP) +
                                                        ":" +
                                                        aMsg.getAttribute (CNetAttribute.MA_DESTINATION_PORT));
    aMDN.setAttribute (AS2MessageMDN.MDNA_ORIG_RECIPIENT, "rfc822; " + aMsg.getHeader (CAS2Header.HEADER_AS2_TO));
    aMDN.setAttribute (AS2MessageMDN.MDNA_FINAL_RECIPIENT, "rfc822; " + aMsg.getPartnership ().getReceiverAS2ID ());
    aMDN.setAttribute (AS2MessageMDN.MDNA_ORIG_MESSAGEID, aMsg.getHeader (CAS2Header.HEADER_MESSAGE_ID));
    aMDN.setAttribute (AS2MessageMDN.MDNA_DISPOSITION, aDisposition.getAsString ());

    final String sDispositionOptions = aMsg.getHeader (CAS2Header.HEADER_DISPOSITION_NOTIFICATION_OPTIONS);
    final DispositionOptions aDispositionOptions = DispositionOptions.createFromString (sDispositionOptions);
    String sMIC = null;
    if (aDispositionOptions.getMICAlgCount () > 0)
    {
      // If the source message was signed or encrypted, include the headers -
      // see message sending for details
      final boolean bIncludeHeadersInMIC = aMsg.getPartnership ().getSigningAlgorithm () != null ||
                                           aMsg.getPartnership ().getEncryptAlgorithm () != null ||
                                           aMsg.getPartnership ().getCompressionType () != null;

      sMIC = getCryptoHelper ().calculateMIC (aMsg.getData (),
                                              aDispositionOptions.getFirstMICAlg (),
                                              bIncludeHeadersInMIC);
    }
    aMDN.setAttribute (AS2MessageMDN.MDNA_MIC, sMIC);

    boolean bSignMDN = false;
    boolean bIncludeCertificateInSignedContent = false;
    if (aDispositionOptions.getProtocol () != null)
    {
      if (aDispositionOptions.isProtocolRequired () || aDispositionOptions.hasMICAlg ())
      {
        // Sign if required or if optional and a MIC algorithm is present
        bSignMDN = true;

        // Include certificate in signed content?
        final ETriState eIncludeCertificateInSignedContent = aMsg.getPartnership ()
                                                                 .getIncludeCertificateInSignedContent ();
        if (eIncludeCertificateInSignedContent.isDefined ())
        {
          // Use per partnership
          bIncludeCertificateInSignedContent = eIncludeCertificateInSignedContent.getAsBooleanValue ();
        }
        else
        {
          // Use global value
          bIncludeCertificateInSignedContent = aSession.isCryptoSignIncludeCertificateInBodyPart ();
        }
      }
    }

    final boolean bUseOldRFC3851MicAlgs = aMsg.getPartnership ().isRFC3851MICAlgs ();

    createMDNData (aSession,
                   aMDN,
                   bSignMDN,
                   bIncludeCertificateInSignedContent,
                   aDispositionOptions.getFirstMICAlg (),
                   bUseOldRFC3851MicAlgs);

    aMDN.updateMessageID ();

    // store MDN into msg in case AsynchMDN is sent fails, needs to be resent by
    // send module
    aMsg.setMDN (aMDN);

    return aMDN;
  }

  public static void parseMDN (@Nonnull final IMessage aMsg,
                               @Nonnull final X509Certificate aReceiverCert,
                               final boolean bUseCertificateInBodyPart) throws Exception
  {
    s_aLogger.info ("Start parsing MDN of" + aMsg.getLoggingText ());

    final IMessageMDN aMdn = aMsg.getMDN ();
    MimeBodyPart aMainPart = aMdn.getData ();
    final ICryptoHelper aCryptoHelper = getCryptoHelper ();

    final boolean bDisableVerify = aMsg.getPartnership ().isDisableVerify ();
    final boolean bMsgIsSigned = aCryptoHelper.isSigned (aMainPart);
    final boolean bForceVerify = aMsg.getPartnership ().isForceVerify ();
    if (bMsgIsSigned && bDisableVerify)
    {
      s_aLogger.info ("Message claims to be signed but signature validation is disabled" + aMsg.getLoggingText ());
    }
    else
      if (bMsgIsSigned || bForceVerify)
      {
        if (bForceVerify && !bMsgIsSigned)
          s_aLogger.info ("Forced verify MDN signature" + aMsg.getLoggingText ());
        else
          if (s_aLogger.isDebugEnabled ())
            s_aLogger.debug ("Verifying MDN signature" + aMsg.getLoggingText ());

        aMainPart = aCryptoHelper.verify (aMainPart, aReceiverCert, bUseCertificateInBodyPart, bForceVerify);
        // Remember that message was signed and verified
        aMdn.setAttribute (AS2Message.ATTRIBUTE_RECEIVED_SIGNED, Boolean.TRUE.toString ());
        s_aLogger.info ("Successfully verified signature of MDN of message" + aMsg.getLoggingText ());
      }

    final MimeMultipart aReportParts = new MimeMultipart (aMainPart.getDataHandler ().getDataSource ());
    final ContentType aReportType = new ContentType (aReportParts.getContentType ());

    if (aReportType.getBaseType ().equalsIgnoreCase ("multipart/report"))
    {
      final int nReportCount = aReportParts.getCount ();
      for (int j = 0; j < nReportCount; j++)
      {
        final MimeBodyPart aReportPart = (MimeBodyPart) aReportParts.getBodyPart (j);
        if (aReportPart.isMimeType (CMimeType.TEXT_PLAIN.getAsString ()))
        {
          // XXX is this "toString" really a correct solution?
          aMdn.setText (aReportPart.getContent ().toString ());
        }
        else
          if (aReportPart.isMimeType ("message/disposition-notification"))
          {
            final InternetHeaders aDisposition = new InternetHeaders (aReportPart.getInputStream ());
            aMdn.setAttribute (AS2MessageMDN.MDNA_REPORTING_UA, aDisposition.getHeader (HEADER_REPORTING_UA, ", "));
            aMdn.setAttribute (AS2MessageMDN.MDNA_ORIG_RECIPIENT,
                               aDisposition.getHeader (HEADER_ORIGINAL_RECIPIENT, ", "));
            aMdn.setAttribute (AS2MessageMDN.MDNA_FINAL_RECIPIENT,
                               aDisposition.getHeader (HEADER_FINAL_RECIPIENT, ", "));
            aMdn.setAttribute (AS2MessageMDN.MDNA_ORIG_MESSAGEID,
                               aDisposition.getHeader (HEADER_ORIGINAL_MESSAGE_ID, ", "));
            aMdn.setAttribute (AS2MessageMDN.MDNA_DISPOSITION, aDisposition.getHeader (HEADER_DISPOSITION, ", "));
            aMdn.setAttribute (AS2MessageMDN.MDNA_MIC, aDisposition.getHeader (HEADER_RECEIVED_CONTENT_MIC, ", "));
          }
          else
            s_aLogger.info ("Got unsupported MDN body part MIME type: " + aReportPart.getContentType ());
      }
    }
  }
}
