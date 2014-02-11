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
package com.helger.as2lib;

import java.util.HashMap;
import java.util.Map;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.annotation.Nonnull;

import com.helger.as2lib.cert.ICertificateFactory;
import com.helger.as2lib.exception.ComponentNotFoundException;
import com.helger.as2lib.partner.IPartnershipFactory;
import com.helger.as2lib.processor.IProcessor;
import com.helger.as2lib.util.javamail.DispositionDataContentHandler;
import com.phloc.commons.annotations.ReturnsMutableCopy;
import com.phloc.commons.collections.ContainerHelper;

public class Session implements ISession
{
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

  @Nonnull
  public ICertificateFactory getCertificateFactory () throws ComponentNotFoundException
  {
    return (ICertificateFactory) getComponent (ICertificateFactory.COMPID_CERTIFICATE_FACTORY);
  }

  public void setComponent (final String sComponentID, final IDynamicComponent aComponent)
  {
    m_aComponents.put (sComponentID, aComponent);
  }

  @Nonnull
  public IDynamicComponent getComponent (final String sComponentID) throws ComponentNotFoundException
  {
    final IDynamicComponent aComponent = m_aComponents.get (sComponentID);
    if (aComponent == null)
      throw new ComponentNotFoundException (sComponentID);
    return aComponent;
  }

  @Nonnull
  @ReturnsMutableCopy
  public Map <String, IDynamicComponent> getAllComponents ()
  {
    return ContainerHelper.newMap (m_aComponents);
  }

  @Nonnull
  public IPartnershipFactory getPartnershipFactory () throws ComponentNotFoundException
  {
    return (IPartnershipFactory) getComponent (IPartnershipFactory.COMPID_PARTNERSHIP_FACTORY);
  }

  @Nonnull
  public IProcessor getProcessor () throws ComponentNotFoundException
  {
    return (IProcessor) getComponent (IProcessor.COMPID_PROCESSOR);
  }
}
