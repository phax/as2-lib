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
import javax.annotation.Nullable;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.commons.annotation.Nonempty;

public class InvalidParameterException extends OpenAS2Exception
{
  private final Object m_aTarget;
  private final String m_sKey;
  private final String m_sValue;

  public InvalidParameterException (final String sMsg, final Object aTarget, final String sKey, final String sValue)
  {
    super (sMsg + " - " + getAsString (sKey, sValue));
    m_aTarget = aTarget;
    m_sKey = sKey;
    m_sValue = sValue;
  }

  public InvalidParameterException (final String sMsg)
  {
    super (sMsg);
    m_aTarget = null;
    m_sKey = null;
    m_sValue = null;
  }

  public String getKey ()
  {
    return m_sKey;
  }

  public Object getTarget ()
  {
    return m_aTarget;
  }

  public String getValue ()
  {
    return m_sValue;
  }

  public static void checkValue (@Nonnull final Object aTarget,
                                 @Nonnull final String sValueName,
                                 @Nullable final Object aValue) throws InvalidParameterException
  {
    if (aValue == null)
      throw new InvalidParameterException ("Value is missing", aTarget, sValueName, null);
  }

  @Nonnull
  @Nonempty
  public static String getAsString (@Nullable final String sKey, @Nullable final String sValue)
  {
    return "Invalid parameter value for " + sKey + ": " + sValue;
  }
}
