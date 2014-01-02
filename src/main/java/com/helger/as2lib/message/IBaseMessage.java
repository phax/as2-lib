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
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.internet.InternetHeaders;

import com.helger.as2lib.partner.Partnership;

/**
 * Base interface for {@link IMessage} and {@link IMessageMDN}.
 * 
 * @author Philip Helger
 */
public interface IBaseMessage extends Serializable
{
  @Nullable
  String getAttribute (String sKey);

  @Nonnull
  Map <String, String> getAttributes ();

  void setAttribute (String sKey, String sValue);

  void setAttributes (@Nullable Map <String, String> aAttributes);

  @Nullable
  String getHeader (String sKey);

  String getHeader (String sKey, String sDelimiter);

  @Nonnull
  InternetHeaders getHeaders ();

  void setHeader (String sKey, String sValue);

  void addHeader (String sKey, String sValue);

  void setHeaders (@Nullable InternetHeaders aHeaders);

  @Nonnull
  DataHistory getHistory ();

  void setHistory (@Nullable DataHistory aHistory);

  String getMessageID ();

  void setMessageID (String sMessageID);

  @Nonnull
  Partnership getPartnership ();

  void setPartnership (@Nullable Partnership aPartnership);

  String generateMessageID ();

  void updateMessageID ();
}
