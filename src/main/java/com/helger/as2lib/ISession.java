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

import java.util.Map;

import javax.annotation.Nonnull;

import com.helger.as2lib.cert.ICertificateFactory;
import com.helger.as2lib.exception.ComponentNotFoundException;
import com.helger.as2lib.partner.IPartnershipFactory;
import com.helger.as2lib.processor.IProcessor;
import com.helger.commons.annotations.Nonempty;
import com.helger.commons.annotations.ReturnsMutableCopy;

/**
 * The <code>Session</code> interface provides configuration and resource
 * information, and a means for components to access the functionality of other
 * components.
 *
 * @author Aaron Silinskas
 * @see IDynamicComponent
 * @see com.helger.as2lib.cert.ICertificateFactory
 * @see com.helger.as2lib.partner.IPartnershipFactory
 * @see com.helger.as2lib.processor.IProcessor
 */
public interface ISession
{
  /**
   * Registers a component to a specified ID.
   *
   * @param sComponentID
   *        registers the component to this ID
   * @param aComponent
   *        component to register
   * @see IDynamicComponent
   */
  void addComponent (@Nonnull @Nonempty String sComponentID, @Nonnull IDynamicComponent aComponent);

  /**
   * Gets the <code>Component</code> currently registered with an ID
   *
   * @param sComponentID
   *        ID to search for
   * @return the component registered to the ID or null
   * @throws ComponentNotFoundException
   *         If a component is not registered with the ID
   */
  @Nonnull
  IDynamicComponent getComponent (String sComponentID) throws ComponentNotFoundException;

  /**
   * Return a map of component ID's to <code>Component</code> objects.
   *
   * @return all registered components, mapped by ID
   */
  @Nonnull
  @ReturnsMutableCopy
  Map <String, IDynamicComponent> getAllComponents ();

  /**
   * Short-cut method to retrieve a certificate factory.
   *
   * @return the currently registered <code>CertificateFactory</code> component
   * @throws ComponentNotFoundException
   *         If a <code>CertificateFactory</code> component has not been
   *         registered
   * @see ICertificateFactory
   * @see IDynamicComponent
   */
  @Nonnull
  ICertificateFactory getCertificateFactory () throws ComponentNotFoundException;

  /**
   * Short-cut method to retrieve a partner factory.
   *
   * @return the currently registered <code>PartnerFactory</code> component
   * @throws ComponentNotFoundException
   *         If a <code>PartnerFactory</code> component has not been registered
   * @see IPartnershipFactory
   * @see IDynamicComponent
   */
  @Nonnull
  IPartnershipFactory getPartnershipFactory () throws ComponentNotFoundException;

  /**
   * Short-cut method to retrieve a processor.
   *
   * @return the currently registered <code>Processor</code> component
   * @throws ComponentNotFoundException
   *         If a <code>Processor</code> component has not been registered
   * @see IProcessor
   * @see IDynamicComponent
   */
  @Nonnull
  IProcessor getProcessor () throws ComponentNotFoundException;
}
