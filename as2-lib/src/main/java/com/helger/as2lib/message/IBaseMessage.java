/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2018 Philip Helger philip[at]helger[dot]com
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

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.partner.Partnership;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.attr.IStringMap;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.http.HttpHeaderMap;

/**
 * Base interface for {@link IMessage} and {@link IMessageMDN}.
 *
 * @author Philip Helger
 */
public interface IBaseMessage extends Serializable
{
  @Nonnull
  @ReturnsMutableObject
  IStringMap attrs ();

  @Nullable
  default String getHeader (@Nonnull final String sName)
  {
    return getHeader (sName, ", ");
  }

  @Nullable
  default String getHeader (@Nonnull final String sName, @Nullable final String sDelimiter)
  {
    return headers ().getHeaderCombined (sName, sDelimiter);
  }

  default boolean containsHeader (@Nullable final String sName)
  {
    return headers ().containsHeaders (sName);
  }

  @Nonnull
  @ReturnsMutableObject
  HttpHeaderMap headers ();

  /**
   * @return Special message ID header
   */
  @Nullable
  default String getMessageID ()
  {
    return getHeader (CHttpHeader.MESSAGE_ID);
  }

  /**
   * Set special message ID header
   *
   * @param sMessageID
   *        Message ID
   */
  default void setMessageID (@Nullable final String sMessageID)
  {
    headers ().setHeader (CHttpHeader.MESSAGE_ID, sMessageID);
  }

  @Nonnull
  String generateMessageID ();

  /**
   * Shortcut for <code>setMessageID (generateMessageID ())</code>
   */
  default void updateMessageID ()
  {
    setMessageID (generateMessageID ());
  }

  @Nonnull
  @ReturnsMutableObject ("Design")
  Partnership partnership ();

  void setPartnership (@Nonnull Partnership aPartnership);

  @Nonnull
  @Nonempty
  String getAsString ();
}
