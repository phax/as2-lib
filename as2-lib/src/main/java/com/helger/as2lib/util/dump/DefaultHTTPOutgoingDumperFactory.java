package com.helger.as2lib.util.dump;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import com.helger.as2lib.message.IBaseMessage;

/**
 * Default implementation of {@link IHTTPOutgoingDumperFactory}. Extracted as a
 * public class in v4.4.5.
 *
 * @author Philip Helger
 */
public class DefaultHTTPOutgoingDumperFactory implements IHTTPOutgoingDumperFactory
{
  // Counter to ensure unique filenames
  private final AtomicInteger m_aCounter = new AtomicInteger (0);
  private final File m_aDumpDirectory;

  public DefaultHTTPOutgoingDumperFactory (@Nonnull final File aDumpDirectory)
  {
    m_aDumpDirectory = aDumpDirectory;
  }

  @Nonnull
  public IHTTPOutgoingDumper apply (@Nonnull final IBaseMessage aMsg)
  {
    return new HTTPOutgoingDumperFileBased (new File (m_aDumpDirectory,
                                                      "as2-outgoing-" +
                                                                        Long.toString (System.currentTimeMillis ()) +
                                                                        "-" +
                                                                        Integer.toString (m_aCounter.getAndIncrement ()) +
                                                                        ".http"));
  }
}
