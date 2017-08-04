package com.helger.as2lib.util;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.mail.internet.InternetHeaders;

import com.helger.commons.http.HttpHeaderMap;

@Immutable
public final class AS2HttpHelper
{
  private AS2HttpHelper ()
  {}

  @Nonnull
  public static InternetHeaders getAsInternetHeaders (@Nonnull final HttpHeaderMap aHeaders)
  {
    final InternetHeaders ret = new InternetHeaders ();
    aHeaders.forEachSingleHeader ( (n, v) -> ret.addHeader (n, v));
    return ret;
  }
}
