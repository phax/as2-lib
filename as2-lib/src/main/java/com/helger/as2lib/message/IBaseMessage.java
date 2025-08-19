/*
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2025 Philip Helger philip[at]helger[dot]com
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
 * Base interface for {@link IMessage} and {@link IMessageMDN}. Must be Serializable, so that
 * writing to disk for re-sending works.
 *
 * @author Philip Helger
 */
public interface IBaseMessage extends Serializable
{
  /**
   * @return Mutable custom attribute map. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableObject
  IStringMap attrs ();

  /**
   * @return Mutable HTTP header map. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableObject
  HttpHeaderMap headers ();

  @Nullable
  default String getHeader (@Nonnull final String sName)
  {
    return getHeaderCombined (sName, ", ");
  }

  @Nullable
  default String getHeaderCombined (@Nonnull final String sName, @Nonnull final String sDelimiter)
  {
    return headers ().getHeaderCombined (sName, sDelimiter);
  }

  @Nullable
  default String getHeaderOrDefault (@Nonnull final String sName, @Nullable final String sDefault)
  {
    final String ret = getHeader (sName);
    return ret != null ? ret : sDefault;
  }

  default boolean containsHeader (@Nullable final String sName)
  {
    return headers ().containsHeaders (sName);
  }

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
  @ReturnsMutableObject
  Partnership partnership ();

  void setPartnership (@Nonnull Partnership aPartnership);

  @Nonnull
  @Nonempty
  String getLoggingText ();

  @Nonnull
  @Nonempty
  String getAsString ();
}
