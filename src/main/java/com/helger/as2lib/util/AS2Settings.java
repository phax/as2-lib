/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2015 Philip Helger philip[at]helger[dot]com
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

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This class contains global settings to be used with the AS2 class.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class AS2Settings
{
  private static final ReadWriteLock s_aRWLock = new ReentrantReadWriteLock ();
  @GuardedBy ("s_aRWLock")
  private static boolean s_bCryptoVerifyUseCertificateInBodyPart = true;

  private AS2Settings ()
  {}

  /**
   * @return <code>true</code> if any certificate passed in a message body is
   *         used for certificate verification or <code>false</code> if only the
   *         certificate present in the partnership factory is to be used.
   */
  public static boolean isCryptoVerifyUseCertificateInBodyPart ()
  {
    s_aRWLock.readLock ().lock ();
    try
    {
      return s_bCryptoVerifyUseCertificateInBodyPart;
    }
    finally
    {
      s_aRWLock.readLock ().unlock ();
    }
  }

  public static void setCryptoVerifyUseCertificateInBodyPart (final boolean bCryptoVerifyUseCertificateInBodyPart)
  {
    s_aRWLock.writeLock ().lock ();
    try
    {
      s_bCryptoVerifyUseCertificateInBodyPart = bCryptoVerifyUseCertificateInBodyPart;
    }
    finally
    {
      s_aRWLock.writeLock ().unlock ();
    }
  }
}
