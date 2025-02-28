/*
 * Copyright (C) 2018-2025 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
