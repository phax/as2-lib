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
package com.helger.as2lib.session;

import java.net.Proxy;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.as2lib.IDynamicComponent;
import com.helger.as2lib.cert.ICertificateFactory;
import com.helger.as2lib.partner.IPartnershipFactory;
import com.helger.as2lib.processor.IMessageProcessor;
import com.helger.as2lib.util.javamail.DispositionDataContentHandler;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.lang.priviledged.AccessControllerHelper;
import com.helger.commons.string.ToStringGenerator;

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

  public static final boolean DEFAULT_CRYPTO_VERIFY_USE_CERTIFICATE_IN_BODY_PART = true;

  private final Map <String, IDynamicComponent> m_aComponents = new HashMap <String, IDynamicComponent> ();
  private boolean m_bCryptoVerifyUseCertificateInBodyPart = DEFAULT_CRYPTO_VERIFY_USE_CERTIFICATE_IN_BODY_PART;
  private Proxy m_aHttpProxy;

  /**
   * Constructor
   */
  public AS2Session ()
  {
    /*
     * Adds a group of content handlers to the Mailcap <code>CommandMap</code>.
     * These handlers are used by the JavaMail API to encode and decode
     * information of specific mime types.
     */
    final MailcapCommandMap aCommandMap = (MailcapCommandMap) CommandMap.getDefaultCommandMap ();
    aCommandMap.addMailcap ("message/disposition-notification;; x-java-content-handler=" +
                            DispositionDataContentHandler.class.getName ());
    AccessControllerHelper.run (new PrivilegedAction <Object> ()
    {
      public Object run ()
      {
        CommandMap.setDefaultCommandMap (aCommandMap);
        return null;
      }
    });
  }

  public final void addComponent (@Nonnull @Nonempty final String sComponentID,
                                  @Nonnull final IDynamicComponent aComponent) throws ComponentDuplicateException
  {
    ValueEnforcer.notEmpty (sComponentID, "ComponentID");
    ValueEnforcer.notNull (aComponent, "Component");
    if (m_aComponents.containsKey (sComponentID))
      throw new ComponentDuplicateException (sComponentID);
    m_aComponents.put (sComponentID, aComponent);
  }

  public void setCertificateFactory (@Nonnull final ICertificateFactory aCertFactory) throws ComponentDuplicateException
  {
    addComponent (COMPONENT_ID_CERTIFICATE_FACTORY, aCertFactory);
  }

  public void setPartnershipFactory (@Nonnull final IPartnershipFactory aPartnershipFactory) throws ComponentDuplicateException
  {
    addComponent (COMPONENT_ID_PARTNERSHIP_FACTORY, aPartnershipFactory);
  }

  public void setMessageProcessor (@Nonnull final IMessageProcessor aMsgProcessor) throws ComponentDuplicateException
  {
    addComponent (COMPONENT_ID_MESSAGE_PROCESSOR, aMsgProcessor);
  }

  @Nonnull
  public final IDynamicComponent getComponent (@Nonnull @Nonempty final String sComponentID) throws ComponentNotFoundException
  {
    ValueEnforcer.notEmpty (sComponentID, "ComponentID");
    final IDynamicComponent aComponent = m_aComponents.get (sComponentID);
    if (aComponent == null)
      throw new ComponentNotFoundException (sComponentID);
    return aComponent;
  }

  @Nonnull
  @ReturnsMutableCopy
  public final Map <String, IDynamicComponent> getAllComponents ()
  {
    return CollectionHelper.newMap (m_aComponents);
  }

  @Nonnull
  public final ICertificateFactory getCertificateFactory () throws ComponentNotFoundException
  {
    return (ICertificateFactory) getComponent (COMPONENT_ID_CERTIFICATE_FACTORY);
  }

  @Nonnull
  public final IPartnershipFactory getPartnershipFactory () throws ComponentNotFoundException
  {
    return (IPartnershipFactory) getComponent (COMPONENT_ID_PARTNERSHIP_FACTORY);
  }

  @Nonnull
  public final IMessageProcessor getMessageProcessor () throws ComponentNotFoundException
  {
    return (IMessageProcessor) getComponent (COMPONENT_ID_MESSAGE_PROCESSOR);
  }

  public boolean isCryptoVerifyUseCertificateInBodyPart ()
  {
    return m_bCryptoVerifyUseCertificateInBodyPart;
  }

  public void setCryptoVerifyUseCertificateInBodyPart (final boolean bCryptoVerifyUseCertificateInBodyPart)
  {
    m_bCryptoVerifyUseCertificateInBodyPart = bCryptoVerifyUseCertificateInBodyPart;
  }

  @Nullable
  public Proxy getHttpProxy ()
  {
    return m_aHttpProxy;
  }

  public void setHttpProxy (@Nullable final Proxy aHttpProxy)
  {
    m_aHttpProxy = aHttpProxy;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("components", m_aComponents).toString ();
  }
}
