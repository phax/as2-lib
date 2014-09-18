/**
 * Copyright (C) 2014 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
