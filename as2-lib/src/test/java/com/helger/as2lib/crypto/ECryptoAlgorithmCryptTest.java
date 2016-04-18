package com.helger.as2lib.crypto;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.commons.string.StringHelper;

/**
 * Unit test class for class {@link ECryptoAlgorithmCrypt}.
 *
 * @author Philip Helger
 */
public final class ECryptoAlgorithmCryptTest
{
  @Test
  public void testBasic ()
  {
    for (final ECryptoAlgorithmCrypt e : ECryptoAlgorithmCrypt.values ())
    {
      assertTrue (StringHelper.hasText (e.getID ()));
      assertNotNull (e.getOID ());
      assertSame (e, ECryptoAlgorithmCrypt.getFromIDOrNull (e.getID ()));
      assertSame (e, ECryptoAlgorithmCrypt.getFromIDOrDefault (e.getID (), ECryptoAlgorithmCrypt.CRYPT_3DES));
      assertSame (e, ECryptoAlgorithmCrypt.getFromIDOrThrow (e.getID ()));
    }
  }
}
