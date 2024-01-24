/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
package com.helger.as2servlet.mdn;

import javax.annotation.Nonnull;

import com.helger.as2lib.exception.AS2Exception;
import com.helger.as2lib.session.AS2Session;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.ICommonsMap;

import jakarta.servlet.ServletException;

/**
 * A special {@link AbstractAS2MDNReceiveXServletHandler} with an external
 * {@link AS2Session}.
 *
 * @author Philip Helger
 * @since 4.8.0
 */
public class AS2MDNReceiveXServletHandlerConstantSession extends AbstractAS2MDNReceiveXServletHandler
{
  private final AS2Session m_aSession;

  public AS2MDNReceiveXServletHandlerConstantSession (@Nonnull final AS2Session aSession)
  {
    ValueEnforcer.notNull (aSession, "Session");
    m_aSession = aSession;
  }

  @Override
  @Nonnull
  protected AS2Session createAS2Session (@Nonnull final ICommonsMap <String, String> aInitParams) throws AS2Exception, ServletException
  {
    return m_aSession;
  }
}
