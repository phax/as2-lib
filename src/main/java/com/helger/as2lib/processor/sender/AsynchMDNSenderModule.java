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

  public boolean canHandle (final String action, final IMessage msg, final Map <String, Object> options)
  {
    if (!action.equals (IProcessorSenderModule.DO_SENDMDN))
      return false;
    return msg instanceof AS2Message;
  }

  public void handle (final String action, final IMessage msg, final Map <String, Object> options) throws OpenAS2Exception
  {
    try
    {
      _sendAsyncMDN ((AS2Message) msg, options);
    }
    finally
    {
      s_aLogger.debug ("asynch mdn message sent");
    }
  }

  protected void updateHttpHeaders (final HttpURLConnection conn, final IMessage msg)
  {
    conn.setRequestProperty ("Connection", "close, TE");
    conn.setRequestProperty ("User-Agent", "OpenAS2 AsynchMDNSender");

    conn.setRequestProperty ("Date", DateUtil.formatDate ("EEE, dd MMM yyyy HH:mm:ss Z"));
    conn.setRequestProperty ("Message-ID", msg.getMessageID ());
    // make sure this is the encoding used in the msg, run TBF1
    conn.setRequestProperty ("Mime-Version", "1.0");
    conn.setRequestProperty ("Content-type", msg.getHeader ("Content-type"));
    conn.setRequestProperty (CAS2Header.AS2_VERSION, "1.1");
    conn.setRequestProperty ("Recipient-Address", msg.getHeader ("Recipient-Address"));
    conn.setRequestProperty (CAS2Header.AS2_TO, msg.getHeader (CAS2Header.AS2_TO));
    conn.setRequestProperty (CAS2Header.AS2_FROM, msg.getHeader (CAS2Header.AS2_FROM));
    conn.setRequestProperty ("Subject", msg.getHeader ("Subject"));
    conn.setRequestProperty ("From", msg.getHeader ("From"));
  }

  private void _sendAsyncMDN (final AS2Message msg, final Map <String, Object> options) throws OpenAS2Exception
  {
    s_aLogger.info ("Async MDN submitted" + msg.getLoggingText ());
    final DispositionType disposition = new DispositionType ("automatic-action", "MDN-sent-automatically", "processed");

    try
    {
      final IMessageMDN mdn = msg.getMDN ();

      // Create a HTTP connection
      final String url = msg.getAsyncMDNurl ();
      final HttpURLConnection conn = getConnection (url, true, true, false, "POST");

      try
      {
        s_aLogger.info ("connected to " + url + msg.getLoggingText ());

        conn.setRequestProperty ("Connection", "close, TE");
        conn.setRequestProperty ("User-Agent", "OpenAS2 AS2Sender");
        // Copy all the header from mdn to the RequestProperties of conn
        final Enumeration <?> headers = mdn.getHeaders ().getAllHeaders ();
        while (headers.hasMoreElements ())
        {
          final Header header = (Header) headers.nextElement ();
          String sHeaderValue = header.getValue ();
          sHeaderValue = sHeaderValue.replace ('\t', ' ');
          sHeaderValue = sHeaderValue.replace ('\n', ' ');
          sHeaderValue = sHeaderValue.replace ('\r', ' ');
          conn.setRequestProperty (header.getName (), sHeaderValue);
        }

        // Note: closing this stream causes connection abort errors on some AS2
        // servers
        final OutputStream messageOut = conn.getOutputStream ();

        // Transfer the data
        final InputStream messageIn = mdn.getData ().getInputStream ();
        final StopWatch aSW = new StopWatch (true);
        final long bytes = IOUtil.copy (messageIn, messageOut);
        aSW.stop ();
        s_aLogger.info ("transferred " + IOUtil.getTransferRate (bytes, aSW) + msg.getLoggingText ());

        // Check the HTTP Response code
        final int nResponseCode = conn.getResponseCode ();
        if (nResponseCode != HttpURLConnection.HTTP_OK &&
            nResponseCode != HttpURLConnection.HTTP_CREATED &&
            nResponseCode != HttpURLConnection.HTTP_ACCEPTED &&
            nResponseCode != HttpURLConnection.HTTP_PARTIAL &&
            nResponseCode != HttpURLConnection.HTTP_NO_CONTENT)
        {
          s_aLogger.error ("sent AsyncMDN [" + disposition.toString () + "] Fail " + msg.getLoggingText ());
          throw new HttpResponseException (url.toString (), nResponseCode, conn.getResponseMessage ());
        }

        s_aLogger.info ("sent AsyncMDN [" + disposition.toString () + "] OK " + msg.getLoggingText ());

        // log & store mdn into backup folder.
        ((ISession) options.get ("session")).getProcessor ().handle (IProcessorStorageModule.DO_STOREMDN, msg, null);
      }
      finally
      {
        conn.disconnect ();
      }
    }
    catch (final HttpResponseException hre)
    {
      // Resend if the HTTP Response has an error code
      hre.terminate ();
      resend (msg, hre);
    }
    catch (final IOException ioe)
    {
      // Resend if a network error occurs during transmission
      final WrappedException wioe = new WrappedException (ioe);
      wioe.addSource (OpenAS2Exception.SOURCE_MESSAGE, msg);
      wioe.terminate ();

      resend (msg, wioe);
    }
    catch (final Exception e)
    {
      // Propagate error if it can't be handled by a resend
      throw new WrappedException (e);
    }
  }

  protected void resend (final IMessage msg, final OpenAS2Exception cause) throws OpenAS2Exception
  {
    final Map <String, Object> options = new HashMap <String, Object> ();
    options.put (IProcessorResenderModule.OPTION_CAUSE, cause);
    options.put (IProcessorResenderModule.OPTION_INITIAL_SENDER, this);
    getSession ().getProcessor ().handle (IProcessorResenderModule.DO_RESEND, msg, options);
  }

}
