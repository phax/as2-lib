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
package com.helger.as2lib.processor.sender;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.mail.internet.MimeBodyPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.cert.ICertificateFactory;
import com.helger.as2lib.exception.DispositionException;
import com.helger.as2lib.exception.HttpResponseException;
import com.helger.as2lib.exception.InvalidParameterException;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedException;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.AS2MessageMDN;
import com.helger.as2lib.message.CFileAttribute;
import com.helger.as2lib.message.CNetAttribute;
import com.helger.as2lib.message.DataHistoryItem;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.partner.CPartnershipIDs;
import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.processor.storage.IProcessorStorageModule;
import com.helger.as2lib.util.AS2Util;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.DateUtil;
import com.helger.as2lib.util.DispositionOptions;
import com.helger.as2lib.util.DispositionType;
import com.helger.as2lib.util.IOUtil;
import com.phloc.commons.io.streams.NonBlockingByteArrayOutputStream;
import com.phloc.commons.io.streams.StreamUtils;
import com.phloc.commons.string.StringParser;
import com.phloc.commons.timing.StopWatch;

public class AS2SenderModule extends AbstractHttpSenderModule
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS2SenderModule.class);

  public boolean canHandle (@Nonnull final String sAction, final IMessage aMsg, final Map <String, Object> aOptions)
  {
    if (!sAction.equals (IProcessorSenderModule.DO_SEND))
      return false;
    return aMsg instanceof AS2Message;
  }

  public void handle (final String sAction, @Nonnull final IMessage aMsg, final Map <String, Object> aOptions) throws OpenAS2Exception
  {
    s_aLogger.info ("message submitted" + aMsg.getLoggingText ());
    if (!(aMsg instanceof AS2Message))
      throw new OpenAS2Exception ("Can't send non-AS2 message");

    // verify all required information is present for sending
    checkRequired (aMsg);

    final int nRetries = retries (aOptions);

    try
    {
      // encrypt and/or sign the message if needed
      final MimeBodyPart aSecuredData = secure (aMsg);
      aMsg.setContentType (aSecuredData.getContentType ());

      // Create the HTTP connection and set up headers
      final String sUrl = aMsg.getPartnership ().getAttribute (CPartnershipIDs.PA_AS2_URL);
      final HttpURLConnection aConn = getConnection (sUrl, true, true, false, "POST");
      try
      {
        updateHttpHeaders (aConn, aMsg);
        aMsg.setAttribute (CNetAttribute.MA_DESTINATION_IP, aConn.getURL ().getHost ());
        aMsg.setAttribute (CNetAttribute.MA_DESTINATION_PORT, Integer.toString (aConn.getURL ().getPort ()));
        final DispositionOptions aDispOptions = new DispositionOptions (aConn.getRequestProperty ("Disposition-Notification-Options"));

        // Calculate and get the original mic
        final boolean bIncludeHeaders = aMsg.getHistory ().getItemCount () > 1;

        final String sMIC = AS2Util.getCryptoHelper ().calculateMIC (aMsg.getData (),
                                                                     aDispOptions.getMICAlg (),
                                                                     bIncludeHeaders);

        if (aMsg.getPartnership ().getAttribute (CPartnershipIDs.PA_AS2_RECEIPT_OPTION) != null)
        {
          // if yes : PA_AS2_RECEIPT_OPTION) != null
          // then keep the original mic & message id.
          // then wait for the another HTTP call by receivers

          storePendingInfo ((AS2Message) aMsg, sMIC);
        }

        s_aLogger.info ("connecting to " + sUrl + aMsg.getLoggingText ());

        // Note: closing this stream causes connection abort errors on some AS2
        // servers
        final OutputStream aMsgOS = aConn.getOutputStream ();

        // Transfer the data
        final InputStream aMsgIS = aSecuredData.getInputStream ();

        final StopWatch aSW = new StopWatch (true);
        final long nBytes = IOUtil.copy (aMsgIS, aMsgOS);
        aSW.stop ();
        s_aLogger.info ("transferred " + IOUtil.getTransferRate (nBytes, aSW) + aMsg.getLoggingText ());

        // Check the HTTP Response code
        final int nResponseCode = aConn.getResponseCode ();
        if (nResponseCode != HttpURLConnection.HTTP_OK &&
            nResponseCode != HttpURLConnection.HTTP_CREATED &&
            nResponseCode != HttpURLConnection.HTTP_ACCEPTED &&
            nResponseCode != HttpURLConnection.HTTP_PARTIAL &&
            nResponseCode != HttpURLConnection.HTTP_NO_CONTENT)
        {
          s_aLogger.error ("Error url " + sUrl + " rc " + nResponseCode + " rm " + aConn.getResponseMessage ());
          throw new HttpResponseException (sUrl, nResponseCode, aConn.getResponseMessage ());
        }

        // Asynch MDN 2007-03-12
        // Receive an MDN
        try
        {
          // Receive an MDN
          if (aMsg.isRequestingMDN ())
          {
            // Check if the AsyncMDN is required
            if (aMsg.getPartnership ().getAttribute (CPartnershipIDs.PA_AS2_RECEIPT_OPTION) == null)
            {
              // go ahead to receive sync MDN
              receiveMDN ((AS2Message) aMsg, aConn, sMIC);
              s_aLogger.info ("message sent" + aMsg.getLoggingText ());
            }
          }

        }
        catch (final DispositionException ex)
        {
          // If a disposition error hasn't been handled, the message transfer
          // was not successful
          throw ex;
        }
        catch (final OpenAS2Exception ex)
        {
          // Don't resend or fail, just log an error if one occurs while
          // receiving the MDN
          s_aLogger.error (OpenAS2Exception.SOURCE_MESSAGE, ex);
          final OpenAS2Exception oae2 = new OpenAS2Exception ("Message was sent but an error occured while receiving the MDN");
          oae2.initCause (ex);
          oae2.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
          oae2.terminate ();
        }
      }
      finally
      {
        aConn.disconnect ();
      }
    }
    catch (final HttpResponseException ex)
    {
      // Resend if the HTTP Response has an error code
      s_aLogger.error ("error hre " + ex.getMessage ());
      ex.terminate ();
      _resend (aMsg, ex, nRetries);
    }
    catch (final IOException ex)
    {
      // Resend if a network error occurs during transmission
      final WrappedException wioe = new WrappedException (ex);
      wioe.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      wioe.terminate ();

      _resend (aMsg, wioe, nRetries);
    }
    catch (final Exception ex)
    {
      // Propagate error if it can't be handled by a resend
      throw new WrappedException (ex);
    }
  }

  // Asynch MDN 2007-03-12
  // added originalmic

  /**
   * @param aMsg
   *        AS2Message
   * @param aConn
   *        URLConnection
   * @param sOriginalMIC
   *        mic value from original msg
   */
  protected void receiveMDN (final AS2Message aMsg, final HttpURLConnection aConn, final String sOriginalMIC) throws OpenAS2Exception,
                                                                                                             IOException
  {
    try
    {
      // Create a MessageMDN and copy HTTP headers
      final IMessageMDN aMdn = new AS2MessageMDN (aMsg);
      copyHttpHeaders (aConn, aMdn.getHeaders ());

      // Receive the MDN data
      final InputStream aConnIn = aConn.getInputStream ();
      final NonBlockingByteArrayOutputStream aMdnStream = new NonBlockingByteArrayOutputStream ();

      // Retrieve the message content
      final long nContentLength = StringParser.parseLong (aMdn.getHeader ("Content-Length"), -1);
      if (nContentLength >= 0)
        StreamUtils.copyInputStreamToOutputStreamWithLimit (aConnIn, aMdnStream, nContentLength);
      else
        StreamUtils.copyInputStreamToOutputStream (aConnIn, aMdnStream);

      final MimeBodyPart aPart = new MimeBodyPart (aMdn.getHeaders (), aMdnStream.toByteArray ());
      aMsg.getMDN ().setData (aPart);

      // get the MDN partnership info
      aMdn.getPartnership ().setSenderID (CPartnershipIDs.PID_AS2, aMdn.getHeader (CAS2Header.AS2_FROM));
      aMdn.getPartnership ().setReceiverID (CPartnershipIDs.PID_AS2, aMdn.getHeader (CAS2Header.AS2_TO));
      getSession ().getPartnershipFactory ().updatePartnership (aMdn, false);

      final ICertificateFactory aCertFactory = getSession ().getCertificateFactory ();
      final X509Certificate aSenderCert = aCertFactory.getCertificate (aMdn, Partnership.PARTNERSHIP_TYPE_SENDER);

      AS2Util.parseMDN (aMsg, aSenderCert);

      getSession ().getProcessor ().handle (IProcessorStorageModule.DO_STOREMDN, aMsg, null);

      final String sDisposition = aMsg.getMDN ().getAttribute (AS2MessageMDN.MDNA_DISPOSITION);

      s_aLogger.info ("received MDN [" + sDisposition + "]" + aMsg.getLoggingText ());

      // Asynch MDN 2007-03-12
      // Verify if the original mic is equal to the mic in returned MDN
      final String sReturnMIC = aMsg.getMDN ().getAttribute (AS2MessageMDN.MDNA_MIC);

      if (!sReturnMIC.replaceAll (" ", "").equals (sOriginalMIC.replaceAll (" ", "")))
      {
        // file was sent completely but the returned mic was not matched,
        // don't know it needs or needs not to be resent ? it's depended on
        // what!
        // anyway, just log the warning message here.
        s_aLogger.info ("mic is not matched, original mic: " +
                        sOriginalMIC +
                        " return mic: " +
                        sReturnMIC +
                        aMsg.getLoggingText ());
      }
      else
      {
        s_aLogger.info ("mic is matched, mic: " + sReturnMIC + aMsg.getLoggingText ());
      }

      try
      {
        new DispositionType (sDisposition).validate ();
      }
      catch (final DispositionException ex)
      {
        ex.setText (aMsg.getMDN ().getText ());

        if (ex.getDisposition () != null && ex.getDisposition ().isWarning ())
        {
          ex.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
          ex.terminate ();
        }
        else
        {
          throw ex;
        }
      }
    }
    catch (final IOException ex)
    {
      throw ex;
    }
    catch (final Exception ex)
    {
      final WrappedException we = new WrappedException (ex);
      we.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      throw we;
    }
  }

  protected void checkRequired (@Nonnull final IMessage aMsg) throws InvalidParameterException
  {
    final Partnership aPartnership = aMsg.getPartnership ();

    try
    {
      InvalidParameterException.checkValue (aMsg, "ContentType", aMsg.getContentType ());
      InvalidParameterException.checkValue (aMsg,
                                            "Attribute: " + CPartnershipIDs.PA_AS2_URL,
                                            aPartnership.getAttribute (CPartnershipIDs.PA_AS2_URL));
      InvalidParameterException.checkValue (aMsg,
                                            "Receiver: " + CPartnershipIDs.PID_AS2,
                                            aPartnership.getReceiverID (CPartnershipIDs.PID_AS2));
      InvalidParameterException.checkValue (aMsg,
                                            "Sender: " + CPartnershipIDs.PID_AS2,
                                            aPartnership.getSenderID (CPartnershipIDs.PID_AS2));
      InvalidParameterException.checkValue (aMsg, "Subject", aMsg.getSubject ());
      InvalidParameterException.checkValue (aMsg,
                                            "Sender: " + Partnership.PID_EMAIL,
                                            aPartnership.getSenderID (Partnership.PID_EMAIL));
      InvalidParameterException.checkValue (aMsg, "Message Data", aMsg.getData ());
    }
    catch (final InvalidParameterException ex)
    {
      ex.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      throw ex;
    }
  }

  private void _resend (@Nonnull final IMessage aMsg, final OpenAS2Exception aCause, final int nTries) throws OpenAS2Exception
  {
    if (!doResend (IProcessorSenderModule.DO_SEND, aMsg, aCause, nTries))
    {
      // Oh dear, we've run out of retries, do something interesting.
      // TODO create a fake failure MDN
      s_aLogger.error ("Message abandoned" + aMsg.getLoggingText ());
    }
  }

  // Returns a MimeBodyPart or MimeMultipart object
  protected MimeBodyPart secure (@Nonnull final IMessage aMsg) throws Exception
  {
    // Set up encrypt/sign variables
    MimeBodyPart aDataBP = aMsg.getData ();

    final Partnership aPartnership = aMsg.getPartnership ();
    final boolean bEncrypt = aPartnership.getAttribute (CPartnershipIDs.PA_ENCRYPT) != null;
    final boolean bSign = aPartnership.getAttribute (CPartnershipIDs.PA_SIGN) != null;

    // Encrypt and/or sign the data if requested
    if (bEncrypt || bSign)
    {
      final ICertificateFactory aCertFactory = getSession ().getCertificateFactory ();

      // Sign the data if requested
      if (bSign)
      {
        final X509Certificate aSenderCert = aCertFactory.getCertificate (aMsg, Partnership.PARTNERSHIP_TYPE_SENDER);
        final PrivateKey aSenderKey = aCertFactory.getPrivateKey (aMsg, aSenderCert);
        final String sAlgorithm = aPartnership.getAttribute (CPartnershipIDs.PA_SIGN);

        aDataBP = AS2Util.getCryptoHelper ().sign (aDataBP, aSenderCert, aSenderKey, sAlgorithm);

        // Asynch MDN 2007-03-12
        final DataHistoryItem aHistoryItem = new DataHistoryItem (aDataBP.getContentType ());
        // *** add one more item to msg history
        aMsg.getHistory ().addItem (aHistoryItem);

        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("signed data" + aMsg.getLoggingText ());
      }

      // Encrypt the data if requested
      if (bEncrypt)
      {
        final String sAlgorithm = aPartnership.getAttribute (CPartnershipIDs.PA_ENCRYPT);

        final X509Certificate aReceiverCert = aCertFactory.getCertificate (aMsg, Partnership.PARTNERSHIP_TYPE_RECEIVER);
        aDataBP = AS2Util.getCryptoHelper ().encrypt (aDataBP, aReceiverCert, sAlgorithm);

        // Asynch MDN 2007-03-12
        final DataHistoryItem aHistoryItem = new DataHistoryItem (aDataBP.getContentType ());
        // *** add one more item to msg history
        aMsg.getHistory ().addItem (aHistoryItem);

        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("encrypted data" + aMsg.getLoggingText ());
      }
    }

    return aDataBP;
  }

  protected void updateHttpHeaders (@Nonnull final HttpURLConnection aConn, @Nonnull final IMessage aMsg)
  {
    final Partnership aPartnership = aMsg.getPartnership ();

    aConn.setRequestProperty ("Connection", "close, TE");
    aConn.setRequestProperty ("User-Agent", "OpenAS2 AS2Sender");

    aConn.setRequestProperty ("Date", DateUtil.getFormattedDateNow ("EEE, dd MMM yyyy HH:mm:ss Z"));
    aConn.setRequestProperty ("Message-ID", aMsg.getMessageID ());
    // make sure this is the encoding used in the msg, run TBF1
    aConn.setRequestProperty ("Mime-Version", "1.0");
    aConn.setRequestProperty ("Content-type", aMsg.getContentType ());
    aConn.setRequestProperty (CAS2Header.AS2_VERSION, "1.1");
    aConn.setRequestProperty ("Recipient-Address", aPartnership.getAttribute (CPartnershipIDs.PA_AS2_URL));
    aConn.setRequestProperty (CAS2Header.AS2_TO, aPartnership.getReceiverID (CPartnershipIDs.PID_AS2));
    aConn.setRequestProperty (CAS2Header.AS2_FROM, aPartnership.getSenderID (CPartnershipIDs.PID_AS2));
    aConn.setRequestProperty ("Subject", aMsg.getSubject ());
    aConn.setRequestProperty ("From", aPartnership.getSenderID (Partnership.PID_EMAIL));

    final String sDispTo = aPartnership.getAttribute (CPartnershipIDs.PA_AS2_MDN_TO);
    if (sDispTo != null)
      aConn.setRequestProperty ("Disposition-Notification-To", sDispTo);

    final String sDispOptions = aPartnership.getAttribute (CPartnershipIDs.PA_AS2_MDN_OPTIONS);
    if (sDispOptions != null)
      aConn.setRequestProperty ("Disposition-Notification-Options", sDispOptions);

    // Asynch MDN 2007-03-12
    final String sReceiptOption = aPartnership.getAttribute (CPartnershipIDs.PA_AS2_RECEIPT_OPTION);
    if (sReceiptOption != null)
      aConn.setRequestProperty ("Receipt-delivery-option", sReceiptOption);

    // As of 2007-06-01
    final String sContentDisp = aMsg.getContentDisposition ();
    if (sContentDisp != null)
      aConn.setRequestProperty ("Content-Disposition", sContentDisp);
  }

  // Asynch MDN 2007-03-12
  /**
   * for storing original mic & outgoing file into pending information file
   * 
   * @param aMsg
   *        AS2Message
   * @param sMIC
   * @throws WrappedException
   */
  protected void storePendingInfo (final AS2Message aMsg, final String sMIC) throws WrappedException
  {
    try
    {
      final String pendingFolder = getSession ().getComponent ("processor").getParameterNotRequired ("pendingmdninfo");

      final FileOutputStream aFOS = new FileOutputStream (pendingFolder +
                                                          "/" +
                                                          aMsg.getMessageID ().substring (1,
                                                                                          aMsg.getMessageID ()
                                                                                              .length () - 1));
      aFOS.write ((sMIC + "\n").getBytes ());

      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Original MIC is : " + sMIC + aMsg.getLoggingText ());

      // input pending folder & original outgoing file name to get and
      // unique file name
      // in order to avoid file overwritting.
      final String sPendingFile = getSession ().getComponent ("processor").getParameterNotRequired ("pendingmdn") +
                                  "/" +
                                  aMsg.getMessageID ().substring (1, aMsg.getMessageID ().length () - 1);

      s_aLogger.info ("Save Original mic & message id. information into folder : " +
                      sPendingFile +
                      aMsg.getLoggingText ());
      aFOS.write (sPendingFile.getBytes ());
      aFOS.close ();
      aMsg.setAttribute (CFileAttribute.MA_PENDINGFILE, sPendingFile);
      aMsg.setAttribute (CFileAttribute.MA_STATUS, CFileAttribute.MA_PENDING);
    }
    catch (final Exception ex)
    {
      final WrappedException we = new WrappedException (ex);
      we.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      throw we;
    }
  }
}
