/*
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2022 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.util.http;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.as2lib.util.dump.IHTTPOutgoingDumper;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.http.HttpHeaderMap;

/**
 * Set HTTP header including logging
 *
 * @author Philip Helger
 */
@Immutable
public final class AS2HttpHeaderSetter
{
  private final AS2HttpClient m_aConn;
  private final IHTTPOutgoingDumper m_aOutgoingDumper;
  private final boolean m_bQuoteHeaderValues;

  /**
   * Constructor with debug support
   *
   * @param aConn
   *        The HTTP URL connection to use. May not be <code>null</code>.
   * @param aOutgoingDumper
   *        An optional outgoing dumper, that will also receive all the headers.
   *        May be <code>null</code>.
   * @param bQuoteHeaderValues
   *        <code>true</code> if HTTP header values should be automatically
   *        quoted, <code>false</code> if not. This might be an interoperability
   *        issue. The receiving side must be able to unquote according to RFC
   *        2616.
   */
  public AS2HttpHeaderSetter (@Nonnull final AS2HttpClient aConn,
                              @Nullable final IHTTPOutgoingDumper aOutgoingDumper,
                              final boolean bQuoteHeaderValues)
  {
    m_aConn = ValueEnforcer.notNull (aConn, "Connection");
    m_aOutgoingDumper = aOutgoingDumper;
    m_bQuoteHeaderValues = bQuoteHeaderValues;
  }

  public void setHttpHeader (@Nonnull final String sName, @Nonnull final String sValue)
  {
    // Ensure automatic quoting is used.
    // The underlying HttpClient does not do this automatically
    final String sUnifiedValue = HttpHeaderMap.getUnifiedValue (sValue, m_bQuoteHeaderValues);
    m_aConn.setHttpHeader (sName, sUnifiedValue);

    if (m_aOutgoingDumper != null)
      m_aOutgoingDumper.dumpHeader (sName, sUnifiedValue);
  }
}
