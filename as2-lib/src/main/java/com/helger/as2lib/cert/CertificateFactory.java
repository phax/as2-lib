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
package com.helger.as2lib.cert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.params.InvalidParameterException;
import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.session.IAS2Session;
import com.helger.as2lib.util.AS2Helper;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.attr.IStringMap;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.io.EAppend;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.security.keystore.EKeyStoreType;

/**
 * An implementation of a file-based certificate factory using a custom key
 * store type.
 *
 * @author Philip Helger
 */
public class CertificateFactory extends AbstractCertificateFactory implements
                                IAliasedCertificateFactory,
                                IKeyStoreCertificateFactory,
                                IStorableCertificateFactory
{
  public static final EKeyStoreType DEFAULT_KEY_STORE_TYPE = EKeyStoreType.PKCS12;

  /** Key store type; since 4.0.0 */
  public static final String ATTR_TYPE = "type";
  public static final String ATTR_FILENAME = "filename";
  public static final String ATTR_PASSWORD = "password";
  public static final String ATTR_SAVE_CHANGES_TO_FILE = "autosave";

  private static final Logger s_aLogger = LoggerFactory.getLogger (CertificateFactory.class);

  private KeyStore m_aKeyStore;

  public CertificateFactory ()
  {}

  @Override
  public void initDynamicComponent (@Nonnull final IAS2Session aSession,
                                    @Nullable final IStringMap aOptions) throws OpenAS2Exception
  {
    super.initDynamicComponent (aSession, aOptions);

    try
    {
      final String sKeyStoreType = getAsString (ATTR_TYPE);
      final EKeyStoreType eKeyStoreType = EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sKeyStoreType,
                                                                                           DEFAULT_KEY_STORE_TYPE);
      m_aKeyStore = AS2Helper.getCryptoHelper ().createNewKeyStore (eKeyStoreType);
    }
    catch (final Exception ex)
    {
      throw WrappedOpenAS2Exception.wrap (ex);
    }

    load (getFilename (), getPassword ());
  }

  @Override
  @Nonnull
  public String getAlias (@Nonnull final Partnership aPartnership,
                          @Nonnull final ECertificatePartnershipType ePartnershipType) throws OpenAS2Exception
  {
    ValueEnforcer.notNull (aPartnership, "Partnership");
    ValueEnforcer.notNull (ePartnershipType, "PartnershipType");

    String sAlias = null;
    switch (ePartnershipType)
    {
      case RECEIVER:
        sAlias = aPartnership.getReceiverX509Alias ();
        break;
      case SENDER:
        sAlias = aPartnership.getSenderX509Alias ();
        break;
    }

    if (sAlias == null)
      throw new CertificateNotFoundException (ePartnershipType, aPartnership);
    return sAlias;
  }

  @Override
  @Nonnull
  protected X509Certificate internalGetCertificate (@Nullable final String sAlias,
                                                    @Nullable final ECertificatePartnershipType ePartnershipType) throws OpenAS2Exception
  {
    try
    {
      final KeyStore aKeyStore = getKeyStore ();
      final X509Certificate aCert = (X509Certificate) aKeyStore.getCertificate (sAlias);
      if (aCert == null)
        throw new CertificateNotFoundException (ePartnershipType, sAlias);
      return aCert;
    }
    catch (final KeyStoreException ex)
    {
      throw WrappedOpenAS2Exception.wrap (ex);
    }
  }

  @Nonnull
  public X509Certificate getCertificate (@Nullable final String sAlias) throws OpenAS2Exception
  {
    return internalGetCertificate (sAlias, null);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsOrderedMap <String, Certificate> getCertificates () throws OpenAS2Exception
  {
    final KeyStore aKeyStore = getKeyStore ();

    try
    {
      final ICommonsOrderedMap <String, Certificate> aCerts = new CommonsLinkedHashMap <> ();
      final Enumeration <String> aAliases = aKeyStore.aliases ();
      while (aAliases.hasMoreElements ())
      {
        final String sCertAlias = aAliases.nextElement ();
        aCerts.put (sCertAlias, aKeyStore.getCertificate (sCertAlias));
      }
      return aCerts;
    }
    catch (final GeneralSecurityException ex)
    {
      throw WrappedOpenAS2Exception.wrap (ex);
    }
  }

  public void setFilename (@Nullable final String sFilename)
  {
    putIn (ATTR_FILENAME, sFilename);
  }

  @Nonnull
  public String getFilename () throws InvalidParameterException
  {
    return getAttributeAsStringRequired (ATTR_FILENAME);
  }

  @Nonnull
  public KeyStore getKeyStore ()
  {
    if (m_aKeyStore == null)
      throw new IllegalStateException ("No keystore present");
    return m_aKeyStore;
  }

  public void setPassword (@Nonnull final char [] aPassword)
  {
    putIn (ATTR_PASSWORD, new String (aPassword));
  }

  @Nonnull
  public char [] getPassword () throws InvalidParameterException
  {
    return getAttributeAsStringRequired (ATTR_PASSWORD).toCharArray ();
  }

  public void setSaveChangesToFile (final boolean bSaveChangesToFile)
  {
    putIn (ATTR_SAVE_CHANGES_TO_FILE, bSaveChangesToFile);
  }

  public boolean isSaveChangesToFile ()
  {
    return getAsBoolean (ATTR_SAVE_CHANGES_TO_FILE, DEFAULT_SAVE_CHANGES_TO_FILE);
  }

  /**
   * Custom callback method that is invoked if something changes in the
   * keystore. By default the changes are written back to disk.
   *
   * @throws OpenAS2Exception
   *         In case saving fails.
   * @see #isSaveChangesToFile()
   * @see #setSaveChangesToFile(boolean)
   */
  @OverrideOnDemand
  protected void onChange () throws OpenAS2Exception
  {
    if (isSaveChangesToFile ())
      save ();
  }

  @Nonnull
  public PrivateKey getPrivateKey (@Nullable final X509Certificate aCert) throws OpenAS2Exception
  {
    final KeyStore aKeyStore = getKeyStore ();
    String sAlias = null;

    try
    {
      // This method heuristically scans the keystore and delivery the first
      // result.
      sAlias = aKeyStore.getCertificateAlias (aCert);
      if (sAlias == null)
        throw new KeyNotFoundException (aCert);

      final PrivateKey aKey = (PrivateKey) aKeyStore.getKey (sAlias, getPassword ());
      if (aKey == null)
        throw new KeyNotFoundException (aCert, sAlias);

      return aKey;
    }
    catch (final GeneralSecurityException ex)
    {
      throw new KeyNotFoundException (aCert, sAlias, ex);
    }
  }

  @Nonnull
  public PrivateKey getPrivateKey (@Nullable final IMessage aMsg,
                                   @Nullable final X509Certificate aCert) throws OpenAS2Exception
  {
    return getPrivateKey (aCert);
  }

  @Nonnull
  public PrivateKey getPrivateKey (@Nullable final IMessageMDN aMDN,
                                   @Nullable final X509Certificate aCert) throws OpenAS2Exception
  {
    return getPrivateKey (aCert);
  }

  public void addCertificate (@Nonnull @Nonempty final String sAlias,
                              @Nonnull final X509Certificate aCert,
                              final boolean bOverwrite) throws OpenAS2Exception
  {
    ValueEnforcer.notEmpty (sAlias, "Alias");
    ValueEnforcer.notNull (aCert, "Cert");

    final KeyStore aKeyStore = getKeyStore ();

    try
    {
      if (aKeyStore.containsAlias (sAlias) && !bOverwrite)
        throw new CertificateExistsException (sAlias);

      aKeyStore.setCertificateEntry (sAlias, aCert);
      onChange ();
      s_aLogger.info ("Added certificate alias '" + sAlias + "' of certificate '" + aCert.getSubjectDN () + "'");
    }
    catch (final GeneralSecurityException ex)
    {
      throw WrappedOpenAS2Exception.wrap (ex);
    }
  }

  public void addPrivateKey (@Nonnull @Nonempty final String sAlias,
                             @Nonnull final Key aKey,
                             @Nonnull final String sPassword) throws OpenAS2Exception
  {
    ValueEnforcer.notEmpty (sAlias, "Alias");
    ValueEnforcer.notNull (aKey, "Key");
    ValueEnforcer.notNull (sPassword, "Password");

    final KeyStore aKeyStore = getKeyStore ();
    try
    {
      if (!aKeyStore.containsAlias (sAlias))
        throw new CertificateNotFoundException (null, sAlias);

      final Certificate [] aCertChain = aKeyStore.getCertificateChain (sAlias);
      aKeyStore.setKeyEntry (sAlias, aKey, sPassword.toCharArray (), aCertChain);
      onChange ();
      s_aLogger.info ("Added key alias '" + sAlias + "'");
    }
    catch (final GeneralSecurityException ex)
    {
      throw WrappedOpenAS2Exception.wrap (ex);
    }
  }

  public void clearCertificates () throws OpenAS2Exception
  {
    final KeyStore aKeyStore = getKeyStore ();
    try
    {
      // Make a copy to be sure
      for (final String sAlias : CollectionHelper.newList (aKeyStore.aliases ()))
        aKeyStore.deleteEntry (sAlias);
      onChange ();
      s_aLogger.info ("Remove all aliases in key store");
    }
    catch (final GeneralSecurityException ex)
    {
      throw WrappedOpenAS2Exception.wrap (ex);
    }
  }

  public void load (@Nonnull final String sFilename, @Nonnull final char [] aPassword) throws OpenAS2Exception
  {
    final InputStream aFIS = FileHelper.getInputStream (new File (sFilename));
    load (aFIS, aPassword);
  }

  public void load (@Nonnull @WillClose final InputStream aIS, @Nonnull final char [] aPassword) throws OpenAS2Exception
  {
    try
    {
      final KeyStore aKeyStore = getKeyStore ();
      synchronized (aKeyStore)
      {
        aKeyStore.load (aIS, aPassword);
      }
    }
    catch (final IOException | GeneralSecurityException ex)
    {
      throw WrappedOpenAS2Exception.wrap (ex);
    }
    finally
    {
      StreamHelper.close (aIS);
    }
  }

  public void load () throws OpenAS2Exception
  {
    load (getFilename (), getPassword ());
  }

  public void removeCertificate (@Nonnull final X509Certificate aCert) throws OpenAS2Exception
  {
    ValueEnforcer.notNull (aCert, "Cert");
    final KeyStore aKeyStore = getKeyStore ();

    try
    {
      final String sAlias = aKeyStore.getCertificateAlias (aCert);
      if (sAlias == null)
        throw new CertificateNotFoundException (aCert);
      removeCertificate (sAlias);
    }
    catch (final GeneralSecurityException ex)
    {
      throw WrappedOpenAS2Exception.wrap (ex);
    }
  }

  public void removeCertificate (@Nullable final String sAlias) throws OpenAS2Exception
  {
    final KeyStore aKeyStore = getKeyStore ();
    try
    {
      final Certificate aCert = aKeyStore.getCertificate (sAlias);
      if (aCert == null)
        throw new CertificateNotFoundException (null, sAlias);

      aKeyStore.deleteEntry (sAlias);
      onChange ();
      s_aLogger.info ("Removed certificate alias '" + sAlias + "'");
    }
    catch (final GeneralSecurityException ex)
    {
      throw WrappedOpenAS2Exception.wrap (ex);
    }
  }

  public void save () throws OpenAS2Exception
  {
    save (getFilename (), getPassword ());
  }

  public void save (@Nonnull final String sFilename, @Nonnull final char [] aPassword) throws OpenAS2Exception
  {
    final OutputStream fOut = FileHelper.getOutputStream (new File (sFilename), EAppend.TRUNCATE);
    save (fOut, aPassword);
  }

  public void save (@Nonnull @WillClose final OutputStream aOS,
                    @Nonnull final char [] aPassword) throws OpenAS2Exception
  {
    try
    {
      final KeyStore aKeyStore = getKeyStore ();
      synchronized (aKeyStore)
      {
        aKeyStore.store (aOS, aPassword);
      }
    }
    catch (final IOException ex)
    {
      throw WrappedOpenAS2Exception.wrap (ex);
    }
    catch (final GeneralSecurityException ex)
    {
      throw WrappedOpenAS2Exception.wrap (ex);
    }
    finally
    {
      StreamHelper.close (aOS);
    }
  }
}
