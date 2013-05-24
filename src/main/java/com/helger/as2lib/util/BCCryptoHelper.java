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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Locale;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.bouncycastle.util.encoders.Base64;

import com.phloc.commons.io.streams.NonBlockingByteArrayInputStream;
import com.phloc.commons.io.streams.NonBlockingByteArrayOutputStream;
import com.phloc.commons.io.streams.StreamUtils;

public class BCCryptoHelper implements ICryptoHelper
{
  public BCCryptoHelper ()
  {
    Security.addProvider (new BouncyCastleProvider ());

    final MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap ();
    mc.addMailcap ("application/pkcs7-signature;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_signature");
    mc.addMailcap ("application/pkcs7-mime;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_mime");
    mc.addMailcap ("application/x-pkcs7-signature;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_signature");
    mc.addMailcap ("application/x-pkcs7-mime;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_mime");
    mc.addMailcap ("multipart/signed;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.multipart_signed");
    CommandMap.setDefaultCommandMap (mc);
  }

  public boolean isEncrypted (final MimeBodyPart part) throws MessagingException
  {
    // Content-Type is sthg like:
    // application/pkcs7-mime; name=smime.p7m; smime-type=enveloped-data
    final ContentType contentType = new ContentType (part.getContentType ());
    final String baseType = contentType.getBaseType ().toLowerCase (Locale.US);
    if (!baseType.equals ("application/pkcs7-mime"))
      return false;

    final String smimeType = contentType.getParameter ("smime-type");
    return smimeType != null && smimeType.equalsIgnoreCase ("enveloped-data");
  }

  public boolean isSigned (final MimeBodyPart part) throws MessagingException
  {
    final ContentType contentType = new ContentType (part.getContentType ());
    final String baseType = contentType.getBaseType ().toLowerCase (Locale.US);
    return baseType.equals ("multipart/signed");
  }

  public String calculateMIC (final MimeBodyPart part, final String digest, final boolean includeHeaders) throws GeneralSecurityException,
                                                                                                         MessagingException,
                                                                                                         IOException
  {
    final String micAlg = convertAlgorithm (digest, true);

    final MessageDigest md = MessageDigest.getInstance (micAlg, "BC");

    // convert the Mime data to a byte array, then to an InputStream
    final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();

    if (includeHeaders)
    {
      part.writeTo (aBAOS);
    }
    else
    {
      StreamUtils.copyInputStreamToOutputStream (part.getInputStream (), aBAOS);
    }

    final byte [] data = aBAOS.toByteArray ();

    final InputStream bIn = trimCRLFPrefix (data);

    // calculate the hash of the data and mime header
    final DigestInputStream digIn = new DigestInputStream (bIn, md);
    final byte [] buf = new byte [4096];
    while (digIn.read (buf) >= 0)
    {}

    aBAOS.close ();

    final byte [] mic = digIn.getMessageDigest ().digest ();
    final String micString = new String (Base64.encode (mic));
    final StringBuilder micResult = new StringBuilder (micString);
    micResult.append (", ").append (digest);

    return micResult.toString ();
  }

  public MimeBodyPart decrypt (final MimeBodyPart part, final Certificate cert, final Key key) throws GeneralSecurityException,
                                                                                              MessagingException,
                                                                                              CMSException,
                                                                                              IOException,
                                                                                              SMIMEException
  {
    // Make sure the data is encrypted
    if (!isEncrypted (part))
    {
      throw new GeneralSecurityException ("Content-Type indicates data isn't encrypted");
    }

    // Cast parameters to what BC needs
    final X509Certificate x509Cert = castCertificate (cert);

    // Parse the MIME body into an SMIME envelope object
    final SMIMEEnveloped envelope = new SMIMEEnveloped (part);

    // Get the recipient object for decryption
    final RecipientId recId = new JceKeyTransRecipientId (x509Cert);

    final RecipientInformation recipient = envelope.getRecipientInfos ().get (recId);

    if (recipient == null)
      throw new GeneralSecurityException ("Certificate does not match part signature");

    // try to decrypt the data
    final byte [] decryptedData = recipient.getContent (new JceKeyTransEnvelopedRecipient ((PrivateKey) key).setProvider ("BC"));

    return SMIMEUtil.toMimeBodyPart (decryptedData);
  }

  @SuppressWarnings ("deprecation")
  public MimeBodyPart encrypt (final MimeBodyPart part, final Certificate cert, final String algorithm) throws GeneralSecurityException,
                                                                                                       SMIMEException
  {
    final X509Certificate x509Cert = castCertificate (cert);
    final String encAlg = convertAlgorithm (algorithm, true);

    final SMIMEEnvelopedGenerator gen = new SMIMEEnvelopedGenerator ();
    gen.addKeyTransRecipient (x509Cert);
    final MimeBodyPart encData = gen.generate (part, encAlg, "BC");
    return encData;
  }

  @SuppressWarnings ("deprecation")
  public MimeBodyPart sign (final MimeBodyPart part, final Certificate cert, final Key key, final String sAlgorithm) throws GeneralSecurityException,
                                                                                                                    SMIMEException,
                                                                                                                    MessagingException
  {
    final String signDigest = convertAlgorithm (sAlgorithm, true);
    final X509Certificate x509Cert = castCertificate (cert);
    final PrivateKey privKey = castKey (key);

    final SMIMESignedGenerator sGen = new SMIMESignedGenerator ();
    sGen.addSigner (privKey, x509Cert, signDigest);

    final MimeMultipart signedData = sGen.generate (part, "BC");

    final MimeBodyPart tmpBody = new MimeBodyPart ();
    tmpBody.setContent (signedData);
    tmpBody.setHeader ("Content-Type", signedData.getContentType ());

    return tmpBody;
  }

  @SuppressWarnings ({ "unchecked", "deprecation" })
  public MimeBodyPart verify (final MimeBodyPart part, final Certificate cert) throws GeneralSecurityException,
                                                                              IOException,
                                                                              MessagingException,
                                                                              CMSException
  {
    // Make sure the data is signed
    if (!isSigned (part))
      throw new GeneralSecurityException ("Content-Type indicates data isn't signed");

    final X509Certificate x509Cert = castCertificate (cert);
    final MimeMultipart mainParts = (MimeMultipart) part.getContent ();
    final SMIMESigned signedPart = new SMIMESigned (mainParts);

    for (final SignerInformation signer : (Collection <SignerInformation>) signedPart.getSignerInfos ().getSigners ())
      if (!signer.verify (x509Cert, "BC"))
        throw new SignatureException ("Verification failed");

    return signedPart.getContent ();
  }

  @Nonnull
  protected static X509Certificate castCertificate (@Nonnull final Certificate cert) throws GeneralSecurityException
  {
    if (cert == null)
      throw new GeneralSecurityException ("Certificate is null");
    if (!(cert instanceof X509Certificate))
      throw new GeneralSecurityException ("Certificate must be an instance of X509Certificate");
    return (X509Certificate) cert;
  }

  @Nonnull
  protected static PrivateKey castKey (@Nonnull final Key key) throws GeneralSecurityException
  {
    if (key == null)
      throw new GeneralSecurityException ("Key is null");
    if (!(key instanceof PrivateKey))
      throw new GeneralSecurityException ("Key must implement PrivateKey interface");
    return (PrivateKey) key;
  }

  @Nonnull
  protected String convertAlgorithm (@Nonnull final String algorithm, final boolean toBC) throws NoSuchAlgorithmException
  {
    if (algorithm == null)
      throw new NoSuchAlgorithmException ("Algorithm is null");

    if (toBC)
    {
      if (algorithm.equalsIgnoreCase (DIGEST_MD5))
        return SMIMESignedGenerator.DIGEST_MD5;
      if (algorithm.equalsIgnoreCase (DIGEST_SHA1))
        return SMIMESignedGenerator.DIGEST_SHA1;
      if (algorithm.equalsIgnoreCase (CRYPT_3DES))
        return SMIMEEnvelopedGenerator.DES_EDE3_CBC;
      if (algorithm.equalsIgnoreCase (CRYPT_CAST5))
        return SMIMEEnvelopedGenerator.CAST5_CBC;
      if (algorithm.equalsIgnoreCase (CRYPT_IDEA))
        return SMIMEEnvelopedGenerator.IDEA_CBC;
      if (algorithm.equalsIgnoreCase (CRYPT_RC2))
        return SMIMEEnvelopedGenerator.RC2_CBC;
      throw new NoSuchAlgorithmException ("Unknown algorithm: " + algorithm);
    }
    if (algorithm.equalsIgnoreCase (SMIMESignedGenerator.DIGEST_MD5))
      return DIGEST_MD5;
    if (algorithm.equalsIgnoreCase (SMIMESignedGenerator.DIGEST_SHA1))
      return DIGEST_SHA1;
    if (algorithm.equalsIgnoreCase (SMIMEEnvelopedGenerator.CAST5_CBC))
      return CRYPT_CAST5;
    if (algorithm.equalsIgnoreCase (SMIMEEnvelopedGenerator.DES_EDE3_CBC))
      return CRYPT_3DES;
    if (algorithm.equalsIgnoreCase (SMIMEEnvelopedGenerator.IDEA_CBC))
      return CRYPT_IDEA;
    if (algorithm.equalsIgnoreCase (SMIMEEnvelopedGenerator.RC2_CBC))
      return CRYPT_RC2;
    throw new NoSuchAlgorithmException ("Unknown algorithm: " + algorithm);
  }

  protected static InputStream trimCRLFPrefix (@Nonnull final byte [] data)
  {
    final NonBlockingByteArrayInputStream bIn = new NonBlockingByteArrayInputStream (data);

    int scanPos = 0;
    final int len = data.length;

    while (scanPos < (len - 1))
    {
      if (new String (data, scanPos, 2).equals ("\r\n"))
      {
        bIn.read ();
        bIn.read ();
        scanPos += 2;
      }
      else
      {
        return bIn;
      }
    }

    return bIn;
  }

  @Nonnull
  public KeyStore getKeyStore () throws KeyStoreException, NoSuchProviderException
  {
    return KeyStore.getInstance ("PKCS12", "BC");
  }

  @Nonnull
  public KeyStore loadKeyStore (@Nullable final InputStream in, @Nonnull final char [] password) throws Exception
  {
    final KeyStore ks = getKeyStore ();
    if (in != null)
      ks.load (in, password);
    return ks;
  }

  @Nonnull
  public KeyStore loadKeyStore (@Nonnull final String filename, @Nonnull final char [] password) throws Exception
  {
    final FileInputStream fIn = new FileInputStream (filename);
    try
    {
      return loadKeyStore (fIn, password);
    }
    finally
    {
      StreamUtils.close (fIn);
    }
  }
}
