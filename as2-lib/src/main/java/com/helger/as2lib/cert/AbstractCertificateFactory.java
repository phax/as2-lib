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
package com.helger.as2lib.cert;

import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.AbstractDynamicComponent;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.partner.Partnership;

/**
 * Abstract implementation of {@link ICertificateFactory}.
 *
 * @author Philip Helger
 */
public abstract class AbstractCertificateFactory extends AbstractDynamicComponent implements ICertificateFactory
{
  @Nonnull
  public abstract String getAlias (@Nonnull final Partnership aPartnership,
                                   @Nonnull final ECertificatePartnershipType ePartnershipType) throws OpenAS2Exception;

  @Nonnull
  protected abstract X509Certificate internalGetCertificate (@Nullable final String sAlias,
                                                             @Nullable final ECertificatePartnershipType ePartnershipType) throws OpenAS2Exception;

  @Nonnull
  public X509Certificate getCertificate (@Nonnull final IMessage aMsg,
                                         @Nullable final ECertificatePartnershipType ePartnershipType) throws OpenAS2Exception
  {
    final String sAlias = getAlias (aMsg.getPartnership (), ePartnershipType);
    return internalGetCertificate (sAlias, ePartnershipType);
  }

  @Nullable
  public X509Certificate getCertificateOrNull (@Nonnull final IMessage aMsg,
                                               @Nullable final ECertificatePartnershipType ePartnershipType) throws OpenAS2Exception
  {
    try
    {
      return getCertificate (aMsg, ePartnershipType);
    }
    catch (final CertificateNotFoundException ex)
    {
      // No such certificate
      return null;
    }
  }

  @Nonnull
  public X509Certificate getCertificate (@Nonnull final IMessageMDN aMDN,
                                         @Nullable final ECertificatePartnershipType ePartnershipType) throws OpenAS2Exception
  {
    final String sAlias = getAlias (aMDN.getPartnership (), ePartnershipType);
    return internalGetCertificate (sAlias, ePartnershipType);
  }

  @Nullable
  public X509Certificate getCertificateOrNull (@Nonnull final IMessageMDN aMDN,
                                               @Nullable final ECertificatePartnershipType ePartnershipType) throws OpenAS2Exception
  {
    try
    {
      return getCertificate (aMDN, ePartnershipType);
    }
    catch (final CertificateNotFoundException ex)
    {
      // No such certificate
      return null;
    }
  }
}
