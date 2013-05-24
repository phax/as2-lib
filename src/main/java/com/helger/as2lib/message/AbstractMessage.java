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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedException;
import com.helger.as2lib.partner.Partnership;

public abstract class AbstractMessage implements IMessage
{
  private DataHistory m_aHistory;
  private InternetHeaders m_aHeaders;
  private Map <String, String> m_aAttributes;
  private IMessageMDN m_aMDN;
  private MimeBodyPart m_aData;
  private Partnership m_aPartnership;

  public AbstractMessage ()
  {}

  public void setAttribute (final String key, final String value)
  {
    getAttributes ().put (key, value);
  }

  public String getAttribute (final String key)
  {
    return getAttributes ().get (key);
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

  public void setContentType (final String contentType)
  {
    setHeader ("Content-Type", contentType);
  }

  public String getContentType ()
  {
    return getHeader ("Content-Type");
  }

  /**
   * @since 2007-06-01
   * @param contentDisposition
   */
  public void setContentDisposition (final String contentDisposition)
  {
    setHeader ("Content-Disposition", contentDisposition);
  }

  /**
   * @since 2007-06-01
   */
  public String getContentDisposition ()
  {
    return getHeader ("Content-Disposition");
  }

  public void setData (final MimeBodyPart data, final DataHistoryItem historyItem)
  {
    m_aData = data;

    if (data != null)
    {
      try
      {
        setContentType (data.getContentType ());
      }
      catch (final MessagingException e)
      {
        setContentType (null);
      }
      try
      {
        setContentDisposition (data.getHeader ("Content-Disposition", null));
      }
      catch (final MessagingException e)
      {
        setContentDisposition (null);
      }
    }

    if (historyItem != null)
    {
      getHistory ().addItem (historyItem);
    }
  }

  public DataHistoryItem setData (final MimeBodyPart data) throws OpenAS2Exception
  {
    try
    {
      final DataHistoryItem historyItem = new DataHistoryItem (data.getContentType ());
      setData (data, historyItem);
      return historyItem;
    }
    catch (final Exception e)
    {
      throw new WrappedException (e);
    }
  }

  public MimeBodyPart getData ()
  {
    return m_aData;
  }

  public void setHeader (final String key, final String value)
  {
    getHeaders ().setHeader (key, value);
  }

  public String getHeader (final String key)
  {
    return getHeader (key, ", ");
  }

  public String getHeader (final String key, final String delimiter)
  {
    return getHeaders ().getHeader (key, delimiter);
  }

  public void setHeaders (final InternetHeaders headers)
  {
    m_aHeaders = headers;
  }

  public InternetHeaders getHeaders ()
  {
    if (m_aHeaders == null)
      m_aHeaders = new InternetHeaders ();
    return m_aHeaders;
  }

  public void setHistory (final DataHistory history)
  {
    m_aHistory = history;
  }

  public DataHistory getHistory ()
  {
    if (m_aHistory == null)
      m_aHistory = new DataHistory ();
    return m_aHistory;
  }

  public void setMDN (final IMessageMDN mdn)
  {
    m_aMDN = mdn;
  }

  public IMessageMDN getMDN ()
  {
    return m_aMDN;
  }

  public void setMessageID (final String messageID)
  {
    setHeader ("Message-ID", messageID);
  }

  public String getMessageID ()
  {
    return getHeader ("Message-ID");
  }

  public void setPartnership (final Partnership partnership)
  {
    m_aPartnership = partnership;
  }

  @Nonnull
  public Partnership getPartnership ()
  {
    if (m_aPartnership == null)
      m_aPartnership = new Partnership ();
    return m_aPartnership;
  }

  public abstract String generateMessageID ();

  public void setSubject (final String subject)
  {
    setHeader ("Subject", subject);
  }

  public String getSubject ()
  {
    return getHeader ("Subject");
  }

  public void addHeader (final String key, final String value)
  {
    getHeaders ().addHeader (key, value);
  }

  @Override
  public String toString ()
  {
    final StringBuilder buf = new StringBuilder ();
    buf.append ("Message From:").append (getPartnership ().getSenderIDs ());
    buf.append ("\nTo:").append (getPartnership ().getReceiverIDs ());

    final Enumeration <?> headerEn = getHeaders ().getAllHeaders ();
    buf.append ("\nHeaders:{");

    while (headerEn.hasMoreElements ())
    {
      final Header header = (Header) headerEn.nextElement ();
      buf.append (header.getName ()).append ("=").append (header.getValue ());
      if (headerEn.hasMoreElements ())
        buf.append (", ");
    }

    buf.append ("}");
    buf.append ("\nAttributes:").append (getAttributes ());

    final IMessageMDN mdn = getMDN ();
    if (mdn != null)
      buf.append ("\nMDN:").append (mdn.toString ());

    return buf.toString ();
  }

  public void updateMessageID ()
  {
    setMessageID (generateMessageID ());
  }

  @SuppressWarnings ("unchecked")
  private void readObject (final java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
  {
    // read in partnership
    m_aPartnership = (Partnership) in.readObject ();

    // read in attributes
    m_aAttributes = (Map <String, String>) in.readObject ();

    // read in data history
    m_aHistory = (DataHistory) in.readObject ();

    try
    {
      // read in message headers
      m_aHeaders = new InternetHeaders (in);

      // read in mime body
      if (in.read () == 1)
      {
        m_aData = new MimeBodyPart (in);
      }
    }
    catch (final MessagingException me)
    {
      throw new IOException ("Messaging exception: " + me.getMessage ());
    }

    // read in MDN
    m_aMDN = (IMessageMDN) in.readObject ();

    if (m_aMDN != null)
    {
      m_aMDN.setMessage (this);
    }
  }

  private void writeObject (final java.io.ObjectOutputStream out) throws IOException
  {
    // write partnership info
    out.writeObject (m_aPartnership);

    // write attributes
    out.writeObject (m_aAttributes);

    // write data history
    out.writeObject (m_aHistory);

    // write message headers
    final Enumeration <?> en = m_aHeaders.getAllHeaderLines ();
    while (en.hasMoreElements ())
    {
      out.writeBytes (en.nextElement ().toString () + "\r\n");
    }

    out.writeBytes (new String ("\r\n"));

    // write the mime body
    final ByteArrayOutputStream baos = new ByteArrayOutputStream ();

    try
    {
      if (m_aData != null)
      {
        baos.write (1);
        m_aData.writeTo (baos);
      }
      else
      {
        baos.write (0);
      }
    }
    catch (final MessagingException e)
    {
      throw new IOException ("Messaging exception: " + e.getMessage ());
    }

    out.write (baos.toByteArray ());
    baos.close ();

    // write the message's MDN
    out.writeObject (m_aMDN);
  }

  public String getLoggingText ()
  {
    return " [" + getMessageID () + "]";
  }
}
