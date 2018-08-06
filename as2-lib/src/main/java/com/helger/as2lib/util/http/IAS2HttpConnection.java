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
import java.net.URL;

import javax.annotation.Nonnull;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.commons.http.HttpHeaderMap;

/**
 * Interface for Http connection, for set and get headers, content, etc.
 *
 * @author Ziv Harpaz
 */
public interface IAS2HttpConnection
{
  /**
   * Set an HTTP header
   *
   * @param sName
   *        Header name
   * @param sValue
   *        Header value
   */
  void setHttpHeader (@Nonnull String sName, @Nonnull String sValue);

  /**
   * @return URL
   * @throws OpenAS2Exception
   *         in case of error
   */
  URL getURL () throws OpenAS2Exception;

  /**
   * @return OutputStream
   * @throws IOException
   *         in case of error
   */
  OutputStream getOutputStream () throws IOException;

  /**
   * @return InputStream
   * @throws OpenAS2Exception
   *         in case of error
   * @throws IOException
   *         in case of error
   */
  InputStream getInputStream () throws OpenAS2Exception, IOException;

  /**
   * @return response HTTP Status as integer
   * @throws OpenAS2Exception
   *         in case of error
   * @throws IOException
   *         in case of error
   */
  int getResponseCode () throws OpenAS2Exception, IOException;

  /**
   * @return the response message
   * @throws OpenAS2Exception
   *         in case of error
   * @throws IOException
   *         in case of error
   */
  String getResponseMessage () throws OpenAS2Exception, IOException;

  /**
   * @return the headers of the request
   * @throws OpenAS2Exception
   *         in case of error
   */
  @Nonnull
  HttpHeaderMap getHeaderFields () throws OpenAS2Exception;

  /**
   * Close the connection
   */
  void disconnect ();

  /**
   * // TODO kick this from the interface
   * 
   * @param toSend
   *        InputStream to send
   * @throws OpenAS2Exception
   *         In case of error
   * @throws IOException
   *         In case of error
   */
  default public void send (final InputStream toSend) throws OpenAS2Exception, IOException
  {
    throw new OpenAS2Exception ("Method not implemented in class " + this.getClass ().getName ());
  }
}
