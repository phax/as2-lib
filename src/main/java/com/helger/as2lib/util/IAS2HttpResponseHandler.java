package com.helger.as2lib.util;

import java.io.IOException;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.mail.internet.InternetHeaders;

import com.helger.commons.io.streams.NonBlockingByteArrayOutputStream;

public interface IAS2HttpResponseHandler
{
  /**
   * Added an HTTP header to the response. This method must be called before any
   * output is written.
   *
   * @param nHttpResponseCode
   *        The HTTP response code. E.g. 200 for "HTTP OK".
   * @param aHeaders
   *        Headers to use. May not be <code>null</code>.
   * @param aData
   *        Data to send as response body. May not be <code>null</code> but may
   *        be empty.
   * @throws IOException
   *         In case of error
   */
  void sendHttpResponse (@Nonnegative int nHttpResponseCode,
                         @Nonnull InternetHeaders aHeaders,
                         @Nonnull NonBlockingByteArrayOutputStream aData) throws IOException;
}
