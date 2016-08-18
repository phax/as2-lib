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
package com.helger.as2lib.processor.sender;

import java.io.File;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.cert.ECertificatePartnershipType;
import com.helger.as2lib.cert.ICertificateFactory;
import com.helger.as2lib.crypto.ECompressionType;
import com.helger.as2lib.crypto.ECryptoAlgorithmCrypt;
import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.as2lib.disposition.DispositionException;
import com.helger.as2lib.disposition.DispositionOptions;
import com.helger.as2lib.disposition.DispositionType;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.AS2MessageMDN;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.params.InvalidParameterException;
import com.helger.as2lib.partner.CPartnershipIDs;
import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.processor.CFileAttribute;
import com.helger.as2lib.processor.CNetAttribute;
import com.helger.as2lib.processor.NoModuleException;
import com.helger.as2lib.processor.storage.IProcessorStorageModule;
import com.helger.as2lib.session.ComponentNotFoundException;
import com.helger.as2lib.util.AS2Helper;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.DateHelper;
import com.helger.as2lib.util.IOHelper;
import com.helger.as2lib.util.http.AS2HttpHeaderWrapperHttpURLConnection;
import com.helger.as2lib.util.http.HTTPHelper;
import com.helger.as2lib.util.http.IAS2HttpHeaderWrapper;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.charset.CCharset;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.io.stream.WrappedOutputStream;
import com.helger.commons.state.ETriState;
import com.helger.commons.string.StringParser;
import com.helger.commons.timing.StopWatch;
import com.helger.http.EHTTPMethod;
import com.helger.mail.cte.EContentTransferEncoding;

/**
 * AS2 sender module to send AS2 messages out.
 *
 * @author Philip Helger
 */
public class AS2SenderModule extends AbstractHttpSenderModule
{
  /** Must be false in production! */
  private static final boolean DEBUG_DUMP_OUTGOING_HTTP = false;

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
                                            aPartnership.getAS2URL ());
      InvalidParameterException.checkValue (aMsg,
                                            "Receiver: " + CPartnershipIDs.PID_AS2,
                                            aPartnership.getReceiverAS2ID ());
      InvalidParameterException.checkValue (aMsg, "Sender: " + CPartnershipIDs.PID_AS2, aPartnership.getSenderAS2ID ());
      InvalidParameterException.checkValue (aMsg, "Subject", aMsg.getSubject ());
      InvalidParameterException.checkValue (aMsg,
                                            "Sender: " + CPartnershipIDs.PID_EMAIL,
                                            aPartnership.getSenderEmail ());
      InvalidParameterException.checkValue (aMsg, "Message Data", aMsg.getData ());
    }
    catch (final InvalidParameterException ex)
    {
      ex.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      throw ex;
    }
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
  protected void storePendingInfo (@Nonnull final AS2Message aMsg, @Nonnull final String sMIC) throws OpenAS2Exception
  {
    OutputStream aFOS = null;
    try
    {
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Original MIC is '" + sMIC + "'" + aMsg.getLoggingText ());

      final String sPendingFolder = FilenameHelper.getAsSecureValidASCIIFilename (getSession ().getMessageProcessor ()
                                                                                               .getAttributeAsString (ATTR_PENDINGMDNINFO));
      final String sMsgFilename = IOHelper.getFilenameFromMessageID (aMsg.getMessageID ());
      final String sPendingFilename = FilenameHelper.getAsSecureValidASCIIFilename (getSession ().getMessageProcessor ()
                                                                                                 .getAttributeAsString (ATTR_PENDINGMDN)) +
                                      "/" +
                                      sMsgFilename;

      s_aLogger.info ("Save Original MIC & message id information into folder '" +
                      sPendingFolder +
                      "'" +
                      aMsg.getLoggingText ());

      // input pending folder & original outgoing file name to get and
      // unique file name in order to avoid file overwriting.
      aFOS = FileHelper.getOutputStream (sPendingFolder + "/" + sMsgFilename);
      aFOS.write ((sMIC + "\n" + sPendingFilename).getBytes (CCharset.CHARSET_ISO_8859_1_OBJ));

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

  /**
   * From RFC 4130 section 7.3.1:
   * <ul>
   * <li>For any signed messages, the MIC to be returned is calculated on the
   * RFC1767/RFC3023 MIME header and content. Canonicalization on the MIME
   * headers MUST be performed before the MIC is calculated, since the sender
   * requesting the signed receipt was also REQUIRED to canonicalize.</li>
   * <li>For encrypted, unsigned messages, the MIC to be returned is calculated
   * on the decrypted RFC 1767/RFC3023 MIME header and content. The content
   * after decryption MUST be canonicalized before the MIC is calculated.</li>
   * <li>For unsigned, unencrypted messages, the MIC MUST be calculated over the
   * message contents without the MIME or any other RFC 2822 headers, since
   * these are sometimes altered or reordered by Mail Transport Agents (MTAs).
   * </li>
   * </ul>
   * So headers must be included if signing or crypting is enabled.<br>
   * <br>
   * From RFC 5402 section 4.1:
   * <ul>
   * <li>MIC Calculation for Signed Message: For any signed message, the MIC to
   * be returned is calculated over the same data that was signed in the
   * original message as per [AS1]. The signed content will be a MIME bodypart
   * that contains either compressed or uncompressed data.</li>
   * <li>MIC Calculation for Encrypted, Unsigned Message: For encrypted,
   * unsigned messages, the MIC to be returned is calculated over the
   * uncompressed data content including all MIME header fields and any applied
   * Content-Transfer-Encoding.</li>
   * <li>MIC Calculation for Unencrypted, Unsigned Message: For unsigned,
   * unencrypted messages, the MIC is calculated over the uncompressed data
   * content including all MIME header fields and any applied
   * Content-Transfer-Encoding</li>
   * </ul>
   * So headers must always be included if compression is enabled.
   *
   * @param aMsg
   *        Source message
   * @return MIC value. Neither <code>null</code> nor empty.
   * @throws Exception
   *         On security or AS2 issues
   */
  @Nonnull
  @Nonempty
  protected String calculateAndStoreMIC (@Nonnull final AS2Message aMsg) throws Exception
  {
    final Partnership aPartnership = aMsg.getPartnership ();
    final String sDispositionOptions = aPartnership.getAS2MDNOptions ();
    final DispositionOptions aDispositionOptions = DispositionOptions.createFromString (sDispositionOptions);

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("DispositionOptions=" + aDispositionOptions);

    // Calculate and get the original mic
    final boolean bIncludeHeadersInMIC = aPartnership.getSigningAlgorithm () != null ||
                                         aPartnership.getEncryptAlgorithm () != null ||
                                         aPartnership.getCompressionType () != null;

    final String sMIC = AS2Helper.getCryptoHelper ().calculateMIC (aMsg.getData (),
                                                                   aDispositionOptions.getFirstMICAlg (),
                                                                   bIncludeHeadersInMIC);
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Calculated MIC: '" + sMIC + "'");

    if (aPartnership.getAS2ReceiptOption () != null)
    {
      // if yes : PA_AS2_RECEIPT_OPTION != null
      // then keep the original mic & message id.
      // then wait for the another HTTP call by receivers
      storePendingInfo (aMsg, sMIC);
    }

    return sMIC;
  }

  @Nonnull
  protected MimeBodyPart compress (@Nonnull final IMessage aMsg,
                                   @Nonnull final MimeBodyPart aData,
                                   @Nonnull final ECompressionType eCompressionType) throws SMIMEException,
                                                                                     MessagingException
  {
    final SMIMECompressedGenerator aCompressedGenerator = new SMIMECompressedGenerator ();

    final String sTransferEncoding = aMsg.getPartnership ()
                                         .getContentTransferEncoding (EContentTransferEncoding.AS2_DEFAULT.getID ());
    aCompressedGenerator.setContentTransferEncoding (sTransferEncoding);

    final MimeBodyPart aCompressedBodyPart = aCompressedGenerator.generate (aData,
                                                                            eCompressionType.createOutputCompressor ());
    aMsg.addHeader (CAS2Header.HEADER_CONTENT_TRANSFER_ENCODING, sTransferEncoding);

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Compressed data with " +
                       eCompressionType +
                       " to " +
                       aCompressedBodyPart.getContentType () +
                       ":" +
                       aMsg.getLoggingText ());

    return aCompressedBodyPart;
  }

  @Nonnull
  protected MimeBodyPart secure (@Nonnull final IMessage aMsg) throws Exception
  {
    // Set up encrypt/sign variables
    MimeBodyPart aDataBP = aMsg.getData ();

    final Partnership aPartnership = aMsg.getPartnership ();
    final ICertificateFactory aCertFactory = getSession ().getCertificateFactory ();

    // Check compression parameters
    // If compression is enabled, by default is is compressed before signing
    final String sCompressionType = aPartnership.getCompressionType ();
    ECompressionType eCompressionType = null;
    boolean bCompressBeforeSign = true;
    if (sCompressionType != null)
    {
      eCompressionType = ECompressionType.getFromIDCaseInsensitiveOrNull (sCompressionType);
      if (eCompressionType == null)
        throw new OpenAS2Exception ("The compression type '" + sCompressionType + "' is not supported!");

      bCompressBeforeSign = aPartnership.isCompressBeforeSign ();
    }

    if (eCompressionType != null && bCompressBeforeSign)
    {
      // Compress before sign
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Compressing outbound message before signing...");
      aDataBP = compress (aMsg, aDataBP, eCompressionType);

      // Replace the message data, because it is the basis for the MIC
      aMsg.setData (aDataBP);
    }

    // Sign the data if requested
    final String sSignAlgorithm = aPartnership.getSigningAlgorithm ();
    if (sSignAlgorithm != null)
    {
      final X509Certificate aSenderCert = aCertFactory.getCertificate (aMsg, ECertificatePartnershipType.SENDER);
      final PrivateKey aSenderKey = aCertFactory.getPrivateKey (aMsg, aSenderCert);
      final ECryptoAlgorithmSign eSignAlgorithm = ECryptoAlgorithmSign.getFromIDOrNull (sSignAlgorithm);
      if (eSignAlgorithm == null)
        throw new OpenAS2Exception ("The signing algorithm '" + sSignAlgorithm + "' is not supported!");

      // Include certificate in signed content?
      boolean bIncludeCertificateInSignedContent;
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
        bIncludeCertificateInSignedContent = getSession ().isCryptoSignIncludeCertificateInBodyPart ();
      }

      // Use old MIC algorithms?
      final boolean bUseRFC3851MICAlg = aPartnership.isRFC3851MICAlgs ();

      // Main signing
      aDataBP = AS2Helper.getCryptoHelper ()
                         .sign (aDataBP,
                                aSenderCert,
                                aSenderKey,
                                eSignAlgorithm,
                                bIncludeCertificateInSignedContent,
                                bUseRFC3851MICAlg);

      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Signed data with " +
                         eSignAlgorithm +
                         " to " +
                         aDataBP.getContentType () +
                         ":" +
                         aMsg.getLoggingText ());
    }

    if (eCompressionType != null && !bCompressBeforeSign)
    {
      // Compress after sign
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Compressing outbound message after signing...");
      aDataBP = compress (aMsg, aDataBP, eCompressionType);
    }

    // Encrypt the data if requested
    final String sCryptAlgorithm = aPartnership.getEncryptAlgorithm ();
    if (sCryptAlgorithm != null)
    {
      final X509Certificate aReceiverCert = aCertFactory.getCertificate (aMsg, ECertificatePartnershipType.RECEIVER);
      final ECryptoAlgorithmCrypt eCryptAlgorithm = ECryptoAlgorithmCrypt.getFromIDOrNull (sCryptAlgorithm);
      if (eCryptAlgorithm == null)
        throw new OpenAS2Exception ("The crypting algorithm '" + sCryptAlgorithm + "' is not supported!");

      aDataBP = AS2Helper.getCryptoHelper ().encrypt (aDataBP, aReceiverCert, eCryptAlgorithm);

      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Encrypted data with " +
                         eCryptAlgorithm +
                         " to " +
                         aDataBP.getContentType () +
                         ":" +
                         aMsg.getLoggingText ());
    }

    return aDataBP;
  }

  protected void updateHttpHeaders (@Nonnull final IAS2HttpHeaderWrapper aConn, @Nonnull final IMessage aMsg)
  {
    final Partnership aPartnership = aMsg.getPartnership ();

    aConn.setHttpHeader (CAS2Header.HEADER_CONNECTION, CAS2Header.DEFAULT_CONNECTION);
    aConn.setHttpHeader (CAS2Header.HEADER_USER_AGENT, CAS2Header.DEFAULT_USER_AGENT);

    aConn.setHttpHeader (CAS2Header.HEADER_DATE, DateHelper.getFormattedDateNow (CAS2Header.DEFAULT_DATE_FORMAT));
    aConn.setHttpHeader (CAS2Header.HEADER_MESSAGE_ID, aMsg.getMessageID ());
    // make sure this is the encoding used in the msg, run TBF1
    aConn.setHttpHeader (CAS2Header.HEADER_MIME_VERSION, CAS2Header.DEFAULT_MIME_VERSION);
    aConn.setHttpHeader (CAS2Header.HEADER_CONTENT_TYPE, aMsg.getContentType ());
    aConn.setHttpHeader (CAS2Header.HEADER_AS2_VERSION, CAS2Header.DEFAULT_AS2_VERSION);
    aConn.setHttpHeader (CAS2Header.HEADER_RECIPIENT_ADDRESS, aPartnership.getAS2URL ());
    aConn.setHttpHeader (CAS2Header.HEADER_AS2_FROM, aPartnership.getSenderAS2ID ());
    aConn.setHttpHeader (CAS2Header.HEADER_AS2_TO, aPartnership.getReceiverAS2ID ());
    aConn.setHttpHeader (CAS2Header.HEADER_SUBJECT, aMsg.getSubject ());
    aConn.setHttpHeader (CAS2Header.HEADER_FROM, aPartnership.getSenderEmail ());
    // Set when compression is enabled
    aConn.setHttpHeader (CAS2Header.HEADER_CONTENT_TRANSFER_ENCODING,
                         aMsg.getHeader (CAS2Header.HEADER_CONTENT_TRANSFER_ENCODING));

    // Determine where to send the MDN to
    final String sDispTo = aPartnership.getAS2MDNTo ();
    if (sDispTo != null)
      aConn.setHttpHeader (CAS2Header.HEADER_DISPOSITION_NOTIFICATION_TO, sDispTo);

    final String sDispositionOptions = aPartnership.getAS2MDNOptions ();
    if (sDispositionOptions != null)
      aConn.setHttpHeader (CAS2Header.HEADER_DISPOSITION_NOTIFICATION_OPTIONS, sDispositionOptions);

    // Asynch MDN 2007-03-12
    final String sReceiptOption = aPartnership.getAS2ReceiptOption ();
    if (sReceiptOption != null)
      aConn.setHttpHeader (CAS2Header.HEADER_RECEIPT_DELIVERY_OPTION, sReceiptOption);

    // As of 2007-06-01
    final String sContententDisposition = aMsg.getContentDisposition ();
    if (sContententDisposition != null)
      aConn.setHttpHeader (CAS2Header.HEADER_CONTENT_DISPOSITION, sContententDisposition);
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
  protected void receiveSyncMDN (@Nonnull final AS2Message aMsg,
                                 @Nonnull final HttpURLConnection aConn,
                                 @Nonnull final String sOriginalMIC) throws OpenAS2Exception, IOException
  {
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Receiving synchronous MDN for message" + aMsg.getLoggingText ());

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

      if (s_aLogger.isTraceEnabled ())
      {
        // Debug print the whole MDN stream
        s_aLogger.trace ("Retrieved MDN stream data:\n" + aMDNStream.getAsString (CCharset.CHARSET_ISO_8859_1_OBJ));
      }

      final MimeBodyPart aPart = new MimeBodyPart (aMDN.getHeaders (), aMDNStream.toByteArray ());
      aMsg.getMDN ().setData (aPart);

      // get the MDN partnership info
      aMDN.getPartnership ().setSenderAS2ID (aMDN.getHeader (CAS2Header.HEADER_AS2_FROM));
      aMDN.getPartnership ().setReceiverAS2ID (aMDN.getHeader (CAS2Header.HEADER_AS2_TO));
      // Set the appropriate keystore aliases
      aMDN.getPartnership ().setSenderX509Alias (aMsg.getPartnership ().getReceiverX509Alias ());
      aMDN.getPartnership ().setReceiverX509Alias (aMsg.getPartnership ().getSenderX509Alias ());
      // Update the partnership
      getSession ().getPartnershipFactory ().updatePartnership (aMDN, false);

      final ICertificateFactory aCertFactory = getSession ().getCertificateFactory ();
      final X509Certificate aSenderCert = aCertFactory.getCertificate (aMDN, ECertificatePartnershipType.SENDER);

      boolean bUseCertificateInBodyPart;
      final ETriState eUseCertificateInBodyPart = aMsg.getPartnership ().getVerifyUseCertificateInBodyPart ();
      if (eUseCertificateInBodyPart.isDefined ())
      {
        // Use per partnership
        bUseCertificateInBodyPart = eUseCertificateInBodyPart.getAsBooleanValue ();
      }
      else
      {
        // Use global value
        bUseCertificateInBodyPart = getSession ().isCryptoVerifyUseCertificateInBodyPart ();
      }

      AS2Helper.parseMDN (aMsg, aSenderCert, bUseCertificateInBodyPart);

      try
      {
        getSession ().getMessageProcessor ().handle (IProcessorStorageModule.DO_STOREMDN, aMsg, null);
      }
      catch (final ComponentNotFoundException ex)
      {
        // No message processor found
      }
      catch (final NoModuleException ex)
      {
        // No module found in message processor
      }

      final String sDisposition = aMsg.getMDN ().getAttribute (AS2MessageMDN.MDNA_DISPOSITION);

      s_aLogger.info ("received MDN [" + sDisposition + "]" + aMsg.getLoggingText ());

      // Asynch MDN 2007-03-12
      // Verify if the original mic is equal to the mic in returned MDN
      final String sReturnMIC = aMsg.getMDN ().getAttribute (AS2MessageMDN.MDNA_MIC);

      // Catch ReturnMIC == null in case the attribute is simply missing
      if (sReturnMIC == null || !sReturnMIC.replaceAll ("\\s+", "").equals (sOriginalMIC.replaceAll ("\\s+", "")))
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
        if (ex.getDisposition ().isWarning ())
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

  @OverrideOnDemand
  protected void onReceivedMDNError (@Nonnull final AS2Message aMsg, @Nonnull final OpenAS2Exception ex)
  {
    final OpenAS2Exception oae2 = new OpenAS2Exception ("Message was sent but an error occured while receiving the MDN",
                                                        ex);
    oae2.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
    oae2.terminate ();
  }

  private void _sendViaHTTP (@Nonnull final AS2Message aMsg,
                             @Nonnull final MimeBodyPart aSecuredMimePart,
                             @Nonnull final String sMIC) throws OpenAS2Exception, IOException, MessagingException
  {
    final Partnership aPartnership = aMsg.getPartnership ();

    // Create the HTTP connection
    final String sUrl = aPartnership.getAS2URL ();
    final boolean bOutput = true;
    final boolean bInput = true;
    final boolean bUseCaches = false;
    final EHTTPMethod eRequestMethod = EHTTPMethod.POST;
    final HttpURLConnection aConn = getConnection (sUrl,
                                                   bOutput,
                                                   bInput,
                                                   bUseCaches,
                                                   eRequestMethod,
                                                   getSession ().getHttpProxy ());
    try
    {
      s_aLogger.info ("Connecting to " + sUrl + aMsg.getLoggingText ());

      updateHttpHeaders (new AS2HttpHeaderWrapperHttpURLConnection (aConn), aMsg);

      aMsg.setAttribute (CNetAttribute.MA_DESTINATION_IP, aConn.getURL ().getHost ());
      aMsg.setAttribute (CNetAttribute.MA_DESTINATION_PORT, Integer.toString (aConn.getURL ().getPort ()));

      // Note: closing this stream causes connection abort errors on some AS2
      // servers
      OutputStream aMsgOS = aConn.getOutputStream ();

      // This stream dumps the HTTP
      OutputStream aDebugOS = null;
      if (DEBUG_DUMP_OUTGOING_HTTP)
      {
        aDebugOS = StreamHelper.getBuffered (FileHelper.getOutputStream (new File ("as2-sent-data",
                                                                                   Long.toString (System.currentTimeMillis ()) +
                                                                                                    ".rawhttp")));

        // Overwrite the used OutputStream to additionally log to the debug
        // OutputStream
        final OutputStream aFinalDebugOS = aDebugOS;
        aMsgOS = new WrappedOutputStream (aMsgOS)
        {
          @Override
          public final void write (final int b) throws IOException
          {
            super.write (b);
            aFinalDebugOS.write (b);
          }
        };
      }

      // Transfer the data
      final InputStream aMsgIS = aSecuredMimePart.getInputStream ();

      final StopWatch aSW = StopWatch.createdStarted ();
      // Main transmission - closes InputStream
      final long nBytes = IOHelper.copy (aMsgIS, aMsgOS);
      aSW.stop ();
      s_aLogger.info ("transferred " + IOHelper.getTransferRate (nBytes, aSW) + aMsg.getLoggingText ());

      // Close debug OS (if used)
      StreamHelper.close (aDebugOS);

      // Check the HTTP Response code
      final int nResponseCode = aConn.getResponseCode ();
      // Accept most of 2xx HTTP response codes
      if (nResponseCode != HttpURLConnection.HTTP_OK &&
          nResponseCode != HttpURLConnection.HTTP_CREATED &&
          nResponseCode != HttpURLConnection.HTTP_ACCEPTED &&
          nResponseCode != HttpURLConnection.HTTP_NO_CONTENT &&
          nResponseCode != HttpURLConnection.HTTP_PARTIAL)
      {
        s_aLogger.error ("Error URL '" + sUrl + "' - HTTP " + nResponseCode + " " + aConn.getResponseMessage ());
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
          if (aPartnership.getAS2ReceiptOption () == null)
          {
            // go ahead to receive sync MDN
            receiveSyncMDN (aMsg, aConn, sMIC);
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
        // Don't re-send or fail, just log an error if one occurs while
        // receiving the MDN
        onReceivedMDNError (aMsg, ex);
      }
    }
    finally
    {
      aConn.disconnect ();
    }
  }

  public void handle (@Nonnull final String sAction,
                      @Nonnull final IMessage aBaseMsg,
                      @Nullable final Map <String, Object> aOptions) throws OpenAS2Exception
  {
    final AS2Message aMsg = (AS2Message) aBaseMsg;
    s_aLogger.info ("Submitting message" + aMsg.getLoggingText ());

    // verify all required information is present for sending
    checkRequired (aMsg);

    final int nRetries = getRetryCount (aMsg.getPartnership (), aOptions);

    try
    {
      // compress and/or sign and/or encrypt the message if needed
      final MimeBodyPart aSecuredData = secure (aMsg);

      // Calculate MIC after compress/sign/crypt was handled, because the
      // message data might change if compression before signing is active.
      final String sMIC = calculateAndStoreMIC (aMsg);

      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Setting message content type to '" + aSecuredData.getContentType () + "'");
      aMsg.setContentType (aSecuredData.getContentType ());

      _sendViaHTTP (aMsg, aSecuredData, sMIC);
    }
    catch (final HttpResponseException ex)
    {
      s_aLogger.error ("Http Response Error " + ex.getMessage ());
      ex.terminate ();

      if (!doResend (IProcessorSenderModule.DO_SEND, aMsg, ex, nRetries))
        throw ex;
    }
    catch (final IOException ex)
    {
      // Re-send if a network error occurs during transmission
      final OpenAS2Exception wioe = WrappedOpenAS2Exception.wrap (ex);
      wioe.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      wioe.terminate ();

      if (!doResend (IProcessorSenderModule.DO_SEND, aMsg, wioe, nRetries))
        throw wioe;
    }
    catch (final Exception ex)
    {
      // Propagate error if it can't be handled by a re-send
      throw WrappedOpenAS2Exception.wrap (ex);
    }
  }
}
