/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013 Philip Helger ph[at]phloc[dot]com
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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.util.StringTokenizer;

import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;

import com.helger.as2lib.message.IMessage;

public class HTTPUtil
{
  public static final String MA_HTTP_REQ_TYPE = "HTTP_REQUEST_TYPE";
  public static final String MA_HTTP_REQ_URL = "HTTP_REQUEST_URL";

  public static String getHTTPResponseMessage (final int responseCode)
  {
    String msg = "Unknown";
    switch (responseCode)
    {
      case 100:
        msg = "Continue";
        break;
      case 101:
        msg = "Switching Protocols";
        break;
      case 200:
        msg = "OK";
        break;
      case 201:
        msg = "Created";
        break;
      case 202:
        msg = "Accepted";
        break;
      case 203:
        msg = "Non-Authoritative Information";
        break;
      case 204:
        msg = "No Content";
        break;
      case 205:
        msg = "Reset Content";
        break;
      case 206:
        msg = "Partial Content";
        break;
      case 300:
        msg = "Multiple Choices";
        break;
      case 301:
        msg = "Moved Permanently";
        break;
      case 302:
        msg = "Found";
        break;
      case 303:
        msg = "See Other";
        break;
      case 304:
        msg = "Not Modified";
        break;
      case 305:
        msg = "Use Proxy";
        break;
      case 307:
        msg = "Temporary Redirect";
        break;
      case 400:
        msg = "Bad Request";
        break;
      case 401:
        msg = "Unauthorized";
        break;
      case 402:
        msg = "Payment Required";
        break;
      case 403:
        msg = "Forbidden";
        break;
      case 404:
        msg = "Not Found";
        break;
      case 405:
        msg = "Method Not Allowed";
        break;
      case 406:
        msg = "Not Acceptable";
        break;
      case 407:
        msg = "Proxy Authentication Required";
        break;
      case 408:
        msg = "Request Time-out";
        break;
      case 409:
        msg = "Conflict";
        break;
      case 410:
        msg = "Gone";
        break;
      case 411:
        msg = "Length Required";
        break;
      case 412:
        msg = "Precondition Failed";
        break;
      case 413:
        msg = "Request Entity Too Large";
        break;
      case 414:
        msg = "Request-URI Too Large";
        break;
      case 415:
        msg = "Unsupported Media Type";
        break;
      case 416:
        msg = "Requested range not satisfiable";
        break;
      case 417:
        msg = "Expectation Failed";
        break;
      case 500:
        msg = "Internal Server Error";
        break;
      case 501:
        msg = "Not Implemented";
        break;
      case 502:
        msg = "Bad Gateway";
        break;
      case 503:
        msg = "Service Unavailable";
        break;
      case 504:
        msg = "Gateway Time-out";
        break;
      case 505:
        msg = "HTTP Version not supported";
        break;
    }
    return msg;
  }

  public static byte [] readData (final Socket s, final IMessage msg) throws IOException, MessagingException
  {
    byte [] data = null;
    // Get the stream and read in the HTTP request and headers
    final BufferedInputStream in = new BufferedInputStream (s.getInputStream ());
    final String [] request = readRequest (in);
    msg.setAttribute (MA_HTTP_REQ_TYPE, request[0]);
    msg.setAttribute (MA_HTTP_REQ_URL, request[1]);
    msg.setHeaders (new InternetHeaders (in));
    final DataInputStream dataIn = new DataInputStream (in);
    // Retrieve the message content
    if (msg.getHeader ("Content-Length") == null)
    {
      final String transfer_encoding = msg.getHeader ("Transfer-Encoding");
      if (transfer_encoding != null)
      {
        if (transfer_encoding.replaceAll ("\\s+", "").equalsIgnoreCase ("chunked"))
        {
          int length = 0;
          for (;;)
          {
            // First get hex chunk length; followed by CRLF
            int blocklen = 0;
            for (;;)
            {
              int ch = dataIn.readByte ();
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
              blocklen = (blocklen * 16) + ch;
            }
            // Zero length is end of chunks
            if (blocklen == 0)
              break;
            // Ok, now read new chunk
            final int newlen = length + blocklen;
            final byte [] newdata = new byte [newlen];
            if (length > 0)
              System.arraycopy (data, 0, newdata, 0, length);
            dataIn.readFully (newdata, length, blocklen);
            data = newdata;
            length = newlen;
            // And now the CRLF after the chunk;
            while (dataIn.readByte () != '\n')
            {}
          }
          msg.setHeader ("Content-Length", new Integer (length).toString ());
        }
        else
        {
          HTTPUtil.sendHTTPResponse (s.getOutputStream (), HttpURLConnection.HTTP_LENGTH_REQUIRED, false);
          throw new IOException ("Transfer-Encoding unimplemented: " + transfer_encoding);
        }
      }
      else
        if (msg.getHeader ("Content-Length") == null)
        {
          HTTPUtil.sendHTTPResponse (s.getOutputStream (), HttpURLConnection.HTTP_LENGTH_REQUIRED, false);
          throw new IOException ("Content-Length missing");
        }
    }
    else
    {
      // Receive the transmission's data
      final int contentSize = Integer.parseInt (msg.getHeader ("Content-Length"));
      data = new byte [contentSize];
      dataIn.readFully (data);
    }
    return data;
  }

  public static String [] readRequest (final InputStream in) throws IOException
  {
    int byteBuf = in.read ();
    final StringBuilder strBuf = new StringBuilder ();
    while ((byteBuf != -1) && (byteBuf != '\r'))
    {
      strBuf.append ((char) byteBuf);
      byteBuf = in.read ();
    }
    if (byteBuf != -1)
    {
      in.read (); // read in the \n
    }
    final StringTokenizer tokens = new StringTokenizer (strBuf.toString (), " ");
    final int tokenCount = tokens.countTokens ();
    if (tokenCount >= 3)
    {
      final String [] requestParts = new String [tokenCount];
      for (int i = 0; i < tokenCount; i++)
      {
        requestParts[i] = tokens.nextToken ();
      }
      return requestParts;
    }
    else
      if (tokenCount == 2)
      {
        final String [] requestParts = new String [3];
        requestParts[0] = tokens.nextToken ();
        requestParts[1] = "/";
        requestParts[2] = tokens.nextToken ();
        return requestParts;
      }
      else
      {
        throw new IOException ("Invalid HTTP Request");
      }
  }

  public static void sendHTTPResponse (final OutputStream out, final int responseCode, final boolean bHasData) throws IOException
  {
    final StringBuilder aSB = new StringBuilder ();
    aSB.append (responseCode).append (' ').append (getHTTPResponseMessage (responseCode)).append ("\r\n");
    final StringBuilder aResponse = new StringBuilder ("HTTP/1.1 ");
    aResponse.append (aSB);
    out.write (aResponse.toString ().getBytes ());
    if (!bHasData)
    {
      // if no data will be sent, write the HTTP code
      out.write ("\r\n".getBytes ());
      out.write (aSB.toString ().getBytes ());
    }
  }
}
