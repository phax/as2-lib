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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.cert.ICertificateFactory;
import com.helger.as2lib.exception.DispositionException;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedException;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.AS2MessageMDN;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.partner.CPartnershipIDs;
import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.processor.receiver.AS2MDNReceiverModule;
import com.helger.as2lib.processor.receiver.AbstractNetModule;
import com.helger.as2lib.processor.storage.IProcessorStorageModule;
import com.helger.as2lib.util.AS2Util;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.DispositionType;
import com.helger.as2lib.util.HTTPUtil;
import com.helger.as2lib.util.javamail.ByteArrayDataSource;
import com.phloc.commons.io.streams.NonBlockingByteArrayOutputStream;
import com.phloc.commons.io.streams.StreamUtils;
import com.phloc.commons.string.StringParser;

public class AS2MDNReceiverHandler implements INetModuleHandler
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AS2MDNReceiverHandler.class);

  private final AS2MDNReceiverModule m_aModule;

  public AS2MDNReceiverHandler (final AS2MDNReceiverModule aModule)
  {
    super ();
    m_aModule = aModule;
  }

  public String getClientInfo (final Socket aSockt)
  {
    return " " + aSockt.getInetAddress ().getHostAddress () + " " + Integer.toString (aSockt.getPort ());
  }

  public AS2MDNReceiverModule getModule ()
  {
    return m_aModule;
  }

  public void handle (final AbstractNetModule aOwner, final Socket aSocket)
  {

    s_aLogger.info ("incoming connection" + " [" + getClientInfo (aSocket) + "]");

    final AS2Message aMsg = new AS2Message ();

    byte [] aData = null;

    // Read in the message request, headers, and data
    try
    {
      aData = HTTPUtil.readData (aSocket, aMsg);
      // Asynch MDN 2007-03-12
      // check if the requested URL is defined in attribute "as2_receipt_option"
      // in one of partnerships, if yes, then process incoming AsyncMDN
      s_aLogger.info ("incoming connection for receiving AsyncMDN" +
                      " [" +
                      getClientInfo (aSocket) +
                      "]" +
                      aMsg.getLoggingText ());

      final MimeBodyPart aReceivedPart = new MimeBodyPart (aMsg.getHeaders (), aData);
      aMsg.setData (aReceivedPart);
      ContentType aReceivedContentType = new ContentType (aReceivedPart.getContentType ());

      aReceivedContentType = new ContentType (aMsg.getHeader ("Content-Type"));

      // MimeBodyPart receivedPart = new MimeBodyPart();
      aReceivedPart.setDataHandler (new DataHandler (new ByteArrayDataSource (aData,
                                                                              aReceivedContentType.toString (),
                                                                              null)));
      aReceivedPart.setHeader ("Content-Type", aReceivedContentType.toString ());

      aMsg.setData (aReceivedPart);

      receiveMDN (aMsg, aData, aSocket.getOutputStream ());

    }
    catch (final Exception ex)
    {
      final NetException ne = new NetException (aSocket.getInetAddress (), aSocket.getPort (), ex);
      ne.terminate ();
    }
  }

  // Asynch MDN 2007-03-12
  /**
   * method for receiving & processing Async MDN sent from receiver.
   */
  protected final void receiveMDN (final AS2Message aMsg, final byte [] aData, final OutputStream aOS) throws OpenAS2Exception,
                                                                                                      IOException
  {
    try
    {
      // Create a MessageMDN and copy HTTP headers
      final IMessageMDN aMdn = new AS2MessageMDN (aMsg);
      // copy headers from msg to MDN from msg
      aMdn.setHeaders (aMsg.getHeaders ());
      final MimeBodyPart part = new MimeBodyPart (aMdn.getHeaders (), aData);
      aMsg.getMDN ().setData (part);

      // get the MDN partnership info
      aMdn.getPartnership ().setSenderID (CPartnershipIDs.PID_AS2, aMdn.getHeader (CAS2Header.HEADER_AS2_FROM));
      aMdn.getPartnership ().setReceiverID (CPartnershipIDs.PID_AS2, aMdn.getHeader (CAS2Header.HEADER_AS2_TO));
      getModule ().getSession ().getPartnershipFactory ().updatePartnership (aMdn, false);

      final ICertificateFactory aCertFactory = getModule ().getSession ().getCertificateFactory ();
      final X509Certificate aSenderCert = aCertFactory.getCertificate (aMdn, Partnership.PARTNERSHIP_TYPE_SENDER);

      AS2Util.parseMDN (aMsg, aSenderCert);

      // in order to name & save the mdn with the original AS2-From + AS2-To +
      // Message id.,
      // the 3 msg attributes have to be reset before calling MDNFileModule
      aMsg.getPartnership ().setReceiverID (CPartnershipIDs.PID_AS2, aMdn.getHeader (CAS2Header.HEADER_AS2_FROM));
      aMsg.getPartnership ().setSenderID (CPartnershipIDs.PID_AS2, aMdn.getHeader (CAS2Header.HEADER_AS2_TO));
      getModule ().getSession ().getPartnershipFactory ().updatePartnership (aMsg, false);
      aMsg.setMessageID (aMsg.getMDN ().getAttribute (AS2MessageMDN.MDNA_ORIG_MESSAGEID));
      getModule ().getSession ().getProcessor ().handle (IProcessorStorageModule.DO_STOREMDN, aMsg, null);

      // check if the mic (message integrity check) is correct

      if (checkAsyncMDN (aMsg))
        HTTPUtil.sendHTTPResponse (aOS, HttpURLConnection.HTTP_OK, false);
      else
        HTTPUtil.sendHTTPResponse (aOS, HttpURLConnection.HTTP_NOT_FOUND, false);

      final String sDisposition = aMsg.getMDN ().getAttribute (AS2MessageMDN.MDNA_DISPOSITION);
      try
      {
        new DispositionType (sDisposition).validate ();
      }
      catch (final DispositionException ex)
      {
        ex.setText (aMsg.getMDN ().getText ());

        if ((ex.getDisposition () != null) && ex.getDisposition ().isWarning ())
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
      HTTPUtil.sendHTTPResponse (aOS, HttpURLConnection.HTTP_BAD_REQUEST, false);
      throw ex;
    }
    catch (final Exception ex)
    {
      HTTPUtil.sendHTTPResponse (aOS, HttpURLConnection.HTTP_BAD_REQUEST, false);

      final WrappedException we = new WrappedException (ex);
      we.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      throw we;
    }
  }

  // Asynch MDN 2007-03-12
  /**
   * verify if the mic is matched.
   * 
   * @param msg
   * @return true if mdn processed
   */
  public boolean checkAsyncMDN (final AS2Message msg)
  {
    try
    {
      // get the returned mic from mdn object
      final String returnmic = msg.getMDN ().getAttribute (AS2MessageMDN.MDNA_MIC);

      // use original message id. to open the pending information file
      // from pendinginfo folder.
      final String ORIG_MESSAGEID = msg.getMDN ().getAttribute (AS2MessageMDN.MDNA_ORIG_MESSAGEID);
      final String pendinginfofile = getModule ().getSession ()
                                                 .getComponent ("processor")
                                                 .getAttributeAsString ("pendingmdninfo") +
                                     "/" +
                                     ORIG_MESSAGEID.substring (1, ORIG_MESSAGEID.length () - 1);
      final BufferedReader pendinginfo = new BufferedReader (new FileReader (pendinginfofile));

      // Get the original mic from the first line of pending information
      // file
      final String originalmic = pendinginfo.readLine ();

      // Get the original pending file from the second line of pending
      // information file
      final File fpendingfile = new File (pendinginfo.readLine ());
      pendinginfo.close ();

      final String disposition = msg.getMDN ().getAttribute (AS2MessageMDN.MDNA_DISPOSITION);

      s_aLogger.info ("received MDN [" + disposition + "]" + msg.getLoggingText ());
      /*
       * original code just did string compare - returnmic.equals(originalmic).
       * Sadly this is not good enough as the mic fields are
       * "base64string, algorithm" taken from a rfc822 style
       * Returned-Content-MIC header and rfc822 headers can contain spaces all
       * over the place. (not to mention comments!). Simple fix - delete all
       * spaces.
       */
      if (originalmic == null || !returnmic.replaceAll ("\\s+", "").equals (originalmic.replaceAll ("\\s+", "")))
      {
        s_aLogger.info ("mic not matched, original mic: " +
                        originalmic +
                        " return mic: " +
                        returnmic +
                        msg.getLoggingText ());
        return false;
      }

      // delete the pendinginfo & pending file if mic is matched
      s_aLogger.info ("mic is matched, mic: " + returnmic + msg.getLoggingText ());
      final File fpendinginfofile = new File (pendinginfofile);
      s_aLogger.info ("delete pendinginfo file : " +
                      fpendinginfofile.getName () +
                      " from pending folder : " +
                      getModule ().getSession ().getComponent ("processor").getAttributeAsString ("pendingmdn") +
                      msg.getLoggingText ());

      fpendinginfofile.delete ();

      s_aLogger.info ("delete pending file : " +
                      fpendingfile.getName () +
                      " from pending folder : " +
                      fpendingfile.getParent () +
                      msg.getLoggingText ());
      fpendingfile.delete ();
    }
    catch (final Exception e)
    {
      s_aLogger.error (e.getMessage (), e);
      return false;
    }
    return true;
  }

  // Copy headers from an Http connection to an InternetHeaders object
  protected static void copyHttpHeaders (final HttpURLConnection conn, final InternetHeaders headers)
  {
    for (final Map.Entry <String, List <String>> connHeader : conn.getHeaderFields ().entrySet ())
    {
      final String headerName = connHeader.getKey ();
      if (headerName != null)
        for (final String value : connHeader.getValue ())
        {
          if (headers.getHeader (headerName) == null)
            headers.setHeader (headerName, value);
          else
            headers.addHeader (headerName, value);
        }
    }
  }

  public void reparse (final AS2Message msg, final HttpURLConnection conn)
  {
    // Create a MessageMDN and copy HTTP headers
    final IMessageMDN mdn = new AS2MessageMDN (msg);
    copyHttpHeaders (conn, mdn.getHeaders ());

    // Receive the MDN data
    NonBlockingByteArrayOutputStream mdnStream = null;
    try
    {
      final InputStream connIn = conn.getInputStream ();
      mdnStream = new NonBlockingByteArrayOutputStream ();

      // Retrieve the message content
      final long nContentLength = StringParser.parseLong (mdn.getHeader ("Content-Length"), -1);
      if (nContentLength >= 0)
        StreamUtils.copyInputStreamToOutputStreamWithLimit (connIn, mdnStream, nContentLength);
      else
        StreamUtils.copyInputStreamToOutputStream (connIn, mdnStream);
    }
    catch (final IOException ioe)
    {
      s_aLogger.error (ioe.getMessage (), ioe);
    }

    MimeBodyPart part = null;
    if (mdnStream != null)
      try
      {
        part = new MimeBodyPart (mdn.getHeaders (), mdnStream.toByteArray ());
      }
      catch (final MessagingException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace ();
      }

    msg.getMDN ().setData (part);

    // get the MDN partnership info
    mdn.getPartnership ().setSenderID (CPartnershipIDs.PID_AS2, mdn.getHeader (CAS2Header.HEADER_AS2_FROM));
    mdn.getPartnership ().setReceiverID (CPartnershipIDs.PID_AS2, mdn.getHeader (CAS2Header.HEADER_AS2_TO));
  }

}
