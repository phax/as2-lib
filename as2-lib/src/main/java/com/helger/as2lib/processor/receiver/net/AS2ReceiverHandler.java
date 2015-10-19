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
package com.helger.as2lib.processor.receiver.net;

import java.net.HttpURLConnection;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.activation.DataHandler;
import javax.annotation.Nonnull;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;

import org.bouncycastle.cms.jcajce.ZlibExpanderProvider;
import org.bouncycastle.mail.smime.SMIMECompressed;
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
import com.helger.as2lib.processor.CNetAttribute;
import com.helger.as2lib.processor.NoModuleException;
import com.helger.as2lib.processor.receiver.AS2ReceiverModule;
import com.helger.as2lib.processor.receiver.AbstractActiveNetModule;
import com.helger.as2lib.processor.sender.IProcessorSenderModule;
import com.helger.as2lib.processor.storage.IProcessorStorageModule;
import com.helger.as2lib.session.ComponentNotFoundException;
import com.helger.as2lib.session.IAS2Session;
import com.helger.as2lib.util.AS2Helper;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.IOHelper;
import com.helger.as2lib.util.http.AS2HttpResponseHandlerSocket;
import com.helger.as2lib.util.http.AS2InputStreamProviderSocket;
import com.helger.as2lib.util.http.HTTPHelper;
import com.helger.as2lib.util.http.IAS2HttpResponseHandler;
import com.helger.as2lib.util.javamail.ByteArrayDataSource;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.lang.StackTraceHelper;
import com.helger.commons.state.ETriState;
import com.helger.commons.timing.StopWatch;

public class AS2ReceiverHandler extends AbstractReceiverHandler
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS2ReceiverHandler.class);

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
    aMsg.setAttribute (CNetAttribute.MA_SOURCE_IP, aSocket.getInetAddress ().toString ());
    aMsg.setAttribute (CNetAttribute.MA_SOURCE_PORT, Integer.toString (aSocket.getPort ()));
    aMsg.setAttribute (CNetAttribute.MA_DESTINATION_IP, aSocket.getLocalAddress ().toString ());
    aMsg.setAttribute (CNetAttribute.MA_DESTINATION_PORT, Integer.toString (aSocket.getLocalPort ()));
    aMsg.setAttribute (AS2Message.ATTRIBUTE_RECEIVED, Boolean.TRUE.toString ());
    return aMsg;
  }

  protected void decrypt (@Nonnull final IMessage aMsg) throws OpenAS2Exception
  {
    final ICertificateFactory aCertFactory = m_aReceiverModule.getSession ().getCertificateFactory ();
    final ICryptoHelper aCryptoHelper = AS2Helper.getCryptoHelper ();

    try
    {
      final boolean bDisableDecrypt = aMsg.getPartnership ().isDisableDecrypt ();
      final boolean bMsgIsEncrypted = aCryptoHelper.isEncrypted (aMsg.getData ());
      final boolean bForceDecrypt = aMsg.getPartnership ().isForceDecrypt ();
      if (bMsgIsEncrypted && bDisableDecrypt)
      {
        s_aLogger.info ("Message claims to be encrypted but decryption is disabled" + aMsg.getLoggingText ());
      }
      else
        if (bMsgIsEncrypted || bForceDecrypt)
        {
          // Decrypt
          if (bForceDecrypt && !bMsgIsEncrypted)
            s_aLogger.info ("Forced decrypting" + aMsg.getLoggingText ());
          else
            if (s_aLogger.isDebugEnabled ())
              s_aLogger.debug ("Decrypting" + aMsg.getLoggingText ());

          final X509Certificate aReceiverCert = aCertFactory.getCertificate (aMsg,
                                                                             ECertificatePartnershipType.RECEIVER);
          final PrivateKey aReceiverKey = aCertFactory.getPrivateKey (aMsg, aReceiverCert);
          final MimeBodyPart aDecryptedData = aCryptoHelper.decrypt (aMsg.getData (),
                                                                     aReceiverCert,
                                                                     aReceiverKey,
                                                                     bForceDecrypt);
          aMsg.setData (aDecryptedData);
          // Remember that message was encrypted
          aMsg.setAttribute (AS2Message.ATTRIBUTE_RECEIVED_ENCRYPTED, Boolean.TRUE.toString ());
          s_aLogger.info ("Successfully decrypted incoming AS2 message" + aMsg.getLoggingText ());
        }
    }
    catch (final Exception ex)
    {
      s_aLogger.error ("Error decrypting " + aMsg.getLoggingText () + ": " + ex.getMessage ());
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
      final boolean bDisableVerify = aMsg.getPartnership ().isDisableVerify ();
      final boolean bMsgIsSigned = aCryptoHelper.isSigned (aMsg.getData ());
      final boolean bForceVerify = aMsg.getPartnership ().isForceVerify ();
      if (bMsgIsSigned && bDisableVerify)
      {
        s_aLogger.info ("Message claims to be signed but signature validation is disabled" + aMsg.getLoggingText ());
      }
      else
        if (bMsgIsSigned || bForceVerify)
        {
          if (bForceVerify && !bMsgIsSigned)
            s_aLogger.info ("Forced verify signature" + aMsg.getLoggingText ());
          else
            if (s_aLogger.isDebugEnabled ())
              s_aLogger.debug ("Verifying signature" + aMsg.getLoggingText ());

          final X509Certificate aSenderCert = aCertFactory.getCertificateOrNull (aMsg,
                                                                                 ECertificatePartnershipType.SENDER);
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
            bUseCertificateInBodyPart = m_aReceiverModule.getSession ().isCryptoVerifyUseCertificateInBodyPart ();
          }

          final MimeBodyPart aVerifiedData = aCryptoHelper.verify (aMsg.getData (),
                                                                   aSenderCert,
                                                                   bUseCertificateInBodyPart,
                                                                   bForceVerify);
          aMsg.setData (aVerifiedData);
          // Remember that message was signed and verified
          aMsg.setAttribute (AS2Message.ATTRIBUTE_RECEIVED_SIGNED, Boolean.TRUE.toString ());
          s_aLogger.info ("Successfully verified signature of incoming AS2 message" + aMsg.getLoggingText ());
        }
    }
    catch (final Exception ex)
    {
      s_aLogger.error ("Error verifying signature " + aMsg.getLoggingText () + ": " + ex.getMessage ());
      throw new DispositionException (DispositionType.createError ("integrity-check-failed"),
                                      AbstractActiveNetModule.DISP_VERIFY_SIGNATURE_FAILED,
                                      ex);
    }
  }

  protected void decompress (@Nonnull final IMessage aMsg) throws DispositionException
  {
    try
    {
      if (aMsg.getPartnership ().isDisableDecompress ())
      {
        s_aLogger.info ("Message claims to be compressed but decompression is disabled" + aMsg.getLoggingText ());
      }
      else
      {
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Decompressing a compressed AS2 message");

        final SMIMECompressed aCompressed = new SMIMECompressed (aMsg.getData ());
        // decompression step MimeBodyPart
        final MimeBodyPart aDecompressedPart = SMIMEUtil.toMimeBodyPart (aCompressed.getContent (new ZlibExpanderProvider ()));
        // Update the message object
        aMsg.setData (aDecompressedPart);
        // Remember that message was decompressed
        aMsg.setAttribute (AS2Message.ATTRIBUTE_RECEIVED_COMPRESSED, Boolean.TRUE.toString ());
        s_aLogger.info ("Successfully decompressed incoming AS2 message" + aMsg.getLoggingText ());
      }
    }
    catch (final Exception ex)
    {
      s_aLogger.error ("Error decompressing received message", ex);
      throw new DispositionException (DispositionType.createError ("unexpected-processing-error"),
                                      AbstractActiveNetModule.DISP_DECOMPRESSION_ERROR,
                                      ex);
    }
  }

  protected void sendSyncMDN (@Nonnull final String sClientInfo,
                              @Nonnull final IAS2HttpResponseHandler aResponseHandler,
                              @Nonnull final AS2Message aMsg,
                              @Nonnull final DispositionType aDisposition,
                              @Nonnull final String sText)
  {
    final boolean bMDNBlocked = aMsg.getPartnership ().isBlockErrorMDN ();
    if (!bMDNBlocked)
    {
      try
      {
        final IAS2Session aSession = m_aReceiverModule.getSession ();
        final IMessageMDN aMdn = AS2Helper.createMDN (aSession, aMsg, aDisposition, sText);

        if (aMsg.isRequestingAsynchMDN ())
        {
          // if asyncMDN requested, close connection and initiate separate MDN
          // send
          final InternetHeaders aHeaders = new InternetHeaders ();
          aHeaders.setHeader (CAS2Header.HEADER_CONTENT_LENGTH, Integer.toString (0));
          // Empty data
          final NonBlockingByteArrayOutputStream aData = new NonBlockingByteArrayOutputStream ();
          aResponseHandler.sendHttpResponse (HttpURLConnection.HTTP_OK, aHeaders, aData);

          s_aLogger.info ("Setup to send asynch MDN [" +
                          aDisposition.getAsString () +
                          "] " +
                          sClientInfo +
                          aMsg.getLoggingText ());

          // trigger explicit sending
          aSession.getMessageProcessor ().handle (IProcessorSenderModule.DO_SENDMDN, aMsg, null);
        }
        else
        {
          // otherwise, send sync MDN back on same connection
          s_aLogger.info ("Sending back sync MDN [" +
                          aDisposition.getAsString () +
                          "] " +
                          sClientInfo +
                          aMsg.getLoggingText ());

          // Get data and therefore content length for sync MDN
          final NonBlockingByteArrayOutputStream aData = new NonBlockingByteArrayOutputStream ();
          final MimeBodyPart aPart = aMdn.getData ();
          StreamHelper.copyInputStreamToOutputStream (aPart.getInputStream (), aData);
          aMdn.setHeader (CAS2Header.HEADER_CONTENT_LENGTH, Integer.toString (aData.getSize ()));

          // start HTTP response
          aResponseHandler.sendHttpResponse (HttpURLConnection.HTTP_OK, aMdn.getHeaders (), aData);

          // Save sent MDN for later examination
          try
          {
            aSession.getMessageProcessor ().handle (IProcessorStorageModule.DO_STOREMDN, aMsg, null);
          }
          catch (final ComponentNotFoundException ex)
          {
            // No message processor found
          }
          catch (final NoModuleException ex)
          {
            // No module found in message processor
          }
          s_aLogger.info ("sent MDN [" + aDisposition.getAsString () + "] " + sClientInfo + aMsg.getLoggingText ());
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
                                     @Nonnull final byte [] aMsgData,
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
        final ContentType aReceivedContentType = new ContentType (aMsg.getHeader (CAS2Header.HEADER_CONTENT_TYPE));
        final String sReceivedContentType = aReceivedContentType.toString ();

        final MimeBodyPart aReceivedPart = new MimeBodyPart ();
        aReceivedPart.setDataHandler (new DataHandler (new ByteArrayDataSource (aMsgData, sReceivedContentType, null)));

        // Header must be set AFTER the DataHandler!
        aReceivedPart.setHeader (CAS2Header.HEADER_CONTENT_TYPE, sReceivedContentType);
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
        aMsg.getPartnership ().setSenderAS2ID (sAS2From);

        final String sAS2To = aMsg.getAS2To ();
        aMsg.getPartnership ().setReceiverAS2ID (sAS2To);

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
        if (s_aLogger.isTraceEnabled ())
          s_aLogger.trace ("Decompressing received message before checking signature...");
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

        if (s_aLogger.isTraceEnabled ())
          if (aMsg.containsAttribute (AS2Message.ATTRIBUTE_RECEIVED_SIGNED))
            s_aLogger.trace ("Decompressing received message after verifying signature...");
          else
            s_aLogger.trace ("Decompressing received message after decryption...");
        decompress (aMsg);
        bIsDecompressed = true;
      }

      if (s_aLogger.isTraceEnabled ())
        try
        {
          s_aLogger.trace ("SMIME Decrypted Content-Disposition: " +
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
          s_aLogger.error ("Failed to trace message: " + aMsg, ex);
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
                                                                                                     StackTraceHelper.getStackAsString (ex),
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
                                        AbstractActiveNetModule.DISP_STORAGE_FAILED + "\n" + ex.getMessage (),
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
                                                                                                     StackTraceHelper.getStackAsString (ex),
                                        ex);
      }

      try
      {
        if (aMsg.isRequestingMDN ())
        {
          // Transmit a success MDN if requested
          sendSyncMDN (sClientInfo,
                       aResponseHandler,
                       aMsg,
                       DispositionType.createSuccess (),
                       AbstractActiveNetModule.DISP_SUCCESS);
        }
        else
        {
          // Just send a HTTP OK
          HTTPHelper.sendSimpleHTTPResponse (aResponseHandler, HttpURLConnection.HTTP_OK);
          s_aLogger.info ("sent HTTP OK " + sClientInfo + aMsg.getLoggingText ());
        }
      }
      catch (final Exception ex)
      {
        throw new WrappedOpenAS2Exception ("Error creating and returning MDN, message was stilled processed", ex);
      }
    }
    catch (final DispositionException ex)
    {
      sendSyncMDN (sClientInfo, aResponseHandler, aMsg, ex.getDisposition (), ex.getText ());
      m_aReceiverModule.handleError (aMsg, ex);
    }
    catch (final OpenAS2Exception ex)
    {
      m_aReceiverModule.handleError (aMsg, ex);
    }
  }

  public void handle (final AbstractActiveNetModule aOwner, @Nonnull final Socket aSocket)
  {
    final String sClientInfo = getClientInfo (aSocket);
    s_aLogger.info ("Incoming connection " + sClientInfo);

    final AS2Message aMsg = createMessage (aSocket);

    final IAS2HttpResponseHandler aResponseHandler = new AS2HttpResponseHandlerSocket (aSocket);

    // Time the transmission
    final StopWatch aSW = StopWatch.createdStarted ();
    byte [] aMsgData = null;
    try
    {
      // Read in the message request, headers, and data
      aMsgData = readAndDecodeHttpRequest (new AS2InputStreamProviderSocket (aSocket), aResponseHandler, aMsg);
    }
    catch (final Exception ex)
    {
      final NetException ne = new NetException (aSocket.getInetAddress (), aSocket.getPort (), ex);
      ne.terminate ();
    }

    aSW.stop ();

    if (aMsgData != null)
    {
      s_aLogger.info ("received " +
                      IOHelper.getTransferRate (aMsgData.length, aSW) +
                      " from " +
                      sClientInfo +
                      aMsg.getLoggingText ());

      handleIncomingMessage (sClientInfo, aMsgData, aMsg, aResponseHandler);
    }
  }
}
