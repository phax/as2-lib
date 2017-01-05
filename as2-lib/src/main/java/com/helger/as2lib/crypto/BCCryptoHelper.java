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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

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
import org.bouncycastle.cms.SignerId;
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

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.IOHelper;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.base64.Base64;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.stream.NullOutputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.lang.priviledged.AccessControllerHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.system.SystemProperties;
import com.helger.mail.cte.EContentTransferEncoding;

/**
 * Implementation of {@link ICryptoHelper} based on BouncyCastle.
 *
 * @author Philip Helger
 */
public final class BCCryptoHelper implements ICryptoHelper
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (BCCryptoHelper.class);
  private static final File s_aDumpDecryptedDirectory;

  static
  {
    final String sDumpDecryptedDirectory = SystemProperties.getPropertyValueOrNull ("AS2.dumpDecryptedDirectory");
    if (StringHelper.hasText (sDumpDecryptedDirectory))
    {
      s_aDumpDecryptedDirectory = new File (sDumpDecryptedDirectory);
      IOHelper.getFileOperationManager ().createDirIfNotExisting (s_aDumpDecryptedDirectory);
      s_aLogger.info ("Using directory " +
                      s_aDumpDecryptedDirectory.getAbsolutePath () +
                      " to dump all decrypted body parts to.");
    }
    else
      s_aDumpDecryptedDirectory = null;
  }

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
    AccessControllerHelper.run ( () -> {
      CommandMap.setDefaultCommandMap (aCommandMap);
      return null;
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
  @Deprecated
  public KeyStore loadKeyStore (@Nonnull final String sFilename, @Nonnull final char [] aPassword) throws Exception
  {
    final InputStream aIS = FileHelper.getInputStream (sFilename);
    try
    {
      return loadKeyStore (aIS, aPassword);
    }
    finally
    {
      StreamHelper.close (aIS);
    }
  }

  public boolean isEncrypted (@Nonnull final MimeBodyPart aPart) throws MessagingException
  {
    ValueEnforcer.notNull (aPart, "Part");

    // Content-Type is sthg like this if encrypted:
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
    final String sBaseType = aContentType.getBaseType ();
    return sBaseType.equalsIgnoreCase ("multipart/signed");
  }

  public boolean isCompressed (@Nonnull final String sContentType) throws OpenAS2Exception
  {
    ValueEnforcer.notNull (sContentType, "ContentType");

    try
    {
      // Content-Type is sthg like this if compressed:
      // application/pkcs7-mime; smime-type=compressed-data; name=smime.p7z
      final ContentType aContentType = new ContentType (sContentType);

      final String sSmimeType = aContentType.getParameter ("smime-type");
      return sSmimeType != null && sSmimeType.equalsIgnoreCase ("compressed-data");
    }
    catch (final MessagingException ex)
    {
      throw WrappedOpenAS2Exception.wrap (ex);
    }
  }

  @Nonnull
  @ReturnsMutableCopy
  private static byte [] _getAsciiBytes (@Nonnull final String sString)
  {
    final char [] aChars = sString.toCharArray ();
    final int nLength = aChars.length;
    final byte [] ret = new byte [nLength];
    for (int i = 0; i < nLength; i++)
      ret[i] = (byte) aChars[i];
    return ret;
  }

  @Nonnull
  public String calculateMIC (@Nonnull final MimeBodyPart aPart,
                              @Nonnull final ECryptoAlgorithmSign eDigestAlgorithm,
                              final boolean bIncludeHeaders) throws GeneralSecurityException,
                                                             MessagingException,
                                                             IOException
  {
    ValueEnforcer.notNull (aPart, "MimeBodyPart");
    ValueEnforcer.notNull (eDigestAlgorithm, "DigestAlgorithm");

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("BCCryptoHelper.calculateMIC (" +
                       eDigestAlgorithm +
                       " [" +
                       eDigestAlgorithm.getOID ().getId () +
                       "], " +
                       bIncludeHeaders +
                       ")");

    final ASN1ObjectIdentifier aMICAlg = eDigestAlgorithm.getOID ();

    final MessageDigest aMessageDigest = MessageDigest.getInstance (aMICAlg.getId (),
                                                                    BouncyCastleProvider.PROVIDER_NAME);

    if (bIncludeHeaders)
    {
      // Start hashing the header
      final byte [] aCRLF = new byte [] { '\r', '\n' };
      final Enumeration <?> aHeaderLines = aPart.getAllHeaderLines ();
      while (aHeaderLines.hasMoreElements ())
      {
        aMessageDigest.update (_getAsciiBytes ((String) aHeaderLines.nextElement ()));
        aMessageDigest.update (aCRLF);
      }

      // The CRLF separator between header and content
      aMessageDigest.update (aCRLF);
    }

    // No need to canonicalize here - see issue #12
    try (final DigestOutputStream aDOS = new DigestOutputStream (new NullOutputStream (), aMessageDigest);
         final OutputStream aOS = MimeUtility.encode (aDOS, aPart.getEncoding ()))
    {
      aPart.getDataHandler ().writeTo (aOS);
    }

    // Build result digest array
    final byte [] aMIC = aMessageDigest.digest ();

    // Perform Base64 encoding and append algorithm ID
    final String ret = Base64.encodeBytes (aMIC) + ", " + eDigestAlgorithm.getID ();

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("  MIC = " + ret);

    return ret;
  }

  private static void _dumpDecrypted (@Nonnull final byte [] aPayload)
  {
    // Ensure a unique filename
    File aDestinationFile;
    int nIndex = 0;
    do
    {
      aDestinationFile = new File (s_aDumpDecryptedDirectory,
                                   "as2-decrypted-" + Long.toString (new Date ().getTime ()) + "-" + nIndex + ".part");
      nIndex++;
    } while (aDestinationFile.exists ());

    s_aLogger.info ("Dumping decrypted MIME part to file " + aDestinationFile.getAbsolutePath ());
    final OutputStream aOS = FileHelper.getOutputStream (aDestinationFile);
    try
    {
      // Add payload
      aOS.write (aPayload);
    }
    catch (final IOException ex)
    {
      s_aLogger.error ("Failed to dump decrypted MIME part to file " + aDestinationFile.getAbsolutePath (), ex);
    }
    finally
    {
      StreamHelper.close (aOS);
    }
  }

  @Nonnull
  public MimeBodyPart decrypt (@Nonnull final MimeBodyPart aPart,
                               @Nonnull final X509Certificate aX509Cert,
                               @Nonnull final PrivateKey aPrivateKey,
                               final boolean bForceDecrypt) throws GeneralSecurityException,
                                                            MessagingException,
                                                            CMSException,
                                                            SMIMEException
  {
    ValueEnforcer.notNull (aPart, "MimeBodyPart");
    ValueEnforcer.notNull (aX509Cert, "X509Cert");
    ValueEnforcer.notNull (aPrivateKey, "PrivateKey");

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("BCCryptoHelper.decrypt; X509 subject=" +
                       aX509Cert.getSubjectX500Principal ().getName () +
                       "; forceDecrypt=" +
                       bForceDecrypt);

    // Make sure the data is encrypted
    if (!bForceDecrypt && !isEncrypted (aPart))
      throw new GeneralSecurityException ("Content-Type indicates data isn't encrypted: " + aPart.getContentType ());

    // Parse the MIME body into an SMIME envelope object
    final SMIMEEnveloped aEnvelope = new SMIMEEnveloped (aPart);

    // Get the recipient object for decryption
    final RecipientId aRecipientID = new JceKeyTransRecipientId (aX509Cert);

    final RecipientInformation aRecipient = aEnvelope.getRecipientInfos ().get (aRecipientID);
    if (aRecipient == null)
      throw new GeneralSecurityException ("Certificate does not match part signature");

    // try to decrypt the data
    final byte [] aDecryptedData = aRecipient.getContent (new JceKeyTransEnvelopedRecipient (aPrivateKey).setProvider (BouncyCastleProvider.PROVIDER_NAME));

    if (s_aDumpDecryptedDirectory != null)
      _dumpDecrypted (aDecryptedData);

    return SMIMEUtil.toMimeBodyPart (aDecryptedData);
  }

  @Nonnull
  public MimeBodyPart encrypt (@Nonnull final MimeBodyPart aPart,
                               @Nonnull final X509Certificate aX509Cert,
                               @Nonnull final ECryptoAlgorithmCrypt eAlgorithm) throws GeneralSecurityException,
                                                                                SMIMEException,
                                                                                CMSException
  {
    ValueEnforcer.notNull (aPart, "MimeBodyPart");
    ValueEnforcer.notNull (aX509Cert, "X509Cert");
    ValueEnforcer.notNull (eAlgorithm, "Algorithm");

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("BCCryptoHelper.encrypt; X509 subject=" +
                       aX509Cert.getSubjectX500Principal ().getName () +
                       "; algorithm=" +
                       eAlgorithm);

    // Check if the certificate is expired or active.
    aX509Cert.checkValidity ();

    final ASN1ObjectIdentifier aEncAlg = eAlgorithm.getOID ();

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
                            @Nonnull final PrivateKey aPrivateKey,
                            @Nonnull final ECryptoAlgorithmSign eAlgorithm,
                            final boolean bIncludeCertificateInSignedContent,
                            final boolean bUseOldRFC3851MicAlgs) throws GeneralSecurityException,
                                                                 SMIMEException,
                                                                 MessagingException,
                                                                 OperatorCreationException
  {
    ValueEnforcer.notNull (aPart, "MimeBodyPart");
    ValueEnforcer.notNull (aX509Cert, "X509Cert");
    ValueEnforcer.notNull (aPrivateKey, "PrivateKey");
    ValueEnforcer.notNull (eAlgorithm, "Algorithm");

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("BCCryptoHelper.sign; X509 subject=" +
                       aX509Cert.getSubjectX500Principal ().getName () +
                       "; algorithm=" +
                       eAlgorithm +
                       "; includeCertificateInSignedContent=" +
                       bIncludeCertificateInSignedContent);

    // Check if the certificate is expired or active.
    aX509Cert.checkValidity ();

    // create a CertStore containing the certificates we want carried
    // in the signature
    final ICommonsList <X509Certificate> aCertList = new CommonsArrayList<> (aX509Cert);
    final JcaCertStore aCertStore = new JcaCertStore (aCertList);

    // create some smime capabilities in case someone wants to respond
    final ASN1EncodableVector aSignedAttrs = new ASN1EncodableVector ();
    final SMIMECapabilityVector aCapabilities = new SMIMECapabilityVector ();
    aCapabilities.addCapability (eAlgorithm.getOID ());
    aSignedAttrs.add (new SMIMECapabilitiesAttribute (aCapabilities));

    // add an encryption key preference for encrypted responses -
    // normally this would be different from the signing certificate...
    // final IssuerAndSerialNumber issAndSer = new IssuerAndSerialNumber (new
    // X500Name (signDN),
    // aX509Cert.getSerialNumber ());
    // aSignedAttrs.add (new SMIMEEncryptionKeyPreferenceAttribute (issAndSer));

    // create the generator for creating an smime/signed message
    final SMIMESignedGenerator aSGen = new SMIMESignedGenerator (bUseOldRFC3851MicAlgs ? SMIMESignedGenerator.RFC3851_MICALGS
                                                                                       : SMIMESignedGenerator.RFC5751_MICALGS);

    // aSGen.addSigner (aPrivKey, aX509Cert, aSignDigest.getId ());

    // add a signer to the generator - this specifies we are using SHA1 and
    // adding the smime attributes above to the signed attributes that
    // will be generated as part of the signature. The encryption algorithm
    // used is taken from the key - in this RSA with PKCS1Padding
    aSGen.addSignerInfoGenerator (new JcaSimpleSignerInfoGeneratorBuilder ().setProvider (BouncyCastleProvider.PROVIDER_NAME)
                                                                            .setSignedAttributeGenerator (new AttributeTable (aSignedAttrs))
                                                                            .build (eAlgorithm.getSignAlgorithmName (),
                                                                                    aPrivateKey,
                                                                                    aX509Cert));

    if (bIncludeCertificateInSignedContent)
    {
      // add our pool of certs and cerls (if any) to go with the signature
      aSGen.addCertificates (aCertStore);
    }

    final MimeMultipart aSignedData = aSGen.generate (aPart);

    final MimeBodyPart aTmpBody = new MimeBodyPart ();
    aTmpBody.setContent (aSignedData);
    aTmpBody.setHeader (CAS2Header.HEADER_CONTENT_TYPE, aSignedData.getContentType ());
    return aTmpBody;
  }

  @Nonnull
  private X509Certificate _verifyFindCertificate (@Nullable final X509Certificate aX509Cert,
                                                  final boolean bUseCertificateInBodyPart,
                                                  @Nonnull final SMIMESignedParser aSignedParser) throws CMSException,
                                                                                                  CertificateException,
                                                                                                  GeneralSecurityException
  {
    X509Certificate aRealX509Cert = aX509Cert;
    if (bUseCertificateInBodyPart)
    {
      // get signing certificates contained in the body part
      SignerId aSignerID = null;
      final Collection <SignerInformation> aSignerCerts = aSignedParser.getSignerInfos ().getSigners ();
      final Iterator <SignerInformation> aSignerCertIterator = aSignerCerts.iterator ();
      if (aSignerCertIterator.hasNext ())
      {
        final SignerInformation aSigner = aSignerCertIterator.next ();
        aSignerID = aSigner.getSID ();
      }
      final Collection <?> aContainedCerts = aSignedParser.getCertificates ().getMatches (aSignerID);
      if (!aContainedCerts.isEmpty ())
      {
        // For PEPPOL the certificate is passed in
        if (aContainedCerts.size () > 1)
          s_aLogger.warn ("Signed part contains " + aContainedCerts.size () + " certificates - using the first one!");

        final X509CertificateHolder aCertHolder = ((X509CertificateHolder) CollectionHelper.getFirstElement (aContainedCerts));
        final X509Certificate aCert = new JcaX509CertificateConverter ().setProvider (BouncyCastleProvider.PROVIDER_NAME)
                                                                        .getCertificate (aCertHolder);
        if (aX509Cert != null && !aX509Cert.equals (aCert))
          s_aLogger.warn ("Certificate mismatch! Provided certificate\n" +
                          aX509Cert +
                          " differs from certficate contained in message\n" +
                          aCert);

        aRealX509Cert = aCert;
      }
    }
    if (aRealX509Cert == null)
      throw new GeneralSecurityException ("No certificate provided" +
                                          (bUseCertificateInBodyPart ? " and none found in the message" : "") +
                                          "!");
    return aRealX509Cert;
  }

  @Nonnull
  public MimeBodyPart verify (@Nonnull final MimeBodyPart aPart,
                              @Nullable final X509Certificate aX509Cert,
                              final boolean bUseCertificateInBodyPart,
                              final boolean bForceVerify) throws GeneralSecurityException,
                                                          IOException,
                                                          MessagingException,
                                                          CMSException,
                                                          OperatorCreationException
  {
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("BCCryptoHelper.verify; X509 subject=" +
                       (aX509Cert == null ? "null" : aX509Cert.getSubjectX500Principal ().getName ()) +
                       "; useCertificateInBodyPart=" +
                       bUseCertificateInBodyPart +
                       "; forceVerify=" +
                       bForceVerify);

    // Make sure the data is signed
    if (!bForceVerify && !isSigned (aPart))
      throw new GeneralSecurityException ("Content-Type indicates data isn't signed: " + aPart.getContentType ());

    final MimeMultipart aMainPart = (MimeMultipart) aPart.getContent ();
    // SMIMESignedParser uses "7bit" as the default - AS2 wants "binary"
    final SMIMESignedParser aSignedParser = new SMIMESignedParser (new JcaDigestCalculatorProviderBuilder ().setProvider (BouncyCastleProvider.PROVIDER_NAME)
                                                                                                            .build (),
                                                                   aMainPart,
                                                                   EContentTransferEncoding.AS2_DEFAULT.getID ());

    final X509Certificate aRealX509Cert = _verifyFindCertificate (aX509Cert, bUseCertificateInBodyPart, aSignedParser);

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug (aRealX509Cert == aX509Cert ? "Verifying signature using the provided certificate (partnership)"
                                                  : "Verifying signature using the certificate contained in the MIME body part");

    // Check if the certificate is expired or active.
    aRealX509Cert.checkValidity ();

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
