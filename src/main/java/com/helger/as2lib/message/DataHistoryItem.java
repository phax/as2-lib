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
package com.helger.as2lib.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

public class DataHistoryItem implements Serializable
{
  private ContentType m_aContentType;
  private Map <String, String> m_aAttributes;

  public DataHistoryItem (final String contentType) throws ParseException
  {
    this (new ContentType (contentType));
  }

  public DataHistoryItem (@Nonnull final ContentType type)
  {
    m_aContentType = type;
  }

  public Map <String, String> getAttributes ()
  {
    if (m_aAttributes == null)
      m_aAttributes = new HashMap <String, String> ();
    return m_aAttributes;
  }

  public ContentType getContentType ()
  {
    return m_aContentType;
  }

  @SuppressWarnings ("unchecked")
  private void readObject (final ObjectInputStream in) throws ParseException, IOException, ClassNotFoundException
  {
    m_aContentType = new ContentType ((String) in.readObject ());
    m_aAttributes = (Map <String, String>) in.readObject ();
  }

  private void writeObject (final ObjectOutputStream out) throws IOException
  {
    out.writeObject (m_aContentType.toString ());
    out.writeObject (m_aAttributes);
  }
}
