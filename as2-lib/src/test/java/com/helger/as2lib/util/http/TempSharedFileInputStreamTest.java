package com.helger.as2lib.util.http;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.mail.util.SharedFileInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import static org.junit.Assert.*;

public class TempSharedFileInputStreamTest {
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetTempSharedFileInputStream() throws Exception {
		String inData = "123456";
		InputStream is = new ByteArrayInputStream(inData.getBytes());
		SharedFileInputStream sis = TempSharedFileInputStream.getTempSharedFileInputStream(is, "myName");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		org.apache.commons.io.IOUtils.copy(sis, baos);
		String res = baos.toString();
		assertEquals("read the data", inData, res);
		sis.close();
	}

	@Test
	public void testStoreContentToTempFile() throws Exception {
		String inData = "123456";
		String name = "xxy";
		InputStream is = new ByteArrayInputStream(inData.getBytes());
		File file = TempSharedFileInputStream.storeContentToTempFile(is, name);
		assertTrue("Temp file exists", file.exists());
		assertTrue("Temp file name includes given name",
			file.getName().indexOf(name)>0);
		//noinspection ResultOfMethodCallIgnored
		file.delete();
	}

	@Test
	public void testFinalize() throws Exception{
		for (int i=0;i<10000;i++) {
			String inData = "123456";
			InputStream is = new ByteArrayInputStream(inData.getBytes());
			TempSharedFileInputStream sis = TempSharedFileInputStream.getTempSharedFileInputStream(is, "myName");
			int t = sis.read();
			assertEquals(t,inData.charAt(0));
			sis.closeAll();

		}
	}
}