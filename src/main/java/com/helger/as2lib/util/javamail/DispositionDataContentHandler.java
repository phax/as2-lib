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

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.io.OutputStream;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataContentHandler;
import javax.activation.DataSource;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import com.helger.commons.collections.ArrayHelper;
import com.helger.commons.io.streams.StreamUtils;

public class DispositionDataContentHandler implements DataContentHandler
{
  private static final ActivationDataFlavor ADF1;
  private static final DataFlavor [] ADFs;

  static
  {
    ADF1 = new ActivationDataFlavor (MimeBodyPart.class, "message/disposition-notification", "Disposition Notification");
    ADFs = new DataFlavor [] { ADF1 };
  }

  public DispositionDataContentHandler ()
  {}

  @Nullable
  public byte [] getContent (@Nonnull final DataSource ds) throws IOException
  {
    return StreamUtils.getAllBytes (StreamUtils.getBuffered (ds.getInputStream ()));
  }

  @Nullable
  public byte [] getTransferData (final DataFlavor df, @Nonnull final DataSource ds) throws IOException
  {
    if (ADF1.equals (df))
      return getContent (ds);
    return null;
  }

  public DataFlavor [] getTransferDataFlavors ()
  {
    return ArrayHelper.getCopy (ADFs);
  }

  public void writeTo (final Object obj, final String mimeType, final OutputStream os) throws IOException
  {
    if (obj instanceof MimeBodyPart)
    {
      try
      {
        ((MimeBodyPart) obj).writeTo (os);
      }
      catch (final MessagingException me)
      {
        throw new IOException (me.getMessage ());
      }
    }
    else
      if (obj instanceof MimeMultipart)
      {
        try
        {
          ((MimeMultipart) obj).writeTo (os);
        }
        catch (final MessagingException me)
        {
          throw new IOException (me.getMessage ());
        }
      }
      else
        if (obj instanceof byte [])
        {
          os.write ((byte []) obj);
        }
        else
          if (obj instanceof String)
          {
            os.write (((String) obj).getBytes ());
          }
          else
          {
            throw new IOException ("Unknown object type: " + obj.getClass ().getName ());
          }
  }
}
