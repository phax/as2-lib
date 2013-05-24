/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013 Philip Helger ph[at]phloc[dot]com
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


import com.helger.as2lib.BaseComponent;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.params.AbstractParameterParser;
import com.helger.as2lib.params.MessageParameters;
import com.phloc.commons.equals.EqualsUtils;

public abstract class AbstractPartnershipFactory extends BaseComponent implements IPartnershipFactory
{
  private List <Partnership> m_aPartnerships;

  @Nonnull
  public Partnership getPartnership (@Nonnull final Partnership p) throws OpenAS2Exception
  {
    Partnership ps = p.getName () == null ? null : getPartnership (getPartnerships (), p.getName ());
    if (ps == null)
      ps = getPartnership (p.getSenderIDs (), p.getReceiverIDs ());

    if (ps == null)
      throw new PartnershipNotFoundException (p);
    return ps;
  }

  public void setPartnerships (final List <Partnership> list)
  {
    m_aPartnerships = list;
  }

  public List <Partnership> getPartnerships ()
  {
    if (m_aPartnerships == null)
      m_aPartnerships = new ArrayList <Partnership> ();
    return m_aPartnerships;
  }

  public void updatePartnership (final IMessage msg, final boolean overwrite) throws OpenAS2Exception
  {
    // Fill in any available partnership information
    final Partnership partnership = getPartnership (msg.getPartnership ());
    msg.getPartnership ().copy (partnership);

    // Set attributes
    if (overwrite)
    {
      final String subject = partnership.getAttribute (Partnership.PA_SUBJECT);
      if (subject != null)
      {
        msg.setSubject (AbstractParameterParser.parse (subject, new MessageParameters (msg)));
      }
    }
  }

  public void updatePartnership (final IMessageMDN mdn, final boolean overwrite) throws OpenAS2Exception
  {
    // Fill in any available partnership information
    final Partnership partnership = getPartnership (mdn.getPartnership ());
    mdn.getPartnership ().copy (partnership);
  }

  protected Partnership getPartnership (final Map <String, String> senderIDs, final Map <String, String> receiverIDs)
  {
    for (final Partnership currentPs : getPartnerships ())
    {
      final Map <String, String> currentSids = currentPs.getSenderIDs ();
      final Map <String, String> currentRids = currentPs.getReceiverIDs ();

      if (compareMap (senderIDs, currentSids) && compareMap (receiverIDs, currentRids))
      {
        return currentPs;
      }
    }

    return null;
  }

  protected static Partnership getPartnership (final List <Partnership> partnerships, final String name)
  {
    for (final Partnership currentPs : partnerships)
      if (EqualsUtils.equals (currentPs.getName (), name))
        return currentPs;
    return null;
  }

  // returns true if all values in searchIds match values in partnerIds
  protected static boolean compareMap (final Map <String, String> searchIds, final Map <String, String> partnerIds)
  {
    if (searchIds.isEmpty ())
      return false;

    for (final Map.Entry <String, String> searchEntry : searchIds.entrySet ())
    {
      final String searchKey = searchEntry.getKey ();
      final String searchValue = searchEntry.getValue ();
      final String partnerValue = partnerIds.get (searchKey);
      if (!EqualsUtils.equals (searchValue, partnerValue))
        return false;
    }
    return true;
  }
}
