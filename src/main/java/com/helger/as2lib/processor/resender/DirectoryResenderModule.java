/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013 Philip Helger ph[at]phloc[dot]com
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
package com.helger.as2lib.processor.resender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.ISession;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedException;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.params.InvalidParameterException;
import com.helger.as2lib.processor.sender.IProcessorSenderModule;
import com.helger.as2lib.util.DateUtil;
import com.helger.as2lib.util.IOUtil;
import com.phloc.commons.CGlobal;

public class DirectoryResenderModule extends AbstractResenderModule
{
  public static final String PARAM_RESEND_DIRECTORY = "resenddir";
  public static final String PARAM_ERROR_DIRECTORY = "errordir";
  // in seconds
  public static final String PARAM_RESEND_DELAY = "resenddelay";

  // TODO Resend set to 15 minutes. Implement a scaling resend time with
  // eventual permanent failure of transmission
  // 15 minutes
  public static final long DEFAULT_RESEND_DELAY = 15 * CGlobal.MILLISECONDS_PER_MINUTE;

  private static final Logger logger = LoggerFactory.getLogger (DirectoryResenderModule.class);

  @Override
  public boolean canHandle (final String action, final IMessage msg, final Map <String, Object> options)
  {
    return action.equals (IProcessorResenderModule.DO_RESEND);
  }

  @Override
  public void handle (final String action, final IMessage msg, final Map <String, Object> options) throws OpenAS2Exception
  {
    try
    {
      final File resendDir = IOUtil.getDirectoryFile (getParameterRequired (PARAM_RESEND_DIRECTORY));
      final File resendFile = IOUtil.getUnique (resendDir, getFilename ());
      final ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream (resendFile));
      String method = (String) options.get (IProcessorResenderModule.OPTION_RESEND_METHOD);
      if (method == null)
        method = IProcessorSenderModule.DO_SEND;
      String retries = (String) options.get (IProcessorResenderModule.OPTION_RETRIES);
      if (retries == null)
        retries = "-1";
      oos.writeObject (method);
      oos.writeObject (retries);
      oos.writeObject (msg);
      oos.close ();

      logger.info ("message put in resend queue" + msg.getLoggingText ());
    }
    catch (final IOException ioe)
    {
      throw new WrappedException (ioe);
    }
  }

  @Override
  public void initDynamicComponent (final ISession session, final Map <String, String> options) throws OpenAS2Exception
  {
    super.initDynamicComponent (session, options);
    getParameterRequired (PARAM_RESEND_DIRECTORY);
    getParameterRequired (PARAM_ERROR_DIRECTORY);
  }

  @Override
  public void resend ()
  {
    try
    {
      // get a list of files that need to be sent now
      final List <File> sendFiles = scanDirectory ();

      // iterator through and send each file
      final Iterator <File> fileIt = sendFiles.iterator ();
      File currentFile;

      while (fileIt.hasNext ())
      {
        currentFile = fileIt.next ();
        processFile (currentFile);
      }
    }
    catch (final OpenAS2Exception oae)
    {
      oae.terminate ();
      forceStop (oae);
    }
  }

  protected String getFilename () throws InvalidParameterException
  {
    long resendDelay;
    if (getParameterNotRequired (PARAM_RESEND_DELAY) == null)
    {
      resendDelay = DEFAULT_RESEND_DELAY;
    }
    else
    {
      resendDelay = getParameterInt (PARAM_RESEND_DELAY) * CGlobal.MILLISECONDS_PER_SECOND;
    }
    final long resendTime = new Date ().getTime () + resendDelay;

    return DateUtil.formatDate ("MM-dd-yy-HH-mm-ss", new Date (resendTime));
  }

  protected boolean isTimeToSend (final File currentFile)
  {
    try
    {
      final StringTokenizer fileTokens = new StringTokenizer (currentFile.getName (), ".", false);

      final Date timestamp = DateUtil.parseDate ("MM-dd-yy-HH-mm-ss", fileTokens.nextToken ());

      return timestamp.before (new Date ());
    }
    catch (final Exception e)
    {
      return true;
    }
  }

  protected void processFile (final File file) throws OpenAS2Exception
  {
    logger.debug ("processing " + file.getAbsolutePath ());

    IMessage msg = null;

    try
    {
      try
      {
        final ObjectInputStream ois = new ObjectInputStream (new FileInputStream (file));
        final String method = (String) ois.readObject ();
        final String retries = (String) ois.readObject ();
        msg = (IMessage) ois.readObject ();
        ois.close ();

        // Transmit the message
        logger.info ("loaded message for resend." + msg.getLoggingText ());

        final Map <String, Object> options = new HashMap <String, Object> ();
        options.put (IProcessorSenderModule.SOPT_RETRIES, retries);
        getSession ().getProcessor ().handle (method, msg, options);

        if (!file.delete ())
        { // Delete the file, sender will re-queue if the transmission fails
          // again
          throw new OpenAS2Exception ("File was successfully sent but not deleted: " + file.getAbsolutePath ());
        }

        logger.info ("deleted " + file.getAbsolutePath () + msg.getLoggingText ());
      }
      catch (final IOException ioe)
      {
        throw new WrappedException (ioe);
      }
      catch (final ClassNotFoundException cnfe)
      {
        throw new WrappedException (cnfe);
      }
    }
    catch (final OpenAS2Exception oae)
    {
      oae.addSource (OpenAS2Exception.SOURCE_MESSAGE, msg);
      oae.addSource (OpenAS2Exception.SOURCE_FILE, file);
      oae.terminate ();
      IOUtil.handleError (file, getParameterRequired (PARAM_ERROR_DIRECTORY));
    }
  }

  protected List <File> scanDirectory () throws OpenAS2Exception
  {
    final File resendDir = IOUtil.getDirectoryFile (getParameterRequired (PARAM_RESEND_DIRECTORY));
    final List <File> sendFiles = new ArrayList <File> ();

    final File [] files = resendDir.listFiles ();

    if (files == null)
    {
      throw new InvalidParameterException ("Error getting list of files in directory",
                                           this,
                                           PARAM_RESEND_DIRECTORY,
                                           resendDir.getAbsolutePath ());
    }

    if (files.length > 0)
    {
      File currentFile;

      for (final File file : files)
      {
        currentFile = file;

        if (currentFile.exists () && currentFile.isFile () && currentFile.canWrite ())
        {
          if (isTimeToSend (currentFile))
          {
            sendFiles.add (currentFile);
          }
        }
      }
    }

    return sendFiles;
  }
}
