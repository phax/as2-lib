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
package com.helger.as2lib.disposition;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.equals.EqualsUtils;
import com.helger.commons.string.ToStringGenerator;

public final class DispositionType
{
  public static final String ACTION_AUTOMATIC_ACTION = "automatic-action";
  public static final String MDNACTION_MDN_SENT_AUTOMATICALLY = "MDN-sent-automatically";
  public static final String STATUS_PROCESSED = "processed";
  public static final String STATUS_MODIFIER_ERROR = "Error";
  public static final String STATUS_MODIFIER_WARNING = "Warning";

  private final String m_sAction;
  private final String m_sMDNAction;
  private final String m_sStatus;
  private final String m_sStatusDescription;
  private final String m_sStatusModifier;

  public DispositionType (@Nonnull final String sAction,
                          @Nonnull final String sMDNAction,
                          @Nonnull final String sStatus,
                          @Nullable final String sStatusModifier,
                          @Nullable final String sStatusDescription)
  {
    m_sAction = ValueEnforcer.notNull (sAction, "Action");
    m_sMDNAction = ValueEnforcer.notNull (sMDNAction, "MDNAction");
    m_sStatus = ValueEnforcer.notNull (sStatus, "Status");
    m_sStatusModifier = sStatusModifier;
    m_sStatusDescription = sStatusDescription;
  }

  public String getAction ()
  {
    return m_sAction;
  }

  public String getMDNAction ()
  {
    return m_sMDNAction;
  }

  public String getStatus ()
  {
    return m_sStatus;
  }

  public String getStatusDescription ()
  {
    return m_sStatusDescription;
  }

  public String getStatusModifier ()
  {
    return m_sStatusModifier;
  }

  public boolean isWarning ()
  {
    return EqualsUtils.nullSafeEqualsIgnoreCase (m_sStatusModifier, STATUS_MODIFIER_WARNING);
  }

  public void validate () throws DispositionException
  {
    if (m_sStatus == null)
      throw new DispositionException (this, null);
    if (!m_sStatus.equalsIgnoreCase (STATUS_PROCESSED))
      throw new DispositionException (this, null);

    if (m_sStatusModifier != null)
      if (m_sStatusModifier.equalsIgnoreCase (STATUS_MODIFIER_ERROR) ||
          m_sStatusModifier.equalsIgnoreCase (STATUS_MODIFIER_WARNING))
        throw new DispositionException (this, null);
  }

  @Nonnull
  public String getAsString ()
  {
    final StringBuilder aSB = new StringBuilder ();
    aSB.append (m_sAction).append ("/").append (m_sMDNAction).append ("; ").append (m_sStatus);
    if (m_sStatusModifier != null)
    {
      aSB.append ("/").append (m_sStatusModifier).append (":");
      if (m_sStatusDescription != null)
        aSB.append (m_sStatusDescription);
    }
    return aSB.toString ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("action", m_sAction)
                                       .append ("MDNAction", m_sMDNAction)
                                       .append ("status", m_sStatus)
                                       .append ("statusDescription", m_sStatusDescription)
                                       .append ("statusModified", m_sStatusModifier)
                                       .toString ();
  }

  @Nonnull
  public static DispositionType createFromString (@Nonnull final String sDisposition) throws OpenAS2Exception
  {
    if (sDisposition == null)
      throw new OpenAS2Exception ("Invalid disposition type format: " + sDisposition);

    try
    {
      final StringTokenizer aDispTokens = new StringTokenizer (sDisposition, "/;:", false);
      final String sAction = aDispTokens.nextToken ().toLowerCase (Locale.US);
      final String sMDNAction = aDispTokens.nextToken ().toLowerCase (Locale.US);
      final String sStatus = aDispTokens.nextToken ().trim ().toLowerCase (Locale.US);

      String sStatusModifier = null;
      String sStatusDescription = null;
      if (aDispTokens.hasMoreTokens ())
      {
        sStatusModifier = aDispTokens.nextToken ().toLowerCase (Locale.US);
        if (aDispTokens.hasMoreTokens ())
          sStatusDescription = aDispTokens.nextToken ().trim ().toLowerCase (Locale.US);
      }
      return new DispositionType (sAction, sMDNAction, sStatus, sStatusModifier, sStatusDescription);
    }
    catch (final NoSuchElementException ex)
    {
      throw new OpenAS2Exception ("Invalid disposition type format: " + sDisposition, ex);
    }
  }

  @Nonnull
  public static DispositionType createSuccess ()
  {
    return new DispositionType (ACTION_AUTOMATIC_ACTION, MDNACTION_MDN_SENT_AUTOMATICALLY, STATUS_PROCESSED, null, null);
  }

  @Nonnull
  public static DispositionType createError (@Nonnull final String sStatusDescription)
  {
    return new DispositionType (ACTION_AUTOMATIC_ACTION,
                                MDNACTION_MDN_SENT_AUTOMATICALLY,
                                STATUS_PROCESSED,
                                STATUS_MODIFIER_ERROR,
                                sStatusDescription);
  }
}
