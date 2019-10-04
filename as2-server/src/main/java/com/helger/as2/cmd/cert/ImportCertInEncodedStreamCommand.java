/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2019 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2.cmd.cert;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import com.helger.as2.cmd.CommandResult;
import com.helger.as2.cmd.ECommandResultType;
import com.helger.as2.util.ByteCoder;
import com.helger.as2lib.cert.IAliasedCertificateFactory;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;

public class ImportCertInEncodedStreamCommand extends AbstractAliasedCertCommand
{
  @Override
  public String getDefaultDescription ()
  {
    return "Import a certificate into the current certificate store using an encoded byte stream";
  }

  @Override
  public String getDefaultName ()
  {
    return "importbystream";
  }

  @Override
  public String getDefaultUsage ()
  {
    return "importbybstream <alias> <encodedCertificateStream>";
  }

  @Override
  public CommandResult execute (final IAliasedCertificateFactory certFx, final Object [] params) throws OpenAS2Exception
  {
    if (params.length != 2)
    {
      return new CommandResult (ECommandResultType.TYPE_INVALID_PARAM_COUNT, getUsage ());
    }

    synchronized (certFx)
    {
      try
      {
        return _importCert (certFx, params[0].toString (), params[1].toString ());
      }
      catch (final Exception ex)
      {
        throw WrappedOpenAS2Exception.wrap (ex);
      }
    }
  }

  private CommandResult _importCert (final IAliasedCertificateFactory certFx,
                                     final String alias,
                                     final String encodedCert) throws CertificateException, OpenAS2Exception
  {

    final NonBlockingByteArrayInputStream bais = new NonBlockingByteArrayInputStream (ByteCoder.decode (encodedCert)
                                                                                               .getBytes ());
    final CertificateFactory cf = CertificateFactory.getInstance ("X.509");
    while (bais.available () > 0)
    {
      final Certificate cert = cf.generateCertificate (bais);
      if (cert instanceof X509Certificate)
      {
        certFx.addCertificate (alias, (X509Certificate) cert, true);

        final CommandResult cmdRes = new CommandResult (ECommandResultType.TYPE_OK,
                                                        "Certificate(s) imported successfully");
        cmdRes.addResult ("Imported certificate: " + cert.toString ());
        return cmdRes;
      }
    }

    return new CommandResult (ECommandResultType.TYPE_ERROR, "No valid X509 certificates found");
  }
}
