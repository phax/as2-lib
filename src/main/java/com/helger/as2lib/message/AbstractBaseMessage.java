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
package com.helger.as2lib.message;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.internet.InternetHeaders;

import com.helger.as2lib.partner.Partnership;

public abstract class AbstractBaseMessage implements IBaseMessage
{
  protected Map <String, String> m_aAttributes;
  protected InternetHeaders m_aHeaders;
  protected DataHistory m_aHistory;
  protected Partnership m_aPartnership;

  public AbstractBaseMessage ()
  {}

  public final void setAttribute (final String sKey, final String sValue)
  {
    getAttributes ().put (sKey, sValue);
  }

  @Nullable
  public final String getAttribute (final String sKey)
  {
    return getAttributes ().get (sKey);
  }

  public final void setAttributes (@Nullable final Map <String, String> aAttributes)
  {
    m_aAttributes = aAttributes;
  }

  @Nonnull
  public final Map <String, String> getAttributes ()
  {
    if (m_aAttributes == null)
      m_aAttributes = new HashMap <String, String> ();
    return m_aAttributes;
  }

  public final void setHeader (final String sKey, final String sValue)
  {
    getHeaders ().setHeader (sKey, sValue);
  }

  public final String getHeader (final String sKey)
  {
    return getHeader (sKey, ", ");
  }

  public final String getHeader (final String sKey, final String sDelimiter)
  {
    return getHeaders ().getHeader (sKey, sDelimiter);
  }

  public final void setHeaders (@Nullable final InternetHeaders aHeaders)
  {
    m_aHeaders = aHeaders;
  }

  @Nonnull
  public final InternetHeaders getHeaders ()
  {
    if (m_aHeaders == null)
      m_aHeaders = new InternetHeaders ();
    return m_aHeaders;
  }

  public final void setMessageID (final String sMessageID)
  {
    setHeader ("Message-ID", sMessageID);
  }

  public final String getMessageID ()
  {
    return getHeader ("Message-ID");
  }

  public final void setPartnership (@Nullable final Partnership aPartnership)
  {
    m_aPartnership = aPartnership;
  }

  @Nonnull
  public final Partnership getPartnership ()
  {
    if (m_aPartnership == null)
      m_aPartnership = new Partnership ();
    return m_aPartnership;
  }

  public final void addHeader (final String sKey, final String sValue)
  {
    getHeaders ().addHeader (sKey, sValue);
  }

  public final void setHistory (@Nullable final DataHistory aHistory)
  {
    m_aHistory = aHistory;
  }

  @Nonnull
  public final DataHistory getHistory ()
  {
    if (m_aHistory == null)
      m_aHistory = new DataHistory ();
    return m_aHistory;
  }

  public final void updateMessageID ()
  {
    setMessageID (generateMessageID ());
  }
}
