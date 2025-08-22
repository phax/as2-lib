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
package com.helger.as2lib.util;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import com.helger.annotation.concurrent.Immutable;
import com.helger.datetime.format.DateTimeFormatterCache;
import com.helger.datetime.format.PDTFromString;
import com.helger.datetime.helper.PDTFactory;

import jakarta.annotation.Nonnull;

@Immutable
public final class AS2DateHelper
{
  private AS2DateHelper ()
  {}

  @Nonnull
  public static String getFormattedDateNow (@Nonnull final String sFormat)
  {
    // Must use "ZonedDateTime" because time zone is part of many formats
    return formatDate (sFormat, PDTFactory.getCurrentZonedDateTime ());
  }

  @Nonnull
  public static String formatDate (@Nonnull final String sFormat, @Nonnull final ZonedDateTime aValue)
  {
    try
    {
      return DateTimeFormatterCache.getDateTimeFormatterSmart (sFormat).format (aValue);
    }
    catch (final DateTimeException ex)
    {
      throw new IllegalArgumentException ("Failed to format date '" + aValue + "' using format '" + sFormat + "'", ex);
    }
  }

  @Nonnull
  public static LocalDateTime parseDate (@Nonnull final String sFormat, @Nonnull final String sValue)
  {
    final ZonedDateTime ret = PDTFromString.getZonedDateTimeFromString (sValue, sFormat);
    if (ret == null)
      throw new IllegalArgumentException ("Failed to parse date '" + sValue + "' using format '" + sFormat + "'");
    return ret.toLocalDateTime ();
  }
}
