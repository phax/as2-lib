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
package com.helger.as2lib.params;

import javax.annotation.Nonnull;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.commons.lang.StackTraceHelper;

public class ExceptionParameters extends AbstractParameterParser
{
  public static final String KEY_NAME = "name";
  public static final String KEY_MESSAGE = "message";
  public static final String KEY_TRACE = "trace";
  public static final String KEY_TERMINATED = "terminated";

  private final OpenAS2Exception m_aTarget;
  private final boolean m_bTerminated;

  public ExceptionParameters (final OpenAS2Exception aTarget, final boolean bTerminated)
  {
    m_aTarget = aTarget;
    m_bTerminated = bTerminated;
  }

  @Override
  public void setParameter (@Nonnull final String sKey, final String sValue) throws InvalidParameterException
  {
    if (sKey == null)
      throw new InvalidParameterException ("Invalid key", this, sKey, sValue);

    if (sKey.equals (KEY_NAME) || sKey.equals (KEY_MESSAGE) || sKey.equals (KEY_TRACE) || sKey.equals (KEY_TERMINATED))
      throw new InvalidParameterException ("Parameter is read-only", this, sKey, sValue);

    throw new InvalidParameterException ("Invalid key", this, sKey, sValue);
  }

  @Override
  @Nonnull
  public String getParameter (@Nonnull final String sKey) throws InvalidParameterException
  {
    if (sKey == null)
      throw new InvalidParameterException ("Invalid key", this, sKey, null);

    Throwable aUnwrappedTarget;
    if (m_aTarget instanceof WrappedOpenAS2Exception)
    {
      aUnwrappedTarget = ((WrappedOpenAS2Exception) m_aTarget).getCause ();
      if (aUnwrappedTarget == null)
        aUnwrappedTarget = m_aTarget;
    }
    else
    {
      aUnwrappedTarget = m_aTarget;
    }

    if (sKey.equals (KEY_NAME))
      return aUnwrappedTarget.getClass ().getName ();
    if (sKey.equals (KEY_MESSAGE))
      return aUnwrappedTarget.getMessage ();
    if (sKey.equals (KEY_TRACE))
      return StackTraceHelper.getStackAsString (m_aTarget);
    if (sKey.equals (KEY_TERMINATED))
      return m_bTerminated ? "terminated" : "";

    throw new InvalidParameterException ("Invalid key", this, sKey, null);
  }
}
