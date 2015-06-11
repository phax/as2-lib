/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2015 Philip Helger philip[at]helger[dot]com
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
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.CAS2Info;
import com.helger.as2lib.crypto.ECryptoAlgorithm;
import com.helger.as2lib.disposition.DispositionOptions;
import com.helger.commons.ValueEnforcer;

/**
 * Settings object for a message delivery.
 *
 * @author oleo Date: May 12, 2010 Time: 5:16:57 PM
 * @author Philip Helger
 */
public class AS2ClientSettings
{
  public static final String DEFAULT_MDN_OPTIONS = new DispositionOptions ().setProtocolImportance (DispositionOptions.IMPORTANCE_OPTIONAL)
                                                                            .setProtocol (DispositionOptions.PROTOCOL_PKCS7_SIGNATURE)
                                                                            .setMICAlgImportance (DispositionOptions.IMPORTANCE_OPTIONAL)
                                                                            .setMICAlg (ECryptoAlgorithm.DIGEST_SHA1)
                                                                            .getAsString ();
  public static final String DEFAULT_MESSAGE_ID_FORMAT = CAS2Info.NAME +
                                                         "-$date.ddMMyyyyHHmmssZ$-$rand.1234$@$msg.sender.as2_id$_$msg.receiver.as2_id$";

  private File m_aKeyStoreFile;
  private String m_sKeyStorePassword;

  private String m_sEenderEmailAddress;
  private String m_sSenderAS2ID;
  private String m_sSenderKeyAlias;

  private String m_sReceiverAS2ID;
  private String m_sReceiverKeyAlias;
  private String m_sDestinationAS2URL;
  public X509Certificate m_aReceiverCert;

  private String m_sPartnershipName;
  private ECryptoAlgorithm m_eCryptAlgo;
  private ECryptoAlgorithm m_eSignAlgo;
  private String m_sMDNOptions = DEFAULT_MDN_OPTIONS;
  private String m_sMessageIDFormat = DEFAULT_MESSAGE_ID_FORMAT;

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
   * @return this
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
   */
  @Nullable
  public File getKeyStoreFile ()
  {
    return m_aKeyStoreFile;
  }

  /**
   * @return The key store password. May be <code>null</code> if not yet set.
   */
  @Nullable
  public String getKeyStorePassword ()
  {
    return m_sKeyStorePassword;
  }

  @Nonnull
  public AS2ClientSettings setSenderData (@Nonnull final String sAS2ID,
                                          @Nonnull final String sEmailAddress,
                                          @Nonnull final String sKeyAlias)
  {
    m_sSenderAS2ID = ValueEnforcer.notNull (sAS2ID, "AS2ID");
    m_sEenderEmailAddress = ValueEnforcer.notNull (sEmailAddress, "EmailAddress");
    m_sSenderKeyAlias = ValueEnforcer.notNull (sKeyAlias, "KeyAlias");
    return this;
  }

  @Nullable
  public String getSenderAS2ID ()
  {
    return m_sSenderAS2ID;
  }

  @Nullable
  public String getSenderEmailAddress ()
  {
    return m_sEenderEmailAddress;
  }

  @Nullable
  public String getSenderKeyAlias ()
  {
    return m_sSenderKeyAlias;
  }

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

  @Nullable
  public String getReceiverAS2ID ()
  {
    return m_sReceiverAS2ID;
  }

  @Nullable
  public String getReceiverKeyAlias ()
  {
    return m_sReceiverKeyAlias;
  }

  @Nullable
  public String getDestinationAS2URL ()
  {
    return m_sDestinationAS2URL;
  }

  @Nonnull
  public AS2ClientSettings setReceiverCertificate (@Nullable final X509Certificate aReceiverCertificate)
  {
    m_aReceiverCert = aReceiverCertificate;
    return this;
  }

  /**
   * @return The explicit certificate of the recipient. This might be used to
   *         dynamically add it to the certificate factory for dynamic
   *         partnership handling (like in PEPPOL). Maybe <code>null</code>.
   */
  @Nullable
  public X509Certificate getReceiverCertificate ()
  {
    return m_aReceiverCert;
  }

  @Nonnull
  public AS2ClientSettings setEncryptAndSign (@Nullable final ECryptoAlgorithm eCryptAlgo,
                                              @Nullable final ECryptoAlgorithm eSignAlgo)
  {
    if (eCryptAlgo != null && !eCryptAlgo.isEncrypting ())
      throw new IllegalArgumentException ("The provided crypt algorithm " +
                                          eCryptAlgo +
                                          " is not possible for crypting.");
    if (eSignAlgo != null && !eSignAlgo.isDigesting ())
      throw new IllegalArgumentException ("The provided sign algorithm " +
                                          eSignAlgo +
                                          " is not possible for digesting.");
    m_eCryptAlgo = eCryptAlgo;
    m_eSignAlgo = eSignAlgo;
    return this;
  }

  @Nullable
  public ECryptoAlgorithm getCryptAlgo ()
  {
    return m_eCryptAlgo;
  }

  @Nullable
  public String getCryptAlgoID ()
  {
    return m_eCryptAlgo == null ? null : m_eCryptAlgo.getID ();
  }

  @Nullable
  public ECryptoAlgorithm getSignAlgo ()
  {
    return m_eSignAlgo;
  }

  @Nullable
  public String getSignAlgoID ()
  {
    return m_eSignAlgo == null ? null : m_eSignAlgo.getID ();
  }

  @Nonnull
  public AS2ClientSettings setPartnershipName (@Nonnull final String sPartnershipName)
  {
    m_sPartnershipName = ValueEnforcer.notNull (sPartnershipName, "PartnershipName");
    return this;
  }

  @Nullable
  public String getPartnershipName ()
  {
    return m_sPartnershipName;
  }

  @Nonnull
  public AS2ClientSettings setMDNOptions (@Nonnull final String sMDNOptions)
  {
    m_sMDNOptions = ValueEnforcer.notNull (sMDNOptions, "MDNOptions");
    return this;
  }

  @Nonnull
  public AS2ClientSettings setMDNOptions (@Nonnull final DispositionOptions aDispositionOptions)
  {
    ValueEnforcer.notNull (aDispositionOptions, "DispositionOptions");
    return setMDNOptions (aDispositionOptions.getAsString ());
  }

  @Nullable
  public String getMDNOptions ()
  {
    return m_sMDNOptions;
  }

  @Nonnull
  public AS2ClientSettings setMessageIDFormat (@Nonnull final String sMessageIDFormat)
  {
    m_sMessageIDFormat = ValueEnforcer.notNull (sMessageIDFormat, "MessageIDFormat");
    return this;
  }

  @Nullable
  public String getMessageIDFormat ()
  {
    return m_sMessageIDFormat;
  }
}
