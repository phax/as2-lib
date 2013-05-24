/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013 Philip Helger ph[at]phloc[dot]com
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
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import com.helger.as2lib.ISession;
import com.helger.as2lib.cert.CertificateNotFoundException;
import com.helger.as2lib.cert.ICertificateFactory;
import com.helger.as2lib.cert.KeyNotFoundException;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.AS2MessageMDN;
import com.helger.as2lib.message.CNetAttribute;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.params.AbstractParameterParser;
import com.helger.as2lib.params.MessageParameters;
import com.helger.as2lib.partner.CAS2Partnership;
import com.helger.as2lib.partner.CASXPartnership;
import com.helger.as2lib.partner.Partnership;

public class AS2Util
{
  private static final class SingletonHolder
  {
    static final BCCryptoHelper s_aInstance = new BCCryptoHelper ();
  }

  @Nonnull
  public static ICryptoHelper getCryptoHelper ()
  {
    return SingletonHolder.s_aInstance;
  }

  public static IMessageMDN createMDN (final ISession session,
                                       final AS2Message msg,
                                       final DispositionType disposition,
                                       final String text) throws Exception
  {
    final AS2MessageMDN mdn = new AS2MessageMDN (msg);
    mdn.setHeader (CAS2Header.AS2_VERSION, "1.1");
    // RFC2822 format: Wed, 04 Mar 2009 10:59:17 +0100
    mdn.setHeader ("Date", DateUtil.formatDate ("EEE, dd MMM yyyy HH:mm:ss Z"));
    mdn.setHeader ("Server", CInfo.NAME_VERSION);
    mdn.setHeader ("Mime-Version", "1.0");
    mdn.setHeader (CAS2Header.AS2_TO, msg.getPartnership ().getSenderID (CAS2Partnership.PID_AS2));
    mdn.setHeader (CAS2Header.AS2_FROM, msg.getPartnership ().getReceiverID (CAS2Partnership.PID_AS2));

    // get the MDN partnership info
    mdn.getPartnership ().setSenderID (CAS2Partnership.PID_AS2, mdn.getHeader (CAS2Header.AS2_FROM));
    mdn.getPartnership ().setReceiverID (CAS2Partnership.PID_AS2, mdn.getHeader (CAS2Header.AS2_TO));
    session.getPartnershipFactory ().updatePartnership (mdn, true);

    mdn.setHeader ("From", msg.getPartnership ().getReceiverID (Partnership.PID_EMAIL));
    final String subject = mdn.getPartnership ().getAttribute (CASXPartnership.PA_MDN_SUBJECT);

    if (subject != null)
    {
      mdn.setHeader ("Subject", AbstractParameterParser.parse (subject, new MessageParameters (msg)));
    }
    else
    {
      mdn.setHeader ("Subject", "Your Requested MDN Response");
    }
    mdn.setText (AbstractParameterParser.parse (text, new MessageParameters (msg)));
    mdn.setAttribute (AS2MessageMDN.MDNA_REPORTING_UA,
                      CInfo.NAME_VERSION +
                          "@" +
                          msg.getAttribute (CNetAttribute.MA_DESTINATION_IP) +
                          ":" +
                          msg.getAttribute (CNetAttribute.MA_DESTINATION_PORT));
    mdn.setAttribute (AS2MessageMDN.MDNA_ORIG_RECIPIENT, "rfc822; " + msg.getHeader (CAS2Header.AS2_TO));
    mdn.setAttribute (AS2MessageMDN.MDNA_FINAL_RECIPIENT,
                      "rfc822; " + msg.getPartnership ().getReceiverID (CAS2Partnership.PID_AS2));
    mdn.setAttribute (AS2MessageMDN.MDNA_ORIG_MESSAGEID, msg.getHeader ("Message-ID"));
    mdn.setAttribute (AS2MessageMDN.MDNA_DISPOSITION, disposition.toString ());

    final DispositionOptions dispOptions = new DispositionOptions (msg.getHeader ("Disposition-Notification-Options"));
    String mic = null;

    if (dispOptions.getMicalg () != null)
    {
      mic = getCryptoHelper ().calculateMIC (msg.getData (),
                                             dispOptions.getMicalg (),
                                             msg.getHistory ().getItemCount () > 1);
    }

    mdn.setAttribute (AS2MessageMDN.MDNA_MIC, mic);
    createMDNData (session, mdn, dispOptions.getMicalg (), dispOptions.getProtocol ());

    mdn.updateMessageID ();

    // store MDN into msg in case AsynchMDN is sent fails, needs to be resent by
    // send module
    msg.setMDN (mdn);

    return mdn;
  }

  public static void createMDNData (final ISession session,
                                    final IMessageMDN mdn,
                                    final String micAlg,
                                    final String signatureProtocol) throws Exception
  {
    // Create the report and sub-body parts
    final MimeMultipart reportParts = new MimeMultipart ();

    // Create the text part
    final MimeBodyPart textPart = new MimeBodyPart ();
    final String text = mdn.getText () + "\r\n";
    textPart.setContent (text, "text/plain");
    textPart.setHeader ("Content-Type", "text/plain");
    reportParts.addBodyPart (textPart);

    // Create the report part
    final MimeBodyPart reportPart = new MimeBodyPart ();
    final InternetHeaders reportValues = new InternetHeaders ();
    reportValues.setHeader ("Reporting-UA", mdn.getAttribute (AS2MessageMDN.MDNA_REPORTING_UA));
    reportValues.setHeader ("Original-Recipient", mdn.getAttribute (AS2MessageMDN.MDNA_ORIG_RECIPIENT));
    reportValues.setHeader ("Final-Recipient", mdn.getAttribute (AS2MessageMDN.MDNA_FINAL_RECIPIENT));
    reportValues.setHeader ("Original-Message-ID", mdn.getAttribute (AS2MessageMDN.MDNA_ORIG_MESSAGEID));
    reportValues.setHeader ("Disposition", mdn.getAttribute (AS2MessageMDN.MDNA_DISPOSITION));
    reportValues.setHeader ("Received-Content-MIC", mdn.getAttribute (AS2MessageMDN.MDNA_MIC));

    final Enumeration <?> reportEn = reportValues.getAllHeaderLines ();
    final StringBuilder reportData = new StringBuilder ();

    while (reportEn.hasMoreElements ())
    {
      reportData.append ((String) reportEn.nextElement ()).append ("\r\n");
    }

    reportData.append ("\r\n");

    final String reportText = reportData.toString ();
    reportPart.setContent (reportText, "message/disposition-notification");
    reportPart.setHeader ("Content-Type", "message/disposition-notification");
    reportParts.addBodyPart (reportPart);

    // Convert report parts to MimeBodyPart
    final MimeBodyPart report = new MimeBodyPart ();
    reportParts.setSubType ("report; report-type=disposition-notification");
    report.setContent (reportParts);
    report.setHeader ("Content-Type", reportParts.getContentType ());

    // Sign the data if needed
    if (signatureProtocol != null)
    {
      final ICertificateFactory certFx = session.getCertificateFactory ();

      try
      {
        final X509Certificate senderCert = certFx.getCertificate (mdn, Partnership.PTYPE_SENDER);
        final PrivateKey senderKey = certFx.getPrivateKey (mdn, senderCert);
        final MimeBodyPart signedReport = getCryptoHelper ().sign (report, senderCert, senderKey, micAlg);
        mdn.setData (signedReport);
      }
      catch (final CertificateNotFoundException cnfe)
      {
        cnfe.terminate ();
        mdn.setData (report);
      }
      catch (final KeyNotFoundException knfe)
      {
        knfe.terminate ();
        mdn.setData (report);
      }
    }
    else
    {
      mdn.setData (report);
    }

    // Update the MDN headers with content information
    final MimeBodyPart data = mdn.getData ();
    mdn.setHeader ("Content-Type", data.getContentType ());

    // int size = getSize(data);
    // mdn.setHeader("Content-Length", Integer.toString(size));
  }

  public static void parseMDN (final IMessage msg, final X509Certificate receiver) throws Exception
  {
    final IMessageMDN mdn = msg.getMDN ();
    MimeBodyPart mainPart = mdn.getData ();
    final ICryptoHelper ch = getCryptoHelper ();

    if (ch.isSigned (mainPart))
    {
      mainPart = ch.verify (mainPart, receiver);
    }

    final MimeMultipart reportParts = new MimeMultipart (mainPart.getDataHandler ().getDataSource ());
    final ContentType reportType = new ContentType (reportParts.getContentType ());

    if (reportType.getBaseType ().equalsIgnoreCase ("multipart/report"))
    {
      final int reportCount = reportParts.getCount ();
      MimeBodyPart reportPart;

      for (int j = 0; j < reportCount; j++)
      {
        reportPart = (MimeBodyPart) reportParts.getBodyPart (j);

        if (reportPart.isMimeType ("text/plain"))
        {
          mdn.setText (reportPart.getContent ().toString ());
        }
        else
          if (reportPart.isMimeType ("message/disposition-notification"))
          {
            final InternetHeaders disposition = new InternetHeaders (reportPart.getInputStream ());
            mdn.setAttribute (AS2MessageMDN.MDNA_REPORTING_UA, disposition.getHeader ("Reporting-UA", ", "));
            mdn.setAttribute (AS2MessageMDN.MDNA_ORIG_RECIPIENT, disposition.getHeader ("Original-Recipient", ", "));
            mdn.setAttribute (AS2MessageMDN.MDNA_FINAL_RECIPIENT, disposition.getHeader ("Final-Recipient", ", "));
            mdn.setAttribute (AS2MessageMDN.MDNA_ORIG_MESSAGEID, disposition.getHeader ("Original-Message-ID", ", "));
            mdn.setAttribute (AS2MessageMDN.MDNA_DISPOSITION, disposition.getHeader ("Disposition", ", "));
            mdn.setAttribute (AS2MessageMDN.MDNA_MIC, disposition.getHeader ("Received-Content-MIC", ", "));
          }
      }
    }
  }
}
