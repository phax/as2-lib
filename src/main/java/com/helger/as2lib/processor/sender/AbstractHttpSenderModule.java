/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013 Philip Helger ph[at]phloc[dot]com
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
package com.helger.as2lib.processor.sender;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.mail.internet.InternetHeaders;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedException;

public abstract class AbstractHttpSenderModule extends AbstractSenderModule
{

  public static final String PARAM_READ_TIMEOUT = "readtimeout";
  public static final String PARAM_CONNECT_TIMEOUT = "connecttimeout";

  public HttpURLConnection getConnection (final String url,
                                          final boolean output,
                                          final boolean input,
                                          final boolean useCaches,
                                          final String requestMethod) throws OpenAS2Exception
  {
    try
    {
      final URL urlObj = new URL (url);
      HttpURLConnection conn;
      conn = (HttpURLConnection) urlObj.openConnection ();
      conn.setDoOutput (output);
      conn.setDoInput (input);
      conn.setUseCaches (useCaches);
      conn.setRequestMethod (requestMethod);
      conn.setConnectTimeout (getParameterInt (PARAM_CONNECT_TIMEOUT, 60000));
      conn.setReadTimeout (getParameterInt (PARAM_READ_TIMEOUT, 60000));
      return conn;
    }
    catch (final IOException ioe)
    {
      throw new WrappedException (ioe);
    }
  }

  // Copy headers from an Http connection to an InternetHeaders object
  protected void copyHttpHeaders (final HttpURLConnection conn, final InternetHeaders headers)
  {
    final Iterator <Entry <String, List <String>>> connHeadersIt = conn.getHeaderFields ().entrySet ().iterator ();
    Iterator <String> connValuesIt;
    Map.Entry <String, List <String>> connHeader;
    String headerName;

    while (connHeadersIt.hasNext ())
    {
      connHeader = connHeadersIt.next ();
      headerName = connHeader.getKey ();

      if (headerName != null)
      {
        connValuesIt = connHeader.getValue ().iterator ();

        while (connValuesIt.hasNext ())
        {
          final String value = connValuesIt.next ();

          if (headers.getHeader (headerName) == null)
          {
            headers.setHeader (headerName, value);
          }
          else
          {
            headers.addHeader (headerName, value);
          }
        }
      }
    }
  }
}
