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
package com.helger.as2lib.params;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.internet.ContentDisposition;

import com.helger.as2lib.message.IMessage;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;

public class MessageParameters extends AbstractParameterParser
{
  public static final String KEY_SENDER = "sender";
  public static final String KEY_RECEIVER = "receiver";
  public static final String KEY_ATTRIBUTES = "attributes";
  public static final String KEY_HEADERS = "headers";
  public static final String KEY_CONTENT_DISPOSITION = "content-disposition";

  private final IMessage m_aTarget;

  public MessageParameters (@Nonnull final IMessage aTarget)
  {
    m_aTarget = ValueEnforcer.notNull (aTarget, "Target");
  }

  @Override
  public void setParameter (@Nonnull final String sKey, @Nullable final String sValue) throws InvalidParameterException
  {
    final String [] aKeyParts = StringHelper.getExplodedArray ('.', sKey, 2);
    if (aKeyParts.length != 2)
      throw new InvalidParameterException ("Invalid key format", this, sKey, null);

    final String sArea = aKeyParts[0];
    final String sAreaID = aKeyParts[1];

    if (sArea.equals (KEY_SENDER))
      m_aTarget.getPartnership ().setSenderID (sAreaID, sValue);
    else
      if (sArea.equals (KEY_RECEIVER))
        m_aTarget.getPartnership ().setReceiverID (sAreaID, sValue);
      else
        if (sArea.equals (KEY_ATTRIBUTES))
          m_aTarget.setAttribute (sAreaID, sValue);
        else
          if (sArea.equals (KEY_HEADERS))
            m_aTarget.setHeader (sAreaID, sValue);
          else
            throw new InvalidParameterException ("Invalid area in key", this, sKey, null);
  }

  @Nonnull
  @Nonempty
  private String _getContentDispositionFilename ()
  {
    String sReturnFilename = "noContentDispositionFilename";
    final String sFilename = m_aTarget.getContentDisposition ();
    if (StringHelper.hasText (sFilename))
    {
      try
      {
        final ContentDisposition aContentDisposition = new ContentDisposition (sFilename);
        sReturnFilename = aContentDisposition.getParameter ("filename");
      }
      catch (final Exception ex)
      {
        throw new IllegalStateException ("Error parsing parameter", ex);
      }
    }
    return sReturnFilename;
  }

  @Override
  public String getParameter (@Nonnull final String sKey) throws InvalidParameterException
  {
    final String [] aKeyParts = StringHelper.getExplodedArray ('.', sKey);
    if (aKeyParts.length != 2)
      throw new InvalidParameterException ("Invalid key format", this, sKey, null);

    final String sArea = aKeyParts[0];
    final String sAreaValue = aKeyParts[1];

    if (sArea.equals (KEY_SENDER))
      return m_aTarget.getPartnership ().getSenderID (sAreaValue);

    if (sArea.equals (KEY_RECEIVER))
      return m_aTarget.getPartnership ().getReceiverID (sAreaValue);

    if (sArea.equals (KEY_ATTRIBUTES))
      return m_aTarget.getAttribute (sAreaValue);

    if (sArea.equals (KEY_HEADERS))
      return m_aTarget.getHeader (sAreaValue);

    if (sArea.equals (KEY_CONTENT_DISPOSITION) && sAreaValue.equals ("filename"))
      return _getContentDispositionFilename ();

    throw new InvalidParameterException ("Invalid area in key", this, sKey, null);
  }
}
