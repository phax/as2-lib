/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2019 Philip Helger philip[at]helger[dot]com
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
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.disposition.DispositionType;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.processor.NoModuleException;
import com.helger.as2lib.processor.storage.IProcessorStorageModule;
import com.helger.as2lib.session.ComponentNotFoundException;
import com.helger.as2lib.util.AS2IOHelper;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.dump.IHTTPOutgoingDumper;
import com.helger.as2lib.util.http.AS2HttpClient;
import com.helger.as2lib.util.http.AS2HttpHeaderSetter;
import com.helger.commons.http.CHttp;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.http.EHttpMethod;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.timing.StopWatch;

public class AsynchMDNSenderModule extends AbstractHttpSenderModule
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AsynchMDNSenderModule.class);

  public AsynchMDNSenderModule ()
  {}

  public boolean canHandle (@Nonnull final String sAction,
                            @Nonnull final IMessage aMsg,
                            @Nullable final Map <String, Object> aOptions)
  {
    return sAction.equals (IProcessorSenderModule.DO_SEND_ASYNC_MDN) && aMsg instanceof AS2Message;
  }

  private void _sendViaHTTP (@Nonnull final AS2Message aMsg,
                             @Nonnull final DispositionType aDisposition,
                             @Nullable final IHTTPOutgoingDumper aOutgoingDumper) throws OpenAS2Exception,
                                                                                  IOException,
                                                                                  MessagingException
  {
    final IMessageMDN aMdn = aMsg.getMDN ();

    // Create a HTTP connection
    final String sUrl = aMsg.getAsyncMDNurl ();
    final EHttpMethod eRequestMethod = EHttpMethod.POST;
    // MDN is a small message. We will always use CHttp
    final AS2HttpClient aConn = getHttpClient (sUrl, eRequestMethod, getSession ().getHttpProxy ());

    try
    {
      if (LOGGER.isInfoEnabled ())
        LOGGER.info ("connected to " + sUrl + aMsg.getLoggingText ());

      // Set all custom headers first (so that they are overridden with the
      // mandatory ones in here)
      // Use HttpHeaderMap and not String to ensure name casing is identical!
      final HttpHeaderMap aHeaderMap = aMdn.headers ().getClone ();

      aHeaderMap.setHeader (CHttpHeader.CONNECTION, CAS2Header.DEFAULT_CONNECTION);
      aHeaderMap.setHeader (CHttpHeader.USER_AGENT, CAS2Header.DEFAULT_USER_AGENT);

      final boolean bQuoteHeaderValues = attrs ().getAsBoolean (ATTR_QUOTE_HEADER_VALUES, DEFAULT_QUOTE_HEADER_VALUES);
      final AS2HttpHeaderSetter aHeaderSetter = new AS2HttpHeaderSetter (aConn, aOutgoingDumper, bQuoteHeaderValues);
      // Copy all the header from mdn to the RequestProperties of conn
      // Avoid double quoting
      aHeaderMap.forEachSingleHeader (aHeaderSetter::setHttpHeader, false);

      if (aOutgoingDumper != null)
        aOutgoingDumper.finishedHeaders ();

      // Note: closing this stream causes connection abort errors on some AS2
      // servers
      OutputStream aMsgOS = aConn.getOutputStream ();

      // This stream dumps the HTTP
      if (aOutgoingDumper != null)
        aMsgOS = aOutgoingDumper.getDumpOS (aMsgOS);

      // Transfer the data
      final InputStream aMessageIS = aMdn.getData ().getInputStream ();
      final StopWatch aSW = StopWatch.createdStarted ();
      final long nBytes = AS2IOHelper.copy (aMessageIS, aMsgOS);

      if (aOutgoingDumper != null)
        aOutgoingDumper.finishedPayload ();

      aSW.stop ();
      if (LOGGER.isInfoEnabled ())
        LOGGER.info ("transferred " + AS2IOHelper.getTransferRate (nBytes, aSW) + aMsg.getLoggingText ());

      // Check the HTTP Response code
      final int nResponseCode = aConn.getResponseCode ();
      if (nResponseCode != CHttp.HTTP_OK &&
          nResponseCode != CHttp.HTTP_CREATED &&
          nResponseCode != CHttp.HTTP_ACCEPTED &&
          nResponseCode != CHttp.HTTP_NO_CONTENT &&
          nResponseCode != CHttp.HTTP_PARTIAL_CONTENT)
      {
        if (LOGGER.isErrorEnabled ())
          LOGGER.error ("sent AsyncMDN [" + aDisposition.getAsString () + "] Fail " + aMsg.getLoggingText ());
        throw new HttpResponseException (sUrl, nResponseCode, aConn.getResponseMessage ());
      }

      if (LOGGER.isInfoEnabled ())
        LOGGER.info ("sent AsyncMDN [" + aDisposition.getAsString () + "] OK " + aMsg.getLoggingText ());

      // log & store mdn into backup folder.
      try
      {
        getSession ().getMessageProcessor ().handle (IProcessorStorageModule.DO_STOREMDN, aMsg, null);
      }
      catch (final ComponentNotFoundException | NoModuleException ex)
      {
        // No message processor found
        // Or no module found in message processor
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
    try
    {
      final AS2Message aMsg = (AS2Message) aBaseMsg;

      if (LOGGER.isInfoEnabled ())
        LOGGER.info ("Async MDN submitted" + aMsg.getLoggingText ());

      final DispositionType aDisposition = DispositionType.createSuccess ();

      final int nRetries = getRetryCount (aMsg.partnership (), aOptions);

      try (final IHTTPOutgoingDumper aOutgoingDumper = getHttpOutgoingDumper (aMsg))
      {
        _sendViaHTTP (aMsg, aDisposition, aOutgoingDumper);
      }
      catch (final HttpResponseException ex)
      {
        if (LOGGER.isErrorEnabled ())
          LOGGER.error ("Http Response Error " + ex.getMessage ());

        // Resend if the HTTP Response has an error code
        ex.terminate ();

        if (!doResend (IProcessorSenderModule.DO_SEND_ASYNC_MDN, aMsg, ex, nRetries))
          throw ex;
      }
      catch (final IOException ex)
      {
        // Resend if a network error occurs during transmission
        final OpenAS2Exception wioe = WrappedOpenAS2Exception.wrap (ex).setSourceMsg (aMsg).terminate ();

        if (!doResend (IProcessorSenderModule.DO_SEND_ASYNC_MDN, aMsg, wioe, nRetries))
          throw wioe;
      }
      catch (final Exception ex)
      {
        // Propagate error if it can't be handled by a resend
        throw WrappedOpenAS2Exception.wrap (ex);
      }
    }
    finally
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Async MDN message sent");
    }
  }
}
