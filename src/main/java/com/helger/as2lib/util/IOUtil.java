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
package com.helger.as2lib.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;

import com.helger.as2lib.exception.InvalidMessageException;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.commons.CGlobal;
import com.helger.commons.annotations.Nonempty;
import com.helger.commons.io.file.FileIOError;
import com.helger.commons.io.file.FileOperationManager;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.io.file.LoggingFileOperationCallback;
import com.helger.commons.io.streams.StreamUtils;
import com.helger.commons.mutable.MutableLong;
import com.helger.commons.timing.StopWatch;

@Immutable
public final class IOUtil
{
  private static final FileOperationManager s_aFOM = new FileOperationManager (new LoggingFileOperationCallback ());

  private IOUtil ()
  {}

  @Nonnull
  public static FileOperationManager getFileOperationManager ()
  {
    return s_aFOM;
  }

  @Nonnegative
  public static long copy (@WillClose final InputStream aIS, @WillNotClose final OutputStream aOS)
  {
    final MutableLong aML = new MutableLong ();
    StreamUtils.copyInputStreamToOutputStream (aIS, aOS, aML);
    return aML.longValue ();
  }

  @Nonnull
  public static File getDirectoryFile (@Nonnull final String sDirectory)
  {
    final File aDir = new File (sDirectory);
    s_aFOM.createDirRecursiveIfNotExisting (aDir);
    return aDir;
  }

  @Nonnull
  @Nonempty
  public static String getTransferRate (final long nBytes, @Nonnull final StopWatch aSW)
  {
    final StringBuilder aSB = new StringBuilder ();
    aSB.append (nBytes).append (" bytes in ").append (aSW.getMillis () / 1000.0).append (" seconds at ");

    final long nMillis = aSW.getMillis ();
    if (nMillis != 0)
    {
      final double dSeconds = nMillis / 1000.0;
      final long nBytesPerSecond = Math.round (nBytes / dSeconds);
      aSB.append (_getTransferRate (nBytesPerSecond));
    }
    else
    {
      aSB.append (_getTransferRate (nBytes));
    }

    return aSB.toString ();
  }

  @Nonnull
  @Nonempty
  private static String _getTransferRate (final long nBytesPerSecond)
  {
    final StringBuilder aSB = new StringBuilder ();
    if (nBytesPerSecond < CGlobal.BYTES_PER_KILOBYTE)
    {
      // < 1024
      aSB.append (nBytesPerSecond).append (" Bps");
    }
    else
    {
      final long nKBytesPerSecond = nBytesPerSecond / CGlobal.BYTES_PER_KILOBYTE;
      if (nKBytesPerSecond < CGlobal.BYTES_PER_KILOBYTE)
      {
        // < 1048576
        aSB.append (nKBytesPerSecond)
           .append (".")
           .append (nBytesPerSecond % CGlobal.BYTES_PER_KILOBYTE)
           .append (" KBps");
      }
      else
      {
        // >= 1048576
        aSB.append (nKBytesPerSecond / CGlobal.BYTES_PER_KILOBYTE)
           .append (".")
           .append (nKBytesPerSecond % CGlobal.BYTES_PER_KILOBYTE)
           .append (" MBps");
      }
    }
    return aSB.toString ();
  }

  @Nonnull
  public static File getUniqueFile (@Nonnull final File aDir, @Nullable final String sFilename)
  {
    int nCounter = -1;
    final String sBaseFilename = FilenameHelper.getAsSecureValidFilename (sFilename);
    while (true)
    {
      final File aTest = new File (aDir, nCounter == -1 ? sBaseFilename : sBaseFilename +
                                                                          "." +
                                                                          Integer.toString (nCounter));
      if (!aTest.exists ())
        return aTest;

      nCounter++;
    }
  }

  // move the file to an error directory
  public static void handleError (@Nonnull final File aFile, @Nonnull final String sErrorDirectory) throws OpenAS2Exception
  {
    File aDestFile = null;

    try
    {
      final File aErrorDir = getDirectoryFile (sErrorDirectory);
      aDestFile = new File (aErrorDir, aFile.getName ());

      // move the file
      aDestFile = moveFile (aFile, aDestFile, false, true);
    }
    catch (final IOException ex)
    {
      final InvalidMessageException im = new InvalidMessageException ("Failed to move " +
                                                                      aFile.getAbsolutePath () +
                                                                      " to error directory " +
                                                                      aDestFile.getAbsolutePath ());
      im.initCause (ex);
      throw im;
    }

    // make sure an error of this event is logged
    final InvalidMessageException ex = new InvalidMessageException ("Moved " +
                                                                    aFile.getAbsolutePath () +
                                                                    " to " +
                                                                    aDestFile.getAbsolutePath ());
    ex.terminate ();
  }

  @Nonnull
  public static File moveFile (@Nonnull final File aSrc,
                               @Nonnull final File aDestFile,
                               final boolean bOverwrite,
                               final boolean bRename) throws IOException
  {
    File aRealDestFile = aDestFile;
    if (!bOverwrite && aRealDestFile.exists ())
    {
      if (!bRename)
        throw new IOException ("File already exists: " + aRealDestFile);
      aRealDestFile = getUniqueFile (aRealDestFile.getAbsoluteFile ().getParentFile (), aRealDestFile.getName ());
    }

    final FileIOError aIOErr = s_aFOM.copyFile (aSrc, aRealDestFile);
    if (aIOErr.isFailure ())
      throw new IOException ("Copy failed: " + aIOErr.toString ());

    // if (!new File(file.getAbsolutePath()).delete()) { // do this if file
    // deletion always fails, may help
    if (!aSrc.delete ())
    {
      aRealDestFile.delete ();
      throw new IOException ("Move failed, unable to delete " + aSrc);
    }
    return aRealDestFile;
  }
}
