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
package com.helger.as2lib.util;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import com.helger.as2lib.exception.OpenAS2Exception;

public class DispositionOptions
{
  private String m_sMicalg;
  private String m_sMicalgImportance;
  private String m_sProtocol;
  private String m_sProtocolImportance;

  public DispositionOptions (final String options) throws OpenAS2Exception
  {
    parseOptions (options);
  }

  public void setMicalg (final String micalg)
  {
    m_sMicalg = micalg;
  }

  // signed-receipt-protocol=optional, pkcs7-signature;
  // signed-receipt-micalg=optional, sha1
  public String getMicalg ()
  {
    return m_sMicalg;
  }

  public void setMicalgImportance (final String micalgImportance)
  {
    m_sMicalgImportance = micalgImportance;
  }

  public String getMicalgImportance ()
  {
    return m_sMicalgImportance;
  }

  public void setProtocol (final String protocol)
  {
    m_sProtocol = protocol;
  }

  public String getProtocol ()
  {
    return m_sProtocol;
  }

  public void setProtocolImportance (final String protocolImportance)
  {
    m_sProtocolImportance = protocolImportance;
  }

  public String getProtocolImportance ()
  {
    return m_sProtocolImportance;
  }

  public String makeOptions ()
  {
    final StringBuilder options = new StringBuilder ();

    if ((getProtocolImportance () == null) &&
        (getProtocol () == null) &&
        (getMicalgImportance () == null) &&
        (getMicalg () == null))
    {
      return new String ("");
    }

    options.append ("signed-receipt-protocol=").append (getProtocolImportance ());
    options.append (", ").append (getProtocol ());
    options.append ("; signed-receipt-micalg=").append (getMicalgImportance ());
    options.append (", ").append (getMicalg ());

    return options.toString ();
  }

  public void parseOptions (final String options) throws OpenAS2Exception
  {
    setProtocolImportance (null);
    setProtocol (null);
    setMicalgImportance (null);
    setMicalg (null);
    if (options != null)
    {

      try
      {
        final StringTokenizer optionTokens = new StringTokenizer (options, "=,;", false);
        if (optionTokens.countTokens () > 5)
        {

          optionTokens.nextToken ();
          setProtocolImportance (optionTokens.nextToken ().trim ());
          setProtocol (optionTokens.nextToken ().trim ());
          optionTokens.nextToken ();
          setMicalgImportance (optionTokens.nextToken ().trim ());
          setMicalg (optionTokens.nextToken ().trim ());

        }
      }
      catch (final NoSuchElementException nsee)
      {
        throw new OpenAS2Exception ("Invalid disposition options format: " + options);
      }
    }
  }

  @Override
  public String toString ()
  {
    return makeOptions ();
  }
}
