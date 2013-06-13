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
package com.helger.as2lib;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.params.InvalidParameterException;
import com.phloc.commons.lang.CGStringHelper;
import com.phloc.commons.string.StringParser;

public class BaseComponent implements IDynamicComponent
{
  private Map <String, String> m_aParameters;
  private ISession m_aSession;

  public String getName ()
  {
    return CGStringHelper.getClassLocalName (this);
  }

  public void setParameter (final String sKey, final String sValue)
  {
    getParameters ().put (sKey, sValue);
  }

  public void setParameter (final String sKey, final int nValue)
  {
    setParameter (sKey, Integer.toString (nValue));
  }

  public String getParameterNotRequired (final String sKey)
  {
    return getParameters ().get (sKey);
  }

  public String getParameter (final String sKey, final String sDefaultValue)
  {
    final String value = getParameterNotRequired (sKey);
    return value == null ? sDefaultValue : value;
  }

  public String getParameterRequired (final String sKey) throws InvalidParameterException
  {
    final String sValue = getParameterNotRequired (sKey);
    if (sValue == null)
      throw new InvalidParameterException (this, sKey, null);
    return sValue;
  }

  public int getParameterInt (final String sKey) throws InvalidParameterException
  {
    final String sValue = getParameterRequired (sKey);
    if (sValue != null)
      return Integer.parseInt (sValue);
    return 0;
  }

  public int getParameterInt (final String sKey, final int nDefault)
  {
    final String sValue = getParameterNotRequired (sKey);
    return StringParser.parseInt (sValue, nDefault);
  }

  @Nonnull
  public Map <String, String> getParameters ()
  {
    if (m_aParameters == null)
      m_aParameters = new HashMap <String, String> ();
    return m_aParameters;
  }

  public ISession getSession ()
  {
    return m_aSession;
  }

  public void initDynamicComponent (final ISession aSession, final Map <String, String> aParameters) throws OpenAS2Exception
  {
    m_aSession = aSession;
    m_aParameters = aParameters;
  }
}
