/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2015 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.client;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.CMimeType;

/**
 * This class represents the content of an AS2 client request.
 *
 * @author oleo Date: May 12, 2010 Time: 5:48:26 PM
 * @author Philip Helger
 */
public class AS2ClientRequest
{
  public static final String DEFAULT_CONTENT_TYPE = CMimeType.APPLICATION_XML.getAsString ();

  // Content type
  private String m_sContentType = DEFAULT_CONTENT_TYPE;
  private final String m_sSubject;
  // Set either text or filename or stream
  // Precedence: byte[] before text
  private byte [] m_aData;
  private String m_sText;
  private Charset m_aCharset;

  /**
   * @param sSubject
   *        The subject to use. May neither be <code>null</code> nor empty.
   */
  public AS2ClientRequest (@Nonnull @Nonempty final String sSubject)
  {
    m_sSubject = ValueEnforcer.notEmpty (sSubject, "Subject");
  }

  public AS2ClientRequest setContentType (@Nonnull @Nonempty final String sContentType)
  {
    m_sContentType = ValueEnforcer.notEmpty (sContentType, "ContentType");
    return this;
  }

  @Nonnull
  @Nonempty
  public String getContentType ()
  {
    return m_sContentType;
  }

  @Nonnull
  @Nonempty
  public String getSubject ()
  {
    return m_sSubject;
  }

  @Nonnull
  public AS2ClientRequest setData (@Nonnull final File aFile)
  {
    return setData (FileHelper.getInputStream (aFile));
  }

  @Nonnull
  public AS2ClientRequest setData (@Nonnull final InputStream aIS)
  {
    ValueEnforcer.notNull (aIS, "InputStream");
    return setData (StreamHelper.getAllBytes (aIS));
  }

  @Nonnull
  public AS2ClientRequest setData (@Nonnull final byte [] aData)
  {
    m_aData = ValueEnforcer.notNull (aData, "Data");
    m_sText = null;
    m_aCharset = null;
    return this;
  }

  @Nonnull
  public AS2ClientRequest setData (@Nonnull final String sText, @Nullable final Charset aCharset)
  {
    m_aData = null;
    m_sText = ValueEnforcer.notNull (sText, "Text");
    m_aCharset = aCharset;
    return this;
  }

  public void applyDataOntoMimeBodyPart (final MimeBodyPart aPart) throws MessagingException
  {
    if (m_aData != null)
    {
      // Set content with a specific MIME type
      aPart.setContent (m_aData, m_sContentType);
    }
    else
      if (m_sText != null)
      {
        // Set text with an optional charset
        aPart.setText (m_sText, m_aCharset == null ? null : m_aCharset.name ());
      }
      else
        throw new IllegalStateException ("No data specified in AS2 client request!");
  }
}
