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
package com.helger.as2lib.util;

import javax.annotation.concurrent.Immutable;

import com.helger.as2lib.CAS2Info;

@Immutable
public final class CAS2Header
{
  public static final String HEADER_AS2_FROM = "AS2-From";
  public static final String HEADER_AS2_TO = "AS2-To";
  public static final String HEADER_AS2_VERSION = "AS2-Version";
  public static final String HEADER_CONNECTION = "Connection";
  public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
  public static final String HEADER_CONTENT_LENGTH = "Content-Length";
  public static final String HEADER_CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String HEADER_DATE = "Date";
  public static final String HEADER_DISPOSITION_NOTIFICATION_OPTIONS = "Disposition-Notification-Options";
  public static final String HEADER_DISPOSITION_NOTIFICATION_TO = "Disposition-Notification-To";
  public static final String HEADER_FROM = "From";
  public static final String HEADER_MESSAGE_ID = "Message-ID";
  public static final String HEADER_MIME_VERSION = "Mime-Version";
  public static final String HEADER_RECEIPT_DELIVERY_OPTION = "Receipt-Delivery-Option";
  public static final String HEADER_RECIPIENT_ADDRESS = "Recipient-Address";
  public static final String HEADER_SERVER = "Server";
  public static final String HEADER_SUBJECT = "Subject";
  public static final String HEADER_TRANSFER_ENCODING = "Transfer-Encoding";
  public static final String HEADER_USER_AGENT = "User-Agent";
  /** Defined by RFC 6017 */
  public static final String HEADER_EDIINT_FEATURES = "EDIINT-Features";

  public static final String DEFAULT_CONNECTION = "close, TE";
  public static final String DEFAULT_USER_AGENT = CAS2Info.NAME + "/AS2Sender";
  public static final String DEFAULT_MIME_VERSION = "1.0";
  /**
   * 1.0: default AS2 version<br>
   * 1.1: Designates those implementations that support compression as defined
   * by RFC 3274 - used by us.<br>
   * 1.2: indicate the support of the EDIINT-Features header field as defined by
   * RFC 6017
   */
  public static final String DEFAULT_AS2_VERSION = "1.1";
  public static final String DEFAULT_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";
  public static final String DEFAULT_CONTENT_TRANSFER_ENCODING = EContentTransferEncoding._8BIT.getID ();

  private CAS2Header ()
  {}
}
