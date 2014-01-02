/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2014 Philip Helger ph[at]phloc[dot]com
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
package com.helger.as2lib.util.javamail;

import java.io.IOException;

import javax.activation.DataSource;
import javax.annotation.Nonnull;

import com.phloc.commons.io.streams.NonBlockingByteArrayInputStream;
import com.phloc.commons.io.streams.NonBlockingByteArrayOutputStream;
import com.phloc.commons.mime.CMimeType;

public class ByteArrayDataSource implements DataSource
{
  private final String m_sContentType;
  private final String m_sName;
  private byte [] m_aBytes;

  public ByteArrayDataSource (final byte [] bytes, final String contentType, final String name)
  {
    m_aBytes = bytes;
    m_sContentType = contentType == null ? CMimeType.APPLICATION_OCTET_STREAM.getAsString () : contentType;
    m_sName = name;
  }

  public byte [] getBytes ()
  {
    return m_aBytes;
  }

  @Nonnull
  public String getContentType ()
  {
    return m_sContentType;
  }

  public String getName ()
  {
    return m_sName;
  }

  @Nonnull
  public NonBlockingByteArrayInputStream getInputStream ()
  {
    return new NonBlockingByteArrayInputStream (m_aBytes);
  }

  @Nonnull
  public NonBlockingByteArrayOutputStream getOutputStream () throws IOException
  {
    return new WrappedOutputStream ();
  }

  private class WrappedOutputStream extends NonBlockingByteArrayOutputStream
  {
    @Override
    public void close ()
    {
      super.close ();
      ByteArrayDataSource.this.m_aBytes = toByteArray ();
    }
  }
}
