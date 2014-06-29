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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;

import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.StringMap;
import com.phloc.commons.annotations.ReturnsMutableCopy;
import com.phloc.commons.string.ToStringGenerator;

/**
 * Base implementation of {@link IBaseMessage} as the base class for
 * {@link AbstractMessage} and {@link AbstractMessageMDN}.
 * 
 * @author Philip Helger
 */
public abstract class AbstractBaseMessage implements IBaseMessage
{
  private StringMap m_aAttributes = new StringMap ();
  private InternetHeaders m_aHeaders = new InternetHeaders ();
  private Partnership m_aPartnership = new Partnership ();

  public AbstractBaseMessage ()
  {}

  protected final void baseReadObject (final ObjectInputStream aOIS) throws IOException, ClassNotFoundException
  {
    // read in attributes
    m_aAttributes = (StringMap) aOIS.readObject ();

    try
    {
      // read in message headers
      m_aHeaders = new InternetHeaders (aOIS);
    }
    catch (final MessagingException ex)
    {
      throw new IOException ("Messaging exception", ex);
    }

    // read in partnership
    m_aPartnership = (Partnership) aOIS.readObject ();
  }

  protected final void baseWriteObject (@Nonnull final ObjectOutputStream aOOS) throws IOException
  {
    // write attributes
    aOOS.writeObject (m_aAttributes);

    // write message headers
    final Enumeration <?> en = m_aHeaders.getAllHeaderLines ();
    while (en.hasMoreElements ())
    {
      aOOS.writeBytes ((String) en.nextElement () + "\r\n");
    }

    aOOS.writeBytes ("\r\n");

    // write partnership info
    aOOS.writeObject (m_aPartnership);
  }

  public final void setAttribute (@Nonnull final String sKey, @Nullable final String sValue)
  {
    m_aAttributes.setAttribute (sKey, sValue);
  }

  @Nullable
  public final String getAttribute (@Nullable final String sKey)
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

  public final void setHeader (@Nonnull final String sKey, @Nullable final String sValue)
  {
    m_aHeaders.setHeader (sKey, sValue);
  }

  @Nullable
  public final String getHeader (@Nonnull final String sKey)
  {
    return getHeader (sKey, ", ");
  }

  @Nullable
  public final String getHeader (@Nonnull final String sKey, @Nullable final String sDelimiter)
  {
    return m_aHeaders.getHeader (sKey, sDelimiter);
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

  public final void addHeader (@Nonnull final String sKey, @Nullable final String sValue)
  {
    m_aHeaders.addHeader (sKey, sValue);
  }

  public final void setMessageID (@Nullable final String sMessageID)
  {
    setHeader (CAS2Header.HEADER_MESSAGE_ID, sMessageID);
  }

  @Nullable
  public final String getMessageID ()
  {
    return getHeader (CAS2Header.HEADER_MESSAGE_ID);
  }

  public final void updateMessageID ()
  {
    setMessageID (generateMessageID ());
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

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("attributes", m_aAttributes)
                                       .append ("headers", m_aHeaders)
                                       .append ("partnership", m_aPartnership)
                                       .toString ();
  }
}
