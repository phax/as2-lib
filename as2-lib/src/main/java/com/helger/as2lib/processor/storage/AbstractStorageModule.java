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
package com.helger.as2lib.processor.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;

import com.helger.as2lib.exception.AS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.params.InvalidParameterException;
import com.helger.as2lib.processor.module.AbstractProcessorModule;
import com.helger.as2lib.session.IAS2Session;
import com.helger.as2lib.util.AS2IOHelper;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.charset.CharsetHelper;
import com.helger.commons.collection.attr.IStringMap;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.system.SystemHelper;

public abstract class AbstractStorageModule extends AbstractProcessorModule implements IProcessorStorageModule
{
  public static final String ATTR_FILENAME = "filename";
  public static final String ATTR_PROTOCOL = "protocol";
  public static final String ATTR_TEMPDIR = "tempdir";
  public static final String ATTR_CHARSET = "charset";

  private final String m_sModuleAction;

  protected AbstractStorageModule (@Nonnull @Nonempty final String sModuleAction)
  {
    m_sModuleAction = ValueEnforcer.notEmpty (sModuleAction, "ModuleAction");
  }

  public final boolean canHandle (@Nonnull final String sAction,
                                  @Nonnull final IMessage aMsg,
                                  @Nullable final Map <String, Object> aOptions)
  {
    if (!sAction.equals (m_sModuleAction))
      return false;

    final String sModProtocol = attrs ().getAsString (ATTR_PROTOCOL);
    if (sModProtocol == null)
      return false;
    return sModProtocol.equals (aMsg.getProtocol ());
  }

  @Override
  public final void initDynamicComponent (@Nonnull final IAS2Session aSession,
                                          @Nullable final IStringMap aOptions) throws AS2Exception
  {
    super.initDynamicComponent (aSession, aOptions);
    getAttributeAsStringRequired (ATTR_FILENAME);
  }

  /**
   * @return The charset configured via {@link #ATTR_CHARSET} parameter or the
   *         system default. Never <code>null</code>.
   */
  @Nonnull
  protected Charset getCharset ()
  {
    final String sCharsetName = attrs ().getAsString (ATTR_CHARSET);
    final Charset aCharset = CharsetHelper.getCharsetFromNameOrNull (sCharsetName);
    return aCharset != null ? aCharset : SystemHelper.getSystemCharset ();
  }

  /**
   * @since 2007-06-01
   * @param aMsg
   *        The source message
   * @param sFileParam
   *        The parameter name including the filename
   * @param sAction
   *        Action name
   * @return File The {@link File} to be used
   * @throws IOException
   *         In case of IO error
   * @throws AS2Exception
   *         In case of error
   */
  protected File getFile (final IMessage aMsg, final String sFileParam, final String sAction) throws IOException,
                                                                                              AS2Exception
  {
    final String sFilename = getFilename (aMsg, sFileParam, sAction);

    // make sure the parent directories exist
    final File aFile = new File (sFilename);
    AS2IOHelper.getFileOperationManager ().createDirRecursiveIfNotExisting (aFile.getParentFile ());
    // don't overwrite existing files
    return AS2IOHelper.getUniqueFile (aFile.getParentFile (),
                                      FilenameHelper.getAsSecureValidFilename (aFile.getName ()));
  }

  protected abstract String getFilename (IMessage aMsg,
                                         String sFileParam,
                                         String sAction) throws InvalidParameterException;

  private static void _writeStreamToFile (@Nonnull @WillClose final InputStream aIS,
                                          @Nonnull final File aDestination) throws IOException
  {
    final FileOutputStream aOS = new FileOutputStream (aDestination);
    if (StreamHelper.copyInputStreamToOutputStreamAndCloseOS (aIS, aOS).isFailure ())
      throw new IOException ("Failed to write content to " + aDestination.getAbsolutePath ());
  }

  protected void store (@Nonnull final File aMsgFile, @Nonnull @WillClose final InputStream aIS) throws IOException
  {
    final String sTempDirname = attrs ().getAsString (ATTR_TEMPDIR);
    if (sTempDirname != null)
    {
      // write the data to a temporary directory first
      final File aTempDir = AS2IOHelper.getDirectoryFile (sTempDirname);
      final File aTempFile = AS2IOHelper.getUniqueFile (aTempDir, aMsgFile.getName ());
      _writeStreamToFile (aIS, aTempFile);

      // copy the temp file over to the destination
      if (AS2IOHelper.getFileOperationManager ().renameFile (aTempFile, aMsgFile).isFailure ())
        throw new IOException ("Rename from " +
                               aTempFile.getAbsolutePath () +
                               " to " +
                               aMsgFile.getAbsolutePath () +
                               " failed!");
    }
    else
    {
      // Write directly to the destination file
      _writeStreamToFile (aIS, aMsgFile);
    }
  }

  @Override
  public boolean equals (final Object o)
  {
    // New member, no change
    return super.equals (o);
  }

  @Override
  public int hashCode ()
  {
    // New member, no change
    return super.hashCode ();
  }
}
