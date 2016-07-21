/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2016 Philip Helger philip[at]helger[dot]com
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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.AbstractDynamicComponent;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.processor.module.IProcessorActiveModule;
import com.helger.as2lib.processor.module.IProcessorModule;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.state.EChange;

/**
 * Abstract empty implementation of {@link IMessageProcessor}. It provides all
 * methods except
 * {@link #handle(String, com.helger.as2lib.message.IMessage, java.util.Map)}.
 *
 * @author Philip Helger
 */
public abstract class AbstractMessageProcessor extends AbstractDynamicComponent implements IMessageProcessor
{
  private final ICommonsList <IProcessorModule> m_aModules = new CommonsArrayList<> ();

  public void addModule (@Nonnull final IProcessorModule aModule)
  {
    ValueEnforcer.notNull (aModule, "Module");
    m_aModules.add (aModule);
  }

  @Nonnull
  public EChange removeModule (@Nullable final IProcessorModule aModule)
  {
    if (aModule == null)
      return EChange.UNCHANGED;
    return m_aModules.removeObject (aModule);
  }

  @Nonnegative
  public int getModuleCount ()
  {
    return m_aModules.size ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IProcessorModule> getAllModules ()
  {
    return m_aModules.getClone ();
  }

  @Nullable
  public <T extends IProcessorModule> T getModuleOfClass (@Nonnull final Class <T> aClass)
  {
    ValueEnforcer.notNull (aClass, "Class");
    return m_aModules.findFirstMapped (x -> aClass.isAssignableFrom (x.getClass ()), x -> aClass.cast (x));
  }

  @Nonnull
  @ReturnsMutableCopy
  public <T extends IProcessorModule> ICommonsList <T> getAllModulesOfClass (@Nonnull final Class <T> aClass)
  {
    ValueEnforcer.notNull (aClass, "Class");
    return m_aModules.getAllInstanceOf (aClass);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IProcessorActiveModule> getAllActiveModules ()
  {
    return m_aModules.getAllInstanceOf (IProcessorActiveModule.class);
  }

  public void startActiveModules ()
  {
    for (final IProcessorActiveModule aModule : getAllActiveModules ())
      try
      {
        aModule.start ();
      }
      catch (final OpenAS2Exception ex)
      {
        ex.terminate ();
      }
  }

  public void stopActiveModules ()
  {
    for (final IProcessorActiveModule aModule : getAllActiveModules ())
      try
      {
        aModule.stop ();
      }
      catch (final OpenAS2Exception ex)
      {
        ex.terminate ();
      }
  }
}
