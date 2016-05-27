/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2016 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.codec.Base64Codec;
import com.helger.commons.codec.ICodec;
import com.helger.commons.codec.IDecoder;
import com.helger.commons.codec.IdentityCodec;
import com.helger.commons.codec.QuotedPrintableCodec;
import com.helger.commons.codec.RFC1522QCodec;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

/**
 * Predefined Content Transfer Encoding types as per
 * https://www.ietf.org/rfc/rfc2045.txt section 6.1.<br>
 * Of course additional transfer encodings can be used.
 *
 * @author Philip Helger
 */
public enum EContentTransferEncoding implements IHasID <String>
{
  _7BIT ("7bit")
  {
    @Override
    public IdentityCodec <byte []> createDecoder ()
    {
      // Nothing to decode
      return ICodec.identity ();
    }
  },
  _8BIT ("8bit")
  {
    @Override
    public IdentityCodec <byte []> createDecoder ()
    {
      // Nothing to decode
      return ICodec.identity ();
    }
  },
  BINARY ("binary")
  {
    @Override
    public IdentityCodec <byte []> createDecoder ()
    {
      // Nothing to decode
      return ICodec.identity ();
    }
  },
  QUOTED_PRINTABLE ("quoted-printable")
  {
    @Override
    public QuotedPrintableCodec createDecoder ()
    {
      return new QuotedPrintableCodec (RFC1522QCodec.getAllPrintableChars ());
    }
  },
  BASE64 ("base64")
  {
    @Override
    public Base64Codec createDecoder ()
    {
      return new Base64Codec ();
    }
  };

  /** AS2 default CTE is "binary" */
  public static final EContentTransferEncoding AS2_DEFAULT = BINARY;

  private final String m_sID;

  private EContentTransferEncoding (@Nonnull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  /**
   * @return A new decoder for this Content Transfer Encoding. May not be
   *         <code>null</code>.
   */
  @Nonnull
  public abstract IDecoder <byte [], byte []> createDecoder ();

  @Nullable
  public static EContentTransferEncoding getFromIDCaseInsensitiveOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDCaseInsensitiveOrNull (EContentTransferEncoding.class, sID);
  }

  @Nullable
  public static EContentTransferEncoding getFromIDCaseInsensitiveOrDefault (@Nullable final String sID,
                                                                            @Nullable final EContentTransferEncoding eDefault)
  {
    return EnumHelper.getFromIDCaseInsensitiveOrDefault (EContentTransferEncoding.class, sID, eDefault);
  }
}
