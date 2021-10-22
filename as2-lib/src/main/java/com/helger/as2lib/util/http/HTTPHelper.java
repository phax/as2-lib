/*
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2021 Philip Helger philip[at]helger[dot]com
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
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.function.Supplier;

import javax.activation.DataSource;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.util.AS2Helper;
import com.helger.as2lib.util.AS2HttpHelper;
import com.helger.as2lib.util.AS2IOHelper;
import com.helger.as2lib.util.dump.HTTPIncomingDumperDirectoryBased;
import com.helger.as2lib.util.dump.IHTTPIncomingDumper;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.codec.IByteArrayCodec;
import com.helger.commons.codec.IdentityCodec;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.http.CHttp;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.StringParser;
import com.helger.commons.system.SystemProperties;
import com.helger.mail.cte.EContentTransferEncoding;
import com.helger.mail.cte.IContentTransferEncoding;
import com.helger.mail.datasource.ByteArrayDataSource;
import com.helger.mail.datasource.InputStreamDataSource;

/**
 * HTTP utility methods.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class HTTPHelper
{
  /** The request method used (POST or GET) */
  public static final String MA_HTTP_REQ_TYPE = "HTTP_REQUEST_TYPE";
  /** The request URL used - defaults to "/" */
  public static final String MA_HTTP_REQ_URL = "HTTP_REQUEST_URL";
  /** The HTTP version used. E.g. "HTTP/1.1" */
  public static final String MA_HTTP_REQ_VERSION = "HTTP_REQUEST_VERSION";

  /** The value of the Content-Transfer-Encoding header (if provided) */
  public static final String MA_HTTP_ORIGINAL_CONTENT_TRANSFER_ENCODING = "HTTP_ORIGINAL_CONTENT_TRANSFER_ENCODING";
  /**
   * The original content length before any eventual decoding (only if
   * Content-Transfer-Encoding is provided)
   */
  public static final String MA_HTTP_ORIGINAL_CONTENT_LENGTH = "HTTP_ORIGINAL_CONTENT_LENGTH";

  private static final Logger LOGGER = LoggerFactory.getLogger (HTTPHelper.class);

  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  @GuardedBy ("RW_LOCK")
  private static Supplier <? extends IHTTPIncomingDumper> s_aHTTPIncomingDumperFactory = () -> null;

  static
  {
    // Set global incoming dump directory
    // New property name since v4.0.3
    String sHttpDumpIncomingDirectory = SystemProperties.getPropertyValueOrNull ("AS2.httpDumpDirectoryIncoming");
    if (StringHelper.hasNoText (sHttpDumpIncomingDirectory))
    {
      // Check old name
      sHttpDumpIncomingDirectory = SystemProperties.getPropertyValueOrNull ("AS2.httpDumpDirectory");
      if (StringHelper.hasText (sHttpDumpIncomingDirectory))
        LOGGER.warn ("You are using a legacy system property name `AS2.httpDumpDirectory`. Please use `AS2.httpDumpDirectoryIncoming` instead.");
    }
    if (StringHelper.hasText (sHttpDumpIncomingDirectory))
    {
      final File aDumpDirectory = new File (sHttpDumpIncomingDirectory);
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
   * @return the global dumper for incoming HTTP requests or <code>null</code>
   *         if none is present
   * @since 3.0.1
   */
  @Nullable
  public static IHTTPIncomingDumper getHTTPIncomingDumper ()
  {
    return RW_LOCK.readLockedGet ( () -> s_aHTTPIncomingDumperFactory.get ());
  }

  /**
   * @return the global dumper factory for incoming HTTP requests. Never
   *         <code>null</code>.
   * @since 4.4.0
   */
  @Nonnull
  public static Supplier <? extends IHTTPIncomingDumper> getHTTPIncomingDumperFactory ()
  {
    return RW_LOCK.readLockedGet ( () -> s_aHTTPIncomingDumperFactory);
  }

  /**
   * Set the global factory for creating dumper for incoming HTTP requests.
   *
   * @param aHttpDumperFactory
   *        The dumper factory to be used. May not be <code>null</code>.
   * @since 3.1.0
   */
  public static void setHTTPIncomingDumperFactory (@Nonnull final Supplier <? extends IHTTPIncomingDumper> aHttpDumperFactory)
  {
    ValueEnforcer.notNull (aHttpDumperFactory, "HttpDumperFactory");
    RW_LOCK.writeLocked ( () -> s_aHTTPIncomingDumperFactory = aHttpDumperFactory);
  }

  /**
   * Read headers and payload from the passed input stream provider. For large
   * file support, return {@link DataSource}. If is on, data is not read.
   *
   * @param aISP
   *        The abstract input stream provider to use. May not be
   *        <code>null</code>.
   * @param aResponseHandler
   *        The HTTP response handler to be used. May not be <code>null</code>.
   * @param aMsg
   *        The Message to be filled. May not be <code>null</code>.
   * @param aIncomingDumper
   *        Optional incoming HTTP dumper. May be <code>null</code>.
   * @return A {@link DataSource} that holds/refers to the body.
   * @throws IOException
   *         In case of error reading from the InputStream
   * @throws MessagingException
   *         In case header line parsing fails
   */
  @Nonnull
  public static DataSource readHttpRequest (@Nonnull final IAS2InputStreamProvider aISP,
                                            @Nonnull final IAS2HttpResponseHandler aResponseHandler,
                                            @Nonnull final IMessage aMsg,
                                            @Nullable final IHTTPIncomingDumper aIncomingDumper) throws IOException, MessagingException
  {
    // Get the stream to read from
    final InputStream aIS = aISP.getInputStream ();

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

    // Generate DataSource
    // Put received data in a MIME body part
    final String sReceivedContentType = AS2HttpHelper.getCleanContentType (aMsg.getHeader (CHttpHeader.CONTENT_TYPE));

    byte [] aBytePayLoad = null;
    final DataSource aPayload;
    final String sContentLength = aMsg.getHeader (CHttpHeader.CONTENT_LENGTH);
    if (sContentLength == null)
    {
      // No "Content-Length" header present
      final InputStream aRealIS;
      final String sTransferEncoding = aMsg.getHeader (CHttpHeader.TRANSFER_ENCODING);
      if (sTransferEncoding != null)
      {
        // Remove all whitespaces in the value
        if (AS2Helper.getWithoutSpaces (sTransferEncoding).equalsIgnoreCase ("chunked"))
        {
          // chunked encoding. Use also file backed stream as the message
          // might be large
          @WillNotClose
          final TempSharedFileInputStream aSharedIS = TempSharedFileInputStream.getTempSharedFileInputStream (new ChunkedInputStream (aIS),
                                                                                                              aMsg.getMessageID ());
          aRealIS = aSharedIS;
          aMsg.setTempSharedFileInputStream (aSharedIS);
        }
        else
        {
          // No "Content-Length" and unsupported "Transfer-Encoding"
          sendSimpleHTTPResponse (aResponseHandler, CHttp.HTTP_LENGTH_REQUIRED);
          throw new IOException ("Transfer-Encoding unimplemented: " + sTransferEncoding);
        }
      }
      else
      {
        // No "Content-Length" and no "Transfer-Encoding"
        sendSimpleHTTPResponse (aResponseHandler, CHttp.HTTP_LENGTH_REQUIRED);
        throw new IOException ("Content-Length missing");
      }

      // Content-length present, or chunked encoding
      aPayload = new InputStreamDataSource (aRealIS, aMsg.getAS2From () == null ? "" : aMsg.getAS2From (), sReceivedContentType, true);
    }
    else
    {
      // content-length exists
      // Read the message body - no Content-Transfer-Encoding handling
      // Retrieve the message content
      // FIXME if a value > 2GB comes in, this will fail!!
      final long nContentLength = StringParser.parseLong (sContentLength, -1);
      if (nContentLength < 0 || nContentLength > Integer.MAX_VALUE)
      {
        // Invalid content length (no int or too big)
        sendSimpleHTTPResponse (aResponseHandler, CHttp.HTTP_LENGTH_REQUIRED);
        throw new IOException ("Content-Length '" +
                               sContentLength +
                               "' is invalid. Only values between 0 and " +
                               Integer.MAX_VALUE +
                               " are allowed.");
      }
      aBytePayLoad = new byte [(int) nContentLength];

      try (final DataInputStream aDataIS = new DataInputStream (aIS))
      {
        aDataIS.readFully (aBytePayLoad);
      }
      aPayload = new ByteArrayDataSource (aBytePayLoad, sReceivedContentType, null);
    }

    // Dump on demand
    if (aIncomingDumper != null)
    {
      aIncomingDumper.dumpIncomingRequest (getAllHTTPHeaderLines (aHeaders),
                                           aBytePayLoad != null ? aBytePayLoad
                                                                : "Payload body was not read yet, and therefore it cannot be dumped (yet) - sorry".getBytes (StandardCharsets.ISO_8859_1),
                                           aMsg);
    }

    return aPayload;

    // Don't close the IS here!
  }

  @Nonnull
  public static DataSource readAndDecodeHttpRequest (@Nonnull final IAS2InputStreamProvider aISP,
                                                     @Nonnull final IAS2HttpResponseHandler aResponseHandler,
                                                     @Nonnull final IMessage aMsg,
                                                     @Nullable final IHTTPIncomingDumper aIncomingDumper) throws IOException,
                                                                                                          MessagingException
  {
    // Main read
    DataSource aPayload = HTTPHelper.readHttpRequest (aISP, aResponseHandler, aMsg, aIncomingDumper);

    // Check the transfer encoding of the request. If none is provided, check
    // the partnership for a default one. If none is in the partnership used the
    // default one
    final String sCTE = aMsg.partnership ().getContentTransferEncodingReceive (EContentTransferEncoding.AS2_DEFAULT.getID ());
    final String sContentTransferEncoding = aMsg.getHeaderOrDefault (CHttpHeader.CONTENT_TRANSFER_ENCODING, sCTE);
    if (StringHelper.hasText (sContentTransferEncoding))
    {
      final IContentTransferEncoding aCTE = EContentTransferEncoding.getFromIDCaseInsensitiveOrNull (sContentTransferEncoding);
      if (aCTE == null)
      {
        if (LOGGER.isWarnEnabled ())
          LOGGER.warn ("Unsupported Content-Transfer-Encoding '" + sContentTransferEncoding + "' is used - ignoring!");
      }
      else
      {
        // Decode data if necessary
        final IByteArrayCodec aCodec = aCTE.createCodec ();

        // TODO: Handle decoding when large file support is on
        if (!(aCodec instanceof IdentityCodec <?>) && aPayload instanceof ByteArrayDataSource)
        {
          byte [] aActualBytes = ((ByteArrayDataSource) aPayload).directGetBytes ();
          // Remember original length before continuing
          final int nOriginalContentLength = aActualBytes.length;

          if (LOGGER.isInfoEnabled ())
            LOGGER.info ("Incoming message uses Content-Transfer-Encoding '" + sContentTransferEncoding + "' - decoding");
          aActualBytes = aCodec.getDecoded (aActualBytes);
          aPayload = new ByteArrayDataSource (aActualBytes, aPayload.getContentType (), aPayload.getName ());

          // Remember that we potentially did something
          aMsg.attrs ().putIn (MA_HTTP_ORIGINAL_CONTENT_TRANSFER_ENCODING, sContentTransferEncoding);
          aMsg.attrs ().putIn (MA_HTTP_ORIGINAL_CONTENT_LENGTH, nOriginalContentLength);
        }
      }
    }

    return aPayload;
  }

  /**
   * Send a simple HTTP response that only contains the HTTP status code and the
   * respective descriptive text. An empty header map us used.
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
      final String sHTTPLine = Integer.toString (nResponseCode) + " " + CHttp.getHttpResponseMessage (nResponseCode) + CHttp.EOL;
      aData.write (sHTTPLine.getBytes (CHttp.HTTP_CHARSET));

      aResponseHandler.sendHttpResponse (nResponseCode, new HttpHeaderMap (), aData);
    }
  }

  /**
   * Read chunk size (including the newline ending it). Discard any other data,
   * e.g. headers that my be there.
   *
   * @param aIS
   *        - input stream to read from
   * @return Chunk length
   * @throws IOException
   *         if stream ends during chunk length read
   */
  public static int readChunkLen (@Nonnull @WillNotClose final InputStream aIS) throws IOException
  {
    int nRes = 0;
    boolean bHeadersStarted = false;
    for (;;)
    {
      int ch = aIS.read ();
      if (ch < 0)
        throw new EOFException ();
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
            if (ch == ';')
              bHeadersStarted = true;
            else
              continue;
      if (!bHeadersStarted)
        nRes = (nRes * 16) + ch;
    }
    return nRes;
  }

  /**
   * Read up to (and including )CRLF.
   *
   * @param aIS
   *        - input stream to read from
   * @throws IOException
   *         if stream ends during chunk length read
   */
  public static void readTillNexLine (@Nonnull @WillNotClose final InputStream aIS) throws IOException
  {
    while (true)
    {
      final int ch = aIS.read ();
      if (ch < 0)
        throw new EOFException ();
      if (ch == '\n')
        break;
    }
  }
}
