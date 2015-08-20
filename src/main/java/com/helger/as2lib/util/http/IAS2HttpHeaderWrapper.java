package com.helger.as2lib.util.http;

import javax.annotation.Nonnull;

/**
 * A wrapper to set HTTP headers on an object - abstraction layer between
 * HttpUrlConnection and HttpClient.
 *
 * @author Philip Helger
 */
public interface IAS2HttpHeaderWrapper
{
  /**
   * Set an HTTP header
   * 
   * @param sName
   *        Header name
   * @param sValue
   *        Header value
   */
  void setHttpHeader (@Nonnull String sName, @Nonnull String sValue);
}
