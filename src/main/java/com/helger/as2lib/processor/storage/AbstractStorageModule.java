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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;

import com.helger.as2lib.exception.InvalidParameterException;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.processor.module.AbstractProcessorModule;
import com.helger.as2lib.session.ISession;
import com.helger.as2lib.util.IOUtil;
import com.helger.as2lib.util.IStringMap;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotations.Nonempty;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.io.streams.StreamUtils;

public abstract class AbstractStorageModule extends AbstractProcessorModule implements IProcessorStorageModule
{
  public static final String ATTR_FILENAME = "filename";
  public static final String ATTR_PROTOCOL = "protocol";
  public static final String ATTR_TEMPDIR = "tempdir";

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

    final String sModProtocol = getAttributeAsString (ATTR_PROTOCOL);
    if (sModProtocol == null)
      return false;
    return sModProtocol.equals (aMsg.getProtocol ());
  }

  @Override
  public final void initDynamicComponent (@Nonnull final ISession aSession, @Nullable final IStringMap aOptions) throws OpenAS2Exception
  {
    super.initDynamicComponent (aSession, aOptions);
    getAttributeAsStringRequired (ATTR_FILENAME);
  }

  /**
   * @since 2007-06-01
   * @param aMsg
   * @param sFileParam
   * @param sAction
   * @return File
   * @throws IOException
   * @throws OpenAS2Exception
   */
  protected File getFile (final IMessage aMsg, final String sFileParam, final String sAction) throws IOException,
                                                                                             OpenAS2Exception
  {
    final String sFilename = getFilename (aMsg, sFileParam, sAction);

    // make sure the parent directories exist
    final File aFile = new File (sFilename);
    IOUtil.getFileOperationManager ().createDirRecursiveIfNotExisting (aFile.getParentFile ());
    // don't overwrite existing files
    return IOUtil.getUniqueFile (aFile.getParentFile (), FilenameHelper.getAsSecureValidFilename (aFile.getName ()));
  }

  protected abstract String getFilename (IMessage aMsg, String sFileParam, String sAction) throws InvalidParameterException;

  protected void store (final File aMsgFile, final InputStream aIS) throws IOException
  {
    final String sTempDirname = getAttributeAsString (ATTR_TEMPDIR);
    if (sTempDirname != null)
    {
      // write the data to a temporary directory first
      final File aTempDir = IOUtil.getDirectoryFile (sTempDirname);
      final File aTempFile = IOUtil.getUniqueFile (aTempDir, aMsgFile.getName ());
      writeStream (aIS, aTempFile);

      // copy the temp file over to the destination
      if (!aTempFile.renameTo (aMsgFile))
        throw new IOException ("Rename from " +
                               aTempFile.getAbsolutePath () +
                               " to " +
                               aMsgFile.getAbsolutePath () +
                               " failed!");
    }
    else
    {
      writeStream (aIS, aMsgFile);
    }
  }

  protected void writeStream (@Nonnull @WillClose final InputStream aIS, final File aDestination) throws IOException
  {
    final FileOutputStream aOS = new FileOutputStream (aDestination);
    if (StreamUtils.copyInputStreamToOutputStreamAndCloseOS (aIS, aOS).isFailure ())
      throw new IOException ("Failed to write content to " + aDestination.getAbsolutePath ());
  }
}
