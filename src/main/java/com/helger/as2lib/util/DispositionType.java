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
package com.helger.as2lib.util;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.exception.DispositionException;
import com.helger.as2lib.exception.OpenAS2Exception;

public class DispositionType
{
  private String m_sAction;
  private String m_sMDNAction;
  private String m_sStatus;
  private String m_sStatusDescription;
  private String m_sStatusModifier;

  public DispositionType (final String sAction,
                          final String sMDNAction,
                          final String sStatus,
                          final String sStatusModifier,
                          final String sStatusDescription)
  {
    m_sAction = sAction;
    m_sMDNAction = sMDNAction;
    m_sStatus = sStatus;
    m_sStatusModifier = sStatusModifier;
    m_sStatusDescription = sStatusDescription;
  }

  public DispositionType (final String sAction, final String sMDNAction, final String sStatus)
  {
    this (sAction, sMDNAction, sStatus, null, null);
  }

  public DispositionType (@Nullable final String sDisposition) throws OpenAS2Exception
  {
    if (sDisposition != null)
      parseDisposition (sDisposition);
  }

  public void setAction (final String sAction)
  {
    m_sAction = sAction;
  }

  public String getAction ()
  {
    return m_sAction;
  }

  public void setMDNAction (final String sMDNAction)
  {
    m_sMDNAction = sMDNAction;
  }

  public String getMDNAction ()
  {
    return m_sMDNAction;
  }

  public void setStatus (final String sStatus)
  {
    m_sStatus = sStatus;
  }

  public String getStatus ()
  {
    return m_sStatus;
  }

  public void setStatusDescription (final String sStatusDescription)
  {
    m_sStatusDescription = sStatusDescription;
  }

  public String getStatusDescription ()
  {
    return m_sStatusDescription;
  }

  public void setStatusModifier (final String sStatusModifier)
  {
    m_sStatusModifier = sStatusModifier;
  }

  public String getStatusModifier ()
  {
    return m_sStatusModifier;
  }

  public boolean isWarning ()
  {
    final String sStatusMod = getStatusModifier ();
    return sStatusMod != null && sStatusMod.equalsIgnoreCase ("warning");
  }

  @Override
  public String toString ()
  {
    return makeDisposition ();
  }

  public void validate () throws DispositionException
  {
    final String sStatus = getStatus ();
    if (sStatus == null)
      throw new DispositionException (this, null);
    if (!sStatus.equalsIgnoreCase ("processed"))
      throw new DispositionException (this, null);

    final String sStatusMod = getStatusModifier ();
    if (sStatusMod != null)
      if (sStatusMod.equalsIgnoreCase ("error") || sStatusMod.equalsIgnoreCase ("warning"))
        throw new DispositionException (this, null);
  }

  @Nonnull
  protected String makeDisposition ()
  {
    final StringBuilder aDispBuf = new StringBuilder ();
    aDispBuf.append (getAction ()).append ("/").append (getMDNAction ()).append ("; ").append (getStatus ());

    if (getStatusModifier () != null)
    {
      aDispBuf.append ("/").append (getStatusModifier ()).append (":");
      if (getStatusDescription () != null)
        aDispBuf.append (getStatusDescription ());
    }

    return aDispBuf.toString ();
  }

  protected void parseDisposition (final String sDisposition) throws OpenAS2Exception
  {
    final StringTokenizer aDispTokens = new StringTokenizer (sDisposition, "/;:", false);
    try
    {
      setAction (aDispTokens.nextToken ().toLowerCase (Locale.US));
      setMDNAction (aDispTokens.nextToken ().toLowerCase (Locale.US));
      setStatus (aDispTokens.nextToken ().trim ().toLowerCase (Locale.US));
      setStatusModifier (null);
      setStatusDescription (null);

      if (aDispTokens.hasMoreTokens ())
      {
        setStatusModifier (aDispTokens.nextToken ().toLowerCase (Locale.US));
        if (aDispTokens.hasMoreTokens ())
          setStatusDescription (aDispTokens.nextToken ().trim ().toLowerCase (Locale.US));
      }
    }
    catch (final NoSuchElementException ex)
    {
      throw new OpenAS2Exception ("Invalid disposition type format: " + sDisposition);
    }
  }
}
