/*
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2025 Philip Helger philip[at]helger[dot]com
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
package com.helger.phase2.util.cert;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.Enumeration;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class AS2KeyStore implements ICertificateStore
{
  private final KeyStore m_aKeyStore;

  public AS2KeyStore (@Nonnull final KeyStore aKeyStore)
  {
    m_aKeyStore = ValueEnforcer.notNull (aKeyStore, "KeyStore");
  }

  @Nonnull
  public KeyStore getKeyStore ()
  {
    return m_aKeyStore;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> getAliases () throws AS2CertificateException
  {
    try
    {
      return new CommonsArrayList <> (getKeyStore ().aliases ());
    }
    catch (final KeyStoreException kse)
    {
      throw new AS2CertificateException ("Error getting aliases", kse);
    }
  }

  @Nullable
  public Certificate getCertificate (@Nullable final String sAlias) throws AS2CertificateException
  {
    if (sAlias == null)
      return null;

    try
    {
      return getKeyStore ().getCertificate (sAlias);
    }
    catch (final KeyStoreException kse)
    {
      throw new AS2CertificateException ("Error getting certificate for alias: " + sAlias, kse);
    }
  }

  public void setCertificate (@Nonnull final String sAlias, @Nonnull final Certificate aCert)
                                                                                              throws AS2CertificateException
  {
    ValueEnforcer.notNull (sAlias, "Alias");
    ValueEnforcer.notNull (aCert, "Certificate");

    try
    {
      getKeyStore ().setCertificateEntry (sAlias, aCert);
    }
    catch (final KeyStoreException kse)
    {
      throw new AS2CertificateException ("Error setting certificate: " + sAlias, kse);
    }
  }

  @Nullable
  public String getAlias (@Nullable final Certificate aCert) throws AS2CertificateException
  {
    if (aCert == null)
      return null;

    try
    {
      return getKeyStore ().getCertificateAlias (aCert);
    }
    catch (final KeyStoreException kse)
    {
      throw new AS2CertificateException ("Error getting alias for certificate: " + aCert.toString (), kse);
    }
  }

  public void removeCertificate (@Nullable final String sAlias) throws AS2CertificateException
  {
    if (sAlias != null)
      try
      {
        getKeyStore ().deleteEntry (sAlias);
      }
      catch (final KeyStoreException kse)
      {
        throw new AS2CertificateException ("Error while removing certificate: " + sAlias, kse);
      }
  }

  public void clearCertificates () throws AS2CertificateException
  {
    try
    {
      final KeyStore aKeyStore = getKeyStore ();
      final Enumeration <String> aAliases = aKeyStore.aliases ();
      while (aAliases.hasMoreElements ())
      {
        aKeyStore.deleteEntry (aAliases.nextElement ());
      }
    }
    catch (final KeyStoreException kse)
    {
      throw new AS2CertificateException ("Error clearing certificates", kse);
    }
  }

  @Nullable
  public Key getKey (@Nullable final String sAlias, @Nullable final char [] aPassword) throws AS2CertificateException
  {
    if (sAlias == null)
      return null;

    try
    {
      return getKeyStore ().getKey (sAlias, aPassword);
    }
    catch (final GeneralSecurityException gse)
    {
      throw new AS2CertificateException ("Error getting key for alias: " + sAlias, gse);
    }
  }

  public void setKey (@Nonnull final String sAlias, @Nonnull final Key aKey, @Nullable final char [] aPassword)
                                                                                                                throws AS2CertificateException
  {
    ValueEnforcer.notNull (sAlias, "Alias");
    ValueEnforcer.notNull (aKey, "Key");

    try
    {
      final KeyStore aKeyStore = getKeyStore ();
      final Certificate [] aCertChain = aKeyStore.getCertificateChain (sAlias);
      aKeyStore.setKeyEntry (sAlias, aKey, aPassword, aCertChain);
    }
    catch (final KeyStoreException kse)
    {
      throw new AS2CertificateException ("Error setting key for alias: " + sAlias, kse);
    }
  }
}
