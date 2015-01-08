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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.Header;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.disposition.DispositionType;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.processor.resender.IProcessorResenderModule;
import com.helger.as2lib.processor.storage.IProcessorStorageModule;
import com.helger.as2lib.session.ComponentNotFoundException;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.IOUtil;
import com.helger.commons.timing.StopWatch;

public class AsynchMDNSenderModule extends AbstractHttpSenderModule
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AsynchMDNSenderModule.class);

  public boolean canHandle (@Nonnull final String sAction,
                            @Nonnull final IMessage aMsg,
                            @Nullable final Map <String, Object> aOptions)
  {
    if (!sAction.equals (IProcessorSenderModule.DO_SENDMDN))
      return false;
    return aMsg instanceof AS2Message;
  }

  private void _resend (@Nonnull final IMessage aMsg, final OpenAS2Exception aCause) throws OpenAS2Exception
  {
    final Map <String, Object> aOptions = new HashMap <String, Object> ();
    aOptions.put (IProcessorResenderModule.OPTION_CAUSE, aCause);
    aOptions.put (IProcessorResenderModule.OPTION_INITIAL_SENDER, this);
    getSession ().getMessageProcessor ().handle (IProcessorResenderModule.DO_RESEND, aMsg, aOptions);
  }

  private void _sendAsyncMDN (@Nonnull final AS2Message aMsg) throws OpenAS2Exception
  {
    s_aLogger.info ("Async MDN submitted" + aMsg.getLoggingText ());
    final DispositionType aDisposition = DispositionType.createSuccess ();

    try
    {
      final IMessageMDN aMdn = aMsg.getMDN ();

      // Create a HTTP connection
      final String sUrl = aMsg.getAsyncMDNurl ();
      final HttpURLConnection aConn = getConnection (sUrl, true, true, false, "POST");

      try
      {
        s_aLogger.info ("connected to " + sUrl + aMsg.getLoggingText ());

        aConn.setRequestProperty (CAS2Header.HEADER_CONNECTION, CAS2Header.DEFAULT_CONNECTION);
        aConn.setRequestProperty (CAS2Header.HEADER_USER_AGENT, CAS2Header.DEFAULT_USER_AGENT);
        // Copy all the header from mdn to the RequestProperties of conn
        final Enumeration <?> aHeaders = aMdn.getHeaders ().getAllHeaders ();
        while (aHeaders.hasMoreElements ())
        {
          final Header aHeader = (Header) aHeaders.nextElement ();
          final String sHeaderValue = aHeader.getValue ().replace ('\t', ' ').replace ('\n', ' ').replace ('\r', ' ');
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
          s_aLogger.error ("sent AsyncMDN [" + aDisposition.getAsString () + "] Fail " + aMsg.getLoggingText ());
          throw new HttpResponseException (sUrl, nResponseCode, aConn.getResponseMessage ());
        }

        s_aLogger.info ("sent AsyncMDN [" + aDisposition.getAsString () + "] OK " + aMsg.getLoggingText ());

        // log & store mdn into backup folder.
        try
        {
          getSession ().getMessageProcessor ().handle (IProcessorStorageModule.DO_STOREMDN, aMsg, null);
        }
        catch (final ComponentNotFoundException ex)
        {
          // May be
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
      ex.terminate ();
      _resend (aMsg, ex);
    }
    catch (final IOException ex)
    {
      // Resend if a network error occurs during transmission
      final OpenAS2Exception wioe = WrappedOpenAS2Exception.wrap (ex);
      wioe.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      wioe.terminate ();

      _resend (aMsg, wioe);
    }
    catch (final Exception ex)
    {
      // Propagate error if it can't be handled by a resend
      throw WrappedOpenAS2Exception.wrap (ex);
    }
  }

  public void handle (@Nonnull final String sAction,
                      @Nonnull final IMessage aMsg,
                      @Nullable final Map <String, Object> aOptions) throws OpenAS2Exception
  {
    try
    {
      _sendAsyncMDN ((AS2Message) aMsg);
    }
    finally
    {
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Async MDN message sent");
    }
  }
}
