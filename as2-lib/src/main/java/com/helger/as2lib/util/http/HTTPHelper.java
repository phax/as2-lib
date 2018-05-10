/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2018 Philip Helger philip[at]helger[dot]com
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
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;

import com.helger.as2lib.message.IBaseMessage;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.util.AS2IOHelper;
import com.helger.as2lib.util.dump.HTTPIncomingDumperDirectoryBased;
import com.helger.as2lib.util.dump.IHTTPIncomingDumper;
import com.helger.as2lib.util.dump.IHTTPOutgoingDumper;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.functional.IFunction;
import com.helger.commons.functional.ISupplier;
import com.helger.commons.http.CHttp;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.http.HttpHeaderMap;
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

  private static ISupplier <? extends IHTTPIncomingDumper> s_aHTTPIncomingDumperFactory = () -> null;
  private static IFunction <? super IBaseMessage, ? extends IHTTPOutgoingDumper> s_aHTTPOutgoingDumperFactory = x -> null;

  static
  {
    final String sHttpDumpDirectory = SystemProperties.getPropertyValueOrNull ("AS2.httpDumpDirectory");
    if (StringHelper.hasText (sHttpDumpDirectory))
    {
      final File aDumpDirectory = new File (sHttpDumpDirectory);
      AS2IOHelper.getFileOperationManager ().createDirIfNotExisting (aDumpDirectory);
      setHTTPIncomingDumperFactory ( () -> new HTTPIncomingDumperDirectoryBased (aDumpDirectory));
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
    final String sContentLength = aMsg.getHeader (CHttpHeader.CONTENT_LENGTH);
    if (sContentLength == null)
    {
      // No "Content-Length" header present
      final String sTransferEncoding = aMsg.getHeader (CHttpHeader.TRANSFER_ENCODING);
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
          aMsg.headers ().setContentLength (nLength);
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
   * @return the dumper for incoming HTTP requests or <code>null</code> if none
   *         is present
   * @since 3.0.1
   */
  @Nullable
  public static IHTTPIncomingDumper getHTTPIncomingDumper ()
  {
    return s_aHTTPIncomingDumperFactory.get ();
  }

  /**
   * Set the factory for creating dumper for incoming HTTP requests.
   *
   * @param aHttpDumperFactory
   *        The dumper factory to be used. May not be <code>null</code>.
   * @since 3.1.0
   */
  public static void setHTTPIncomingDumperFactory (@Nonnull final ISupplier <? extends IHTTPIncomingDumper> aHttpDumperFactory)
  {
    ValueEnforcer.notNull (aHttpDumperFactory, "HttpDumperFactory");
    s_aHTTPIncomingDumperFactory = aHttpDumperFactory;
  }

  /**
   * @param aMsg
   *        The message for which a dumper should be created.
   * @return the dumper for outgoing HTTP requests or <code>null</code> if none
   *         is present. Must be closed afterwards!
   * @since 3.0.1
   */
  @Nullable
  public static IHTTPOutgoingDumper getHTTPOutgoingDumper (@Nonnull final IBaseMessage aMsg)
  {
    return s_aHTTPOutgoingDumperFactory.apply (aMsg);
  }

  /**
   * Set the factory for creating dumper for outgoing HTTP requests
   *
   * @param aHttpDumperFactory
   *        The dumper factory to be used. May not be <code>null</code>.
   * @since 3.1.0
   */
  public static void setHTTPOutgoingDumperFactory (@Nullable final IFunction <? super IBaseMessage, ? extends IHTTPOutgoingDumper> aHttpDumperFactory)
  {
    ValueEnforcer.notNull (aHttpDumperFactory, "HttpDumperFactory");
    s_aHTTPOutgoingDumperFactory = aHttpDumperFactory;
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
    aMsg.attrs ().putIn (MA_HTTP_REQ_TYPE, aRequest[0]);
    // Request URL (e.g. "/as2")
    aMsg.attrs ().putIn (MA_HTTP_REQ_URL, aRequest[1]);
    // HTTP version (e.g. "HTTP/1.1")
    aMsg.attrs ().putIn (MA_HTTP_REQ_VERSION, aRequest[2]);

    // Parse all HTTP headers from stream
    final InternetHeaders aHeaders = new InternetHeaders (aIS);
    // Convert to header map
    final Enumeration <Header> aEnum = aHeaders.getAllHeaders ();
    while (aEnum.hasMoreElements ())
    {
      final Header aHeader = aEnum.nextElement ();
      aMsg.headers ().addHeader (aHeader.getName (), aHeader.getValue ());
    }

    // Read the message body - no Content-Transfer-Encoding handling
    final byte [] aPayload = readHttpPayload (aIS, aResponseHandler, aMsg);

    // Dump on demand
    final IHTTPIncomingDumper aIncomingDumper = getHTTPIncomingDumper ();
    if (aIncomingDumper != null)
      aIncomingDumper.dumpIncomingRequest (getAllHTTPHeaderLines (aHeaders), aPayload, aMsg);

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
      final String sHTTPLine = Integer.toString (nResponseCode) +
                               " " +
                               CHttp.getHttpResponseMessage (nResponseCode) +
                               CHttp.EOL;
      aData.write (sHTTPLine.getBytes (CHttp.HTTP_CHARSET));

      aResponseHandler.sendHttpResponse (nResponseCode, new HttpHeaderMap (), aData);
    }
  }

  /**
   * Copy headers from an HTTP connection to an InternetHeaders object
   * Will switch according to type
   *
   * @param aFromConn
   *        Connection - source. May not be <code>null</code>.
   * @param aHeaders
   *        Headers - destination. May not be <code>null</code>.
   */
  public static void copyHttpHeaders (@Nonnull final IAS2HttpConnection aFromConn, @Nonnull final HttpHeaderMap aHeaders)
  {
    if (aFromConn instanceof HttpURLConnection)
      copyHttpHeaders(aFromConn, aHeaders);
    else throw new IllegalArgumentException("class "+
      aFromConn.getClass().getCanonicalName()+" not supported");
  }
  /**
   * Copy headers from an HTTP connection to an InternetHeaders object
   *
   * @param aFromConn
   *        Connection - source. May not be <code>null</code>.
   * @param aHeaders
   *        Headers - destination. May not be <code>null</code>.
   */
  public static void copyHttpHeaders (@Nonnull final HttpURLConnection aFromConn, @Nonnull final HttpHeaderMap aHeaders)
  {
    for (final Map.Entry <String, List <String>> aConnHeader : aFromConn.getHeaderFields ().entrySet ())
    {
      final String sHeaderName = aConnHeader.getKey ();
      if (sHeaderName != null)
        for (final String sHeaderValue : aConnHeader.getValue ())
          aHeaders.addHeader (sHeaderName, sHeaderValue);
    }
  }
}
