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
package com.helger.as2lib.partner;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.AbstractDynamicComponent;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.params.MessageParameters;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ELockType;
import com.helger.commons.annotation.IsLocked;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.state.EChange;

/**
 * Abstract {@link IPartnershipFactory} implementation using
 * {@link PartnershipMap} as the underlying data storage object.
 * 
 * @author Philip Helger
 */
@ThreadSafe
public abstract class AbstractPartnershipFactory extends AbstractDynamicComponent implements IPartnershipFactory
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AbstractPartnershipFactory.class);

  private final PartnershipMap m_aPartnerships = new PartnershipMap ();

  /**
   * Callback method that is invoked, when this object is modified. This method
   * must be overridden to do something useful. A use case scenario could e.g.
   * be automatic storage of changes.
   *
   * @throws OpenAS2Exception
   *         In case anything goes wrong
   */
  @OverrideOnDemand
  @IsLocked (ELockType.WRITE)
  protected void markAsChanged () throws OpenAS2Exception
  {}

  @Nonnull
  @OverridingMethodsMustInvokeSuper
  public Partnership getPartnership (@Nonnull final Partnership aPartnership) throws OpenAS2Exception
  {
    ValueEnforcer.notNull (aPartnership, "Partnership");

    m_aRWLock.readLock ().lock ();
    try
    {
      Partnership aRealPartnership = m_aPartnerships.getPartnershipByName (aPartnership.getName ());
      if (aRealPartnership == null)
      {
        // Found no partnership by name
        aRealPartnership = m_aPartnerships.getPartnershipByID (aPartnership.getAllSenderIDs (),
                                                               aPartnership.getAllReceiverIDs ());
      }

      if (aRealPartnership == null)
        throw new PartnershipNotFoundException (aPartnership);
      return aRealPartnership;
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  @Nullable
  public Partnership getPartnershipByName (@Nullable final String sName)
  {
    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aPartnerships.getPartnershipByName (sName);
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  @Nonnull
  @ReturnsMutableCopy
  public Set <String> getAllPartnershipNames ()
  {
    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aPartnerships.getAllPartnershipNames ();
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <Partnership> getAllPartnerships ()
  {
    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aPartnerships.getAllPartnerships ();
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  @Nonnull
  public IPartnershipMap getPartnershipMap ()
  {
    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aPartnerships;
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  protected final void setPartnerships (@Nonnull final PartnershipMap aPartnerships) throws OpenAS2Exception
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      m_aPartnerships.setPartnerships (aPartnerships);
      markAsChanged ();
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
  }

  @Nonnull
  public final EChange addPartnership (@Nonnull final Partnership aPartnership) throws OpenAS2Exception
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      if (m_aPartnerships.addPartnership (aPartnership).isUnchanged ())
        return EChange.UNCHANGED;
      markAsChanged ();
      return EChange.CHANGED;
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
  }

  @Nonnull
  public final EChange removePartnership (@Nonnull final Partnership aPartnership) throws OpenAS2Exception
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      if (m_aPartnerships.removePartnership (aPartnership).isUnchanged ())
        return EChange.UNCHANGED;
      markAsChanged ();
      return EChange.CHANGED;
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
  }

  public final void updatePartnership (@Nonnull final IMessage aMsg, final boolean bOverwrite) throws OpenAS2Exception
  {
    ValueEnforcer.notNull (aMsg, "Message");

    // Fill in any available partnership information
    final Partnership aPartnership = getPartnership (aMsg.getPartnership ());

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Updating partnership " + aPartnership);

    // Update partnership data of message with the stored ones
    aMsg.getPartnership ().copyFrom (aPartnership);

    // Set attributes
    if (bOverwrite)
    {
      final String sSubject = aPartnership.getAttribute (CPartnershipIDs.PA_SUBJECT);
      if (sSubject != null)
      {
        aMsg.setSubject (new MessageParameters (aMsg).format (sSubject));
      }
    }
  }

  public final void updatePartnership (@Nonnull final IMessageMDN aMdn,
                                       final boolean bOverwrite) throws OpenAS2Exception
  {
    ValueEnforcer.notNull (aMdn, "MessageMDN");

    // Fill in any available partnership information
    final Partnership aPartnership = getPartnership (aMdn.getPartnership ());
    aMdn.getPartnership ().copyFrom (aPartnership);
  }
}
