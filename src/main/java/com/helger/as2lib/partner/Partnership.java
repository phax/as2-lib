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

import com.phloc.commons.equals.EqualsUtils;

public class Partnership implements Serializable
{
  // Sender partner type
  public static final String PTYPE_SENDER = "sender";
  // Receiver partner type
  public static final String PTYPE_RECEIVER = "receiver";
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

  public void setName (final String name)
  {
    m_sName = name;
  }

  public String getName ()
  {
    return m_sName;
  }

  public void setAttribute (final String id, final String value)
  {
    getAttributes ().put (id, value);
  }

  public String getAttribute (final String id)
  {
    return getAttributes ().get (id);
  }

  public void setAttributes (final Map <String, String> attributes)
  {
    m_aAttributes = attributes;
  }

  public Map <String, String> getAttributes ()
  {
    if (m_aAttributes == null)
    {
      m_aAttributes = new HashMap <String, String> ();
    }

    return m_aAttributes;
  }

  public void setReceiverID (final String id, final String value)
  {
    getReceiverIDs ().put (id, value);
  }

  public String getReceiverID (final String id)
  {
    return getReceiverIDs ().get (id);
  }

  public void setReceiverIDs (final Map <String, String> receiverIDs)
  {
    m_aReceiverIDs = receiverIDs;
  }

  public Map <String, String> getReceiverIDs ()
  {
    if (m_aReceiverIDs == null)
    {
      m_aReceiverIDs = new HashMap <String, String> ();
    }

    return m_aReceiverIDs;
  }

  public void setSenderID (final String id, final String value)
  {
    getSenderIDs ().put (id, value);
  }

  public String getSenderID (final String id)
  {
    return getSenderIDs ().get (id);
  }

  public void setSenderIDs (final Map <String, String> senderIDs)
  {
    m_aSenderIDs = senderIDs;
  }

  public Map <String, String> getSenderIDs ()
  {
    if (m_aSenderIDs == null)
    {
      m_aSenderIDs = new HashMap <String, String> ();
    }

    return m_aSenderIDs;
  }

  public boolean matches (final Partnership partnership)
  {
    final Map <String, String> senderIDs = partnership.getSenderIDs ();
    final Map <String, String> receiverIDs = partnership.getReceiverIDs ();

    if (compareIDs (senderIDs, getSenderIDs ()))
    {
      return true;
    }
    else
      if (compareIDs (receiverIDs, getReceiverIDs ()))
      {
        return true;
      }

    return false;
  }

  @Override
  public String toString ()
  {
    final StringBuilder buf = new StringBuilder ();
    buf.append ("Partnership " + getName ());
    buf.append (" Sender IDs = ").append (getSenderIDs ());
    buf.append (" Receiver IDs = ").append (getReceiverIDs ());
    buf.append (" Attributes = ").append (getAttributes ());

    return buf.toString ();
  }

  protected boolean compareIDs (final Map <String, String> ids, final Map <String, String> compareTo)
  {
    if (ids.isEmpty ())
    {
      return false;
    }

    for (final Map.Entry <String, String> currentId : ids.entrySet ())
    {
      final String currentValue = currentId.getValue ();
      final String compareValue = compareTo.get (currentId.getKey ());

      if (!EqualsUtils.equals (currentValue, compareValue))
        return false;
    }

    return true;
  }

  public void copy (final Partnership partnership)
  {
    if (partnership.getName () != null)
    {
      setName (partnership.getName ());
    }
    getSenderIDs ().putAll (partnership.getSenderIDs ());
    getReceiverIDs ().putAll (partnership.getReceiverIDs ());
    getAttributes ().putAll (partnership.getAttributes ());
  }
}
