package com.helger.as2lib.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * Test class for class {@link MIC}
 *
 * @author Philip Helger
 */
public final class MICTest
{
  @Test
  public void testBasic ()
  {
    final MIC m1 = new MIC ("abc".getBytes (StandardCharsets.ISO_8859_1), ECryptoAlgorithmSign.DIGEST_SHA_256);
    assertNotNull (m1.getAsAS2String ());
    final MIC m2 = MIC.parse (m1.getAsAS2String ());
    assertEquals (m1, m2);
  }

  @Test (expected = IllegalArgumentException.class)
  public void testParseInvalidNull ()
  {
    MIC.parse (null);
  }

  @Test (expected = IllegalArgumentException.class)
  public void testParseInvalidEmpty ()
  {
    MIC.parse ("");
  }

  @Test (expected = IllegalArgumentException.class)
  public void testParseInvalidNoSep ()
  {
    MIC.parse ("VGVzdA==");
  }

  @Test (expected = IllegalArgumentException.class)
  public void testParseInvalidNoAlg ()
  {
    MIC.parse ("VGVzdA==, ");
  }

  @Test (expected = IllegalArgumentException.class)
  public void testParseInvalidInvalidAlg ()
  {
    MIC.parse ("VGVzdA==, blub");
  }

  @SuppressWarnings ("deprecation")
  @Test
  public void testIssue75 ()
  {
    final byte [] aBytes = "abc".getBytes (StandardCharsets.ISO_8859_1);

    MIC m1 = new MIC (aBytes, ECryptoAlgorithmSign.DIGEST_RSA_MD5);
    MIC m2 = new MIC (aBytes, ECryptoAlgorithmSign.DIGEST_MD5);
    assertEquals (m1, m2);

    m1 = new MIC (aBytes, ECryptoAlgorithmSign.DIGEST_RSA_SHA1);
    m2 = new MIC (aBytes, ECryptoAlgorithmSign.DIGEST_SHA1);
    assertEquals (m1, m2);

    m1 = new MIC (aBytes, ECryptoAlgorithmSign.DIGEST_SHA_1);
    m2 = new MIC (aBytes, ECryptoAlgorithmSign.DIGEST_SHA1);
    assertEquals (m1, m2);

    m1 = new MIC (aBytes, ECryptoAlgorithmSign.DIGEST_SHA_256);
    m2 = new MIC (aBytes, ECryptoAlgorithmSign.DIGEST_SHA256);
    assertEquals (m1, m2);

    m1 = new MIC (aBytes, ECryptoAlgorithmSign.DIGEST_SHA_384);
    m2 = new MIC (aBytes, ECryptoAlgorithmSign.DIGEST_SHA384);
    assertEquals (m1, m2);

    m1 = new MIC (aBytes, ECryptoAlgorithmSign.DIGEST_SHA_512);
    m2 = new MIC (aBytes, ECryptoAlgorithmSign.DIGEST_SHA512);
    assertEquals (m1, m2);
  }

}
