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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.mail.Header;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.ISession;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedException;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.processor.resender.IProcessorResenderModule;
import com.helger.as2lib.processor.storage.IProcessorStorageModule;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.DateUtil;
import com.helger.as2lib.util.DispositionType;
import com.helger.as2lib.util.IOUtil;
import com.phloc.commons.timing.StopWatch;

public class AsynchMDNSenderModule extends AbstractHttpSenderModule
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AsynchMDNSenderModule.class);

  public boolean canHandle (final String sAction, final IMessage aMsg, final Map <String, Object> aOptions)
  {
    if (!sAction.equals (IProcessorSenderModule.DO_SENDMDN))
      return false;
    return aMsg instanceof AS2Message;
  }

  public void handle (final String sAction, final IMessage aMsg, final Map <String, Object> aOptions) throws OpenAS2Exception
  {
    try
    {
      _sendAsyncMDN ((AS2Message) aMsg, aOptions);
    }
    finally
    {
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("asynch mdn message sent");
    }
  }

  protected void updateHttpHeaders (final HttpURLConnection aConn, final IMessage aMsg)
  {
    aConn.setRequestProperty ("Connection", "close, TE");
    aConn.setRequestProperty ("User-Agent", "OpenAS2 AsynchMDNSender");

    aConn.setRequestProperty ("Date", DateUtil.formatDate ("EEE, dd MMM yyyy HH:mm:ss Z"));
    aConn.setRequestProperty ("Message-ID", aMsg.getMessageID ());
    // make sure this is the encoding used in the msg, run TBF1
    aConn.setRequestProperty ("Mime-Version", "1.0");
    aConn.setRequestProperty ("Content-type", aMsg.getHeader ("Content-type"));
    aConn.setRequestProperty (CAS2Header.AS2_VERSION, "1.1");
    aConn.setRequestProperty ("Recipient-Address", aMsg.getHeader ("Recipient-Address"));
    aConn.setRequestProperty (CAS2Header.AS2_TO, aMsg.getHeader (CAS2Header.AS2_TO));
    aConn.setRequestProperty (CAS2Header.AS2_FROM, aMsg.getHeader (CAS2Header.AS2_FROM));
    aConn.setRequestProperty ("Subject", aMsg.getHeader ("Subject"));
    aConn.setRequestProperty ("From", aMsg.getHeader ("From"));
  }

  private void _sendAsyncMDN (final AS2Message aMsg, final Map <String, Object> aOptions) throws OpenAS2Exception
  {
    s_aLogger.info ("Async MDN submitted" + aMsg.getLoggingText ());
    final DispositionType aDisposition = new DispositionType ("automatic-action", "MDN-sent-automatically", "processed");

    try
    {
      final IMessageMDN aMdn = aMsg.getMDN ();

      // Create a HTTP connection
      final String sUrl = aMsg.getAsyncMDNurl ();
      final HttpURLConnection aConn = getConnection (sUrl, true, true, false, "POST");

      try
      {
        s_aLogger.info ("connected to " + sUrl + aMsg.getLoggingText ());

        aConn.setRequestProperty ("Connection", "close, TE");
        aConn.setRequestProperty ("User-Agent", "OpenAS2 AS2Sender");
        // Copy all the header from mdn to the RequestProperties of conn
        final Enumeration <?> aHeaders = aMdn.getHeaders ().getAllHeaders ();
        while (aHeaders.hasMoreElements ())
        {
          final Header aHeader = (Header) aHeaders.nextElement ();
          String sHeaderValue = aHeader.getValue ();
          sHeaderValue = sHeaderValue.replace ('\t', ' ');
          sHeaderValue = sHeaderValue.replace ('\n', ' ');
          sHeaderValue = sHeaderValue.replace ('\r', ' ');
          aConn.setRequestProperty (aHeader.getName (), sHeaderValue);
        }

        // Note: closing this stream causes connection abort errors on some AS2
        // servers
        final OutputStream aMessageOS = aConn.getOutputStream ();

        // Transfer the data
        final InputStream aMessageIS = aMdn.getData ().getInputStream ();
        final StopWatch aSW = new StopWatch (true);
        final long nBytes = IOUtil.copy (aMessageIS, aMessageOS);
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
          s_aLogger.error ("sent AsyncMDN [" + aDisposition.toString () + "] Fail " + aMsg.getLoggingText ());
          throw new HttpResponseException (sUrl.toString (), nResponseCode, aConn.getResponseMessage ());
        }

        s_aLogger.info ("sent AsyncMDN [" + aDisposition.toString () + "] OK " + aMsg.getLoggingText ());

        // log & store mdn into backup folder.
        ((ISession) aOptions.get ("session")).getProcessor ().handle (IProcessorStorageModule.DO_STOREMDN, aMsg, null);
      }
      finally
      {
        aConn.disconnect ();
      }
    }
    catch (final HttpResponseException ex)
    {
      // Resend if the HTTP Response has an error code
      ex.terminate ();
      resend (aMsg, ex);
    }
    catch (final IOException ex)
    {
      // Resend if a network error occurs during transmission
      final WrappedException wioe = new WrappedException (ex);
      wioe.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      wioe.terminate ();

      resend (aMsg, wioe);
    }
    catch (final Exception ex)
    {
      // Propagate error if it can't be handled by a resend
      throw new WrappedException (ex);
    }
  }

  protected void resend (final IMessage aMsg, final OpenAS2Exception aCause) throws OpenAS2Exception
  {
    final Map <String, Object> aOptions = new HashMap <String, Object> ();
    aOptions.put (IProcessorResenderModule.OPTION_CAUSE, aCause);
    aOptions.put (IProcessorResenderModule.OPTION_INITIAL_SENDER, this);
    getSession ().getProcessor ().handle (IProcessorResenderModule.DO_RESEND, aMsg, aOptions);
  }
}
