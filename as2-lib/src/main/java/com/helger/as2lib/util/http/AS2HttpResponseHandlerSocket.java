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
package com.helger.as2lib.util.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.WillNotClose;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.http.CHttp;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.IWriteToStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.http.EHttpVersion;

/**
 * An implementation of {@link IAS2HttpResponseHandler} that writes an HTTP 1.1
 * response directly to a {@link Socket}.
 *
 * @author Philip Helger
 */
public class AS2HttpResponseHandlerSocket implements IAS2HttpResponseHandler
{
  private final Socket m_aSocket;

  public AS2HttpResponseHandlerSocket (@Nonnull final Socket aSocket)
  {
    m_aSocket = ValueEnforcer.notNull (aSocket, "Socket");
  }

  /**
   * @return The HTTP version to use. May not be <code>null</code>.
   */
  @Nonnull
  @OverrideOnDemand
  public EHttpVersion getHTTPVersion ()
  {
    return EHttpVersion.HTTP_11;
  }

  @Nonnull
  @OverrideOnDemand
  public OutputStream createOutputStream () throws IOException
  {
    return StreamHelper.getBuffered (m_aSocket.getOutputStream ());
  }

  public void sendHttpResponse (@Nonnegative final int nHttpResponseCode,
                                @Nonnull final HttpHeaderMap aHeaders,
                                @Nonnull @WillNotClose final IWriteToStream aData) throws IOException
  {
    ValueEnforcer.isGT0 (nHttpResponseCode, "HttpResponseCode");
    ValueEnforcer.notNull (aHeaders, "Headers");
    ValueEnforcer.notNull (aData, "Data");

    try (final OutputStream aOS = createOutputStream ())
    {
      // Send HTTP version and response code
      final String sHttpStatusLine = getHTTPVersion ().getName () +
                                     " " +
                                     Integer.toString (nHttpResponseCode) +
                                     " " +
                                     CHttp.getHttpResponseMessage (nHttpResponseCode) +
                                     CHttp.EOL;
      aOS.write (sHttpStatusLine.getBytes (CHttp.HTTP_CHARSET));

      // Add response headers
      for (final String sHeaderLine : aHeaders.getAllHeaderLines ())
        aOS.write ((sHeaderLine + CHttp.EOL).getBytes (CHttp.HTTP_CHARSET));

      // Empty line as separator
      aOS.write (CHttp.EOL.getBytes (CHttp.HTTP_CHARSET));

      // Write body
      aData.writeTo (aOS);

      // Done
      aOS.flush ();
    }
  }
}
