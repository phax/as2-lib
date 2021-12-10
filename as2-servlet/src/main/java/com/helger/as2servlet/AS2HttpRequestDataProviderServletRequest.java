/*
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2021 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2servlet;

import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;

import com.helger.as2lib.util.http.IAS2HttpRequestDataProvider;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.stream.NonClosingInputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.web.scope.IRequestWebScope;

/**
 * Implementation of {@link IAS2HttpRequestDataProvider} based on a
 * {@link ServletRequest} {@link InputStream}.
 *
 * @author Philip Helger
 * @since 4.8.0
 */
@Immutable
final class AS2HttpRequestDataProviderServletRequest implements IAS2HttpRequestDataProvider
{
  private final IRequestWebScope m_aRequestScope;
  private final ServletInputStream m_aRequestIS;

  /**
   * Constructor
   *
   * @param aRequestScope
   *        HTTP Servlet Request. May not be <code>null</code>.
   * @param aRequestIS
   *        Servlet request InputStream to read from. Will not be closed. May
   *        not be <code>null</code>.
   */
  public AS2HttpRequestDataProviderServletRequest (@Nonnull final IRequestWebScope aRequestScope,
                                                   @Nonnull @WillNotClose final ServletInputStream aRequestIS)
  {
    ValueEnforcer.notNull (aRequestScope, "RequestScope");
    ValueEnforcer.notNull (aRequestIS, "RequestIS");
    m_aRequestScope = aRequestScope;
    m_aRequestIS = aRequestIS;
  }

  /**
   * Will return a buffered, {@link NonClosingInputStream} that when closed,
   * will not close in source stream.
   *
   * @return {@link InputStream}
   */
  @Nonnull
  public InputStream getHttpInputStream ()
  {
    // Use "NonClosing" internally to that the returned stream is easily
    // discovered as "buffered"
    return StreamHelper.getBuffered (new NonClosingInputStream (m_aRequestIS));
  }

  @Nonnull
  @Nonempty
  public String getHttpRequestMethod ()
  {
    return m_aRequestScope.getHttpMethod ().getName ();
  }

  @Nonnull
  @Nonempty
  public String getHttpRequestUrl ()
  {
    return m_aRequestScope.getRequestURIDecoded ();
  }

  @Nonnull
  @Nonempty
  public String getHttpRequestVersion ()
  {
    return m_aRequestScope.getHttpVersion ().getName ();
  }

  @Nonnull
  public HttpHeaderMap getHttpHeaderMap ()
  {
    return m_aRequestScope.headers ();
  }
}
