/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2021 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.crypto;

import java.io.Serializable;
import java.util.Arrays;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.base64.Base64;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * The MIC value
 *
 * @author Philip Helger
 * @since 4.4.0
 */
public class MIC implements Serializable
{
  private final byte [] m_aMICBytes;
  private final ECryptoAlgorithmSign m_eDigestAlgorithm;

  public MIC (@Nonnull final byte [] aMICBytes, @Nonnull final ECryptoAlgorithmSign eDigestAlgorithm)
  {
    ValueEnforcer.notNull (aMICBytes, "MICBytes");
    ValueEnforcer.notNull (eDigestAlgorithm, "DigestAlgorithm");

    m_aMICBytes = aMICBytes;
    m_eDigestAlgorithm = eDigestAlgorithm;
  }

  @Nonnull
  public byte [] micBytes ()
  {
    return m_aMICBytes;
  }

  @Nonnull
  public ECryptoAlgorithmSign getDigestAlgorithm ()
  {
    return m_eDigestAlgorithm;
  }

  /**
   * @return This is the Base64-encoded message digest of the specified
   *         algorithm. The exact layout must be
   *         <code>&lt;Base64EncodedMIC&gt;, &lt;MICAlgorithmID&gt;</code>
   */
  @Nonnull
  @Nonempty
  public String getAsAS2String ()
  {
    return Base64.encodeBytes (m_aMICBytes) + ", " + m_eDigestAlgorithm.getID ();
  }

  /**
   * Special unification for https://github.com/phax/as2-lib/issues/75
   *
   * @param eAlgorithm
   *        Source algorithm. May not be <code>null</code>.
   * @return The unified algorithm. Never <code>null</code>.
   */
  @SuppressWarnings ("deprecation")
  @Nonnull
  private static ECryptoAlgorithmSign _getUnified (@Nonnull final ECryptoAlgorithmSign eAlgorithm)
  {
    switch (eAlgorithm)
    {
      case DIGEST_RSA_MD5:
        return ECryptoAlgorithmSign.DIGEST_MD5;
      case DIGEST_RSA_SHA1:
      case DIGEST_SHA1:
        return ECryptoAlgorithmSign.DIGEST_SHA_1;
      case DIGEST_SHA256:
        return ECryptoAlgorithmSign.DIGEST_SHA_256;
      case DIGEST_SHA384:
        return ECryptoAlgorithmSign.DIGEST_SHA_384;
      case DIGEST_SHA512:
        return ECryptoAlgorithmSign.DIGEST_SHA_512;
      default:
        return eAlgorithm;
    }
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final MIC rhs = (MIC) o;
    return Arrays.equals (m_aMICBytes, rhs.m_aMICBytes) && _getUnified (m_eDigestAlgorithm).equals (_getUnified (rhs.m_eDigestAlgorithm));
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aMICBytes).append (m_eDigestAlgorithm).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("MICBytes", m_aMICBytes).append ("DigestAlgorithm", m_eDigestAlgorithm).getToString ();
  }

  @Nullable
  public static MIC parse (@Nullable final String sMIC)
  {
    if (StringHelper.hasNoText (sMIC))
      return null;

    final StringTokenizer st = new StringTokenizer (sMIC, ", \t\r\n");

    final String sMICBytes = st.nextToken ();
    final byte [] aMICBytes = Base64.safeDecode (sMICBytes);
    if (aMICBytes == null)
      throw new IllegalArgumentException ("Failed to base64 decode '" + sMICBytes + "'");

    if (!st.hasMoreTokens ())
      throw new IllegalArgumentException ("Separator after Base64 bytes is missing");

    final String sAlgorithm = st.nextToken ();
    final ECryptoAlgorithmSign eDigestAlgorithm = ECryptoAlgorithmSign.getFromIDOrNull (sAlgorithm);
    if (eDigestAlgorithm == null)
      throw new IllegalArgumentException ("Failed to parse digest algorithm '" + sAlgorithm + "'");
    return new MIC (aMICBytes, eDigestAlgorithm);
  }
}
