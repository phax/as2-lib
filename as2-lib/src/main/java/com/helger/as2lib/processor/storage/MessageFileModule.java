/*
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2025 Philip Helger philip[at]helger[dot]com
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
import java.nio.charset.Charset;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.disposition.AS2DispositionException;
import com.helger.as2lib.disposition.DispositionType;
import com.helger.as2lib.exception.AS2Exception;
import com.helger.as2lib.exception.WrappedAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.params.AS2InvalidParameterException;
import com.helger.as2lib.params.CompositeParameters;
import com.helger.as2lib.params.DateParameters;
import com.helger.as2lib.params.MessageParameters;
import com.helger.as2lib.processor.receiver.AbstractActiveNetModule;
import com.helger.commons.http.CHttp;
import com.helger.commons.io.stream.StringInputStream;

/**
 * Store message content and optionally message headers and attributes to a file
 *
 * @author Philip Helger
 */
public class MessageFileModule extends AbstractStorageModule
{
  public static final String ATTR_HEADER = "header";

  private static final Logger LOGGER = LoggerFactory.getLogger (MessageFileModule.class);

  public MessageFileModule ()
  {
    super (DO_STORE);
  }

  @Nullable
  public final String getHeaderFilename ()
  {
    return attrs ().getAsString (ATTR_HEADER);
  }

  public final void setHeaderFilename (@Nullable final String sHeaderFilename)
  {
    if (sHeaderFilename == null)
      attrs ().remove (ATTR_HEADER);
    else
      attrs ().putIn (ATTR_HEADER, sHeaderFilename);
  }

  public void handle (@Nonnull final String sAction,
                      @Nonnull final IMessage aMsg,
                      @Nullable final Map <String, Object> aOptions) throws AS2Exception
  {
    // store message content
    try
    {
      final File aMsgFile = getFile (aMsg, getAttributeAsStringRequired (ATTR_FILENAME));
      try (final InputStream aIS = aMsg.getData ().getInputStream ())
      {
        store (aMsgFile, aIS);
      }
      aMsg.attrs ().put (MessageParameters.ATTR_STORED_FILE_NAME, aMsgFile.getAbsolutePath ());
      LOGGER.info ("stored message to " + aMsgFile.getAbsolutePath () + aMsg.getLoggingText ());
    }
    catch (final AS2DispositionException ex)
    {
      // Re-throw "as is"
      throw ex;
    }
    catch (final Exception ex)
    {
      throw AS2DispositionException.wrap (ex,
                                          () -> DispositionType.createError ("error-storing-transaction"),
                                          () -> AbstractActiveNetModule.DISP_STORAGE_FAILED);
    }

    // Store message headers and attributes
    final String sHeaderFilename = getHeaderFilename ();
    if (sHeaderFilename != null)
    {
      try
      {
        final File aHeaderFile = getFile (aMsg, sHeaderFilename);
        try (final InputStream aIS = getHeaderStream (aMsg, getCharset ()))
        {
          store (aHeaderFile, aIS);
        }
        LOGGER.info ("stored headers to " + aHeaderFile.getAbsolutePath () + aMsg.getLoggingText ());
      }
      catch (final IOException ex)
      {
        throw WrappedAS2Exception.wrap (ex);
      }
    }
  }

  @Override
  @Nonnull
  protected String getFilename (@Nonnull final IMessage aMsg,
                                @Nullable final String sFileParam) throws AS2InvalidParameterException
  {
    final CompositeParameters aCompParams = new CompositeParameters (false).add ("date", new DateParameters ())
                                                                           .add ("msg", new MessageParameters (aMsg));
    return aCompParams.format (sFileParam);
  }

  @Nonnull
  protected static InputStream getHeaderStream (@Nonnull final IMessage aMsg, @Nonnull final Charset aCharset)
  {
    final StringBuilder aSB = new StringBuilder ();

    // write headers and a trailing EOL to the string builder
    aSB.append ("Message Headers:").append (CHttp.EOL);
    aMsg.headers ().forEachHeaderLine (sHeaderLine -> aSB.append (sHeaderLine).append (CHttp.EOL), true);
    aSB.append (CHttp.EOL);

    // write attributes to the string buffer
    aSB.append ("Attributes:").append (CHttp.EOL);
    for (final Map.Entry <String, String> aEntry : aMsg.attrs ().entrySet ())
    {
      aSB.append (aEntry.getKey ()).append (": ").append (aEntry.getValue ()).append (CHttp.EOL);
    }

    return new StringInputStream (aSB.toString (), aCharset);
  }
}
