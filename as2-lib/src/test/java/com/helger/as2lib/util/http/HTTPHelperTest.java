/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2020 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.util.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.activation.DataSource;
import javax.annotation.Nonnull;
import javax.annotation.WillClose;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.util.dump.IHTTPIncomingDumper;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.io.stream.StringInputStream;

public final class HTTPHelperTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (HTTPHelperTest.class);
  private String m_sRegularMessage;
  private String m_sChunkedMessage;
  private String m_sRegularMessageBody;
  private String m_sNoLengthMessage;
  private String m_sBadTransferEncodingMessage;

  @Before
  public void init ()
  {
    m_sRegularMessageBody = "------=_Part_1_462911221.1531652105780\r\n" +
                            "Content-Type: application/xml; name=dummy.txt\r\n" +
                            "Content-Transfer-Encoding: 7bit\r\n" +
                            "Content-Disposition: attachment; filename=dummy.txt\r\n" +
                            "\r\n" +
                            "EOF\r\n" +
                            "------=_Part_1_462911221.1531652105780\r\n" +
                            "Content-Type: application/pkcs7-signature; name=smime.p7s; smime-type=signed-data\r\n" +
                            "Content-Transfer-Encoding: base64\r\n" +
                            "Content-Disposition: attachment; filename=\"smime.p7s\"\r\n" +
                            "Content-Description: S/MIME Cryptographic Signature\r\n" +
                            "\r\n" +
                            "MIAGCSqGSIb3DQEHAqCAMIACAQExDzANBglghkgBZQMEAgMFADCABgkqhkiG9w0BBwEAAKCAMIID\r\n" +
                            "2jCCAsKgAwIBAgIJAIIwcTXVbDA0MA0GCSqGSIb3DQEBCwUAMG8xCzAJBgNVBAYTAk5BMRAwDgYD\r\n" +
                            "VQQIDAdDVE1EZW1vMRAwDgYDVQQHDAdDVE1EZW1vMREwDwYDVQQKDAhDVE0gRGVtbzEXMBUGA1UE\r\n" +
                            "CwwOQk1DIENUTURlbW8gQ0ExEDAOBgNVBAMMB01GVGRlbW8wHhcNMTgwNjEzMTYwNzAyWhcNMjUw\r\n" +
                            "NDE3MTYwNzAyWjB3MQswCQYDVQQGEwJOQTEQMA4GA1UECAwHQ1RNRGVtbzEQMA4GA1UEBwwHQ1RN\r\n" +
                            "RGVtbzERMA8GA1UECgwIQ1RNIERlbW8xIDAeBgNVBAsMF0JNQyBDVE1EZW1vIFNlcnZlciBDZXJ0\r\n" +
                            "MQ8wDQYDVQQDDAZ1c2VyX2MwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC2JgAVSBBL\r\n" +
                            "wjClg3sIJN5fj1k66hhUzSoore9gSwafIkmJ3/CxcqSZL/SkpJTRbfaUq9bgT0mTcbCG9isTqWIl\r\n" +
                            "UEVQVrNqteG6hrngOKAWjJL4RrhUfZLDT2EVrQspyj56TXaS3Mt08sqovY+YdsodHINeuie9bSin\r\n" +
                            "DTBKxF36gVQdK3otyG8jOhAtPs2v+E74LUCFzCRx/U9ieOjeUm75wae3IxORyphYPgdQoNTJKaxc\r\n" +
                            "kFXrqVzsdEne5HHYCSgK+mlvbw4pgD7vEckoi5p3HX2cqhJd9JCYNnsMc8jIianKfJIJ4fzeIGck\r\n" +
                            "11oYbSQoFCKhZOq1GUJ4cOiuw8mpAgMBAAGjcTBvMB8GA1UdIwQYMBaAFDZCZ9iMcW+BmfYGWrm4\r\n" +
                            "PmbsJJOEMAkGA1UdEwQCMAAwHQYDVR0OBBYEFN7OX7z1yGQjPM+nNiPvJksCV4MUMAsGA1UdDwQE\r\n" +
                            "AwIE8DAVBgNVHREEDjAMggp0ZXN0U2VuZGVyMA0GCSqGSIb3DQEBCwUAA4IBAQCly1Qpeuw6LrpP\r\n" +
                            "pVcaeW08QZrBRebDD1wNjYh2cLKIm/FNHMWQRBeYwwegTH7DFzVrL8FsTLRf6yXrh0ClW8HKgymS\r\n" +
                            "k8hc364dYRSHtXyqiNTSapZbl5vTIieubC0pMaGnS1tLPIUOgYCqa1FxenMwdXLY8BIt8+wSI+5G\r\n" +
                            "skiwA9R1zUbxa/m5MsavK6o7KikKBagJ45jOzNVtKJVO/2k2PxA7lyYSAsv/G6VWRNqsTFKPR46A\r\n" +
                            "ZegBO4lTJYJ0wnFOUAh3DRPaIjcDsghXXGfhdbiQJHT0YK7ua6yS7n0EJHFo0ZdUw+x3VkavHI0L\r\n" +
                            "RQ5n8rUR32Q8SoYHSAqedN2RAAAxggKAMIICfAIBATB8MG8xCzAJBgNVBAYTAk5BMRAwDgYDVQQI\r\n" +
                            "DAdDVE1EZW1vMRAwDgYDVQQHDAdDVE1EZW1vMREwDwYDVQQKDAhDVE0gRGVtbzEXMBUGA1UECwwO\r\n" +
                            "Qk1DIENUTURlbW8gQ0ExEDAOBgNVBAMMB01GVGRlbW8CCQCCMHE11WwwNDANBglghkgBZQMEAgMF\r\n" +
                            "AKCB1jAYBgkqhkiG9w0BCQMxCwYJKoZIhvcNAQcBMBwGCSqGSIb3DQEJBTEPFw0xODA3MTUxMDU1\r\n" +
                            "MDZaMBwGCSqGSIb3DQEJDzEPMA0wCwYJYIZIAWUDBAIDMC0GCSqGSIb3DQEJNDEgMB4wDQYJYIZI\r\n" +
                            "AWUDBAIDBQChDQYJKoZIhvcNAQENBQAwTwYJKoZIhvcNAQkEMUIEQEU4umZZOH86xvI2gquHoyhF\r\n" +
                            "8odEYrm2GBKfCtKYSRA8oKGXp4bObjishBbPw25nw4j7/+N7iyUhAIUQLj9c6zEwDQYJKoZIhvcN\r\n" +
                            "AQENBQAEggEAhdX1Xg0/OFTt+JEG/JXhl58OqhndKk3QlZ+KvNEVUwaR6KDr/OHataJGzvJvYT0W\r\n" +
                            "WL3DH2w5mxEErGXefIu2FOJaeJJkeoypmpLfEOZAcjfjk2slLGS8pphqSRC7cPjzRhTUvHJkiZSN\r\n" +
                            "vEMnrSe+jUhwt+Cu8UxarMpi59nYCY35XNYN37EFl3XymO/BmzlKH4GNy8Kj2SBTHWR+pwpxQsOF\r\n" +
                            "93At8vbUUcs1ff7WNABg9zOtAckD9rrSMsSl199AoQ7kyYmv7LDJpPFZ1jAxHlXrstjG8ooKZM0i\r\n" +
                            "Nqmslehi0Yk16Mr3DnLtD5ewSNt6NtRnxhHHkzxG/ZC53nE2FgAAAAAAAA==\r\n" +
                            "------=_Part_1_462911221.1531652105780--\r\n";
    m_sRegularMessage = "POST /HttpReceiver HTTP/1.1\r\n" +
                        "content-type: multipart/signed; protocol=\"application/pkcs7-signature\"; micalg=sha-512;    boundary=\"----=_Part_1_462911221.1531652105780\"\r\n" +
                        "subject: [--sign]\r\n" +
                        "message-id: <ph-OpenAS2-15072018135504+0300-0583@testsender_testreceiver>\r\n" +
                        "content-disposition: attachment; filename=dummy.txt\r\n" +
                        "User-Agent: ph-OpenAS2/AS2Sender\r\n" +
                        "Date: Sun, 15 Jul 2018 13:55:06 +0300\r\n" +
                        "Mime-Version: 1.0\r\n" +
                        "AS2-Version: 1.1\r\n" +
                        "Recipient-Address: http://localhost:10080/HttpReceiver\r\n" +
                        "AS2-From: testsender\r\n" +
                        "AS2-To: testreceiver\r\n" +
                        "From: email@example.org\r\n" +
                        "Disposition-Notification-Options: signed-receipt-protocol=required, pkcs7-signature; signed-receipt-micalg=required, sha-384\r\n" +
                        "Cache-Control: no-cache\r\n" +
                        "Pragma: no-cache\r\n" +
                        "Host: localhost:10080\r\n" +
                        "Accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2\r\n" +
                        "Connection: keep-alive\r\n" +
                        "Content-Length: 2814\r\n" +
                        "\r\n" +
                        m_sRegularMessageBody;

    m_sChunkedMessage = "POST /HttpReceiver HTTP/1.1\r\n" +
                        "Content-Type: multipart/signed; protocol=\"application/pkcs7-signature\"; micalg=sha-512;    boundary=\"----=_Part_1_1029148906.1531651777438\"\r\n" +
                        "Subject: [-Dlog4j2.debug, --stream, --sign]\r\n" +
                        "Message-ID: <ph-OpenAS2-15072018134936+0300-1718@testsender_testreceiver>\r\n" +
                        "Content-Disposition: attachment; filename=dummy.txt\r\n" +
                        "Connection: close, TE\r\n" +
                        "User-Agent: ph-OpenAS2/AS2Sender\r\n" +
                        "Date: Sun, 15 Jul 2018 13:49:37 +0300\r\n" +
                        "Mime-Version: 1.0\r\n" +
                        "AS2-Version: 1.1\r\n" +
                        "Recipient-Address: http://localhost:10080/HttpReceiver\r\n" +
                        "AS2-From: testsender\r\n" +
                        "AS2-To: testreceiver\r\n" +
                        "From: email@example.org\r\n" +
                        "Content-Transfer-Encoding: \r\n" +
                        "Disposition-Notification-Options: signed-receipt-protocol=required, pkcs7-signature; signed-receipt-micalg=required, sha-384\r\n" +
                        "Transfer-Encoding: chunked\r\n" +
                        "Host: localhost:10080\r\n" +
                        "Accept-Encoding: gzip,deflate\r\n" +
                        "\r\n" +
                        "4\r\n" +
                        "1234" +
                        "\r\n" +
                        "0\r\n";
    m_sNoLengthMessage = "POST /HttpReceiver HTTP/1.1\r\n" +
                         "Content-Type: multipart/signed; protocol=\"application/pkcs7-signature\"; micalg=sha-512;    boundary=\"----=_Part_1_1029148906.1531651777438\"\r\n" +
                         "Subject: [-Dlog4j2.debug, --stream, --sign]\r\n" +
                         "Message-ID: <ph-OpenAS2-15072018134936+0300-1718@testsender_testreceiver>\r\n" +
                         "Content-Disposition: attachment; filename=dummy.txt\r\n" +
                         "Connection: close, TE\r\n" +
                         "User-Agent: ph-OpenAS2/AS2Sender\r\n" +
                         "Date: Sun, 15 Jul 2018 13:49:37 +0300\r\n" +
                         "Mime-Version: 1.0\r\n" +
                         "AS2-Version: 1.1\r\n" +
                         "Recipient-Address: http://localhost:10080/HttpReceiver\r\n" +
                         "AS2-From: testsender\r\n" +
                         "AS2-To: testreceiver\r\n" +
                         "From: email@example.org\r\n" +
                         "Content-Transfer-Encoding: \r\n" +
                         "Disposition-Notification-Options: signed-receipt-protocol=required, pkcs7-signature; signed-receipt-micalg=required, sha-384\r\n" +
                         "Host: localhost:10080\r\n" +
                         "Accept-Encoding: gzip,deflate\r\n" +
                         "\r\n" +
                         "4\r\n" +
                         "1234" +
                         "\r\n" +
                         "0\r\n";
    m_sBadTransferEncodingMessage = "POST /HttpReceiver HTTP/1.1\r\n" +
                                    "Content-Type: multipart/signed; protocol=\"application/pkcs7-signature\"; micalg=sha-512;    boundary=\"----=_Part_1_1029148906.1531651777438\"\r\n" +
                                    "Subject: [-Dlog4j2.debug, --stream, --sign]\r\n" +
                                    "Message-ID: <ph-OpenAS2-15072018134936+0300-1718@testsender_testreceiver>\r\n" +
                                    "Content-Disposition: attachment; filename=dummy.txt\r\n" +
                                    "Connection: close, TE\r\n" +
                                    "User-Agent: ph-OpenAS2/AS2Sender\r\n" +
                                    "Date: Sun, 15 Jul 2018 13:49:37 +0300\r\n" +
                                    "Mime-Version: 1.0\r\n" +
                                    "AS2-Version: 1.1\r\n" +
                                    "Recipient-Address: http://localhost:10080/HttpReceiver\r\n" +
                                    "AS2-From: testsender\r\n" +
                                    "AS2-To: testreceiver\r\n" +
                                    "From: email@example.org\r\n" +
                                    "Transfer-Encoding: cXXhunked\r\n" +
                                    "Content-Transfer-Encoding: \r\n" +
                                    "Disposition-Notification-Options: signed-receipt-protocol=required, pkcs7-signature; signed-receipt-micalg=required, sha-384\r\n" +
                                    "Host: localhost:10080\r\n" +
                                    "Accept-Encoding: gzip,deflate\r\n" +
                                    "\r\n" +
                                    "4\r\n" +
                                    "1234" +
                                    "\r\n" +
                                    "0\r\n";
  }

  private static final IAS2HttpResponseHandler MOCK_RH = (nHttpResponseCode, aHeaders, aData) -> {};
  private static final IHTTPIncomingDumper INCOMING_DUMPER = null;

  @Test (expected = EOFException.class)
  public void testReadChunkLenEOS () throws Exception
  {
    final InputStream noNewLine = new StringInputStream ("1", StandardCharsets.UTF_8);
    HTTPHelper.readChunkLen (noNewLine);
    fail ("An EOFException should have been thrown");
  }

  @Test
  public void testReadChunkLenWithHeader () throws Exception
  {
    final NonBlockingByteArrayInputStream noNewLine = new NonBlockingByteArrayInputStream ("1A;name=value\r\n".getBytes (StandardCharsets.UTF_8));
    final int res = HTTPHelper.readChunkLen (noNewLine);
    assertEquals ("Chunk size with header", 26, res);
  }

  @Test
  public void testReadChunkLenNoHeader () throws Exception
  {
    final NonBlockingByteArrayInputStream noNewLine = new NonBlockingByteArrayInputStream ("1f\n".getBytes (StandardCharsets.UTF_8));
    final int res = HTTPHelper.readChunkLen (noNewLine);
    assertEquals ("Chunk size with header", 31, res);
  }

  @Test
  public void testReadChunkLenEmpty () throws Exception
  {
    final NonBlockingByteArrayInputStream noNewLine = new NonBlockingByteArrayInputStream ("\n".getBytes (StandardCharsets.UTF_8));
    final int res = HTTPHelper.readChunkLen (noNewLine);
    assertEquals ("Chunk size with header", 0, res);
  }

  @Test
  public void testReadHttpRequestRegularMessage () throws Exception
  {
    // non stream
    NonBlockingByteArrayInputStream is = new NonBlockingByteArrayInputStream (m_sRegularMessage.getBytes (StandardCharsets.UTF_8));
    AS2Message aMsg = new AS2Message ();
    IAS2InputStreamProvider mockStreamProvider = new MockAS2InputStreamProvider (is);
    final DataSource resRegular = HTTPHelper.readHttpRequest (mockStreamProvider, MOCK_RH, aMsg, INCOMING_DUMPER);

    // stream
    is = new NonBlockingByteArrayInputStream (m_sRegularMessage.getBytes (StandardCharsets.UTF_8));
    aMsg = new AS2Message ();
    mockStreamProvider = new MockAS2InputStreamProvider (is);
    final DataSource resStream = HTTPHelper.readHttpRequest (mockStreamProvider, MOCK_RH, aMsg, INCOMING_DUMPER);
    assertTrue ("Compare regular and stream read", _compareLineByLine (resRegular.getInputStream (), resStream.getInputStream ()));
  }

  @Test
  public void testReadHttpRequestStreamMessage () throws Exception
  {
    // non stream
    NonBlockingByteArrayInputStream is = new NonBlockingByteArrayInputStream (m_sChunkedMessage.getBytes (StandardCharsets.UTF_8));
    AS2Message aMsg = new AS2Message ();
    IAS2InputStreamProvider mockStreamProvider = new MockAS2InputStreamProvider (is);
    final DataSource resRegular = HTTPHelper.readHttpRequest (mockStreamProvider, MOCK_RH, aMsg, INCOMING_DUMPER);

    // stream
    is = new NonBlockingByteArrayInputStream (m_sChunkedMessage.getBytes (StandardCharsets.UTF_8));
    aMsg = new AS2Message ();
    mockStreamProvider = new MockAS2InputStreamProvider (is);
    final DataSource resStream = HTTPHelper.readHttpRequest (mockStreamProvider, MOCK_RH, aMsg, INCOMING_DUMPER);

    assertTrue ("Compare regular and stream read", _compareLineByLine (resRegular.getInputStream (), resStream.getInputStream ()));
  }

  @Test (expected = IOException.class)
  public void testNoLengthMessageRegular () throws Exception
  {
    final NonBlockingByteArrayInputStream is = new NonBlockingByteArrayInputStream (m_sNoLengthMessage.getBytes (StandardCharsets.UTF_8));
    // non stream
    final AS2Message aMsg = new AS2Message ();
    final IAS2InputStreamProvider mockStreamProvider = new MockAS2InputStreamProvider (is);
    HTTPHelper.readHttpRequest (mockStreamProvider, MOCK_RH, aMsg, INCOMING_DUMPER);
  }

  @Test (expected = IOException.class)
  public void testNoLengthMessageStream () throws Exception
  {
    final NonBlockingByteArrayInputStream is = new NonBlockingByteArrayInputStream (m_sNoLengthMessage.getBytes (StandardCharsets.UTF_8));
    // stream
    final AS2Message aMsg = new AS2Message ();
    final IAS2InputStreamProvider mockStreamProvider = new MockAS2InputStreamProvider (is);
    HTTPHelper.readHttpRequest (mockStreamProvider, MOCK_RH, aMsg, INCOMING_DUMPER);
  }

  @Test (expected = IOException.class)
  public void testBadTRansferEncodingMessageRegular () throws Exception
  {
    final NonBlockingByteArrayInputStream is = new NonBlockingByteArrayInputStream (m_sBadTransferEncodingMessage.getBytes (StandardCharsets.UTF_8));
    // stream
    final AS2Message aMsg = new AS2Message ();
    final IAS2InputStreamProvider mockStreamProvider = new MockAS2InputStreamProvider (is);
    HTTPHelper.readHttpRequest (mockStreamProvider, MOCK_RH, aMsg, INCOMING_DUMPER);
  }

  @Test (expected = IOException.class)
  public void testBadTRansferEncodingMessageStream () throws Exception
  {
    final NonBlockingByteArrayInputStream is = new NonBlockingByteArrayInputStream (m_sBadTransferEncodingMessage.getBytes (StandardCharsets.UTF_8));
    // stream
    final AS2Message aMsg = new AS2Message ();
    final IAS2InputStreamProvider mockStreamProvider = new MockAS2InputStreamProvider (is);
    HTTPHelper.readHttpRequest (mockStreamProvider, MOCK_RH, aMsg, INCOMING_DUMPER);
  }

  private static boolean _compareLineByLine (@Nonnull @WillClose final InputStream is1, @Nonnull @WillClose final InputStream is2)
  {
    try
    {
      final ICommonsList <String> aLines1 = StreamHelper.readStreamLines (is1, StandardCharsets.ISO_8859_1);
      final ICommonsList <String> aLines2 = StreamHelper.readStreamLines (is2, StandardCharsets.ISO_8859_1);
      if (aLines1.size () != aLines2.size ())
      {
        if (LOGGER.isErrorEnabled ())
          LOGGER.error ("input streams has different No of lines: " + aLines1.size () + " and " + aLines2.size ());
        return false;
      }
      for (int i = 0; i < aLines1.size (); i++)
      {
        final String sLine1 = aLines1.get (i);
        final String sLine2 = aLines2.get (i);
        if (!sLine1.equals (sLine2))
        {
          if (LOGGER.isErrorEnabled ())
            LOGGER.error ("Input streams differ on line " + i + ":\n1:" + sLine1 + "\n2:" + sLine2 + "\n");
          return false;
        }
      }
      return true;
    }
    finally
    {
      StreamHelper.close (is1);
      StreamHelper.close (is2);
    }
  }
}
