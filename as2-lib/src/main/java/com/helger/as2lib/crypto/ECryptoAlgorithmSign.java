/*
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2023 Philip Helger philip[at]helger[dot]com
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

import com.helger.commons.annotation.DevelopersNote;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.lang.EnumHelper;

/**
 * This enum contains all signing supported crypto algorithms. The algorithms
 * contained in here may not be used for encryption of anything. See
 * {@link ECryptoAlgorithmCrypt} for encryption algorithms.<br>
 * Note: Mendelson uses the RFC 5751 identifiers.
 *
 * @author Philip Helger
 */
public enum ECryptoAlgorithmSign implements ICryptoAlgorithm
{
  /** See compatibility note in RFC 5751, section 3.4.3.1 */
  @Deprecated
  @DevelopersNote ("Use DIGEST_MD5 instead")
  DIGEST_RSA_MD5("rsa-md5", PKCSObjectIdentifiers.md5, "MD5WITHRSA", "md5"),

  /** See compatibility note in RFC 5751, section 3.4.3.1 */
  @Deprecated
  @DevelopersNote ("Use DIGEST_SHA1 or DIGEST_SHA_1 instead")
  DIGEST_RSA_SHA1("rsa-sha1", OIWObjectIdentifiers.idSHA1, "SHA1WITHRSA", "sha1"),

  /** Same for RFC 3851 and RFC 5751 */
  DIGEST_MD5 ("md5", PKCSObjectIdentifiers.md5, "MD5WITHRSA", "md5"),

  /**
   * Old version as of RFC 3851.
   */
  @DevelopersNote ("Use DIGEST_SHA_1 instead")
  DIGEST_SHA1("sha1", OIWObjectIdentifiers.idSHA1, "SHA1WITHRSA", "sha1"),

  /**
   * Old version as of RFC 3851.
   */
  @DevelopersNote ("Use DIGEST_SHA_256 instead")
  DIGEST_SHA256("sha256", NISTObjectIdentifiers.id_sha256, "SHA256WITHRSA", "sha256"),

  /**
   * Old version as of RFC 3851.
   */
  @DevelopersNote ("Use DIGEST_SHA_384 instead")
  DIGEST_SHA384("sha384", NISTObjectIdentifiers.id_sha384, "SHA384WITHRSA", "sha384"),
  /**
   * Old version as of RFC 3851.
   */
  @DevelopersNote ("Use DIGEST_SHA_512 instead")
  DIGEST_SHA512("sha512", NISTObjectIdentifiers.id_sha512, "SHA512WITHRSA", "sha512"),
  /**
   * New version as of RFC 5751.
   */
  DIGEST_SHA_1 ("sha-1", OIWObjectIdentifiers.idSHA1, "SHA1WITHRSA", "sha-1"),
  /**
   * New version as of RFC 5751.
   */
  DIGEST_SHA_224 ("sha-224", NISTObjectIdentifiers.id_sha224, "SHA224WITHRSA", "sha-224"),
  /**
   * New version as of RFC 5751.
   */
  DIGEST_SHA_256 ("sha-256", NISTObjectIdentifiers.id_sha256, "SHA256WITHRSA", "sha-256"),
  /**
   * New version as of RFC 5751.
   */
  DIGEST_SHA_384 ("sha-384", NISTObjectIdentifiers.id_sha384, "SHA384WITHRSA", "sha-384"),
  /**
   * New version as of RFC 5751.
   */
  DIGEST_SHA_512 ("sha-512", NISTObjectIdentifiers.id_sha512, "SHA512WITHRSA", "sha-512"),

  /**
   * id_rsassa_pkcs1_v1_5_with_sha3_256<br>
   * This identifier only works with the "SunRsaSign" provider
   */
  RSASSA_PKCS1_V1_5_WITH_SHA3_256 ("rsassa_pkcs1_v1_5_with_sha3_256",
                                   NISTObjectIdentifiers.id_rsassa_pkcs1_v1_5_with_sha3_256,
                                   "RSASSAPSS",
                                   "sha-256"),

  /**
   * RSASSA-PSS with SHA256
   *
   * @since 5.0.0
   */
  RSASSA_PSS_WITH_SHA256 ("rsassa-pss-sha256", NISTObjectIdentifiers.id_sha256, "SHA256WITHRSAANDMGF1", "sha-256"),

  /**
   * RSASSA-PSS with SHA512
   *
   * @since 5.0.0
   */
  RSASSA_PSS_WITH_SHA512 ("rsassa-pss-sha512", NISTObjectIdentifiers.id_sha512, "SHA512WITHRSAANDMGF1", "sha-512");

  public static final ECryptoAlgorithmSign DEFAULT_RFC_3851 = DIGEST_SHA1;
  public static final ECryptoAlgorithmSign DEFAULT_RFC_5751 = DIGEST_SHA_256;

  private final String m_sID;
  private final ASN1ObjectIdentifier m_aOID;
  private final String m_sBCAlgorithmName;
  private final String m_sSignAlgorithmID;

  ECryptoAlgorithmSign (@Nonnull @Nonempty final String sID,
                        @Nonnull final ASN1ObjectIdentifier aOID,
                        @Nonnull @Nonempty final String sBCAlgorithmName,
                        @Nullable final String sSignAlgorithmID)
  {
    m_sID = sID;
    m_aOID = aOID;
    m_sBCAlgorithmName = sBCAlgorithmName;
    m_sSignAlgorithmID = sSignAlgorithmID;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nonnull
  public ASN1ObjectIdentifier getOID ()
  {
    return m_aOID;
  }

  /**
   * @return The algorithm name to be used for BouncyCastle to do the SMIME
   *         packaging.
   */
  @Nonnull
  @Nonempty
  public String getSignAlgorithmName ()
  {
    return m_sBCAlgorithmName;
  }

  @Nonnull
  @Nonempty
  public String getSignAlgorithmID ()
  {
    return m_sSignAlgorithmID;
  }

  /**
   * @return <code>true</code> if this is an algorithm defined by RFC 3851,
   *         <code>false</code> otherwise. Please note that some algorithms are
   *         contained in both algorithm sets!
   * @since 4.2.0
   */
  public boolean isRFC3851Algorithm ()
  {
    return this == DIGEST_RSA_MD5 ||
           this == DIGEST_RSA_SHA1 ||
           this == DIGEST_MD5 ||
           this == DIGEST_SHA1 ||
           this == DIGEST_SHA256 ||
           this == DIGEST_SHA384 ||
           this == DIGEST_SHA512;
  }

  /**
   * @return <code>true</code> if this is an algorithm defined by RFC 5751,
   *         <code>false</code> otherwise. Please note that some algorithms are
   *         contained in both algorithm sets!
   * @since 4.2.0
   */
  public boolean isRFC5751Algorithm ()
  {
    return this == DIGEST_MD5 ||
           this == DIGEST_SHA_1 ||
           this == DIGEST_SHA_256 ||
           this == DIGEST_SHA_384 ||
           this == DIGEST_SHA_512;
  }

  @Nullable
  public static ECryptoAlgorithmSign getFromIDOrNull (@Nullable final String sID)
  {
    // Case insensitive for #32
    return EnumHelper.getFromIDCaseInsensitiveOrNull (ECryptoAlgorithmSign.class, sID);
  }

  @Nonnull
  public static ECryptoAlgorithmSign getFromIDOrThrow (@Nullable final String sID)
  {
    // Case insensitive for #32
    return EnumHelper.getFromIDCaseInsensitiveOrThrow (ECryptoAlgorithmSign.class, sID);
  }

  @Nullable
  public static ECryptoAlgorithmSign getFromIDOrDefault (@Nullable final String sID,
                                                         @Nullable final ECryptoAlgorithmSign eDefault)
  {
    // Case insensitive for #32
    return EnumHelper.getFromIDCaseInsensitiveOrDefault (ECryptoAlgorithmSign.class, sID, eDefault);
  }
}
