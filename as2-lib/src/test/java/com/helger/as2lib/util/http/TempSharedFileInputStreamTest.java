package com.helger.as2lib.util.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import javax.mail.util.SharedFileInputStream;

import org.junit.Test;

import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;

/**
 * Test class of class {@link TempSharedFileInputStream}.
 * 
 * @author Ziv Harpaz
 */
public final class TempSharedFileInputStreamTest
{
  @Test
  public void testGetTempSharedFileInputStream () throws Exception
  {
    final String inData = "123456";
    try (final InputStream is = new NonBlockingByteArrayInputStream (inData.getBytes ());
        final SharedFileInputStream sis = TempSharedFileInputStream.getTempSharedFileInputStream (is, "myName"))
    {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
      org.apache.commons.io.IOUtils.copy (sis, baos);
      final String res = baos.toString ();
      assertEquals ("read the data", inData, res);
      sis.close ();
    }
  }

  @Test
  public void testStoreContentToTempFile () throws Exception
  {
    final String inData = "123456";
    final String name = "xxy";
    try (final InputStream is = new NonBlockingByteArrayInputStream (inData.getBytes ()))
    {
      final File file = TempSharedFileInputStream.storeContentToTempFile (is, name);
      assertTrue ("Temp file exists", file.exists ());
      assertTrue ("Temp file name includes given name", file.getName ().indexOf (name) > 0);
      // noinspection ResultOfMethodCallIgnored
      file.delete ();
    }
  }

  @Test
  public void testFinalize () throws Exception
  {
    for (int i = 0; i < 10000; i++)
    {
      final String inData = "123456";
      try (final InputStream is = new NonBlockingByteArrayInputStream (inData.getBytes ());
          final TempSharedFileInputStream sis = TempSharedFileInputStream.getTempSharedFileInputStream (is, "myName"))
      {
        final int t = sis.read ();
        assertEquals (t, inData.charAt (0));
        sis.closeAll ();
      }
    }
  }
}
