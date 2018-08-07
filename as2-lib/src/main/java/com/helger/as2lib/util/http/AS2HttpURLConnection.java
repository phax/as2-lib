/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2018 Philip Helger philip[at]helger[dot]com
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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.string.StringHelper;

/**
 * AS2 Http connection, Implemented as HttpURLConnection.
 *
 * @author Ziv Harpaz
 */
public class AS2HttpURLConnection implements IAS2HttpConnection
{
  private final HttpURLConnection m_aHttpURLConnection;

  public AS2HttpURLConnection (@Nonnull final HttpURLConnection aConnection)
  {
    m_aHttpURLConnection = aConnection;
  }

  /**
   * Set an HTTP header
   *
   * @param sName
   *        Header name
   * @param sValue
   *        Header value
   */
  public void setHttpHeader (@Nonnull final String sName, @Nonnull final String sValue)
  {
    m_aHttpURLConnection.setRequestProperty (sName, sValue);
  }

  /**
   * Get URL
   */
  public URL getURL ()
  {
    return m_aHttpURLConnection.getURL ();
  }

  /**
   * Get OutputStream
   */
  public OutputStream getOutputStream () throws IOException
  {
    return m_aHttpURLConnection.getOutputStream ();
  }

  /**
   * Get InputStream
   */
  public InputStream getInputStream () throws IOException
  {
    return m_aHttpURLConnection.getInputStream ();
  }

  /**
   * Get response HTTP Status as integer
   */
  public int getResponseCode () throws IOException
  {
    return m_aHttpURLConnection.getResponseCode ();
  }

  /**
   * Get the response message
   */
  public String getResponseMessage () throws IOException
  {
    return m_aHttpURLConnection.getResponseMessage ();
  }

  /**
   * Get the headers of the request
   */
  @Nonnull
  public HttpHeaderMap getHeaderFields ()
  {
    final HttpHeaderMap ret = new HttpHeaderMap ();
    for (final Map.Entry <String, List <String>> aEntry : m_aHttpURLConnection.getHeaderFields ().entrySet ())
    {
      final String sName = aEntry.getKey ();
      // Sometimes the status line (like 'HTTP/1.1 200 OK') comes as a header with no
      // name
      if (StringHelper.hasText (sName))
        for (final String sValue : aEntry.getValue ())
          ret.addHeader (sName, sValue);
    }
    return ret;
  }

  /**
   * Close the connection
   */
  public void disconnect ()
  {
    m_aHttpURLConnection.disconnect ();
  }
}
