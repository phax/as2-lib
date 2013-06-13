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
package com.helger.as2lib.params;

import java.util.StringTokenizer;

import javax.annotation.Nonnull;

import com.helger.as2lib.message.IMessageMDN;

public class MessageMDNParameters extends AbstractParameterParser
{
  public static final String KEY_MESSAGE = "msg";
  public static final String KEY_SENDER = "sender";
  public static final String KEY_RECEIVER = "receiver";
  public static final String KEY_TEXT = "text";
  public static final String KEY_ATTRIBUTES = "attributes";
  public static final String KEY_HEADERS = "headers";

  private final IMessageMDN m_aTarget;

  public MessageMDNParameters (@Nonnull final IMessageMDN aTarget)
  {
    m_aTarget = aTarget;
  }

  @Nonnull
  public IMessageMDN getTarget ()
  {
    return m_aTarget;
  }

  @Override
  public void setParameter (final String sKey, final String sValue) throws InvalidParameterException
  {
    final IMessageMDN aTarget = getTarget ();
    final StringTokenizer aKeyParts = new StringTokenizer (sKey, ".", false);

    if (aKeyParts.countTokens () < 2)
      throw new InvalidParameterException ("Invalid key format", this, "key", sKey);

    final String sArea = aKeyParts.nextToken ();
    if (sArea.equals (KEY_MESSAGE))
    {
      if (aKeyParts.countTokens () < 3)
        throw new InvalidParameterException ("Invalid key format", this, "key", sKey);

      final String sMessageKey = aKeyParts.nextToken () + "." + aKeyParts.nextToken ();
      if (aTarget.getMessage () == null)
        throw new InvalidParameterException ("MDN has no message", this, "key", sKey);

      new MessageParameters (aTarget.getMessage ()).setParameter (sMessageKey, sValue);
    }
    else
    {
      final String sAreaID = aKeyParts.nextToken ();
      if (sArea.equals (KEY_TEXT))
        aTarget.setText (sValue);
      else
        if (sArea.equals (KEY_ATTRIBUTES))
          aTarget.setAttribute (sAreaID, sValue);
        else
          if (sArea.equals (KEY_HEADERS))
            aTarget.setHeader (sAreaID, sValue);
          else
            throw new InvalidParameterException ("Invalid area in key", this, "key", sKey);
    }
  }

  @Override
  public String getParameter (final String sKey) throws InvalidParameterException
  {
    final IMessageMDN aTarget = getTarget ();
    final StringTokenizer aKeyParts = new StringTokenizer (sKey, ".", false);
    if (aKeyParts.countTokens () > 2)
    {
      aKeyParts.nextToken ();
      final String sMsgKey = aKeyParts.nextToken () + "." + aKeyParts.nextToken ();
      return new MessageParameters (aTarget.getMessage ()).getParameter (sMsgKey);
    }

    if (aKeyParts.countTokens () < 2)
      throw new InvalidParameterException ("Invalid key format", this, "key", sKey);

    final String sArea = aKeyParts.nextToken ();
    final String sAreaID = aKeyParts.nextToken ();

    if (sArea.equals (KEY_SENDER))
      return getTarget ().getPartnership ().getSenderID (sAreaID);
    if (sArea.equals (KEY_RECEIVER))
      return getTarget ().getPartnership ().getReceiverID (sAreaID);
    if (sArea.equals (KEY_TEXT))
      return aTarget.getText ();
    if (sArea.equals (KEY_ATTRIBUTES))
      return aTarget.getAttribute (sAreaID);
    if (sArea.equals (KEY_HEADERS))
      return aTarget.getHeader (sAreaID);
    throw new InvalidParameterException ("Invalid area in key", this, "key", sKey);
  }
}
