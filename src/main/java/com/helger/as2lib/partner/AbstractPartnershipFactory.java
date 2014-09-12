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
package com.helger.as2lib.partner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.as2lib.AbstractDynamicComponent;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.PartnershipNotFoundException;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.params.AbstractParameterParser;
import com.helger.as2lib.params.MessageParameters;
import com.helger.as2lib.util.IStringMap;
import com.helger.as2lib.util.StringMap;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotations.ReturnsMutableCopy;
import com.helger.commons.collections.ContainerHelper;
import com.helger.commons.equals.EqualsUtils;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;

@NotThreadSafe
public abstract class AbstractPartnershipFactory extends AbstractDynamicComponent implements IPartnershipFactory
{
  private final PartnerMap m_aPartners = new PartnerMap ();
  private final List <Partnership> m_aPartnerships = new ArrayList <Partnership> ();

  public void addPartner (@Nonnull final StringMap aNewPartner) throws OpenAS2Exception
  {
    m_aPartners.add (aNewPartner);
  }

  @Nonnull
  public EChange removePartner (@Nullable final String sPartnerName)
  {
    return m_aPartners.removePartner (sPartnerName);
  }

  @Nullable
  public StringMap getPartnerOfName (@Nullable final String sPartnerName)
  {
    return m_aPartners.getPartnerOfName (sPartnerName);
  }

  @Nonnull
  @ReturnsMutableCopy
  public Set <String> getAllPartnerNames ()
  {
    return m_aPartners.getAllPartnerNames ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <StringMap> getAllPartners ()
  {
    return m_aPartners.getAllPartners ();
  }

  @Nonnull
  public IPartnerMap getPartnerMap ()
  {
    return m_aPartners;
  }

  protected final void setPartners (@Nonnull final PartnerMap aPartners)
  {
    m_aPartners.set (aPartners);
  }

  @Nonnull
  @OverridingMethodsMustInvokeSuper
  public Partnership getPartnership (@Nonnull final Partnership aPartnership) throws OpenAS2Exception
  {
    ValueEnforcer.notNull (aPartnership, "Partnership");
    Partnership aRealPartnership = getPartnershipByName (m_aPartnerships, aPartnership.getName ());
    if (aRealPartnership == null)
    {
      // Found no partnership by name
      aRealPartnership = getPartnershipByID (aPartnership.getAllSenderIDs (), aPartnership.getAllReceiverIDs ());
    }

    if (aRealPartnership == null)
      throw new PartnershipNotFoundException ("Partnership not found: " + aPartnership);
    return aRealPartnership;
  }

  protected final void setPartnerships (@Nullable final List <Partnership> aPartnerships)
  {
    m_aPartnerships.clear ();
    if (aPartnerships != null)
      m_aPartnerships.addAll (aPartnerships);
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <Partnership> getAllPartnerships ()
  {
    return ContainerHelper.newList (m_aPartnerships);
  }

  public final void addPartnership (@Nonnull final Partnership aPartnership)
  {
    ValueEnforcer.notNull (aPartnership, "Partnership");
    m_aPartnerships.add (aPartnership);
  }

  @Nonnull
  public final EChange removePartnership (@Nonnull final Partnership aPartnership)
  {
    ValueEnforcer.notNull (aPartnership, "Partnership");
    return EChange.valueOf (m_aPartnerships.remove (aPartnership));
  }

  public final void updatePartnership (@Nonnull final IMessage aMsg, final boolean bOverwrite) throws OpenAS2Exception
  {
    // Fill in any available partnership information
    final Partnership aPartnership = getPartnership (aMsg.getPartnership ());

    // Update partnership data of message with the stored ones
    aMsg.getPartnership ().copyFrom (aPartnership);

    // Set attributes
    if (bOverwrite)
    {
      final String sSubject = aPartnership.getAttribute (Partnership.PA_SUBJECT);
      if (sSubject != null)
      {
        aMsg.setSubject (AbstractParameterParser.parse (sSubject, new MessageParameters (aMsg)));
      }
    }
  }

  public final void updatePartnership (@Nonnull final IMessageMDN aMdn, final boolean bOverwrite) throws OpenAS2Exception
  {
    // Fill in any available partnership information
    final Partnership aPartnership = getPartnership (aMdn.getPartnership ());
    aMdn.getPartnership ().copyFrom (aPartnership);
  }

  @Nullable
  protected final Partnership getPartnershipByID (@Nonnull final IStringMap aSenderIDs,
                                                  @Nonnull final IStringMap aReceiverIDs)
  {
    for (final Partnership aPartnership : m_aPartnerships)
    {
      final IStringMap aCurrentSenderIDs = aPartnership.getAllSenderIDs ();
      if (arePartnerIDsPresent (aSenderIDs, aCurrentSenderIDs))
      {
        final IStringMap aCurrentReceiverIDs = aPartnership.getAllReceiverIDs ();
        if (arePartnerIDsPresent (aReceiverIDs, aCurrentReceiverIDs))
          return aPartnership;
      }
    }

    return null;
  }

  @Nullable
  protected static Partnership getPartnershipByName (@Nonnull final List <Partnership> aPartnerships,
                                                     @Nullable final String sName)
  {
    if (StringHelper.hasText (sName))
      for (final Partnership aCurrentPartnership : aPartnerships)
        if (aCurrentPartnership.getName ().equals (sName))
          return aCurrentPartnership;
    return null;
  }

  /**
   * @param aSearchIDs
   *        Search IDs
   * @param aPartnerIds
   *        Partner IDs.
   * @return <code>true</code> if searchIds is not empty and if all values in
   *         searchIds match values in partnerIds. This means that partnerIds
   *         can contain more elements than searchIds
   */
  private static boolean arePartnerIDsPresent (@Nonnull final IStringMap aSearchIDs,
                                               @Nonnull final IStringMap aPartnerIds)
  {
    if (aSearchIDs.containsNoAttribute ())
      return false;

    for (final Map.Entry <String, String> aSearchEntry : aSearchIDs)
    {
      final String sSearchValue = aSearchEntry.getValue ();
      final String sPartnerValue = aPartnerIds.getAttributeObject (aSearchEntry.getKey ());
      if (!EqualsUtils.equals (sSearchValue, sPartnerValue))
        return false;
    }
    return true;
  }
}
