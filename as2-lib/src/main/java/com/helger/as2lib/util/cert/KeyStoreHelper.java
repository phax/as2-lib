/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2016 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.util.cert;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;

import javax.annotation.Nonnull;

import com.helger.as2lib.crypto.ICryptoHelper;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.io.EAppend;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.stream.StreamHelper;

/**
 * KeyStore reader and write class
 *
 * @author Philip Helger
 */
public final class KeyStoreHelper
{
  private KeyStoreHelper ()
  {}

  @Nonnull
  public static OpenAS2KeyStore readKeyStore (@Nonnull final String sFilename,
                                              @Nonnull final char [] aPassword,
                                              @Nonnull final ICryptoHelper aCryptoHelper) throws Exception
  {
    final InputStream aIS = FileHelper.getInputStream (sFilename);
    if (aIS == null)
      throw new IllegalArgumentException ("Failed to open KeyStore '" + sFilename + "' for reading");

    try
    {
      final KeyStore aKeyStore = aCryptoHelper.loadKeyStore (aIS, aPassword);
      return new OpenAS2KeyStore (aKeyStore);
    }
    finally
    {
      StreamHelper.close (aIS);
    }
  }

  public static void writeKeyStore (@Nonnull final OpenAS2KeyStore aKeyStore,
                                    @Nonnull final String sFilename,
                                    @Nonnull final char [] aPassword) throws GeneralSecurityException, IOException
  {
    final OutputStream aOS = FileHelper.getOutputStream (sFilename, EAppend.TRUNCATE);
    if (aOS == null)
      throw new IllegalArgumentException ("Failed to open KeyStore '" + sFilename + "' for writing");

    try
    {
      aKeyStore.getKeyStore ().store (aOS, aPassword);
    }
    finally
    {
      StreamHelper.close (aOS);
    }
  }

  @Nonnull
  public static X509Certificate readX509Certificate (@Nonnull final String sFilename) throws CertificateException
  {
    final InputStream aIS = FileHelper.getInputStream (sFilename);
    if (aIS == null)
      throw new IllegalArgumentException ("Failed to open KeyStore '" + sFilename + "' for reading");

    try
    {
      final CertificateFactory cf = CertificateFactory.getInstance ("X.509");
      final Collection <? extends Certificate> c = cf.generateCertificates (aIS);
      return (X509Certificate) CollectionHelper.getFirstElement (c);
    }
    finally
    {
      StreamHelper.close (aIS);
    }
  }
}
