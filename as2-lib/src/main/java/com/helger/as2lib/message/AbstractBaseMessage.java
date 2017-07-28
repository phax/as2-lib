/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2017 Philip Helger philip[at]helger[dot]com
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
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.util.StringMap;
import com.helger.as2lib.util.http.HTTPHelper;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.string.ToStringGenerator;

/**
 * Base implementation of {@link IBaseMessage} as the base class for
 * {@link AbstractMessage} and {@link AbstractMessageMDN}.
 *
 * @author Philip Helger
 */
public abstract class AbstractBaseMessage implements IBaseMessage
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AbstractBaseMessage.class);

  private StringMap m_aAttributes = new StringMap ();
  private InternetHeaders m_aHeaders = new InternetHeaders ();
  private Partnership m_aPartnership = Partnership.createPlaceholderPartnership ();

  public AbstractBaseMessage ()
  {}

  private void readObject (@Nonnull final ObjectInputStream aOIS) throws IOException, ClassNotFoundException
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

  private void writeObject (@Nonnull final ObjectOutputStream aOOS) throws IOException
  {
    // write attributes
    aOOS.writeObject (m_aAttributes);

    // write message headers
    final Enumeration <?> en = m_aHeaders.getAllHeaderLines ();
    while (en.hasMoreElements ())
    {
      aOOS.write (((String) en.nextElement () + HTTPHelper.EOL).getBytes (StandardCharsets.ISO_8859_1));
    }

    aOOS.write (HTTPHelper.EOL.getBytes (StandardCharsets.ISO_8859_1));

    // write partnership info
    aOOS.writeObject (m_aPartnership);
  }

  public final StringMap attrs ()
  {
    return m_aAttributes;
  }

  public final void setHeader (@Nonnull final String sKey, @Nullable final String sValue)
  {
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Setting message header: '" + sKey + "' = '" + sValue + "'");
    m_aHeaders.setHeader (sKey, sValue);
  }

  public final void addHeader (@Nonnull final String sKey, @Nullable final String sValue)
  {
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Adding message header: '" + sKey + "' = '" + sValue + "'");
    m_aHeaders.addHeader (sKey, sValue);
  }

  @Nullable
  public final String getHeader (@Nonnull final String sKey, @Nullable final String sDelimiter)
  {
    return m_aHeaders.getHeader (sKey, sDelimiter);
  }

  public final void setHeaders (@Nullable final InternetHeaders aHeaders)
  {
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Setting all message headers " + aHeaders);
    if (aHeaders == null)
      m_aHeaders = new InternetHeaders ();
    else
      m_aHeaders = aHeaders;
  }

  @Nonnull
  @ReturnsMutableObject
  public final InternetHeaders headers ()
  {
    return m_aHeaders;
  }

  public final void setPartnership (@Nonnull final Partnership aPartnership)
  {
    ValueEnforcer.notNull (aPartnership, "Partnership");
    m_aPartnership = aPartnership;
  }

  @Nonnull
  @ReturnsMutableObject
  public final Partnership partnership ()
  {
    return m_aPartnership;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("attributes", m_aAttributes)
                                       .append ("headers", m_aHeaders)
                                       .append ("partnership", m_aPartnership)
                                       .getToString ();
  }
}
