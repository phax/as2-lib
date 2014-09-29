/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2014 Philip Helger philip[at]helger[dot]com
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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.GlobalDebug;
import com.helger.commons.string.ToStringGenerator;

/**
 * Implementation of HostnameVerifier always returning <code>true</code>.
 * 
 * @author Philip Helger
 */
public class HostnameVerifierAlwaysTrue implements HostnameVerifier
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (HostnameVerifierAlwaysTrue.class);

  private final boolean m_bDebug;

  public HostnameVerifierAlwaysTrue ()
  {
    this (GlobalDebug.isDebugMode ());
  }

  public HostnameVerifierAlwaysTrue (final boolean bDebug)
  {
    m_bDebug = bDebug;
  }

  /**
   * @return The debug flag as passed in the constructor.
   */
  public boolean isDebug ()
  {
    return m_bDebug;
  }

  public boolean verify (final String sURLHostname, final SSLSession aSession)
  {
    if (m_bDebug)
      s_aLogger.debug ("Hostname '" + sURLHostname + "' is accepted by default in SSL session " + aSession + "!");
    return true;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("debug", m_bDebug).toString ();
  }
}
