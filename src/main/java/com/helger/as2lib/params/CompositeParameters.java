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
package com.helger.as2lib.params;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.exception.InvalidParameterException;

public class CompositeParameters extends AbstractParameterParser
{
  private Map <String, AbstractParameterParser> m_aParameterParsers;
  private boolean m_bIgnoreMissingParsers;

  public CompositeParameters (final boolean bIgnoreMissingParsers)
  {
    m_bIgnoreMissingParsers = bIgnoreMissingParsers;
  }

  public CompositeParameters (final boolean bIgnoreMissingParsers,
                              final Map <String, AbstractParameterParser> aParameterParsers)
  {
    this (bIgnoreMissingParsers);
    getParameterParsers ().putAll (aParameterParsers);
  }

  @Nonnull
  public CompositeParameters add (final String sKey, final AbstractParameterParser aParam)
  {
    getParameterParsers ().put (sKey, aParam);
    return this;
  }

  public void setIgnoreMissingParsers (final boolean bIgnoreMissingParsers)
  {
    m_bIgnoreMissingParsers = bIgnoreMissingParsers;
  }

  public boolean getIgnoreMissingParsers ()
  {
    return m_bIgnoreMissingParsers;
  }

  @Override
  public void setParameter (final String sKey, final String sValue) throws InvalidParameterException
  {
    final StringTokenizer aKeyParts = new StringTokenizer (sKey, ".", false);

    final AbstractParameterParser aParser = getParameterParsers ().get (aKeyParts.nextToken ());
    if (aParser != null)
    {
      if (!aKeyParts.hasMoreTokens ())
        throw new InvalidParameterException ("Invalid key format", this, sKey, null);

      final StringBuilder aKeyBuf = new StringBuilder (aKeyParts.nextToken ());
      while (aKeyParts.hasMoreTokens ())
        aKeyBuf.append ('.').append (aKeyParts.nextToken ());
      aParser.setParameter (aKeyBuf.toString (), sValue);
    }
    else
      if (!getIgnoreMissingParsers ())
        throw new InvalidParameterException ("Invalid area in key", this, sKey, sValue);
  }

  @Override
  public String getParameter (final String sKey) throws InvalidParameterException
  {
    final StringTokenizer aKeyParts = new StringTokenizer (sKey, ".", false);

    final String aParserID = aKeyParts.nextToken ();
    final AbstractParameterParser aParser = getParameterParsers ().get (aParserID);
    if (aParser != null)
    {
      if (!aKeyParts.hasMoreTokens ())
        throw new InvalidParameterException ("Invalid key format", this, sKey, null);

      final StringBuilder aKeyBuf = new StringBuilder (aKeyParts.nextToken ());
      while (aKeyParts.hasMoreTokens ())
        aKeyBuf.append ('.').append (aKeyParts.nextToken ());
      return aParser.getParameter (aKeyBuf.toString ());
    }
    if (!getIgnoreMissingParsers ())
      throw new InvalidParameterException ("Invalid area in key", this, sKey, null);

    return "";
  }

  public void setParameterParsers (@Nullable final Map <String, AbstractParameterParser> aParameterParsers)
  {
    m_aParameterParsers = aParameterParsers;
  }

  @Nonnull
  protected Map <String, AbstractParameterParser> getParameterParsers ()
  {
    if (m_aParameterParsers == null)
      m_aParameterParsers = new HashMap <String, AbstractParameterParser> ();
    return m_aParameterParsers;
  }
}
