package com.helger.as2.webapp.module;

import javax.annotation.Nonnull;

import com.helger.as2lib.processor.receiver.net.AS2ReceiverHandler;
import com.helger.as2servlet.util.AS2ServletReceiverModule;

/**
 * Configurable version of {@link AS2ServletReceiverModule}.
 *
 * @author Philip Helger
 */
public class ConfigurableAS2ServletReceiverModule extends AS2ServletReceiverModule
{
  @Override
  @Nonnull
  public AS2ReceiverHandler createHandler ()
  {
    final AS2ReceiverHandler ret = super.createHandler ();
    // Customize receive handler
    ret.setSendExceptionsInMDN (true);
    ret.setSendExceptionStackTraceInMDN (false);
    return ret;
  }
}
