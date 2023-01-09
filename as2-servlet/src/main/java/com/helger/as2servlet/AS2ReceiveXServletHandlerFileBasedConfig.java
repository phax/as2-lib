/*
 * Copyright (C) 2015-2023 Philip Helger (www.helger.com)
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

import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;

import com.helger.as2lib.exception.AS2Exception;
import com.helger.as2lib.session.AS2Session;
import com.helger.as2servlet.util.AS2ServletXMLSession;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.string.StringHelper;

/**
 * A special {@link AbstractAS2ReceiveXServletHandler} with a file based
 * configuration.
 *
 * @author Philip Helger
 */
public class AS2ReceiveXServletHandlerFileBasedConfig extends AbstractAS2ReceiveXServletHandler
{
  /**
   * Get the AS2 configuration file to be used. By default this method reads it
   * from the Servlet init-param called
   * {@link #SERVLET_INIT_PARAM_AS2_SERVLET_CONFIG_FILENAME}. You may override
   * this method to use another way of retrieving the configuration file. <br>
   * Note: it must be a {@link File} because the configuration file allows for
   * "%home%" parameter substitution which uses the directory of the
   * configuration file as the base directory.
   *
   * @param aInitParams
   *        Servlet init parameters
   * @return The configuration file to be used. MUST not be <code>null</code>.
   * @throws ServletException
   *         If no or an invalid configuration file was provided.
   */
  @OverrideOnDemand
  @Nonnull
  protected File getConfigurationFile (@Nonnull final ICommonsMap <String, String> aInitParams) throws ServletException
  {
    final String sConfigurationFilename = aInitParams.get (SERVLET_INIT_PARAM_AS2_SERVLET_CONFIG_FILENAME);
    if (StringHelper.hasNoText (sConfigurationFilename))
      throw new ServletException ("Servlet Init-Parameter '" + SERVLET_INIT_PARAM_AS2_SERVLET_CONFIG_FILENAME + "' is missing or empty!");

    try
    {
      return new File (sConfigurationFilename).getCanonicalFile ();
    }
    catch (final IOException ex)
    {
      throw new ServletException ("Failed to get the canonical file from '" + sConfigurationFilename + "'", ex);
    }
  }

  @Override
  @Nonnull
  protected AS2Session createAS2Session (@Nonnull final ICommonsMap <String, String> aInitParams) throws AS2Exception, ServletException
  {
    // Get configuration file
    final File aConfigurationFile = getConfigurationFile (aInitParams);
    if (aConfigurationFile == null)
      throw new ServletException ("No configuration file provided!");

    return new AS2ServletXMLSession (aConfigurationFile);
  }
}
