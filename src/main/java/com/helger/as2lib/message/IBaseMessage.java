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
package com.helger.as2lib.message;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.internet.InternetHeaders;

import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.util.IStringMap;
import com.helger.as2lib.util.StringMap;
import com.helger.commons.annotations.ReturnsMutableCopy;

/**
 * Base interface for {@link IMessage} and {@link IMessageMDN}.
 *
 * @author Philip Helger
 */
public interface IBaseMessage extends Serializable
{
  @Nullable
  String getAttribute (@Nullable String sKey);

  @Nonnull
  @ReturnsMutableCopy
  StringMap getAllAttributes ();

  void setAttribute (@Nonnull String sKey, @Nullable String sValue);

  void setAttributes (@Nullable IStringMap aAttributes);

  @Nullable
  String getHeader (@Nonnull String sKey);

  @Nullable
  String getHeader (@Nonnull String sKey, @Nullable String sDelimiter);

  @Nonnull
  InternetHeaders getHeaders ();

  void setHeader (@Nonnull String sKey, @Nullable String sValue);

  void addHeader (@Nonnull String sKey, @Nullable String sValue);

  void setHeaders (@Nullable InternetHeaders aHeaders);

  /**
   * @return Special message ID header
   */
  @Nullable
  String getMessageID ();

  /**
   * Set special message ID header
   *
   * @param sMessageID
   *        Message ID
   */
  void setMessageID (@Nullable String sMessageID);

  @Nonnull
  String generateMessageID ();

  /**
   * Shortcut for <code>setMessageID (generateMessageID ())</code>
   */
  void updateMessageID ();

  @Nonnull
  Partnership getPartnership ();

  void setPartnership (@Nonnull Partnership aPartnership);
}
