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
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedException;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.AS2MessageMDN;
import com.helger.as2lib.message.CFileAttribute;
import com.helger.as2lib.message.CNetAttribute;
import com.helger.as2lib.message.DataHistoryItem;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.params.InvalidParameterException;
import com.helger.as2lib.partner.CAS2Partnership;
import com.helger.as2lib.partner.CSecurePartnership;
import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.processor.storage.IProcessorStorageModule;
import com.helger.as2lib.util.AS2Util;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.DateUtil;
import com.helger.as2lib.util.DispositionException;
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

  public boolean canHandle (final String action, final IMessage msg, final Map <String, Object> options)
  {
    if (!action.equals (IProcessorSenderModule.DO_SEND))
      return false;
    return msg instanceof AS2Message;
  }

  public void handle (final String action, @Nonnull final IMessage aMsg, final Map <String, Object> options) throws OpenAS2Exception
  {
    s_aLogger.info ("message submitted" + aMsg.getLoggingText ());

    if (!(aMsg instanceof AS2Message))
      throw new OpenAS2Exception ("Can't send non-AS2 message");

    // verify all required information is present for sending
    checkRequired (aMsg);

    final int nRetries = retries (options);

    try
    {
      // encrypt and/or sign the message if needed
      final MimeBodyPart securedData = secure (aMsg);
      aMsg.setContentType (securedData.getContentType ());

      // Create the HTTP connection and set up headers
      final String url = aMsg.getPartnership ().getAttribute (CAS2Partnership.PA_AS2_URL);
      final HttpURLConnection conn = getConnection (url, true, true, false, "POST");
      try
      {
        updateHttpHeaders (conn, aMsg);
        aMsg.setAttribute (CNetAttribute.MA_DESTINATION_IP, conn.getURL ().getHost ());
        aMsg.setAttribute (CNetAttribute.MA_DESTINATION_PORT, Integer.toString (conn.getURL ().getPort ()));
        final DispositionOptions dispOptions = new DispositionOptions (conn.getRequestProperty ("Disposition-Notification-Options"));

        // Calculate and get the original mic
        final boolean bIncludeHeaders = aMsg.getHistory ().getItemCount () > 1;

        final String mic = AS2Util.getCryptoHelper ().calculateMIC (aMsg.getData (),
                                                                    dispOptions.getMicalg (),
                                                                    bIncludeHeaders);

        if (aMsg.getPartnership ().getAttribute (CAS2Partnership.PA_AS2_RECEIPT_OPTION) != null)
        {
          // if yes : PA_AS2_RECEIPT_OPTION) != null
          // then keep the original mic & message id.
          // then wait for the another HTTP call by receivers

          storePendingInfo ((AS2Message) aMsg, mic);
        }

        s_aLogger.info ("connecting to " + url + aMsg.getLoggingText ());

        // Note: closing this stream causes connection abort errors on some AS2
        // servers
        final OutputStream messageOut = conn.getOutputStream ();

        // Transfer the data
        final InputStream messageIn = securedData.getInputStream ();

        final StopWatch aSW = new StopWatch (true);
        final long bytes = IOUtil.copy (messageIn, messageOut);
        aSW.stop ();
        s_aLogger.info ("transferred " + IOUtil.getTransferRate (bytes, aSW) + aMsg.getLoggingText ());

        // Check the HTTP Response code
        final int nResponseCode = conn.getResponseCode ();
        if (nResponseCode != HttpURLConnection.HTTP_OK &&
            nResponseCode != HttpURLConnection.HTTP_CREATED &&
            nResponseCode != HttpURLConnection.HTTP_ACCEPTED &&
            nResponseCode != HttpURLConnection.HTTP_PARTIAL &&
            nResponseCode != HttpURLConnection.HTTP_NO_CONTENT)
        {
          s_aLogger.error ("error url " +
                           url.toString () +
                           " rc " +
                           nResponseCode +
                           " rm " +
                           conn.getResponseMessage ());
          throw new HttpResponseException (url.toString (), nResponseCode, conn.getResponseMessage ());
        }

        // Asynch MDN 2007-03-12
        // Receive an MDN
        try
        {
          // Receive an MDN
          if (aMsg.isRequestingMDN ())
          {

            // Check if the AsyncMDN is required
            if (aMsg.getPartnership ().getAttribute (CAS2Partnership.PA_AS2_RECEIPT_OPTION) == null)
            {
              // go ahead to receive sync MDN
              receiveMDN ((AS2Message) aMsg, conn, mic);
              s_aLogger.info ("message sent" + aMsg.getLoggingText ());
            }
          }

        }
        catch (final DispositionException de)
        {
          // If a disposition error hasn't been handled, the message transfer
          // was not successful
          throw de;
        }
        catch (final OpenAS2Exception oae)
        {
          // Don't resend or fail, just log an error if one occurs while
          // receiving the MDN
          s_aLogger.error (OpenAS2Exception.SOURCE_MESSAGE, oae);
          final OpenAS2Exception oae2 = new OpenAS2Exception ("Message was sent but an error occured while receiving the MDN");
          oae2.initCause (oae);
          oae2.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
          oae2.terminate ();
        }

      }
      finally
      {
        conn.disconnect ();
      }

    }
    catch (final HttpResponseException hre)
    {
      // Resend if the HTTP Response has an error code
      s_aLogger.error ("error hre " + hre.getMessage ());
      hre.terminate ();
      _resend (aMsg, hre, nRetries);
    }
    catch (final IOException ioe)
    {
      // Resend if a network error occurs during transmission
      final WrappedException wioe = new WrappedException (ioe);
      wioe.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      wioe.terminate ();

      _resend (aMsg, wioe, nRetries);
    }
    catch (final Exception e)
    {
      // Propagate error if it can't be handled by a resend
      throw new WrappedException (e);
    }
  }

  // Asynch MDN 2007-03-12
  // added originalmic

  /**
   * @param msg
   *        AS2Message
   * @param conn
   *        URLConnection
   * @param originalmic
   *        mic value from original msg
   */
  protected void receiveMDN (final AS2Message msg, final HttpURLConnection conn, final String originalmic) throws OpenAS2Exception,
                                                                                                          IOException
  {
    try
    {
      // Create a MessageMDN and copy HTTP headers
      final IMessageMDN mdn = new AS2MessageMDN (msg);
      copyHttpHeaders (conn, mdn.getHeaders ());

      // Receive the MDN data
      final InputStream connIn = conn.getInputStream ();
      final NonBlockingByteArrayOutputStream mdnStream = new NonBlockingByteArrayOutputStream ();

      // Retrieve the message content
      final long nContentLength = StringParser.parseLong (mdn.getHeader ("Content-Length"), -1);
      if (nContentLength >= 0)
        IOUtil.copy (connIn, mdnStream, nContentLength);
      else
        StreamUtils.copyInputStreamToOutputStream (connIn, mdnStream);

      final MimeBodyPart part = new MimeBodyPart (mdn.getHeaders (), mdnStream.toByteArray ());
      msg.getMDN ().setData (part);

      // get the MDN partnership info
      mdn.getPartnership ().setSenderID (CAS2Partnership.PID_AS2, mdn.getHeader (CAS2Header.AS2_FROM));
      mdn.getPartnership ().setReceiverID (CAS2Partnership.PID_AS2, mdn.getHeader (CAS2Header.AS2_TO));
      getSession ().getPartnershipFactory ().updatePartnership (mdn, false);

      final ICertificateFactory cFx = getSession ().getCertificateFactory ();
      final X509Certificate senderCert = cFx.getCertificate (mdn, Partnership.PTYPE_SENDER);

      AS2Util.parseMDN (msg, senderCert);

      getSession ().getProcessor ().handle (IProcessorStorageModule.DO_STOREMDN, msg, null);

      final String disposition = msg.getMDN ().getAttribute (AS2MessageMDN.MDNA_DISPOSITION);

      s_aLogger.info ("received MDN [" + disposition + "]" + msg.getLoggingText ());

      // Asynch MDN 2007-03-12
      // Verify if the original mic is equal to the mic in returned MDN
      final String returnmic = msg.getMDN ().getAttribute (AS2MessageMDN.MDNA_MIC);

      if (!returnmic.replaceAll (" ", "").equals (originalmic.replaceAll (" ", "")))
      {
        // file was sent completely but the returned mic was not matched,
        // don't know it needs or needs not to be resent ? it's depended on
        // what!
        // anyway, just log the warning message here.
        s_aLogger.info ("mic is not matched, original mic: " +
                        originalmic +
                        " return mic: " +
                        returnmic +
                        msg.getLoggingText ());
      }
      else
      {
        s_aLogger.info ("mic is matched, mic: " + returnmic + msg.getLoggingText ());
      }

      try
      {
        new DispositionType (disposition).validate ();
      }
      catch (final DispositionException de)
      {
        de.setText (msg.getMDN ().getText ());

        if (de.getDisposition () != null && de.getDisposition ().isWarning ())
        {
          de.addSource (OpenAS2Exception.SOURCE_MESSAGE, msg);
          de.terminate ();
        }
        else
        {
          throw de;
        }
      }
    }
    catch (final IOException ex)
    {
      throw ex;
    }
    catch (final Exception e)
    {
      final WrappedException we = new WrappedException (e);
      we.addSource (OpenAS2Exception.SOURCE_MESSAGE, msg);
      throw we;
    }
  }

  protected void checkRequired (final IMessage msg) throws InvalidParameterException
  {
    final Partnership partnership = msg.getPartnership ();

    try
    {
      InvalidParameterException.checkValue (msg, "ContentType", msg.getContentType ());
      InvalidParameterException.checkValue (msg,
                                            "Attribute: " + CAS2Partnership.PA_AS2_URL,
                                            partnership.getAttribute (CAS2Partnership.PA_AS2_URL));
      InvalidParameterException.checkValue (msg,
                                            "Receiver: " + CAS2Partnership.PID_AS2,
                                            partnership.getReceiverID (CAS2Partnership.PID_AS2));
      InvalidParameterException.checkValue (msg,
                                            "Sender: " + CAS2Partnership.PID_AS2,
                                            partnership.getSenderID (CAS2Partnership.PID_AS2));
      InvalidParameterException.checkValue (msg, "Subject", msg.getSubject ());
      InvalidParameterException.checkValue (msg,
                                            "Sender: " + Partnership.PID_EMAIL,
                                            partnership.getSenderID (Partnership.PID_EMAIL));
      InvalidParameterException.checkValue (msg, "Message Data", msg.getData ());
    }
    catch (final InvalidParameterException rpe)
    {
      rpe.addSource (OpenAS2Exception.SOURCE_MESSAGE, msg);
      throw rpe;
    }
  }

  private void _resend (final IMessage msg, final OpenAS2Exception cause, final int tries) throws OpenAS2Exception
  {
    if (!doResend (IProcessorSenderModule.DO_SEND, msg, cause, tries))
    {
      // Oh dear, we've run out of reetries, do something interesting.
      // TODO create a fake failure MDN
      s_aLogger.info ("Message abandoned" + msg.getLoggingText ());
    }
  }

  // Returns a MimeBodyPart or MimeMultipart object
  protected MimeBodyPart secure (final IMessage msg) throws Exception
  {
    // Set up encrypt/sign variables
    MimeBodyPart aDataBP = msg.getData ();

    final Partnership partnership = msg.getPartnership ();
    final boolean encrypt = partnership.getAttribute (CSecurePartnership.PA_ENCRYPT) != null;
    final boolean sign = partnership.getAttribute (CSecurePartnership.PA_SIGN) != null;

    // Encrypt and/or sign the data if requested
    if (encrypt || sign)
    {
      final ICertificateFactory certFx = getSession ().getCertificateFactory ();

      // Sign the data if requested
      if (sign)
      {
        final X509Certificate senderCert = certFx.getCertificate (msg, Partnership.PTYPE_SENDER);
        final PrivateKey senderKey = certFx.getPrivateKey (msg, senderCert);
        final String sAlgorithm = partnership.getAttribute (CSecurePartnership.PA_SIGN);

        aDataBP = AS2Util.getCryptoHelper ().sign (aDataBP, senderCert, senderKey, sAlgorithm);

        // Asynch MDN 2007-03-12
        final DataHistoryItem historyItem = new DataHistoryItem (aDataBP.getContentType ());
        // *** add one more item to msg history
        msg.getHistory ().addItem (historyItem);

        s_aLogger.debug ("signed data" + msg.getLoggingText ());
      }

      // Encrypt the data if requested
      if (encrypt)
      {
        final String sAlgorithm = partnership.getAttribute (CSecurePartnership.PA_ENCRYPT);

        final X509Certificate receiverCert = certFx.getCertificate (msg, Partnership.PTYPE_RECEIVER);
        aDataBP = AS2Util.getCryptoHelper ().encrypt (aDataBP, receiverCert, sAlgorithm);

        // Asynch MDN 2007-03-12
        final DataHistoryItem historyItem = new DataHistoryItem (aDataBP.getContentType ());
        // *** add one more item to msg history
        msg.getHistory ().addItem (historyItem);

        s_aLogger.debug ("encrypted data" + msg.getLoggingText ());
      }
    }

    return aDataBP;
  }

  protected void updateHttpHeaders (@Nonnull final HttpURLConnection conn, @Nonnull final IMessage msg)
  {
    final Partnership partnership = msg.getPartnership ();

    conn.setRequestProperty ("Connection", "close, TE");
    conn.setRequestProperty ("User-Agent", "OpenAS2 AS2Sender");

    conn.setRequestProperty ("Date", DateUtil.formatDate ("EEE, dd MMM yyyy HH:mm:ss Z"));
    conn.setRequestProperty ("Message-ID", msg.getMessageID ());
    // make sure this is the encoding used in the msg, run TBF1
    conn.setRequestProperty ("Mime-Version", "1.0");
    conn.setRequestProperty ("Content-type", msg.getContentType ());
    conn.setRequestProperty (CAS2Header.AS2_VERSION, "1.1");
    conn.setRequestProperty ("Recipient-Address", partnership.getAttribute (CAS2Partnership.PA_AS2_URL));
    conn.setRequestProperty (CAS2Header.AS2_TO, partnership.getReceiverID (CAS2Partnership.PID_AS2));
    conn.setRequestProperty (CAS2Header.AS2_FROM, partnership.getSenderID (CAS2Partnership.PID_AS2));
    conn.setRequestProperty ("Subject", msg.getSubject ());
    conn.setRequestProperty ("From", partnership.getSenderID (Partnership.PID_EMAIL));

    final String dispTo = partnership.getAttribute (CAS2Partnership.PA_AS2_MDN_TO);
    if (dispTo != null)
      conn.setRequestProperty ("Disposition-Notification-To", dispTo);

    final String dispOptions = partnership.getAttribute (CAS2Partnership.PA_AS2_MDN_OPTIONS);
    if (dispOptions != null)
      conn.setRequestProperty ("Disposition-Notification-Options", dispOptions);

    // Asynch MDN 2007-03-12
    final String receiptOption = partnership.getAttribute (CAS2Partnership.PA_AS2_RECEIPT_OPTION);
    if (receiptOption != null)
      conn.setRequestProperty ("Receipt-delivery-option", receiptOption);

    // As of 2007-06-01
    final String contentDisp = msg.getContentDisposition ();
    if (contentDisp != null)
      conn.setRequestProperty ("Content-Disposition", contentDisp);
  }

  // Asynch MDN 2007-03-12
  /**
   * for storing original mic & outgoing file into pending information file
   * 
   * @param msg
   *        AS2Message
   * @param mic
   * @throws WrappedException
   */
  protected void storePendingInfo (final AS2Message msg, final String mic) throws WrappedException
  {
    try
    {

      final String pendingFolder = getSession ().getComponent ("processor").getParameters ().get ("pendingmdninfo");

      final FileOutputStream fos = new FileOutputStream (pendingFolder +
                                                         "/" +
                                                         msg.getMessageID ()
                                                            .substring (1, msg.getMessageID ().length () - 1));
      fos.write ((mic + "\n").getBytes ());
      s_aLogger.debug ("Original MIC is : " + mic + msg.getLoggingText ());

      // input pending folder & original outgoing file name to get and
      // unique file name
      // in order to avoid file overwritting.
      final String pendingFile = getSession ().getComponent ("processor").getParameters ().get ("pendingmdn") +
                                 "/" +
                                 msg.getMessageID ().substring (1, msg.getMessageID ().length () - 1);

      s_aLogger.info ("Save Original mic & message id. information into folder : " +
                      pendingFile +
                      msg.getLoggingText ());
      fos.write (pendingFile.getBytes ());
      fos.close ();
      msg.setAttribute (CFileAttribute.MA_PENDINGFILE, pendingFile);
      msg.setAttribute (CFileAttribute.MA_STATUS, CFileAttribute.MA_PENDING);

    }
    catch (final Exception e)
    {

      final WrappedException we = new WrappedException (e);
      we.addSource (OpenAS2Exception.SOURCE_MESSAGE, msg);
      throw we;

    }
  }

}
