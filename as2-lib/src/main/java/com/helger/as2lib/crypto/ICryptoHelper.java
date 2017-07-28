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
package com.helger.as2lib.crypto;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.internet.MimeBodyPart;

import com.helger.as2lib.exception.OpenAS2Exception;

/**
 * Base interface for all crypto related methods in this project.
 *
 * @author Philip Helger
 */
public interface ICryptoHelper
{
  /**
   * @return A new key store. The default implementation creates a PKCS12 key
   *         store.
   * @throws Exception
   *         In case something goes wrong.
   */
  @Nonnull
  KeyStore createNewKeyStore () throws Exception;

  /**
   * Load a key store from the specified input stream.
   *
   * @param aIS
   *        The input stream to load the key store from. May not be
   *        <code>null</code>.
   * @param aPassword
   *        The password to be used for loading. May not be <code>null</code>.
   * @return The loaded key store and never <code>null</code>.
   * @throws Exception
   *         In case loading fails.
   */
  @Nonnull
  KeyStore loadKeyStore (@Nonnull InputStream aIS, @Nonnull char [] aPassword) throws Exception;

  /**
   * Check if the passed MIME body part is encrypted. The default implementation
   * checks if the base type of the content type is "application/pkcs7-mime" and
   * if the parameter "smime-type" has the value "enveloped-data".
   *
   * @param aPart
   *        The part to be checked.
   * @return <code>true</code> if it is encrypted, <code>false</code> otherwise.
   * @throws Exception
   *         In case something goes wrong.
   */
  boolean isEncrypted (@Nonnull MimeBodyPart aPart) throws Exception;

  /**
   * Check if the passed MIME body part is signed. The default implementation
   * checks if the base type of the content type is "multipart/signed".
   *
   * @param aPart
   *        The part to be checked.
   * @return <code>true</code> if it is signed, <code>false</code> otherwise.
   * @throws Exception
   *         In case something goes wrong.
   */
  boolean isSigned (@Nonnull MimeBodyPart aPart) throws Exception;

  /**
   * Check if the passed content type indicates compression. The default
   * implementation checks if the parameter "smime-type" has the value
   * "compressed-data".
   *
   * @param sContentType
   *        The content type to be checked. May not be <code>null</code>.
   * @return <code>true</code> if it is compressed, <code>false</code>
   *         otherwise.
   * @throws OpenAS2Exception
   *         In case something goes wrong.
   */
  boolean isCompressed (@Nonnull String sContentType) throws OpenAS2Exception;

  /**
   * Calculate the MIC
   *
   * @param aPart
   *        MIME part to calculate the MIC from. May not be <code>null</code>.
   * @param eDigestAlgorithm
   *        The digest algorithm to be used. May not be <code>null</code>.
   * @param bIncludeHeaders
   *        <code>true</code> if the MIME headers should be included,
   *        <code>false</code> if only the content should be used.
   * @return The calculated MIC and never <code>null</code>. This is the
   *         Base64-encoded message digest of the specified algorithm.
   * @throws Exception
   *         In case something goes wrong.
   */
  @Nonnull
  String calculateMIC (@Nonnull MimeBodyPart aPart,
                       @Nonnull ECryptoAlgorithmSign eDigestAlgorithm,
                       boolean bIncludeHeaders) throws Exception;

  @Nonnull
  MimeBodyPart encrypt (@Nonnull MimeBodyPart aPart,
                        @Nonnull X509Certificate aCert,
                        @Nonnull ECryptoAlgorithmCrypt eAlgorithm) throws Exception;

  @Nonnull
  MimeBodyPart decrypt (@Nonnull MimeBodyPart aPart,
                        @Nonnull X509Certificate aCert,
                        @Nonnull PrivateKey aKey,
                        boolean bForceDecrypt) throws Exception;

  /**
   * Sign a MIME body part.
   *
   * @param aPart
   *        MIME body part to be signed. May not be <code>null</code>.
   * @param aCert
   *        The certificate that should be added to the signed information. May
   *        not be <code>null</code>.
   * @param aKey
   *        Private key to be used for signing. May not be <code>null</code>.
   * @param eAlgorithm
   *        The algorithm to be used for signing. May not be <code>null</code>.
   * @param bIncludeCertificateInSignedContent
   *        <code>true</code> if the passed certificate should be part of the
   *        signed content, <code>false</code> if the certificate should not be
   *        put in the content. E.g. for PEPPOL this must be <code>true</code>.
   * @param bUseOldRFC3851MicAlgs
   *        <code>true</code> to use the old RFC 3851 MIC algorithm names (e.g.
   *        <code>sha1</code>), <code>false</code> to use the new RFC 5751 MIC
   *        algorithm names (e.g. <code>sha-1</code>).
   * @return The signed MIME body part. Never <code>null</code>.
   * @throws Exception
   *         In case something goes wrong.
   */
  @Nonnull
  MimeBodyPart sign (@Nonnull MimeBodyPart aPart,
                     @Nonnull X509Certificate aCert,
                     @Nonnull PrivateKey aKey,
                     @Nonnull ECryptoAlgorithmSign eAlgorithm,
                     boolean bIncludeCertificateInSignedContent,
                     boolean bUseOldRFC3851MicAlgs) throws Exception;

  /**
   * Verify the specified Mime Body part against the part certificate
   *
   * @param aPart
   *        Original part
   * @param aCert
   *        Certificate to check against or <code>null</code> if the certificate
   *        provided in the message should be used.
   * @param bUseCertificateInBodyPart
   *        If <code>true</code> any certificate that is passed in the body part
   *        is used for verification. If <code>false</code> only the provided
   *        certificate is used.
   * @param bForceVerify
   *        <code>true</code> to force verification even if the Content-Type
   *        header does not indicate so.
   * @return The signed content. Never <code>null</code>.
   * @throws Exception
   *         In case something goes wrong.
   */
  @Nonnull
  MimeBodyPart verify (@Nonnull MimeBodyPart aPart,
                       @Nullable X509Certificate aCert,
                       boolean bUseCertificateInBodyPart,
                       boolean bForceVerify) throws Exception;
}
