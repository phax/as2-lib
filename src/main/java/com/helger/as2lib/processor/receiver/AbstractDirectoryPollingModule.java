/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2015 Philip Helger philip[at]helger[dot]com
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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.message.CFileAttribute;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.params.AbstractParameterParser;
import com.helger.as2lib.params.InvalidParameterException;
import com.helger.as2lib.params.MessageParameters;
import com.helger.as2lib.partner.CPartnershipIDs;
import com.helger.as2lib.processor.sender.IProcessorSenderModule;
import com.helger.as2lib.session.IAS2Session;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.IOUtil;
import com.helger.as2lib.util.IStringMap;
import com.helger.as2lib.util.javamail.ByteArrayDataSource;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.io.file.FileIOError;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.commons.mime.CMimeType;

public abstract class AbstractDirectoryPollingModule extends AbstractPollingModule
{
  public static final String DEFAULT_CONTENT_TRANSFER_ENCODING = "8bit";
  public static final String ATTR_OUTBOX_DIRECTORY = "outboxdir";
  public static final String ATTR_ERROR_DIRECTORY = "errordir";
  public static final String ATTR_SENT_DIRECTORY = "sentdir";
  public static final String ATTR_FORMAT = "format";
  public static final String ATTR_DELIMITERS = "delimiters";
  public static final String ATTR_DEFAULTS = "defaults";
  public static final String ATTR_MIMETYPE = "mimetype";
  public static final String ATTR_SENDFILENAME = "sendfilename";

  private static final Logger s_aLogger = LoggerFactory.getLogger (AbstractDirectoryPollingModule.class);

  private Map <String, Long> m_aTrackedFiles;

  @Override
  public void initDynamicComponent (@Nonnull final IAS2Session aSession,
                                    @Nullable final IStringMap aOptions) throws OpenAS2Exception
  {
    super.initDynamicComponent (aSession, aOptions);
    getAttributeAsStringRequired (ATTR_OUTBOX_DIRECTORY);
    getAttributeAsStringRequired (ATTR_ERROR_DIRECTORY);
  }

  @Override
  public void poll ()
  {
    try
    {
      // scan the directory for new files
      scanDirectory (getAttributeAsStringRequired (ATTR_OUTBOX_DIRECTORY));

      // update tracking info. if a file is ready, process it
      updateTracking ();
    }
    catch (final Exception ex)
    {
      WrappedOpenAS2Exception.wrap (ex).terminate ();
      forceStop (ex);
    }
  }

  protected void scanDirectory (final String sDirectory) throws InvalidParameterException
  {
    final File aDir = IOUtil.getDirectoryFile (sDirectory);

    // get a list of entries in the directory
    final File [] aFiles = aDir.listFiles ();
    if (aFiles == null)
    {
      throw new InvalidParameterException ("Error getting list of files in directory",
                                           this,
                                           ATTR_OUTBOX_DIRECTORY,
                                           aDir.getAbsolutePath ());
    }

    // iterator through each entry, and start tracking new files
    if (aFiles.length > 0)
      for (final File aCurrentFile : aFiles)
        if (checkFile (aCurrentFile))
        {
          // start watching the file's size if it's not already being watched
          trackFile (aCurrentFile);
        }
  }

  protected boolean checkFile (@Nonnull final File aFile)
  {
    if (aFile.exists () && aFile.isFile ())
    {
      try
      {
        // check for a write-lock on file, will skip file if it's write locked
        final FileOutputStream aFOS = new FileOutputStream (aFile, true);
        aFOS.close ();
        return true;
      }
      catch (final IOException ioe)
      {
        // a sharing violation occurred, ignore the file for now
      }
    }
    return false;
  }

  protected void trackFile (@Nonnull final File aFile)
  {
    final Map <String, Long> aTrackedFiles = getTrackedFiles ();
    final String sFilePath = aFile.getAbsolutePath ();
    if (!aTrackedFiles.containsKey (sFilePath))
      aTrackedFiles.put (sFilePath, Long.valueOf (aFile.length ()));
  }

  protected void updateTracking () throws OpenAS2Exception
  {
    // clone the trackedFiles map, iterator through the clone and modify the
    // original to avoid iterator exceptions
    // is there a better way to do this?
    final Map <String, Long> aTrackedFiles = getTrackedFiles ();

    // We need to operate on a copy
    for (final Entry <String, Long> aFileEntry : CollectionHelper.newMap (aTrackedFiles).entrySet ())
    {
      // get the file and it's stored length
      final File aFile = new File (aFileEntry.getKey ());
      final long nFileLength = aFileEntry.getValue ().longValue ();

      // if the file no longer exists, remove it from the tracker
      if (!checkFile (aFile))
      {
        aTrackedFiles.remove (aFileEntry.getKey ());
      }
      else
      {
        // if the file length has changed, update the tracker
        final long nNewLength = aFile.length ();
        if (nNewLength != nFileLength)
        {
          aTrackedFiles.put (aFileEntry.getKey (), Long.valueOf (nNewLength));
        }
        else
        {
          // if the file length has stayed the same, process the file and stop
          // tracking it
          try
          {
            processFile (aFile);
          }
          finally
          {
            aTrackedFiles.remove (aFileEntry.getKey ());
          }
        }
      }
    }
  }

  protected void processFile (@Nonnull final File aFile) throws OpenAS2Exception
  {
    s_aLogger.info ("processing " + aFile.getAbsolutePath ());

    final IMessage aMsg = createMessage ();
    aMsg.setAttribute (CFileAttribute.MA_FILEPATH, aFile.getAbsolutePath ());
    aMsg.setAttribute (CFileAttribute.MA_FILENAME, aFile.getName ());

    /*
     * asynch mdn logic 2007-03-12 save the file name into message object, it
     * will be stored into pending information file
     */
    aMsg.setAttribute (CFileAttribute.MA_PENDINGFILE, aFile.getName ());

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("AS2Message was created");

    try
    {
      updateMessage (aMsg, aFile);
      s_aLogger.info ("file assigned to message " + aFile.getAbsolutePath () + aMsg.getLoggingText ());

      if (aMsg.getData () == null)
        throw new InvalidMessageException ("No Data");

      // Transmit the message
      getSession ().getMessageProcessor ().handle (IProcessorSenderModule.DO_SEND, aMsg, null);

      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("AS2Message was successfully handled my the MessageProcessor");

      /*
       * asynch mdn logic 2007-03-12 If the return status is pending in msg's
       * attribute "status" then copy the transmitted file to pending folder and
       * wait for the receiver to make another HTTP call to post AsyncMDN
       */
      if (CFileAttribute.MA_PENDING.equals (aMsg.getAttribute (CFileAttribute.MA_STATUS)))
      {
        final File aPendingFile = new File (aMsg.getPartnership ().getAttribute (CFileAttribute.MA_PENDING),
                                            aMsg.getAttribute (CFileAttribute.MA_PENDINGFILE));
        final FileIOError aIOErr = IOUtil.getFileOperationManager ().copyFile (aFile, aPendingFile);
        if (aIOErr.isFailure ())
          throw new OpenAS2Exception ("File was successfully sent but not copied to pending folder: " +
                                      aPendingFile +
                                      " - " +
                                      aIOErr.toString ());

        s_aLogger.info ("copied " +
                        aFile.getAbsolutePath () +
                        " to pending folder : " +
                        aPendingFile.getAbsolutePath () +
                        aMsg.getLoggingText ());
      }

      // If the Sent Directory option is set, move the transmitted file to
      // the sent directory
      if (containsAttribute (ATTR_SENT_DIRECTORY))
      {
        File aSentFile = null;
        try
        {
          aSentFile = new File (IOUtil.getDirectoryFile (getAttributeAsStringRequired (ATTR_SENT_DIRECTORY)),
                                aFile.getName ());
          aSentFile = IOUtil.moveFile (aFile, aSentFile, false, true);

          s_aLogger.info ("moved " +
                          aFile.getAbsolutePath () +
                          " to " +
                          aSentFile.getAbsolutePath () +
                          aMsg.getLoggingText ());

        }
        catch (final IOException ex)
        {
          final OpenAS2Exception se = new OpenAS2Exception ("File was successfully sent but not moved to sent folder: " +
                                                            aSentFile);
          se.initCause (ex);
        }
      }
      else
      {
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Trying to delete file " + aFile.getAbsolutePath ());

        if (!aFile.delete ())
        {
          // Delete the file if a sent directory isn't set
          throw new OpenAS2Exception ("File was successfully sent but not deleted: " + aFile);
        }
      }

      s_aLogger.info ("deleted " + aFile.getAbsolutePath () + aMsg.getLoggingText ());

    }
    catch (final OpenAS2Exception ex)
    {
      s_aLogger.info (ex.getLocalizedMessage () + aMsg.getLoggingText ());
      ex.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      ex.addSource (OpenAS2Exception.SOURCE_FILE, aFile);
      ex.terminate ();
      IOUtil.handleError (aFile, getAttributeAsStringRequired (ATTR_ERROR_DIRECTORY));
    }
  }

  @Nonnull
  protected abstract IMessage createMessage ();

  public void updateMessage (@Nonnull final IMessage aMsg, @Nonnull final File aFile) throws OpenAS2Exception
  {
    final MessageParameters aParams = new MessageParameters (aMsg);

    final String sDefaults = getAttributeAsString (ATTR_DEFAULTS);
    if (sDefaults != null)
      aParams.setParameters (sDefaults);

    final String sFilename = aFile.getName ();
    final String sFormat = getAttributeAsString (ATTR_FORMAT);
    if (sFormat != null)
    {
      final String sDelimiters = getAttributeAsString (ATTR_DELIMITERS, ".-");
      aParams.setParameters (sFormat, sDelimiters, sFilename);
    }

    try
    {
      final byte [] aData = SimpleFileIO.getAllFileBytes (aFile);
      String sContentType = getAttributeAsString (ATTR_MIMETYPE);
      if (sContentType == null)
      {
        sContentType = CMimeType.APPLICATION_OCTET_STREAM.getAsString ();
      }
      else
      {
        try
        {
          sContentType = AbstractParameterParser.parse (sContentType, aParams);
        }
        catch (final InvalidParameterException ex)
        {
          s_aLogger.error ("Bad content-type" + sContentType + aMsg.getLoggingText ());
          sContentType = CMimeType.APPLICATION_OCTET_STREAM.getAsString ();
        }
      }
      final ByteArrayDataSource aByteSource = new ByteArrayDataSource (aData, sContentType, null);
      final MimeBodyPart aBody = new MimeBodyPart ();
      aBody.setDataHandler (new DataHandler (aByteSource));
      final String sEncodeType = aMsg.getPartnership ().getAttribute (CPartnershipIDs.PA_CONTENT_TRANSFER_ENCODING);
      if (sEncodeType != null)
        aBody.setHeader (CAS2Header.HEADER_CONTENT_TRANSFER_ENCODING, sEncodeType);
      else
      {
        // default is 8bit
        aBody.setHeader (CAS2Header.HEADER_CONTENT_TRANSFER_ENCODING, DEFAULT_CONTENT_TRANSFER_ENCODING);
      }

      // below statement is not filename related, just want to make it
      // consist with the parameter "mimetype="application/EDI-X12""
      // defined in config.xml 2007-06-01
      aBody.setHeader (CAS2Header.HEADER_CONTENT_TYPE, sContentType);

      // add below statement will tell the receiver to save the filename
      // as the one sent by sender. 2007-06-01
      final String sSendFilename = getAttributeAsString (ATTR_SENDFILENAME);
      if ("true".equals (sSendFilename))
      {
        final String sMAFilename = aMsg.getAttribute (CFileAttribute.MA_FILENAME);
        final String sContentDisposition = "Attachment; filename=\"" + sMAFilename + "\"";
        aBody.setHeader (CAS2Header.HEADER_CONTENT_DISPOSITION, sContentDisposition);
        aMsg.setContentDisposition (sContentDisposition);
      }

      aMsg.setData (aBody);
    }
    catch (final MessagingException ex)
    {
      throw WrappedOpenAS2Exception.wrap (ex);
    }

    // update the message's partnership with any stored information
    getSession ().getPartnershipFactory ().updatePartnership (aMsg, true);
    aMsg.updateMessageID ();
  }

  @Nonnull
  @ReturnsMutableObject ("speed")
  public Map <String, Long> getTrackedFiles ()
  {
    if (m_aTrackedFiles == null)
      m_aTrackedFiles = new HashMap <String, Long> ();
    return m_aTrackedFiles;
  }
}
