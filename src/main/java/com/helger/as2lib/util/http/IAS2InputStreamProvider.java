package com.helger.as2lib.util.http;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;

public interface IAS2InputStreamProvider
{
  /**
   * @return Never <code>null</code>
   * @throws IOException
   *         In case of error
   */
  @Nonnull
  InputStream getInputStream () throws IOException;
}
