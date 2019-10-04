/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2019 Philip Helger philip[at]helger[dot]com
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE FREEBSD PROJECT ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE FREEBSD PROJECT OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the FreeBSD Project.
 */
package com.helger.as2.app;

import java.util.List;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2.app.session.AS2ServerXMLSession;
import com.helger.as2.cmd.CommandManager;
import com.helger.as2.cmd.ICommandRegistry;
import com.helger.as2.cmdprocessor.AbstractCommandProcessor;
import com.helger.as2lib.CAS2Info;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.lang.ClassHelper;
import com.helger.commons.string.StringHelper;

/**
 * original author unknown in this release added ability to have multiple
 * command processors
 *
 * @author joseph mcverry
 */
public class MainOpenAS2Server
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainOpenAS2Server.class);

  public static void main (final String [] args)
  {
    new MainOpenAS2Server ().start (ArrayHelper.getFirst (args));
  }

  public void start (@Nullable final String sConfigFilePath)
  {
    AS2ServerXMLSession aXMLSession = null;
    try
    {
      LOGGER.info (CAS2Info.NAME_VERSION + " - starting Server...");

      // create the OpenAS2 Session object
      // this is used by all other objects to access global configs and
      // functionality
      LOGGER.info ("Loading configuration...");
      if (StringHelper.hasText (sConfigFilePath))
      {
        // Load config file
        aXMLSession = new AS2ServerXMLSession (sConfigFilePath);
      }
      else
      {
        LOGGER.info ("Usage:");
        LOGGER.info ("java " + getClass ().getName () + " <configuration file>");
        throw new Exception ("Missing configuration file name on the commandline. You may specify src/main/resources/config/config.xml");
      }

      // start the active processor modules
      LOGGER.info ("Starting Active Modules...");
      aXMLSession.getMessageProcessor ().startActiveModules ();

      final ICommandRegistry aCommandRegistry = aXMLSession.getCommandRegistry ();
      final CommandManager aCommandMgr = aXMLSession.getCommandManager ();
      final List <AbstractCommandProcessor> aCommandProcessors = aCommandMgr.getProcessors ();
      for (final AbstractCommandProcessor cmd : aCommandProcessors)
      {
        LOGGER.info ("Loading Command Processor " + cmd.getClass ().getName () + "");
        cmd.init ();
        cmd.addCommands (aCommandRegistry);
        new Thread (cmd, ClassHelper.getClassLocalName (cmd)).start ();
      }

      // enter the command processing loop
      LOGGER.info ("OpenAS2 Started");

      // Start waiting for termination
      breakOut: while (true)
      {
        for (final AbstractCommandProcessor cmd : aCommandProcessors)
        {
          if (cmd.isTerminated ())
            break breakOut;
        }
        // Wait outside loop in case no command processor is present
        Thread.sleep (100);
      }
      LOGGER.info ("- OpenAS2 Stopped -");
    }
    catch (final Throwable t)
    {
      t.printStackTrace ();
    }
    finally
    {
      if (aXMLSession != null)
      {
        try
        {
          aXMLSession.getMessageProcessor ().stopActiveModules ();
        }
        catch (final OpenAS2Exception same)
        {
          same.terminate ();
        }
      }

      LOGGER.info ("OpenAS2 has shut down");
    }
  }
}
