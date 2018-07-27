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
import java.net.Socket;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.stream.NonClosingInputStream;
import com.helger.commons.io.stream.StreamHelper;

/**
 * Implementation of {@link IAS2InputStreamProvider} based on a {@link Socket}
 * {@link InputStream}.
 *
 * @author Philip Helger
 */
@Immutable
public class AS2InputStreamProviderSocket implements IAS2InputStreamProvider
{
  private final Socket m_aSocket;
  private final boolean m_bNonUpwardClosing;

  /**
   * Constructor
   *
   * @param aSocket
   *        Socket to read from. May not be <code>null</code>.
   */
  public AS2InputStreamProviderSocket (@Nonnull final Socket aSocket)
  {
    this (aSocket, false);
  }

  /**
   * Constructor
   *
   * @param aSocket
   *        Socket to read from. May not be <code>null</code>.
   * @param bNonUpwardClosing
   *        When true, closing the {@link InputStream} will not close the
   *        {@link Socket}
   */
  public AS2InputStreamProviderSocket (@Nonnull final Socket aSocket, final boolean bNonUpwardClosing)
  {
    ValueEnforcer.notNull (aSocket, "Socket");
    m_aSocket = aSocket;
    m_bNonUpwardClosing = bNonUpwardClosing;
  }

  /**
   * According to instance initialization, will either return the regular
   * {@link InputStream}, or a {@link NonClosingInputStream} that when closed,
   * will not close in source stream. This is useful when working with
   * <code>java.net.SocketInputStream</code> as close() on a socket stream
   * closes the {@link Socket}
   *
   * @return {@link InputStream}
   * @throws IOException
   *         in case of error
   */
  @Nonnull
  public InputStream getInputStream () throws IOException
  {
    if (m_bNonUpwardClosing)
      return getNonUpwardClosingInputStream ();
    return StreamHelper.getBuffered (m_aSocket.getInputStream ());
  }

  @Nonnull
  public InputStream getNonUpwardClosingInputStream () throws IOException
  {
    // Use "NonClosing" internally to that the returned stream is easily
    // discovered as "buffered"
    return StreamHelper.getBuffered (new NonClosingInputStream (m_aSocket.getInputStream ()));
  }
}
