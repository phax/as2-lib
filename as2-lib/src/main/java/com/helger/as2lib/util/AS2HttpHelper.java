/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2019 Philip Helger philip[at]helger[dot]com
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.string.StringHelper;

@Immutable
public final class AS2HttpHelper
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AS2HttpHelper.class);

  private AS2HttpHelper ()
  {}

  @Nullable
  public static ContentType parseContentType (@Nullable final String sContentType)
  {
    if (StringHelper.hasText (sContentType))
      try
      {
        return new ContentType (sContentType);
      }
      catch (final ParseException ex)
      {
        // Something went wrong
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Error parsing Content-Type", ex);
      }
    return null;
  }

  @Nullable
  public static String getCleanContentType (@Nullable final String sContentType)
  {
    final ContentType aCT = parseContentType (sContentType);
    return aCT != null ? aCT.toString () : null;
  }

  @Nonnull
  public static InternetHeaders getAsInternetHeaders (@Nonnull final HttpHeaderMap aHeaders)
  {
    final InternetHeaders ret = new InternetHeaders ();
    aHeaders.forEachSingleHeader (ret::addHeader);
    return ret;
  }
}
