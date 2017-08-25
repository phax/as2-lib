/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.as2servlet;

import com.helger.commons.http.EHttpMethod;
import com.helger.xservlet.AbstractXServlet;

/**
 * This is the main servlet that takes AS2 messages and processes them. This
 * servlet is configured to accept only POST requests. The logic for receiving
 * is contained in {@link AS2ReceiveXServletHandler}.
 *
 * @author Philip Helger
 */
public final class AS2ReceiveServlet extends AbstractXServlet
{
  public AS2ReceiveServlet ()
  {
    handlerRegistry ().registerHandler (EHttpMethod.POST, new AS2ReceiveXServletHandler ());
  }
}
