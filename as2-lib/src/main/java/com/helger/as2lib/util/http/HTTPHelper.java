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
package com.helger.as2lib.util.http;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.message.IBaseMessage;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.IOHelper;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.string.StringHelper;
import com.helger.commons.system.SystemProperties;

/**
 * HTTP utility methods.
 *
 * @author Philip Helger
 */
public final class HTTPHelper
{
  /** The request method used (POST or GET) */
  public static final String MA_HTTP_REQ_TYPE = "HTTP_REQUEST_TYPE";
  /** The request URL used - defaults to "/" */
  public static final String MA_HTTP_REQ_URL = "HTTP_REQUEST_URL";
  /** The HTTP version used. E.g. "HTTP/1.1" */
  public static final String MA_HTTP_REQ_VERSION = "HTTP_REQUEST_VERSION";

  /* End of line MUST be \r and \n */
  public static final String EOL = "\r\n";

  private static final Logger s_aLogger = LoggerFactory.getLogger (HTTPHelper.class);
  private static IHTTPIncomingDumper s_aHTTPIncomingDumper = null;
  private static IHTTPOutgoingDumper s_aHTTPOutgoingDumper = null;

  static
  {
    final String sHttpDumpDirectory = SystemProperties.getPropertyValueOrNull ("AS2.httpDumpDirectory");
    if (StringHelper.hasText (sHttpDumpDirectory))
    {
      final File aDumpDirectory = new File (sHttpDumpDirectory);
      IOHelper.getFileOperationManager ().createDirIfNotExisting (aDumpDirectory);
      setHTTPIncomingDumper (new HTTPIncomingDumperDirectoryBased (aDumpDirectory));
    }
  }

  private HTTPHelper ()
  {}

  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsList <String> getAllHTTPHeaderLines (@Nonnull final InternetHeaders aHeaders)
  {
    final ICommonsList <String> ret = new CommonsArrayList <> ();
    final Enumeration <?> aEnum = aHeaders.getAllHeaderLines ();
    while (aEnum.hasMoreElements ())
      ret.add ((String) aEnum.nextElement ());
    return ret;
  }

  @Nonnull
  @Nonempty
  public static String getHTTPResponseMessage (final int nResponseCode)
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

  @Nonnull
  public static byte [] readHttpPayload (@Nonnull final InputStream aIS,
                                         @Nonnull final IAS2HttpResponseHandler aResponseHandler,
                                         @Nonnull final IMessage aMsg) throws IOException
  {
    ValueEnforcer.notNull (aIS, "InputStream");
    ValueEnforcer.notNull (aResponseHandler, "ResponseHandler");
    ValueEnforcer.notNull (aMsg, "Msg");

    final DataInputStream aDataIS = new DataInputStream (aIS);

    // Retrieve the message content
    byte [] aData = null;
    final String sContentLength = aMsg.getHeader (CAS2Header.HEADER_CONTENT_LENGTH);
    if (sContentLength == null)
    {
      // No "Content-Length" header present
      final String sTransferEncoding = aMsg.getHeader (CAS2Header.HEADER_TRANSFER_ENCODING);
      if (sTransferEncoding != null)
      {
        // Remove all whitespaces in the value
        if (sTransferEncoding.replaceAll ("\\s+", "").equalsIgnoreCase ("chunked"))
        {
          // chunked encoding
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
            while (true)
            {
              final int n = aDataIS.readByte ();
              if (n == '\n')
                break;
            }
          }
          aMsg.setHeader (CAS2Header.HEADER_CONTENT_LENGTH, Integer.toString (nLength));
        }
        else
        {
          // No "Content-Length" and unsupported "Transfer-Encoding"
          sendSimpleHTTPResponse (aResponseHandler, HttpURLConnection.HTTP_LENGTH_REQUIRED);
          throw new IOException ("Transfer-Encoding unimplemented: " + sTransferEncoding);
        }
      }
      else
      {
        // No "Content-Length" and no "Transfer-Encoding"
        sendSimpleHTTPResponse (aResponseHandler, HttpURLConnection.HTTP_LENGTH_REQUIRED);
        throw new IOException ("Content-Length missing");
      }
    }
    else
    {
      // "Content-Length" is present
      // Receive the transmission's data
      // XX if a value > 2GB comes in, this will fail!!
      final int nContentSize = Integer.parseInt (sContentLength);
      aData = new byte [nContentSize];
      aDataIS.readFully (aData);
    }

    return aData;
  }

  /**
   * Read the first line of the HTTP request InputStream and parse out HTTP
   * method (e.g. "GET" or "POST"), request URL (e.g "/as2") and HTTP version
   * (e.g. "HTTP/1.1")
   *
   * @param aIS
   *        Stream to read the first line from
   * @return An array with 3 elements, containing method, URL and HTTP version
   * @throws IOException
   *         In case of IO error
   */
  @Nonnull
  @Nonempty
  private static String [] _readRequestInfo (@Nonnull final InputStream aIS) throws IOException
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
      // read in the \n following the "\r"
      aIS.read ();
    }

    final StringTokenizer aTokens = new StringTokenizer (aSB.toString (), " ");
    final int nTokenCount = aTokens.countTokens ();
    if (nTokenCount >= 3)
    {
      // Return all tokens
      final String [] aRequestParts = new String [nTokenCount];
      for (int i = 0; i < nTokenCount; i++)
        aRequestParts[i] = aTokens.nextToken ();
      return aRequestParts;
    }

    if (nTokenCount == 2)
    {
      // Default the request URL to "/"
      final String [] aRequestParts = new String [3];
      aRequestParts[0] = aTokens.nextToken ();
      aRequestParts[1] = "/";
      aRequestParts[2] = aTokens.nextToken ();
      return aRequestParts;
    }
    throw new IOException ("Invalid HTTP Request (" + aSB.toString () + ")");
  }

  /**
   * @return <code>true</code> if a dumper for incoming HTTP requests is
   *         installed, <code>false</code> otherwise
   * @since 3.0.1
   */
  public static boolean isHTTPIncomingDumpEnabled ()
  {
    return getHTTPIncomingDumper () != null;
  }

  /**
   * @return the dumper for incoming HTTP requests or <code>null</code> if none
   *         is present
   * @since 3.0.1
   */
  @Nullable
  public static IHTTPIncomingDumper getHTTPIncomingDumper ()
  {
    return s_aHTTPIncomingDumper;
  }

  /**
   * Set the dumper for incoming HTTP requests or <code>null</code> if none
   * should be used
   *
   * @param aHttpDumper
   *        The dumper to be used. May be <code>null</code>.
   * @since 3.0.1
   */
  public static void setHTTPIncomingDumper (@Nullable final IHTTPIncomingDumper aHttpDumper)
  {
    s_aHTTPIncomingDumper = aHttpDumper;
    if (aHttpDumper != null)
      s_aLogger.info ("Using the following handler to dump incoming requests: " + aHttpDumper);
    else
      s_aLogger.info ("Incoming request dumping is disabled.");
  }

  /**
   * @return <code>true</code> if a dumper for outgoing HTTP requests is
   *         installed, <code>false</code> otherwise
   * @since 3.0.1
   */
  public static boolean isHTTPOutgoingDumpEnabled ()
  {
    return getHTTPOutgoingDumper () != null;
  }

  /**
   * @return the dumper for outgoing HTTP requests or <code>null</code> if none
   *         is present
   * @since 3.0.1
   */
  @Nullable
  public static IHTTPOutgoingDumper getHTTPOutgoingDumper ()
  {
    return s_aHTTPOutgoingDumper;
  }

  /**
   * Set the dumper for outgoing HTTP requests or <code>null</code> if none
   * should be used
   *
   * @param aHttpDumper
   *        The dumper to be used. May be <code>null</code>.
   * @since 3.0.1
   */
  public static void setHTTPOutgoingDumper (@Nullable final IHTTPOutgoingDumper aHttpDumper)
  {
    s_aHTTPOutgoingDumper = aHttpDumper;
    if (aHttpDumper != null)
      s_aLogger.info ("Using the following handler to dump outgoing requests: " + aHttpDumper);
    else
      s_aLogger.info ("Outgoing request dumping is disabled.");
  }

  /**
   * @param aHeaderLines
   *        Header lines.
   * @param aPayload
   *        Received payload.
   * @deprecated Use
   *             {@link #dumpIncomingHttpRequest(ICommonsList,byte[],IBaseMessage)}
   *             instead
   */
  @Deprecated
  public static void dumpHttpRequest (@Nonnull final ICommonsList <String> aHeaderLines,
                                      @Nonnull final byte [] aPayload)
  {
    dumpIncomingHttpRequest (aHeaderLines, aPayload, (IBaseMessage) null);
  }

  /**
   * Dump an incoming HTTP request using the globally set HTTP dumper.
   *
   * @param aHeaderLines
   *        Header lines.
   * @param aPayload
   *        Received payload.
   * @param aMsg
   *        The message stub. May be <code>null</code> for legacy reasons.
   * @see #getHTTPIncomingDumper()
   * @see #setHTTPIncomingDumper(IHTTPIncomingDumper)
   */
  public static void dumpIncomingHttpRequest (@Nonnull final ICommonsList <String> aHeaderLines,
                                              @Nonnull final byte [] aPayload,
                                              @Nullable final IBaseMessage aMsg)
  {
    if (s_aHTTPIncomingDumper != null)
      s_aHTTPIncomingDumper.dumpIncomingRequest (aHeaderLines, aPayload, aMsg);
  }

  /**
   * Read headers and payload from the passed input stream provider.
   *
   * @param aISP
   *        The abstract input stream provider to use. May not be
   *        <code>null</code>.
   * @param aResponseHandler
   *        The HTTP response handler to be used. May not be <code>null</code>.
   * @param aMsg
   *        The Message to be filled. May not be <code>null</code>.
   * @return The payload of the HTTP request.
   * @throws IOException
   *         In case of error reading from the InputStream
   * @throws MessagingException
   *         In case header line parsing fails
   */
  @Nonnull
  public static byte [] readHttpRequest (@Nonnull final IAS2InputStreamProvider aISP,
                                         @Nonnull final IAS2HttpResponseHandler aResponseHandler,
                                         @Nonnull final IMessage aMsg) throws IOException, MessagingException
  {
    // Get the stream to read from
    final InputStream aIS = aISP.getInputStream ();
    if (aIS == null)
      throw new IllegalStateException ("Failed to open InputStream from " + aISP);

    // Read the HTTP meta data
    final String [] aRequest = _readRequestInfo (aIS);
    // Request method (e.g. "POST")
    aMsg.setAttribute (MA_HTTP_REQ_TYPE, aRequest[0]);
    // Request URL (e.g. "/as2")
    aMsg.setAttribute (MA_HTTP_REQ_URL, aRequest[1]);
    // HTTP version (e.g. "HTTP/1.1")
    aMsg.setAttribute (MA_HTTP_REQ_VERSION, aRequest[2]);

    // Parse all HTTP headers from stream
    final InternetHeaders aHeaders = new InternetHeaders (aIS);
    aMsg.setHeaders (aHeaders);

    // Read the message body - no Content-Transfer-Encoding handling
    final byte [] aPayload = readHttpPayload (aIS, aResponseHandler, aMsg);

    // Dump on demand
    if (isHTTPIncomingDumpEnabled ())
      dumpIncomingHttpRequest (getAllHTTPHeaderLines (aHeaders), aPayload, aMsg);

    return aPayload;

    // Don't close the IS here!
  }

  /**
   * Send a simple HTTP response that only contains the HTTP status code and the
   * respective descriptive text.
   *
   * @param aResponseHandler
   *        The response handler to be used.
   * @param nResponseCode
   *        The HTTP response code to use.
   * @throws IOException
   *         In case sending fails for whatever reason
   */
  public static void sendSimpleHTTPResponse (@Nonnull final IAS2HttpResponseHandler aResponseHandler,
                                             @Nonnegative final int nResponseCode) throws IOException
  {
    try (final NonBlockingByteArrayOutputStream aData = new NonBlockingByteArrayOutputStream ())
    {
      final String sHTTPLine = Integer.toString (nResponseCode) + " " + getHTTPResponseMessage (nResponseCode) + EOL;
      aData.write (sHTTPLine.getBytes (StandardCharsets.ISO_8859_1));

      final InternetHeaders aHeaders = new InternetHeaders ();
      aResponseHandler.sendHttpResponse (nResponseCode, aHeaders, aData);
    }
  }

  /**
   * Copy headers from an HTTP connection to an InternetHeaders object
   *
   * @param aConn
   *        Connection - source. May not be <code>null</code>.
   * @param aHeaders
   *        Headers - destination. May not be <code>null</code>.
   */
  public static void copyHttpHeaders (@Nonnull final HttpURLConnection aConn, @Nonnull final InternetHeaders aHeaders)
  {
    for (final Map.Entry <String, List <String>> aConnHeader : aConn.getHeaderFields ().entrySet ())
    {
      final String sHeaderName = aConnHeader.getKey ();
      if (sHeaderName != null)
        for (final String sHeaderValue : aConnHeader.getValue ())
        {
          if (aHeaders.getHeader (sHeaderName) == null)
            aHeaders.setHeader (sHeaderName, sHeaderValue);
          else
            aHeaders.addHeader (sHeaderName, sHeaderValue);
        }
    }
  }
}
