/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2018 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.processor.sender;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import com.helger.as2lib.AS2GlobalSettings;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.util.http.AS2HttpClient;
import com.helger.as2lib.util.http.AS2HttpURLConnection;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.http.EHttpMethod;
import com.helger.commons.ws.HostnameVerifierVerifyAll;
import com.helger.commons.ws.TrustManagerTrustAll;

/**
 * Abstract HTTP based sender module
 *
 * @author Philip Helger
 */
public abstract class AbstractHttpSenderModule extends AbstractSenderModule
{
  /** Attribute name for connection timeout in milliseconds */
  public static final String ATTR_CONNECT_TIMEOUT = "connecttimeout";
  /** Attribute name for read timeout in milliseconds */
  public static final String ATTR_READ_TIMEOUT = "readtimeout";
  /** Default connection timeout: 60 seconds */
  public static final int DEFAULT_CONNECT_TIMEOUT_MS = 60_000;
  /** Default read timeout: 60 seconds */
  public static final int DEFAULT_READ_TIMEOUT_MS = 60_000;

  /**
   * Create the {@link SSLContext} to be used for https connections. By default
   * the SSL context will trust all hosts and present no keys. Override this
   * method in a subclass to customize this handling.
   *
   * @return The created {@link SSLContext}. May not be <code>null</code>.
   * @throws GeneralSecurityException
   *         If something internally goes wrong.
   */
  @Nonnull
  @OverrideOnDemand
  public static SSLContext createSSLContext () throws GeneralSecurityException
  {
    // Trust all server certificates
    final SSLContext aSSLCtx = SSLContext.getInstance ("TLS");
    final SecureRandom aSecureRandom = AS2GlobalSettings.getSecureRandom ();
    // If aSecureRandom stays null it is also okay

    aSSLCtx.init (null, new TrustManager [] { new TrustManagerTrustAll () }, aSecureRandom);
    return aSSLCtx;
  }

  /**
   * Get the hostname verifier to be used. By default an instance of
   * {@link HostnameVerifierVerifyAll} is returned. Override this method to
   * change this default behavior.
   *
   * @return The hostname verifier to be used. If the returned value is
   *         <code>null</code> it will not be applied to the https connection.
   */
  @Nullable
  @OverrideOnDemand
  public static HostnameVerifier createHostnameVerifier ()
  {
    return new HostnameVerifierVerifyAll ();
  }

  @Nonnull
  public AS2HttpURLConnection getHttpURLConnection (@Nonnull @Nonempty final String sUrl,
                                                    final boolean bOutput,
                                                    final boolean bInput,
                                                    final boolean bUseCaches,
                                                    @Nonnull final EHttpMethod eRequestMethod,
                                                    @Nullable final Proxy aProxy) throws OpenAS2Exception
  {
    try
    {
      final URL aUrlObj = new URL (sUrl);
      final HttpURLConnection aConn = (HttpURLConnection) (aProxy == null ? aUrlObj.openConnection ()
                                                                          : aUrlObj.openConnection (aProxy));
      aConn.setDoOutput (bOutput);
      aConn.setDoInput (bInput);
      aConn.setUseCaches (bUseCaches);
      aConn.setRequestMethod (eRequestMethod.getName ());
      aConn.setConnectTimeout (getAsInt (ATTR_CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT_MS));
      aConn.setReadTimeout (getAsInt (ATTR_READ_TIMEOUT, DEFAULT_READ_TIMEOUT_MS));

      if (aConn instanceof HttpsURLConnection)
      {
        // SSL handling
        final HttpsURLConnection aConns = (HttpsURLConnection) aConn;

        // Create SSL context
        final SSLContext aSSLCtx = createSSLContext ();
        aConns.setSSLSocketFactory (aSSLCtx.getSocketFactory ());

        // Get hostname verifier
        final HostnameVerifier aHV = createHostnameVerifier ();
        if (aHV != null)
          aConns.setHostnameVerifier (aHV);
      }

      return new AS2HttpURLConnection (aConn);
    }
    catch (final IOException | GeneralSecurityException ex)
    {
      throw WrappedOpenAS2Exception.wrap (ex);
    }
  }

  /**
   * Generate a HttpClient connection. It works with streams and avoids holding
   * whole messge in memory. note that bOutput, bInput, and bUseCaches are not
   * supported
   *
   * @param sUrl
   * @param eRequestMethod
   * @param aProxy
   * @return a {@link AS2HttpClient} object to work with
   * @throws OpenAS2Exception
   */
  @Nonnull
  public AS2HttpClient getHttpClient (@Nonnull @Nonempty final String sUrl,
                                      @Nonnull final EHttpMethod eRequestMethod,
                                      @Nullable final Proxy aProxy) throws OpenAS2Exception
  {
    return new AS2HttpClient (sUrl,
                              getAsInt (ATTR_CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT_MS),
                              getAsInt (ATTR_READ_TIMEOUT, DEFAULT_READ_TIMEOUT_MS),
                              eRequestMethod,
                              aProxy);
  }
}
