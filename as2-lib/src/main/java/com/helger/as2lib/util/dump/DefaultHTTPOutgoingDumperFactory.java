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
package com.helger.as2lib.util.dump;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import com.helger.as2lib.message.IBaseMessage;

/**
 * Default implementation of {@link IHTTPOutgoingDumperFactory}. Extracted as a
 * public class in v4.4.5.
 *
 * @author Philip Helger
 */
public class DefaultHTTPOutgoingDumperFactory implements IHTTPOutgoingDumperFactory
{
  // Counter to ensure unique filenames
  private final AtomicInteger m_aCounter = new AtomicInteger (0);
  private final File m_aDumpDirectory;

  public DefaultHTTPOutgoingDumperFactory (@Nonnull final File aDumpDirectory)
  {
    m_aDumpDirectory = aDumpDirectory;
  }

  @Nonnull
  public IHTTPOutgoingDumper apply (@Nonnull final IBaseMessage aMsg)
  {
    return new HTTPOutgoingDumperFileBased (new File (m_aDumpDirectory,
                                                      "as2-outgoing-" +
                                                                        Long.toString (System.currentTimeMillis ()) +
                                                                        "-" +
                                                                        Integer.toString (m_aCounter.getAndIncrement ()) +
                                                                        ".http"));
  }
}
