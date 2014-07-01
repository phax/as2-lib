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
package com.helger.as2lib.processor.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.exception.InvalidParameterException;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedException;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.params.AbstractParameterParser;
import com.helger.as2lib.params.CompositeParameters;
import com.helger.as2lib.params.DateParameters;
import com.helger.as2lib.params.MessageMDNParameters;
import com.phloc.commons.io.streams.NonBlockingByteArrayInputStream;

public class MDNFileModule extends AbstractStorageModule
{
  public void handle (@Nonnull final String sAction,
                      @Nonnull final IMessage aMsg,
                      @Nullable final Map <String, Object> aOptions) throws OpenAS2Exception
  {
    // store mdn data
    if (aMsg.getMDN () == null)
      throw new OpenAS2Exception ("Message has no MDN");

    try
    {
      final File aMdnFile = getFile (aMsg, getParameterRequired (PARAM_FILENAME), "");
      final InputStream aIS = getMDNStream (aMsg.getMDN ());
      store (aMdnFile, aIS);
    }
    catch (final IOException ex)
    {
      throw new WrappedException (ex);
    }
  }

  @Override
  protected String getModuleAction ()
  {
    return DO_STOREMDN;
  }

  @Override
  protected String getFilename (final IMessage aMsg, final String sFileParam, final String sAction) throws InvalidParameterException
  {
    final IMessageMDN aMdn = aMsg.getMDN ();
    final CompositeParameters aCompParams = new CompositeParameters (false).add ("date", new DateParameters ())
                                                                           .add ("mdn", new MessageMDNParameters (aMdn));
    return AbstractParameterParser.parse (sFileParam, aCompParams);
  }

  protected InputStream getMDNStream (final IMessageMDN aMdn)
  {
    final StringBuilder aMdnBuf = new StringBuilder ();

    // write headers to the string buffer
    aMdnBuf.append ("Headers:\r\n");

    final Enumeration <?> aHeaderLines = aMdn.getHeaders ().getAllHeaderLines ();
    while (aHeaderLines.hasMoreElements ())
    {
      final String sHeaderLine = (String) aHeaderLines.nextElement ();
      aMdnBuf.append (sHeaderLine).append ("\r\n");
    }

    aMdnBuf.append ("\r\n");

    // write attributes to the string buffer
    aMdnBuf.append ("Attributes:\r\n");
    for (final Map.Entry <String, String> aEntry : aMdn.getAllAttributes ())
    {
      aMdnBuf.append (aEntry.getKey ()).append (": ").append (aEntry.getValue ()).append ("\r\n");
    }
    // finally, write the MDN text
    aMdnBuf.append ("Text:\r\n").append (aMdn.getText ());

    return new NonBlockingByteArrayInputStream (aMdnBuf.toString ().getBytes ());
  }
}
