/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2018 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.util.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.mail.util.SharedFileInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.io.file.FilenameHelper;

/**
 * Stores the content of the input {@link InputStream} in a temporary file, and
 * opens {@link SharedFileInputStream} on that file. When the stream is closed,
 * the file will be deleted, and the input stream will be closed.
 */
public class TempSharedFileInputStream extends SharedFileInputStream
{
  private static final Logger LOGGER = LoggerFactory.getLogger (TempSharedFileInputStream.class);
  private final File tempFile;
  private final InputStream srcIS;
  private final int num = 0;

  private TempSharedFileInputStream (@Nonnull final File file, @Nonnull final InputStream is) throws IOException
  {
    super (file);
    srcIS = is;
    tempFile = file;
  }

  /**
   * Stores the content of the input {@link InputStream} in a temporary file (in
   * the system temporary directory, and opens {@link SharedFileInputStream} on
   * that file.
   *
   * @param aIS
   *        {@link InputStream} to read from
   * @param name
   *        name to use in the temporary file to link it to the delivered
   *        message. May be null
   * @return {@link TempSharedFileInputStream} on the created temporary file.
   * @throws IOException
   */
  static TempSharedFileInputStream getTempSharedFileInputStream (@Nonnull final InputStream aIS,
                                                                 final String name) throws IOException
  {
    final File aDest = storeContentToTempFile (aIS, name);
    return new TempSharedFileInputStream (aDest, aIS);
  }

  /**
   * Stores the content of the input {@link InputStream} in a temporary file (in
   * the system temporary directory.
   *
   * @param aIS
   *        {@link InputStream} to read from
   * @param name
   *        name to use in the temporary file to link it to the delivered
   *        message. May be null
   * @return The created {@link File}
   * @throws IOException
   */
  protected static File storeContentToTempFile (@Nonnull final InputStream aIS, final String name) throws IOException
  {
    // create temp file and write steam content to it
    // name may contain ":" on Windows and that would fail the tests!
    final String suffix = null == name ? "tmp" : FilenameHelper.getAsSecureValidASCIIFilename (name);
    final File aDest = File.createTempFile ("TempSharedFileInputStream", suffix);
    try (final FileOutputStream aOS = new FileOutputStream (aDest))
    {
      final long transferred = org.apache.commons.io.IOUtils.copyLarge (aIS, aOS);
      LOGGER.debug ("%l bytes copied to %s", transferred, aDest.getAbsolutePath ());
      return aDest;
    }
  }

  /**
   * close - Do nothing, to prevent early close, as the cryptographic processing
   * stages closes their input stream
   */
  @Override
  public void close () throws IOException
  {
    LOGGER.debug ("close() called, doing nothing.");
  }

  /**
   * finalize - closes also the input stream, and deletes the backing file
   */
  @Override
  public void finalize () throws IOException
  {
    try
    {
      super.finalize ();
      closeAll ();
    }
    catch (final Throwable t)
    {
      LOGGER.error ("Exception in finalize()", t);
      throw new IOException (t.getClass ().getName () + ":" + t.getMessage ());
    }
  }

  /**
   * closeAll - closes the input stream, and deletes the backing file
   */
  public void closeAll () throws IOException
  {
    srcIS.close ();
    super.close ();
    if (tempFile.exists ())
    {
      if (!tempFile.delete ())
      {
        LOGGER.error ("Failed to delete file {}", tempFile.getAbsolutePath ());
      }
    }
  }
}
