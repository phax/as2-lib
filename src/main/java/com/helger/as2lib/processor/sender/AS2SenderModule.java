/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2015 Philip Helger philip[at]helger[dot]com
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.bouncycastle.mail.smime.SMIMECompressedGenerator;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.operator.OutputCompressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.cert.ECertificatePartnershipType;
import com.helger.as2lib.cert.ICertificateFactory;
import com.helger.as2lib.crypto.ECryptoAlgorithmCrypt;
import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.as2lib.disposition.DispositionException;
import com.helger.as2lib.disposition.DispositionOptions;
import com.helger.as2lib.disposition.DispositionType;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.AS2MessageMDN;
import com.helger.as2lib.message.DataHistoryItem;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.params.InvalidParameterException;
import com.helger.as2lib.partner.CPartnershipIDs;
import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.processor.CFileAttribute;
import com.helger.as2lib.processor.CNetAttribute;
import com.helger.as2lib.processor.storage.IProcessorStorageModule;
import com.helger.as2lib.session.ComponentNotFoundException;
import com.helger.as2lib.util.AS2Helper;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.DateHelper;
import com.helger.as2lib.util.IOHelper;
import com.helger.as2lib.util.http.HTTPHelper;
import com.helger.commons.charset.CCharset;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.string.StringParser;
import com.helger.commons.timing.StopWatch;

public class AS2SenderModule extends AbstractHttpSenderModule
{
  private static final String ATTR_PENDINGMDNINFO = "pendingmdninfo";
  private static final String ATTR_PENDINGMDN = "pendingmdn";
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS2SenderModule.class);

  public AS2SenderModule ()
  {}

  public boolean canHandle (@Nonnull final String sAction,
                            @Nonnull final IMessage aMsg,
                            @Nullable final Map <String, Object> aOptions)
  {
    return IProcessorSenderModule.DO_SEND.equals (sAction) && aMsg instanceof AS2Message;
  }

  protected void checkRequired (@Nonnull final AS2Message aMsg) throws InvalidParameterException
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
                                            "Sender: " + CPartnershipIDs.PID_EMAIL,
                                            aPartnership.getSenderID (CPartnershipIDs.PID_EMAIL));
      InvalidParameterException.checkValue (aMsg, "Message Data", aMsg.getData ());
    }
    catch (final InvalidParameterException ex)
    {
      ex.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      throw ex;
    }
  }

  public void handle (@Nonnull final String sAction,
                      @Nonnull final IMessage aMsg,
                      @Nullable final Map <String, Object> aOptions) throws OpenAS2Exception
  {
    final AS2Message aRealMsg = (AS2Message) aMsg;
    s_aLogger.info ("message submitted: " + aRealMsg.getLoggingText ());

    // verify all required information is present for sending
    checkRequired (aRealMsg);

    final int nRetries = getRetries (aOptions);

    try
    {
      // encrypt and/or sign the message if needed
      final MimeBodyPart aSecuredData = secure (aRealMsg);

      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Setting message content type to '" + aSecuredData.getContentType () + "'");

      aRealMsg.setContentType (aSecuredData.getContentType ());

      // Create the HTTP connection and set up headers
      final String sUrl = aRealMsg.getPartnership ().getAttribute (CPartnershipIDs.PA_AS2_URL);

      final HttpURLConnection aConn = getConnection (sUrl, true, true, false, "POST", getSession ().getHttpProxy ());
      try
      {
        updateHttpHeaders (aConn, aRealMsg);

        aRealMsg.setAttribute (CNetAttribute.MA_DESTINATION_IP, aConn.getURL ().getHost ());
        aRealMsg.setAttribute (CNetAttribute.MA_DESTINATION_PORT, Integer.toString (aConn.getURL ().getPort ()));

        final String sDispositionOptions = aConn.getRequestProperty (CAS2Header.HEADER_DISPOSITION_NOTIFICATION_OPTIONS);
        final DispositionOptions aDispositionOptions = DispositionOptions.createFromString (sDispositionOptions);

        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("DispositionOptions=" + aDispositionOptions);

        // Calculate and get the original mic
        final boolean bIncludeHeadersInMIC = aRealMsg.getHistory ().getItemCount () > 1;

        final String sMIC = AS2Helper.getCryptoHelper ().calculateMIC (aRealMsg.getData (),
                                                                       aDispositionOptions.getFirstMICAlg (),
                                                                       bIncludeHeadersInMIC);

        if (aRealMsg.getPartnership ().getAttribute (CPartnershipIDs.PA_AS2_RECEIPT_OPTION) != null)
        {
          // if yes : PA_AS2_RECEIPT_OPTION != null
          // then keep the original mic & message id.
          // then wait for the another HTTP call by receivers
          storePendingInfo (aRealMsg, sMIC);
        }

        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Calculated MIC: '" + sMIC + "'");

        s_aLogger.info ("Connecting to " + sUrl + aRealMsg.getLoggingText ());

        // Note: closing this stream causes connection abort errors on some AS2
        // servers
        final OutputStream aMsgOS = aConn.getOutputStream ();

        // Transfer the data
        final InputStream aMsgIS = aSecuredData.getInputStream ();

        final StopWatch aSW = StopWatch.createdStarted ();
        final long nBytes = IOHelper.copy (aMsgIS, aMsgOS);
        aSW.stop ();
        s_aLogger.info ("transferred " + IOHelper.getTransferRate (nBytes, aSW) + aRealMsg.getLoggingText ());

        // Check the HTTP Response code
        final int nResponseCode = aConn.getResponseCode ();
        if (nResponseCode != HttpURLConnection.HTTP_OK &&
            nResponseCode != HttpURLConnection.HTTP_CREATED &&
            nResponseCode != HttpURLConnection.HTTP_ACCEPTED &&
            nResponseCode != HttpURLConnection.HTTP_PARTIAL &&
            nResponseCode != HttpURLConnection.HTTP_NO_CONTENT)
        {
          s_aLogger.error ("Error URL '" + sUrl + "' - HTTP " + nResponseCode + " " + aConn.getResponseMessage ());
          throw new HttpResponseException (sUrl, nResponseCode, aConn.getResponseMessage ());
        }

        // Asynch MDN 2007-03-12
        // Receive an MDN
        try
        {
          // Receive an MDN
          if (aRealMsg.isRequestingMDN ())
          {
            // Check if the AsyncMDN is required
            if (aRealMsg.getPartnership ().getAttribute (CPartnershipIDs.PA_AS2_RECEIPT_OPTION) == null)
            {
              // go ahead to receive sync MDN
              receiveMDN (aRealMsg, aConn, sMIC);
              s_aLogger.info ("message sent" + aRealMsg.getLoggingText ());
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
          final OpenAS2Exception oae2 = new OpenAS2Exception ("Message was sent but an error occured while receiving the MDN");
          oae2.initCause (ex);
          oae2.addSource (OpenAS2Exception.SOURCE_MESSAGE, aRealMsg);
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
      s_aLogger.error ("Http Response Error " + ex.getMessage ());
      ex.terminate ();
      _resend (aRealMsg, ex, nRetries);
    }
    catch (final IOException ex)
    {
      // Resend if a network error occurs during transmission
      final OpenAS2Exception wioe = WrappedOpenAS2Exception.wrap (ex);
      wioe.addSource (OpenAS2Exception.SOURCE_MESSAGE, aRealMsg);
      wioe.terminate ();

      _resend (aRealMsg, wioe, nRetries);
    }
    catch (final Exception ex)
    {
      // Propagate error if it can't be handled by a resend
      throw WrappedOpenAS2Exception.wrap (ex);
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
   * @throws OpenAS2Exception
   *         in case of an error
   * @throws IOException
   *         in case of an IO error
   */
  protected void receiveMDN (@Nonnull final AS2Message aMsg,
                             @Nonnull final HttpURLConnection aConn,
                             @Nonnull final String sOriginalMIC) throws OpenAS2Exception, IOException
  {
    try
    {
      // Create a MessageMDN and copy HTTP headers
      final IMessageMDN aMDN = new AS2MessageMDN (aMsg);
      HTTPHelper.copyHttpHeaders (aConn, aMDN.getHeaders ());

      // Receive the MDN data
      final InputStream aConnIS = aConn.getInputStream ();
      final NonBlockingByteArrayOutputStream aMDNStream = new NonBlockingByteArrayOutputStream ();
      try
      {
        // Retrieve the whole MDN content
        final long nContentLength = StringParser.parseLong (aMDN.getHeader (CAS2Header.HEADER_CONTENT_LENGTH), -1);
        if (nContentLength >= 0)
          StreamHelper.copyInputStreamToOutputStreamWithLimit (aConnIS, aMDNStream, nContentLength);
        else
          StreamHelper.copyInputStreamToOutputStream (aConnIS, aMDNStream);
      }
      finally
      {
        StreamHelper.close (aMDNStream);
      }

      if (false)
      {
        // Debug print the whole MDN stream
        System.out.println (aMDNStream.getAsString (CCharset.CHARSET_ISO_8859_1_OBJ));
      }

      final MimeBodyPart aPart = new MimeBodyPart (aMDN.getHeaders (), aMDNStream.toByteArray ());
      aMsg.getMDN ().setData (aPart);

      // get the MDN partnership info
      aMDN.getPartnership ().setSenderID (CPartnershipIDs.PID_AS2, aMDN.getHeader (CAS2Header.HEADER_AS2_FROM));
      aMDN.getPartnership ().setReceiverID (CPartnershipIDs.PID_AS2, aMDN.getHeader (CAS2Header.HEADER_AS2_TO));
      getSession ().getPartnershipFactory ().updatePartnership (aMDN, false);

      final ICertificateFactory aCertFactory = getSession ().getCertificateFactory ();
      final X509Certificate aSenderCert = aCertFactory.getCertificate (aMDN, ECertificatePartnershipType.SENDER);

      AS2Helper.parseMDN (aMsg, aSenderCert, getSession ().isCryptoVerifyUseCertificateInBodyPart ());

      try
      {
        getSession ().getMessageProcessor ().handle (IProcessorStorageModule.DO_STOREMDN, aMsg, null);
      }
      catch (final ComponentNotFoundException ex)
      {
        // No processor found
      }

      final String sDisposition = aMsg.getMDN ().getAttribute (AS2MessageMDN.MDNA_DISPOSITION);

      s_aLogger.info ("received MDN [" + sDisposition + "]" + aMsg.getLoggingText ());

      // Asynch MDN 2007-03-12
      // Verify if the original mic is equal to the mic in returned MDN
      final String sReturnMIC = aMsg.getMDN ().getAttribute (AS2MessageMDN.MDNA_MIC);

      // Catch ReturnMIC == null in case the attribute is simply missing
      if (sReturnMIC == null || !sReturnMIC.replaceAll (" ", "").equals (sOriginalMIC.replaceAll (" ", "")))
      {
        // file was sent completely but the returned mic was not matched,
        // don't know it needs or needs not to be resent ? it's depended on
        // what!
        // anyway, just log the warning message here.
        s_aLogger.info ("MIC IS NOT MATCHED, original mic: '" +
                        sOriginalMIC +
                        "' return mic: '" +
                        sReturnMIC +
                        "'" +
                        aMsg.getLoggingText ());
      }
      else
      {
        s_aLogger.info ("mic is matched, mic: " + sReturnMIC + aMsg.getLoggingText ());
      }

      try
      {
        DispositionType.createFromString (sDisposition).validate ();
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
      final OpenAS2Exception we = WrappedOpenAS2Exception.wrap (ex);
      we.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      throw we;
    }
  }

  private void _resend (@Nonnull final IMessage aMsg,
                        final OpenAS2Exception aCause,
                        final int nTries) throws OpenAS2Exception
  {
    if (!doResend (IProcessorSenderModule.DO_SEND, aMsg, aCause, nTries))
    {
      // Oh dear, we've run out of retries, do something interesting.
      // TODO create a fake failure MDN
      s_aLogger.error ("Message abandoned" + aMsg.getLoggingText ());
    }
  }

  protected void compress (@Nonnull final IMessage aMsg,
                           @Nonnull final OutputCompressor aOutputCompressor) throws SMIMEException, OpenAS2Exception
  {
    final SMIMECompressedGenerator aCompressedGenerator = new SMIMECompressedGenerator ();

    String sEncodeType = aMsg.getPartnership ().getAttribute (CPartnershipIDs.PA_CONTENT_TRANSFER_ENCODING);
    if (sEncodeType == null)
      sEncodeType = CAS2Header.DEFAULT_CONTENT_TRANSFER_ENCODING;
    aCompressedGenerator.setContentTransferEncoding (sEncodeType);

    final MimeBodyPart aCompressedBodyPart = aCompressedGenerator.generate (aMsg.getData (), aOutputCompressor);
    aMsg.addHeader (CAS2Header.HEADER_CONTENT_TRANSFER_ENCODING, sEncodeType);
    aMsg.setData (aCompressedBodyPart);

    if (s_aLogger.isTraceEnabled ())
    {
      try
      {
        s_aLogger.trace ("Compressed MIME msg AFTER COMPRESSION Content-Type:" + aCompressedBodyPart.getContentType ());
        s_aLogger.trace ("Compressed MIME msg AFTER COMPRESSION Content-Type Header:" +
                         aCompressedBodyPart.getHeader ("Content-Type"));
        s_aLogger.trace ("Compressed MIME msg AFTER COMPRESSION Content-Disposition:" +
                         aCompressedBodyPart.getDisposition ());
      }
      catch (final MessagingException e)
      {
        // ignore
      }
      s_aLogger.trace ("Msg AFTER COMPRESSION Content-Type:" + aMsg.getContentType ());
    }
  }

  @Nonnull
  protected MimeBodyPart secure (@Nonnull final IMessage aMsg) throws Exception
  {
    // Set up encrypt/sign variables
    MimeBodyPart aDataBP = aMsg.getData ();

    final Partnership aPartnership = aMsg.getPartnership ();
    final String sSignAlgorithm = aPartnership.getAttribute (CPartnershipIDs.PA_SIGN);
    final boolean bSign = sSignAlgorithm != null;
    final String sCryptAlgorithm = aPartnership.getAttribute (CPartnershipIDs.PA_ENCRYPT);
    final boolean bEncrypt = sCryptAlgorithm != null;
    final ICertificateFactory aCertFactory = getSession ().getCertificateFactory ();

    // Sign the data if requested
    if (bSign)
    {
      final X509Certificate aSenderCert = aCertFactory.getCertificate (aMsg, ECertificatePartnershipType.SENDER);
      final PrivateKey aSenderKey = aCertFactory.getPrivateKey (aMsg, aSenderCert);
      final ECryptoAlgorithmSign eSignAlgorithm = ECryptoAlgorithmSign.getFromIDOrNull (sSignAlgorithm);

      aDataBP = AS2Helper.getCryptoHelper ().sign (aDataBP, aSenderCert, aSenderKey, eSignAlgorithm);

      // Asynch MDN 2007-03-12
      final DataHistoryItem aHistoryItem = new DataHistoryItem (aDataBP.getContentType ());
      // *** add one more item to msg history
      aMsg.getHistory ().addItem (aHistoryItem);

      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Signed data with " + eSignAlgorithm + ":" + aMsg.getLoggingText ());
    }

    // Encrypt the data if requested
    if (bEncrypt)
    {
      final X509Certificate aReceiverCert = aCertFactory.getCertificate (aMsg, ECertificatePartnershipType.RECEIVER);
      final ECryptoAlgorithmCrypt eCryptAlgorithm = ECryptoAlgorithmCrypt.getFromIDOrNull (sCryptAlgorithm);

      aDataBP = AS2Helper.getCryptoHelper ().encrypt (aDataBP, aReceiverCert, eCryptAlgorithm);

      // Asynch MDN 2007-03-12
      final DataHistoryItem aHistoryItem = new DataHistoryItem (aDataBP.getContentType ());
      // *** add one more item to msg history
      aMsg.getHistory ().addItem (aHistoryItem);

      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Encrypted data with " + eCryptAlgorithm + ":" + aMsg.getLoggingText ());
    }

    return aDataBP;
  }

  protected void updateHttpHeaders (@Nonnull final HttpURLConnection aConn, @Nonnull final IMessage aMsg)
  {
    final Partnership aPartnership = aMsg.getPartnership ();

    aConn.setRequestProperty (CAS2Header.HEADER_CONNECTION, CAS2Header.DEFAULT_CONNECTION);
    aConn.setRequestProperty (CAS2Header.HEADER_USER_AGENT, CAS2Header.DEFAULT_USER_AGENT);

    aConn.setRequestProperty (CAS2Header.HEADER_DATE, DateHelper.getFormattedDateNow (CAS2Header.DEFAULT_DATE_FORMAT));
    aConn.setRequestProperty (CAS2Header.HEADER_MESSAGE_ID, aMsg.getMessageID ());
    // make sure this is the encoding used in the msg, run TBF1
    aConn.setRequestProperty (CAS2Header.HEADER_MIME_VERSION, CAS2Header.DEFAULT_MIME_VERSION);
    aConn.setRequestProperty (CAS2Header.HEADER_CONTENT_TYPE, aMsg.getContentType ());
    aConn.setRequestProperty (CAS2Header.HEADER_AS2_VERSION, CAS2Header.DEFAULT_AS2_VERSION);
    aConn.setRequestProperty (CAS2Header.HEADER_RECIPIENT_ADDRESS,
                              aPartnership.getAttribute (CPartnershipIDs.PA_AS2_URL));
    aConn.setRequestProperty (CAS2Header.HEADER_AS2_TO, aPartnership.getReceiverID (CPartnershipIDs.PID_AS2));
    aConn.setRequestProperty (CAS2Header.HEADER_AS2_FROM, aPartnership.getSenderID (CPartnershipIDs.PID_AS2));
    aConn.setRequestProperty (CAS2Header.HEADER_SUBJECT, aMsg.getSubject ());
    aConn.setRequestProperty (CAS2Header.HEADER_FROM, aPartnership.getSenderID (CPartnershipIDs.PID_EMAIL));

    // Determine where to send the MDN to
    final String sDispTo = aPartnership.getAttribute (CPartnershipIDs.PA_AS2_MDN_TO);
    if (sDispTo != null)
      aConn.setRequestProperty (CAS2Header.HEADER_DISPOSITION_NOTIFICATION_TO, sDispTo);

    final String sDispositionOptions = aPartnership.getAttribute (CPartnershipIDs.PA_AS2_MDN_OPTIONS);
    if (sDispositionOptions != null)
      aConn.setRequestProperty (CAS2Header.HEADER_DISPOSITION_NOTIFICATION_OPTIONS, sDispositionOptions);

    // Asynch MDN 2007-03-12
    final String sReceiptOption = aPartnership.getAttribute (CPartnershipIDs.PA_AS2_RECEIPT_OPTION);
    if (sReceiptOption != null)
      aConn.setRequestProperty (CAS2Header.HEADER_RECEIPT_DELIVERY_OPTION, sReceiptOption);

    // As of 2007-06-01
    final String sContententDisposition = aMsg.getContentDisposition ();
    if (sContententDisposition != null)
      aConn.setRequestProperty (CAS2Header.HEADER_CONTENT_DISPOSITION, sContententDisposition);
  }

  // Asynch MDN 2007-03-12
  /**
   * for storing original mic and outgoing file into pending information file
   *
   * @param aMsg
   *        AS2Message
   * @param sMIC
   *        MIC value
   * @throws OpenAS2Exception
   *         In case of an error
   */
  protected void storePendingInfo (@Nonnull final AS2Message aMsg, final String sMIC) throws OpenAS2Exception
  {
    OutputStream aFOS = null;
    try
    {
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Original MIC is : " + sMIC + aMsg.getLoggingText ());

      final String sPendingFolder = FilenameHelper.getAsSecureValidASCIIFilename (getSession ().getMessageProcessor ()
                                                                                               .getAttributeAsString (ATTR_PENDINGMDNINFO));
      final String sMsgFilename = IOHelper.getFilenameFromMessageID (aMsg.getMessageID ());
      final String sPendingFilename = FilenameHelper.getAsSecureValidASCIIFilename (getSession ().getMessageProcessor ()
                                                                                                 .getAttributeAsString (ATTR_PENDINGMDN)) +
                                      "/" +
                                      sMsgFilename;

      s_aLogger.info ("Save Original mic & message id information into folder '" +
                      sPendingFolder +
                      "'" +
                      aMsg.getLoggingText ());

      // input pending folder & original outgoing file name to get and
      // unique file name in order to avoid file overwriting.
      aFOS = FileHelper.getOutputStream (sPendingFolder + "/" + sMsgFilename);
      aFOS.write ((sMIC + "\n").getBytes (CCharset.CHARSET_ISO_8859_1_OBJ));
      aFOS.write (sPendingFilename.getBytes (CCharset.CHARSET_ISO_8859_1_OBJ));

      // remember
      aMsg.setAttribute (CFileAttribute.MA_PENDING_FILENAME, sPendingFilename);
      aMsg.setAttribute (CFileAttribute.MA_STATUS, CFileAttribute.MA_STATUS_PENDING);
    }
    catch (final Exception ex)
    {
      final OpenAS2Exception we = WrappedOpenAS2Exception.wrap (ex);
      we.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      throw we;
    }
    finally
    {
      StreamHelper.close (aFOS);
    }
  }
}
