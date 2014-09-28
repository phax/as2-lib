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
package com.helger.as2lib.util;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.commons.string.StringHelper;

public final class DispositionOptions
{
  private String m_sMICAlg;
  private String m_sMICAlgImportance;
  private String m_sProtocol;
  private String m_sProtocolImportance;

  public DispositionOptions ()
  {}

  public void setMICAlg (@Nullable final String sMICAlg)
  {
    m_sMICAlg = sMICAlg;
  }

  @Nullable
  public String getMICAlg ()
  {
    return m_sMICAlg;
  }

  public void setMICAlgImportance (@Nullable final String sMICAlgImportance)
  {
    m_sMICAlgImportance = sMICAlgImportance;
  }

  @Nullable
  public String getMICAlgImportance ()
  {
    return m_sMICAlgImportance;
  }

  public void setProtocol (@Nullable final String sProtocol)
  {
    m_sProtocol = sProtocol;
  }

  @Nullable
  public String getProtocol ()
  {
    return m_sProtocol;
  }

  public void setProtocolImportance (@Nullable final String sProtocolImportance)
  {
    m_sProtocolImportance = sProtocolImportance;
  }

  @Nullable
  public String getProtocolImportance ()
  {
    return m_sProtocolImportance;
  }

  @Nonnull
  public String getAsString ()
  {
    if (m_sProtocolImportance == null && m_sProtocol == null && m_sMICAlgImportance == null && m_sMICAlg == null)
    {
      return "";
    }

    return "signed-receipt-protocol=" +
           m_sProtocolImportance +
           ", " +
           m_sProtocol +
           "; signed-receipt-micalg=" +
           m_sMICAlgImportance +
           ", " +
           m_sMICAlg;
  }

  @Override
  @Deprecated
  public String toString ()
  {
    return getAsString ();
  }

  /**
   * Parse Strings like <code>signed-receipt-protocol=optional, pkcs7-signature;
   * signed-receipt-micalg=optional, sha1</code>
   *
   * @param sOptions
   *        The string to parse. May be <code>null</code> in which case an empty
   *        object will be returned.
   * @return Never <code>null</code>.
   * @throws OpenAS2Exception
   *         In the very unlikely case of a programming error in
   *         {@link StringTokenizer}.
   */
  @Nonnull
  public static DispositionOptions createFromString (@Nullable final String sOptions) throws OpenAS2Exception
  {
    final DispositionOptions ret = new DispositionOptions ();
    if (StringHelper.hasText (sOptions))
    {
      try
      {
        final StringTokenizer aOptionTokens = new StringTokenizer (sOptions, "=,;", false);
        if (aOptionTokens.countTokens () > 5)
        {
          // Skip "signed-receipt-protocol"
          aOptionTokens.nextToken ();
          ret.setProtocolImportance (aOptionTokens.nextToken ().trim ());
          ret.setProtocol (aOptionTokens.nextToken ().trim ());

          // Skip "signed-receipt-micalg"
          aOptionTokens.nextToken ();
          ret.setMICAlgImportance (aOptionTokens.nextToken ().trim ());
          ret.setMICAlg (aOptionTokens.nextToken ().trim ());
        }
      }
      catch (final NoSuchElementException ex)
      {
        throw new OpenAS2Exception ("Invalid disposition options format: " + sOptions);
      }
    }
    return ret;
  }
}
