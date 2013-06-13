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

    final MailcapCommandMap aCommandMap = (MailcapCommandMap) CommandMap.getDefaultCommandMap ();
    aCommandMap.addMailcap ("application/pkcs7-signature;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_signature");
    aCommandMap.addMailcap ("application/pkcs7-mime;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.pkcs7_mime");
    aCommandMap.addMailcap ("application/x-pkcs7-signature;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_signature");
    aCommandMap.addMailcap ("application/x-pkcs7-mime;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.x_pkcs7_mime");
    aCommandMap.addMailcap ("multipart/signed;; x-java-content-handler=org.bouncycastle.mail.smime.handlers.multipart_signed");
    CommandMap.setDefaultCommandMap (aCommandMap);
  }

  public boolean isEncrypted (@Nonnull final MimeBodyPart aPart) throws MessagingException
  {
    // Content-Type is sthg like:
    // application/pkcs7-mime; name=smime.p7m; smime-type=enveloped-data
    final ContentType aContentType = new ContentType (aPart.getContentType ());
    final String sBaseType = aContentType.getBaseType ().toLowerCase (Locale.US);
    if (!sBaseType.equals ("application/pkcs7-mime"))
      return false;

    final String sSmimeType = aContentType.getParameter ("smime-type");
    return sSmimeType != null && sSmimeType.equalsIgnoreCase ("enveloped-data");
  }

  public boolean isSigned (final MimeBodyPart aPart) throws MessagingException
  {
    final ContentType aContentType = new ContentType (aPart.getContentType ());
    final String sBaseType = aContentType.getBaseType ().toLowerCase (Locale.US);
    return sBaseType.equals ("multipart/signed");
  }

  public String calculateMIC (final MimeBodyPart aPart, final String sDigest, final boolean bIncludeHeaders) throws GeneralSecurityException,
                                                                                                            MessagingException,
                                                                                                            IOException
  {
    final String sMICAlg = convertAlgorithm (sDigest, true);

    final MessageDigest aMessageDigest = MessageDigest.getInstance (sMICAlg, "BC");

    // convert the Mime data to a byte array, then to an InputStream
    final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();

    if (bIncludeHeaders)
    {
      aPart.writeTo (aBAOS);
    }
    else
    {
      StreamUtils.copyInputStreamToOutputStream (aPart.getInputStream (), aBAOS);
    }

    final byte [] aData = aBAOS.toByteArray ();

    final InputStream bIn = trimCRLFPrefix (aData);

    // calculate the hash of the data and mime header
    final DigestInputStream aDigIS = new DigestInputStream (bIn, aMessageDigest);
    final byte [] aBuf = new byte [4096];
    while (aDigIS.read (aBuf) >= 0)
    {}

    aBAOS.close ();

    final byte [] aMIC = aDigIS.getMessageDigest ().digest ();
    final String sMICString = new String (Base64.encode (aMIC));

    final StringBuilder aMICResult = new StringBuilder (sMICString);
    aMICResult.append (", ").append (sDigest);
    return aMICResult.toString ();
  }

  public MimeBodyPart decrypt (final MimeBodyPart aPart, final Certificate aCert, final Key aKey) throws GeneralSecurityException,
                                                                                                 MessagingException,
                                                                                                 CMSException,
                                                                                                 IOException,
                                                                                                 SMIMEException
  {
    // Make sure the data is encrypted
    if (!isEncrypted (aPart))
      throw new GeneralSecurityException ("Content-Type indicates data isn't encrypted");

    // Cast parameters to what BC needs
    final X509Certificate x509Cert = castCertificate (aCert);

    // Parse the MIME body into an SMIME envelope object
    final SMIMEEnveloped aEnvelope = new SMIMEEnveloped (aPart);

    // Get the recipient object for decryption
    final RecipientId aRecipientID = new JceKeyTransRecipientId (x509Cert);

    final RecipientInformation aRecipient = aEnvelope.getRecipientInfos ().get (aRecipientID);
    if (aRecipient == null)
      throw new GeneralSecurityException ("Certificate does not match part signature");

    // try to decrypt the data
    final byte [] aDecryptedData = aRecipient.getContent (new JceKeyTransEnvelopedRecipient ((PrivateKey) aKey).setProvider ("BC"));
    return SMIMEUtil.toMimeBodyPart (aDecryptedData);
  }

  @SuppressWarnings ("deprecation")
  public MimeBodyPart encrypt (final MimeBodyPart aPart, final Certificate aCert, final String sAlgorithm) throws GeneralSecurityException,
                                                                                                          SMIMEException
  {
    final X509Certificate aX509Cert = castCertificate (aCert);
    final String sEncAlg = convertAlgorithm (sAlgorithm, true);

    final SMIMEEnvelopedGenerator aGen = new SMIMEEnvelopedGenerator ();
    aGen.addKeyTransRecipient (aX509Cert);
    final MimeBodyPart aEncData = aGen.generate (aPart, sEncAlg, "BC");
    return aEncData;
  }

  @SuppressWarnings ("deprecation")
  public MimeBodyPart sign (final MimeBodyPart aPart, final Certificate aCert, final Key aKey, final String sAlgorithm) throws GeneralSecurityException,
                                                                                                                       SMIMEException,
                                                                                                                       MessagingException
  {
    final String sSignDigest = convertAlgorithm (sAlgorithm, true);
    final X509Certificate aX509Cert = castCertificate (aCert);
    final PrivateKey aPrivKey = castKey (aKey);

    final SMIMESignedGenerator aSGen = new SMIMESignedGenerator ();
    aSGen.addSigner (aPrivKey, aX509Cert, sSignDigest);

    final MimeMultipart aSignedData = aSGen.generate (aPart, "BC");

    final MimeBodyPart aTmpBody = new MimeBodyPart ();
    aTmpBody.setContent (aSignedData);
    aTmpBody.setHeader ("Content-Type", aSignedData.getContentType ());

    return aTmpBody;
  }

  @SuppressWarnings ({ "unchecked", "deprecation" })
  public MimeBodyPart verify (final MimeBodyPart aPart, final Certificate aCert) throws GeneralSecurityException,
                                                                                IOException,
                                                                                MessagingException,
                                                                                CMSException
  {
    // Make sure the data is signed
    if (!isSigned (aPart))
      throw new GeneralSecurityException ("Content-Type indicates data isn't signed");

    final X509Certificate aX509Cert = castCertificate (aCert);
    final MimeMultipart aMainParts = (MimeMultipart) aPart.getContent ();
    final SMIMESigned aSignedPart = new SMIMESigned (aMainParts);

    for (final SignerInformation aSignerInfo : (Collection <SignerInformation>) aSignedPart.getSignerInfos ()
                                                                                           .getSigners ())
      if (!aSignerInfo.verify (aX509Cert, "BC"))
        throw new SignatureException ("Verification failed");

    return aSignedPart.getContent ();
  }

  @Nonnull
  protected static X509Certificate castCertificate (@Nonnull final Certificate aCert) throws GeneralSecurityException
  {
    if (aCert == null)
      throw new GeneralSecurityException ("Certificate is null");
    if (!(aCert instanceof X509Certificate))
      throw new GeneralSecurityException ("Certificate must be an instance of X509Certificate");
    return (X509Certificate) aCert;
  }

  @Nonnull
  protected static PrivateKey castKey (@Nonnull final Key aKey) throws GeneralSecurityException
  {
    if (aKey == null)
      throw new GeneralSecurityException ("Key is null");
    if (!(aKey instanceof PrivateKey))
      throw new GeneralSecurityException ("Key must implement PrivateKey interface");
    return (PrivateKey) aKey;
  }

  @Nonnull
  protected String convertAlgorithm (@Nonnull final String sAlgorithm, final boolean bToBC) throws NoSuchAlgorithmException
  {
    if (sAlgorithm == null)
      throw new NoSuchAlgorithmException ("Algorithm is null");

    if (bToBC)
    {
      if (sAlgorithm.equalsIgnoreCase (DIGEST_MD5))
        return SMIMESignedGenerator.DIGEST_MD5;
      if (sAlgorithm.equalsIgnoreCase (DIGEST_SHA1))
        return SMIMESignedGenerator.DIGEST_SHA1;
      if (sAlgorithm.equalsIgnoreCase (CRYPT_3DES))
        return SMIMEEnvelopedGenerator.DES_EDE3_CBC;
      if (sAlgorithm.equalsIgnoreCase (CRYPT_CAST5))
        return SMIMEEnvelopedGenerator.CAST5_CBC;
      if (sAlgorithm.equalsIgnoreCase (CRYPT_IDEA))
        return SMIMEEnvelopedGenerator.IDEA_CBC;
      if (sAlgorithm.equalsIgnoreCase (CRYPT_RC2))
        return SMIMEEnvelopedGenerator.RC2_CBC;
      throw new NoSuchAlgorithmException ("Unknown algorithm: " + sAlgorithm);
    }

    if (sAlgorithm.equalsIgnoreCase (SMIMESignedGenerator.DIGEST_MD5))
      return DIGEST_MD5;
    if (sAlgorithm.equalsIgnoreCase (SMIMESignedGenerator.DIGEST_SHA1))
      return DIGEST_SHA1;
    if (sAlgorithm.equalsIgnoreCase (SMIMEEnvelopedGenerator.CAST5_CBC))
      return CRYPT_CAST5;
    if (sAlgorithm.equalsIgnoreCase (SMIMEEnvelopedGenerator.DES_EDE3_CBC))
      return CRYPT_3DES;
    if (sAlgorithm.equalsIgnoreCase (SMIMEEnvelopedGenerator.IDEA_CBC))
      return CRYPT_IDEA;
    if (sAlgorithm.equalsIgnoreCase (SMIMEEnvelopedGenerator.RC2_CBC))
      return CRYPT_RC2;
    throw new NoSuchAlgorithmException ("Unknown algorithm: " + sAlgorithm);
  }

  @Nonnull
  protected static InputStream trimCRLFPrefix (@Nonnull final byte [] aData)
  {
    final NonBlockingByteArrayInputStream aIS = new NonBlockingByteArrayInputStream (aData);

    int nScanPos = 0;
    final int nLen = aData.length;
    while (nScanPos < (nLen - 1))
    {
      if (!new String (aData, nScanPos, 2).equals ("\r\n"))
        break;

      // skip \r\n
      aIS.read ();
      aIS.read ();
      nScanPos += 2;
    }

    return aIS;
  }

  @Nonnull
  public KeyStore getKeyStore () throws KeyStoreException, NoSuchProviderException
  {
    return KeyStore.getInstance ("PKCS12", "BC");
  }

  @Nonnull
  public KeyStore loadKeyStore (@Nullable final InputStream aIS, @Nonnull final char [] aPassword) throws Exception
  {
    final KeyStore aKeyStore = getKeyStore ();
    if (aIS != null)
      aKeyStore.load (aIS, aPassword);
    return aKeyStore;
  }

  @Nonnull
  public KeyStore loadKeyStore (@Nonnull final String sFilename, @Nonnull final char [] aPassword) throws Exception
  {
    final FileInputStream aFIS = new FileInputStream (sFilename);
    try
    {
      return loadKeyStore (aFIS, aPassword);
    }
    finally
    {
      StreamUtils.close (aFIS);
    }
  }
}
