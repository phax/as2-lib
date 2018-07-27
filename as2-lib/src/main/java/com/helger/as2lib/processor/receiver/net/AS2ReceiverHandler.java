/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2018 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.processor.receiver.net;

import static com.helger.as2lib.params.MessageParameters.ATTR_LARGE_FILE_SUPPORT_ON;
import static com.helger.as2lib.params.MessageParameters.ATTR_STORED_FILE_NAME;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;

import org.bouncycastle.cms.jcajce.ZlibExpanderProvider;
import org.bouncycastle.mail.smime.SMIMECompressed;
import org.bouncycastle.mail.smime.SMIMECompressedParser;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.cert.ECertificatePartnershipType;
import com.helger.as2lib.cert.ICertificateFactory;
import com.helger.as2lib.crypto.ICryptoHelper;
import com.helger.as2lib.disposition.DispositionException;
import com.helger.as2lib.disposition.DispositionType;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.params.MessageParameters;
import com.helger.as2lib.processor.CNetAttribute;
import com.helger.as2lib.processor.NoModuleException;
import com.helger.as2lib.processor.receiver.AS2ReceiverModule;
import com.helger.as2lib.processor.receiver.AbstractActiveNetModule;
import com.helger.as2lib.processor.sender.IProcessorSenderModule;
import com.helger.as2lib.processor.storage.IProcessorStorageModule;
import com.helger.as2lib.session.ComponentNotFoundException;
import com.helger.as2lib.session.IAS2Session;
import com.helger.as2lib.util.AS2Helper;
import com.helger.as2lib.util.AS2IOHelper;
import com.helger.as2lib.util.http.AS2HttpResponseHandlerSocket;
import com.helger.as2lib.util.http.AS2InputStreamProviderSocket;
import com.helger.as2lib.util.http.HTTPHelper;
import com.helger.as2lib.util.http.IAS2HttpResponseHandler;
import com.helger.as2lib.util.http.TempSharedFileInputStream;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.lang.StackTraceHelper;
import com.helger.commons.state.ESuccess;
import com.helger.commons.state.ETriState;
import com.helger.commons.timing.StopWatch;
import com.helger.mail.datasource.ByteArrayDataSource;

public class AS2ReceiverHandler extends AbstractReceiverHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS2ReceiverHandler.class);

  private final AS2ReceiverModule m_aReceiverModule;

  public AS2ReceiverHandler (@Nonnull final AS2ReceiverModule aModule)
  {
    m_aReceiverModule = ValueEnforcer.notNull (aModule, "Module");
  }

  @Nonnull
  protected final AS2ReceiverModule getReceiverModule ()
  {
    return m_aReceiverModule;
  }

  /**
   * Create a new message and record the source ip and port
   *
   * @param aSocket
   *        The socket through which the message will be read.
   * @return The {@link AS2Message} to use and never <code>null</code>.
   */
  @Nonnull
  protected AS2Message createMessage (@Nonnull final Socket aSocket)
  {
    final AS2Message aMsg = new AS2Message ();
    aMsg.attrs ().putIn (CNetAttribute.MA_SOURCE_IP, aSocket.getInetAddress ().getHostAddress ());
    aMsg.attrs ().putIn (CNetAttribute.MA_SOURCE_PORT, aSocket.getPort ());
    aMsg.attrs ().putIn (CNetAttribute.MA_DESTINATION_IP, aSocket.getLocalAddress ().getHostAddress ());
    aMsg.attrs ().putIn (CNetAttribute.MA_DESTINATION_PORT, aSocket.getLocalPort ());
    aMsg.attrs ().putIn (AS2Message.ATTRIBUTE_RECEIVED, true);
    return aMsg;
  }

  protected void decrypt (@Nonnull final IMessage aMsg) throws OpenAS2Exception
  {
    final ICertificateFactory aCertFactory = m_aReceiverModule.getSession ().getCertificateFactory ();
    final ICryptoHelper aCryptoHelper = AS2Helper.getCryptoHelper ();

    try
    {
      final boolean bDisableDecrypt = aMsg.partnership ().isDisableDecrypt ();
      final boolean bMsgIsEncrypted = aCryptoHelper.isEncrypted (aMsg.getData ());
      final boolean bForceDecrypt = aMsg.partnership ().isForceDecrypt ();
      final boolean bLargeFileSupportOn = aMsg.attrs ().getAsBoolean (ATTR_LARGE_FILE_SUPPORT_ON);
      if (LOGGER.isDebugEnabled () && bLargeFileSupportOn)
        LOGGER.debug ("Large file support on for " + aMsg.getLoggingText ());
      if (bMsgIsEncrypted && bDisableDecrypt)
      {
        if (LOGGER.isInfoEnabled ())
          LOGGER.info ("Message claims to be encrypted but decryption is disabled" + aMsg.getLoggingText ());
      }
      else
        if (bMsgIsEncrypted || bForceDecrypt)
        {
          // Decrypt
          if (bForceDecrypt && !bMsgIsEncrypted)
          {
            if (LOGGER.isInfoEnabled ())
              LOGGER.info ("Forced decrypting" + aMsg.getLoggingText ());
          }
          else
            if (LOGGER.isDebugEnabled ())
              LOGGER.debug ("Decrypting" + aMsg.getLoggingText ());

          final X509Certificate aReceiverCert = aCertFactory.getCertificate (aMsg,
                                                                             ECertificatePartnershipType.RECEIVER);
          final PrivateKey aReceiverKey = aCertFactory.getPrivateKey (aMsg, aReceiverCert);
          final MimeBodyPart aDecryptedData = aCryptoHelper.decrypt (aMsg.getData (),
                                                                     aReceiverCert,
                                                                     aReceiverKey,
                                                                     bForceDecrypt,
                                                                     bLargeFileSupportOn);
          aMsg.setData (aDecryptedData);
          // Remember that message was encrypted
          aMsg.attrs ().putIn (AS2Message.ATTRIBUTE_RECEIVED_ENCRYPTED, true);

          if (LOGGER.isInfoEnabled ())
            LOGGER.info ("Successfully decrypted incoming AS2 message" + aMsg.getLoggingText ());
        }
    }
    catch (final Exception ex)
    {
      if (LOGGER.isErrorEnabled ())
        LOGGER.error ("Error decrypting " + aMsg.getLoggingText () + ": " + ex.getMessage ());

      throw new DispositionException (DispositionType.createError ("decryption-failed"),
                                      AbstractActiveNetModule.DISP_DECRYPTION_ERROR,
                                      ex);
    }
  }

  protected void verify (@Nonnull final IMessage aMsg) throws OpenAS2Exception
  {
    final ICertificateFactory aCertFactory = m_aReceiverModule.getSession ().getCertificateFactory ();
    final ICryptoHelper aCryptoHelper = AS2Helper.getCryptoHelper ();

    try
    {
      final boolean bDisableVerify = aMsg.partnership ().isDisableVerify ();
      final boolean bMsgIsSigned = aCryptoHelper.isSigned (aMsg.getData ());
      final boolean bForceVerify = aMsg.partnership ().isForceVerify ();
      if (bMsgIsSigned && bDisableVerify)
      {
        if (LOGGER.isInfoEnabled ())
          LOGGER.info ("Message claims to be signed but signature validation is disabled" + aMsg.getLoggingText ());
      }
      else
        if (bMsgIsSigned || bForceVerify)
        {
          if (bForceVerify && !bMsgIsSigned)
          {
            if (LOGGER.isInfoEnabled ())
              LOGGER.info ("Forced verify signature" + aMsg.getLoggingText ());
          }
          else
            if (LOGGER.isDebugEnabled ())
              LOGGER.debug ("Verifying signature" + aMsg.getLoggingText ());

          final X509Certificate aSenderCert = aCertFactory.getCertificateOrNull (aMsg,
                                                                                 ECertificatePartnershipType.SENDER);
          boolean bUseCertificateInBodyPart;
          final ETriState eUseCertificateInBodyPart = aMsg.partnership ().getVerifyUseCertificateInBodyPart ();
          if (eUseCertificateInBodyPart.isDefined ())
          {
            // Use per partnership
            bUseCertificateInBodyPart = eUseCertificateInBodyPart.getAsBooleanValue ();
          }
          else
          {
            // Use global value
            bUseCertificateInBodyPart = m_aReceiverModule.getSession ().isCryptoVerifyUseCertificateInBodyPart ();
          }

          final MimeBodyPart aVerifiedData = aCryptoHelper.verify (aMsg.getData (),
                                                                   aSenderCert,
                                                                   bUseCertificateInBodyPart,
                                                                   bForceVerify);
          aMsg.setData (aVerifiedData);
          // Remember that message was signed and verified
          aMsg.attrs ().putIn (AS2Message.ATTRIBUTE_RECEIVED_SIGNED, true);

          if (LOGGER.isInfoEnabled ())
            LOGGER.info ("Successfully verified signature of incoming AS2 message" + aMsg.getLoggingText ());
        }
    }
    catch (final Exception ex)
    {
      if (LOGGER.isErrorEnabled ())
        LOGGER.error ("Error verifying signature " + aMsg.getLoggingText () + ": " + ex.getMessage ());

      throw new DispositionException (DispositionType.createError ("integrity-check-failed"),
                                      AbstractActiveNetModule.DISP_VERIFY_SIGNATURE_FAILED,
                                      ex);
    }
  }

  protected void decompress (@Nonnull final IMessage aMsg) throws DispositionException
  {
    try
    {
      if (aMsg.partnership ().isDisableDecompress ())
      {
        if (LOGGER.isInfoEnabled ())
          LOGGER.info ("Message claims to be compressed but decompression is disabled" + aMsg.getLoggingText ());
      }
      else
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Decompressing a compressed AS2 message");

        MimeBodyPart aDecompressedPart;
        final ZlibExpanderProvider aExpander = new ZlibExpanderProvider ();

        if (aMsg.attrs ().getAsBoolean (ATTR_LARGE_FILE_SUPPORT_ON))
        {
          // Compress using stream
          if (LOGGER.isDebugEnabled ())
          {
            StringBuilder partInf = new StringBuilder ();
            MimeBodyPart part = aMsg.getData ();
            Enumeration <String> lines = part.getAllHeaderLines ();
            while (lines.hasMoreElements ())
            {
              partInf.append (lines.nextElement ()).append ("\n");
            }
            partInf.append ("Headers before uncompress\n");
            LOGGER.debug (partInf.toString ());
          }

          SMIMECompressedParser smimeCompressedParser = new SMIMECompressedParser (aMsg.getData (), 8 * 1024);// TODO:
                                                                                                              // get
                                                                                                              // buffer
                                                                                                              // from
                                                                                                              // configuration
          aDecompressedPart = SMIMEUtil.toMimeBodyPart (smimeCompressedParser.getContent (aExpander));
        }
        else
        {
          final SMIMECompressed aCompressed = new SMIMECompressed (aMsg.getData ());
          // decompression step MimeBodyPart
          aDecompressedPart = SMIMEUtil.toMimeBodyPart (aCompressed.getContent (aExpander));
        }
        // Update the message object
        aMsg.setData (aDecompressedPart);
        // Remember that message was decompressed
        aMsg.attrs ().putIn (AS2Message.ATTRIBUTE_RECEIVED_COMPRESSED, true);

        if (LOGGER.isInfoEnabled ())
          LOGGER.info ("Successfully decompressed incoming AS2 message" + aMsg.getLoggingText ());
      }
    }
    catch (final Exception ex)
    {
      if (LOGGER.isErrorEnabled ())
        LOGGER.error ("Error decompressing received message", ex);

      throw new DispositionException (DispositionType.createError ("unexpected-processing-error"),
                                      AbstractActiveNetModule.DISP_DECOMPRESSION_ERROR,
                                      ex);
    }
  }

  protected void sendMDN (@Nonnull final String sClientInfo,
                          @Nonnull final IAS2HttpResponseHandler aResponseHandler,
                          @Nonnull final AS2Message aMsg,
                          @Nonnull final DispositionType aDisposition,
                          @Nonnull final String sText,
                          @Nonnull final ESuccess eSuccess)
  {
    final boolean bAllowErrorMDN = !aMsg.partnership ().isBlockErrorMDN ();
    if (eSuccess.isSuccess () || bAllowErrorMDN)
    {
      try
      {
        final IAS2Session aSession = m_aReceiverModule.getSession ();
        final IMessageMDN aMdn = AS2Helper.createMDN (aSession, aMsg, aDisposition, sText);

        if (aMsg.isRequestingAsynchMDN ())
        {
          // if asyncMDN requested, close existing synchronous connection and
          // initiate separate MDN send
          final HttpHeaderMap aHeaders = new HttpHeaderMap ();
          aHeaders.setContentLength (0);
          try (final NonBlockingByteArrayOutputStream aData = new NonBlockingByteArrayOutputStream ())
          {
            // Empty data
            // Ideally this would be HTTP 204 (no content)
            aResponseHandler.sendHttpResponse (HttpURLConnection.HTTP_OK, aHeaders, aData);
          }

          if (LOGGER.isInfoEnabled ())
            LOGGER.info ("Setup to send async MDN [" +
                         aDisposition.getAsString () +
                         "] " +
                         sClientInfo +
                         aMsg.getLoggingText ());

          // trigger explicit async sending
          aSession.getMessageProcessor ().handle (IProcessorSenderModule.DO_SEND_ASYNC_MDN, aMsg, null);
        }
        else
        {
          // otherwise, send sync MDN back on same connection
          if (LOGGER.isInfoEnabled ())
            LOGGER.info ("Sending back sync MDN [" +
                         aDisposition.getAsString () +
                         "] " +
                         sClientInfo +
                         aMsg.getLoggingText ());

          // Get data and therefore content length for sync MDN
          try (final NonBlockingByteArrayOutputStream aData = new NonBlockingByteArrayOutputStream ())
          {
            final MimeBodyPart aPart = aMdn.getData ();
            StreamHelper.copyInputStreamToOutputStream (aPart.getInputStream (), aData);
            aMdn.headers ().setContentLength (aData.size ());

            // start HTTP response
            aResponseHandler.sendHttpResponse (HttpURLConnection.HTTP_OK, aMdn.headers (), aData);
          }

          // Save sent MDN for later examination
          try
          {
            aSession.getMessageProcessor ().handle (IProcessorStorageModule.DO_STOREMDN, aMsg, null);
          }
          catch (final ComponentNotFoundException | NoModuleException ex)
          {
            // No message processor found
            // or No module found in message processor
          }
          if (LOGGER.isInfoEnabled ())
            LOGGER.info ("sent MDN [" + aDisposition.getAsString () + "] " + sClientInfo + aMsg.getLoggingText ());
        }
      }
      catch (final Exception ex)
      {
        final OpenAS2Exception we = WrappedOpenAS2Exception.wrap (ex);
        we.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
        we.terminate ();
      }
    }
  }

  /**
   * This method can be used to handle an incoming HTTP message AFTER the
   * headers where extracted.
   *
   * @param sClientInfo
   *        Client connection info
   * @param aMsgData
   *        The message body
   * @param aMsg
   *        The AS2 message that will be filled by this method
   * @param aResponseHandler
   *        The response handler which handles HTTP error messages as well as
   *        synchronous MDN.
   */
  public void handleIncomingMessage (@Nonnull final String sClientInfo,
                                     @Nonnull final DataSource aMsgData,
                                     @Nonnull final AS2Message aMsg,
                                     @Nonnull final IAS2HttpResponseHandler aResponseHandler)
  {
    // TODO store HTTP request, headers, and data to file in Received folder
    // -> use message-id for filename?
    try
    {
      final IAS2Session aSession = m_aReceiverModule.getSession ();

      try
      {
        // Put received data in a MIME body part
        final ContentType aReceivedContentType = new ContentType (aMsg.getHeader (CHttpHeader.CONTENT_TYPE));
        final String sReceivedContentType = aReceivedContentType.toString ();

        final MimeBodyPart aReceivedPart = new MimeBodyPart ();
        aReceivedPart.setDataHandler (new DataHandler (aMsgData));

        // Header must be set AFTER the DataHandler!
        aReceivedPart.setHeader (CHttpHeader.CONTENT_TYPE, sReceivedContentType);
        aMsg.setData (aReceivedPart);
      }
      catch (final Exception ex)
      {
        throw new DispositionException (DispositionType.createError ("unexpected-processing-error"),
                                        AbstractActiveNetModule.DISP_PARSING_MIME_FAILED,
                                        ex);
      }

      // Extract AS2 ID's from header, find the message's partnership and
      // update the message
      try
      {
        final String sAS2From = aMsg.getAS2From ();
        aMsg.partnership ().setSenderAS2ID (sAS2From);

        final String sAS2To = aMsg.getAS2To ();
        aMsg.partnership ().setReceiverAS2ID (sAS2To);

        // Fill all partnership attributes etc.
        aSession.getPartnershipFactory ().updatePartnership (aMsg, false);
      }
      catch (final OpenAS2Exception ex)
      {
        throw new DispositionException (DispositionType.createError ("authentication-failed"),
                                        AbstractActiveNetModule.DISP_PARTNERSHIP_NOT_FOUND,
                                        ex);
      }

      // Per RFC5402 compression is always before encryption but can be before
      // or after signing of message but only in one place
      final ICryptoHelper aCryptoHelper = AS2Helper.getCryptoHelper ();
      boolean bIsDecompressed = false;

      // Decrypt and verify signature of the data, and attach data to the
      // message
      decrypt (aMsg);

      if (aCryptoHelper.isCompressed (aMsg.getContentType ()))
      {
        if (LOGGER.isTraceEnabled ())
          LOGGER.trace ("Decompressing received message before checking signature...");
        decompress (aMsg);
        bIsDecompressed = true;
      }

      verify (aMsg);

      if (aCryptoHelper.isCompressed (aMsg.getContentType ()))
      {
        // Per RFC5402 compression is always before encryption but can be before
        // or after signing of message but only in one place
        if (bIsDecompressed)
        {
          throw new DispositionException (DispositionType.createError ("decompression-failed"),
                                          AbstractActiveNetModule.DISP_DECOMPRESSION_ERROR,
                                          new Exception ("Message has already been decompressed. Per RFC5402 it cannot occur twice."));
        }

        if (LOGGER.isTraceEnabled ())
          if (aMsg.attrs ().containsKey (AS2Message.ATTRIBUTE_RECEIVED_SIGNED))
            LOGGER.trace ("Decompressing received message after verifying signature...");
          else
            LOGGER.trace ("Decompressing received message after decryption...");
        decompress (aMsg);
        bIsDecompressed = true;
      }

      if (LOGGER.isTraceEnabled ())
        try
        {
          LOGGER.trace ("SMIME Decrypted Content-Disposition: " +
                        aMsg.getContentDisposition () +
                        "\n      Content-Type received: " +
                        aMsg.getContentType () +
                        "\n      HEADERS after decryption: " +
                        aMsg.getData ().getAllHeaders () +
                        "\n      Content-Disposition in MSG detData() MIMEPART after decryption: " +
                        aMsg.getData ().getContentType ());
        }
        catch (final MessagingException ex)
        {
          if (LOGGER.isErrorEnabled ())
            LOGGER.error ("Failed to trace message: " + aMsg, ex);
        }

      // Validate the received message before storing
      try
      {
        aSession.getMessageProcessor ().handle (IProcessorStorageModule.DO_VALIDATE_BEFORE_STORE, aMsg, null);
      }
      catch (final NoModuleException ex)
      {
        // No module installed - ignore
      }
      catch (final OpenAS2Exception ex)
      {
        throw new DispositionException (DispositionType.createError ("unexpected-processing-error"),
                                        AbstractActiveNetModule.DISP_VALIDATION_FAILED +
                                                                                                     "\n" +
                                                                                                     MessageParameters.getEscapedString (StackTraceHelper.getStackAsString (ex)),
                                        ex);
      }

      // Store the received message
      try
      {
        aSession.getMessageProcessor ().handle (IProcessorStorageModule.DO_STORE, aMsg, null);
      }
      catch (final NoModuleException ex)
      {
        // No module installed - ignore
      }
      catch (final OpenAS2Exception ex)
      {
        throw new DispositionException (DispositionType.createError ("unexpected-processing-error"),
                                        AbstractActiveNetModule.DISP_STORAGE_FAILED +
                                                                                                     "\n" +
                                                                                                     MessageParameters.getEscapedString (ex.getMessage ()),
                                        ex);
      }

      // Validate the received message after storing
      try
      {
        aSession.getMessageProcessor ().handle (IProcessorStorageModule.DO_VALIDATE_AFTER_STORE, aMsg, null);
      }
      catch (final NoModuleException ex)
      {
        // No module installed - ignore
      }
      catch (final OpenAS2Exception ex)
      {
        throw new DispositionException (DispositionType.createError ("unexpected-processing-error"),
                                        AbstractActiveNetModule.DISP_VALIDATION_FAILED +
                                                                                                     "\n" +
                                                                                                     MessageParameters.getEscapedString (StackTraceHelper.getStackAsString (ex)),
                                        ex);
      }

      try
      {
        if (aMsg.isRequestingMDN ())
        {
          // Transmit a success MDN if requested
          if (aMsg.attrs ().getAsBoolean (ATTR_LARGE_FILE_SUPPORT_ON))
          {
            // if large file support is on, the message does not hold the actual
            // data. in order to get the data for the MDN, a new message that
            // will take the data from the file that was written should be used
            String sStoredFileName = aMsg.attrs ().getAsString (ATTR_STORED_FILE_NAME);
            FileDataSource aFileDataSource = new FileDataSource (sStoredFileName);
            DataHandler aDataFromFile = new DataHandler (aFileDataSource);
            aMsg.getData ().setDataHandler (aDataFromFile);
          }
          sendMDN (sClientInfo,
                   aResponseHandler,
                   aMsg,
                   DispositionType.createSuccess (),
                   AbstractActiveNetModule.DISP_SUCCESS,
                   ESuccess.SUCCESS);
        }
        else
        {
          // Just send a HTTP OK
          HTTPHelper.sendSimpleHTTPResponse (aResponseHandler, HttpURLConnection.HTTP_OK);
          if (LOGGER.isInfoEnabled ())
            LOGGER.info ("sent HTTP OK " + sClientInfo + aMsg.getLoggingText ());
        }
      }
      catch (final Exception ex)
      {
        throw new WrappedOpenAS2Exception ("Error creating and returning MDN, message was stilled processed", ex);
      }
    }
    catch (final DispositionException ex)
    {
      sendMDN (sClientInfo, aResponseHandler, aMsg, ex.getDisposition (), ex.getText (), ESuccess.FAILURE);
      m_aReceiverModule.handleError (aMsg, ex);
    }
    catch (final OpenAS2Exception ex)
    {
      m_aReceiverModule.handleError (aMsg, ex);
    }
    finally
    {
      // close the temporary shared stream if it exists
      TempSharedFileInputStream sis = aMsg.getTempSharedFileInputStream ();
      if (null != sis)
      {
        try
        {
          sis.closeAll ();
        }
        catch (IOException e)
        {
          LOGGER.error ("Exception while closing TempSharedFileInputStream", e);
        }
      }
    }
  }

  public void handle (@Nullable final AbstractActiveNetModule aOwner, @Nonnull final Socket aSocket)
  {
    final String sClientInfo = getClientInfo (aSocket);
    final boolean bLargeFileSupportOn = aOwner.getAsBoolean (ATTR_LARGE_FILE_SUPPORT_ON);
    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("Incoming connection " + sClientInfo);

    final AS2Message aMsg = createMessage (aSocket);
    aMsg.attrs ().putIn (ATTR_LARGE_FILE_SUPPORT_ON, bLargeFileSupportOn);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Large file support on:" + aMsg.attrs ().getAsBoolean (ATTR_LARGE_FILE_SUPPORT_ON));

    final IAS2HttpResponseHandler aResponseHandler = new AS2HttpResponseHandlerSocket (aSocket);

    // Time the transmission
    final StopWatch aSW = StopWatch.createdStarted ();
    DataSource aMsgDataSource = null;
    try
    {
      // Read in the message request, headers, and data
      aMsgDataSource = readAndDecodeHttpRequest (new AS2InputStreamProviderSocket (aSocket, bLargeFileSupportOn),
                                                 aResponseHandler,
                                                 aMsg);
    }
    catch (final Exception ex)
    {
      final NetException ne = new NetException (aSocket.getInetAddress (), aSocket.getPort (), ex);
      ne.terminate ();
    }

    aSW.stop ();

    if (aMsgDataSource != null)
      if (aMsgDataSource instanceof ByteArrayDataSource)
      {
        if (LOGGER.isInfoEnabled ())
          LOGGER.info ("received " +
                       AS2IOHelper.getTransferRate (((ByteArrayDataSource) aMsgDataSource).directGetBytes ().length,
                                                    aSW) +
                       " from " +
                       sClientInfo +
                       aMsg.getLoggingText ());

      }
      else
      {
        LOGGER.info ("received message from " + sClientInfo + aMsg.getLoggingText ());

      }
    handleIncomingMessage (sClientInfo, aMsgDataSource, aMsg, aResponseHandler);
  }
}
