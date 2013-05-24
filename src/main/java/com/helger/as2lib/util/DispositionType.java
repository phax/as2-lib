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
package com.helger.as2lib.util;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import com.helger.as2lib.exception.OpenAS2Exception;

public class DispositionType
{
  private String m_sAction;
  private String m_sMDNAction;
  private String m_sStatus;
  private String m_sStatusDescription;
  private String m_sStatusModifier;

  public DispositionType (final String action,
                          final String mdnAction,
                          final String status,
                          final String statusModifier,
                          final String statusDescription)
  {
    m_sAction = action;
    m_sMDNAction = mdnAction;
    m_sStatus = status;
    m_sStatusModifier = statusModifier;
    m_sStatusDescription = statusDescription;
  }

  public DispositionType (final String action, final String mdnAction, final String status)
  {
    this (action, mdnAction, status, null, null);
  }

  public DispositionType (final String disposition) throws OpenAS2Exception
  {
    if (disposition != null)
    {
      parseDisposition (disposition);
    }
  }

  public void setAction (final String action)
  {
    m_sAction = action;
  }

  public String getAction ()
  {
    return m_sAction;
  }

  public void setMdnAction (final String mdnAction)
  {
    m_sMDNAction = mdnAction;
  }

  public String getMdnAction ()
  {
    return m_sMDNAction;
  }

  public void setStatus (final String status)
  {
    m_sStatus = status;
  }

  public String getStatus ()
  {
    return m_sStatus;
  }

  public void setStatusDescription (final String statusDescription)
  {
    m_sStatusDescription = statusDescription;
  }

  public String getStatusDescription ()
  {
    return m_sStatusDescription;
  }

  public void setStatusModifier (final String statusModifier)
  {
    m_sStatusModifier = statusModifier;
  }

  public String getStatusModifier ()
  {
    return m_sStatusModifier;
  }

  public boolean isWarning ()
  {
    final String statusMod = getStatusModifier ();

    return ((statusMod != null) && statusMod.equalsIgnoreCase ("warning"));
  }

  @Override
  public String toString ()
  {
    return makeDisposition ();
  }

  public void validate () throws DispositionException
  {
    final String status = getStatus ();

    if (status == null)
    {
      throw new DispositionException (this, null);
    }
    else
      if (!status.equalsIgnoreCase ("processed"))
      {
        throw new DispositionException (this, null);
      }

    final String statusMod = getStatusModifier ();

    if (statusMod != null)
    {
      if (statusMod.equalsIgnoreCase ("error") || statusMod.equalsIgnoreCase ("warning"))
      {
        throw new DispositionException (this, null);
      }
    }
  }

  protected String makeDisposition ()
  {
    final StringBuilder dispBuf = new StringBuilder ();
    dispBuf.append (getAction ()).append ("/").append (getMdnAction ());
    dispBuf.append ("; ").append (getStatus ());

    if (getStatusModifier () != null)
    {
      dispBuf.append ("/").append (getStatusModifier ()).append (":");

      if (getStatusDescription () != null)
      {
        dispBuf.append (getStatusDescription ());
      }
    }

    return dispBuf.toString ();
  }

  protected void parseDisposition (final String disposition) throws OpenAS2Exception
  {
    final StringTokenizer dispTokens = new StringTokenizer (disposition, "/;:", false);

    try
    {
      setAction (dispTokens.nextToken ().toLowerCase (Locale.US));
      setMdnAction (dispTokens.nextToken ().toLowerCase (Locale.US));
      setStatus (dispTokens.nextToken ().trim ().toLowerCase (Locale.US));
      setStatusModifier (null);
      setStatusDescription (null);

      if (dispTokens.hasMoreTokens ())
      {
        setStatusModifier (dispTokens.nextToken ().toLowerCase (Locale.US));

        if (dispTokens.hasMoreTokens ())
        {
          setStatusDescription (dispTokens.nextToken ().trim ().toLowerCase (Locale.US));
        }
      }
    }
    catch (final NoSuchElementException nsee)
    {
      throw new OpenAS2Exception ("Invalid disposition type format: " + disposition);
    }
  }
}
