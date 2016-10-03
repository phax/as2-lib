/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2016 Philip Helger philip[at]helger[dot]com
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.util.CAS2Header;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.string.ToStringGenerator;

/**
 * Abstract base implementation of the {@link IMessage} interface.
 *
 * @author Philip Helger
 */
public abstract class AbstractMessage extends AbstractBaseMessage implements IMessage
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AbstractMessage.class);

  private MimeBodyPart m_aData;
  private IMessageMDN m_aMDN;

  public AbstractMessage ()
  {}

  @Nullable
  public String getAS2From ()
  {
    return getHeader (CAS2Header.HEADER_AS2_FROM);
  }

  @Nullable
  public String getAS2To ()
  {
    return getHeader (CAS2Header.HEADER_AS2_TO);
  }

  public void setContentType (@Nullable final String sContentType)
  {
    setHeader (CAS2Header.HEADER_CONTENT_TYPE, sContentType);
  }

  @Nullable
  public String getContentType ()
  {
    return getHeader (CAS2Header.HEADER_CONTENT_TYPE);
  }

  /**
   * @since 2007-06-01
   * @param sContentDisposition
   *        Content disposition
   */
  public void setContentDisposition (@Nullable final String sContentDisposition)
  {
    setHeader (CAS2Header.HEADER_CONTENT_DISPOSITION, sContentDisposition);
  }

  /**
   * @since 2007-06-01
   */
  @Nullable
  public String getContentDisposition ()
  {
    return getHeader (CAS2Header.HEADER_CONTENT_DISPOSITION);
  }

  public void setSubject (@Nullable final String sSubject)
  {
    setHeader (CAS2Header.HEADER_SUBJECT, sSubject);
  }

  @Nullable
  public String getSubject ()
  {
    return getHeader (CAS2Header.HEADER_SUBJECT);
  }

  public void setData (@Nullable final MimeBodyPart aData)
  {
    // Remember data
    m_aData = aData;

    if (aData != null)
    {
      // Set content type from data
      try
      {
        setContentType (aData.getContentType ());
      }
      catch (final MessagingException ex)
      {
        s_aLogger.warn ("Failed to set the Content-Type from the MimeBodyPart. Defaulting to null.");
        setContentType (null);
      }

      // Set content disposition from data
      try
      {
        setContentDisposition (aData.getHeader (CAS2Header.HEADER_CONTENT_DISPOSITION, null));
      }
      catch (final MessagingException ex)
      {
        s_aLogger.warn ("Failed to set the Content-Disposition from the MimeBodyPart. Defaulting to null.");
        setContentDisposition (null);
      }
    }
  }

  @Nullable
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

  @Nonnull
  @Nonempty
  public String getLoggingText ()
  {
    final String sMsgID = getMessageID ();
    return " [" + (sMsgID == null ? "no message ID set" : sMsgID) + "]";
  }

  @Nonnull
  @Nonempty
  public String getAsString ()
  {
    final StringBuilder aSB = new StringBuilder ();
    aSB.append ("Message From:").append (getPartnership ().getAllSenderIDs ().getAllAttributes ());
    aSB.append ("\nTo:").append (getPartnership ().getAllReceiverIDs ().getAllAttributes ());

    aSB.append ("\nHeaders:")
       .append (getHeadersDebugFormatted ())
       .append ("\nAttributes:")
       .append (getAllAttributes ().getAllAttributes ());

    final IMessageMDN aMDN = getMDN ();
    if (aMDN != null)
      aSB.append ("\nMDN:").append (aMDN.getAsString ());

    return aSB.toString ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("data", m_aData).append ("MDN", m_aMDN).toString ();
  }

  private void readObject (@Nonnull final ObjectInputStream aOIS) throws IOException, ClassNotFoundException
  {
    try
    {
      // read in mime body
      if (aOIS.read () == 1)
        m_aData = new MimeBodyPart (aOIS);
    }
    catch (final MessagingException ex)
    {
      throw new IOException ("Messaging exception", ex);
    }

    // read in MDN
    m_aMDN = (IMessageMDN) aOIS.readObject ();
    if (m_aMDN != null)
      m_aMDN.setMessage (this);
  }

  private void writeObject (@Nonnull final ObjectOutputStream aOOS) throws IOException
  {
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
}
