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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.as2lib.util.IStringMap;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.state.EChange;

/**
 * The default implementation of {@link IPartnershipMap}.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class PartnershipMap implements IPartnershipMap
{
  private final Map <String, Partnership> m_aMap = new LinkedHashMap <String, Partnership> ();

  public PartnershipMap ()
  {}

  public void setPartnerships (@Nonnull final PartnershipMap aPartnerships)
  {
    ValueEnforcer.notNull (aPartnerships, "Partnerships");
    m_aMap.clear ();
    m_aMap.putAll (aPartnerships.m_aMap);
  }

  @Nonnull
  public EChange addPartnership (@Nonnull final Partnership aPartnership)
  {
    ValueEnforcer.notNull (aPartnership, "Partnership");
    final String sName = aPartnership.getName ();
    if (m_aMap.containsKey (sName))
      return EChange.UNCHANGED;
    m_aMap.put (sName, aPartnership);
    return EChange.CHANGED;
  }

  public void setPartnership (@Nonnull final Partnership aPartnership)
  {
    ValueEnforcer.notNull (aPartnership, "Partnership");
    // overwrite if already present
    m_aMap.put (aPartnership.getName (), aPartnership);
  }

  @Nonnull
  public EChange removePartnership (@Nonnull final Partnership aPartnership)
  {
    ValueEnforcer.notNull (aPartnership, "Partnership");
    return EChange.valueOf (m_aMap.remove (aPartnership.getName ()) != null);
  }

  @Nullable
  public Partnership getPartnershipByName (@Nullable final String sName)
  {
    return m_aMap.get (sName);
  }

  /**
   * @param aSearchIDs
   *        Search IDs. May not be <code>null</code>.
   * @param aPartnerIDs
   *        Partner IDs. May not be <code>null</code>.
   * @return <code>true</code> if searchIds is not empty and if all values in
   *         searchIds match values in partnerIds. This means that partnerIds
   *         can contain more elements than searchIds
   */
  private static boolean _arePartnerIDsPresent (@Nonnull final IStringMap aSearchIDs,
                                                @Nonnull final IStringMap aPartnerIDs)
  {
    if (aSearchIDs.containsNoAttribute ())
      return false;

    for (final Map.Entry <String, String> aSearchEntry : aSearchIDs)
    {
      final String sSearchValue = aSearchEntry.getValue ();
      final String sPartnerValue = aPartnerIDs.getAttributeAsString (aSearchEntry.getKey ());
      if (!EqualsHelper.equals (sSearchValue, sPartnerValue))
        return false;
    }
    return true;
  }

  @Nullable
  public Partnership getPartnershipByID (@Nonnull final IStringMap aSenderIDs, @Nonnull final IStringMap aReceiverIDs)
  {
    for (final Partnership aPartnership : m_aMap.values ())
    {
      final IStringMap aCurrentSenderIDs = aPartnership.getAllSenderIDs ();
      if (_arePartnerIDsPresent (aSenderIDs, aCurrentSenderIDs))
      {
        final IStringMap aCurrentReceiverIDs = aPartnership.getAllReceiverIDs ();
        if (_arePartnerIDsPresent (aReceiverIDs, aCurrentReceiverIDs))
          return aPartnership;
      }
    }

    return null;
  }

  @Nonnull
  @ReturnsMutableCopy
  public Set <String> getAllPartnershipNames ()
  {
    return CollectionHelper.newOrderedSet (m_aMap.keySet ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <Partnership> getAllPartnerships ()
  {
    return CollectionHelper.newList (m_aMap.values ());
  }
}
