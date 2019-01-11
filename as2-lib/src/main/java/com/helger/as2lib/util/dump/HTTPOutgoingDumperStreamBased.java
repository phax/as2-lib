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
package com.helger.as2lib.util.dump;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.WillCloseWhenClosed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.http.CHttp;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * Abstract outgoing HTTP dumper using an {@link OutputStream} for operations.
 *
 * @author Philip Helger
 * @since 3.1.0
 */
public class HTTPOutgoingDumperStreamBased implements IHTTPOutgoingDumper
{
  private static final Logger LOGGER = LoggerFactory.getLogger (HTTPOutgoingDumperStreamBased.class);

  private final OutputStream m_aOS;
  private int m_nHeaders = 0;

  /**
   * @param aOS
   *        The output stream to dump to. May not be <code>null</code>.
   */
  public HTTPOutgoingDumperStreamBased (@Nonnull @WillCloseWhenClosed final OutputStream aOS)
  {
    ValueEnforcer.notNull (aOS, "OutputStream");
    m_aOS = aOS;
  }

  @Nonnull
  protected final OutputStream getWrappedOS ()
  {
    return m_aOS;
  }

  private void _write (final int nByte)
  {
    try
    {
      m_aOS.write (nByte);
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Error dumping byte", ex);
    }
  }

  private void _write (@Nonnull final byte [] aBytes)
  {
    _write (aBytes, 0, aBytes.length);
  }

  private void _write (@Nonnull final byte [] aBytes, @Nonnegative final int nOfs, @Nonnegative final int nLen)
  {
    try
    {
      m_aOS.write (aBytes, nOfs, nLen);
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Error dumping bytes", ex);
    }
  }

  public void start (@Nonnull final String sURL)
  {
    final String sLine = "# Starting AS2 transmission to '" + sURL + "'" + CHttp.EOL;
    _write (sLine.getBytes (CHttp.HTTP_CHARSET));
  }

  public void dumpHeader (@Nonnull final String sName, @Nonnull final String sValue)
  {
    final String sHeaderLine = sName + ": " + sValue + CHttp.EOL;
    _write (sHeaderLine.getBytes (CHttp.HTTP_CHARSET));
    m_nHeaders++;
  }

  @Override
  public void finishedHeaders ()
  {
    if (m_nHeaders > 0)
    {
      // empty line
      _write (CHttp.EOL.getBytes (CHttp.HTTP_CHARSET));
    }
  }

  public void dumpPayload (final int nByte)
  {
    _write (nByte);
  }

  public void dumpPayload (@Nonnull final byte [] aBytes, @Nonnegative final int nOfs, @Nonnegative final int nLen)
  {
    _write (aBytes, nOfs, nLen);
  }

  @Override
  public void finishedPayload ()
  {
    StreamHelper.flush (m_aOS);
  }

  @Override
  public void close ()
  {
    StreamHelper.close (m_aOS);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("OutputStream", m_aOS).getToString ();
  }
}
