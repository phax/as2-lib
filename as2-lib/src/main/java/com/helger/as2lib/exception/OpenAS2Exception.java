/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2017 Philip Helger philip[at]helger[dot]com
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.lang.ClassHelper;

/**
 * Base class for all AS2 related exceptions used in this project.
 *
 * @author Philip Helger
 */
public class OpenAS2Exception extends Exception
{
  public static final String SOURCE_MESSAGE = "message";
  public static final String SOURCE_FILE = "file";
  private static final Logger s_aLogger = LoggerFactory.getLogger (OpenAS2Exception.class);

  private final ICommonsOrderedMap <String, Object> m_aSources = new CommonsLinkedHashMap<> ();

  public OpenAS2Exception ()
  {
    log (false);
  }

  public OpenAS2Exception (@Nullable final String sMsg)
  {
    super (sMsg);
  }

  public OpenAS2Exception (@Nullable final String sMsg, @Nullable final Throwable aCause)
  {
    super (sMsg, aCause);
  }

  public OpenAS2Exception (@Nullable final Throwable aCause)
  {
    super (aCause);
  }

  @Nonnull
  @ReturnsMutableCopy
  public final ICommonsOrderedMap <String, Object> getAllSources ()
  {
    return m_aSources.getClone ();
  }

  @Nullable
  public final Object getSource (@Nullable final String sID)
  {
    return m_aSources.get (sID);
  }

  public final void addSource (@Nonnull final String sID, @Nullable final Object aSource)
  {
    m_aSources.put (sID, aSource);
  }

  public final void terminate ()
  {
    log (true);
  }

  /**
   * @param bTerminated
   *        <code>true</code> if the exception was terminated
   */
  protected void log (final boolean bTerminated)
  {
    s_aLogger.info ("OpenAS2 " +
                    ClassHelper.getClassLocalName (getClass ()) +
                    " " +
                    (bTerminated ? "terminated" : "caught") +
                    ": " +
                    getMessage () +
                    (m_aSources.isEmpty () ? "" : "; sources: " + m_aSources),
                    getCause ());
  }
}
