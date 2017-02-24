/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2017 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.supplementary.functest;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeUtility;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.http.HTTPHelper;
import com.helger.commons.base64.Base64;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.io.stream.StringInputStream;
import com.helger.commons.string.StringHelper;
import com.helger.mail.cte.EContentTransferEncoding;
import com.helger.mail.datasource.ByteArrayDataSource;

/**
 * Special tests to see how {@link MimeBodyPart} works :)
 *
 * @author Philip Helger
 */
public final class MimeBodyPartFuncTest
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (MimeBodyPartFuncTest.class);

  @Test
  public void testWriteContentTransferEncoding8Bit () throws MessagingException, IOException
  {
    final String sTestMsg = "Test message\nLine 2\n\nLine 4\nEOF";

    // Build message content
    final MimeBodyPart aPart = new MimeBodyPart ();
    aPart.setText (sTestMsg, StandardCharsets.ISO_8859_1.name ());
    aPart.addHeader ("x-custom", "junit");
    aPart.addHeader (CAS2Header.HEADER_CONTENT_TYPE, "text/plain");
    aPart.addHeader (CAS2Header.HEADER_CONTENT_TRANSFER_ENCODING, EContentTransferEncoding._8BIT.getID ());
    aPart.addHeader (CAS2Header.HEADER_CONTENT_LENGTH, Integer.toString (sTestMsg.length ()));

    final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();
    aPart.writeTo (aBAOS);
    StreamHelper.close (aBAOS);

    final String sMsgPart = aBAOS.getAsString (StandardCharsets.ISO_8859_1);
    if (false)
      s_aLogger.info (sMsgPart);

    assertTrue (sMsgPart, sMsgPart.contains ("Content-Type: text/plain"));
    assertTrue (sMsgPart, sMsgPart.contains ("Content-Transfer-Encoding: 8bit"));
    assertTrue (sMsgPart, sMsgPart.contains ("x-custom: junit"));
    assertTrue (sMsgPart, sMsgPart.contains (sTestMsg));
  }

  @Test
  public void testWriteContentTransferEncodingBase64 () throws MessagingException, IOException
  {
    final String sTestMsg = "Test message\nLine 2\n\nLine 4\nEOF";
    final String sEncodedMsg = Base64.safeEncode (sTestMsg, StandardCharsets.ISO_8859_1);

    // Build message content
    final MimeBodyPart aPart = new MimeBodyPart ();
    aPart.setText (sTestMsg, StandardCharsets.ISO_8859_1.name ());
    aPart.addHeader ("x-custom", "junit");
    aPart.addHeader (CAS2Header.HEADER_CONTENT_TYPE, "text/plain");
    aPart.addHeader (CAS2Header.HEADER_CONTENT_TRANSFER_ENCODING, EContentTransferEncoding.BASE64.getID ());
    aPart.addHeader (CAS2Header.HEADER_CONTENT_LENGTH, Integer.toString (sEncodedMsg.length ()));

    final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();
    aPart.writeTo (aBAOS);
    StreamHelper.close (aBAOS);

    final String sMsgPart = aBAOS.getAsString (StandardCharsets.ISO_8859_1);
    if (false)
      s_aLogger.info (sMsgPart);

    assertTrue (sMsgPart, sMsgPart.contains ("Content-Type: text/plain"));
    assertTrue (sMsgPart, sMsgPart.contains ("Content-Transfer-Encoding: base64"));
    assertTrue (sMsgPart, sMsgPart.contains ("x-custom: junit"));
    assertTrue (sMsgPart, sMsgPart.contains (sEncodedMsg));
  }

  @Test
  public void testReadContentTransferEncodingBase64 () throws MessagingException, IOException
  {
    final String sHTTP = "Content-Type: text/plain" +
                         HTTPHelper.EOL +
                         "Content-Transfer-Encoding: base64" +
                         HTTPHelper.EOL +
                         "x-custom: junit" +
                         HTTPHelper.EOL +
                         "Content-Length: 44" +
                         HTTPHelper.EOL +
                         HTTPHelper.EOL +
                         "VGVzdCBtZXNzYWdlCkxpbmUgMgoKTGluZSA0CkVPRg==" +
                         HTTPHelper.EOL;
    InputStream aIS = new StringInputStream (sHTTP, StandardCharsets.ISO_8859_1);

    // Parse all HTTP headers from stream
    final InternetHeaders aHeaders = new InternetHeaders (aIS);
    final String sCTE = aHeaders.getHeader (CAS2Header.HEADER_CONTENT_TRANSFER_ENCODING)[0];

    if (StringHelper.hasText (sCTE))
      aIS = MimeUtility.decode (aIS, sCTE);

    // Read the payload
    final byte [] aData = StreamHelper.getAllBytes (aIS);

    // Extract content type
    final ContentType aReceivedContentType = new ContentType (aHeaders.getHeader (CAS2Header.HEADER_CONTENT_TYPE)[0]);
    final String sReceivedContentType = aReceivedContentType.toString ();

    final MimeBodyPart aReceivedPart = new MimeBodyPart ();
    aReceivedPart.setDataHandler (new ByteArrayDataSource (aData, sReceivedContentType, null).getAsDataHandler ());
    aReceivedPart.setHeader ("x-received", "true");

    final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();
    aReceivedPart.writeTo (aBAOS);
    StreamHelper.close (aBAOS);

    final String sMsgPart = aBAOS.getAsString (StandardCharsets.ISO_8859_1);
    if (true)
      s_aLogger.info (sMsgPart);
  }
}
