/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2016 Philip Helger philip[at]helger[dot]com
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
import java.util.Date;
import java.util.Map;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.params.InvalidParameterException;
import com.helger.as2lib.processor.sender.IProcessorSenderModule;
import com.helger.as2lib.session.IAS2Session;
import com.helger.as2lib.util.DateHelper;
import com.helger.as2lib.util.IOHelper;
import com.helger.as2lib.util.IStringMap;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.CommonsHashMap;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsMap;

/**
 * An asynchronous, persisting, file based, polling resender module. Upon
 * {@link #handle(String, IMessage, Map)} it writes the document into a file and
 * there is a background poller task that checks for resending (see
 * {@link #resend()}). If re-sending fails, the document is moved into an error
 * folder.
 *
 * @author OpenAS2
 */
public class DirectoryResenderModule extends AbstractActiveResenderModule
{
  public static final String ATTR_RESEND_DIRECTORY = "resenddir";
  public static final String ATTR_ERROR_DIRECTORY = "errordir";

  private static final String FILENAME_DATE_FORMAT = "MM-dd-yy-HH-mm-ss";

  private static final Logger s_aLogger = LoggerFactory.getLogger (DirectoryResenderModule.class);

  @Override
  public void initDynamicComponent (@Nonnull final IAS2Session aSession,
                                    @Nullable final IStringMap aOptions) throws OpenAS2Exception
  {
    super.initDynamicComponent (aSession, aOptions);
    getAttributeAsStringRequired (ATTR_RESEND_DIRECTORY);
    getAttributeAsStringRequired (ATTR_ERROR_DIRECTORY);
  }

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
      final File aResendDir = IOHelper.getDirectoryFile (getAttributeAsStringRequired (ATTR_RESEND_DIRECTORY));
      final File aResendFile = IOHelper.getUniqueFile (aResendDir, getFilename ());
      try (final ObjectOutputStream aOOS = new ObjectOutputStream (new FileOutputStream (aResendFile)))
      {
        String sResendAction = (String) aOptions.get (IProcessorResenderModule.OPTION_RESEND_ACTION);
        if (sResendAction == null)
        {
          s_aLogger.warn ("The resending method is missing - default to message sending!");
          sResendAction = IProcessorSenderModule.DO_SEND;
        }

        String sRetries = (String) aOptions.get (IProcessorResenderModule.OPTION_RETRIES);
        if (sRetries == null)
        {
          s_aLogger.warn ("The resending retry count is missing - default to " +
                          IProcessorResenderModule.DEFAULT_RETRIES +
                          "!");
          sRetries = Integer.toString (IProcessorResenderModule.DEFAULT_RETRIES);
        }

        aOOS.writeObject (sResendAction);
        aOOS.writeObject (sRetries);
        aOOS.writeObject (aMsg);
      }

      s_aLogger.info ("Message put in resend queue" + aMsg.getLoggingText ());
    }
    catch (final IOException ioe)
    {
      throw WrappedOpenAS2Exception.wrap (ioe);
    }
  }

  /**
   * Build the filename for re-sending. The filename consists of the date and
   * time when the document is to be re-send.
   *
   * @return The filename and never <code>null</code>.
   * @throws InvalidParameterException
   *         Only theoretically
   */
  @Nonnull
  protected String getFilename () throws InvalidParameterException
  {
    final long nResendDelayMS = getResendDelayMS ();
    final long nResendTime = new Date ().getTime () + nResendDelayMS;
    return DateHelper.formatDate (FILENAME_DATE_FORMAT, new Date (nResendTime));
  }

  protected boolean isTimeToSend (@Nonnull final File aCurrentFile)
  {
    try
    {
      final StringTokenizer aFileTokens = new StringTokenizer (aCurrentFile.getName (), ".", false);
      final Date aTimestamp = DateHelper.parseDate (FILENAME_DATE_FORMAT, aFileTokens.nextToken ());
      return aTimestamp.before (new Date ());
    }
    catch (final Exception ex)
    {
      return true;
    }
  }

  protected void resendFile (@Nonnull final File aFile) throws OpenAS2Exception
  {
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Processing " + aFile.getAbsolutePath ());

    IMessage aMsg = null;
    try
    {
      try
      {
        String sResendAction;
        String sRetries;
        try (final ObjectInputStream aOIS = new ObjectInputStream (new FileInputStream (aFile)))
        {
          sResendAction = (String) aOIS.readObject ();
          sRetries = (String) aOIS.readObject ();
          aMsg = (IMessage) aOIS.readObject ();
        }

        // Decrement retries
        sRetries = Integer.toString (Integer.parseInt (sRetries) - 1);

        // Transmit the message
        s_aLogger.info ("loaded message for resend." + aMsg.getLoggingText ());

        final ICommonsMap <String, Object> aOptions = new CommonsHashMap<> ();
        aOptions.put (IProcessorResenderModule.OPTION_RETRIES, sRetries);
        getSession ().getMessageProcessor ().handle (sResendAction, aMsg, aOptions);

        if (IOHelper.getFileOperationManager ().deleteFile (aFile).isFailure ())
        {
          // Delete the file, sender will re-queue if the transmission fails
          // again
          throw new OpenAS2Exception ("File was successfully sent but not deleted: " + aFile.getAbsolutePath ());
        }

        s_aLogger.info ("deleted " + aFile.getAbsolutePath () + aMsg.getLoggingText ());
      }
      catch (final IOException ex)
      {
        throw WrappedOpenAS2Exception.wrap (ex);
      }
      catch (final ClassNotFoundException ex)
      {
        throw WrappedOpenAS2Exception.wrap (ex);
      }
    }
    catch (final OpenAS2Exception ex)
    {
      ex.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      ex.addSource (OpenAS2Exception.SOURCE_FILE, aFile);
      ex.terminate ();
      IOHelper.handleError (aFile, getAttributeAsStringRequired (ATTR_ERROR_DIRECTORY));
    }
  }

  /**
   * @return A list with all files that are ready to be resend.
   * @throws InvalidParameterException
   *         In case the directory listing fails
   */
  @Nonnull
  @ReturnsMutableCopy
  protected ICommonsList <File> scanDirectory () throws InvalidParameterException
  {
    final File aResendDir = IOHelper.getDirectoryFile (getAttributeAsStringRequired (ATTR_RESEND_DIRECTORY));

    final File [] aFiles = aResendDir.listFiles ();
    if (aFiles == null)
    {
      throw new InvalidParameterException ("Error getting list of files in directory",
                                           this,
                                           ATTR_RESEND_DIRECTORY,
                                           aResendDir.getAbsolutePath ());
    }

    final ICommonsList <File> ret = new CommonsArrayList<> ();
    if (aFiles.length > 0)
      for (final File aCurrentFile : aFiles)
        if (aCurrentFile.exists () && aCurrentFile.isFile () && aCurrentFile.canWrite () && isTimeToSend (aCurrentFile))
          ret.add (aCurrentFile);
    return ret;
  }

  @Override
  public void resend ()
  {
    try
    {
      // get a list of files that need to be sent now
      final ICommonsList <File> aSendFiles = scanDirectory ();

      // iterator through and send each file
      for (final File aCurrentFile : aSendFiles)
        resendFile (aCurrentFile);
    }
    catch (final OpenAS2Exception ex)
    {
      ex.terminate ();
      forceStop (ex);
    }
  }
}
