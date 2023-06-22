/*
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2023 Philip Helger philip[at]helger[dot]com
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

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.http.HttpHeaderMap;

/**
 * Provider interface to retrieve an AS2 {@link InputStream}.
 *
 * @author Philip Helger
 */
public interface IAS2HttpRequestDataProvider
{
  /**
   * Get the input stream to read from. May not be <code>null</code>.
   *
   * @return Never <code>null</code>
   * @throws IOException
   *         In case of error
   */
  @Nonnull
  InputStream getHttpInputStream () throws IOException;

  /**
   * @return <code>true</code> if chunked encoding was already processed by an
   *         outside component (e.g. via Servlet), or <code>false</code> if it
   *         needs to be processed internally.
   * @since 5.0.3
   */
  boolean isChunkedEncodingAlreadyProcessed ();

  /**
   * @return The HTTP request method used. Usually this should be
   *         <code>POST</code>.
   */
  @Nullable
  String getHttpRequestMethod ();

  /**
   * @return The HTTP request URL used. Something like <code>/as2</code>.
   */
  @Nullable
  String getHttpRequestUrl ();

  /**
   * @return The HTTP request version used. Something like
   *         <code>HTTP/1.1</code>.
   */
  @Nonnull
  String getHttpRequestVersion ();

  /**
   * @return The provided HTTP header map. Mutable map is returned. Never
   *         <code>null</code>.
   */
  @Nonnull
  HttpHeaderMap getHttpHeaderMap ();
}
