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
package com.helger.as2lib.processor.receiver.net;

import java.io.IOException;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.function.Consumer;

import javax.activation.DataSource;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.util.dump.IHTTPIncomingDumper;
import com.helger.as2lib.util.http.HTTPHelper;
import com.helger.as2lib.util.http.IAS2HttpResponseHandler;
import com.helger.as2lib.util.http.IAS2InputStreamProvider;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.codec.IByteArrayCodec;
import com.helger.commons.codec.IdentityCodec;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.string.StringHelper;
import com.helger.mail.cte.EContentTransferEncoding;
import com.helger.mail.cte.IContentTransferEncoding;
import com.helger.mail.datasource.ByteArrayDataSource;

/**
 * Abstract base class for Message and MDN receive handlers.
 *
 * @author Philip Helger
 */
public abstract class AbstractReceiverHandler implements INetModuleHandler
{
  /** The value of the Content-Transfer-Encoding header (if provided) */
  public static final String MA_HTTP_ORIGINAL_CONTENT_TRANSFER_ENCODING = "HTTP_ORIGINAL_CONTENT_TRANSFER_ENCODING";
  /**
   * The original content length before any eventual decoding (only if
   * Content-Transfer-Encoding is provided)
   */
  public static final String MA_HTTP_ORIGINAL_CONTENT_LENGTH = "HTTP_ORIGINAL_CONTENT_LENGTH";

  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractReceiverHandler.class);

  private Consumer <X509Certificate> m_aVerificationCertificateConsumer;
  private IHTTPIncomingDumper m_aHttpIncomingDumper;

  /**
   * @return The consumer for the effective certificate upon signature
   *         verification. May be <code>null</code>. The default is
   *         <code>null</code>.
   * @since 4.4.1
   */
  @Nullable
  public final Consumer <X509Certificate> getVerificationCertificateConsumer ()
  {
    return m_aVerificationCertificateConsumer;
  }

  /**
   * Set the consumer for the effective certificate upon signature verification.
   *
   * @param aVerificationCertificateConsumer
   *        The consumer to be used. May be <code>null</code>.
   * @return this for chaining
   * @since 4.4.1
   */
  @Nonnull
  public final AbstractReceiverHandler setVerificationCertificateConsumer (@Nullable final Consumer <X509Certificate> aVerificationCertificateConsumer)
  {
    m_aVerificationCertificateConsumer = aVerificationCertificateConsumer;
    return this;
  }

  /**
   * @return The specific incoming dumper of this receiver. May be
   *         <code>null</code>.
   * @since v4.4.5
   */
  @Nullable
  public final IHTTPIncomingDumper getHttpIncomingDumper ()
  {
    return m_aHttpIncomingDumper;
  }

  /**
   * Get the customized incoming dumper, falling back to the global incoming
   * dumper if no specific dumper is set.
   *
   * @return The effective incoming dumper. May be <code>null</code>.
   * @since v4.4.5
   */
  @Nullable
  public final IHTTPIncomingDumper getEffectiveHttpIncomingDumper ()
  {
    // Dump on demand
    IHTTPIncomingDumper ret = m_aHttpIncomingDumper;
    if (ret == null)
    {
      // Fallback to global dumper
      ret = HTTPHelper.getHTTPIncomingDumper ();
    }
    return ret;
  }

  /**
   * Set the specific incoming dumper of this receiver. If this is set, it
   * overrides the global dumper.
   *
   * @param aHttpIncomingDumper
   *        The specific incoming dumper to be used. May be <code>null</code>.
   * @since v4.4.5
   */
  public final void setHttpIncomingDumper (@Nullable final IHTTPIncomingDumper aHttpIncomingDumper)
  {
    m_aHttpIncomingDumper = aHttpIncomingDumper;
  }

  @Nonnull
  @Nonempty
  public String getClientInfo (@Nonnull final Socket aSocket)
  {
    return aSocket.getInetAddress ().getHostAddress () + ":" + aSocket.getPort ();
  }

  // Returns DataSource for large file support
  @Nonnull
  protected DataSource readAndDecodeHttpRequest (@Nonnull final IAS2InputStreamProvider aISP,
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
    final String sCTE = aMsg.partnership ()
                            .getContentTransferEncodingReceive (EContentTransferEncoding.AS2_DEFAULT.getID ());
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
            LOGGER.info ("Incoming message uses Content-Transfer-Encoding '" +
                         sContentTransferEncoding +
                         "' - decoding");
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
}
