package com.helger.as2lib.crypto;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.commons.string.StringHelper;

/**
 * Unit test class for class {@link ECryptoAlgorithmSign}.
 *
 * @author Philip Helger
 */
public final class ECryptoAlgorithmSignTest
{
  @Test
  public void testBasic ()
  {
    for (final ECryptoAlgorithmSign e : ECryptoAlgorithmSign.values ())
    {
      assertTrue (StringHelper.hasText (e.getID ()));
      assertNotNull (e.getOID ());
      assertTrue (StringHelper.hasText (e.getSignAlgorithmName ()));
      assertSame (e, ECryptoAlgorithmSign.getFromIDOrNull (e.getID ()));
      assertSame (e, ECryptoAlgorithmSign.getFromIDOrDefault (e.getID (), ECryptoAlgorithmSign.DIGEST_MD5));
      assertSame (e, ECryptoAlgorithmSign.getFromIDOrThrow (e.getID ()));
    }
  }
}
