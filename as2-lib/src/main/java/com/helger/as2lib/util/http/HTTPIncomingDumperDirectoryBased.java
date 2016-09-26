/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2016 Philip Helger philip[at]helger[dot]com
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
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.charset.CCharset;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * Directory based HTTP dumper.
 *
 * @author Philip Helger
 */
public class HTTPIncomingDumperDirectoryBased implements IHTTPIncomingDumper
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (HTTPIncomingDumperDirectoryBased.class);

  private final File m_aDumpDirectory;

  public HTTPIncomingDumperDirectoryBased (@Nonnull final File aDumpDirectory)
  {
    ValueEnforcer.notNull (aDumpDirectory, "DumpDirectory");
    ValueEnforcer.isTrue (FileHelper.existsDir (aDumpDirectory),
                          () -> "DumpDirectory " + aDumpDirectory + " does not exist!");
    m_aDumpDirectory = aDumpDirectory;
  }

  @Nonnull
  public File getDumpDirectory ()
  {
    return m_aDumpDirectory;
  }

  /**
   * The filename to be used to store the request in the folder provided in the
   * constructor.
   *
   * @param nIndex
   *        Unique index to avoid duplicate filenames
   * @return The local filename without any path
   */
  @Nonnull
  protected String getStoreFilename (final int nIndex)
  {
    return "as2-" + Long.toString (new Date ().getTime ()) + "-" + nIndex + ".http";
  }

  public void dumpIncomingRequest (@Nonnull final List <String> aHeaderLines, @Nonnull final byte [] aPayload)
  {
    // Ensure a unique filename
    File aDestinationFile;
    int nIndex = 0;
    do
    {
      aDestinationFile = new File (m_aDumpDirectory, getStoreFilename (nIndex));
      nIndex++;
      if (nIndex > 100)
        throw new IllegalStateException ("Avoid endless loop to store message!");
    } while (aDestinationFile.exists ());

    s_aLogger.info ("Dumping HTTP request to file " + aDestinationFile.getAbsolutePath ());
    try (final OutputStream aOS = FileHelper.getOutputStream (aDestinationFile))
    {
      for (final String sHeaderLine : aHeaderLines)
        aOS.write ((sHeaderLine + HTTPHelper.EOL).getBytes (CCharset.CHARSET_ISO_8859_1_OBJ));

      // empty line
      aOS.write (HTTPHelper.EOL.getBytes (CCharset.CHARSET_ISO_8859_1_OBJ));

      // Add payload
      aOS.write (aPayload);
    }
    catch (final IOException ex)
    {
      s_aLogger.error ("Failed to dump HTTP request to file " + aDestinationFile.getAbsolutePath (), ex);
    }
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("DumpDirectory", m_aDumpDirectory).toString ();
  }
}
