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
package com.helger.as2lib.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.WillClose;
import javax.annotation.WillNotClose;


import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.InvalidMessageException;
import com.phloc.commons.CGlobal;
import com.phloc.commons.annotations.Nonempty;
import com.phloc.commons.io.file.FileIOError;
import com.phloc.commons.io.file.FileOperations;
import com.phloc.commons.io.file.FileUtils;
import com.phloc.commons.io.file.FilenameHelper;
import com.phloc.commons.io.streams.StreamUtils;
import com.phloc.commons.mutable.MutableLong;
import com.phloc.commons.timing.StopWatch;

public class IOUtil
{
  public static final String MSG_WAIT_FOR_KEYPRESS = "Waiting for keypress...";
  public static final String MSG_PROMPT = "> ";

  public static long copy (@WillClose final InputStream in, @WillNotClose final OutputStream out)
  {
    final MutableLong aML = new MutableLong ();
    StreamUtils.copyInputStreamToOutputStream (in, out, aML);
    return aML.longValue ();
  }

  public static void copy (@WillClose final InputStream in,
                           @WillNotClose final OutputStream out,
                           @Nonnegative final long contentSize)
  {
    StreamUtils.copyInputStreamToOutputStream (in,
                                               out,
                                               new byte [16 * CGlobal.BYTES_PER_KILOBYTE],
                                               null,
                                               Long.valueOf (contentSize));
  }

  public static File getDirectoryFile (final String directory)
  {
    final File aDir = new File (directory);
    FileUtils.ensureParentDirectoryIsPresent (new File (aDir, "dummy"));
    return aDir;
  }

  public static String getTransferRate (final long bytes, final StopWatch stub)
  {
    final StringBuilder aSB = new StringBuilder ();
    aSB.append (bytes).append (" bytes in ").append (stub.getMillis () / 1000.0).append ("seconds at ");

    final long time = stub.getMillis ();
    if (time != 0)
    {
      final double stime = time / 1000.0;
      final long rate = Math.round (bytes / stime);
      aSB.append (_getTransferRate (rate));
    }
    else
    {
      aSB.append (_getTransferRate (bytes));
    }

    return aSB.toString ();
  }

  @Nonnull
  @Nonempty
  private static String _getTransferRate (final long bytesPerSecond)
  {
    final StringBuilder aSB = new StringBuilder ();
    if (bytesPerSecond < 1024)
      aSB.append (bytesPerSecond).append (" Bps");
    else
    {
      final long kbytesPerSecond = bytesPerSecond / 1024;
      if (kbytesPerSecond < 1024)
        aSB.append (kbytesPerSecond).append (".").append (bytesPerSecond % 1024).append (" KBps");
      else
        aSB.append (kbytesPerSecond / 1024).append (".").append (kbytesPerSecond % 1024).append (" MBps");
    }
    return aSB.toString ();
  }

  public static File getUnique (final File dir, final String filename)
  {
    int counter = -1;
    final String sBaseFilename = FilenameHelper.getAsSecureValidFilename (filename);

    while (true)
    {
      final File test = new File (dir, counter == -1 ? sBaseFilename : sBaseFilename + "." + Integer.toString (counter));
      if (!test.exists ())
        return test;

      counter++;
    }
  }

  // move the file to an error directory
  public static void handleError (final File file, final String errorDirectory) throws OpenAS2Exception
  {
    File destFile = null;

    try
    {
      final File errorDir = getDirectoryFile (errorDirectory);
      destFile = new File (errorDir, file.getName ());

      // move the file
      destFile = moveFile (file, destFile, false, true);
    }
    catch (final IOException ioe)
    {
      final InvalidMessageException im = new InvalidMessageException ("Failed to move " +
                                                                      file.getAbsolutePath () +
                                                                      " to error directory " +
                                                                      destFile.getAbsolutePath ());
      im.initCause (ioe);
      throw im;
    }

    // make sure an error of this event is logged
    final InvalidMessageException imMoved = new InvalidMessageException ("Moved " +
                                                                         file.getAbsolutePath () +
                                                                         " to " +
                                                                         destFile.getAbsolutePath ());
    imMoved.terminate ();
  }

  public static File moveFile (final File src, final File pdest, final boolean overwrite, final boolean rename) throws IOException
  {
    File dest = pdest;
    if (!overwrite && dest.exists ())
    {
      if (!rename)
        throw new IOException ("File already exists: " + dest);
      dest = getUnique (dest.getAbsoluteFile ().getParentFile (), dest.getName ());
    }

    final FileIOError aIOErr = FileOperations.copyFile (src, dest);
    if (aIOErr.isFailure ())
      throw new IOException ("Copy failed: " + aIOErr.toString ());

    // if (!new File(file.getAbsolutePath()).delete()) { // do this if file
    // deletion always fails, may help
    if (!src.delete ())
    {
      dest.delete ();
      throw new IOException ("Move failed, unable to delete " + src);
    }
    return dest;
  }
}
