/*
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2025 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.processor;

import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.IDynamicComponent;
import com.helger.as2lib.exception.AS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.processor.module.IProcessorActiveModule;
import com.helger.as2lib.processor.module.IProcessorModule;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;

/**
 * Process incoming messages.
 *
 * @author Philip Helger
 */
public interface IMessageProcessor extends IDynamicComponent
{
  String ATTR_PENDINGMDN = "pendingmdn";
  String ATTR_PENDINGMDNINFO = "pendingmdninfo";

  /**
   * @return The name of the folder that contains the messages with the pending MDNs. May be
   *         <code>null</code>.
   * @see #ATTR_PENDINGMDN
   * @since 4.6.4
   */
  @Nullable
  default String getPendingMDNFolder ()
  {
    return attrs ().getAsString (ATTR_PENDINGMDN);
  }

  /**
   * Set the name of the folder that contains the messages with the pending MDN.
   *
   * @param sPendingMDNFolder
   *        The name of the folder. May neither be <code>null</code> nor empty.
   * @see #ATTR_PENDINGMDN
   * @since 4.6.4
   */
  default void setPendingMDNFolder (@Nonnull @Nonempty final String sPendingMDNFolder)
  {
    ValueEnforcer.notEmpty (sPendingMDNFolder, "PendingMDNFolder");
    attrs ().putIn (ATTR_PENDINGMDN, sPendingMDNFolder);
  }

  /**
   * @return The name of the folder that contains the pending MDN information files. May be
   *         <code>null</code>.
   * @see #ATTR_PENDINGMDNINFO
   * @since 4.6.4
   */
  @Nullable
  default String getPendingMDNInfoFolder ()
  {
    return attrs ().getAsString (ATTR_PENDINGMDNINFO);
  }

  /**
   * Set the name of the folder that contains the pending MDN information files.
   *
   * @param sPendingMDNInfoFolder
   *        The name of the folder. May neither be <code>null</code> nor empty.
   * @see #ATTR_PENDINGMDNINFO
   * @since 4.6.4
   */
  default void setPendingMDNInfoFolder (@Nonnull @Nonempty final String sPendingMDNInfoFolder)
  {
    ValueEnforcer.notEmpty (sPendingMDNInfoFolder, "PendingMDNInfoFolder");
    attrs ().putIn (ATTR_PENDINGMDNINFO, sPendingMDNInfoFolder);
  }

  void handle (@Nonnull String sAction, @Nonnull IMessage aMsg, @Nullable Map <String, Object> aOptions)
                                                                                                         throws AS2Exception;

  void addModule (@Nonnull IProcessorModule aModule);

  @Nonnull
  EChange removeModule (@Nullable IProcessorModule aModule);

  @Nonnegative
  int getModuleCount ();

  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <IProcessorModule> getAllModules ();

  @Nullable
  <T extends IProcessorModule> T getModuleOfClass (@Nonnull Class <T> aClass);

  @Nonnull
  @ReturnsMutableCopy
  <T extends IProcessorModule> ICommonsList <T> getAllModulesOfClass (@Nonnull Class <T> aClass);

  /**
   * @return A list of all modules, that implement the <code>IProcessorActiveModule</code>
   *         interface. Never <code>null</code> but maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <IProcessorActiveModule> getAllActiveModules ();

  /**
   * Call <code>start</code> on all modules that implement the <code>IProcessorActiveModule</code>
   * interface.
   */
  void startActiveModules ();

  /**
   * Call <code>stop</code> on all modules that implement the <code>IProcessorActiveModule</code>
   * interface.
   */
  void stopActiveModules ();
}
