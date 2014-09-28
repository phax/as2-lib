package com.helger.as2lib.util.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Enumeration;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.mail.internet.InternetHeaders;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.streams.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.streams.StreamUtils;

public final class AS2HttpResponseHandlerSocket implements IAS2HttpResponseHandler
{
  private final Socket m_aSocket;

  public AS2HttpResponseHandlerSocket (@Nonnull final Socket aSocket)
  {
    m_aSocket = aSocket;
  }

  @Nonnull
  public OutputStream createOutputStream () throws IOException
  {
    return StreamUtils.getBuffered (m_aSocket.getOutputStream ());
  }

  public void sendHttpResponse (@Nonnegative final int nHttpResponseCode,
                                @Nonnull final InternetHeaders aHeaders,
                                @Nonnull final NonBlockingByteArrayOutputStream aData) throws IOException
  {
    ValueEnforcer.isGT0 (nHttpResponseCode, "HttpResponseCode");
    ValueEnforcer.notNull (aHeaders, "Headers");
    ValueEnforcer.notNull (aData, "Data");

    final OutputStream aOS = createOutputStream ();

    // Send HTTP version and response code
    final String sMsg = Integer.toString (nHttpResponseCode) +
                        " " +
                        HTTPUtil.getHTTPResponseMessage (nHttpResponseCode) +
                        "\r\n";
    aOS.write (("HTTP/1.1 " + sMsg).getBytes ());

    // Add headers
    final Enumeration <?> aHeaderLines = aHeaders.getAllHeaderLines ();
    while (aHeaderLines.hasMoreElements ())
    {
      final String sHeader = (String) aHeaderLines.nextElement () + "\r\n";
      aOS.write (sHeader.getBytes ());
    }
    aOS.write ("\r\n".getBytes ());

    // Write body
    aData.writeTo (aOS);

    // Done
    aOS.flush ();
    aOS.close ();
  }
}
