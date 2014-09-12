/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2014 Philip Helger philip[at]helger[dot]com
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
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.InvalidParameterException;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.processor.sender.IProcessorSenderModule;
import com.helger.as2lib.session.ISession;
import com.helger.as2lib.util.DateUtil;
import com.helger.as2lib.util.IOUtil;
import com.helger.as2lib.util.IStringMap;
import com.helger.commons.CGlobal;
import com.helger.commons.annotations.ReturnsMutableCopy;

public class DirectoryResenderModule extends AbstractResenderModule
{
  private static final String DATE_FORMAT = "MM-dd-yy-HH-mm-ss";
  public static final String PARAM_RESEND_DIRECTORY = "resenddir";
  public static final String PARAM_ERROR_DIRECTORY = "errordir";
  // in seconds
  public static final String PARAM_RESEND_DELAY = "resenddelay";

  // TODO Resend set to 15 minutes. Implement a scaling resend time with
  // eventual permanent failure of transmission
  // 15 minutes
  public static final long DEFAULT_RESEND_DELAY = 15 * CGlobal.MILLISECONDS_PER_MINUTE;

  private static final Logger s_aLogger = LoggerFactory.getLogger (DirectoryResenderModule.class);

  @Override
  public boolean canHandle (@Nonnull final String sAction,
                            @Nonnull final IMessage aMsg,
                            @Nullable final Map <String, Object> aOptions)
  {
    return sAction.equals (IProcessorResenderModule.DO_RESEND);
  }

  @Override
  public void handle (@Nonnull final String sAction,
                      @Nonnull final IMessage aMsg,
                      @Nullable final Map <String, Object> aOptions) throws OpenAS2Exception
  {
    try
    {
      final File aResendDir = IOUtil.getDirectoryFile (getAttributeAsStringRequired (PARAM_RESEND_DIRECTORY));
      final File aResendFile = IOUtil.getUniqueFile (aResendDir, getFilename ());
      final ObjectOutputStream aOOS = new ObjectOutputStream (new FileOutputStream (aResendFile));
      String sMethod = (String) aOptions.get (IProcessorResenderModule.OPTION_RESEND_METHOD);
      if (sMethod == null)
        sMethod = IProcessorSenderModule.DO_SEND;
      String sRetries = (String) aOptions.get (IProcessorResenderModule.OPTION_RETRIES);
      if (sRetries == null)
        sRetries = "-1";
      aOOS.writeObject (sMethod);
      aOOS.writeObject (sRetries);
      aOOS.writeObject (aMsg);
      aOOS.close ();

      s_aLogger.info ("message put in resend queue" + aMsg.getLoggingText ());
    }
    catch (final IOException ioe)
    {
      throw new WrappedOpenAS2Exception (ioe);
    }
  }

  @Override
  public void initDynamicComponent (@Nonnull final ISession aSession, @Nullable final IStringMap aOptions) throws OpenAS2Exception
  {
    super.initDynamicComponent (aSession, aOptions);
    getAttributeAsStringRequired (PARAM_RESEND_DIRECTORY);
    getAttributeAsStringRequired (PARAM_ERROR_DIRECTORY);
  }

  @Override
  public void resend ()
  {
    try
    {
      // get a list of files that need to be sent now
      final List <File> aSendFiles = scanDirectory ();

      // iterator through and send each file
      for (final File aCurrentFile : aSendFiles)
        processFile (aCurrentFile);
    }
    catch (final OpenAS2Exception ex)
    {
      ex.terminate ();
      forceStop (ex);
    }
  }

  @Nonnull
  protected String getFilename () throws InvalidParameterException
  {
    long nResendDelay;
    if (getAttributeAsString (PARAM_RESEND_DELAY) == null)
      nResendDelay = DEFAULT_RESEND_DELAY;
    else
      nResendDelay = getAttributeAsIntRequired (PARAM_RESEND_DELAY) * CGlobal.MILLISECONDS_PER_SECOND;

    final long nResendTime = new Date ().getTime () + nResendDelay;
    return DateUtil.formatDate (DATE_FORMAT, new Date (nResendTime));
  }

  protected boolean isTimeToSend (@Nonnull final File aCurrentFile)
  {
    try
    {
      final StringTokenizer aFileTokens = new StringTokenizer (aCurrentFile.getName (), ".", false);
      final Date aTimestamp = DateUtil.parseDate (DATE_FORMAT, aFileTokens.nextToken ());
      return aTimestamp.before (new Date ());
    }
    catch (final Exception ex)
    {
      return true;
    }
  }

  protected void processFile (@Nonnull final File aFile) throws OpenAS2Exception
  {
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("processing " + aFile.getAbsolutePath ());

    IMessage aMsg = null;

    try
    {
      try
      {
        final ObjectInputStream aOIS = new ObjectInputStream (new FileInputStream (aFile));
        String sMethod;
        String sRetries;
        try
        {
          sMethod = (String) aOIS.readObject ();
          sRetries = (String) aOIS.readObject ();
          aMsg = (IMessage) aOIS.readObject ();
        }
        finally
        {
          aOIS.close ();
        }

        // Transmit the message
        s_aLogger.info ("loaded message for resend." + aMsg.getLoggingText ());

        final Map <String, Object> aOptions = new HashMap <String, Object> ();
        aOptions.put (IProcessorSenderModule.SENDER_OPTION_RETRIES, sRetries);
        getSession ().getProcessor ().handle (sMethod, aMsg, aOptions);

        if (IOUtil.getFileOperationManager ().deleteFile (aFile).isFailure ())
        {
          // Delete the file, sender will re-queue if the transmission fails
          // again
          throw new OpenAS2Exception ("File was successfully sent but not deleted: " + aFile.getAbsolutePath ());
        }

        s_aLogger.info ("deleted " + aFile.getAbsolutePath () + aMsg.getLoggingText ());
      }
      catch (final IOException ex)
      {
        throw new WrappedOpenAS2Exception (ex);
      }
      catch (final ClassNotFoundException ex)
      {
        throw new WrappedOpenAS2Exception (ex);
      }
    }
    catch (final OpenAS2Exception ex)
    {
      ex.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      ex.addSource (OpenAS2Exception.SOURCE_FILE, aFile);
      ex.terminate ();
      IOUtil.handleError (aFile, getAttributeAsStringRequired (PARAM_ERROR_DIRECTORY));
    }
  }

  @Nonnull
  @ReturnsMutableCopy
  protected List <File> scanDirectory () throws OpenAS2Exception
  {
    final File aResendDir = IOUtil.getDirectoryFile (getAttributeAsStringRequired (PARAM_RESEND_DIRECTORY));

    final File [] aFiles = aResendDir.listFiles ();
    if (aFiles == null)
    {
      throw new InvalidParameterException ("Error getting list of files in directory",
                                           this,
                                           PARAM_RESEND_DIRECTORY,
                                           aResendDir.getAbsolutePath ());
    }

    final List <File> ret = new ArrayList <File> ();
    if (aFiles.length > 0)
      for (final File aCurrentFile : aFiles)
        if (aCurrentFile.exists () && aCurrentFile.isFile () && aCurrentFile.canWrite () && isTimeToSend (aCurrentFile))
          ret.add (aCurrentFile);
    return ret;
  }
}
