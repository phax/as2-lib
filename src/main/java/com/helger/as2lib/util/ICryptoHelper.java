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
package com.helger.as2lib.util;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;

import javax.mail.internet.MimeBodyPart;

public interface ICryptoHelper
{
  String DIGEST_MD5 = "md5";
  String DIGEST_SHA1 = "sha1";
  String CRYPT_CAST5 = "cast5";
  String CRYPT_3DES = "3des";
  String CRYPT_IDEA = "idea";
  String CRYPT_RC2 = "rc2";

  boolean isEncrypted (MimeBodyPart aPart) throws Exception;

  KeyStore getKeyStore () throws Exception;

  KeyStore loadKeyStore (InputStream aIS, char [] aPassword) throws Exception;

  KeyStore loadKeyStore (String sFilename, char [] aPassword) throws Exception;

  boolean isSigned (MimeBodyPart aPart) throws Exception;

  String calculateMIC (MimeBodyPart aPart, String sDigest, boolean bIncludeHeaders) throws Exception;

  MimeBodyPart decrypt (MimeBodyPart aPart, Certificate aCert, Key aKey) throws Exception;

  MimeBodyPart encrypt (MimeBodyPart aPart, Certificate aCert, String sAlgorithm) throws Exception;

  MimeBodyPart sign (MimeBodyPart aPart, Certificate aCert, Key key, String sAlgorithm) throws Exception;

  MimeBodyPart verify (MimeBodyPart aPart, Certificate aCert) throws Exception;
}
