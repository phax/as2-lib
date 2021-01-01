/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2021 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.exception;

import java.io.File;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.CAS2Info;
import com.helger.as2lib.message.IMessage;
import com.helger.commons.lang.ClassHelper;

/**
 * Base class for all AS2 related exceptions used in this project.
 *
 * @author Philip Helger
 */
public class AS2Exception extends Exception
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS2Exception.class);

  private IMessage m_aSrcMsg;
  private File m_aSrcFile;

  public AS2Exception ()
  {
    log (false);
  }

  public AS2Exception (@Nullable final String sMsg)
  {
    super (sMsg);
  }

  public AS2Exception (@Nullable final String sMsg, @Nullable final Throwable aCause)
  {
    super (sMsg, aCause);
  }

  public AS2Exception (@Nullable final Throwable aCause)
  {
    super (aCause);
  }

  @Nullable
  public final IMessage getSourceMsg ()
  {
    return m_aSrcMsg;
  }

  @Nonnull
  public final AS2Exception setSourceMsg (@Nullable final IMessage aSrcMsg)
  {
    m_aSrcMsg = aSrcMsg;
    return this;
  }

  @Nullable
  public final File getSourceFile ()
  {
    return m_aSrcFile;
  }

  @Nonnull
  public final AS2Exception setSourceFile (@Nullable final File aSrcFile)
  {
    m_aSrcFile = aSrcFile;
    return this;
  }

  @Nonnull
  public final AS2Exception terminate ()
  {
    log (true);
    return this;
  }

  /**
   * @param bTerminated
   *        <code>true</code> if the exception was terminated
   */
  protected final void log (final boolean bTerminated)
  {
    if (LOGGER.isInfoEnabled ())
      LOGGER.info (CAS2Info.NAME_VERSION +
                   " " +
                   ClassHelper.getClassLocalName (getClass ()) +
                   " " +
                   (bTerminated ? "terminated" : "caught") +
                   ": " +
                   getMessage () +
                   (m_aSrcFile == null ? "" : "; source file: " + m_aSrcFile.getAbsolutePath ()) +
                   (m_aSrcMsg == null ? "" : "; source msg: " + m_aSrcMsg.getLoggingText ()),
                   getCause ());
  }
}
