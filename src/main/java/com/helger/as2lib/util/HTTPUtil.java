/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2014 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;
import javax.annotation.WillNotClose;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;

import com.helger.as2lib.message.IMessage;
import com.helger.commons.annotations.Nonempty;

public final class HTTPUtil
{
  public static final String MA_HTTP_REQ_TYPE = "HTTP_REQUEST_TYPE";
  public static final String MA_HTTP_REQ_URL = "HTTP_REQUEST_URL";

  private HTTPUtil ()
  {}

  @Nonnull
  @Nonempty
  private static String _getHTTPResponseMessage (final int nResponseCode)
  {
    String sMsg;
    switch (nResponseCode)
    {
      case 100:
        sMsg = "Continue";
        break;
      case 101:
        sMsg = "Switching Protocols";
        break;
      case 200:
        sMsg = "OK";
        break;
      case 201:
        sMsg = "Created";
        break;
      case 202:
        sMsg = "Accepted";
        break;
      case 203:
        sMsg = "Non-Authoritative Information";
        break;
      case 204:
        sMsg = "No Content";
        break;
      case 205:
        sMsg = "Reset Content";
        break;
      case 206:
        sMsg = "Partial Content";
        break;
      case 300:
        sMsg = "Multiple Choices";
        break;
      case 301:
        sMsg = "Moved Permanently";
        break;
      case 302:
        sMsg = "Found";
        break;
      case 303:
        sMsg = "See Other";
        break;
      case 304:
        sMsg = "Not Modified";
        break;
      case 305:
        sMsg = "Use Proxy";
        break;
      case 307:
        sMsg = "Temporary Redirect";
        break;
      case 400:
        sMsg = "Bad Request";
        break;
      case 401:
        sMsg = "Unauthorized";
        break;
      case 402:
        sMsg = "Payment Required";
        break;
      case 403:
        sMsg = "Forbidden";
        break;
      case 404:
        sMsg = "Not Found";
        break;
      case 405:
        sMsg = "Method Not Allowed";
        break;
      case 406:
        sMsg = "Not Acceptable";
        break;
      case 407:
        sMsg = "Proxy Authentication Required";
        break;
      case 408:
        sMsg = "Request Time-out";
        break;
      case 409:
        sMsg = "Conflict";
        break;
      case 410:
        sMsg = "Gone";
        break;
      case 411:
        sMsg = "Length Required";
        break;
      case 412:
        sMsg = "Precondition Failed";
        break;
      case 413:
        sMsg = "Request Entity Too Large";
        break;
      case 414:
        sMsg = "Request-URI Too Large";
        break;
      case 415:
        sMsg = "Unsupported Media Type";
        break;
      case 416:
        sMsg = "Requested range not satisfiable";
        break;
      case 417:
        sMsg = "Expectation Failed";
        break;
      case 500:
        sMsg = "Internal Server Error";
        break;
      case 501:
        sMsg = "Not Implemented";
        break;
      case 502:
        sMsg = "Bad Gateway";
        break;
      case 503:
        sMsg = "Service Unavailable";
        break;
      case 504:
        sMsg = "Gateway Time-out";
        break;
      case 505:
        sMsg = "HTTP Version not supported";
        break;
      default:
        sMsg = "Unknown (" + nResponseCode + ")";
        break;
    }
    return sMsg;
  }

  public static byte [] readData (@Nonnull final IAS2InputStreamProvider aISP,
                                  @Nonnull final IAS2OutputStreamCreator aOSC,
                                  @Nonnull final IMessage aMsg) throws IOException, MessagingException
  {
    byte [] aData = null;
    // Get the stream and read in the HTTP request and headers
    final InputStream aIS = aISP.getInputStream ();
    final String [] aRequest = _readRequest (aIS);
    aMsg.setAttribute (MA_HTTP_REQ_TYPE, aRequest[0]);
    aMsg.setAttribute (MA_HTTP_REQ_URL, aRequest[1]);
    aMsg.setHeaders (new InternetHeaders (aIS));
    final DataInputStream aDataIS = new DataInputStream (aIS);
    // Retrieve the message content
    if (aMsg.getHeader (CAS2Header.HEADER_CONTENT_LENGTH) == null)
    {
      final String sTransferEncoding = aMsg.getHeader (CAS2Header.HEADER_TRANSFER_ENCODING);
      if (sTransferEncoding != null)
      {
        if (sTransferEncoding.replaceAll ("\\s+", "").equalsIgnoreCase ("chunked"))
        {
          int nLength = 0;
          for (;;)
          {
            // First get hex chunk length; followed by CRLF
            int nBlocklen = 0;
            for (;;)
            {
              int ch = aDataIS.readByte ();
              if (ch == '\n')
                break;
              if (ch >= 'a' && ch <= 'f')
                ch -= ('a' - 10);
              else
                if (ch >= 'A' && ch <= 'F')
                  ch -= ('A' - 10);
                else
                  if (ch >= '0' && ch <= '9')
                    ch -= '0';
                  else
                    continue;
              nBlocklen = (nBlocklen * 16) + ch;
            }
            // Zero length is end of chunks
            if (nBlocklen == 0)
              break;
            // Ok, now read new chunk
            final int nNewlen = nLength + nBlocklen;
            final byte [] aNewData = new byte [nNewlen];
            if (nLength > 0)
              System.arraycopy (aData, 0, aNewData, 0, nLength);
            aDataIS.readFully (aNewData, nLength, nBlocklen);
            aData = aNewData;
            nLength = nNewlen;
            // And now the CRLF after the chunk;
            while (aDataIS.readByte () != '\n')
            {}
          }
          aMsg.setHeader (CAS2Header.HEADER_CONTENT_LENGTH, Integer.toString (nLength));
        }
        else
        {
          sendHTTPResponse (aOSC.createOutputStream (), HttpURLConnection.HTTP_LENGTH_REQUIRED, false);
          throw new IOException ("Transfer-Encoding unimplemented: " + sTransferEncoding);
        }
      }
      else
        if (aMsg.getHeader (CAS2Header.HEADER_CONTENT_LENGTH) == null)
        {
          sendHTTPResponse (aOSC.createOutputStream (), HttpURLConnection.HTTP_LENGTH_REQUIRED, false);
          throw new IOException ("Content-Length missing");
        }
    }
    else
    {
      // Receive the transmission's data
      final int nContentSize = Integer.parseInt (aMsg.getHeader (CAS2Header.HEADER_CONTENT_LENGTH));
      aData = new byte [nContentSize];
      aDataIS.readFully (aData);
    }
    return aData;
  }

  @Nonnull
  private static String [] _readRequest (@Nonnull final InputStream aIS) throws IOException
  {
    int nByteBuf = aIS.read ();
    final StringBuilder aSB = new StringBuilder ();
    while (nByteBuf != -1 && nByteBuf != '\r')
    {
      aSB.append ((char) nByteBuf);
      nByteBuf = aIS.read ();
    }
    if (nByteBuf != -1)
    {
      // read in the \n
      aIS.read ();
    }

    final StringTokenizer aTokens = new StringTokenizer (aSB.toString (), " ");
    final int nTokenCount = aTokens.countTokens ();
    if (nTokenCount >= 3)
    {
      final String [] aRequestParts = new String [nTokenCount];
      for (int i = 0; i < nTokenCount; i++)
        aRequestParts[i] = aTokens.nextToken ();
      return aRequestParts;
    }

    if (nTokenCount == 2)
    {
      final String [] aRequestParts = new String [3];
      aRequestParts[0] = aTokens.nextToken ();
      aRequestParts[1] = "/";
      aRequestParts[2] = aTokens.nextToken ();
      return aRequestParts;
    }
    throw new IOException ("Invalid HTTP Request");
  }

  public static void sendHTTPResponse (@Nonnull @WillNotClose final OutputStream aOS,
                                       final int nResponseCode,
                                       final boolean bHasData) throws IOException
  {
    final String sMsg = Integer.toString (nResponseCode) + " " + _getHTTPResponseMessage (nResponseCode) + "\r\n";
    aOS.write (("HTTP/1.1 " + sMsg).getBytes ());
    if (!bHasData)
    {
      // if no data will be sent, write the HTTP code
      aOS.write ("\r\n".getBytes ());
      aOS.write (sMsg.getBytes ());
    }
  }
}
