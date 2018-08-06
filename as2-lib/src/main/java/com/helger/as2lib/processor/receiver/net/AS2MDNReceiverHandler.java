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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.activation.DataSource;
import javax.annotation.Nonnull;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.cert.ECertificatePartnershipType;
import com.helger.as2lib.cert.ICertificateFactory;
import com.helger.as2lib.disposition.DispositionException;
import com.helger.as2lib.disposition.DispositionType;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.AS2MessageMDN;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.processor.NoModuleException;
import com.helger.as2lib.processor.receiver.AS2MDNReceiverModule;
import com.helger.as2lib.processor.receiver.AbstractActiveNetModule;
import com.helger.as2lib.processor.storage.IProcessorStorageModule;
import com.helger.as2lib.session.ComponentNotFoundException;
import com.helger.as2lib.util.AS2Helper;
import com.helger.as2lib.util.AS2HttpHelper;
import com.helger.as2lib.util.AS2IOHelper;
import com.helger.as2lib.util.dump.IHTTPIncomingDumper;
import com.helger.as2lib.util.http.AS2HttpResponseHandlerSocket;
import com.helger.as2lib.util.http.AS2InputStreamProviderSocket;
import com.helger.as2lib.util.http.HTTPHelper;
import com.helger.as2lib.util.http.IAS2HttpConnection;
import com.helger.as2lib.util.http.IAS2HttpResponseHandler;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.io.stream.NonBlockingBufferedReader;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.state.ETriState;
import com.helger.commons.string.StringParser;
import com.helger.mail.datasource.ByteArrayDataSource;

public class AS2MDNReceiverHandler extends AbstractReceiverHandler
{
  private static final String ATTR_PENDINGMDNINFO = "pendingmdninfo";
  private static final String ATTR_PENDINGMDN = "pendingmdn";

  private static final Logger LOGGER = LoggerFactory.getLogger (AS2MDNReceiverHandler.class);

  private final AS2MDNReceiverModule m_aModule;

  public AS2MDNReceiverHandler (@Nonnull final AS2MDNReceiverModule aModule)
  {
    m_aModule = ValueEnforcer.notNull (aModule, "Module");
  }

  @Nonnull
  public AS2MDNReceiverModule getModule ()
  {
    return m_aModule;
  }

  public void handle (@Nonnull final AbstractActiveNetModule aOwner, @Nonnull final Socket aSocket)
  {
    final String sClientInfo = getClientInfo (aSocket);

    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("incoming connection [" + sClientInfo + "]");

    final AS2Message aMsg = new AS2Message ();

    final IAS2HttpResponseHandler aResponseHandler = new AS2HttpResponseHandlerSocket (aSocket);

    byte [] aData = null;

    // Read in the message request, headers, and data
    try
    {
      final DataSource aDataSourceBody = readAndDecodeHttpRequest (new AS2InputStreamProviderSocket (aSocket),
                                                                   aResponseHandler,
                                                                   aMsg);
      aData = StreamHelper.getAllBytes (aDataSourceBody.getInputStream ());

      // Asynch MDN 2007-03-12
      // check if the requested URL is defined in attribute "as2_receipt_option"
      // in one of partnerships, if yes, then process incoming AsyncMDN
      if (LOGGER.isInfoEnabled ())
        LOGGER.info ("incoming connection for receiving AsyncMDN" + " [" + sClientInfo + "]" + aMsg.getLoggingText ());

      final ContentType aReceivedContentType = new ContentType (aMsg.getHeader (CHttpHeader.CONTENT_TYPE));
      final String sReceivedContentType = aReceivedContentType.toString ();

      final MimeBodyPart aReceivedPart = new MimeBodyPart (AS2HttpHelper.getAsInternetHeaders (aMsg.headers ()), aData);
      aMsg.setData (aReceivedPart);

      // MimeBodyPart receivedPart = new MimeBodyPart();
      aReceivedPart.setDataHandler (new ByteArrayDataSource (aData, sReceivedContentType, null).getAsDataHandler ());
      // Must be set AFTER the DataHandler!
      aReceivedPart.setHeader (CHttpHeader.CONTENT_TYPE, sReceivedContentType);

      aMsg.setData (aReceivedPart);

      receiveMDN (aMsg, aData, aResponseHandler);
    }
    catch (final Exception ex)
    {
      final NetException ne = new NetException (aSocket.getInetAddress (), aSocket.getPort (), ex);
      ne.terminate ();
    }
  }

  // Asynch MDN 2007-03-12
  /**
   * method for receiving and processing Async MDN sent from receiver.
   *
   * @param aMsg
   *        The MDN message
   * @param aData
   *        The MDN content
   * @param aResponseHandler
   *        The HTTP response handler for setting the correct HTTP response code
   * @throws OpenAS2Exception
   *         In case of error
   * @throws IOException
   *         In case of IO error
   */
  protected final void receiveMDN (@Nonnull final AS2Message aMsg,
                                   final byte [] aData,
                                   @Nonnull final IAS2HttpResponseHandler aResponseHandler) throws OpenAS2Exception,
                                                                                            IOException
  {
    try
    {
      // Create a MessageMDN and copy HTTP headers
      final IMessageMDN aMDN = new AS2MessageMDN (aMsg);
      // copy headers from msg to MDN from msg
      aMDN.headers ().setAllHeaders (aMsg.headers ());

      final MimeBodyPart aPart = new MimeBodyPart (AS2HttpHelper.getAsInternetHeaders (aMDN.headers ()), aData);
      aMsg.getMDN ().setData (aPart);

      // get the MDN partnership info
      aMDN.partnership ().setSenderAS2ID (aMDN.getHeader (CHttpHeader.AS2_FROM));
      aMDN.partnership ().setReceiverAS2ID (aMDN.getHeader (CHttpHeader.AS2_TO));
      // Set the appropriate keystore aliases
      aMDN.partnership ().setSenderX509Alias (aMsg.partnership ().getReceiverX509Alias ());
      aMDN.partnership ().setReceiverX509Alias (aMsg.partnership ().getSenderX509Alias ());
      // Update the partnership
      getModule ().getSession ().getPartnershipFactory ().updatePartnership (aMDN, false);

      final ICertificateFactory aCertFactory = getModule ().getSession ().getCertificateFactory ();
      final X509Certificate aSenderCert = aCertFactory.getCertificate (aMDN, ECertificatePartnershipType.SENDER);

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
        bUseCertificateInBodyPart = getModule ().getSession ().isCryptoVerifyUseCertificateInBodyPart ();
      }

      AS2Helper.parseMDN (aMsg, aSenderCert, bUseCertificateInBodyPart);

      // in order to name & save the mdn with the original AS2-From + AS2-To +
      // Message id.,
      // the 3 msg attributes have to be reset before calling MDNFileModule
      aMsg.partnership ().setSenderAS2ID (aMDN.getHeader (CHttpHeader.AS2_TO));
      aMsg.partnership ().setReceiverAS2ID (aMDN.getHeader (CHttpHeader.AS2_FROM));
      getModule ().getSession ().getPartnershipFactory ().updatePartnership (aMsg, false);
      aMsg.setMessageID (aMsg.getMDN ().attrs ().getAsString (AS2MessageMDN.MDNA_ORIG_MESSAGEID));
      try
      {
        getModule ().getSession ().getMessageProcessor ().handle (IProcessorStorageModule.DO_STOREMDN, aMsg, null);
      }
      catch (final ComponentNotFoundException | NoModuleException ex)
      {
        // No message processor found
        // Or no module found in message processor
      }

      // check if the mic (message integrity check) is correct
      if (checkAsyncMDN (aMsg))
        HTTPHelper.sendSimpleHTTPResponse (aResponseHandler, HttpURLConnection.HTTP_OK);
      else
        HTTPHelper.sendSimpleHTTPResponse (aResponseHandler, HttpURLConnection.HTTP_NOT_FOUND);

      final String sDisposition = aMsg.getMDN ().attrs ().getAsString (AS2MessageMDN.MDNA_DISPOSITION);
      try
      {
        DispositionType.createFromString (sDisposition).validate ();
      }
      catch (final DispositionException ex)
      {
        ex.setText (aMsg.getMDN ().getText ());
        if (ex.getDisposition ().isWarning ())
        {
          // Warning
          ex.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
          ex.terminate ();
        }
        else
        {
          // Error
          throw ex;
        }
      }
    }
    catch (final IOException ex)
    {
      HTTPHelper.sendSimpleHTTPResponse (aResponseHandler, HttpURLConnection.HTTP_BAD_REQUEST);
      throw ex;
    }
    catch (final Exception ex)
    {
      HTTPHelper.sendSimpleHTTPResponse (aResponseHandler, HttpURLConnection.HTTP_BAD_REQUEST);

      final OpenAS2Exception we = WrappedOpenAS2Exception.wrap (ex);
      we.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      throw we;
    }
  }

  // Asynch MDN 2007-03-12
  /**
   * verify if the mic is matched.
   *
   * @param aMsg
   *        Message
   * @return true if mdn processed
   */
  public boolean checkAsyncMDN (final AS2Message aMsg)
  {
    try
    {
      // get the returned mic from mdn object
      final String sReturnMIC = aMsg.getMDN ().attrs ().getAsString (AS2MessageMDN.MDNA_MIC);

      // use original message id. to open the pending information file
      // from pendinginfo folder.
      final String sOrigMessageID = aMsg.getMDN ().attrs ().getAsString (AS2MessageMDN.MDNA_ORIG_MESSAGEID);
      final String sPendingInfoFile = getModule ().getSession ()
                                                  .getMessageProcessor ()
                                                  .getAsString (ATTR_PENDINGMDNINFO) +
                                      "/" +
                                      AS2IOHelper.getFilenameFromMessageID (sOrigMessageID);

      String sOriginalMIC;
      File aPendingFile;
      try (final NonBlockingBufferedReader aPendingInfoReader = new NonBlockingBufferedReader (new FileReader (sPendingInfoFile)))
      {
        // Get the original mic from the first line of pending information
        // file
        sOriginalMIC = aPendingInfoReader.readLine ();

        // Get the original pending file from the second line of pending
        // information file
        aPendingFile = new File (aPendingInfoReader.readLine ());
      }

      final String sDisposition = aMsg.getMDN ().attrs ().getAsString (AS2MessageMDN.MDNA_DISPOSITION);

      if (LOGGER.isInfoEnabled ())
        LOGGER.info ("received MDN [" + sDisposition + "]" + aMsg.getLoggingText ());

      /*
       * original code just did string compare - returnmic.equals(originalmic). Sadly
       * this is not good enough as the mic fields are "base64string, algorithm" taken
       * from a rfc822 style Returned-Content-MIC header and rfc822 headers can
       * contain spaces all over the place. (not to mention comments!). Simple fix -
       * delete all spaces.
       */
      if (sOriginalMIC == null || !sReturnMIC.replaceAll ("\\s+", "").equals (sOriginalMIC.replaceAll ("\\s+", "")))
      {
        if (LOGGER.isInfoEnabled ())
          LOGGER.info ("MIC IS NOT MATCHED, original mic: " +
                       sOriginalMIC +
                       " return mic: " +
                       sReturnMIC +
                       aMsg.getLoggingText ());
        return false;
      }

      // delete the pendinginfo & pending file if mic is matched
      if (LOGGER.isInfoEnabled ())
        LOGGER.info ("mic is matched, mic: " + sReturnMIC + aMsg.getLoggingText ());

      final File aPendingInfoFile = new File (sPendingInfoFile);
      if (LOGGER.isInfoEnabled ())
        LOGGER.info ("delete pendinginfo file : " +
                     aPendingInfoFile.getName () +
                     " from pending folder : " +
                     getModule ().getSession ().getMessageProcessor ().getAsString (ATTR_PENDINGMDN) +
                     aMsg.getLoggingText ());
      if (!aPendingInfoFile.delete ())
      {
        if (LOGGER.isErrorEnabled ())
          LOGGER.error ("Error delete pendinginfo file " + aPendingFile);
      }

      if (LOGGER.isInfoEnabled ())
        LOGGER.info ("delete pending file : " +
                     aPendingFile.getName () +
                     " from pending folder : " +
                     aPendingFile.getParent () +
                     aMsg.getLoggingText ());
      if (!aPendingFile.delete ())
      {
        if (LOGGER.isErrorEnabled ())
          LOGGER.error ("Error delete pending file " + aPendingFile);
      }
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error checking async MDN", ex);
      return false;
    }
    return true;
  }

  public void reparse (@Nonnull final AS2Message aMsg, final IAS2HttpConnection aConn) throws OpenAS2Exception
  {
    // Create a MessageMDN and copy HTTP headers
    final IMessageMDN aMDN = new AS2MessageMDN (aMsg);
    if (true)
    {
      // Workaround for #48
      for (final Map.Entry <String, ICommonsList <String>> aEntry : aConn.getHeaderFields ())
        for (final String sValue : aEntry.getValue ())
          aMDN.headers ().addHeader (aEntry.getKey (), sValue);
    }
    else
    {
      // Bug in ph-commons 9.1.3 :(
      aMDN.headers ().addAllHeaders (aConn.getHeaderFields ());
    }

    // Receive the MDN data
    NonBlockingByteArrayOutputStream aMDNStream = null;
    try
    {
      final InputStream aIS = aConn.getInputStream ();
      aMDNStream = new NonBlockingByteArrayOutputStream ();

      // Retrieve the message content
      final long nContentLength = StringParser.parseLong (aMDN.getHeader (CHttpHeader.CONTENT_LENGTH), -1);
      if (nContentLength >= 0)
        StreamHelper.copyInputStreamToOutputStreamWithLimit (aIS, aMDNStream, nContentLength);
      else
        StreamHelper.copyInputStreamToOutputStream (aIS, aMDNStream);
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Error reparsing", ex);
    }
    finally
    {
      StreamHelper.close (aMDNStream);
    }

    final IHTTPIncomingDumper aIncomingDumper = HTTPHelper.getHTTPIncomingDumper ();
    if (aIncomingDumper != null)
      aIncomingDumper.dumpIncomingRequest (aMDN.headers ().getAllHeaderLines (),
                                           aMDNStream != null ? aMDNStream.toByteArray ()
                                                              : ArrayHelper.EMPTY_BYTE_ARRAY,
                                           aMDN);

    MimeBodyPart aPart = null;
    if (aMDNStream != null)
      try
      {
        aPart = new MimeBodyPart (AS2HttpHelper.getAsInternetHeaders (aMDN.headers ()), aMDNStream.toByteArray ());
      }
      catch (final MessagingException ex)
      {
        LOGGER.error ("Error creating MimeBodyPart", ex);
      }
    aMsg.getMDN ().setData (aPart);

    // get the MDN partnership info
    aMDN.partnership ().setSenderAS2ID (aMDN.getHeader (CHttpHeader.AS2_FROM));
    aMDN.partnership ().setReceiverAS2ID (aMDN.getHeader (CHttpHeader.AS2_TO));
  }
}
