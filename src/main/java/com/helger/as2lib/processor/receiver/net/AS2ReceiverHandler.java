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
package com.helger.as2lib.processor.receiver.net;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.activation.DataHandler;
import javax.annotation.Nonnull;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.cert.ICertificateFactory;
import com.helger.as2lib.exception.DispositionException;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedException;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.CNetAttribute;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.partner.CPartnershipIDs;
import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.processor.receiver.AS2ReceiverModule;
import com.helger.as2lib.processor.receiver.AbstractNetModule;
import com.helger.as2lib.processor.sender.IProcessorSenderModule;
import com.helger.as2lib.processor.storage.IProcessorStorageModule;
import com.helger.as2lib.util.AS2Util;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.DispositionType;
import com.helger.as2lib.util.HTTPUtil;
import com.helger.as2lib.util.ICryptoHelper;
import com.helger.as2lib.util.IOUtil;
import com.helger.as2lib.util.javamail.ByteArrayDataSource;
import com.helger.commons.annotations.Nonempty;
import com.helger.commons.io.streams.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.streams.StreamUtils;
import com.helger.commons.timing.StopWatch;

public class AS2ReceiverHandler implements INetModuleHandler
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS2ReceiverHandler.class);

  private final AS2ReceiverModule m_aModule;

  public AS2ReceiverHandler (@Nonnull final AS2ReceiverModule aModule)
  {
    m_aModule = aModule;
  }

  @Nonnull
  @Nonempty
  public String getClientInfo (@Nonnull final Socket aSocket)
  {
    return " " + aSocket.getInetAddress ().getHostAddress () + " " + Integer.toString (aSocket.getPort ());
  }

  @Nonnull
  public AS2ReceiverModule getModule ()
  {
    return m_aModule;
  }

  public void handle (final AbstractNetModule owner, @Nonnull final Socket aSocket)
  {
    s_aLogger.info ("incoming connection" + getClientInfo (aSocket));

    final AS2Message aMsg = createMessage (aSocket);

    byte [] aData = null;

    // Time the transmission
    final StopWatch aSW = new StopWatch (true);

    // Read in the message request, headers, and data
    try
    {
      aData = HTTPUtil.readData (aSocket, aMsg);
    }
    catch (final Exception ex)
    {
      final NetException ne = new NetException (aSocket.getInetAddress (), aSocket.getPort (), ex);
      ne.terminate ();
    }

    aSW.stop ();

    if (aData != null)
    {
      s_aLogger.info ("received " +
                      IOUtil.getTransferRate (aData.length, aSW) +
                      " from" +
                      getClientInfo (aSocket) +
                      aMsg.getLoggingText ());

      // TODO store HTTP request, headers, and data to file in Received folder
      // -> use message-id for filename?
      try
      {
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
          aReceivedPart.setDataHandler (new DataHandler (new ByteArrayDataSource (aData, sReceivedContentType, null)));
          aReceivedPart.setHeader (CAS2Header.HEADER_CONTENT_TYPE, sReceivedContentType);
          aMsg.setData (aReceivedPart);
        }
        catch (final Exception ex)
        {
          throw new DispositionException (new DispositionType ("automatic-action",
                                                               "MDN-sent-automatically",
                                                               "processed",
                                                               "Error",
                                                               "unexpected-processing-error"),
                                          AS2ReceiverModule.DISP_PARSING_MIME_FAILED,
                                          ex);
        }

        // Extract AS2 ID's from header, find the message's partnership and
        // update the message
        try
        {
          aMsg.getPartnership ().setSenderID (CPartnershipIDs.PID_AS2, aMsg.getHeader (CAS2Header.HEADER_AS2_FROM));
          aMsg.getPartnership ().setReceiverID (CPartnershipIDs.PID_AS2, aMsg.getHeader (CAS2Header.HEADER_AS2_TO));
          getModule ().getSession ().getPartnershipFactory ().updatePartnership (aMsg, false);
        }
        catch (final OpenAS2Exception ex)
        {
          throw new DispositionException (new DispositionType ("automatic-action",
                                                               "MDN-sent-automatically",
                                                               "processed",
                                                               "Error",
                                                               "authentication-failed"),
                                          AS2ReceiverModule.DISP_PARTNERSHIP_NOT_FOUND,
                                          ex);
        }

        // Decrypt and verify signature of the data, and attach data to the
        // message
        decryptAndVerify (aMsg);

        // Process the received message
        try
        {
          getModule ().getSession ().getProcessor ().handle (IProcessorStorageModule.DO_STORE, aMsg, null);
        }
        catch (final OpenAS2Exception ex)
        {
          throw new DispositionException (new DispositionType ("automatic-action",
                                                               "MDN-sent-automatically",
                                                               "processed",
                                                               "Error",
                                                               "unexpected-processing-error"),
                                          AS2ReceiverModule.DISP_STORAGE_FAILED,
                                          ex);
        }

        // Transmit a success MDN if requested
        try
        {
          if (aMsg.isRequestingMDN ())
          {
            sendMDN (aSocket,
                     aMsg,
                     new DispositionType ("automatic-action", "MDN-sent-automatically", "processed"),
                     AS2ReceiverModule.DISP_SUCCESS);
          }
          else
          {
            final OutputStream aOS = StreamUtils.getBuffered (aSocket.getOutputStream ());
            try
            {
              HTTPUtil.sendHTTPResponse (aOS, HttpURLConnection.HTTP_OK, false);
            }
            finally
            {
              StreamUtils.close (aOS);
            }
            s_aLogger.info ("sent HTTP OK" + getClientInfo (aSocket) + aMsg.getLoggingText ());
          }
        }
        catch (final Exception ex)
        {
          throw new WrappedException ("Error creating and returning MDN, message was stilled processed", ex);
        }
      }
      catch (final DispositionException ex)
      {
        sendMDN (aSocket, aMsg, ex.getDisposition (), ex.getText ());
        getModule ().handleError (aMsg, ex);
      }
      catch (final OpenAS2Exception ex)
      {
        getModule ().handleError (aMsg, ex);
      }
    }
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

  protected final void decryptAndVerify (final IMessage aMsg) throws OpenAS2Exception
  {
    final ICertificateFactory aCertFactory = getModule ().getSession ().getCertificateFactory ();
    final ICryptoHelper aCryptoHelper = AS2Util.getCryptoHelper ();

    try
    {
      if (aCryptoHelper.isEncrypted (aMsg.getData ()))
      {
        // Decrypt
        s_aLogger.debug ("decrypting" + aMsg.getLoggingText ());

        final X509Certificate aReceiverCert = aCertFactory.getCertificate (aMsg, Partnership.PARTNERSHIP_TYPE_RECEIVER);
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
      throw new DispositionException (new DispositionType ("automatic-action",
                                                           "MDN-sent-automatically",
                                                           "processed",
                                                           "Error",
                                                           "decryption-failed"),
                                      AS2ReceiverModule.DISP_DECRYPTION_ERROR,
                                      ex);
    }

    try
    {
      if (aCryptoHelper.isSigned (aMsg.getData ()))
      {
        s_aLogger.debug ("verifying signature" + aMsg.getLoggingText ());

        final X509Certificate aSenderCert = aCertFactory.getCertificate (aMsg, Partnership.PARTNERSHIP_TYPE_SENDER);
        aMsg.setData (aCryptoHelper.verify (aMsg.getData (), aSenderCert));
      }
    }
    catch (final Exception ex)
    {
      s_aLogger.error ("Error verifying signature " + aMsg.getLoggingText () + ": " + ex.getMessage ());
      throw new DispositionException (new DispositionType ("automatic-action",
                                                           "MDN-sent-automatically",
                                                           "processed",
                                                           "Error",
                                                           "integrity-check-failed"),
                                      AS2ReceiverModule.DISP_VERIFY_SIGNATURE_FAILED,
                                      ex);
    }
  }

  protected void sendMDN (final Socket aSocket,
                          @Nonnull final AS2Message aMsg,
                          final DispositionType aDisposition,
                          final String sText)
  {
    final boolean bMdnBlocked = aMsg.getPartnership ().getAttribute (CPartnershipIDs.PA_BLOCK_ERROR_MDN) != null;
    if (!bMdnBlocked)
    {
      try
      {
        final IMessageMDN aMdn = AS2Util.createMDN (getModule ().getSession (), aMsg, aDisposition, sText);

        final OutputStream aOS = StreamUtils.getBuffered (aSocket.getOutputStream ());
        // if asyncMDN requested, close connection and initiate separate MDN
        // send
        if (aMsg.isRequestingAsynchMDN ())
        {
          HTTPUtil.sendHTTPResponse (aOS, HttpURLConnection.HTTP_OK, false);
          aOS.write ("Content-Length: 0\r\n\r\n".getBytes ());
          aOS.flush ();
          aOS.close ();
          s_aLogger.info ("setup to send asynch MDN [" +
                          aDisposition.toString () +
                          "]" +
                          getClientInfo (aSocket) +
                          aMsg.getLoggingText ());
          getModule ().getSession ().getProcessor ().handle (IProcessorSenderModule.DO_SENDMDN, aMsg, null);
          return;
        }

        // otherwise, send sync MDN back on same connection
        HTTPUtil.sendHTTPResponse (aOS, HttpURLConnection.HTTP_OK, true);

        // make sure to set the content-length header
        final NonBlockingByteArrayOutputStream aData = new NonBlockingByteArrayOutputStream ();
        final MimeBodyPart aPart = aMdn.getData ();
        StreamUtils.copyInputStreamToOutputStream (aPart.getInputStream (), aData);
        aMdn.setHeader (CAS2Header.HEADER_CONTENT_LENGTH, Integer.toString (aData.size ()));

        final Enumeration <?> aHeaders = aMdn.getHeaders ().getAllHeaderLines ();
        while (aHeaders.hasMoreElements ())
        {
          final String sHeader = (String) aHeaders.nextElement () + "\r\n";
          aOS.write (sHeader.getBytes ());
        }

        aOS.write ("\r\n".getBytes ());

        aData.writeTo (aOS);
        aOS.flush ();
        aOS.close ();

        // Save sent MDN for later examination
        getModule ().getSession ().getProcessor ().handle (IProcessorStorageModule.DO_STOREMDN, aMsg, null);
        s_aLogger.info ("sent MDN [" +
                        aDisposition.toString () +
                        "]" +
                        getClientInfo (aSocket) +
                        aMsg.getLoggingText ());
      }
      catch (final Exception ex)
      {
        final WrappedException we = new WrappedException ("Error sending MDN", ex);
        we.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
        we.terminate ();
      }
    }
  }
}
