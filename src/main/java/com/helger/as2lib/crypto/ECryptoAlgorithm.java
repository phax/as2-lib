/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2015 Philip Helger philip[at]helger[dot]com
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
import org.bouncycastle.cms.CMSAlgorithm;

import com.helger.commons.annotations.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

public enum ECryptoAlgorithm implements IHasID <String>
{
  DIGEST_MD5 ("md5", PKCSObjectIdentifiers.md5, ECryptoAlgorithmMode.DIGEST),
  DIGEST_SHA1 ("sha1", OIWObjectIdentifiers.idSHA1, ECryptoAlgorithmMode.DIGEST),
  DIGEST_SHA256 ("sha256", NISTObjectIdentifiers.id_sha256, ECryptoAlgorithmMode.DIGEST),
  DIGEST_SHA384 ("sha384", NISTObjectIdentifiers.id_sha384, ECryptoAlgorithmMode.DIGEST),
  DIGEST_SHA512 ("sha512", NISTObjectIdentifiers.id_sha512, ECryptoAlgorithmMode.DIGEST),
  CRYPT_CAST5 ("cast5", CMSAlgorithm.CAST5_CBC, ECryptoAlgorithmMode.CRYPT),
  CRYPT_3DES ("3des", PKCSObjectIdentifiers.des_EDE3_CBC, ECryptoAlgorithmMode.CRYPT),
  CRYPT_IDEA ("idea", CMSAlgorithm.IDEA_CBC, ECryptoAlgorithmMode.CRYPT),
  CRYPT_RC2 ("rc2", PKCSObjectIdentifiers.RC2_CBC, ECryptoAlgorithmMode.CRYPT);

  private final String m_sID;
  private final ASN1ObjectIdentifier m_aOID;
  private final ECryptoAlgorithmMode m_eMode;

  private ECryptoAlgorithm (@Nonnull @Nonempty final String sID,
                            @Nonnull final ASN1ObjectIdentifier aOID,
                            @Nonnull final ECryptoAlgorithmMode eMode)
  {
    m_sID = sID;
    m_aOID = aOID;
    m_eMode = eMode;
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

  @Nonnull
  public ECryptoAlgorithmMode getCryptAlgorithmMode ()
  {
    return m_eMode;
  }

  public boolean isDigesting ()
  {
    return m_eMode.isDigesting ();
  }

  public boolean isEncrypting ()
  {
    return m_eMode.isEncrypting ();
  }

  @Nullable
  public static ECryptoAlgorithm getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ECryptoAlgorithm.class, sID);
  }

  @Nullable
  @Deprecated
  public static ASN1ObjectIdentifier getASN1OIDFromIDOrNull (@Nullable final String sID)
  {
    final ECryptoAlgorithm e = getFromIDOrNull (sID);
    return e == null ? null : e.getOID ();
  }
}
