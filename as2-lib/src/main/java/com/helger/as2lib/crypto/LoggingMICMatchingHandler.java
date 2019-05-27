package com.helger.as2lib.crypto;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.message.IMessage;

/**
 * Logging implementation of {@link IMICMatchingHandler}
 *
 * @author Philip Helger
 * @since 4.40
 */
public class LoggingMICMatchingHandler implements IMICMatchingHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (LoggingMICMatchingHandler.class);

  public void onMICMatch (@Nonnull final IMessage aMsg, @Nonnull final String sMIC)
  {
    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("MIC is matched, MIC: " + sMIC + aMsg.getLoggingText ());
  }

  public void onMICMismatch (final IMessage aMsg, final String sOriginalMIC, final String sReceivedMIC)
  {
    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("MIC IS NOT MATCHED; original MIC: " +
                   sOriginalMIC +
                   " received MIC: " +
                   sReceivedMIC +
                   aMsg.getLoggingText ());
  }
}
