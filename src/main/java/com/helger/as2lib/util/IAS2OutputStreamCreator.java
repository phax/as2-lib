package com.helger.as2lib.util;

import java.io.OutputStream;

import javax.annotation.Nonnull;

public interface IAS2OutputStreamCreator
{
  /**
   * @return Never <code>null</code>
   * @throws Exception
   *         In case of error
   */
  @Nonnull
  OutputStream createOutputStream () throws Exception;
}
