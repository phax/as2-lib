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

public class CompositeParameters extends AbstractParameterParser
{
  private Map <String, AbstractParameterParser> m_aParameterParsers;
  private boolean m_bIgnoreMissingParsers;

  public CompositeParameters (final boolean ignoreMissingParsers)
  {
    super ();
    m_bIgnoreMissingParsers = ignoreMissingParsers;
  }

  public CompositeParameters (final boolean ignoreMissingParsers,
                              final Map <String, AbstractParameterParser> parameterParsers)
  {
    super ();
    m_bIgnoreMissingParsers = ignoreMissingParsers;
    getParameterParsers ().putAll (parameterParsers);
  }

  public CompositeParameters add (final String key, final AbstractParameterParser param)
  {
    getParameterParsers ().put (key, param);
    return this;
  }

  public void setIgnoreMissingParsers (final boolean ignoreMissingParsers)
  {
    m_bIgnoreMissingParsers = ignoreMissingParsers;
  }

  public boolean getIgnoreMissingParsers ()
  {
    return m_bIgnoreMissingParsers;
  }

  @Override
  public void setParameter (final String key, final String value) throws InvalidParameterException
  {
    final StringTokenizer keyParts = new StringTokenizer (key, ".", false);

    final AbstractParameterParser parser = getParameterParsers ().get (keyParts.nextToken ());
    if (parser != null)
    {
      if (!keyParts.hasMoreTokens ())
      {
        throw new InvalidParameterException ("Invalid key format", this, key, null);
      }

      final StringBuilder keyBuf = new StringBuilder (keyParts.nextToken ());

      while (keyParts.hasMoreTokens ())
      {
        keyBuf.append (".");
        keyBuf.append (keyParts.nextToken ());
      }

      parser.setParameter (keyBuf.toString (), value);
    }
    else
      if (!getIgnoreMissingParsers ())
      {
        throw new InvalidParameterException ("Invalid area in key", this, key, value);
      }
  }

  @Override
  public String getParameter (final String key) throws InvalidParameterException
  {
    final StringTokenizer keyParts = new StringTokenizer (key, ".", false);

    final String parserID = keyParts.nextToken ();
    final AbstractParameterParser parser = getParameterParsers ().get (parserID);

    if (parser != null)
    {
      if (!keyParts.hasMoreTokens ())
      {
        throw new InvalidParameterException ("Invalid key format", this, key, null);
      }

      final StringBuilder keyBuf = new StringBuilder (keyParts.nextToken ());

      while (keyParts.hasMoreTokens ())
      {
        keyBuf.append (".");
        keyBuf.append (keyParts.nextToken ());
      }

      return parser.getParameter (keyBuf.toString ());
    }
    if (!getIgnoreMissingParsers ())
      throw new InvalidParameterException ("Invalid area in key", this, key, null);

    return "";
  }

  public void setParameterParsers (final Map <String, AbstractParameterParser> parameterParsers)
  {
    m_aParameterParsers = parameterParsers;
  }

  protected Map <String, AbstractParameterParser> getParameterParsers ()
  {
    if (m_aParameterParsers == null)
      m_aParameterParsers = new HashMap <String, AbstractParameterParser> ();
    return m_aParameterParsers;
  }
}
