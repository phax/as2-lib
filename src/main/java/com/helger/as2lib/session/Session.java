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
package com.helger.as2lib.session;

import java.util.HashMap;
import java.util.Map;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.annotation.Nonnull;

import com.helger.as2lib.IDynamicComponent;
import com.helger.as2lib.cert.ICertificateFactory;
import com.helger.as2lib.exception.ComponentNotFoundException;
import com.helger.as2lib.exception.DuplicateComponentException;
import com.helger.as2lib.partner.IPartnershipFactory;
import com.helger.as2lib.processor.IMessageProcessor;
import com.helger.as2lib.util.javamail.DispositionDataContentHandler;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotations.Nonempty;
import com.helger.commons.annotations.ReturnsMutableCopy;
import com.helger.commons.collections.ContainerHelper;
import com.helger.commons.string.ToStringGenerator;

public class Session implements ISession
{
  public static final String COMPONENT_ID_CERTIFICATE_FACTORY = "certificatefactory";
  public static final String COMPONENT_ID_PARTNERSHIP_FACTORY = "partnershipfactory";
  public static final String COMPONENT_ID_MESSAGE_PROCESSOR = "message-processor";

  private final Map <String, IDynamicComponent> m_aComponents = new HashMap <String, IDynamicComponent> ();

  /**
   * Constructor
   */
  public Session ()
  {
    /*
     * Adds a group of content handlers to the Mailcap <code>CommandMap</code>.
     * These handlers are used by the JavaMail API to encode and decode
     * information of specific mime types.
     */
    final MailcapCommandMap aCommandCap = (MailcapCommandMap) CommandMap.getDefaultCommandMap ();
    aCommandCap.addMailcap ("message/disposition-notification;; x-java-content-handler=" +
                            DispositionDataContentHandler.class.getName ());
    CommandMap.setDefaultCommandMap (aCommandCap);
  }

  public final void addComponent (@Nonnull @Nonempty final String sComponentID,
                                  @Nonnull final IDynamicComponent aComponent) throws DuplicateComponentException
  {
    ValueEnforcer.notEmpty (sComponentID, "ComponentID");
    ValueEnforcer.notNull (aComponent, "Component");
    if (m_aComponents.containsKey (sComponentID))
      throw new DuplicateComponentException (sComponentID);
    m_aComponents.put (sComponentID, aComponent);
  }

  public void setCertificateFactory (@Nonnull final ICertificateFactory aCertFactory) throws DuplicateComponentException
  {
    addComponent (COMPONENT_ID_CERTIFICATE_FACTORY, aCertFactory);
  }

  public void setPartnershipFactory (@Nonnull final IPartnershipFactory aPartnershipFactory) throws DuplicateComponentException
  {
    addComponent (COMPONENT_ID_PARTNERSHIP_FACTORY, aPartnershipFactory);
  }

  public void setMessageProcessor (@Nonnull final IMessageProcessor aMsgProcessor) throws DuplicateComponentException
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
    return ContainerHelper.newMap (m_aComponents);
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

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("components", m_aComponents).toString ();
  }
}
