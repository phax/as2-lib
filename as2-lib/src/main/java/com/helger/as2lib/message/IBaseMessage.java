/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2017 Philip Helger philip[at]helger[dot]com
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
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.internet.InternetHeaders;

import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.IOHelper;
import com.helger.as2lib.util.IStringMap;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsOrderedMap;

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
  default String getHeader (@Nonnull final String sKey)
  {
    return getHeader (sKey, ", ");
  }

  @Nullable
  String getHeader (@Nonnull String sKey, @Nullable String sDelimiter);

  @Nonnull
  @ReturnsMutableObject ("design")
  InternetHeaders headers ();

  /**
   * Iterate each header.
   *
   * @param aConsumer
   *        Consumer to be invoked. May not be <code>null</code>.
   * @since 3.0.4
   */
  default void forEachHeader (@Nonnull final BiConsumer <String, String> aConsumer)
  {
    IOHelper.forEachHeader (headers (), aConsumer);
  }

  /**
   * @return A map of all headers. Never <code>null</code>.
   * @since 3.0.4
   */
  @Nonnull
  @ReturnsMutableCopy
  default ICommonsOrderedMap <String, String> getAllHeaders ()
  {
    final ICommonsOrderedMap <String, String> aMap = new CommonsLinkedHashMap <> ();
    forEachHeader ( (k, v) -> aMap.put (k, v));
    return aMap;
  }

  @Nonnull
  @Nonempty
  default String getHeadersDebugFormatted ()
  {
    return getAllHeaders ().toString ();
  }

  /**
   * Set a generic header. If it already exist it will be overwritten.
   *
   * @param sKey
   *        Header name
   * @param sValue
   *        Header value
   * @see #addHeader(String, String)
   */
  void setHeader (@Nonnull String sKey, @Nullable String sValue);

  /**
   * Add a generic header
   *
   * @param sKey
   *        Header name
   * @param sValue
   *        Header value
   * @see #setHeader(String, String)
   */
  void addHeader (@Nonnull String sKey, @Nullable String sValue);

  /**
   * Set all headers from the providers headers object. All existing headers are
   * discarded.
   *
   * @param aHeaders
   *        The headers object to be used. May be <code>null</code> in which
   *        case all existing headers are removed.
   */
  void setHeaders (@Nullable InternetHeaders aHeaders);

  /**
   * @return Special message ID header
   */
  @Nullable
  default String getMessageID ()
  {
    return getHeader (CAS2Header.HEADER_MESSAGE_ID);
  }

  /**
   * Set special message ID header
   *
   * @param sMessageID
   *        Message ID
   */
  default void setMessageID (@Nullable final String sMessageID)
  {
    setHeader (CAS2Header.HEADER_MESSAGE_ID, sMessageID);
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
