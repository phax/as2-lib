package com.helger.as2lib.crypto;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cms.CMSAlgorithm;

import com.helger.commons.annotations.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

public enum ECryptoAlgorithm implements IHasID <String>
{
  DIGEST_MD5 ("md5", PKCSObjectIdentifiers.md5),
  DIGEST_SHA1 ("sha1", OIWObjectIdentifiers.idSHA1),
  CRYPT_CAST5 ("cast5", CMSAlgorithm.CAST5_CBC),
  CRYPT_3DES ("3des", PKCSObjectIdentifiers.des_EDE3_CBC),
  CRYPT_IDEA ("idea", CMSAlgorithm.IDEA_CBC),
  CRYPT_RC2 ("rc2", PKCSObjectIdentifiers.RC2_CBC);

  private final String m_sID;
  private final ASN1ObjectIdentifier m_aOID;

  private ECryptoAlgorithm (@Nonnull @Nonempty final String sID, @Nonnull final ASN1ObjectIdentifier aOID)
  {
    m_sID = sID;
    m_aOID = aOID;
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

  @Nullable
  public static ECryptoAlgorithm getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ECryptoAlgorithm.class, sID);
  }

  @Nullable
  public static ASN1ObjectIdentifier getASN1OIDFromIDOrNull (@Nullable final String sID)
  {
    final ECryptoAlgorithm e = getFromIDOrNull (sID);
    return e == null ? null : e.getOID ();
  }
}
