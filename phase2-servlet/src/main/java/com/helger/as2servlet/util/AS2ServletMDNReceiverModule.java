/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.as2servlet.util;

import com.helger.as2lib.exception.AS2Exception;
import com.helger.as2lib.processor.receiver.AS2MDNReceiverModule;

/**
 * A specialized {@link AS2MDNReceiverModule} implementation that disables the
 * active parts.
 *
 * @author Philip Helger
 * @since 4.6.4
 */
public class AS2ServletMDNReceiverModule extends AS2MDNReceiverModule
{
  @Override
  public void doStart () throws AS2Exception
  {
    // Would start a thread - so don't do it in the servlet environment
  }

  @Override
  public void doStop () throws AS2Exception
  {
    // Would stop a thread - so don't do it in the servlet environment
  }
}
