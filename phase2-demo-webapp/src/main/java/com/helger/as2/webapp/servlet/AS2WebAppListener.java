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
package com.helger.as2.webapp.servlet;

import com.helger.web.scope.mgr.WebScopeManager;

import jakarta.annotation.Nonnull;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/**
 * A very simple listener, specifically modified for this demo application. Use
 * this as a &lt;listener&gt; in your <code>web.xml</code>.
 *
 * @author Philip Helger
 * @since 4.8.0
 */
public class AS2WebAppListener implements ServletContextListener
{
  /**
   * Do the global initialization when not using the
   * {@link ServletContextListener}.
   *
   * @param aSC
   *        The servlet context. May not be <code>null</code>.
   */
  public static void staticInit (@Nonnull final ServletContext aSC)
  {
    com.helger.as2servlet.AS2WebAppListener.staticInit (aSC);
    GlobalAS2Session.ensureClassIsLoaded ();
  }

  public void contextInitialized (@Nonnull final ServletContextEvent aSCE)
  {
    final ServletContext aSC = aSCE.getServletContext ();
    staticInit (aSC);
  }

  /**
   * Do the global shutdown.
   */
  public static void staticDestroy ()
  {
    GlobalAS2Session.shutDown ();
    WebScopeManager.onGlobalEnd ();
  }

  public void contextDestroyed (@Nonnull final ServletContextEvent aSce)
  {
    staticDestroy ();
  }
}
