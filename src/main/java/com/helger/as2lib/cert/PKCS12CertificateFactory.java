/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2014 Philip Helger ph[at]phloc[dot]com
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

import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;

import com.helger.as2lib.ISession;
import com.helger.as2lib.exception.CertificateExistsException;
import com.helger.as2lib.exception.CertificateNotFoundException;
import com.helger.as2lib.exception.InvalidParameterException;
import com.helger.as2lib.exception.KeyNotFoundException;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.partner.CPartnershipIDs;
import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.util.AS2Util;
import com.helger.as2lib.util.IStringMap;
import com.helger.commons.annotations.ReturnsMutableCopy;
import com.helger.commons.io.streams.StreamUtils;

public class PKCS12CertificateFactory extends AbstractCertificateFactory implements IAliasedCertificateFactory, IKeyStoreCertificateFactory, IStorableCertificateFactory
{
  public static final String PARAM_FILENAME = "filename";
  public static final String PARAM_PASSWORD = "password";
  public static final String PARAM_INTERVAL = "interval";

  private KeyStore m_aKeyStore;

  @Nonnull
  public String getAlias (@Nonnull final Partnership aPartnership,
                          @Nonnull final ECertificatePartnershipType ePartnershipType) throws OpenAS2Exception
  {
    String sAlias = null;
    switch (ePartnershipType)
    {
      case RECEIVER:
        sAlias = aPartnership.getReceiverID (CPartnershipIDs.PID_X509_ALIAS);
        break;
      case SENDER:
        sAlias = aPartnership.getSenderID (CPartnershipIDs.PID_X509_ALIAS);
        break;
    }

    if (sAlias == null)
      throw new CertificateNotFoundException (ePartnershipType, null);
    return sAlias;
  }

  @Nonnull
  private X509Certificate _getCertificate (final ECertificatePartnershipType ePartnershipType, final String sAlias) throws OpenAS2Exception
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
      throw new WrappedOpenAS2Exception (ex);
    }
  }

  @Nonnull
  public X509Certificate getCertificate (final String sAlias) throws OpenAS2Exception
  {
    return _getCertificate (null, sAlias);
  }

  public X509Certificate getCertificate (@Nonnull final IMessage aMsg,
                                         final ECertificatePartnershipType ePartnershipType) throws OpenAS2Exception
  {
    try
    {
      final String sAlias = getAlias (aMsg.getPartnership (), ePartnershipType);
      return _getCertificate (ePartnershipType, sAlias);
    }
    catch (final CertificateNotFoundException ex)
    {
      throw ex;
    }
  }

  public X509Certificate getCertificate (@Nonnull final IMessageMDN aMDN,
                                         final ECertificatePartnershipType ePartnershipType) throws OpenAS2Exception
  {
    try
    {
      final String sAlias = getAlias (aMDN.getPartnership (), ePartnershipType);
      return _getCertificate (ePartnershipType, sAlias);
    }
    catch (final CertificateNotFoundException ex)
    {
      throw ex;
    }
  }

  @Nonnull
  @ReturnsMutableCopy
  public Map <String, Certificate> getCertificates () throws OpenAS2Exception
  {
    final KeyStore aKeyStore = getKeyStore ();

    try
    {
      final Map <String, Certificate> aCerts = new HashMap <String, Certificate> ();
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
      throw new WrappedOpenAS2Exception (ex);
    }
  }

  public void setFilename (final String sFilename)
  {
    setAttribute (PARAM_FILENAME, sFilename);
  }

  public String getFilename () throws InvalidParameterException
  {
    return getParameterRequired (PARAM_FILENAME);
  }

  public void setKeyStore (final KeyStore aKeyStore)
  {
    m_aKeyStore = aKeyStore;
  }

  public KeyStore getKeyStore ()
  {
    return m_aKeyStore;
  }

  public void setPassword (final char [] aPassword)
  {
    setAttribute (PARAM_PASSWORD, new String (aPassword));
  }

  @Nonnull
  public char [] getPassword () throws InvalidParameterException
  {
    return getParameterRequired (PARAM_PASSWORD).toCharArray ();
  }

  public PrivateKey getPrivateKey (final X509Certificate aCert) throws OpenAS2Exception
  {
    final KeyStore aKeyStore = getKeyStore ();
    String sAlias = null;

    try
    {
      sAlias = aKeyStore.getCertificateAlias (aCert);
      if (sAlias == null)
        throw new KeyNotFoundException (aCert, null);

      final PrivateKey key = (PrivateKey) aKeyStore.getKey (sAlias, getPassword ());
      if (key == null)
        throw new KeyNotFoundException (aCert, null);

      return key;
    }
    catch (final GeneralSecurityException ex)
    {
      throw new KeyNotFoundException (aCert, sAlias, ex);
    }
  }

  public PrivateKey getPrivateKey (final IMessage aMsg, final X509Certificate aCert) throws OpenAS2Exception
  {
    return getPrivateKey (aCert);
  }

  public PrivateKey getPrivateKey (final IMessageMDN aMDN, final X509Certificate aCert) throws OpenAS2Exception
  {
    return getPrivateKey (aCert);
  }

  public void addCertificate (final String sAlias, final X509Certificate aCert, final boolean bOverwrite) throws OpenAS2Exception
  {
    final KeyStore aKeyStore = getKeyStore ();

    try
    {
      if (aKeyStore.containsAlias (sAlias) && !bOverwrite)
        throw new CertificateExistsException (sAlias);

      aKeyStore.setCertificateEntry (sAlias, aCert);
      save (getFilename (), getPassword ());
    }
    catch (final GeneralSecurityException ex)
    {
      throw new WrappedOpenAS2Exception (ex);
    }
  }

  public void addPrivateKey (final String sAlias, final Key aKey, final String sPassword) throws OpenAS2Exception
  {
    final KeyStore aKeyStore = getKeyStore ();
    try
    {
      if (!aKeyStore.containsAlias (sAlias))
        throw new CertificateNotFoundException (null, sAlias);

      final Certificate [] aCertChain = aKeyStore.getCertificateChain (sAlias);
      aKeyStore.setKeyEntry (sAlias, aKey, sPassword.toCharArray (), aCertChain);
      save (getFilename (), getPassword ());
    }
    catch (final GeneralSecurityException ex)
    {
      throw new WrappedOpenAS2Exception (ex);
    }
  }

  public void clearCertificates () throws OpenAS2Exception
  {
    final KeyStore aKeyStore = getKeyStore ();
    try
    {
      final Enumeration <String> aAliases = aKeyStore.aliases ();
      while (aAliases.hasMoreElements ())
        aKeyStore.deleteEntry (aAliases.nextElement ());
      save (getFilename (), getPassword ());
    }
    catch (final GeneralSecurityException ex)
    {
      throw new WrappedOpenAS2Exception (ex);
    }
  }

  @Override
  public void initDynamicComponent (@Nonnull final ISession aSession, @Nullable final IStringMap aOptions) throws OpenAS2Exception
  {
    super.initDynamicComponent (aSession, aOptions);

    try
    {
      m_aKeyStore = AS2Util.getCryptoHelper ().getKeyStore ();
    }
    catch (final Exception ex)
    {
      throw new WrappedOpenAS2Exception (ex);
    }

    load (getFilename (), getPassword ());
  }

  public void load (final String sFilename, final char [] aPassword) throws OpenAS2Exception
  {
    try
    {
      final FileInputStream aFIS = new FileInputStream (sFilename);
      load (aFIS, aPassword);
    }
    catch (final IOException ex)
    {
      throw new WrappedOpenAS2Exception (ex);
    }
  }

  public void load (@WillClose final InputStream aIS, final char [] aPassword) throws OpenAS2Exception
  {
    try
    {
      final KeyStore aKeyStore = getKeyStore ();
      synchronized (aKeyStore)
      {
        aKeyStore.load (aIS, aPassword);
      }
    }
    catch (final IOException ex)
    {
      throw new WrappedOpenAS2Exception (ex);
    }
    catch (final GeneralSecurityException ex)
    {
      throw new WrappedOpenAS2Exception (ex);
    }
    finally
    {
      StreamUtils.close (aIS);
    }
  }

  public void load () throws OpenAS2Exception
  {
    load (getFilename (), getPassword ());
  }

  public void removeCertificate (final X509Certificate aCert) throws OpenAS2Exception
  {
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
      throw new WrappedOpenAS2Exception (ex);
    }
  }

  public void removeCertificate (final String sAlias) throws OpenAS2Exception
  {
    final KeyStore aKeyStore = getKeyStore ();
    try
    {
      if (aKeyStore.getCertificate (sAlias) == null)
        throw new CertificateNotFoundException (null, sAlias);

      aKeyStore.deleteEntry (sAlias);
      save (getFilename (), getPassword ());
    }
    catch (final GeneralSecurityException ex)
    {
      throw new WrappedOpenAS2Exception (ex);
    }
  }

  public void save () throws OpenAS2Exception
  {
    save (getFilename (), getPassword ());
  }

  public void save (final String sFilename, final char [] aPassword) throws OpenAS2Exception
  {
    try
    {
      final FileOutputStream fOut = new FileOutputStream (sFilename, false);
      save (fOut, aPassword);
    }
    catch (final IOException ex)
    {
      throw new WrappedOpenAS2Exception (ex);
    }
  }

  public void save (@WillClose final OutputStream aOS, final char [] aPassword) throws OpenAS2Exception
  {
    try
    {
      getKeyStore ().store (aOS, aPassword);
    }
    catch (final IOException ex)
    {
      throw new WrappedOpenAS2Exception (ex);
    }
    catch (final GeneralSecurityException ex)
    {
      throw new WrappedOpenAS2Exception (ex);
    }
    finally
    {
      StreamUtils.close (aOS);
    }
  }
}
