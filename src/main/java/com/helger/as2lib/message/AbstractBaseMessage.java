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
package com.helger.as2lib.message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.internet.InternetHeaders;

import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.util.StringMap;
import com.phloc.commons.annotations.ReturnsMutableCopy;

public abstract class AbstractBaseMessage implements IBaseMessage
{
  public static final String HEADER_MESSAGE_ID = "Message-ID";

  protected StringMap m_aAttributes = new StringMap ();
  protected InternetHeaders m_aHeaders = new InternetHeaders ();
  protected DataHistory m_aHistory = new DataHistory ();
  protected Partnership m_aPartnership = new Partnership ();

  public AbstractBaseMessage ()
  {}

  public final void setAttribute (final String sKey, final String sValue)
  {
    m_aAttributes.setAttribute (sKey, sValue);
  }

  @Nullable
  public final String getAttribute (final String sKey)
  {
    return m_aAttributes.getAttributeAsString (sKey);
  }

  public final void setAttributes (@Nullable final StringMap aAttributes)
  {
    m_aAttributes.setAttributes (aAttributes != null ? aAttributes.getAllAttributes () : null);
  }

  @Nonnull
  @ReturnsMutableCopy
  public final StringMap getAllAttributes ()
  {
    return m_aAttributes.getClone ();
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
    if (aHeaders == null)
      m_aHeaders = new InternetHeaders ();
    else
      m_aHeaders = aHeaders;
  }

  @Nonnull
  public final InternetHeaders getHeaders ()
  {
    return m_aHeaders;
  }

  public final void addHeader (final String sKey, final String sValue)
  {
    m_aHeaders.addHeader (sKey, sValue);
  }

  public final void setMessageID (final String sMessageID)
  {
    setHeader (HEADER_MESSAGE_ID, sMessageID);
  }

  public final String getMessageID ()
  {
    return getHeader (HEADER_MESSAGE_ID);
  }

  public final void updateMessageID ()
  {
    setMessageID (generateMessageID ());
  }

  @Nonnull
  public final DataHistory getHistory ()
  {
    return m_aHistory;
  }

  public final void setPartnership (@Nullable final Partnership aPartnership)
  {
    if (aPartnership != null)
      m_aPartnership = aPartnership;
    else
      m_aPartnership = new Partnership ();
  }

  @Nonnull
  public final Partnership getPartnership ()
  {
    return m_aPartnership;
  }
}
