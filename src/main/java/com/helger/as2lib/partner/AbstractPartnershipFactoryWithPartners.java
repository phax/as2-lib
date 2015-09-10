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
import javax.annotation.concurrent.ThreadSafe;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.state.EChange;

/**
 * Abstract {@link IPartnershipFactoryWithPartners} implementation based on
 * {@link AbstractPartnershipFactory} using {@link PartnerMap} as the underlying
 * data storage object for the partners.
 *
 * @author Philip Helger
 */
@ThreadSafe
public abstract class AbstractPartnershipFactoryWithPartners extends AbstractPartnershipFactory implements IPartnershipFactoryWithPartners
{
  private final PartnerMap m_aPartners = new PartnerMap ();

  protected final void setPartners (@Nonnull final PartnerMap aPartners) throws OpenAS2Exception
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      m_aPartners.setPartners (aPartners);
      markAsChanged ();
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
  }

  public void addPartner (@Nonnull final Partner aNewPartner) throws OpenAS2Exception
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      m_aPartners.addPartner (aNewPartner);
      markAsChanged ();
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
  }

  @Nonnull
  public EChange removePartner (@Nullable final String sPartnerName) throws OpenAS2Exception
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      if (m_aPartners.removePartner (sPartnerName).isUnchanged ())
        return EChange.UNCHANGED;
      markAsChanged ();
      return EChange.CHANGED;
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
  }

  @Nullable
  public Partner getPartnerOfName (@Nullable final String sPartnerName)
  {
    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aPartners.getPartnerOfName (sPartnerName);
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  @Nonnull
  @ReturnsMutableCopy
  public Set <String> getAllPartnerNames ()
  {
    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aPartners.getAllPartnerNames ();
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <Partner> getAllPartners ()
  {
    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aPartners.getAllPartners ();
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  @Nonnull
  public IPartnerMap getPartnerMap ()
  {
    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aPartners;
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }
}
