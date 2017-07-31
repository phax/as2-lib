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
package com.helger.as2lib.util;

import javax.annotation.concurrent.Immutable;

import com.helger.as2lib.CAS2Info;
import com.helger.commons.http.CHttpHeader;

@Immutable
public final class CAS2Header
{
  public static final String HEADER_AS2_FROM = CHttpHeader.AS2_FROM;
  public static final String HEADER_AS2_TO = CHttpHeader.AS2_TO;
  public static final String HEADER_AS2_VERSION = CHttpHeader.AS2_VERSION;
  public static final String HEADER_CONNECTION = CHttpHeader.CONNECTION;
  public static final String HEADER_CONTENT_DISPOSITION = CHttpHeader.CONTENT_DISPOSITION;
  public static final String HEADER_CONTENT_LENGTH = CHttpHeader.CONTENT_LENGTH;
  public static final String HEADER_CONTENT_TRANSFER_ENCODING = CHttpHeader.CONTENT_TRANSFER_ENCODING;
  public static final String HEADER_CONTENT_TYPE = CHttpHeader.CONTENT_TYPE;
  public static final String HEADER_DATE = CHttpHeader.DATE;
  public static final String HEADER_DISPOSITION_NOTIFICATION_OPTIONS = CHttpHeader.DISPOSITION_NOTIFICATION_OPTIONS;
  /** The URL where the async MDN should be send to - limited to RFC 2822 */
  public static final String HEADER_DISPOSITION_NOTIFICATION_TO = CHttpHeader.DISPOSITION_NOTIFICATION_TO;
  public static final String HEADER_FROM = CHttpHeader.FROM;
  public static final String HEADER_MESSAGE_ID = CHttpHeader.MESSAGE_ID;
  public static final String HEADER_MIME_VERSION = CHttpHeader.MIME_VERSION;
  /** The URL where the async MDN should be send to */
  public static final String HEADER_RECEIPT_DELIVERY_OPTION = CHttpHeader.RECEIPT_DELIVERY_OPTION;
  public static final String HEADER_RECIPIENT_ADDRESS = CHttpHeader.RECIPIENT_ADDRESS;
  public static final String HEADER_SERVER = CHttpHeader.SERVER;
  public static final String HEADER_SUBJECT = CHttpHeader.SUBJECT;
  public static final String HEADER_TRANSFER_ENCODING = CHttpHeader.TRANSFER_ENCODING;
  public static final String HEADER_USER_AGENT = CHttpHeader.USER_AGENT;
  /** Defined by RFC 6017 */
  public static final String HEADER_EDIINT_FEATURES = CHttpHeader.EDIINT_FEATURES;

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
  /**
   * RFC2822 format: Wed, 04 Mar 2009 10:59:17 +0100
   */
  public static final String DEFAULT_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";

  private CAS2Header ()
  {}
}
