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
package com.helger.as2lib.processor.storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedException;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.params.AbstractParameterParser;
import com.helger.as2lib.params.CompositeParameters;
import com.helger.as2lib.params.DateParameters;
import com.helger.as2lib.params.InvalidParameterException;
import com.helger.as2lib.params.MessageParameters;
import com.helger.as2lib.processor.receiver.AS2ReceiverModule;
import com.helger.as2lib.util.DispositionException;
import com.helger.as2lib.util.DispositionType;

public class MessageFileModule extends AbstractStorageModule
{
  public static final String PARAM_HEADER = "header";

  private static final Logger s_aLogger = LoggerFactory.getLogger (MessageFileModule.class);

  public void handle (final String action, final IMessage msg, final Map <String, Object> options) throws OpenAS2Exception
  {
    // store message content
    try
    {
      final File msgFile = getFile (msg, getParameterRequired (PARAM_FILENAME), action);
      final InputStream in = msg.getData ().getInputStream ();
      store (msgFile, in);
      s_aLogger.info ("stored message to " + msgFile.getAbsolutePath () + msg.getLoggingText ());
    }
    catch (final Exception e)
    {
      throw new DispositionException (new DispositionType ("automatic-action",
                                                           "MDN-sent-automatically",
                                                           "processed",
                                                           "Error",
                                                           "Error storing transaction"),
                                      AS2ReceiverModule.DISP_STORAGE_FAILED,
                                      e);
    }

    final String headerFilename = getParameterNotRequired (PARAM_HEADER);
    if (headerFilename != null)
    {
      try
      {
        final File headerFile = getFile (msg, headerFilename, action);
        final InputStream in = getHeaderStream (msg);
        store (headerFile, in);
        s_aLogger.info ("stored headers to " + headerFile.getAbsolutePath () + msg.getLoggingText ());
      }
      catch (final IOException ioe)
      {
        throw new WrappedException (ioe);
      }
    }
  }

  @Override
  protected String getModuleAction ()
  {
    return DO_STORE;
  }

  @Override
  protected String getFilename (final IMessage msg, final String fileParam, final String action) throws InvalidParameterException
  {
    final CompositeParameters compParams = new CompositeParameters (false).add ("date", new DateParameters ())
                                                                          .add ("msg", new MessageParameters (msg));

    return AbstractParameterParser.parse (fileParam, compParams);
  }

  protected InputStream getHeaderStream (final IMessage msg)
  {
    final StringBuilder headerBuf = new StringBuilder ();

    // write headers to the string buffer
    headerBuf.append ("Headers:\r\n");

    final Enumeration <?> headers = msg.getHeaders ().getAllHeaderLines ();
    while (headers.hasMoreElements ())
    {
      final String header = (String) headers.nextElement ();
      headerBuf.append (header).append ("\r\n");
    }

    headerBuf.append ("\r\n");

    // write attributes to the string buffer
    headerBuf.append ("Attributes:\r\n");

    for (final Map.Entry <String, String> attrEntry : msg.getAttributes ().entrySet ())
    {
      headerBuf.append (attrEntry.getKey ()).append (": ").append (attrEntry.getValue ()).append ("\r\n");
    }

    return new ByteArrayInputStream (headerBuf.toString ().getBytes ());
  }
}
