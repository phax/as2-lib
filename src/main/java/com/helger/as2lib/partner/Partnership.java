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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.phloc.commons.equals.EqualsUtils;

public class Partnership implements Serializable
{
  // Sender partner type
  public static final String PARTNERSHIP_TYPE_SENDER = "sender";
  // Receiver partner type
  public static final String PARTNERSHIP_TYPE_RECEIVER = "receiver";
  // Email address
  public static final String PID_EMAIL = "email";
  // AS1 or AS2
  public static final String PA_PROTOCOL = "protocol";
  // Subject sent in messages
  public static final String PA_SUBJECT = "subject";
  // optional content transfer encoding value
  public static final String PA_CONTENT_TRANSFER_ENCODING = "content_transfer_encoding";

  private Map <String, String> m_aAttributes;
  private Map <String, String> m_aReceiverIDs;
  private Map <String, String> m_aSenderIDs;
  private String m_sName;

  public void setName (final String sKey)
  {
    m_sName = sKey;
  }

  public String getName ()
  {
    return m_sName;
  }

  public void setAttribute (final String sKey, final String sValue)
  {
    getAttributes ().put (sKey, sValue);
  }

  @Nullable
  public String getAttribute (final String sKey)
  {
    return getAttributes ().get (sKey);
  }

  public void setAttributes (@Nullable final Map <String, String> aAttributes)
  {
    m_aAttributes = aAttributes;
  }

  @Nonnull
  public Map <String, String> getAttributes ()
  {
    if (m_aAttributes == null)
      m_aAttributes = new HashMap <String, String> ();
    return m_aAttributes;
  }

  public void setReceiverID (@Nullable final String sKey, final String sValue)
  {
    getReceiverIDs ().put (sKey, sValue);
  }

  @Nullable
  public String getReceiverID (@Nullable final String sKey)
  {
    return getReceiverIDs ().get (sKey);
  }

  public void setReceiverIDs (@Nullable final Map <String, String> aReceiverIDs)
  {
    m_aReceiverIDs = aReceiverIDs;
  }

  @Nonnull
  public Map <String, String> getReceiverIDs ()
  {
    if (m_aReceiverIDs == null)
      m_aReceiverIDs = new HashMap <String, String> ();
    return m_aReceiverIDs;
  }

  public void setSenderID (final String sKey, final String sValue)
  {
    getSenderIDs ().put (sKey, sValue);
  }

  @Nullable
  public String getSenderID (@Nullable final String sKey)
  {
    return getSenderIDs ().get (sKey);
  }

  public void setSenderIDs (@Nullable final Map <String, String> aSenderIDs)
  {
    m_aSenderIDs = aSenderIDs;
  }

  @Nonnull
  public Map <String, String> getSenderIDs ()
  {
    if (m_aSenderIDs == null)
      m_aSenderIDs = new HashMap <String, String> ();
    return m_aSenderIDs;
  }

  public boolean matches (@Nonnull final Partnership aPartnership)
  {
    final Map <String, String> aSenderIDs = aPartnership.getSenderIDs ();
    final Map <String, String> aReceiverIDs = aPartnership.getReceiverIDs ();

    return compareIDs (aSenderIDs, getSenderIDs ()) && compareIDs (aReceiverIDs, getReceiverIDs ());
  }

  protected boolean compareIDs (@Nonnull final Map <String, String> aIDs, @Nonnull final Map <String, String> aCompareTo)
  {
    if (aIDs.isEmpty ())
      return false;

    for (final Map.Entry <String, String> aEntry : aIDs.entrySet ())
    {
      final String sCurrentValue = aEntry.getValue ();
      final String sCompareValue = aCompareTo.get (aEntry.getKey ());
      if (!EqualsUtils.equals (sCurrentValue, sCompareValue))
        return false;
    }

    return true;
  }

  public void copyFrom (@Nonnull final Partnership aPartnership)
  {
    if (aPartnership.getName () != null)
      setName (aPartnership.getName ());
    getSenderIDs ().putAll (aPartnership.getSenderIDs ());
    getReceiverIDs ().putAll (aPartnership.getReceiverIDs ());
    getAttributes ().putAll (aPartnership.getAttributes ());
  }

  @Override
  public String toString ()
  {
    final StringBuilder buf = new StringBuilder ();
    buf.append ("Partnership ").append (getName ());
    buf.append (" Sender IDs = ").append (getSenderIDs ());
    buf.append (" Receiver IDs = ").append (getReceiverIDs ());
    buf.append (" Attributes = ").append (getAttributes ());
    return buf.toString ();
  }
}
