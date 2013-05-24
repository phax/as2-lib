/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013 Philip Helger ph[at]phloc[dot]com
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
import javax.annotation.WillClose;

import com.helger.as2lib.ISession;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedException;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.params.InvalidParameterException;
import com.helger.as2lib.partner.CSecurePartnership;
import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.util.AS2Util;
import com.phloc.commons.annotations.ReturnsMutableCopy;
import com.phloc.commons.io.streams.StreamUtils;

public class PKCS12CertificateFactory extends AbstractCertificateFactory implements IAliasedCertificateFactory, IKeyStoreCertificateFactory, IStorableCertificateFactory
{
  public static final String PARAM_FILENAME = "filename";
  public static final String PARAM_PASSWORD = "password";
  public static final String PARAM_INTERVAL = "interval";

  private KeyStore m_aKeyStore;

  @Nonnull
  public String getAlias (final Partnership partnership, final String partnershipType) throws OpenAS2Exception
  {
    String alias = null;
    if (Partnership.PTYPE_RECEIVER.equals (partnershipType))
      alias = partnership.getReceiverID (CSecurePartnership.PID_X509_ALIAS);
    else
      if (Partnership.PTYPE_SENDER.equals (partnershipType))
        alias = partnership.getSenderID (CSecurePartnership.PID_X509_ALIAS);

    if (alias == null)
      throw new CertificateNotFoundException (partnershipType, null);
    return alias;
  }

  @Nonnull
  public X509Certificate getCertificate (final String alias) throws OpenAS2Exception
  {
    try
    {
      final KeyStore ks = getKeyStore ();
      final X509Certificate cert = (X509Certificate) ks.getCertificate (alias);
      if (cert == null)
        throw new CertificateNotFoundException (null, alias);
      return cert;
    }
    catch (final KeyStoreException kse)
    {
      throw new WrappedException (kse);
    }
  }

  public X509Certificate getCertificate (final IMessage msg, final String partnershipType) throws OpenAS2Exception
  {
    try
    {
      final String sAlias = getAlias (msg.getPartnership (), partnershipType);
      return getCertificate (sAlias);
    }
    catch (final CertificateNotFoundException cnfe)
    {
      cnfe.setPartnershipType (partnershipType);
      throw cnfe;
    }
  }

  public X509Certificate getCertificate (final IMessageMDN mdn, final String partnershipType) throws OpenAS2Exception
  {
    try
    {
      final String sAlias = getAlias (mdn.getPartnership (), partnershipType);
      return getCertificate (sAlias);
    }
    catch (final CertificateNotFoundException cnfe)
    {
      cnfe.setPartnershipType (partnershipType);
      throw cnfe;
    }
  }

  @Nonnull
  @ReturnsMutableCopy
  public Map <String, Certificate> getCertificates () throws OpenAS2Exception
  {
    final KeyStore ks = getKeyStore ();

    try
    {
      final Map <String, Certificate> certs = new HashMap <String, Certificate> ();
      final Enumeration <String> e = ks.aliases ();
      while (e.hasMoreElements ())
      {
        final String certAlias = e.nextElement ();
        certs.put (certAlias, ks.getCertificate (certAlias));
      }
      return certs;
    }
    catch (final GeneralSecurityException gse)
    {
      throw new WrappedException (gse);
    }
  }

  public void setFilename (final String filename)
  {
    getParameters ().put (PARAM_FILENAME, filename);
  }

  public String getFilename () throws InvalidParameterException
  {
    return getParameterRequired (PARAM_FILENAME);
  }

  public void setKeyStore (final KeyStore keyStore)
  {
    m_aKeyStore = keyStore;
  }

  public KeyStore getKeyStore ()
  {
    return m_aKeyStore;
  }

  public void setPassword (final char [] password)
  {
    getParameters ().put (PARAM_PASSWORD, new String (password));
  }

  @Nonnull
  public char [] getPassword () throws InvalidParameterException
  {
    return getParameterRequired (PARAM_PASSWORD).toCharArray ();
  }

  public PrivateKey getPrivateKey (final X509Certificate cert) throws OpenAS2Exception
  {
    final KeyStore ks = getKeyStore ();
    String alias = null;

    try
    {
      alias = ks.getCertificateAlias (cert);
      if (alias == null)
        throw new KeyNotFoundException (cert, null);

      final PrivateKey key = (PrivateKey) ks.getKey (alias, getPassword ());
      if (key == null)
        throw new KeyNotFoundException (cert, null);

      return key;
    }
    catch (final GeneralSecurityException e)
    {
      throw new KeyNotFoundException (cert, alias, e);
    }
  }

  public PrivateKey getPrivateKey (final IMessage msg, final X509Certificate cert) throws OpenAS2Exception
  {
    return getPrivateKey (cert);
  }

  public PrivateKey getPrivateKey (final IMessageMDN mdn, final X509Certificate cert) throws OpenAS2Exception
  {
    return getPrivateKey (cert);
  }

  public void addCertificate (final String alias, final X509Certificate cert, final boolean overwrite) throws OpenAS2Exception
  {
    final KeyStore ks = getKeyStore ();

    try
    {
      if (ks.containsAlias (alias) && !overwrite)
        throw new CertificateExistsException (alias);

      ks.setCertificateEntry (alias, cert);
      save (getFilename (), getPassword ());
    }
    catch (final GeneralSecurityException gse)
    {
      throw new WrappedException (gse);
    }
  }

  public void addPrivateKey (final String alias, final Key key, final String password) throws OpenAS2Exception
  {
    final KeyStore ks = getKeyStore ();
    try
    {
      if (!ks.containsAlias (alias))
        throw new CertificateNotFoundException (null, alias);

      final Certificate [] certChain = ks.getCertificateChain (alias);
      ks.setKeyEntry (alias, key, password.toCharArray (), certChain);
      save (getFilename (), getPassword ());
    }
    catch (final GeneralSecurityException gse)
    {
      throw new WrappedException (gse);
    }
  }

  public void clearCertificates () throws OpenAS2Exception
  {
    final KeyStore ks = getKeyStore ();
    try
    {
      final Enumeration <String> aliases = ks.aliases ();
      while (aliases.hasMoreElements ())
        ks.deleteEntry (aliases.nextElement ());
      save (getFilename (), getPassword ());
    }
    catch (final GeneralSecurityException gse)
    {
      throw new WrappedException (gse);
    }
  }

  @Override
  public void initDynamicComponent (final ISession session, final Map <String, String> options) throws OpenAS2Exception
  {
    super.initDynamicComponent (session, options);

    try
    {
      m_aKeyStore = AS2Util.getCryptoHelper ().getKeyStore ();
    }
    catch (final Exception e)
    {
      throw new WrappedException (e);
    }

    load (getFilename (), getPassword ());
  }

  public void load (final String filename, final char [] password) throws OpenAS2Exception
  {
    try
    {
      final FileInputStream fIn = new FileInputStream (filename);
      load (fIn, password);
    }
    catch (final IOException ioe)
    {
      throw new WrappedException (ioe);
    }
  }

  public void load (@WillClose final InputStream in, final char [] password) throws OpenAS2Exception
  {
    try
    {
      final KeyStore ks = getKeyStore ();
      synchronized (ks)
      {
        ks.load (in, password);
      }
    }
    catch (final IOException ioe)
    {
      throw new WrappedException (ioe);
    }
    catch (final GeneralSecurityException gse)
    {
      throw new WrappedException (gse);
    }
    finally
    {
      StreamUtils.close (in);
    }
  }

  public void load () throws OpenAS2Exception
  {
    load (getFilename (), getPassword ());
  }

  public void removeCertificate (final X509Certificate cert) throws OpenAS2Exception
  {
    final KeyStore ks = getKeyStore ();

    try
    {
      final String alias = ks.getCertificateAlias (cert);
      if (alias == null)
        throw new CertificateNotFoundException (cert);
      removeCertificate (alias);
    }
    catch (final GeneralSecurityException gse)
    {
      throw new WrappedException (gse);
    }
  }

  public void removeCertificate (final String alias) throws OpenAS2Exception
  {
    final KeyStore ks = getKeyStore ();
    try
    {
      if (ks.getCertificate (alias) == null)
        throw new CertificateNotFoundException (null, alias);

      ks.deleteEntry (alias);
      save (getFilename (), getPassword ());
    }
    catch (final GeneralSecurityException gse)
    {
      throw new WrappedException (gse);
    }
  }

  public void save () throws OpenAS2Exception
  {
    save (getFilename (), getPassword ());
  }

  public void save (final String filename, final char [] password) throws OpenAS2Exception
  {
    try
    {
      final FileOutputStream fOut = new FileOutputStream (filename, false);
      save (fOut, password);
    }
    catch (final IOException ioe)
    {
      throw new WrappedException (ioe);
    }
  }

  public void save (@WillClose final OutputStream out, final char [] password) throws OpenAS2Exception
  {
    try
    {
      getKeyStore ().store (out, password);
    }
    catch (final IOException ioe)
    {
      throw new WrappedException (ioe);
    }
    catch (final GeneralSecurityException gse)
    {
      throw new WrappedException (gse);
    }
    finally
    {
      StreamUtils.close (out);
    }
  }
}
