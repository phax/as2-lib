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
package com.helger.as2lib.cert;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.IDynamicComponent;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;

/**
 * Base interface for a certificate factory.
 *
 * @author unknown
 */
public interface ICertificateFactory extends IDynamicComponent
{
  @Nonnull
  X509Certificate getCertificate (@Nonnull IMessage aMsg, @Nullable ECertificatePartnershipType ePartnershipType) throws OpenAS2Exception;

  @Nullable
  X509Certificate getCertificateOrNull (@Nonnull IMessage aMsg, @Nullable ECertificatePartnershipType ePartnershipType) throws OpenAS2Exception;

  @Nonnull
  PrivateKey getPrivateKey (@Nullable IMessage aMsg, @Nullable X509Certificate aCert) throws OpenAS2Exception;

  @Nonnull
  X509Certificate getCertificate (@Nonnull IMessageMDN aMDN, @Nullable ECertificatePartnershipType ePartnershipType) throws OpenAS2Exception;

  @Nullable
  X509Certificate getCertificateOrNull (@Nonnull IMessageMDN aMDN,
                                        @Nullable ECertificatePartnershipType ePartnershipType) throws OpenAS2Exception;

  @Nonnull
  PrivateKey getPrivateKey (@Nullable IMessageMDN aMDN, @Nullable X509Certificate aCert) throws OpenAS2Exception;
}
