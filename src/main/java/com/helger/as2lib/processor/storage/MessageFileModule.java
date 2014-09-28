/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2014 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.processor.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.disposition.DispositionException;
import com.helger.as2lib.disposition.DispositionType;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.params.AbstractParameterParser;
import com.helger.as2lib.params.CompositeParameters;
import com.helger.as2lib.params.DateParameters;
import com.helger.as2lib.params.InvalidParameterException;
import com.helger.as2lib.params.MessageParameters;
import com.helger.as2lib.processor.receiver.AbstractNetModule;
import com.helger.commons.io.streams.NonBlockingByteArrayInputStream;

/**
 * Store message content and optionally message headers and attributes to a file
 *
 * @author Philip Helger
 */
public class MessageFileModule extends AbstractStorageModule
{
  public static final String ATTR_HEADER = "header";

  private static final Logger s_aLogger = LoggerFactory.getLogger (MessageFileModule.class);

  public MessageFileModule ()
  {
    super (DO_STORE);
  }

  public void handle (@Nonnull final String sAction,
                      @Nonnull final IMessage aMsg,
                      @Nullable final Map <String, Object> aOptions) throws OpenAS2Exception
  {
    // store message content
    try
    {
      final File aMsgFile = getFile (aMsg, getAttributeAsStringRequired (ATTR_FILENAME), sAction);
      final InputStream aIS = aMsg.getData ().getInputStream ();
      store (aMsgFile, aIS);
      s_aLogger.info ("stored message to " + aMsgFile.getAbsolutePath () + aMsg.getLoggingText ());
    }
    catch (final Exception ex)
    {
      throw new DispositionException (new DispositionType ("automatic-action",
                                                           "MDN-sent-automatically",
                                                           "processed",
                                                           "Error",
                                                           "Error storing transaction"),
                                      AbstractNetModule.DISP_STORAGE_FAILED,
                                      ex);
    }

    // Store message headers and attributes
    final String sHeaderFilename = getAttributeAsString (ATTR_HEADER);
    if (sHeaderFilename != null)
    {
      try
      {
        final File aHeaderFile = getFile (aMsg, sHeaderFilename, sAction);
        final InputStream aIS = getHeaderStream (aMsg);
        store (aHeaderFile, aIS);
        s_aLogger.info ("stored headers to " + aHeaderFile.getAbsolutePath () + aMsg.getLoggingText ());
      }
      catch (final IOException ex)
      {
        throw WrappedOpenAS2Exception.wrap (ex);
      }
    }
  }

  @Override
  protected String getFilename (final IMessage aMsg, final String sFileParam, final String sAction) throws InvalidParameterException
  {
    final CompositeParameters aCompParams = new CompositeParameters (false).add ("date", new DateParameters ())
                                                                           .add ("msg", new MessageParameters (aMsg));
    return AbstractParameterParser.parse (sFileParam, aCompParams);
  }

  @Nonnull
  protected InputStream getHeaderStream (@Nonnull final IMessage aMsg)
  {
    final StringBuilder aSB = new StringBuilder ();

    // write headers to the string buffer
    aSB.append ("Headers:\r\n");

    final Enumeration <?> aHeaderLines = aMsg.getHeaders ().getAllHeaderLines ();
    while (aHeaderLines.hasMoreElements ())
    {
      final String sHeaderLine = (String) aHeaderLines.nextElement ();
      aSB.append (sHeaderLine).append ("\r\n");
    }

    aSB.append ("\r\n");

    // write attributes to the string buffer
    aSB.append ("Attributes:\r\n");
    for (final Map.Entry <String, String> attrEntry : aMsg.getAllAttributes ())
    {
      aSB.append (attrEntry.getKey ()).append (": ").append (attrEntry.getValue ()).append ("\r\n");
    }

    // TODO which charset?
    return new NonBlockingByteArrayInputStream (aSB.toString ().getBytes ());
  }
}
