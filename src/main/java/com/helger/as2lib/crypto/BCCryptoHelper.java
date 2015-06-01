/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2015 Philip Helger philip[at]helger[dot]com
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

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PrivilegedAction;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.mail.smime.SMIMESignedParser;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.util.CAS2Header;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.base64.Base64;
import com.helger.commons.collections.CollectionHelper;
import com.helger.commons.io.file.FileUtils;
import com.helger.commons.io.streams.NonBlockingByteArrayInputStream;
import com.helger.commons.io.streams.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.streams.StreamUtils;
import com.helger.commons.priviledged.AccessControllerHelper;

/**
 * Implementation of {@link ICryptoHelper} based on BouncyCastle
 *
 * @author Philip Helger
 */
public final class BCCryptoHelper implements ICryptoHelper
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (BCCryptoHelper.class);

  public BCCryptoHelper ()
  {
    Security.addProvider (new BouncyCastleProvider ());

    final MailcapCommandMap aCommandMap = (MailcapCommandMap) CommandMap.getDefaultCommandMap ();
    aCommandMap.addMailcap ("application/pkcs7-signature;; x-java-content-handler=" +
                            org.bouncycastle.mail.smime.handlers.pkcs7_signature.class.getName ());
    aCommandMap.addMailcap ("application/pkcs7-mime;; x-java-content-handler=" +
                            org.bouncycastle.mail.smime.handlers.pkcs7_mime.class.getName ());
    aCommandMap.addMailcap ("application/x-pkcs7-signature;; x-java-content-handler=" +
                            org.bouncycastle.mail.smime.handlers.x_pkcs7_signature.class.getName ());
    aCommandMap.addMailcap ("application/x-pkcs7-mime;; x-java-content-handler=" +
                            org.bouncycastle.mail.smime.handlers.x_pkcs7_mime.class.getName ());
    aCommandMap.addMailcap ("multipart/signed;; x-java-content-handler=" +
                            org.bouncycastle.mail.smime.handlers.multipart_signed.class.getName ());
    AccessControllerHelper.run (new PrivilegedAction <Object> ()
    {
      public Object run ()
      {
        CommandMap.setDefaultCommandMap (aCommandMap);
        return null;
      }
    });
  }

  @Nonnull
  public KeyStore createNewKeyStore () throws KeyStoreException, NoSuchProviderException
  {
    return KeyStore.getInstance ("PKCS12", BouncyCastleProvider.PROVIDER_NAME);
  }

  @Nonnull
  public KeyStore loadKeyStore (@Nullable final InputStream aIS, @Nonnull final char [] aPassword) throws Exception
  {
    final KeyStore aKeyStore = createNewKeyStore ();
    if (aIS != null)
      aKeyStore.load (aIS, aPassword);
    return aKeyStore;
  }

  @Nonnull
  public KeyStore loadKeyStore (@Nonnull final String sFilename, @Nonnull final char [] aPassword) throws Exception
  {
    final InputStream aIS = FileUtils.getInputStream (sFilename);
    try
    {
      return loadKeyStore (aIS, aPassword);
    }
    finally
    {
      StreamUtils.close (aIS);
    }
  }

  public boolean isEncrypted (@Nonnull final MimeBodyPart aPart) throws MessagingException
  {
    ValueEnforcer.notNull (aPart, "Part");

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
    ValueEnforcer.notNull (aPart, "Part");

    final ContentType aContentType = new ContentType (aPart.getContentType ());
    final String sBaseType = aContentType.getBaseType ().toLowerCase (Locale.US);
    return sBaseType.equals ("multipart/signed");
  }

  /**
   * Remove all leading "\r\n" combinations.
   *
   * @param aData
   *        Byte array to work on.
   * @return An input stream to read from. The leading "\r\n"'s have been
   *         skipped.
   */
  @Nonnull
  private static NonBlockingByteArrayInputStream _trimCRLFPrefix (@Nonnull final byte [] aData)
  {
    final NonBlockingByteArrayInputStream aIS = new NonBlockingByteArrayInputStream (aData);

    int nScanPos = 0;
    final int nLen = aData.length;
    while (nScanPos < nLen - 1)
    {
      if (aData[nScanPos] != '\r' || aData[nScanPos + 1] != '\n')
        break;

      // skip \r\n
      aIS.read ();
      aIS.read ();
      nScanPos += 2;
    }

    return aIS;
  }

  @Nonnull
  public String calculateMIC (@Nonnull final MimeBodyPart aPart,
                              @Nonnull final String sDigestAlgorithm,
                              final boolean bIncludeHeaders) throws GeneralSecurityException,
                                                            MessagingException,
                                                            IOException
  {
    final ASN1ObjectIdentifier aMICAlg = ECryptoAlgorithm.getASN1OIDFromIDOrNull (sDigestAlgorithm);
    if (aMICAlg == null)
      throw new IllegalArgumentException ("Unsupported digest algorithm '" + sDigestAlgorithm + "' provided!");

    final MessageDigest aMessageDigest = MessageDigest.getInstance (aMICAlg.getId (),
                                                                    BouncyCastleProvider.PROVIDER_NAME);

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
    aBAOS.close ();

    // Cut all leading "\r\n"
    final NonBlockingByteArrayInputStream aIS = _trimCRLFPrefix (aData);

    // calculate the hash of the data and mime header
    final DigestInputStream aDigIS = new DigestInputStream (aIS, aMessageDigest);
    final byte [] aBuf = new byte [4096];
    while (aDigIS.read (aBuf) >= 0)
    {}
    aDigIS.close ();

    // Build result digest array
    final byte [] aMIC = aDigIS.getMessageDigest ().digest ();

    // Perform Base64 encoding
    final String sMICString = Base64.encodeBytes (aMIC);

    // Concatenate
    return sMICString + ", " + sDigestAlgorithm;
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
      throw new GeneralSecurityException ("Content-Type indicates data isn't encrypted: " + aPart.getContentType ());

    // Parse the MIME body into an SMIME envelope object
    final SMIMEEnveloped aEnvelope = new SMIMEEnveloped (aPart);

    // Get the recipient object for decryption
    final RecipientId aRecipientID = new JceKeyTransRecipientId (aX509Cert);

    final RecipientInformation aRecipient = aEnvelope.getRecipientInfos ().get (aRecipientID);
    if (aRecipient == null)
      throw new GeneralSecurityException ("Certificate does not match part signature");

    // try to decrypt the data
    final byte [] aDecryptedData = aRecipient.getContent (new JceKeyTransEnvelopedRecipient (aKey).setProvider (BouncyCastleProvider.PROVIDER_NAME));
    return SMIMEUtil.toMimeBodyPart (aDecryptedData);
  }

  @Nonnull
  public MimeBodyPart encrypt (@Nonnull final MimeBodyPart aPart,
                               @Nonnull final X509Certificate aX509Cert,
                               @Nonnull final String sAlgorithm) throws GeneralSecurityException,
                                                                SMIMEException,
                                                                CMSException
  {
    final ASN1ObjectIdentifier aEncAlg = ECryptoAlgorithm.getASN1OIDFromIDOrNull (sAlgorithm);

    final SMIMEEnvelopedGenerator aGen = new SMIMEEnvelopedGenerator ();
    aGen.addRecipientInfoGenerator (new JceKeyTransRecipientInfoGenerator (aX509Cert).setProvider (BouncyCastleProvider.PROVIDER_NAME));

    final OutputEncryptor aEncryptor = new JceCMSContentEncryptorBuilder (aEncAlg).setProvider (BouncyCastleProvider.PROVIDER_NAME)
                                                                                  .build ();
    final MimeBodyPart aEncData = aGen.generate (aPart, aEncryptor);
    return aEncData;
  }

  @Nonnull
  public MimeBodyPart sign (@Nonnull final MimeBodyPart aPart,
                            @Nonnull final X509Certificate aX509Cert,
                            @Nonnull final PrivateKey aPrivKey,
                            @Nonnull final String sAlgorithm) throws GeneralSecurityException,
                                                             SMIMEException,
                                                             MessagingException,
                                                             OperatorCreationException
  {
    // create a CertStore containing the certificates we want carried
    // in the signature
    final List <X509Certificate> aCertList = new ArrayList <X509Certificate> ();
    aCertList.add (aX509Cert);
    final JcaCertStore aCertStore = new JcaCertStore (aCertList);

    // create some smime capabilities in case someone wants to respond
    final ASN1EncodableVector aSignedAttrs = new ASN1EncodableVector ();
    final SMIMECapabilityVector aCapabilities = new SMIMECapabilityVector ();
    aCapabilities.addCapability (ECryptoAlgorithm.getASN1OIDFromIDOrNull (sAlgorithm));
    aSignedAttrs.add (new SMIMECapabilitiesAttribute (aCapabilities));

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
    aSGen.addSignerInfoGenerator (new JcaSimpleSignerInfoGeneratorBuilder ().setProvider (BouncyCastleProvider.PROVIDER_NAME)
                                                                            .setSignedAttributeGenerator (new AttributeTable (aSignedAttrs))
                                                                            .build ("SHA1withRSA", aPrivKey, aX509Cert));

    // add our pool of certs and cerls (if any) to go with the signature
    aSGen.addCertificates (aCertStore);

    final MimeMultipart aSignedData = aSGen.generate (aPart);

    final MimeBodyPart aTmpBody = new MimeBodyPart ();
    aTmpBody.setContent (aSignedData);
    aTmpBody.setHeader (CAS2Header.HEADER_CONTENT_TYPE, aSignedData.getContentType ());
    return aTmpBody;
  }

  @Nonnull
  public MimeBodyPart verify (@Nonnull final MimeBodyPart aPart,
                              @Nullable final X509Certificate aX509Cert,
                              final boolean bAllowCertificateInBodyPart) throws GeneralSecurityException,
                                                                        IOException,
                                                                        MessagingException,
                                                                        CMSException,
                                                                        OperatorCreationException,
                                                                        SMIMEException
  {
    // Make sure the data is signed
    if (!isSigned (aPart))
      throw new GeneralSecurityException ("Content-Type indicates data isn't signed: " + aPart.getContentType ());

    final MimeMultipart aMainPart = (MimeMultipart) aPart.getContent ();
    final SMIMESignedParser aSignedParser = new SMIMESignedParser (new JcaDigestCalculatorProviderBuilder ().build (),
                                                                   aMainPart);

    X509Certificate aRealX509Cert = aX509Cert;
    if (bAllowCertificateInBodyPart)
    {
      // get all certificates contained in the body part
      final Collection <?> aContainedCerts = aSignedParser.getCertificates ().getMatches (null);
      if (!aContainedCerts.isEmpty ())
      {
        // For PEPPOL the certificate is passed in
        if (aContainedCerts.size () > 1)
          s_aLogger.warn ("Signed part contains " + aContainedCerts.size () + " certificates - using the first one!");

        final X509CertificateHolder aCertHolder = ((X509CertificateHolder) CollectionHelper.getFirstElement (aContainedCerts));
        final X509Certificate aCert = new JcaX509CertificateConverter ().setProvider (BouncyCastleProvider.PROVIDER_NAME)
                                                                        .getCertificate (aCertHolder);
        if (aX509Cert != null && !aX509Cert.equals (aCert))
          s_aLogger.warn ("Provided certificate " + aX509Cert + " differs from retrieved certficate: " + aCert);

        aRealX509Cert = aCert;
      }
    }
    if (aRealX509Cert == null)
      throw new GeneralSecurityException ("No certificate provided" +
                                          (bAllowCertificateInBodyPart ? " and none found in the message" : "") +
                                          "!");

    // Verify certificate
    final SignerInformationVerifier aSIV = new JcaSimpleSignerInfoVerifierBuilder ().setProvider (BouncyCastleProvider.PROVIDER_NAME)
                                                                                    .build (aRealX509Cert.getPublicKey ());

    for (final Object aSigner : aSignedParser.getSignerInfos ().getSigners ())
    {
      final SignerInformation aSignerInfo = (SignerInformation) aSigner;
      if (!aSignerInfo.verify (aSIV))
        throw new SignatureException ("Verification failed");
    }

    return aSignedParser.getContent ();
  }
}
