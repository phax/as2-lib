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
package com.helger.as2lib.processor.receiver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.ISession;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedException;
import com.helger.as2lib.message.CFileAttribute;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.InvalidMessageException;
import com.helger.as2lib.params.AbstractParameterParser;
import com.helger.as2lib.params.InvalidParameterException;
import com.helger.as2lib.params.MessageParameters;
import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.processor.sender.IProcessorSenderModule;
import com.helger.as2lib.util.IOUtil;
import com.helger.as2lib.util.javamail.ByteArrayDataSource;
import com.phloc.commons.io.file.FileIOError;
import com.phloc.commons.io.file.FileOperations;
import com.phloc.commons.io.file.SimpleFileIO;

public abstract class AbstractDirectoryPollingModule extends AbstractPollingModule
{
  public static final String PARAM_OUTBOX_DIRECTORY = "outboxdir";
  public static final String PARAM_ERROR_DIRECTORY = "errordir";
  public static final String PARAM_SENT_DIRECTORY = "sentdir";
  public static final String PARAM_FORMAT = "format";
  public static final String PARAM_DELIMITERS = "delimiters";
  public static final String PARAM_DEFAULTS = "defaults";
  public static final String PARAM_MIMETYPE = "mimetype";
  private Map <String, Long> m_aTrackedFiles;

  private final Logger s_aLogger = LoggerFactory.getLogger (AbstractDirectoryPollingModule.class);

  @Override
  public void initDynamicComponent (final ISession session, final Map <String, String> options) throws OpenAS2Exception
  {
    super.initDynamicComponent (session, options);
    getParameterRequired (PARAM_OUTBOX_DIRECTORY);
    getParameterRequired (PARAM_ERROR_DIRECTORY);
  }

  @Override
  public void poll ()
  {
    try
    {
      // scan the directory for new files
      scanDirectory (getParameterRequired (PARAM_OUTBOX_DIRECTORY));

      // update tracking info. if a file is ready, process it
      updateTracking ();
    }
    catch (final OpenAS2Exception oae)
    {
      oae.terminate ();
      forceStop (oae);
    }
    catch (final Exception e)
    {
      new WrappedException (e).terminate ();
      forceStop (e);
    }
  }

  protected void scanDirectory (final String directory) throws InvalidParameterException
  {
    final File dir = IOUtil.getDirectoryFile (directory);

    // get a list of entries in the directory
    final File [] files = dir.listFiles ();
    if (files == null)
    {
      throw new InvalidParameterException ("Error getting list of files in directory",
                                           this,
                                           PARAM_OUTBOX_DIRECTORY,
                                           dir.getAbsolutePath ());
    }

    // iterator through each entry, and start tracking new files
    if (files.length > 0)
    {
      for (final File currentFile : files)
      {
        if (checkFile (currentFile))
        {
          // start watching the file's size if it's not already being watched
          trackFile (currentFile);
        }
      }
    }
  }

  protected boolean checkFile (final File file)
  {
    if (file.exists () && file.isFile ())
    {
      try
      {
        // check for a write-lock on file, will skip file if it's write locked
        final FileOutputStream fOut = new FileOutputStream (file, true);
        fOut.close ();
        return true;
      }
      catch (final IOException ioe)
      {
        // a sharing violation occurred, ignore the file for now
      }
    }
    return false;
  }

  protected void trackFile (final File file)
  {
    final Map <String, Long> trackedFiles = getTrackedFiles ();
    final String filePath = file.getAbsolutePath ();
    if (trackedFiles.get (filePath) == null)
    {
      trackedFiles.put (filePath, new Long (file.length ()));
    }
  }

  protected void updateTracking () throws OpenAS2Exception
  {
    // clone the trackedFiles map, iterator through the clone and modify the
    // original to avoid iterator exceptions
    // is there a better way to do this?
    final Map <String, Long> trackedFiles = getTrackedFiles ();
    final Map <String, Long> trackedFilesClone = new HashMap <String, Long> (trackedFiles);

    for (final Entry <String, Long> entry : trackedFilesClone.entrySet ())
    {
      // get the file and it's stored length
      final Map.Entry <String, Long> fileEntry = entry;
      final File file = new File (fileEntry.getKey ());
      final long fileLength = fileEntry.getValue ().longValue ();

      // if the file no longer exists, remove it from the tracker
      if (!checkFile (file))
      {
        trackedFiles.remove (fileEntry.getKey ());
      }
      else
      {
        // if the file length has changed, update the tracker
        final long newLength = file.length ();
        if (newLength != fileLength)
        {
          trackedFiles.put (fileEntry.getKey (), new Long (newLength));
        }
        else
        {
          // if the file length has stayed the same, process the file and stop
          // tracking it
          try
          {
            processFile (file);
          }
          finally
          {
            trackedFiles.remove (fileEntry.getKey ());
          }
        }
      }
    }
  }

  protected void processFile (final File file) throws OpenAS2Exception
  {

    s_aLogger.info ("processing " + file.getAbsolutePath ());

    final IMessage msg = createMessage ();
    msg.setAttribute (CFileAttribute.MA_FILEPATH, file.getAbsolutePath ());
    msg.setAttribute (CFileAttribute.MA_FILENAME, file.getName ());

    /*
     * asynch mdn logic 2007-03-12 save the file name into message object, it
     * will be stored into pending information file
     */
    msg.setAttribute (CFileAttribute.MA_PENDINGFILE, file.getName ());

    try
    {
      updateMessage (msg, file);
      s_aLogger.info ("file assigned to message " + file.getAbsolutePath () + msg.getLoggingText ());

      if (msg.getData () == null)
      {
        throw new InvalidMessageException ("No Data");
      }

      // Transmit the message
      getSession ().getProcessor ().handle (IProcessorSenderModule.DO_SEND, msg, null);

      /*
       * asynch mdn logic 2007-03-12 If the return status is pending in msg's
       * attribute "status" then copy the transmitted file to pending folder and
       * wait for the receiver to make another HTTP call to post AsyncMDN
       */
      if (msg.getAttribute (CFileAttribute.MA_STATUS) != null &&
          msg.getAttribute (CFileAttribute.MA_STATUS).equals (CFileAttribute.MA_PENDING))
      {
        final File pendingFile = new File (msg.getPartnership ().getAttribute (CFileAttribute.MA_PENDING),
                                           msg.getAttribute (CFileAttribute.MA_PENDINGFILE));
        final FileIOError aIOErr = FileOperations.copyFile (file, pendingFile);
        if (aIOErr.isFailure ())
          throw new OpenAS2Exception ("File was successfully sent but not copied to pending folder: " +
                                      pendingFile +
                                      " - " +
                                      aIOErr.toString ());

        s_aLogger.info ("copied " +
                        file.getAbsolutePath () +
                        " to pending folder : " +
                        pendingFile.getAbsolutePath () +
                        msg.getLoggingText ());
      }

      // If the Sent Directory option is set, move the transmitted file to
      // the sent directory

      if (getParameterNotRequired (PARAM_SENT_DIRECTORY) != null)
      {
        File sentFile = null;

        try
        {
          sentFile = new File (IOUtil.getDirectoryFile (getParameterRequired (PARAM_SENT_DIRECTORY)), file.getName ());
          sentFile = IOUtil.moveFile (file, sentFile, false, true);

          s_aLogger.info ("moved " +
                          file.getAbsolutePath () +
                          " to " +
                          sentFile.getAbsolutePath () +
                          msg.getLoggingText ());

        }
        catch (final IOException iose)
        {
          final OpenAS2Exception se = new OpenAS2Exception ("File was successfully sent but not moved to sent folder: " +
                                                            sentFile);
          se.initCause (iose);
        }
      }
      else
        if (!file.delete ())
        { // Delete the file if a sent directory isn't set
          throw new OpenAS2Exception ("File was successfully sent but not deleted: " + file);
        }

      s_aLogger.info ("deleted " + file.getAbsolutePath () + msg.getLoggingText ());

    }
    catch (final OpenAS2Exception oae)
    {
      s_aLogger.info (oae.getLocalizedMessage () + msg.getLoggingText ());
      oae.addSource (OpenAS2Exception.SOURCE_MESSAGE, msg);
      oae.addSource (OpenAS2Exception.SOURCE_FILE, file);
      oae.terminate ();
      IOUtil.handleError (file, getParameterRequired (PARAM_ERROR_DIRECTORY));
    }

  }

  protected abstract IMessage createMessage ();

  public void updateMessage (final IMessage msg, final File file) throws OpenAS2Exception
  {
    final MessageParameters params = new MessageParameters (msg);

    final String defaults = getParameterNotRequired (PARAM_DEFAULTS);
    if (defaults != null)
      params.setParameters (defaults);

    final String filename = file.getName ();
    final String format = getParameterNotRequired (PARAM_FORMAT);
    if (format != null)
    {
      final String delimiters = getParameter (PARAM_DELIMITERS, ".-");
      params.setParameters (format, delimiters, filename);
    }

    try
    {
      final byte [] data = SimpleFileIO.readFileBytes (file);
      String contentType = getParameterNotRequired (PARAM_MIMETYPE);
      if (contentType == null)
      {
        contentType = "application/octet-stream";
      }
      else
      {
        try
        {
          contentType = AbstractParameterParser.parse (contentType, params);
        }
        catch (final InvalidParameterException e)
        {
          s_aLogger.error ("Bad content-type" + contentType + msg.getLoggingText ());
          contentType = "application/octet-stream";
        }
      }
      final ByteArrayDataSource byteSource = new ByteArrayDataSource (data, contentType, null);
      final MimeBodyPart body = new MimeBodyPart ();
      body.setDataHandler (new DataHandler (byteSource));
      final String encodeType = msg.getPartnership ().getAttribute (Partnership.PA_CONTENT_TRANSFER_ENCODING);
      if (encodeType != null)
        body.setHeader ("Content-Transfer-Encoding", encodeType);
      else
        body.setHeader ("Content-Transfer-Encoding", "8bit"); // default is 8bit

      // below statement is not filename related, just want to make it
      // consist with the parameter "mimetype="application/EDI-X12""
      // defined in config.xml 2007-06-01

      body.setHeader ("Content-Type", contentType);

      // add below statement will tell the receiver to save the filename
      // as the one sent by sender. 2007-06-01
      final String sendFileName = getParameterNotRequired ("sendfilename");
      if (sendFileName != null && sendFileName.equals ("true"))
      {
        body.setHeader ("Content-Disposition", "Attachment; filename=\"" +
                                               msg.getAttribute (CFileAttribute.MA_FILENAME) +
                                               "\"");
        msg.setContentDisposition ("Attachment; filename=\"" + msg.getAttribute (CFileAttribute.MA_FILENAME) + "\"");
      }

      msg.setData (body);
    }
    catch (final MessagingException me)
    {
      throw new WrappedException (me);
    }

    // update the message's partnership with any stored information
    getSession ().getPartnershipFactory ().updatePartnership (msg, true);
    msg.updateMessageID ();
  }

  public Map <String, Long> getTrackedFiles ()
  {
    if (m_aTrackedFiles == null)
    {
      m_aTrackedFiles = new HashMap <String, Long> ();
    }
    return m_aTrackedFiles;
  }
}
