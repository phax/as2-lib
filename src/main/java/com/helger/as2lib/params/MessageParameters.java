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
package com.helger.as2lib.params;

import java.io.File;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;
import javax.mail.internet.ContentDisposition;

import com.helger.as2lib.exception.InvalidParameterException;
import com.helger.as2lib.message.IMessage;

public class MessageParameters extends AbstractParameterParser
{
  public static final String KEY_SENDER = "sender";
  public static final String KEY_RECEIVER = "receiver";
  public static final String KEY_ATTRIBUTES = "attributes";
  public static final String KEY_HEADERS = "headers";
  public static final String KEY_CONTENT_FILENAME = "content-disposition";

  private final IMessage m_aTarget;

  public MessageParameters (@Nonnull final IMessage aTarget)
  {
    m_aTarget = aTarget;
  }

  @Nonnull
  public IMessage getTarget ()
  {
    return m_aTarget;
  }

  @Override
  public void setParameter (final String sKey, final String sValue) throws InvalidParameterException
  {
    final StringTokenizer aKeyParts = new StringTokenizer (sKey, ".", false);
    if (aKeyParts.countTokens () != 2)
      throw new InvalidParameterException ("Invalid key format", this, sKey, null);

    final String sArea = aKeyParts.nextToken ();
    final String sAreaID = aKeyParts.nextToken ();

    if (sArea.equals (KEY_SENDER))
      getTarget ().getPartnership ().setSenderID (sAreaID, sValue);
    else
      if (sArea.equals (KEY_RECEIVER))
        getTarget ().getPartnership ().setReceiverID (sAreaID, sValue);
      else
        if (sArea.equals (KEY_ATTRIBUTES))
          getTarget ().setAttribute (sAreaID, sValue);
        else
          if (sArea.equals (KEY_HEADERS))
            getTarget ().setHeader (sAreaID, sValue);
          else
            throw new InvalidParameterException ("Invalid area in key", this, sKey, null);
  }

  @Override
  public String getParameter (final String sKey) throws InvalidParameterException
  {
    final StringTokenizer aKeyParts = new StringTokenizer (sKey, ".", false);
    if (aKeyParts.countTokens () != 2)
      throw new InvalidParameterException ("Invalid key format", this, sKey, null);

    final String sArea = aKeyParts.nextToken ();
    final String sAreaID = aKeyParts.nextToken ();
    if (sArea.equals (KEY_SENDER))
      return getTarget ().getPartnership ().getSenderID (sAreaID);
    if (sArea.equals (KEY_RECEIVER))
      return getTarget ().getPartnership ().getReceiverID (sAreaID);
    if (sArea.equals (KEY_ATTRIBUTES))
      return getTarget ().getAttribute (sAreaID);
    if (sArea.equals (KEY_HEADERS))
      return getTarget ().getHeader (sAreaID);
    if (sArea.equals (KEY_CONTENT_FILENAME) && sAreaID.equals ("filename"))
    {
      String sReturnFilename = "noContentDispositionFilename";
      final String sFilename = m_aTarget.getContentDisposition ();
      if (sFilename == null || sFilename.length () < 1)
        return sReturnFilename;
      try
      {
        final int nPos = sFilename.lastIndexOf (File.separator);
        if (nPos >= 0)
          sReturnFilename = sFilename.substring (0, nPos + 1);

        final ContentDisposition aContentDisposition = new ContentDisposition (sFilename);
        sReturnFilename = aContentDisposition.getParameter ("filename");
      }
      catch (final Exception ex)
      {
        ex.printStackTrace ();
      }
      return sReturnFilename;
    }
    throw new InvalidParameterException ("Invalid area in key", this, sKey, null);
  }
}
