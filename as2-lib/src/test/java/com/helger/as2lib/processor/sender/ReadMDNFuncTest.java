package com.helger.as2lib.processor.sender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.function.Consumer;

import javax.mail.internet.MimeBodyPart;

import org.bouncycastle.cms.CMSException;
import org.junit.Test;

import com.helger.as2lib.crypto.ICryptoHelper;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.AS2MessageMDN;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.util.AS2Helper;
import com.helger.as2lib.util.AS2HttpHelper;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.io.stream.NonBlockingBufferedReader;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.security.certificate.CertificateHelper;

public class ReadMDNFuncTest
{
  @Test
  public void testReadMDN () throws Exception
  {
    final String sPrefix = "mdn/4af6f84c-d882-4466-8e0c-305a7fbe37b3";
    final IReadableResource aHeaderRes = new ClassPathResource (sPrefix + ".header");
    assertTrue (aHeaderRes.exists ());
    final IReadableResource aPayloadRes = new ClassPathResource (sPrefix + ".payload");
    assertTrue (aPayloadRes.exists ());
    final IReadableResource aCertRes = new ClassPathResource (sPrefix + ".pem");
    assertTrue (aCertRes.exists ());

    final HttpHeaderMap aHeaders = new HttpHeaderMap ();
    try (
        NonBlockingBufferedReader aBR = new NonBlockingBufferedReader (aHeaderRes.getReader (StandardCharsets.ISO_8859_1)))
    {
      String s;
      while ((s = aBR.readLine ()) != null)
      {
        final int i = s.indexOf (':');
        final String sName = s.substring (0, i).trim ();
        final String sValue = s.substring (i + 1).trim ();
        aHeaders.addHeader (sName, sValue);
      }
    }
    assertEquals ("<MOKOsi42435716cf621589dnode1POP000046@sfgt1.unix.fina.hr>",
                  aHeaders.getFirstHeaderValue ("Message-ID"));

    final X509Certificate aCert = CertificateHelper.convertStringToCertficateOrNull (StreamHelper.getAllBytesAsString (aCertRes,
                                                                                                                       StandardCharsets.ISO_8859_1));
    assertNotNull (aCert);

    final AS2Message aMsg = new AS2Message ();

    // Create a MessageMDN and copy HTTP headers
    final IMessageMDN aMDN = new AS2MessageMDN (aMsg);
    aMDN.headers ().addAllHeaders (aHeaders);

    final MimeBodyPart aPart = new MimeBodyPart (AS2HttpHelper.getAsInternetHeaders (aMDN.headers ()),
                                                 StreamHelper.getAllBytes (aPayloadRes));
    assertNotNull (aPart);
    aMsg.getMDN ().setData (aPart);

    final ICryptoHelper aCryptoHelper = AS2Helper.getCryptoHelper ();
    assertTrue (aCryptoHelper.isSigned (aPart));
    assertFalse (aCryptoHelper.isEncrypted (aPart));
    assertFalse (aCryptoHelper.isCompressed (aPart.getContentType ()));

    final Consumer <X509Certificate> aCertHolder = null;
    try
    {
      AS2Helper.parseMDN (aMsg, aCert, true, aCertHolder);
      fail ();
    }
    catch (final CMSException ex)
    {
      // expected to fail
      assertTrue (ex.getCause () instanceof IOException);
    }
  }
}
