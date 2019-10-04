/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2019 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2.cmdprocessor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2.cmd.ICommand;
import com.helger.as2.cmd.ICommandRegistry;
import com.helger.as2lib.IDynamicComponent;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.session.IAS2Session;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.annotation.UnsupportedOperation;
import com.helger.commons.collection.attr.StringMap;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.lang.ClassHelper;

public abstract class AbstractCommandProcessor implements ICommandProcessor, IDynamicComponent, Runnable
{
  private final StringMap m_aAttrs = new StringMap ();
  private final ICommonsOrderedMap <String, ICommand> m_aCommands = new CommonsLinkedHashMap <> ();
  private volatile boolean m_bTerminated = false;

  public AbstractCommandProcessor ()
  {}

  @Nonnull
  @ReturnsMutableObject
  public final StringMap attrs ()
  {
    return m_aAttrs;
  }

  @Nonnull
  public String getName ()
  {
    return ClassHelper.getClassLocalName (this);
  }

  @UnsupportedOperation
  public IAS2Session getSession ()
  {
    throw new UnsupportedOperationException ("No session available!");
  }

  public void init () throws OpenAS2Exception
  {}

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsMap <String, ICommand> getAllCommands ()
  {
    return m_aCommands.getClone ();
  }

  @Nullable
  public ICommand getCommand (final String name)
  {
    return m_aCommands.get (name);
  }

  public boolean isTerminated ()
  {
    return m_bTerminated;
  }

  @UnsupportedOperation
  public void processCommand () throws OpenAS2Exception
  {
    throw new OpenAS2Exception ("super class method call, not initialized correctly");
  }

  public void addCommands (@Nonnull final ICommandRegistry aCommandRegistry)
  {
    ValueEnforcer.notNull (aCommandRegistry, "CommandRegistry");
    m_aCommands.putAll (aCommandRegistry.getAllCommands ());
  }

  public void terminate ()
  {
    m_bTerminated = true;
  }
}
