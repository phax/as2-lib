package com.helger.as2lib.util.http;

import com.helger.as2lib.message.AS2Message;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import javax.activation.DataSource;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.helger.as2lib.params.MessageParameters.ATTR_LARGE_FILE_SUPPORT_ON;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

public class HTTPHelperTest {

	String sRegularMessage, sChunkedMessage, sRegularMessageBody, sChunkedMessageBody,
	sNoLengthMessage, sBadTransferEncodingMessage;

	@Test(expected = EOFException.class)
	public void testReadChunkLenEOS() throws Exception {
		InputStream noNewLine = new ByteArrayInputStream("1".getBytes());
		HTTPHelper.readChunkLen(noNewLine);
		fail("An EOFException should have been thrown");
	}

	@Test
	public void testReadChunkLenWithHeader() throws Exception {
		InputStream noNewLine = new ByteArrayInputStream("1A;name=value\r\n".getBytes());
		int res = HTTPHelper.readChunkLen(noNewLine);
		assertEquals("Chunk size with header",26, res);
	}

	@Test
	public void testReadChunkLenNoHeader() throws Exception {
		InputStream noNewLine = new ByteArrayInputStream("1f\n".getBytes());
		int res = HTTPHelper.readChunkLen(noNewLine);
		assertEquals("Chunk size with header",31, res);
	}

	@Test
	public void testReadChunkLenEmpty() throws Exception {
		InputStream noNewLine = new ByteArrayInputStream("\n".getBytes());
		int res = HTTPHelper.readChunkLen(noNewLine);
		assertEquals("Chunk size with header",0, res);
	}

	@Test
	public void testReadHttpRequestRegularMessage() throws Exception {
		IAS2HttpResponseHandler mockedResponseHandler = mock(IAS2HttpResponseHandler.class);
		InputStream is = new ByteArrayInputStream(sRegularMessage.getBytes());
		//non stream
		AS2Message aMsg = new AS2Message();
		aMsg.attrs ().putIn (ATTR_LARGE_FILE_SUPPORT_ON, false);
		DataSource resRegular = HTTPHelper.readHttpRequest(is, mockedResponseHandler, aMsg);
		//stream
		is = new ByteArrayInputStream(sRegularMessage.getBytes());
		aMsg = new AS2Message();
		aMsg.attrs ().putIn (ATTR_LARGE_FILE_SUPPORT_ON, true);
		DataSource resStream = HTTPHelper.readHttpRequest(is, mockedResponseHandler, aMsg);
		assertTrue("Compare regular and stream read",
			compareLineByLine(resRegular.getInputStream(), resStream.getInputStream()));
	}

	@Test
	public void testReadHttpRequestStreamMessage() throws Exception {
		IAS2HttpResponseHandler mockedResponseHandler = mock(IAS2HttpResponseHandler.class);
		InputStream is = new ByteArrayInputStream(sChunkedMessage.getBytes());
		//non stream
		AS2Message aMsg = new AS2Message();
		aMsg.attrs ().putIn (ATTR_LARGE_FILE_SUPPORT_ON, false);
		DataSource resRegular = HTTPHelper.readHttpRequest(is, mockedResponseHandler, aMsg);
		//stream
		is = new ByteArrayInputStream(sChunkedMessage.getBytes());
		aMsg = new AS2Message();
		aMsg.attrs ().putIn (ATTR_LARGE_FILE_SUPPORT_ON, true);
		DataSource resStream = HTTPHelper.readHttpRequest(is, mockedResponseHandler, aMsg);
		assertTrue("Compare regular and stream read",
			compareLineByLine(resRegular.getInputStream(), resStream.getInputStream()));
	}

	@Test(expected = IOException.class)
	public void testNoLengthMessageRegular() throws Exception {
		IAS2HttpResponseHandler mockedResponseHandler = mock(IAS2HttpResponseHandler.class);
		InputStream is = new ByteArrayInputStream(sNoLengthMessage.getBytes());
		//non stream
		AS2Message aMsg = new AS2Message();
		aMsg.attrs ().putIn (ATTR_LARGE_FILE_SUPPORT_ON, false);
		HTTPHelper.readHttpRequest(is, mockedResponseHandler, aMsg);
	}

	@Test(expected = IOException.class)
	public void testNoLengthMessageStream() throws Exception {
		IAS2HttpResponseHandler mockedResponseHandler = mock(IAS2HttpResponseHandler.class);
		InputStream is = new ByteArrayInputStream(sNoLengthMessage.getBytes());
		//stream
		is = new ByteArrayInputStream(sNoLengthMessage.getBytes());
		AS2Message aMsg = new AS2Message();
		aMsg.attrs ().putIn (ATTR_LARGE_FILE_SUPPORT_ON, true);
		HTTPHelper.readHttpRequest(is, mockedResponseHandler, aMsg);
	}

	@Test(expected = IOException.class)
	public void testBadTRansferEncodingMessageRegular() throws Exception {
		IAS2HttpResponseHandler mockedResponseHandler = mock(IAS2HttpResponseHandler.class);
		InputStream is = new ByteArrayInputStream(sBadTransferEncodingMessage.getBytes());
		//stream
		AS2Message aMsg = new AS2Message();
		aMsg.attrs ().putIn (ATTR_LARGE_FILE_SUPPORT_ON, false);
		HTTPHelper.readHttpRequest(is, mockedResponseHandler, aMsg);
	}

	@Test(expected = IOException.class)
	public void testBadTRansferEncodingMessageStream() throws Exception {
		IAS2HttpResponseHandler mockedResponseHandler = mock(IAS2HttpResponseHandler.class);
		InputStream is = new ByteArrayInputStream(sBadTransferEncodingMessage.getBytes());
		//stream
		AS2Message aMsg = new AS2Message();
		aMsg.attrs ().putIn (ATTR_LARGE_FILE_SUPPORT_ON, true);
		HTTPHelper.readHttpRequest(is, mockedResponseHandler, aMsg);
	}

	private boolean compareLineByLine(InputStream is1, InputStream is2)throws IOException{
		List<String> is1Lines = IOUtils.readLines(is1);
		List<String> is2Lines = IOUtils.readLines(is2);
		if (is1Lines.size() != is2Lines.size()){
			System.out.printf("input streams has different No of lines: %d, and %d",
				is1Lines.size(), is2Lines.size());
			return false;
		}
		for (int i=0; i< is1Lines.size(); i++){
			String l1 = is1Lines.get(i);
			String l2 = is2Lines.get(i);
			if (! l1.equals(l2)){
				System.out.printf("Input streams differ on line %d:\n1:%s\n2:%s\n",
					i, l1, l2);
				return false;
			}
		}
		return true;
	}

	@Before
	public void init(){
		sRegularMessageBody =
			"------=_Part_1_462911221.1531652105780\r\n" +
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
				"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\r\n" +
				"------=_Part_1_462911221.1531652105780--\r\n";
		sRegularMessage =
			"POST /HttpReceiver HTTP/1.1\r\n" +
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
				sRegularMessageBody;

		sChunkedMessage =
			"POST /HttpReceiver HTTP/1.1\r\n" +
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
				"4\r\n"+
		"1234"+
		"\r\n"+
		"0\r\n";
		sNoLengthMessage =
			"POST /HttpReceiver HTTP/1.1\r\n" +
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
				"4\r\n"+
				"1234"+
				"\r\n"+
				"0\r\n";
		sBadTransferEncodingMessage =
			"POST /HttpReceiver HTTP/1.1\r\n" +
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
				"4\r\n"+
				"1234"+
				"\r\n"+
				"0\r\n";
	}
}