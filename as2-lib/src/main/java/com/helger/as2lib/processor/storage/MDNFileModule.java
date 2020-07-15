/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2020 Philip Helger philip[at]helger[dot]com
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
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.AS2Exception;
import com.helger.as2lib.exception.WrappedAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.params.AS2InvalidParameterException;
import com.helger.as2lib.params.CompositeParameters;
import com.helger.as2lib.params.DateParameters;
import com.helger.as2lib.params.MessageMDNParameters;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.system.ENewLineMode;

/**
 * Store an MDN to a file
 *
 * @author Philip Helger
 */
public class MDNFileModule extends AbstractStorageModule
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MDNFileModule.class);

  public MDNFileModule ()
  {
    super (DO_STOREMDN);
  }

  public void handle (@Nonnull final String sAction,
                      @Nonnull final IMessage aMsg,
                      @Nullable final Map <String, Object> aOptions) throws AS2Exception
  {
    // store mdn data
    if (aMsg.getMDN () == null)
      throw new AS2Exception ("Message has no MDN");

    try
    {
      final File aMdnFile = getFile (aMsg, getAttributeAsStringRequired (ATTR_FILENAME));
      final InputStream aIS = getMDNStream (aMsg.getMDN ());
      store (aMdnFile, aIS);
      LOGGER.info ("stored MDN to " + aMdnFile.getAbsolutePath ());
    }
    catch (final IOException ex)
    {
      throw WrappedAS2Exception.wrap (ex);
    }
  }

  @Override
  protected String getFilename (@Nonnull final IMessage aMsg, @Nullable final String sFileParam) throws AS2InvalidParameterException
  {
    final IMessageMDN aMDN = aMsg.getMDN ();
    final CompositeParameters aCompParams = new CompositeParameters (false).add ("date", new DateParameters ())
                                                                           .add ("mdn", new MessageMDNParameters (aMDN));
    return aCompParams.format (sFileParam);
  }

  @Nonnull
  protected InputStream getMDNStream (@Nonnull final IMessageMDN aMdn)
  {
    final String sNewLine = ENewLineMode.DEFAULT.getText ();

    final StringBuilder aSB = new StringBuilder ();

    // write HTTP headers to the string buffer
    aSB.append ("MDN Headers:").append (sNewLine);

    // Should use ISO-8859-1 charset for HTTP headers
    aMdn.headers ().forEachHeaderLine (sHeaderLine -> aSB.append (sHeaderLine).append (sNewLine), true);

    // Empty line
    aSB.append (sNewLine);

    // write attributes to the string buffer
    aSB.append ("MDN Attributes:").append (sNewLine);
    for (final Map.Entry <String, String> aEntry : aMdn.attrs ().entrySet ())
    {
      aSB.append (aEntry.getKey ()).append (": ").append (aEntry.getValue ()).append (sNewLine);
    }

    // finally, write the MDN text
    aSB.append ("Text:").append (sNewLine).append (aMdn.getText ());

    return new NonBlockingByteArrayInputStream (aSB.toString ().getBytes (getCharset ()));
  }
}
