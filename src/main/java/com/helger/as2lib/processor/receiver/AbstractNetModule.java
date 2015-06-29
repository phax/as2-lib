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
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.message.CNetAttribute;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.params.CompositeParameters;
import com.helger.as2lib.params.DateParameters;
import com.helger.as2lib.params.MessageParameters;
import com.helger.as2lib.partner.CPartnershipIDs;
import com.helger.as2lib.processor.receiver.net.INetModuleHandler;
import com.helger.as2lib.session.IAS2Session;
import com.helger.as2lib.util.IOUtil;
import com.helger.as2lib.util.IStringMap;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.lang.ClassHelper;

public abstract class AbstractNetModule extends AbstractReceiverModule
{
  public static final String ATTR_ADDRESS = "address";
  public static final String ATTR_PORT = "port";
  public static final String ATTR_ERROR_DIRECTORY = "errordir";
  public static final String ATTR_ERROR_FORMAT = "errorformat";
  public static final String DEFAULT_ERROR_FORMAT = "$date.yyyyMMddhhmmss$";

  // Macros for responses
  public static final String MSG_SENDER = "$" + MessageParameters.KEY_SENDER + "." + CPartnershipIDs.PID_AS2 + "$";
  public static final String MSG_RECEIVER = "$" + MessageParameters.KEY_RECEIVER + "." + CPartnershipIDs.PID_AS2 + "$";
  public static final String MSG_DATE = "$" + MessageParameters.KEY_HEADERS + ".date" + "$";
  public static final String MSG_SUBJECT = "$" + MessageParameters.KEY_HEADERS + ".subject" + "$";
  public static final String MSG_SOURCE_ADDRESS = "$" +
                                                  MessageParameters.KEY_ATTRIBUTES +
                                                  "." +
                                                  CNetAttribute.MA_SOURCE_IP +
                                                  "$";
  public static final String DP_HEADER = "The message sent to Recipient " +
                                         MSG_RECEIVER +
                                         " on " +
                                         MSG_DATE +
                                         " with Subject " +
                                         MSG_SUBJECT +
                                         " has been received, ";
  public static final String DP_DECRYPTED = DP_HEADER +
                                            "the EDI Interchange was successfully decrypted and it's integrity was verified. ";
  public static final String DP_VERIFIED = DP_DECRYPTED +
                                           "In addition, the sender of the message, Sender " +
                                           MSG_SENDER +
                                           " at Location " +
                                           MSG_SOURCE_ADDRESS +
                                           " was authenticated as the originator of the message. ";

  // Response texts
  public static final String DISP_PARTNERSHIP_NOT_FOUND = DP_HEADER +
                                                          "but the Sender " +
                                                          MSG_SENDER +
                                                          " and/or Recipient " +
                                                          MSG_RECEIVER +
                                                          " are unknown.";
  public static final String DISP_PARSING_MIME_FAILED = DP_HEADER +
                                                        "but an error occured while parsing the MIME content.";
  public static final String DISP_DECRYPTION_ERROR = DP_HEADER + "but an error occured decrypting the content.";
  public static final String DISP_VERIFY_SIGNATURE_FAILED = DP_DECRYPTED +
                                                            "Authentication of the originator of the message failed.";
  public static final String DISP_VALIDATION_FAILED = DP_VERIFIED +
                                                      " An error occured while validating the received data.";
  public static final String DISP_STORAGE_FAILED = DP_VERIFIED +
                                                   " An error occured while storing the data to the file system.";
  public static final String DISP_SUCCESS = DP_VERIFIED +
                                            "There is no guarantee however that the EDI Interchange was syntactically correct, or was received by the EDI application/translator.";

  private MainThread m_aMainThread;

  public AbstractNetModule ()
  {}

  @Override
  public void doStart () throws OpenAS2Exception
  {
    try
    {
      final String sAddress = getAttributeAsString (ATTR_ADDRESS);
      final int nPort = getAttributeAsInt (ATTR_PORT, 0);
      m_aMainThread = new MainThread (this, sAddress, nPort);
      m_aMainThread.start ();
    }
    catch (final IOException ioe)
    {
      throw WrappedOpenAS2Exception.wrap (ioe);
    }
  }

  @Override
  public void doStop () throws OpenAS2Exception
  {
    if (m_aMainThread != null)
    {
      m_aMainThread.terminate ();
      m_aMainThread = null;
    }
  }

  @Override
  public void initDynamicComponent (@Nonnull final IAS2Session aSession, @Nullable final IStringMap aOptions) throws OpenAS2Exception
  {
    super.initDynamicComponent (aSession, aOptions);

    // Ensure port parameter is present
    getAttributeAsStringRequired (ATTR_PORT);
  }

  @Nonnull
  public abstract INetModuleHandler createHandler ();

  public void handleError (@Nonnull final IMessage aMsg, @Nonnull final OpenAS2Exception aSrcEx)
  {
    aSrcEx.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
    aSrcEx.terminate ();

    try
    {
      final CompositeParameters aParams = new CompositeParameters (false).add ("date", new DateParameters ())
                                                                         .add ("msg", new MessageParameters (aMsg));

      final String sErrorFilename = aParams.format (getAttributeAsString (ATTR_ERROR_FORMAT, DEFAULT_ERROR_FORMAT));
      final String sErrorDirectory = getAttributeAsStringRequired (ATTR_ERROR_DIRECTORY);
      final File aMsgErrorFile = IOUtil.getUniqueFile (IOUtil.getDirectoryFile (sErrorDirectory),
                                                       FilenameHelper.getAsSecureValidFilename (sErrorFilename));

      final OutputStream aFOS = FileHelper.getOutputStream (aMsgErrorFile);
      try
      {
        final String sMsgText = aMsg.getAsString ();
        aFOS.write (sMsgText.getBytes ());

        // Enable this line, to also store the body of the source message
        if (false)
          StreamHelper.copyInputStreamToOutputStream (aMsg.getData ().getInputStream (), aFOS);
      }
      finally
      {
        StreamHelper.close (aFOS);
      }

      // make sure an error of this event is logged
      final InvalidMessageException im = new InvalidMessageException ("Stored invalid message to " +
                                                                      aMsgErrorFile.getAbsolutePath ());
      im.terminate ();
    }
    catch (final Exception ex)
    {
      final OpenAS2Exception we = WrappedOpenAS2Exception.wrap (ex);
      we.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      we.terminate ();
    }
  }

  private static final class ConnectionThread extends Thread
  {
    private static final Logger s_aLogger = LoggerFactory.getLogger (ConnectionThread.class);

    private final AbstractNetModule m_aOwner;
    private final Socket m_aSocket;

    public ConnectionThread (@Nonnull final AbstractNetModule aOwner, @Nonnull final Socket aSocket)
    {
      super ("AS2ConnectionThread-" + ClassHelper.getClassLocalName (aOwner));
      m_aOwner = aOwner;
      m_aSocket = aSocket;
    }

    @Override
    public void run ()
    {
      s_aLogger.info ("AS2ConnectionThread: run");
      final Socket s = m_aSocket;

      m_aOwner.createHandler ().handle (m_aOwner, s);

      try
      {
        s.close ();
      }
      catch (final IOException ex)
      {
        WrappedOpenAS2Exception.wrap (ex).terminate ();
      }
      finally
      {
        s_aLogger.info ("AS2ConnectionThread: done running");
      }
    }
  }

  private static final class MainThread extends Thread
  {
    private static final Logger s_aLogger = LoggerFactory.getLogger (MainThread.class);

    private final AbstractNetModule m_aOwner;
    private final ServerSocket m_aSocket;
    private volatile boolean m_bTerminated;

    public MainThread (@Nonnull final AbstractNetModule aOwner,
                       @Nullable final String sAddress,
                       @Nonnegative final int nPort) throws IOException
    {
      super ("AS2MainThread-" + ClassHelper.getClassLocalName (aOwner));
      m_aOwner = aOwner;
      m_aSocket = new ServerSocket ();
      final InetSocketAddress aAddr = sAddress == null ? new InetSocketAddress (nPort)
                                                      : new InetSocketAddress (sAddress, nPort);
      m_aSocket.bind (aAddr);
      s_aLogger.info ("Inited " + getName () + " at " + aAddr);
    }

    @Override
    public void run ()
    {
      s_aLogger.info ("AS2MainThread: run");
      while (!m_bTerminated && !isInterrupted ())
      {
        try
        {
          final Socket aConn = m_aSocket.accept ();
          aConn.setSoLinger (true, 60);
          new ConnectionThread (m_aOwner, aConn).start ();
        }
        catch (final Exception ex)
        {
          if (!m_bTerminated)
            m_aOwner.forceStop (ex);
        }
      }

      s_aLogger.info ("AS2MainThread: done running");
    }

    public void terminate ()
    {
      if (!m_bTerminated)
      {
        m_bTerminated = true;
        if (m_aSocket != null)
          try
          {
            m_aSocket.close ();
          }
          catch (final IOException ex)
          {
            m_aOwner.forceStop (ex);
          }
      }
    }
  }
}
