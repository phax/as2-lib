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
package com.helger.as2lib.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.SignatureException;
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

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.util.encoders.Base64;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.streams.NonBlockingByteArrayInputStream;
import com.helger.commons.io.streams.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.streams.StreamUtils;

public final class BCCryptoHelper implements ICryptoHelper
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

  public boolean isSigned (@Nonnull final MimeBodyPart aPart) throws MessagingException
  {
    final ContentType aContentType = new ContentType (aPart.getContentType ());
    final String sBaseType = aContentType.getBaseType ().toLowerCase (Locale.US);
    return sBaseType.equals ("multipart/signed");
  }

  @Nonnull
  public String calculateMIC (@Nonnull final MimeBodyPart aPart,
                              @Nonnull final String sDigest,
                              final boolean bIncludeHeaders) throws GeneralSecurityException,
                                                            MessagingException,
                                                            IOException
  {
    final ASN1ObjectIdentifier aMICAlg = _convertAlgorithmToBC (sDigest);

    final MessageDigest aMessageDigest = MessageDigest.getInstance (aMICAlg.getId (), "BC");

    // convert the Mime data to a byte array, then to an InputStream
    final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();
    if (bIncludeHeaders)
    {
      aPart.writeTo (aBAOS);
    }
    else
    {
      // Only the "content" of the part
      StreamUtils.copyInputStreamToOutputStream (aPart.getInputStream (), aBAOS);
    }

    final byte [] aData = aBAOS.toByteArray ();

    final InputStream bIn = _trimCRLFPrefix (aData);

    // calculate the hash of the data and mime header
    final DigestInputStream aDigIS = new DigestInputStream (bIn, aMessageDigest);
    final byte [] aBuf = new byte [4096];
    while (aDigIS.read (aBuf) >= 0)
    {}

    aBAOS.close ();

    final byte [] aMIC = aDigIS.getMessageDigest ().digest ();
    final String sMICString = new String (Base64.encode (aMIC));

    return sMICString + ", " + sDigest;
  }

  @Nonnull
  public MimeBodyPart decrypt (@Nonnull final MimeBodyPart aPart,
                               @Nonnull final X509Certificate aX509Cert,
                               @Nonnull final PrivateKey aKey) throws GeneralSecurityException,
                                                              MessagingException,
                                                              CMSException,
                                                              IOException,
                                                              SMIMEException
  {
    // Make sure the data is encrypted
    if (!isEncrypted (aPart))
      throw new GeneralSecurityException ("Content-Type indicates data isn't encrypted");

    // Parse the MIME body into an SMIME envelope object
    final SMIMEEnveloped aEnvelope = new SMIMEEnveloped (aPart);

    // Get the recipient object for decryption
    final RecipientId aRecipientID = new JceKeyTransRecipientId (aX509Cert);

    final RecipientInformation aRecipient = aEnvelope.getRecipientInfos ().get (aRecipientID);
    if (aRecipient == null)
      throw new GeneralSecurityException ("Certificate does not match part signature");

    // try to decrypt the data
    final byte [] aDecryptedData = aRecipient.getContent (new JceKeyTransEnvelopedRecipient (aKey).setProvider ("BC"));
    return SMIMEUtil.toMimeBodyPart (aDecryptedData);
  }

  @SuppressWarnings ("deprecation")
  @Nonnull
  public MimeBodyPart encrypt (@Nonnull final MimeBodyPart aPart,
                               @Nonnull final X509Certificate aX509Cert,
                               @Nonnull final String sAlgorithm) throws GeneralSecurityException,
                                                                SMIMEException,
                                                                CMSException
  {
    final ASN1ObjectIdentifier aEncAlg = _convertAlgorithmToBC (sAlgorithm);

    final SMIMEEnvelopedGenerator aGen = new SMIMEEnvelopedGenerator ();
    aGen.addRecipientInfoGenerator (new JceKeyTransRecipientInfoGenerator (aX509Cert).setProvider ("BC"));

    final OutputEncryptor aEncryptor = new JceCMSContentEncryptorBuilder (aEncAlg).setProvider ("BC").build ();
    final MimeBodyPart aEncData = aGen.generate (aPart, aEncryptor);
    return aEncData;
  }

  @SuppressWarnings ("deprecation")
  @Nonnull
  public MimeBodyPart sign (@Nonnull final MimeBodyPart aPart,
                            @Nonnull final X509Certificate aX509Cert,
                            @Nonnull final PrivateKey aPrivKey,
                            @Nonnull final String sAlgorithm) throws GeneralSecurityException,
                                                             SMIMEException,
                                                             MessagingException,
                                                             OperatorCreationException
  {
    final ASN1ObjectIdentifier aSignDigest = _convertAlgorithmToBC (sAlgorithm);

    //
    // create some smime capabilities in case someone wants to respond
    //
    final ASN1EncodableVector aSignedAttrs = new ASN1EncodableVector ();
    final SMIMECapabilityVector caps = new SMIMECapabilityVector ();
    caps.addCapability (aSignDigest);
    aSignedAttrs.add (new SMIMECapabilitiesAttribute (caps));

    // add an encryption key preference for encrypted responses -
    // normally this would be different from the signing certificate...
    // final IssuerAndSerialNumber issAndSer = new IssuerAndSerialNumber (new
    // X500Name (signDN),
    // aX509Cert.getSerialNumber ());
    // aSignedAttrs.add (new SMIMEEncryptionKeyPreferenceAttribute (issAndSer));

    // create the generator for creating an smime/signed message
    final SMIMESignedGenerator aSGen = new SMIMESignedGenerator ();
    // aSGen.addSigner (aPrivKey, aX509Cert, aSignDigest.getId ());

    // add a signer to the generator - this specifies we are using SHA1 and
    // adding the smime attributes above to the signed attributes that
    // will be generated as part of the signature. The encryption algorithm
    // used is taken from the key - in this RSA with PKCS1Padding
    aSGen.addSignerInfoGenerator (new JcaSimpleSignerInfoGeneratorBuilder ().setProvider ("BC")
                                                                            .setSignedAttributeGenerator (new AttributeTable (aSignedAttrs))
                                                                            .build ("SHA1withRSA", aPrivKey, aX509Cert));

    // add our pool of certs and cerls (if any) to go with the signature
    // aSGen.addCertificates (certs);

    final MimeMultipart aSignedData = aSGen.generate (aPart);

    final MimeBodyPart aTmpBody = new MimeBodyPart ();
    aTmpBody.setContent (aSignedData);
    aTmpBody.setHeader (CAS2Header.HEADER_CONTENT_TYPE, aSignedData.getContentType ());
    return aTmpBody;
  }

  @SuppressWarnings ({ "unchecked", "deprecation" })
  public MimeBodyPart verify (@Nonnull final MimeBodyPart aPart, final X509Certificate aX509Cert) throws GeneralSecurityException,
                                                                                                 IOException,
                                                                                                 MessagingException,
                                                                                                 CMSException
  {
    // Make sure the data is signed
    if (!isSigned (aPart))
      throw new GeneralSecurityException ("Content-Type indicates data isn't signed");

    final MimeMultipart aMainParts = (MimeMultipart) aPart.getContent ();
    final SMIMESigned aSignedPart = new SMIMESigned (aMainParts);

    for (final SignerInformation aSignerInfo : (Collection <SignerInformation>) aSignedPart.getSignerInfos ()
                                                                                           .getSigners ())
      if (!aSignerInfo.verify (aX509Cert, "BC"))
        throw new SignatureException ("Verification failed");

    return aSignedPart.getContent ();
  }

  @Nonnull
  private static ASN1ObjectIdentifier _convertAlgorithmToBC (@Nonnull final String sAlgorithm) throws NoSuchAlgorithmException
  {
    ValueEnforcer.notNull (sAlgorithm, "Algorithm");

    if (sAlgorithm.equalsIgnoreCase (DIGEST_MD5))
      return PKCSObjectIdentifiers.md5;
    if (sAlgorithm.equalsIgnoreCase (DIGEST_SHA1))
      return OIWObjectIdentifiers.idSHA1;
    if (sAlgorithm.equalsIgnoreCase (CRYPT_3DES))
      return PKCSObjectIdentifiers.des_EDE3_CBC;
    if (sAlgorithm.equalsIgnoreCase (CRYPT_CAST5))
      return CMSAlgorithm.CAST5_CBC;
    if (sAlgorithm.equalsIgnoreCase (CRYPT_IDEA))
      return CMSAlgorithm.IDEA_CBC;
    if (sAlgorithm.equalsIgnoreCase (CRYPT_RC2))
      return PKCSObjectIdentifiers.RC2_CBC;
    throw new NoSuchAlgorithmException ("Unknown algorithm to BC: " + sAlgorithm);
  }

  @Nonnull
  private static InputStream _trimCRLFPrefix (@Nonnull final byte [] aData)
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
