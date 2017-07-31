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
package com.helger.as2lib.client;

import java.io.File;
import java.io.Serializable;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.CAS2Info;
import com.helger.as2lib.cert.IStorableCertificateFactory;
import com.helger.as2lib.crypto.ECompressionType;
import com.helger.as2lib.crypto.ECryptoAlgorithmCrypt;
import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.as2lib.disposition.DispositionOptions;
import com.helger.as2lib.processor.resender.IProcessorResenderModule;
import com.helger.as2lib.processor.sender.AbstractHttpSenderModule;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.string.StringHelper;

/**
 * Settings object for a message delivery.
 *
 * @author oleo Date: May 12, 2010 Time: 5:16:57 PM
 * @author Philip Helger
 */
public class AS2ClientSettings implements Serializable
{
  /**
   * The default MDN options to be used.
   *
   * @see #setMDNOptions(DispositionOptions)
   */
  public static final String DEFAULT_MDN_OPTIONS = new DispositionOptions ().setProtocolImportance (DispositionOptions.IMPORTANCE_OPTIONAL)
                                                                            .setProtocol (DispositionOptions.PROTOCOL_PKCS7_SIGNATURE)
                                                                            .setMICAlgImportance (DispositionOptions.IMPORTANCE_OPTIONAL)
                                                                            .setMICAlg (ECryptoAlgorithmSign.DIGEST_SHA_1)
                                                                            .getAsString ();

  /**
   * The default message ID format to use.
   *
   * @see #setMessageIDFormat(String)
   */
  public static final String DEFAULT_MESSAGE_ID_FORMAT = CAS2Info.NAME +
                                                         "-$date.ddMMyyyyHHmmssZ$-$rand.1234$@$msg.sender.as2_id$_$msg.receiver.as2_id$";
  /** By default no retry happens. */
  public static final int DEFAULT_RETRY_COUNT = IProcessorResenderModule.DEFAULT_RETRIES;
  /** Default connection timeout: 60 seconds */
  public static final int DEFAULT_CONNECT_TIMEOUT_MS = AbstractHttpSenderModule.DEFAULT_CONNECT_TIMEOUT_MS;
  /** Default read timeout: 60 seconds */
  public static final int DEFAULT_READ_TIMEOUT_MS = AbstractHttpSenderModule.DEFAULT_READ_TIMEOUT_MS;

  private File m_aKeyStoreFile;
  private String m_sKeyStorePassword;
  private boolean m_bSaveKeyStoreChangesToFile = IStorableCertificateFactory.DEFAULT_SAVE_CHANGES_TO_FILE;

  private String m_sSenderEmailAddress;
  private String m_sSenderAS2ID;
  private String m_sSenderKeyAlias;

  private String m_sReceiverAS2ID;
  private String m_sReceiverKeyAlias;
  private String m_sDestinationAS2URL;
  private X509Certificate m_aReceiverCert;

  private String m_sPartnershipName;
  private ECryptoAlgorithmCrypt m_eCryptAlgo;
  private ECryptoAlgorithmSign m_eSignAlgo;
  private ECompressionType m_eCompressionType;
  private boolean m_bCompressBeforeSigning = true;
  private String m_sMDNOptions = DEFAULT_MDN_OPTIONS;
  private String m_sAsyncMDNUrl;
  private String m_sMessageIDFormat = DEFAULT_MESSAGE_ID_FORMAT;

  private int m_nRetryCount = DEFAULT_RETRY_COUNT;
  private int m_nConnectTimeoutMS = DEFAULT_CONNECT_TIMEOUT_MS;
  private int m_nReadTimeoutMS = DEFAULT_READ_TIMEOUT_MS;

  private final HttpHeaderMap m_aCustomHeaders = new HttpHeaderMap ();

  public AS2ClientSettings ()
  {}

  /**
   * Set the details of the certificate store of the client. The keystore must
   * be in PKCS12 format.
   *
   * @param aFile
   *        The keystore file. May not be <code>null</code>.
   * @param sPassword
   *        The password used to open the key store. May not be
   *        <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public AS2ClientSettings setKeyStore (@Nonnull final File aFile, @Nonnull final String sPassword)
  {
    m_aKeyStoreFile = ValueEnforcer.notNull (aFile, "File");
    m_sKeyStorePassword = ValueEnforcer.notNull (sPassword, "Password");
    return this;
  }

  /**
   * @return The key store file. May be <code>null</code> if not yet set.
   * @see #setKeyStore(File, String)
   */
  @Nullable
  public File getKeyStoreFile ()
  {
    return m_aKeyStoreFile;
  }

  /**
   * @return The key store password. May be <code>null</code> if not yet set.
   * @see #setKeyStore(File, String)
   */
  @Nullable
  public String getKeyStorePassword ()
  {
    return m_sKeyStorePassword;
  }

  /**
   * Change the behavior if all changes to the keystore should trigger a saving
   * to the original file.
   *
   * @param bSaveKeyStoreChangesToFile
   *        <code>true</code> if key store changes should be written back to the
   *        file, <code>false</code> if not.
   * @return this for chaining
   */
  @Nonnull
  public AS2ClientSettings setSaveKeyStoreChangesToFile (final boolean bSaveKeyStoreChangesToFile)
  {
    m_bSaveKeyStoreChangesToFile = bSaveKeyStoreChangesToFile;
    return this;
  }

  /**
   * @return <code>true</code> if key store changes should be written back to
   *         the file, <code>false</code> if not.
   */
  public boolean isSaveKeyStoreChangesToFile ()
  {
    return m_bSaveKeyStoreChangesToFile;
  }

  /**
   * Set the sender data.
   *
   * @param sAS2ID
   *        Sender AS2 ID. May not be <code>null</code>.
   * @param sEmailAddress
   *        Sender email address. May not be <code>null</code>.
   * @param sKeyAlias
   *        Alias into the keystore for identifying the sender's key. May not be
   *        <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public AS2ClientSettings setSenderData (@Nonnull final String sAS2ID,
                                          @Nonnull final String sEmailAddress,
                                          @Nonnull final String sKeyAlias)
  {
    m_sSenderAS2ID = ValueEnforcer.notNull (sAS2ID, "AS2ID");
    m_sSenderEmailAddress = ValueEnforcer.notNull (sEmailAddress, "EmailAddress");
    m_sSenderKeyAlias = ValueEnforcer.notNull (sKeyAlias, "KeyAlias");
    return this;
  }

  /**
   * @return The sender AS2 ID. May be <code>null</code> if not set.
   * @see #setSenderData(String, String, String)
   */
  @Nullable
  public String getSenderAS2ID ()
  {
    return m_sSenderAS2ID;
  }

  /**
   * @return The sender's email address. May be <code>null</code> if not set.
   * @see #setSenderData(String, String, String)
   */
  @Nullable
  public String getSenderEmailAddress ()
  {
    return m_sSenderEmailAddress;
  }

  /**
   * @return The senders key alias in the keystore. May be <code>null</code> if
   *         not set.
   * @see #setSenderData(String, String, String)
   * @see #setKeyStore(File, String)
   */
  @Nullable
  public String getSenderKeyAlias ()
  {
    return m_sSenderKeyAlias;
  }

  /**
   * Set the receiver data.
   *
   * @param sAS2ID
   *        Receiver AS2 ID. May not be <code>null</code>.
   * @param sKeyAlias
   *        Alias into the keystore for identifying the receivers certificate.
   *        May not be <code>null</code>.
   * @param sAS2URL
   *        Destination URL to send the request to. May not be <code>null</code>
   *        .
   * @return this for chaining
   */
  @Nonnull
  public AS2ClientSettings setReceiverData (@Nonnull final String sAS2ID,
                                            @Nonnull final String sKeyAlias,
                                            @Nonnull final String sAS2URL)
  {
    m_sReceiverAS2ID = ValueEnforcer.notNull (sAS2ID, "AS2ID");
    m_sReceiverKeyAlias = ValueEnforcer.notNull (sKeyAlias, "KeyAlias");
    m_sDestinationAS2URL = ValueEnforcer.notNull (sAS2URL, "AS2URL");
    return this;
  }

  /**
   * @return The receiver AS2 ID. May be <code>null</code> if not set.
   * @see #setReceiverData(String, String, String)
   */
  @Nullable
  public String getReceiverAS2ID ()
  {
    return m_sReceiverAS2ID;
  }

  /**
   * @return The receivers key alias in the keystore. May be <code>null</code>
   *         if not set.
   * @see #setReceiverData(String, String, String)
   * @see #setKeyStore(File, String)
   */
  @Nullable
  public String getReceiverKeyAlias ()
  {
    return m_sReceiverKeyAlias;
  }

  /**
   * @return The destination URL to send the request to. May be
   *         <code>null</code> if not set.
   * @see #setReceiverData(String, String, String)
   */
  @Nullable
  public String getDestinationAS2URL ()
  {
    return m_sDestinationAS2URL;
  }

  /**
   * Explicitly set the receiver certificate to be used. This might be used to
   * dynamically add it to the certificate factory for dynamic partnership
   * handling (like in PEPPOL).
   *
   * @param aReceiverCertificate
   *        The receiver certificate. May be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public AS2ClientSettings setReceiverCertificate (@Nullable final X509Certificate aReceiverCertificate)
  {
    m_aReceiverCert = aReceiverCertificate;
    return this;
  }

  /**
   * @return The explicit certificate of the recipient. This might be used to
   *         dynamically add it to the certificate factory for dynamic
   *         partnership handling (like in PEPPOL). May be <code>null</code>.
   * @see #setReceiverCertificate(X509Certificate)
   */
  @Nullable
  public X509Certificate getReceiverCertificate ()
  {
    return m_aReceiverCert;
  }

  /**
   * Set the encryption and signing algorithms to use.
   *
   * @param eCryptAlgo
   *        The encryption algorithm. May be <code>null</code> to indicate that
   *        the message should not be encrypted.
   * @param eSignAlgo
   *        The signing algorithm. May be <code>null</code> to indicate that the
   *        message should not be signed.
   * @return this for chaining.
   */
  @Nonnull
  public AS2ClientSettings setEncryptAndSign (@Nullable final ECryptoAlgorithmCrypt eCryptAlgo,
                                              @Nullable final ECryptoAlgorithmSign eSignAlgo)
  {
    m_eCryptAlgo = eCryptAlgo;
    m_eSignAlgo = eSignAlgo;
    return this;
  }

  /**
   * @return The algorithm used to encrypt the message. May be <code>null</code>
   *         if not set.
   * @see #setEncryptAndSign(ECryptoAlgorithmCrypt, ECryptoAlgorithmSign)
   */
  @Nullable
  public ECryptoAlgorithmCrypt getCryptAlgo ()
  {
    return m_eCryptAlgo;
  }

  /**
   * @return The ID of the algorithm used to encrypt the message. May be
   *         <code>null</code> if not set.
   * @see #setEncryptAndSign(ECryptoAlgorithmCrypt, ECryptoAlgorithmSign)
   */
  @Nullable
  public String getCryptAlgoID ()
  {
    return m_eCryptAlgo == null ? null : m_eCryptAlgo.getID ();
  }

  /**
   * @return The algorithm used to sign the message. May be <code>null</code> if
   *         not set.
   * @see #setEncryptAndSign(ECryptoAlgorithmCrypt, ECryptoAlgorithmSign)
   */
  @Nullable
  public ECryptoAlgorithmSign getSignAlgo ()
  {
    return m_eSignAlgo;
  }

  /**
   * @return The ID of the algorithm used to sign the message. May be
   *         <code>null</code> if not set.
   * @see #setEncryptAndSign(ECryptoAlgorithmCrypt, ECryptoAlgorithmSign)
   */
  @Nullable
  public String getSignAlgoID ()
  {
    return m_eSignAlgo == null ? null : m_eSignAlgo.getID ();
  }

  /**
   * Enable or disable the compression of the message. Note: compression
   * requires the receiver to support AS2 version 1.1!
   *
   * @param eCompressionType
   *        The compression type to use. Pass <code>null</code> to not compress
   *        the message (that is also the default).
   * @param bCompressBeforeSigning
   *        <code>true</code> to compress the data before it is signed,
   *        <code>false</code> to sign first and than compress the message.
   * @return this for chaining
   */
  @Nonnull
  public AS2ClientSettings setCompress (@Nullable final ECompressionType eCompressionType,
                                        final boolean bCompressBeforeSigning)
  {
    m_eCompressionType = eCompressionType;
    m_bCompressBeforeSigning = bCompressBeforeSigning;
    return this;
  }

  /**
   * @return The compression type used to compress the message. May be
   *         <code>null</code> to indicate no compression.
   * @see #setCompress(ECompressionType, boolean)
   */
  @Nullable
  public ECompressionType getCompressionType ()
  {
    return m_eCompressionType;
  }

  /**
   * Check if compress before sign or sign before compress is used. This flag is
   * only evaluated if {@link #getCompressionType()} is not <code>null</code>.
   *
   * @return <code>true</code> to compress before signing, <code>false</code> to
   *         sign before compressing
   * @see #setCompress(ECompressionType, boolean)
   */
  public boolean isCompressBeforeSigning ()
  {
    return m_bCompressBeforeSigning;
  }

  /**
   * Set the name of the partnership for lookup and dynamic creation.
   *
   * @param sPartnershipName
   *        The partnership name. May not be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public AS2ClientSettings setPartnershipName (@Nonnull final String sPartnershipName)
  {
    m_sPartnershipName = ValueEnforcer.notNull (sPartnershipName, "PartnershipName");
    return this;
  }

  /**
   * @return The partnership name to be used. May be <code>null</code> if not
   *         set.
   * @see #setPartnershipName(String)
   */
  @Nullable
  public String getPartnershipName ()
  {
    return m_sPartnershipName;
  }

  /**
   * Set the MDN options to be used. Since 3.0.4 the MDN options (corresponding
   * to the 'Disposition-Notification-Options' header) may be <code>null</code>.
   *
   * @param sMDNOptions
   *        The <code>Disposition-Notification-Options</code> String to be used.
   *        May be <code>null</code>.
   * @return this for chaining
   * @see #setMDNOptions(DispositionOptions)
   */
  @Nonnull
  public AS2ClientSettings setMDNOptions (@Nullable final String sMDNOptions)
  {
    m_sMDNOptions = sMDNOptions;
    return this;
  }

  /**
   * Set the MDN options to be used.
   *
   * @param aDispositionOptions
   *        The <code>Disposition-Notification-Options</code> structured object
   *        to be used. May not be <code>null</code>.
   * @return this for chaining
   * @see #setMDNOptions(String)
   */
  @Nonnull
  public AS2ClientSettings setMDNOptions (@Nonnull final DispositionOptions aDispositionOptions)
  {
    ValueEnforcer.notNull (aDispositionOptions, "DispositionOptions");
    return setMDNOptions (aDispositionOptions.getAsString ());
  }

  /**
   * Get the current MDN options. Since 3.0.4 the MDN options (corresponding to
   * the 'Disposition-Notification-Options' header) may be <code>null</code>.
   *
   * @return The MDN options (<code>Disposition-Notification-Options</code>
   *         header) to be used. May be <code>null</code>. The default is
   *         defined in {@link #DEFAULT_MDN_OPTIONS}.
   * @see #setMDNOptions(DispositionOptions)
   * @see #setMDNOptions(String)
   */
  @Nullable
  public String getMDNOptions ()
  {
    return m_sMDNOptions;
  }

  /**
   * @return <code>true</code> if MDN options are specified (the default),
   *         <code>false</code> if not.
   * @since 3.0.4
   */
  public boolean hasMDNOptions ()
  {
    return StringHelper.hasText (m_sMDNOptions);
  }

  /**
   * Set the asynchronous MDN URL to be used.
   *
   * @param sAsyncMDNUrl
   *        May be <code>null</code> in which case a synchronous MDN is
   *        requested (which is also the default).
   * @return this for chaining
   * @since 3.0.4
   */
  @Nonnull
  public AS2ClientSettings setAsyncMDNUrl (@Nullable final String sAsyncMDNUrl)
  {
    m_sAsyncMDNUrl = sAsyncMDNUrl;
    return this;
  }

  /**
   * @return The URL for the asynchronous MDN. If this is <code>null</code> than
   *         a synchronous MDN is requested. By default a synchronous MDN is
   *         requested.
   * @since 3.0.4
   */
  @Nullable
  public String getAsyncMDNUrl ()
  {
    return m_sAsyncMDNUrl;
  }

  /**
   * @return <code>true</code> if an asynchronous MDN is requested,
   *         <code>false</code> if not (default).
   * @since 3.0.4
   * @see #getAsyncMDNUrl()
   */
  public boolean isAsyncMDNRequested ()
  {
    return StringHelper.hasText (m_sAsyncMDNUrl);
  }

  /**
   * Set the Message ID format. This string may contain placeholders as
   * supported by the <code>com.helger.as2lib.params</code> parameters parsers.
   *
   * @param sMessageIDFormat
   *        The message ID format to use. May not be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public AS2ClientSettings setMessageIDFormat (@Nonnull final String sMessageIDFormat)
  {
    m_sMessageIDFormat = ValueEnforcer.notNull (sMessageIDFormat, "MessageIDFormat");
    return this;
  }

  /**
   * @return The message ID format to use. Never <code>null</code>. It defaults
   *         to {@value #DEFAULT_MESSAGE_ID_FORMAT}.
   * @see #DEFAULT_MESSAGE_ID_FORMAT
   * @see #setMessageIDFormat(String)
   */
  @Nonnull
  public String getMessageIDFormat ()
  {
    return m_sMessageIDFormat;
  }

  /**
   * Set the retry count for sending,
   *
   * @param nRetryCount
   *        Sending retry count. Values &le; 0 mean "no retry".
   * @return this for chaining
   * @see #getRetryCount()
   */
  @Nonnull
  public AS2ClientSettings setRetryCount (final int nRetryCount)
  {
    m_nRetryCount = nRetryCount;
    return this;
  }

  /**
   * @return The number of retries to be performed. May be &le; 0 meaning that
   *         no retry will happen. The default value is
   *         {@link #DEFAULT_RETRY_COUNT}.
   * @see #setRetryCount(int)
   */
  public int getRetryCount ()
  {
    return m_nRetryCount;
  }

  /**
   * Set the connection timeout in milliseconds.
   *
   * @param nConnectTimeoutMS
   *        Connect timeout milliseconds.
   * @return this for chaining
   * @see #getConnectTimeoutMS()
   * @since 3.0.2
   */
  @Nonnull
  public AS2ClientSettings setConnectTimeoutMS (final int nConnectTimeoutMS)
  {
    m_nConnectTimeoutMS = nConnectTimeoutMS;
    return this;
  }

  /**
   * @return The connection timeout in milliseconds. The default value is
   *         {@link #DEFAULT_CONNECT_TIMEOUT_MS}.
   * @since 3.0.2
   */
  public int getConnectTimeoutMS ()
  {
    return m_nConnectTimeoutMS;
  }

  /**
   * Set the read timeout in milliseconds.
   *
   * @param nReadTimeoutMS
   *        Read timeout milliseconds.
   * @return this for chaining
   * @see #getReadTimeoutMS()
   * @since 3.0.2
   */
  @Nonnull
  public AS2ClientSettings setReadTimeoutMS (final int nReadTimeoutMS)
  {
    m_nReadTimeoutMS = nReadTimeoutMS;
    return this;
  }

  /**
   * @return The read timeout in milliseconds. The default value is
   *         {@link #DEFAULT_READ_TIMEOUT_MS}.
   * @since 3.0.2
   */
  public int getReadTimeoutMS ()
  {
    return m_nReadTimeoutMS;
  }

  /**
   * @return The mutable custom header map. Never <code>null</code>.
   * @since 3.0.5
   */
  @Nonnull
  @ReturnsMutableObject ("Design")
  public HttpHeaderMap customHeaders ()
  {
    return m_aCustomHeaders;
  }
}
