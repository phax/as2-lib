/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2014 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.crypto;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.internet.MimeBodyPart;

public interface ICryptoHelper
{
  boolean isEncrypted (@Nonnull MimeBodyPart aPart) throws Exception;

  @Nonnull
  KeyStore createNewKeyStore () throws Exception;

  @Nonnull
  KeyStore loadKeyStore (@Nonnull InputStream aIS, @Nonnull char [] aPassword) throws Exception;

  @Nonnull
  KeyStore loadKeyStore (@Nonnull String sFilename, @Nonnull char [] aPassword) throws Exception;

  boolean isSigned (@Nonnull MimeBodyPart aPart) throws Exception;

  @Nonnull
  String calculateMIC (@Nonnull MimeBodyPart aPart, @Nonnull String sDigest, boolean bIncludeHeaders) throws Exception;

  @Nonnull
  MimeBodyPart decrypt (@Nonnull MimeBodyPart aPart, @Nonnull X509Certificate aCert, @Nonnull PrivateKey aKey) throws Exception;

  @Nonnull
  MimeBodyPart encrypt (@Nonnull MimeBodyPart aPart, @Nonnull X509Certificate aCert, @Nonnull String sAlgorithm) throws Exception;

  @Nonnull
  MimeBodyPart sign (@Nonnull MimeBodyPart aPart,
                     @Nonnull X509Certificate aCert,
                     @Nonnull PrivateKey key,
                     @Nonnull String sAlgorithm) throws Exception;

  /**
   * Verify the specified Mime Body part against the part certificate
   *
   * @param aPart
   *        Original part
   * @param aCert
   *        Certificate to check against or <code>null</code> if the certificate
   *        provided in the message should be used.
   * @return The signed content
   * @throws Exception
   */
  @Nonnull
  MimeBodyPart verify (@Nonnull MimeBodyPart aPart, @Nullable X509Certificate aCert) throws Exception;
}
