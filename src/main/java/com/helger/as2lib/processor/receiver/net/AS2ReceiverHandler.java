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
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;

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
import com.helger.as2lib.message.CNetAttribute;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.partner.CPartnershipIDs;
import com.helger.as2lib.processor.NoModuleException;
import com.helger.as2lib.processor.receiver.AS2ReceiverModule;
import com.helger.as2lib.processor.receiver.AbstractNetModule;
import com.helger.as2lib.processor.sender.IProcessorSenderModule;
import com.helger.as2lib.processor.storage.IProcessorStorageModule;
import com.helger.as2lib.session.ComponentNotFoundException;
import com.helger.as2lib.session.IAS2Session;
import com.helger.as2lib.util.AS2Util;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.IOUtil;
import com.helger.as2lib.util.http.AS2HttpResponseHandlerSocket;
import com.helger.as2lib.util.http.AS2InputStreamProviderSocket;
import com.helger.as2lib.util.http.HTTPUtil;
import com.helger.as2lib.util.http.IAS2HttpResponseHandler;
import com.helger.as2lib.util.javamail.ByteArrayDataSource;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotations.Nonempty;
import com.helger.commons.io.streams.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.streams.StreamUtils;
import com.helger.commons.lang.StackTraceHelper;
import com.helger.commons.timing.StopWatch;

public class AS2ReceiverHandler implements INetModuleHandler
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

  @Nonnull
  @Nonempty
  public static String getClientInfo (@Nonnull final Socket aSocket)
  {
    return aSocket.getInetAddress ().getHostAddress () + ":" + aSocket.getPort ();
  }

  // Create a new message and record the source ip and port
  @Nonnull
  protected AS2Message createMessage (@Nonnull final Socket aSocket)
  {
    final AS2Message aMsg = new AS2Message ();
    aMsg.setAttribute (CNetAttribute.MA_SOURCE_IP, aSocket.getInetAddress ().toString ());
    aMsg.setAttribute (CNetAttribute.MA_SOURCE_PORT, Integer.toString (aSocket.getPort ()));
    aMsg.setAttribute (CNetAttribute.MA_DESTINATION_IP, aSocket.getLocalAddress ().toString ());
    aMsg.setAttribute (CNetAttribute.MA_DESTINATION_PORT, Integer.toString (aSocket.getLocalPort ()));
    return aMsg;
  }

  protected void decrypt (@Nonnull final IMessage aMsg) throws OpenAS2Exception
  {
    final ICertificateFactory aCertFactory = m_aReceiverModule.getSession ().getCertificateFactory ();
    final ICryptoHelper aCryptoHelper = AS2Util.getCryptoHelper ();

    try
    {
      if (aCryptoHelper.isEncrypted (aMsg.getData ()))
      {
        // Decrypt
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Decrypting" + aMsg.getLoggingText ());

        final X509Certificate aReceiverCert = aCertFactory.getCertificate (aMsg, ECertificatePartnershipType.RECEIVER);
        final PrivateKey aReceiverKey = aCertFactory.getPrivateKey (aMsg, aReceiverCert);
        final MimeBodyPart aDecryptedData = aCryptoHelper.decrypt (aMsg.getData (), aReceiverCert, aReceiverKey);
        aMsg.setData (aDecryptedData);
        // Ensure a valid content type
        new ContentType (aMsg.getData ().getContentType ());
      }
    }
    catch (final Exception ex)
    {
      s_aLogger.error ("Error decrypting " + aMsg.getLoggingText () + ": " + ex.getMessage ());
      throw new DispositionException (DispositionType.createError ("decryption-failed"),
                                      AbstractNetModule.DISP_DECRYPTION_ERROR,
                                      ex);
    }
  }

  protected void verify (@Nonnull final IMessage aMsg) throws OpenAS2Exception
  {
    final ICertificateFactory aCertFactory = m_aReceiverModule.getSession ().getCertificateFactory ();
    final ICryptoHelper aCryptoHelper = AS2Util.getCryptoHelper ();

    try
    {
      if (aCryptoHelper.isSigned (aMsg.getData ()))
      {
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Verifying signature" + aMsg.getLoggingText ());

        final X509Certificate aSenderCert = aCertFactory.getCertificateOrNull (aMsg, ECertificatePartnershipType.SENDER);
        final MimeBodyPart aVerifiedData = aCryptoHelper.verify (aMsg.getData (), aSenderCert);
        aMsg.setData (aVerifiedData);
      }
    }
    catch (final Exception ex)
    {
      s_aLogger.error ("Error verifying signature " + aMsg.getLoggingText () + ": " + ex.getMessage ());
      throw new DispositionException (DispositionType.createError ("integrity-check-failed"),
                                      AbstractNetModule.DISP_VERIFY_SIGNATURE_FAILED,
                                      ex);
    }
  }

  protected void sendMDN (@Nonnull final String sClientInfo,
                          @Nonnull final IAS2HttpResponseHandler aResponseHandler,
                          @Nonnull final AS2Message aMsg,
                          @Nonnull final DispositionType aDisposition,
                          @Nonnull final String sText)
  {
    final boolean bMDNBlocked = aMsg.getPartnership ().getAttribute (CPartnershipIDs.PA_BLOCK_ERROR_MDN) != null;
    if (!bMDNBlocked)
    {
      try
      {
        final IAS2Session aSession = m_aReceiverModule.getSession ();
        final IMessageMDN aMdn = AS2Util.createMDN (aSession, aMsg, aDisposition, sText);

        if (aMsg.isRequestingAsynchMDN ())
        {
          // if asyncMDN requested, close connection and initiate separate MDN
          // send
          final InternetHeaders aHeaders = new InternetHeaders ();
          aHeaders.setHeader (CAS2Header.HEADER_CONTENT_LENGTH, Integer.toString (0));
          // Empty data
          final NonBlockingByteArrayOutputStream aData = new NonBlockingByteArrayOutputStream ();
          aResponseHandler.sendHttpResponse (HttpURLConnection.HTTP_OK, aHeaders, aData);

          s_aLogger.info ("setup to send asynch MDN [" +
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

          // Get data and therefore content length for sync MDN
          final NonBlockingByteArrayOutputStream aData = new NonBlockingByteArrayOutputStream ();
          final MimeBodyPart aPart = aMdn.getData ();
          StreamUtils.copyInputStreamToOutputStream (aPart.getInputStream (), aData);
          aMdn.setHeader (CAS2Header.HEADER_CONTENT_LENGTH, Integer.toString (aData.size ()));

          // start HTTP response
          aResponseHandler.sendHttpResponse (HttpURLConnection.HTTP_OK, aMdn.getHeaders (), aData);

          // Save sent MDN for later examination
          try
          {
            aSession.getMessageProcessor ().handle (IProcessorStorageModule.DO_STOREMDN, aMsg, null);
          }
          catch (final ComponentNotFoundException ex)
          {
            // May be...
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

      // Put received data in a MIME body part
      try
      {
        /*
         * receivedPart = new MimeBodyPart(msg.getHeaders(), data);
         * msg.setData(receivedPart); receivedContentType = new
         * ContentType(receivedPart.getContentType());
         */
        final ContentType aReceivedContentType = new ContentType (aMsg.getHeader (CAS2Header.HEADER_CONTENT_TYPE));
        final String sReceivedContentType = aReceivedContentType.toString ();

        final MimeBodyPart aReceivedPart = new MimeBodyPart ();
        aReceivedPart.setDataHandler (new DataHandler (new ByteArrayDataSource (aMsgData, sReceivedContentType, null)));
        aReceivedPart.setHeader (CAS2Header.HEADER_CONTENT_TYPE, sReceivedContentType);
        aMsg.setData (aReceivedPart);
      }
      catch (final Exception ex)
      {
        throw new DispositionException (DispositionType.createError ("unexpected-processing-error"),
                                        AbstractNetModule.DISP_PARSING_MIME_FAILED,
                                        ex);
      }

      // Extract AS2 ID's from header, find the message's partnership and
      // update the message
      try
      {
        final String sAS2From = aMsg.getAS2From ();
        aMsg.getPartnership ().setSenderID (CPartnershipIDs.PID_AS2, sAS2From);

        final String sAS2To = aMsg.getAS2To ();
        aMsg.getPartnership ().setReceiverID (CPartnershipIDs.PID_AS2, sAS2To);

        // Fill all partnership attributes etc.
        aSession.getPartnershipFactory ().updatePartnership (aMsg, false);
      }
      catch (final OpenAS2Exception ex)
      {
        throw new DispositionException (DispositionType.createError ("authentication-failed"),
                                        AbstractNetModule.DISP_PARTNERSHIP_NOT_FOUND,
                                        ex);
      }

      // Decrypt and verify signature of the data, and attach data to the
      // message
      decrypt (aMsg);
      verify (aMsg);

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
                                        AbstractNetModule.DISP_VALIDATION_FAILED +
                                            "\n" +
                                            StackTraceHelper.getStackAsString (ex),
                                        ex);
      }

      // Store the received message
      try
      {
        aSession.getMessageProcessor ().handle (IProcessorStorageModule.DO_STORE, aMsg, null);
      }
      catch (final OpenAS2Exception ex)
      {
        throw new DispositionException (DispositionType.createError ("unexpected-processing-error"),
                                        AbstractNetModule.DISP_STORAGE_FAILED + "\n" + ex.getMessage (),
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
                                        AbstractNetModule.DISP_VALIDATION_FAILED +
                                            "\n" +
                                            StackTraceHelper.getStackAsString (ex),
                                        ex);
      }

      try
      {
        if (aMsg.isRequestingMDN ())
        {
          // Transmit a success MDN if requested
          sendMDN (sClientInfo,
                   aResponseHandler,
                   aMsg,
                   DispositionType.createSuccess (),
                   AbstractNetModule.DISP_SUCCESS);
        }
        else
        {
          // Just send a HTTP OK
          HTTPUtil.sendSimpleHTTPResponse (aResponseHandler, HttpURLConnection.HTTP_OK);
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
      sendMDN (sClientInfo, aResponseHandler, aMsg, ex.getDisposition (), ex.getText ());
      m_aReceiverModule.handleError (aMsg, ex);
    }
    catch (final OpenAS2Exception ex)
    {
      m_aReceiverModule.handleError (aMsg, ex);
    }
  }

  public void handle (final AbstractNetModule owner, @Nonnull final Socket aSocket)
  {
    final String sClientInfo = getClientInfo (aSocket);
    s_aLogger.info ("Incoming connection " + sClientInfo);

    final AS2Message aMsg = createMessage (aSocket);

    byte [] aMsgData = null;

    final IAS2HttpResponseHandler aResponseHandler = new AS2HttpResponseHandlerSocket (aSocket);

    // Time the transmission
    final StopWatch aSW = new StopWatch (true);

    // Read in the message request, headers, and data
    try
    {
      // Read HTTP request incl. headers
      aMsgData = HTTPUtil.readHttpRequest (new AS2InputStreamProviderSocket (aSocket), aResponseHandler, aMsg);
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
                      IOUtil.getTransferRate (aMsgData.length, aSW) +
                      " from " +
                      sClientInfo +
                      aMsg.getLoggingText ());

      handleIncomingMessage (sClientInfo, aMsgData, aMsg, aResponseHandler);
    }
  }
}
