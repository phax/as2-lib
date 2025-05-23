/*
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2025 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.supplementary.main;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.cert.X509Certificate;

import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.client.AS2Client;
import com.helger.as2lib.client.AS2ClientRequest;
import com.helger.as2lib.client.AS2ClientResponse;
import com.helger.as2lib.client.AS2ClientSettings;
import com.helger.as2lib.crypto.ECompressionType;
import com.helger.as2lib.crypto.ECryptoAlgorithmCrypt;
import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.as2lib.disposition.DispositionOptions;
import com.helger.as2lib.util.cert.AS2KeyStoreHelper;
import com.helger.as2lib.util.dump.HTTPIncomingDumperStreamBased;
import com.helger.as2lib.util.dump.HTTPOutgoingDumperStreamBased;
import com.helger.as2lib.util.dump.IHTTPOutgoingDumperFactory;
import com.helger.as2lib.util.http.HTTPHelper;
import com.helger.commons.io.stream.NonClosingOutputStream;
import com.helger.commons.mime.CMimeType;
import com.helger.mail.cte.EContentTransferEncoding;
import com.helger.security.keystore.EKeyStoreType;

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;

/**
 * Test class to send an AS2 messages to the Mendelson test server.
 *
 * @author Philip Helger
 */
public final class MainSendToMendelsonTestServer
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainSendToMendelsonTestServer.class);

  public static void main (final String [] args) throws Exception
  {
    Proxy aHttpProxy = null;
    if (false)
      aHttpProxy = new Proxy (Proxy.Type.HTTP, new InetSocketAddress ("1.2.3.4", 8080));

    IHTTPOutgoingDumperFactory aOutgoingDumperFactory = null;
    if (false)
      aOutgoingDumperFactory = x -> new HTTPOutgoingDumperStreamBased (System.out);
    if (false)
      HTTPHelper.setHTTPIncomingDumperFactory ( () -> new HTTPIncomingDumperStreamBased (new NonClosingOutputStream (System.out)));

    // Start client configuration
    final AS2ClientSettings aSettings = new AS2ClientSettings ();
    aSettings.setKeyStore (EKeyStoreType.PKCS12, new File ("src/test/resources/external/mendelson/key3.pfx"), "test");

    // Fixed sender
    aSettings.setSenderData ("mycompanyAS2", "phax.as2-lib@github.com", "key3");

    // Fixed receiver - key alias must be "mendelsontestAS2"
    aSettings.setReceiverData ("mendelsontestAS2",
                               "mendelsontestAS2",
                               true ? "https://testas2.mendelson-e-c.com:8444/as2/HttpReceiver"
                                    : "http://testas2.mendelson-e-c.com:8080/as2/HttpReceiver");
    final X509Certificate aReceiverCertificate = AS2KeyStoreHelper.readX509Certificate ("src/test/resources/external/mendelson/key4.cer");
    aSettings.setReceiverCertificate (aReceiverCertificate);

    // AS2 stuff
    aSettings.setPartnershipName (aSettings.getSenderAS2ID () + "_" + aSettings.getReceiverAS2ID ());
    // When a signed message is used, the algorithm for MIC and message must be
    // identical
    final ECryptoAlgorithmSign eSignAlgo = ECryptoAlgorithmSign.DIGEST_SHA_256;
    // CBC works, GCM is not supported
    final ECryptoAlgorithmCrypt eCryptAlgo = ECryptoAlgorithmCrypt.CRYPT_AES128_CBC;
    final ECompressionType eCompress = ECompressionType.ZLIB;
    final boolean bCompressBeforeSigning = AS2ClientSettings.DEFAULT_COMPRESS_BEFORE_SIGNING;

    aSettings.setMDNOptions (new DispositionOptions ().setMICAlg (eSignAlgo)
                                                      .setMICAlgImportance (DispositionOptions.IMPORTANCE_REQUIRED)
                                                      .setProtocol (DispositionOptions.PROTOCOL_PKCS7_SIGNATURE)
                                                      .setProtocolImportance (DispositionOptions.IMPORTANCE_REQUIRED));

    aSettings.setEncryptAndSign (eCryptAlgo, eSignAlgo);
    aSettings.setCompress (eCompress, bCompressBeforeSigning);
    aSettings.setMessageIDFormat ("github-phax-as2-lib-$date.uuuuMMdd-HHmmssZ$-$rand.1234$@$msg.sender.as2_id$_$msg.receiver.as2_id$");
    aSettings.setRetryCount (1);
    aSettings.setConnectTimeout (Timeout.ofSeconds (10));
    aSettings.setResponseTimeout (Timeout.ofSeconds (10));
    aSettings.setHttpOutgoingDumperFactory (aOutgoingDumperFactory);

    // Build client request
    final AS2ClientRequest aRequest = new AS2ClientRequest ("AS2 test message from as2-lib");
    aRequest.setData (new DataHandler (new FileDataSource (new File ("src/test/resources/external/mendelson/testcontent.attachment"))));
    aRequest.setContentType (CMimeType.TEXT_PLAIN.getAsString ());

    if (false)
      aRequest.setContentTransferEncoding (EContentTransferEncoding.BASE64);

    // Send message
    final AS2ClientResponse aResponse = new AS2Client ().setHttpProxy (aHttpProxy)
                                                        .sendSynchronous (aSettings, aRequest);
    if (aResponse.hasException ())
      LOGGER.info (aResponse.getAsString ());

    LOGGER.info ("Done");
  }
}
