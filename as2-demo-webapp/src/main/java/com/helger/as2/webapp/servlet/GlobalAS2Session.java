package com.helger.as2.webapp.servlet;

import java.io.File;

import javax.annotation.concurrent.Immutable;

import com.helger.as2lib.exception.AS2Exception;
import com.helger.as2lib.session.AS2ComponentNotFoundException;
import com.helger.as2servlet.util.AS2ServletXMLSession;
import com.helger.commons.exception.InitializationException;

/**
 * Wrapper around the global AS2 session.
 *
 * @author Philip Helger
 * @since 4.8.0
 */
@Immutable
public final class GlobalAS2Session
{
  public static final AS2ServletXMLSession AS2_SESSION;
  static
  {
    try
    {
      // Load the configuration from that file
      // In a real-world application you can make this path customizable e.g.
      // via a system property or an environment variable.
      AS2_SESSION = new AS2ServletXMLSession (new File ("config/config.xml"));
      // Start them once
      AS2_SESSION.getMessageProcessor ().startActiveModules ();
    }
    catch (final AS2Exception ex)
    {
      throw new InitializationException (ex);
    }
  }

  private GlobalAS2Session ()
  {}

  static void ensureClassIsLoaded ()
  {
    // Do nothing
  }

  /**
   * Call this method at the application end, to shutdown all active modules.
   */
  public static void shutDown ()
  {
    if (AS2_SESSION != null)
      try
      {
        AS2_SESSION.getMessageProcessor ().stopActiveModules ();
      }
      catch (final AS2ComponentNotFoundException ex)
      {
        // we don't care on shut down
      }
  }
}
