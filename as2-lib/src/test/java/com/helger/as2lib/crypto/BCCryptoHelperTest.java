/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2018 Philip Helger philip[at]helger[dot]com
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.mail.internet.MimeBodyPart;

import org.junit.Test;

import com.helger.as2lib.util.AS2Helper;
import com.helger.as2lib.util.AS2HttpHelper;
import com.helger.as2lib.util.cert.AS2KeyStoreHelper;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.mail.cte.EContentTransferEncoding;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.KeyStoreHelper;

/**
 * Unit test class for class {@link BCCryptoHelper}.
 *
 * @author Philip Helger
 */
public final class BCCryptoHelperTest
{
  private static final String PATH = "src/test/resources/mendelson/key1.pfx";
  private static final KeyStore KS = KeyStoreHelper.loadKeyStore (EKeyStoreType.PKCS12, PATH, "test").getKeyStore ();
  private static final PrivateKeyEntry PKE = KeyStoreHelper.loadPrivateKey (KS, PATH, "key1", "test".toCharArray ())
                                                           .getKeyEntry ();
  private static final X509Certificate CERT_ENCRYPT;
  static
  {
    try
    {
      CERT_ENCRYPT = AS2KeyStoreHelper.readX509Certificate ("src/test/resources/mendelson/key2.cer");
    }
    catch (final CertificateException ex)
    {
      throw new InitializationException (ex);
    }
  }

  @Test
  public void testCreateKeyStores () throws KeyStoreException, NoSuchProviderException
  {
    // Ensure each keystore type can be created
    final BCCryptoHelper x = new BCCryptoHelper ();
    for (final EKeyStoreType e : EKeyStoreType.values ())
    {
      assertNotNull (x.createNewKeyStore (e));
    }
  }

  @Test
  public void testSignWithAllAlgorithms () throws Exception
  {
    final MimeBodyPart aPart = new MimeBodyPart ();
    aPart.setText ("Hello world");

    for (int nIncludeCert = 0; nIncludeCert < 2; ++nIncludeCert)
      for (final ECryptoAlgorithmSign eAlgo : ECryptoAlgorithmSign.values ())
      {
        final MimeBodyPart aSigned = AS2Helper.getCryptoHelper ()
                                              .sign (aPart,
                                                     (X509Certificate) PKE.getCertificate (),
                                                     PKE.getPrivateKey (),
                                                     eAlgo,
                                                     nIncludeCert == 1,
                                                     eAlgo.isRFC3851Algorithm (),
                                                     EContentTransferEncoding.BASE64.getID ());
        assertNotNull (aSigned);

        final String [] aContentTypes = aSigned.getHeader (CHttpHeader.CONTENT_TYPE);
        assertNotNull (aContentTypes);
        assertEquals (1, aContentTypes.length);
        final String sContentType = aContentTypes[0];
        final String sExpectedStart = "multipart/signed; protocol=\"application/pkcs7-signature\"; micalg=" +
                                      eAlgo.getID () +
                                      "; \r\n\tboundary=\"----=_Part";
        assertTrue (sContentType + " does not start with " + sExpectedStart, sContentType.startsWith (sExpectedStart));
      }
  }

  @Test
  public void testSignWithAllCTEs () throws Exception
  {
    final MimeBodyPart aPart = new MimeBodyPart ();
    aPart.setText ("Hello world");

    for (final EContentTransferEncoding eCTE : EContentTransferEncoding.values ())
    {
      final MimeBodyPart aSigned = AS2Helper.getCryptoHelper ()
                                            .sign (aPart,
                                                   (X509Certificate) PKE.getCertificate (),
                                                   PKE.getPrivateKey (),
                                                   ECryptoAlgorithmSign.DIGEST_SHA_512,
                                                   true,
                                                   false,
                                                   eCTE.getID ());
      assertNotNull (aSigned);

      final String [] aContentTypes = aSigned.getHeader (CHttpHeader.CONTENT_TYPE);
      assertNotNull (aContentTypes);
      assertEquals (1, aContentTypes.length);
      final String sContentType = aContentTypes[0];
      final String sExpectedStart = "multipart/signed; protocol=\"application/pkcs7-signature\"; micalg=sha-512; \r\n\tboundary=\"----=_Part";
      assertTrue (sContentType + " does not start with " + sExpectedStart, sContentType.startsWith (sExpectedStart));
    }
  }

  @Test
  public void testSign_Base64 () throws Exception
  {
    final MimeBodyPart aPart = new MimeBodyPart ();
    aPart.setText ("Hello world", StandardCharsets.ISO_8859_1.name ());

    final MimeBodyPart aSigned = AS2Helper.getCryptoHelper ()
                                          .sign (aPart,
                                                 (X509Certificate) PKE.getCertificate (),
                                                 PKE.getPrivateKey (),
                                                 ECryptoAlgorithmSign.DIGEST_SHA_256,
                                                 false,
                                                 false,
                                                 EContentTransferEncoding.BASE64.getID ());
    assertNotNull (aSigned);

    final String sBoundary = AS2HttpHelper.parseContentType (aSigned.getContentType ()).getParameter ("boundary");
    assertNotNull (sBoundary);

    final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();
    aSigned.writeTo (aBAOS);

    final String sExpectedStart = "Content-Type: multipart/signed; protocol=\"application/pkcs7-signature\"; micalg=sha-256; \r\n" +
                                  "\tboundary=\"" +
                                  sBoundary +
                                  "\"\r\n" +
                                  "\r\n" +
                                  "--" +
                                  sBoundary +
                                  "\r\n" +
                                  "Content-Type: text/plain; charset=ISO-8859-1\r\n" +
                                  "Content-Transfer-Encoding: 7bit\r\n" +
                                  "\r\n" +
                                  "Hello world\r\n" +
                                  "--" +
                                  sBoundary +
                                  "\r\n" +
                                  "Content-Type: application/pkcs7-signature; name=smime.p7s; smime-type=signed-data\r\n" +
                                  "Content-Transfer-Encoding: base64\r\n" +
                                  "Content-Disposition: attachment; filename=\"smime.p7s\"\r\n" +
                                  "Content-Description: S/MIME Cryptographic Signature\r\n" +
                                  "\r\n" +
                                  "MIAGCSqGSIb3DQEHAqCAMIACAQExDzANBglghkgBZQMEAgEFADCABgkqhkiG9w0BBwEAADGCAhsw\r\n" +
                                  "ggIXAgEBMIG3MIGuMSYwJAYJKoZIhvcNAQkBFhdyb3NldHRhbmV0QG1lbmRlbHNvbi5kZTELMAkG\r\n" +
                                  "A1UEBhMCREUxDzANBgNVBAgTBkJlcmxpbjEPMA0GA1UEBxMGQmVybGluMSIwIAYDVQQKExltZW5k\r\n" +
                                  "ZWxzb24tZS1jb21tZXJjZSBHbWJIMSIwIAYDVQQLExltZW5kZWxzb24tZS1jb21tZXJjZSBHbWJI\r\n" +
                                  "MQ0wCwYDVQQDEwRtZW5kAgRDjv27MA0GCWCGSAFlAwQCAQUAoIG2MBgGCSqGSIb3DQEJAzELBgkq\r\n";
    final String sExpectedEnd = "\r\n" + "--" + sBoundary + "--\r\n";
    final String sReal = aBAOS.getAsString (StandardCharsets.ISO_8859_1);
    assertTrue (sReal.startsWith (sExpectedStart));
    assertTrue (sReal.endsWith (sExpectedEnd));
  }

  @Test
  public void testSign_Binary () throws Exception
  {
    final MimeBodyPart aPart = new MimeBodyPart ();
    aPart.setText ("Hello world", StandardCharsets.ISO_8859_1.name ());

    final MimeBodyPart aSigned = AS2Helper.getCryptoHelper ()
                                          .sign (aPart,
                                                 (X509Certificate) PKE.getCertificate (),
                                                 PKE.getPrivateKey (),
                                                 ECryptoAlgorithmSign.DIGEST_SHA_256,
                                                 false,
                                                 false,
                                                 EContentTransferEncoding.BINARY.getID ());
    assertNotNull (aSigned);

    final String sBoundary = AS2HttpHelper.parseContentType (aSigned.getContentType ()).getParameter ("boundary");
    assertNotNull (sBoundary);

    final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();
    aSigned.writeTo (aBAOS);

    final String sExpectedStart = "Content-Type: multipart/signed; protocol=\"application/pkcs7-signature\"; micalg=sha-256; \r\n" +
                                  "\tboundary=\"" +
                                  sBoundary +
                                  "\"\r\n" +
                                  "\r\n" +
                                  "--" +
                                  sBoundary +
                                  "\r\n" +
                                  "Content-Type: text/plain; charset=ISO-8859-1\r\n" +
                                  "Content-Transfer-Encoding: 7bit\r\n" +
                                  "\r\n" +
                                  "Hello world\r\n" +
                                  "--" +
                                  sBoundary +
                                  "\r\n" +
                                  "Content-Type: application/pkcs7-signature; name=smime.p7s; smime-type=signed-data\r\n" +
                                  "Content-Transfer-Encoding: binary\r\n" +
                                  "Content-Disposition: attachment; filename=\"smime.p7s\"\r\n" +
                                  "Content-Description: S/MIME Cryptographic Signature\r\n" +
                                  "\r\n";
    final String sExpectedEnd = "\r\n" + "--" + sBoundary + "--\r\n";
    final String sReal = aBAOS.getAsString (StandardCharsets.ISO_8859_1);
    assertTrue (sReal.startsWith (sExpectedStart));
    assertTrue (sReal.endsWith (sExpectedEnd));
  }

  @Test
  public void testSign_QuotedPrintable () throws Exception
  {
    final MimeBodyPart aPart = new MimeBodyPart ();
    aPart.setText ("Hello world", StandardCharsets.ISO_8859_1.name ());

    final MimeBodyPart aSigned = AS2Helper.getCryptoHelper ()
                                          .sign (aPart,
                                                 (X509Certificate) PKE.getCertificate (),
                                                 PKE.getPrivateKey (),
                                                 ECryptoAlgorithmSign.DIGEST_SHA_256,
                                                 false,
                                                 false,
                                                 EContentTransferEncoding.QUOTED_PRINTABLE.getID ());
    assertNotNull (aSigned);

    final String sBoundary = AS2HttpHelper.parseContentType (aSigned.getContentType ()).getParameter ("boundary");
    assertNotNull (sBoundary);

    final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();
    aSigned.writeTo (aBAOS);

    final String sExpectedStart = "Content-Type: multipart/signed; protocol=\"application/pkcs7-signature\"; micalg=sha-256; \r\n" +
                                  "\tboundary=\"" +
                                  sBoundary +
                                  "\"\r\n" +
                                  "\r\n" +
                                  "--" +
                                  sBoundary +
                                  "\r\n" +
                                  "Content-Type: text/plain; charset=ISO-8859-1\r\n" +
                                  "Content-Transfer-Encoding: 7bit\r\n" +
                                  "\r\n" +
                                  "Hello world\r\n" +
                                  "--" +
                                  sBoundary +
                                  "\r\n" +
                                  "Content-Type: application/pkcs7-signature; name=smime.p7s; smime-type=signed-data\r\n" +
                                  "Content-Transfer-Encoding: quoted-printable\r\n" +
                                  "Content-Disposition: attachment; filename=\"smime.p7s\"\r\n" +
                                  "Content-Description: S/MIME Cryptographic Signature\r\n" +
                                  "\r\n" +
                                  "0=80=06=09*=86H=86=F7\r\n" +
                                  "=01=07=02=A0=800=80=02=01=011=0F0\r\n" +
                                  "=06=09`=86H=01e=03=04=02=01=05=000=80=06=09*=86H=86=F7\r\n" +
                                  "=01=07=01=00=001=82=02=1B0=82=02=17=02=01=010=81=B70=81=AE1&0$=06=09*=86H=\r\n" +
                                  "=86=F7\r\n" +
                                  "=01=09=01=16=17rosettanet@mendelson.de1=0B0=09=06=03U=04=06=13=02DE1=0F0\r\n" +
                                  "=06=03U=04=08=13=06Berlin1=0F0\r\n" +
                                  "=06=03U=04=07=13=06Berlin1\"0 =06=03U=04\r\n" +
                                  "=13=19mendelson-e-commerce GmbH1\"0 =06=03U=04=0B=13=19mendelson-e-commerce =\r\n" +
                                  "GmbH1\r\n" +
                                  "0=0B=06=03U=04=03=13=04mend=02=04C=8E=FD=BB0\r\n" +
                                  "=06=09`=86H=01e=03=04=02=01=05=00=A0=81=B60=18=06=09*=86H=86=F7\r\n" +
                                  "=01=09=031=0B=06=09*=86H=86=F7\r\n" +
                                  "=01=07=010=1C=06=09*=86H=86=F7\r\n" +
                                  "=01=09=051=0F=17\r\n";
    final String sExpectedEnd = "\r\n" + "--" + sBoundary + "--\r\n";
    final String sReal = aBAOS.getAsString (StandardCharsets.ISO_8859_1);
    assertTrue (sReal.startsWith (sExpectedStart));
    assertTrue (sReal.endsWith (sExpectedEnd));
  }

  @Test
  public void testEncryptWithAllAlgorithms () throws Exception
  {
    final MimeBodyPart aPart = new MimeBodyPart ();
    aPart.setText ("Hello world");

    for (final ECryptoAlgorithmCrypt eAlgo : ECryptoAlgorithmCrypt.values ())
    {
      final MimeBodyPart aEncrypted = AS2Helper.getCryptoHelper ()
                                               .encrypt (aPart,
                                                         CERT_ENCRYPT,
                                                         eAlgo,
                                                         EContentTransferEncoding.BASE64.getID ());
      assertNotNull (aEncrypted);

      assertArrayEquals (new String [] { "application/pkcs7-mime; name=\"smime.p7m\"; smime-type=enveloped-data" },
                         aEncrypted.getHeader (CHttpHeader.CONTENT_TYPE));
      assertArrayEquals (new String [] { "attachment; filename=\"smime.p7m\"" },
                         aEncrypted.getHeader (CHttpHeader.CONTENT_DISPOSITION));
    }
  }

  @Test
  public void testEncryptWithAllCTEs () throws Exception
  {
    final MimeBodyPart aPart = new MimeBodyPart ();
    aPart.setText ("Hello world");

    for (final EContentTransferEncoding eCTE : EContentTransferEncoding.values ())
    {
      final MimeBodyPart aEncrypted = AS2Helper.getCryptoHelper ()
                                               .encrypt (aPart,
                                                         CERT_ENCRYPT,
                                                         ECryptoAlgorithmCrypt.CRYPT_AES256_GCM,
                                                         eCTE.getID ());
      assertNotNull (aEncrypted);

      assertArrayEquals (new String [] { "application/pkcs7-mime; name=\"smime.p7m\"; smime-type=enveloped-data" },
                         aEncrypted.getHeader (CHttpHeader.CONTENT_TYPE));
      assertArrayEquals (new String [] { "attachment; filename=\"smime.p7m\"" },
                         aEncrypted.getHeader (CHttpHeader.CONTENT_DISPOSITION));
    }
  }

  @Test
  public void testEncryptCTE_Base64 () throws Exception
  {
    final MimeBodyPart aPart = new MimeBodyPart ();
    aPart.setText ("Hello world");

    final MimeBodyPart aEncrypted = AS2Helper.getCryptoHelper ()
                                             .encrypt (aPart,
                                                       CERT_ENCRYPT,
                                                       ECryptoAlgorithmCrypt.CRYPT_3DES,
                                                       EContentTransferEncoding.BASE64.getID ());
    final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();
    aEncrypted.writeTo (aBAOS);

    final String sExpectedStart = "Content-Type: application/pkcs7-mime; name=\"smime.p7m\"; smime-type=enveloped-data\r\n" +
                                  "Content-Transfer-Encoding: base64\r\n" +
                                  "Content-Disposition: attachment; filename=\"smime.p7m\"\r\n" +
                                  "Content-Description: S/MIME Encrypted Message\r\n" +
                                  "\r\n" +
                                  "MIAGCSqGSIb3DQEHA6CAMIACAQAxggFTMIIBTwIBADCBtzCBrjEmMCQGCSqGSIb3DQEJARYXcm9z\r\n" +
                                  "ZXR0YW5ldEBtZW5kZWxzb24uZGUxCzAJBgNVBAYTAkRFMQ8wDQYDVQQIEwZCZXJsaW4xDzANBgNV\r\n" +
                                  "BAcTBkJlcmxpbjEiMCAGA1UEChMZbWVuZGVsc29uLWUtY29tbWVyY2UgR21iSDEiMCAGA1UECxMZ\r\n" +
                                  "bWVuZGVsc29uLWUtY29tbWVyY2UgR21iSDENMAsGA1UEAxMEbWVuZAIEQ4798zANBgkqhkiG9w0B\r\n";
    final String sReal = aBAOS.getAsString (StandardCharsets.ISO_8859_1);
    assertTrue (sReal.startsWith (sExpectedStart));
  }

  @Test
  public void testEncryptCTE_Binary () throws Exception
  {
    final MimeBodyPart aPart = new MimeBodyPart ();
    aPart.setText ("Hello world");

    final MimeBodyPart aEncrypted = AS2Helper.getCryptoHelper ()
                                             .encrypt (aPart,
                                                       CERT_ENCRYPT,
                                                       ECryptoAlgorithmCrypt.CRYPT_3DES,
                                                       EContentTransferEncoding.BINARY.getID ());
    final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();
    aEncrypted.writeTo (aBAOS);

    final String sExpectedStart = "Content-Type: application/pkcs7-mime; name=\"smime.p7m\"; smime-type=enveloped-data\r\n" +
                                  "Content-Transfer-Encoding: binary\r\n" +
                                  "Content-Disposition: attachment; filename=\"smime.p7m\"\r\n" +
                                  "Content-Description: S/MIME Encrypted Message\r\n" +
                                  "\r\n";
    final String sReal = aBAOS.getAsString (StandardCharsets.ISO_8859_1);
    assertTrue (sReal.startsWith (sExpectedStart));
  }

  @Test
  public void testEncryptCTE_QuotedPrintable () throws Exception
  {
    final MimeBodyPart aPart = new MimeBodyPart ();
    aPart.setText ("Hello world");

    final MimeBodyPart aEncrypted = AS2Helper.getCryptoHelper ()
                                             .encrypt (aPart,
                                                       CERT_ENCRYPT,
                                                       ECryptoAlgorithmCrypt.CRYPT_3DES,
                                                       EContentTransferEncoding.QUOTED_PRINTABLE.getID ());
    final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();
    aEncrypted.writeTo (aBAOS);

    final String sExpectedStart = "Content-Type: application/pkcs7-mime; name=\"smime.p7m\"; smime-type=enveloped-data\r\n" +
                                  "Content-Transfer-Encoding: quoted-printable\r\n" +
                                  "Content-Disposition: attachment; filename=\"smime.p7m\"\r\n" +
                                  "Content-Description: S/MIME Encrypted Message\r\n" +
                                  "\r\n" +
                                  "0=80=06=09*=86H=86=F7\r\n" +
                                  "=01=07=03=A0=800=80=02=01=001=82=01S0=82=01O=02=01=000=81=B70=81=AE1&0$=06=\r\n" +
                                  "=09*=86H=86=F7\r\n" +
                                  "=01=09=01=16=17rosettanet@mendelson.de1=0B0=09=06=03U=04=06=13=02DE1=0F0\r\n" +
                                  "=06=03U=04=08=13=06Berlin1=0F0\r\n" +
                                  "=06=03U=04=07=13=06Berlin1\"0 =06=03U=04\r\n" +
                                  "=13=19mendelson-e-commerce GmbH1\"0 =06=03U=04=0B=13=19mendelson-e-commerce =\r\n" +
                                  "GmbH1\r\n" +
                                  "0=0B=06=03U=04=03=13=04mend=02=04C=8E=FD=F30\r\n" +
                                  "=06=09*=86H=86=F7\r\n";
    final String sReal = aBAOS.getAsString (StandardCharsets.ISO_8859_1);
    assertTrue (sReal.startsWith (sExpectedStart));
  }
}
