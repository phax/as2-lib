/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2014 Philip Helger ph[at]phloc[dot]com
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

import com.helger.as2lib.ISession;
import com.helger.as2lib.cert.ECertificatePartnershipType;
import com.helger.as2lib.cert.ICertificateFactory;
import com.helger.as2lib.exception.CertificateNotFoundException;
import com.helger.as2lib.exception.KeyNotFoundException;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.AS2MessageMDN;
import com.helger.as2lib.message.CNetAttribute;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.params.AbstractParameterParser;
import com.helger.as2lib.params.MessageParameters;
import com.helger.as2lib.partner.CPartnershipIDs;
import com.helger.as2lib.partner.Partnership;
import com.helger.commons.mime.CMimeType;

@Immutable
public final class AS2Util
{
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

  private AS2Util ()
  {}

  @Nonnull
  public static ICryptoHelper getCryptoHelper ()
  {
    return SingletonHolder.s_aInstance;
  }

  @Nonnull
  public static IMessageMDN createMDN (@Nonnull final ISession aSession,
                                       @Nonnull final AS2Message aMsg,
                                       @Nonnull final DispositionType aDisposition,
                                       @Nonnull final String sText) throws Exception
  {
    final AS2MessageMDN aMdn = new AS2MessageMDN (aMsg);
    aMdn.setHeader (CAS2Header.HEADER_AS2_VERSION, CAS2Header.DEFAULT_AS2_VERSION);
    // RFC2822 format: Wed, 04 Mar 2009 10:59:17 +0100
    aMdn.setHeader (CAS2Header.HEADER_DATE, DateUtil.getFormattedDateNow (CAS2Header.DEFAULT_DATE_FORMAT));
    aMdn.setHeader (CAS2Header.HEADER_SERVER, CInfo.NAME_VERSION);
    aMdn.setHeader (CAS2Header.HEADER_MIME_VERSION, CAS2Header.DEFAULT_MIME_VERSION);
    aMdn.setHeader (CAS2Header.HEADER_AS2_TO, aMsg.getPartnership ().getSenderID (CPartnershipIDs.PID_AS2));
    aMdn.setHeader (CAS2Header.HEADER_AS2_FROM, aMsg.getPartnership ().getReceiverID (CPartnershipIDs.PID_AS2));

    // get the MDN partnership info
    aMdn.getPartnership ().setSenderID (CPartnershipIDs.PID_AS2, aMdn.getHeader (CAS2Header.HEADER_AS2_FROM));
    aMdn.getPartnership ().setReceiverID (CPartnershipIDs.PID_AS2, aMdn.getHeader (CAS2Header.HEADER_AS2_TO));
    aSession.getPartnershipFactory ().updatePartnership (aMdn, true);

    aMdn.setHeader (CAS2Header.HEADER_FROM, aMsg.getPartnership ().getReceiverID (Partnership.PID_EMAIL));
    final String sSubject = aMdn.getPartnership ().getAttribute (CPartnershipIDs.PA_MDN_SUBJECT);
    if (sSubject != null)
    {
      aMdn.setHeader (CAS2Header.HEADER_SUBJECT, AbstractParameterParser.parse (sSubject, new MessageParameters (aMsg)));
    }
    else
    {
      aMdn.setHeader (CAS2Header.HEADER_SUBJECT, "Your Requested MDN Response");
    }
    aMdn.setText (AbstractParameterParser.parse (sText, new MessageParameters (aMsg)));
    aMdn.setAttribute (AS2MessageMDN.MDNA_REPORTING_UA,
                       CInfo.NAME_VERSION +
                           "@" +
                           aMsg.getAttribute (CNetAttribute.MA_DESTINATION_IP) +
                           ":" +
                           aMsg.getAttribute (CNetAttribute.MA_DESTINATION_PORT));
    aMdn.setAttribute (AS2MessageMDN.MDNA_ORIG_RECIPIENT, "rfc822; " + aMsg.getHeader (CAS2Header.HEADER_AS2_TO));
    aMdn.setAttribute (AS2MessageMDN.MDNA_FINAL_RECIPIENT,
                       "rfc822; " + aMsg.getPartnership ().getReceiverID (CPartnershipIDs.PID_AS2));
    aMdn.setAttribute (AS2MessageMDN.MDNA_ORIG_MESSAGEID, aMsg.getHeader (CAS2Header.HEADER_MESSAGE_ID));
    aMdn.setAttribute (AS2MessageMDN.MDNA_DISPOSITION, aDisposition.toString ());

    final DispositionOptions aDispOptions = new DispositionOptions (aMsg.getHeader (CAS2Header.HEADER_DISPOSITION_NOTIFICATION_OPTIONS));
    String sMIC = null;
    if (aDispOptions.getMICAlg () != null)
    {
      sMIC = getCryptoHelper ().calculateMIC (aMsg.getData (),
                                              aDispOptions.getMICAlg (),
                                              aMsg.getHistory ().getItemCount () > 1);
    }

    aMdn.setAttribute (AS2MessageMDN.MDNA_MIC, sMIC);
    createMDNData (aSession, aMdn, aDispOptions.getMICAlg (), aDispOptions.getProtocol ());

    aMdn.updateMessageID ();

    // store MDN into msg in case AsynchMDN is sent fails, needs to be resent by
    // send module
    aMsg.setMDN (aMdn);

    return aMdn;
  }

  public static void createMDNData (@Nonnull final ISession aSession,
                                    @Nonnull final IMessageMDN aMdn,
                                    @Nonnull final String sMicAlg,
                                    @Nullable final String sSignatureProtocol) throws Exception
  {
    // Create the report and sub-body parts
    final MimeMultipart aReportParts = new MimeMultipart ();

    // Create the text part
    final MimeBodyPart aTextPart = new MimeBodyPart ();
    final String sText = aMdn.getText () + "\r\n";
    aTextPart.setContent (sText, CMimeType.TEXT_PLAIN.getAsString ());
    aTextPart.setHeader (CAS2Header.HEADER_CONTENT_TYPE, CMimeType.TEXT_PLAIN.getAsString ());
    aReportParts.addBodyPart (aTextPart);

    // Create the report part
    final MimeBodyPart aReportPart = new MimeBodyPart ();
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
    {
      aReportData.append ((String) aReportEn.nextElement ()).append ("\r\n");
    }
    aReportData.append ("\r\n");

    final String sReportText = aReportData.toString ();
    aReportPart.setContent (sReportText, "message/disposition-notification");
    aReportPart.setHeader (CAS2Header.HEADER_CONTENT_TYPE, "message/disposition-notification");
    aReportParts.addBodyPart (aReportPart);

    // Convert report parts to MimeBodyPart
    final MimeBodyPart aReport = new MimeBodyPart ();
    aReportParts.setSubType ("report; report-type=disposition-notification");
    aReport.setContent (aReportParts);
    aReport.setHeader (CAS2Header.HEADER_CONTENT_TYPE, aReportParts.getContentType ());

    // Sign the data if needed
    if (sSignatureProtocol != null)
    {
      final ICertificateFactory aCertFactory = aSession.getCertificateFactory ();
      try
      {
        final X509Certificate aSenderCert = aCertFactory.getCertificate (aMdn, ECertificatePartnershipType.SENDER);
        final PrivateKey aSenderKey = aCertFactory.getPrivateKey (aMdn, aSenderCert);
        final MimeBodyPart aSignedReport = getCryptoHelper ().sign (aReport, aSenderCert, aSenderKey, sMicAlg);
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
      aMdn.setData (aReport);
    }

    // Update the MDN headers with content information
    final MimeBodyPart aData = aMdn.getData ();
    aMdn.setHeader (CAS2Header.HEADER_CONTENT_TYPE, aData.getContentType ());

    // final int size = getSize (aData);
    // aMdn.setHeader (CAS2Header.HEADER_CONTENT_LENGTH, Integer.toString
    // (size));
  }

  public static void parseMDN (final IMessage aMsg, final X509Certificate aReceiver) throws Exception
  {
    final IMessageMDN aMdn = aMsg.getMDN ();
    MimeBodyPart aMainPart = aMdn.getData ();
    final ICryptoHelper aCryptoHelper = getCryptoHelper ();

    if (aCryptoHelper.isSigned (aMainPart))
    {
      aMainPart = aCryptoHelper.verify (aMainPart, aReceiver);
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
      }
    }
  }
}
