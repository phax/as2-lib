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

import java.io.Serializable;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.util.IStringMap;
import com.helger.as2lib.util.StringMap;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.string.ToStringGenerator;

public class Partnership implements Serializable
{
  public static final String DEFAULT_NAME = "auto-created-dummy";

  private String m_sName;
  private final StringMap m_aAttributes = new StringMap ();
  private final StringMap m_aSenderIDs = new StringMap ();
  private final StringMap m_aReceiverIDs = new StringMap ();

  public Partnership ()
  {
    this (DEFAULT_NAME);
  }

  public Partnership (@Nonnull final String sName)
  {
    setName (sName);
  }

  public void setName (@Nonnull final String sName)
  {
    m_sName = ValueEnforcer.notNull (sName, "Name");
  }

  @Nonnull
  public String getName ()
  {
    return m_sName;
  }

  public void setAttribute (final String sKey, final String sValue)
  {
    m_aAttributes.setAttribute (sKey, sValue);
  }

  @Nullable
  public String getAttribute (@Nullable final String sKey)
  {
    return m_aAttributes.getAttributeAsString (sKey);
  }

  @Nullable
  public String getAttribute (@Nullable final String sKey, @Nullable final String sDefault)
  {
    return m_aAttributes.getAttributeAsString (sKey, sDefault);
  }

  @Nonnull
  @ReturnsMutableCopy
  public IStringMap getAllAttributes ()
  {
    return m_aAttributes.getClone ();
  }

  public void addAllAttributes (@Nullable final Map <String, String> aAttributes)
  {
    m_aAttributes.addAttributes (aAttributes);
  }

  public void setSenderID (@Nonnull final String sKey, @Nullable final String sValue)
  {
    m_aSenderIDs.setAttribute (sKey, sValue);
  }

  public void setSenderAS2ID (@Nullable final String sValue)
  {
    setSenderID (CPartnershipIDs.PID_AS2, sValue);
  }

  public void setSenderX509Alias (@Nullable final String sValue)
  {
    setSenderID (CPartnershipIDs.PID_X509_ALIAS, sValue);
  }

  public void setSenderEmail (@Nullable final String sValue)
  {
    setSenderID (CPartnershipIDs.PID_EMAIL, sValue);
  }

  public void addSenderIDs (@Nullable final Map <String, String> aMap)
  {
    m_aSenderIDs.addAttributes (aMap);
  }

  @Nullable
  public String getSenderID (@Nullable final String sKey)
  {
    return m_aSenderIDs.getAttributeAsString (sKey);
  }

  @Nullable
  public String getSenderAS2ID ()
  {
    return getSenderID (CPartnershipIDs.PID_AS2);
  }

  @Nullable
  public String getSenderX509Alias ()
  {
    return getSenderID (CPartnershipIDs.PID_X509_ALIAS);
  }

  @Nullable
  public String getSenderEmail ()
  {
    return getSenderID (CPartnershipIDs.PID_EMAIL);
  }

  public boolean containsSenderID (@Nullable final String sKey)
  {
    return m_aSenderIDs.containsAttribute (sKey);
  }

  public boolean containsSenderAS2ID ()
  {
    return containsSenderID (CPartnershipIDs.PID_AS2);
  }

  public boolean containsSenderX509Alias ()
  {
    return containsSenderID (CPartnershipIDs.PID_X509_ALIAS);
  }

  public boolean containsSenderEmail ()
  {
    return containsSenderID (CPartnershipIDs.PID_EMAIL);
  }

  @Nonnull
  @ReturnsMutableCopy
  public IStringMap getAllSenderIDs ()
  {
    return m_aSenderIDs.getClone ();
  }

  /**
   * Set an arbitrary receiver ID.
   *
   * @param sKey
   *        The name of the ID. May not be <code>null</code>.
   * @param sValue
   *        The value to be set. It may be <code>null</code> in which case the
   *        attribute is removed.
   */
  public void setReceiverID (@Nonnull final String sKey, @Nullable final String sValue)
  {
    m_aReceiverIDs.setAttribute (sKey, sValue);
  }

  /**
   * Set the receivers AS2 ID.
   *
   * @param sValue
   *        The value to be set. May be <code>null</code>.
   * @see #getReceiverAS2ID()
   * @see #containsReceiverAS2ID()
   */
  public void setReceiverAS2ID (@Nullable final String sValue)
  {
    setReceiverID (CPartnershipIDs.PID_AS2, sValue);
  }

  /**
   * Set the receivers X509 alias.
   *
   * @param sValue
   *        The value to be set. May be <code>null</code>.
   * @see #getReceiverX509Alias()
   * @see #containsReceiverX509Alias()
   */
  public void setReceiverX509Alias (@Nullable final String sValue)
  {
    setReceiverID (CPartnershipIDs.PID_X509_ALIAS, sValue);
  }

  /**
   * Set the receivers email address.
   *
   * @param sValue
   *        The value to be set. May be <code>null</code>.
   * @see #getReceiverEmail()
   * @see #containsReceiverEmail()
   */
  public void setReceiverEmail (@Nullable final String sValue)
  {
    setReceiverID (CPartnershipIDs.PID_EMAIL, sValue);
  }

  /**
   * Add all receiver IDs provided in the passed map. Existing receiver IDs are
   * not altered.
   *
   * @param aMap
   *        The map to use. May be <code>null</code>.
   */
  public void addReceiverIDs (@Nullable final Map <String, String> aMap)
  {
    m_aReceiverIDs.addAttributes (aMap);
  }

  /**
   * Get the value of an arbitrary receiver ID
   *
   * @param sKey
   *        The name of the ID to query. May be <code>null</code>.
   * @return The contained value if the name is not <code>null</code> and
   *         contained in the receiver IDs.
   */
  @Nullable
  public String getReceiverID (@Nullable final String sKey)
  {
    return m_aReceiverIDs.getAttributeAsString (sKey);
  }

  /**
   * @return the receiver's AS2 ID or <code>null</code> if it is not set
   * @see #setReceiverAS2ID(String)
   * @see #containsReceiverAS2ID()
   */
  @Nullable
  public String getReceiverAS2ID ()
  {
    return getReceiverID (CPartnershipIDs.PID_AS2);
  }

  /**
   * @return the receiver's X509 alias or <code>null</code> if it is not set
   * @see #setReceiverX509Alias(String)
   * @see #containsReceiverX509Alias()
   */
  @Nullable
  public String getReceiverX509Alias ()
  {
    return getReceiverID (CPartnershipIDs.PID_X509_ALIAS);
  }

  /**
   * @return the receiver's email address or <code>null</code> if it is not set.
   * @see #setReceiverEmail(String)
   * @see #containsReceiverEmail()
   */
  @Nullable
  public String getReceiverEmail ()
  {
    return getReceiverID (CPartnershipIDs.PID_EMAIL);
  }

  /**
   * Check if an arbitrary receiver ID is present.
   *
   * @param sKey
   *        The name of the ID to query. May be <code>null</code>.
   * @return <code>true</code> if the name is not <code>null</code> and
   *         contained in the receiver IDs.
   */
  public boolean containsReceiverID (@Nullable final String sKey)
  {
    return m_aReceiverIDs.containsAttribute (sKey);
  }

  /**
   * @return <code>true</code> if the receiver's AS2 ID is present,
   *         <code>false</code> otherwise.
   * @see #setReceiverAS2ID(String)
   * @see #getReceiverAS2ID()
   */
  public boolean containsReceiverAS2ID ()
  {
    return containsReceiverID (CPartnershipIDs.PID_AS2);
  }

  /**
   * @return <code>true</code> if the receiver's X509 alias is present,
   *         <code>false</code> otherwise.
   * @see #setReceiverX509Alias(String)
   * @see #getReceiverX509Alias()
   */
  public boolean containsReceiverX509Alias ()
  {
    return containsReceiverID (CPartnershipIDs.PID_X509_ALIAS);
  }

  /**
   * @return <code>true</code> if the receiver's email address is present,
   *         <code>false</code> otherwise.
   * @see #setReceiverEmail(String)
   * @see #getReceiverEmail()
   */
  public boolean containsReceiverEmail ()
  {
    return containsReceiverID (CPartnershipIDs.PID_EMAIL);
  }

  /**
   * @return All receiver IDs. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableCopy
  public IStringMap getAllReceiverIDs ()
  {
    return m_aReceiverIDs.getClone ();
  }

  /**
   * Check if sender and receiver IDs of this partnership match the ones of the
   * provided partnership.
   *
   * @param aPartnership
   *        The partnership to compare to. May not be <code>null</code>.
   * @return <code>true</code> if sender and receiver IDs of this partnership
   *         are present in the sender and receiver IDs of the provided
   *         partnership.
   */
  public boolean matches (@Nonnull final Partnership aPartnership)
  {
    ValueEnforcer.notNull (aPartnership, "Partnership");
    return compareIDs (m_aSenderIDs, aPartnership.m_aSenderIDs) &&
           compareIDs (m_aReceiverIDs, aPartnership.m_aReceiverIDs);
  }

  /**
   * Check if all values from the left side are also present on the right side.
   *
   * @param aIDs
   *        The source map which must be fully contained in the aCompareTo map
   * @param aCompareTo
   *        The map to compare to. May not be <code>null</code>. It may contain
   *        more attributes than aIDs but must at least contain the same ones.
   * @return <code>true</code> if aIDs is not empty and all values from aIDs are
   *         also present in aCompareTo, <code>false</code> otherwise.
   */
  protected boolean compareIDs (@Nonnull final IStringMap aIDs, @Nonnull final IStringMap aCompareTo)
  {
    if (aIDs.containsNoAttribute ())
      return false;

    for (final Map.Entry <String, String> aEntry : aIDs)
    {
      final String sCurrentValue = aEntry.getValue ();
      final String sCompareValue = aCompareTo.getAttributeObject (aEntry.getKey ());
      if (!EqualsHelper.equals (sCurrentValue, sCompareValue))
        return false;
    }
    return true;
  }

  /**
   * Set all fields of this partnership with the data from the provided
   * partnership. Name, sender IDs, receiver IDs and attributes are fully
   * overwritten!
   *
   * @param aPartnership
   *        The partnership to copy the data from. May not be <code>null</code>.
   */
  public void copyFrom (@Nonnull final Partnership aPartnership)
  {
    ValueEnforcer.notNull (aPartnership, "Partnership");

    // Avoid doing something
    if (aPartnership != this)
    {
      m_sName = aPartnership.getName ();
      m_aSenderIDs.setAttributes (aPartnership.m_aSenderIDs);
      m_aReceiverIDs.setAttributes (aPartnership.m_aReceiverIDs);
      m_aAttributes.setAttributes (aPartnership.m_aAttributes);
    }
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("name", m_sName)
                                       .append ("senderIDs", m_aSenderIDs)
                                       .append ("receiverIDs", m_aReceiverIDs)
                                       .append ("attributes", m_aAttributes)
                                       .toString ();
  }
}
