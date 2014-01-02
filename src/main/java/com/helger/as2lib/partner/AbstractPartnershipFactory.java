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
package com.helger.as2lib.partner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.BaseComponent;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.PartnershipNotFoundException;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.params.AbstractParameterParser;
import com.helger.as2lib.params.MessageParameters;
import com.phloc.commons.equals.EqualsUtils;

public abstract class AbstractPartnershipFactory extends BaseComponent implements IPartnershipFactory
{
  private List <Partnership> m_aPartnerships;

  @Nonnull
  public Partnership getPartnership (@Nonnull final Partnership aPartnership) throws OpenAS2Exception
  {
    Partnership aRealPartnership = aPartnership.getName () == null ? null : getPartnership (getPartnerships (),
                                                                                            aPartnership.getName ());
    if (aRealPartnership == null)
      aRealPartnership = getPartnership (aPartnership.getSenderIDs (), aPartnership.getReceiverIDs ());

    if (aRealPartnership == null)
      throw new PartnershipNotFoundException ("Partnership not found: " + aPartnership);
    return aRealPartnership;
  }

  public void setPartnerships (@Nullable final List <Partnership> aPartnerships)
  {
    m_aPartnerships = aPartnerships;
  }

  @Nonnull
  public List <Partnership> getPartnerships ()
  {
    if (m_aPartnerships == null)
      m_aPartnerships = new ArrayList <Partnership> ();
    return m_aPartnerships;
  }

  public void updatePartnership (final IMessage aMsg, final boolean bOverwrite) throws OpenAS2Exception
  {
    // Fill in any available partnership information
    final Partnership aPartnership = getPartnership (aMsg.getPartnership ());
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

  public void updatePartnership (@Nonnull final IMessageMDN aMdn, final boolean bOverwrite) throws OpenAS2Exception
  {
    // Fill in any available partnership information
    final Partnership aPartnership = getPartnership (aMdn.getPartnership ());
    aMdn.getPartnership ().copyFrom (aPartnership);
  }

  @Nullable
  protected Partnership getPartnership (@Nonnull final Map <String, String> aSenderIDs,
                                        @Nonnull final Map <String, String> aReceiverIDs)
  {
    for (final Partnership aPartnerships : getPartnerships ())
    {
      final Map <String, String> aCurrentSenderIDs = aPartnerships.getSenderIDs ();
      if (compareMap (aSenderIDs, aCurrentSenderIDs))
      {
        final Map <String, String> aCurrentReceiverIDs = aPartnerships.getReceiverIDs ();
        if (compareMap (aReceiverIDs, aCurrentReceiverIDs))
          return aPartnerships;
      }
    }

    return null;
  }

  @Nullable
  protected static Partnership getPartnership (@Nonnull final List <Partnership> aPartnerships,
                                               @Nullable final String sName)
  {
    for (final Partnership aCurrentPartnership : aPartnerships)
      if (EqualsUtils.equals (aCurrentPartnership.getName (), sName))
        return aCurrentPartnership;
    return null;
  }

  // returns true if all values in searchIds match values in partnerIds
  protected static boolean compareMap (@Nonnull final Map <String, String> aSearchIDs,
                                       @Nonnull final Map <String, String> aPartnerIds)
  {
    if (aSearchIDs.isEmpty ())
      return false;

    for (final Map.Entry <String, String> aSearchEntry : aSearchIDs.entrySet ())
    {
      final String sSearchKey = aSearchEntry.getKey ();
      final String sSearchValue = aSearchEntry.getValue ();
      final String sPartnerValue = aPartnerIds.get (sSearchKey);
      if (!EqualsUtils.equals (sSearchValue, sPartnerValue))
        return false;
    }
    return true;
  }
}
