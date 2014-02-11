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
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedException;
import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.StringMap;
import com.phloc.commons.io.streams.NonBlockingByteArrayOutputStream;

public abstract class AbstractMessage extends AbstractBaseMessage implements IMessage
{
  private IMessageMDN m_aMDN;
  private MimeBodyPart m_aData;

  public AbstractMessage ()
  {}

  public void setContentType (final String sContentType)
  {
    setHeader (CAS2Header.HEADER_CONTENT_TYPE, sContentType);
  }

  public String getContentType ()
  {
    return getHeader (CAS2Header.HEADER_CONTENT_TYPE);
  }

  /**
   * @since 2007-06-01
   * @param sContentDisposition
   */
  public void setContentDisposition (final String sContentDisposition)
  {
    setHeader (CAS2Header.HEADER_CONTENT_DISPOSITION, sContentDisposition);
  }

  /**
   * @since 2007-06-01
   */
  public String getContentDisposition ()
  {
    return getHeader (CAS2Header.HEADER_CONTENT_DISPOSITION);
  }

  public void setData (@Nullable final MimeBodyPart aData, @Nullable final DataHistoryItem aHistoryItem)
  {
    m_aData = aData;
    if (aData != null)
    {
      try
      {
        setContentType (aData.getContentType ());
      }
      catch (final MessagingException ex)
      {
        setContentType (null);
      }
      try
      {
        setContentDisposition (aData.getHeader (CAS2Header.HEADER_CONTENT_DISPOSITION, null));
      }
      catch (final MessagingException ex)
      {
        setContentDisposition (null);
      }
    }

    if (aHistoryItem != null)
      getHistory ().addItem (aHistoryItem);
  }

  @Nonnull
  public DataHistoryItem setData (@Nonnull final MimeBodyPart aData) throws OpenAS2Exception
  {
    try
    {
      final DataHistoryItem aHistoryItem = new DataHistoryItem (aData.getContentType ());
      setData (aData, aHistoryItem);
      return aHistoryItem;
    }
    catch (final Exception ex)
    {
      throw new WrappedException (ex);
    }
  }

  public MimeBodyPart getData ()
  {
    return m_aData;
  }

  public void setMDN (@Nullable final IMessageMDN aMDN)
  {
    m_aMDN = aMDN;
  }

  @Nullable
  public IMessageMDN getMDN ()
  {
    return m_aMDN;
  }

  public abstract String generateMessageID ();

  public void setSubject (final String sSubject)
  {
    setHeader (CAS2Header.HEADER_SUBJECT, sSubject);
  }

  public String getSubject ()
  {
    return getHeader (CAS2Header.HEADER_SUBJECT);
  }

  @Override
  public String toString ()
  {
    final StringBuilder aSB = new StringBuilder ();
    aSB.append ("Message From:").append (getPartnership ().getSenderIDs ());
    aSB.append ("\nTo:").append (getPartnership ().getReceiverIDs ());

    aSB.append ("\nHeaders:{");
    final Enumeration <?> aHeaders = getHeaders ().getAllHeaders ();
    while (aHeaders.hasMoreElements ())
    {
      final Header aHeader = (Header) aHeaders.nextElement ();
      aSB.append (aHeader.getName ()).append ("=").append (aHeader.getValue ());
      if (aHeaders.hasMoreElements ())
        aSB.append (", ");
    }
    aSB.append ("}").append ("\nAttributes:").append (getAllAttributes ());

    final IMessageMDN aMDN = getMDN ();
    if (aMDN != null)
      aSB.append ("\nMDN:").append (aMDN.toString ());

    return aSB.toString ();
  }

  @SuppressWarnings ("unchecked")
  private void readObject (final ObjectInputStream aOIS) throws IOException, ClassNotFoundException
  {
    // read in partnership
    m_aPartnership = (Partnership) aOIS.readObject ();

    // read in attributes
    m_aAttributes = (StringMap) aOIS.readObject ();

    // read in data history
    m_aHistory = (DataHistory) aOIS.readObject ();

    try
    {
      // read in message headers
      m_aHeaders = new InternetHeaders (aOIS);

      // read in mime body
      if (aOIS.read () == 1)
        m_aData = new MimeBodyPart (aOIS);
    }
    catch (final MessagingException ex)
    {
      throw new IOException ("Messaging exception: " + ex.getMessage ());
    }

    // read in MDN
    m_aMDN = (IMessageMDN) aOIS.readObject ();
    if (m_aMDN != null)
      m_aMDN.setMessage (this);
  }

  private void writeObject (@Nonnull final ObjectOutputStream aOOS) throws IOException
  {
    // write partnership info
    aOOS.writeObject (m_aPartnership);

    // write attributes
    aOOS.writeObject (m_aAttributes);

    // write data history
    aOOS.writeObject (m_aHistory);

    // write message headers
    final Enumeration <?> en = m_aHeaders.getAllHeaderLines ();
    while (en.hasMoreElements ())
    {
      aOOS.writeBytes (en.nextElement ().toString () + "\r\n");
    }

    aOOS.writeBytes ("\r\n");

    // write the mime body
    final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();
    try
    {
      if (m_aData != null)
      {
        aBAOS.write (1);
        m_aData.writeTo (aBAOS);
      }
      else
      {
        aBAOS.write (0);
      }
    }
    catch (final MessagingException ex)
    {
      throw new IOException ("Messaging exception: " + ex.getMessage ());
    }

    aOOS.write (aBAOS.toByteArray ());
    aBAOS.close ();

    // write the message's MDN
    aOOS.writeObject (m_aMDN);
  }

  public String getLoggingText ()
  {
    return " [" + getMessageID () + "]";
  }
}
