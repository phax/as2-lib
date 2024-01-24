/*
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2024 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.session;

import java.net.Proxy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.as2lib.IDynamicComponent;
import com.helger.as2lib.cert.ICertificateFactory;
import com.helger.as2lib.partner.IPartnershipFactory;
import com.helger.as2lib.processor.IMessageProcessor;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.javamail.DispositionDataContentHandler;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.lang.priviledged.AccessControllerHelper;
import com.helger.commons.string.ToStringGenerator;

import jakarta.activation.CommandMap;
import jakarta.activation.MailcapCommandMap;

/**
 * Default implementation of {@link IAS2Session}
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class AS2Session implements IAS2Session
{
  public static final String COMPONENT_ID_CERTIFICATE_FACTORY = "certificatefactory";
  public static final String COMPONENT_ID_PARTNERSHIP_FACTORY = "partnershipfactory";
  public static final String COMPONENT_ID_MESSAGE_PROCESSOR = "message-processor";

  public static final boolean DEFAULT_CRYPTO_SIGN_INCLUDE_CERTIFICATE_IN_BODY_PART = true;
  public static final boolean DEFAULT_CRYPTO_VERIFY_USE_CERTIFICATE_IN_BODY_PART = true;

  private final ICommonsMap <String, IDynamicComponent> m_aComponents = new CommonsHashMap <> ();
  private boolean m_bCryptoSignIncludeCertificateInBodyPart = DEFAULT_CRYPTO_SIGN_INCLUDE_CERTIFICATE_IN_BODY_PART;
  private boolean m_bCryptoVerifyUseCertificateInBodyPart = DEFAULT_CRYPTO_VERIFY_USE_CERTIFICATE_IN_BODY_PART;
  private Proxy m_aHttpProxy;
  private String m_sAS2VersionID = CAS2Header.DEFAULT_AS2_VERSION;

  public static void makeAS2CommandMapChanges ()
  {
    /*
     * Adds a group of content handlers to the Mailcap <code>CommandMap</code>.
     * These handlers are used by the JavaMail API to encode and decode
     * information of specific mime types.
     */
    final MailcapCommandMap aCommandMap = (MailcapCommandMap) CommandMap.getDefaultCommandMap ();
    aCommandMap.addMailcap ("message/disposition-notification;; x-java-content-handler=" + DispositionDataContentHandler.class.getName ());
    AccessControllerHelper.run ( () -> {
      CommandMap.setDefaultCommandMap (aCommandMap);
      return null;
    });
  }

  /**
   * Constructor
   */
  public AS2Session ()
  {
    makeAS2CommandMapChanges ();
  }

  public final void addComponent (@Nonnull @Nonempty final String sComponentID,
                                  @Nonnull final IDynamicComponent aComponent) throws AS2ComponentDuplicateException
  {
    ValueEnforcer.notEmpty (sComponentID, "ComponentID");
    ValueEnforcer.notNull (aComponent, "Component");
    if (m_aComponents.containsKey (sComponentID))
      throw new AS2ComponentDuplicateException (sComponentID);
    m_aComponents.put (sComponentID, aComponent);
  }

  public void setCertificateFactory (@Nonnull final ICertificateFactory aCertFactory) throws AS2ComponentDuplicateException
  {
    addComponent (COMPONENT_ID_CERTIFICATE_FACTORY, aCertFactory);
  }

  public void setPartnershipFactory (@Nonnull final IPartnershipFactory aPartnershipFactory) throws AS2ComponentDuplicateException
  {
    addComponent (COMPONENT_ID_PARTNERSHIP_FACTORY, aPartnershipFactory);
  }

  public void setMessageProcessor (@Nonnull final IMessageProcessor aMsgProcessor) throws AS2ComponentDuplicateException
  {
    addComponent (COMPONENT_ID_MESSAGE_PROCESSOR, aMsgProcessor);
  }

  @Nonnull
  public final IDynamicComponent getComponent (@Nonnull @Nonempty final String sComponentID) throws AS2ComponentNotFoundException
  {
    ValueEnforcer.notEmpty (sComponentID, "ComponentID");
    final IDynamicComponent aComponent = m_aComponents.get (sComponentID);
    if (aComponent == null)
      throw new AS2ComponentNotFoundException (sComponentID);
    return aComponent;
  }

  /**
   * All modifications done here, have impact on the whole session
   *
   * @return The mutual component map. Never <code>null</code> handle with care.
   */
  @Nonnull
  @ReturnsMutableObject
  protected final ICommonsMap <String, IDynamicComponent> components ()
  {
    return m_aComponents;
  }

  @Nonnull
  @ReturnsMutableCopy
  public final ICommonsMap <String, IDynamicComponent> getAllComponents ()
  {
    return m_aComponents.getClone ();
  }

  @Nonnull
  public final ICertificateFactory getCertificateFactory () throws AS2ComponentNotFoundException
  {
    return (ICertificateFactory) getComponent (COMPONENT_ID_CERTIFICATE_FACTORY);
  }

  @Nonnull
  public final IPartnershipFactory getPartnershipFactory () throws AS2ComponentNotFoundException
  {
    return (IPartnershipFactory) getComponent (COMPONENT_ID_PARTNERSHIP_FACTORY);
  }

  @Nonnull
  public final IMessageProcessor getMessageProcessor () throws AS2ComponentNotFoundException
  {
    return (IMessageProcessor) getComponent (COMPONENT_ID_MESSAGE_PROCESSOR);
  }

  public final boolean isCryptoSignIncludeCertificateInBodyPart ()
  {
    return m_bCryptoSignIncludeCertificateInBodyPart;
  }

  public final void setCryptoSignIncludeCertificateInBodyPart (final boolean bCryptoSignIncludeCertificateInBodyPart)
  {
    m_bCryptoSignIncludeCertificateInBodyPart = bCryptoSignIncludeCertificateInBodyPart;
  }

  public final boolean isCryptoVerifyUseCertificateInBodyPart ()
  {
    return m_bCryptoVerifyUseCertificateInBodyPart;
  }

  public final void setCryptoVerifyUseCertificateInBodyPart (final boolean bCryptoVerifyUseCertificateInBodyPart)
  {
    m_bCryptoVerifyUseCertificateInBodyPart = bCryptoVerifyUseCertificateInBodyPart;
  }

  @Nullable
  public final Proxy getHttpProxy ()
  {
    return m_aHttpProxy;
  }

  public final void setHttpProxy (@Nullable final Proxy aHttpProxy)
  {
    m_aHttpProxy = aHttpProxy;
  }

  @Nonnull
  @Nonempty
  public final String getAS2VersionID ()
  {
    return m_sAS2VersionID;
  }

  public final void setAS2VersionID (@Nonnull @Nonempty final String sAS2Version)
  {
    ValueEnforcer.notEmpty (sAS2Version, "AS2Version");
    m_sAS2VersionID = sAS2Version;
  }

  @OverridingMethodsMustInvokeSuper
  public void resetToDefault ()
  {
    // Clear old stuff
    components ().clear ();
    setCryptoSignIncludeCertificateInBodyPart (DEFAULT_CRYPTO_SIGN_INCLUDE_CERTIFICATE_IN_BODY_PART);
    setCryptoVerifyUseCertificateInBodyPart (DEFAULT_CRYPTO_VERIFY_USE_CERTIFICATE_IN_BODY_PART);
    setHttpProxy (null);
    setAS2VersionID (CAS2Header.DEFAULT_AS2_VERSION);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Components", m_aComponents)
                                       .append ("CryptoSignIncludeCertificateInBodyPart", m_bCryptoSignIncludeCertificateInBodyPart)
                                       .append ("CryptoVerifyUseCertificateInBodyPart", m_bCryptoVerifyUseCertificateInBodyPart)
                                       .append ("HttpProxy", m_aHttpProxy)
                                       .append ("AS2VersionID", m_sAS2VersionID)
                                       .getToString ();
  }
}
