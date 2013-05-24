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

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;

import com.helger.as2lib.partner.Partnership;

public abstract class AbstractMessageMDN implements IMessageMDN
{
  private DataHistory m_aHistory;
  private InternetHeaders m_aHeaders;
  private Partnership m_aPartnership;
  private Map <String, String> m_aAttributes;
  private IMessage m_aMessage;
  private MimeBodyPart m_aData;
  private String m_sText;

  public AbstractMessageMDN (final IMessage msg)
  {
    super ();
    m_aMessage = msg;
    msg.setMDN (this);
  }

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

  public void setData (final MimeBodyPart data)
  {
    m_aData = data;
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
    {
      m_aHeaders = new InternetHeaders ();
    }

    return m_aHeaders;
  }

  public void setMessage (final IMessage message)
  {
    m_aMessage = message;
  }

  public IMessage getMessage ()
  {
    return m_aMessage;
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

  public Partnership getPartnership ()
  {
    if (m_aPartnership == null)
    {
      m_aPartnership = new Partnership ();
    }

    return m_aPartnership;
  }

  public void setText (final String text)
  {
    m_sText = text;
  }

  public String getText ()
  {
    return m_sText;
  }

  public void addHeader (final String key, final String value)
  {
    getHeaders ().addHeader (key, value);
  }

  public abstract String generateMessageID ();

  public void setHistory (final DataHistory history)
  {
    m_aHistory = history;
  }

  public DataHistory getHistory ()
  {
    if (m_aHistory == null)
    {
      m_aHistory = new DataHistory ();
    }

    return m_aHistory;
  }

  @Override
  public String toString ()
  {
    final StringBuilder buf = new StringBuilder ();
    buf.append ("MDN From:").append (getPartnership ().getSenderIDs ());
    buf.append ("To:").append (getPartnership ().getReceiverIDs ());

    final Enumeration <?> headerEn = getHeaders ().getAllHeaders ();
    buf.append ("\r\nHeaders:{");

    while (headerEn.hasMoreElements ())
    {
      final Header header = (Header) headerEn.nextElement ();
      buf.append (header.getName ()).append ("=").append (header.getValue ());

      if (headerEn.hasMoreElements ())
      {
        buf.append (", ");
      }
    }

    buf.append ("}");
    buf.append ("\r\nAttributes:").append (getAttributes ());
    buf.append ("\r\nText: \r\n");
    buf.append (getText ()).append ("\r\n");

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

    // read in text
    m_sText = (String) in.readObject ();

    try
    {
      // read in message headers
      m_aHeaders = new InternetHeaders (in);

      // read in mime body
      if (in.read () == 1)
      {
        m_aData = new MimeBodyPart (in);
      }
      else
      {
        m_aData = null;
      }
    }
    catch (final MessagingException me)
    {
      throw new IOException ("Messaging exception: " + me.getMessage ());
    }
  }

  private void writeObject (final java.io.ObjectOutputStream out) throws IOException
  {
    // write partnership info
    out.writeObject (m_aPartnership);

    // write attributes
    out.writeObject (m_aAttributes);

    // write text
    out.writeObject (m_sText);

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
  }
}
