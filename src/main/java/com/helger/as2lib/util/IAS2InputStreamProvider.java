package com.helger.as2lib.util;

import java.io.InputStream;

import javax.annotation.Nonnull;

public interface IAS2InputStreamProvider
{
  /**
   * @return Never <code>null</code>
   * @throws Exception
   *         In case of error
   */
  @Nonnull
  InputStream getOutputStream () throws Exception;
}
