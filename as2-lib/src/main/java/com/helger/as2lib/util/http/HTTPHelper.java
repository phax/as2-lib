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

import java.io.*;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.activation.DataSource;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetHeaders;
import javax.mail.util.SharedFileInputStream;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.IBaseMessage;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.params.MessageParameters;
import com.helger.as2lib.util.AS2IOHelper;
import com.helger.as2lib.util.dump.HTTPIncomingDumperDirectoryBased;
import com.helger.as2lib.util.dump.HTTPOutgoingDumperFileBased;
import com.helger.as2lib.util.dump.IHTTPIncomingDumper;
import com.helger.as2lib.util.dump.IHTTPOutgoingDumper;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.functional.IFunction;
import com.helger.commons.functional.ISupplier;
import com.helger.commons.http.CHttp;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.system.SystemProperties;
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

  private static final Logger LOGGER = LoggerFactory.getLogger (HTTPHelper.class);

  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("s_aRWLock")
  private static ISupplier <? extends IHTTPIncomingDumper> s_aHTTPIncomingDumperFactory = () -> null;
  @GuardedBy ("s_aRWLock")
  private static IFunction <? super IBaseMessage, ? extends IHTTPOutgoingDumper> s_aHTTPOutgoingDumperFactory = x -> null;

  private static final class OutgoingDumperFactory implements IFunction <IBaseMessage, IHTTPOutgoingDumper>
  {
    // Counter to ensure unique filenames
    private final AtomicInteger m_aCounter = new AtomicInteger (0);
    private final File m_aDumpDirectory;

    public OutgoingDumperFactory (@Nonnull final File aDumpDirectory)
    {
      m_aDumpDirectory = aDumpDirectory;
    }

    @Nonnull
    public IHTTPOutgoingDumper apply (@Nonnull final IBaseMessage aMsg)
    {
      return new HTTPOutgoingDumperFileBased (new File (m_aDumpDirectory,
                                                        "as2-outgoing-" +
                                                                          Long.toString (System.currentTimeMillis ()) +
                                                                          "-" +
                                                                          Integer.toString (m_aCounter.getAndIncrement ()) +
                                                                          ".http"));
    }
  }

  static
  {
    // Set global incoming dump directory
    {
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

    // Set global outgoing dump directory (since v4.0.3)
    {
      final String sHttpDumpOutgoingDirectory = SystemProperties.getPropertyValueOrNull ("AS2.httpDumpDirectoryOutgoing");
      if (StringHelper.hasText (sHttpDumpOutgoingDirectory))
      {
        final File aDumpDirectory = new File (sHttpDumpOutgoingDirectory);
        AS2IOHelper.getFileOperationManager ().createDirIfNotExisting (aDumpDirectory);
        setHTTPOutgoingDumperFactory (new OutgoingDumperFactory (aDumpDirectory));
      }
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
            int nBlocklen = readChunkLen(aDataIS);
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
            readTillNexLine(aDataIS);
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
    return s_aRWLock.readLocked ( () -> s_aHTTPIncomingDumperFactory.get ());
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
    s_aRWLock.writeLocked ( () -> s_aHTTPIncomingDumperFactory = aHttpDumperFactory);
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
    return s_aRWLock.readLocked ( () -> s_aHTTPOutgoingDumperFactory.apply (aMsg));
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
    s_aRWLock.writeLocked ( () -> s_aHTTPOutgoingDumperFactory = aHttpDumperFactory);
  }

  /**
   * Read headers and payload from the passed input stream provider. For large file support, return {@link DataSource}. If is on, data is not read.
   *
   * @param aISP
   *        The abstract input stream provider to use. May not be
   *        <code>null</code>.
   * @param aResponseHandler
   *        The HTTP response handler to be used. May not be <code>null</code>.
   * @param aMsg
   *        The Message to be filled. May not be <code>null</code>.
   * @return A {@link DataSource} that holds/refers to the body.
   * @throws IOException
   *         In case of error reading from the InputStream
   * @throws MessagingException
   *         In case header line parsing fails
   */
  @Nonnull
  public static DataSource readHttpRequest (@Nonnull final IAS2InputStreamProvider aISP,
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

    //Generate DataSource
    // Put received data in a MIME body part
    final ContentType aReceivedContentType = new ContentType (aMsg.getHeader (CHttpHeader.CONTENT_TYPE));
    final String sReceivedContentType = aReceivedContentType.toString ();
    byte [] aBytePayLoad = null;
    DataSource aPayload;
    if (aMsg.attrs().getAsBoolean(MessageParameters.ATTR_LARGE_FILE_SUPPORT_ON)) {
      InputStream is = aIS;
      final String sContentLength = aMsg.getHeader (CHttpHeader.CONTENT_LENGTH);
      if (sContentLength == null) {
        // No "Content-Length" header present
        final String sTransferEncoding = aMsg.getHeader (CHttpHeader.TRANSFER_ENCODING);
        if (sTransferEncoding != null)
        {
          // Remove all whitespaces in the value
          if (sTransferEncoding.replaceAll ("\\s+", "").equalsIgnoreCase ("chunked"))
          {
            // chunked encoding. Use also file backed stream as the message might be large
            TempSharedFileInputStream sis = TempSharedFileInputStream.getTempSharedFileInputStream(
              new ChunkedInputStream(aIS), aMsg.getMessageID());
            is = sis;
            aMsg.setTempSharedFileInputStream(sis);
          } else {
            // No "Content-Length" and unsupported "Transfer-Encoding"
            sendSimpleHTTPResponse(aResponseHandler, HttpURLConnection.HTTP_LENGTH_REQUIRED);
            throw new IOException("Transfer-Encoding unimplemented: " + sTransferEncoding);
          }
        } else {
          // No "Content-Length" and no "Transfer-Encoding"
          sendSimpleHTTPResponse (aResponseHandler, HttpURLConnection.HTTP_LENGTH_REQUIRED);
          throw new IOException ("Content-Length missing");
        }
      }
      //Content-length present, or chunked encoding
      aPayload = new InputStreamDataSource(is,
        aMsg.getAS2From()==null ? "" : aMsg.getAS2From(),
        sReceivedContentType,
        true);
    } else { //Large message support off
      // Read the message body - no Content-Transfer-Encoding handling
      aBytePayLoad = readHttpPayload(aIS, aResponseHandler, aMsg);
      aPayload = new ByteArrayDataSource(
        aBytePayLoad,
        sReceivedContentType,
        null);
    }

    // Dump on demand
    final IHTTPIncomingDumper aIncomingDumper = getHTTPIncomingDumper ();
    if (aIncomingDumper != null)
      aIncomingDumper.dumpIncomingRequest (getAllHTTPHeaderLines (aHeaders),
        aBytePayLoad!=null
          ?aBytePayLoad
          :"Large File Support: body was not read yet".getBytes(),
        aMsg);

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
   *
   * @param aFromConn
   *        Connection - source. May not be <code>null</code>.
   * @param aHeaders
   *        Headers - destination. May not be <code>null</code>.
   */
  public static void copyHttpHeaders (@Nonnull final IAS2HttpConnection aFromConn, @Nonnull final HttpHeaderMap aHeaders) throws OpenAS2Exception
  {
    for (final Map.Entry <String, List <String>> aConnHeader : aFromConn.getHeaderFields ().entrySet ())
    {
      final String sHeaderName = aConnHeader.getKey ();
      if (sHeaderName != null)
        for (final String sHeaderValue : aConnHeader.getValue ())
          aHeaders.addHeader (sHeaderName, sHeaderValue);
    }
  }

  /**
   * Read chunk size (including the newline ending it). Discard any other data, e.g. headers that my be there.
   *
   * @param aIS - input stream to read from
   * @return Chunk length
   * @throws IOException
   *         if stream ends during chunk length read
   */
  public static int readChunkLen(InputStream aIS) throws IOException{
    int nRes=0;
    boolean headersStarted=false;
    for (;;)
    {
      int ch = aIS.read ();
      if (ch < 0)
        throw new EOFException();
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
          headersStarted=true;
      else
        continue;
      if (! headersStarted)
        nRes = (nRes * 16) + ch;
    }
    return nRes;
  }

  /**
   * Read up to (and including )CRLF.
   *
   * @param aIS - input stream to read from
   * @throws IOException
   *         if stream ends during chunk length read
   */
  public static void readTillNexLine(InputStream aIS) throws IOException {
    while (true) {
      final int n = aIS.read();
      if (n == '\n')
        break;
    }
  }
}
