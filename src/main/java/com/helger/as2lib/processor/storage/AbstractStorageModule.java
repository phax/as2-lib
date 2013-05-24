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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.helger.as2lib.ISession;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.params.InvalidParameterException;
import com.helger.as2lib.processor.module.AbstractProcessorModule;
import com.helger.as2lib.util.IOUtil;
import com.phloc.commons.io.file.FilenameHelper;
import com.phloc.commons.io.streams.StreamUtils;

public abstract class AbstractStorageModule extends AbstractProcessorModule implements IProcessorStorageModule
{
  public static final String PARAM_FILENAME = "filename";
  public static final String PARAM_PROTOCOL = "protocol";
  public static final String PARAM_TEMPDIR = "tempdir";

  public boolean canHandle (final String action, final IMessage msg, final Map <String, Object> options)
  {
    if (!action.equals (getModuleAction ()))
      return false;

    final String modProtocol = getParameterNotRequired (PARAM_PROTOCOL);
    final String msgProtocol = msg.getProtocol ();
    if (modProtocol != null)
    {
      if (msgProtocol != null && msgProtocol.equals (modProtocol))
        return true;
      return false;
    }
    return true;
  }

  @Override
  public void initDynamicComponent (final ISession session, final Map <String, String> options) throws OpenAS2Exception
  {
    super.initDynamicComponent (session, options);
    getParameterRequired (PARAM_FILENAME);
  }

  protected abstract String getModuleAction ();

  /**
   * @since 2007-06-01
   * @param msg
   * @param fileParam
   * @param action
   * @return File
   * @throws IOException
   * @throws OpenAS2Exception
   */
  protected File getFile (final IMessage msg, final String fileParam, final String action) throws IOException,
                                                                                          OpenAS2Exception
  {
    final String filename = getFilename (msg, fileParam, action);

    // make sure the parent directories exist
    final File file = new File (filename);
    final File parentDir = file.getParentFile ();
    parentDir.mkdirs ();
    // don't overwrite existing files
    return IOUtil.getUnique (parentDir, FilenameHelper.getAsSecureValidFilename (file.getName ()));
  }

  protected abstract String getFilename (IMessage msg, String fileParam, String action) throws InvalidParameterException;

  protected void store (final File msgFile, final InputStream in) throws IOException
  {
    final String tempDirname = getParameterNotRequired (PARAM_TEMPDIR);
    if (tempDirname != null)
    {
      // write the data to a temporary directory first
      final File tempDir = IOUtil.getDirectoryFile (tempDirname);
      final String tempFilename = msgFile.getName ();
      final File tempFile = IOUtil.getUnique (tempDir, tempFilename);
      writeStream (in, tempFile);

      // copy the temp file over to the destination
      tempFile.renameTo (msgFile);
    }
    else
    {
      writeStream (in, msgFile);
    }
  }

  protected void writeStream (final InputStream in, final File destination) throws IOException
  {
    final FileOutputStream out = new FileOutputStream (destination);
    StreamUtils.copyInputStreamToOutputStreamAndCloseOS (in, out);
  }
}
