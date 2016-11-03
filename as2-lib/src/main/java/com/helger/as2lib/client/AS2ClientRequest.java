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
package com.helger.as2lib.client;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.activation.DataHandler;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;
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
  private byte [] m_aDataByteArray;
  private String m_sDataText;
  private Charset m_aDataCharset;
  // DataHandler is not Serializable
  private DataHandler m_aDataHandler;
  private String m_sFilename;

  /**
   * @param sSubject
   *        The subject to use. May neither be <code>null</code> nor empty.
   */
  public AS2ClientRequest (@Nonnull @Nonempty final String sSubject)
  {
    m_sSubject = ValueEnforcer.notEmpty (sSubject, "Subject");
  }

  /**
   * @return The subject as provided in the constructor. May neither be
   *         <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public String getSubject ()
  {
    return m_sSubject;
  }

  /**
   * Set the content type to be used.
   *
   * @param sContentType
   *        The content type. May neither be <code>null</code> nor empty.
   * @return this
   */
  @Nonnull
  public AS2ClientRequest setContentType (@Nonnull @Nonempty final String sContentType)
  {
    m_sContentType = ValueEnforcer.notEmpty (sContentType, "ContentType");
    return this;
  }

  /**
   * @return The content type to be used. Defaults to
   *         {@link #DEFAULT_CONTENT_TYPE}.
   */
  @Nonnull
  @Nonempty
  public String getContentType ()
  {
    return m_sContentType;
  }

  /**
   * Set the content of the {@link File} as a payload. No charset is applied and
   * therefore no content type starting with "text/" may be used. The name of
   * the file is used as the payload file name.
   *
   * @param aFile
   *        {@link File} to read the content from. Never <code>null</code>.
   * @return this
   */
  @Nonnull
  public AS2ClientRequest setData (@Nonnull final File aFile)
  {
    return setData (aFile, (Charset) null);
  }

  /**
   * Set the content of the {@link File} as a payload. If no charset is applied
   * ( <code>null</code>) no content type starting with "text/" may be used. The
   * name of the file is used as the payload file name.
   *
   * @param aFile
   *        {@link File} to read the content from. Never <code>null</code>.
   * @param aCharset
   *        Charset to use. If it is <code>null</code> the content is set as a
   *        byte array, if not <code>null</code> the content is set as a String.
   * @return this
   */
  @Nonnull
  public AS2ClientRequest setData (@Nonnull final File aFile, @Nullable final Charset aCharset)
  {
    ValueEnforcer.notNull (aFile, "File");
    setData (FileHelper.getInputStream (aFile), aCharset);

    // Set filename by default
    setFilename (aFile.getName ());
    return this;
  }

  /**
   * Set the content of the {@link InputStream} as a payload. No charset is
   * applied and therefore no content type starting with "text/" may be used.
   *
   * @param aIS
   *        {@link InputStream} to read the content from. Never
   *        <code>null</code>.
   * @return this
   */
  @Nonnull
  public AS2ClientRequest setData (@Nonnull @WillClose final InputStream aIS)
  {
    return setData (aIS, (Charset) null);
  }

  /**
   * Set the content of the {@link InputStream} as a payload. No charset is
   * applied and therefore no content type starting with "text/" may be used.
   *
   * @param aIS
   *        {@link InputStream} to read the content from. Never
   *        <code>null</code>.
   * @param aCharset
   *        Charset to use. If it is <code>null</code> the content is set as a
   *        byte array, if not <code>null</code> the content is set as a String.
   * @return this
   */
  @Nonnull
  public AS2ClientRequest setData (@Nonnull @WillClose final InputStream aIS, @Nullable final Charset aCharset)
  {
    ValueEnforcer.notNull (aIS, "InputStream");
    final byte [] aBytes = StreamHelper.getAllBytes (aIS);
    if (aCharset == null)
    {
      // Set pure byte array
      return setData (aBytes);
    }

    // Convert to String and remember charset
    return setData (new String (aBytes, aCharset), aCharset);
  }

  @Nonnull
  public AS2ClientRequest setData (@Nonnull final byte [] aData)
  {
    m_aDataByteArray = ValueEnforcer.notNull (aData, "Data");
    m_sDataText = null;
    m_aDataCharset = null;
    m_aDataHandler = null;
    return this;
  }

  @Nonnull
  public AS2ClientRequest setData (@Nonnull final String sText, @Nullable final Charset aCharset)
  {
    m_aDataByteArray = null;
    m_sDataText = ValueEnforcer.notNull (sText, "Text");
    m_aDataCharset = aCharset;
    m_aDataHandler = null;
    return this;
  }

  @Nonnull
  public AS2ClientRequest setData (@Nonnull final DataHandler aDataHandler)
  {
    m_aDataByteArray = null;
    m_sDataText = null;
    m_aDataCharset = null;
    m_aDataHandler = ValueEnforcer.notNull (aDataHandler, "DataHandler");
    return this;
  }

  /**
   * Set the filename to be used to name the content. This will add a
   * <code>Content-Disposition: attachment; filename=...</code> header to the
   * MIME part
   *
   * @param sFilename
   *        Filename to use. May be <code>null</code> to indicate none (also the
   *        default)
   * @return this
   */
  @Nonnull
  public AS2ClientRequest setFilename (@Nullable final String sFilename)
  {
    m_sFilename = sFilename;
    return this;
  }

  public void applyDataOntoMimeBodyPart (@Nonnull final MimeBodyPart aPart) throws MessagingException
  {
    if (m_aDataByteArray != null)
    {
      // Set content with a specific MIME type
      aPart.setContent (m_aDataByteArray, m_sContentType);
    }
    else
      if (m_sDataText != null)
      {
        // Set text with an optional charset
        // Sets the "text/plain" content-type internally!
        aPart.setText (m_sDataText, m_aDataCharset == null ? null : m_aDataCharset.name ());
      }
      else
        if (m_aDataHandler != null)
        {
          aPart.setDataHandler (m_aDataHandler);
        }
        else
          throw new IllegalStateException ("No data specified in AS2 client request!");

    // Set as filename as well
    if (m_sFilename != null)
      aPart.setFileName (m_sFilename);
  }
}
