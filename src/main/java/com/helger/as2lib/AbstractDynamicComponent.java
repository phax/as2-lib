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
package com.helger.as2lib;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import com.helger.as2lib.exception.InvalidParameterException;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.session.ISession;
import com.helger.as2lib.util.IStringMap;
import com.helger.as2lib.util.StringMap;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.lang.CGStringHelper;
import com.helger.commons.string.ToStringGenerator;

public abstract class AbstractDynamicComponent extends StringMap implements IDynamicComponent
{
  private ISession m_aSession;

  @Nonnull
  public String getName ()
  {
    return CGStringHelper.getClassLocalName (this);
  }

  @Nonnull
  public final String getAttributeAsStringRequired (@Nonnull final String sKey) throws InvalidParameterException
  {
    final String sValue = getAttributeAsString (sKey);
    if (sValue == null)
      throw new InvalidParameterException ("Parameter not found", this, sKey, null);
    return sValue;
  }

  public final int getAttributeAsIntRequired (@Nonnull final String sKey) throws InvalidParameterException
  {
    final int nValue = getAttributeAsInt (sKey, Integer.MIN_VALUE);
    if (nValue == Integer.MIN_VALUE)
      throw new InvalidParameterException ("Parameter not found", this, sKey, null);
    return nValue;
  }

  @Nonnull
  public final ISession getSession ()
  {
    if (m_aSession == null)
      throw new IllegalStateException ("No session present so far!");
    return m_aSession;
  }

  @OverridingMethodsMustInvokeSuper
  public void initDynamicComponent (@Nonnull final ISession aSession, @Nullable final IStringMap aParameters) throws OpenAS2Exception
  {
    m_aSession = ValueEnforcer.notNull (aSession, "Session");
    setAttributes (aParameters != null ? aParameters.getAllAttributes () : null);
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ()).append ("session", m_aSession).toString ();
  }
}
