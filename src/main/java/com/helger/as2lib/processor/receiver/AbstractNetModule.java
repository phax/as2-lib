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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.ISession;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedException;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.InvalidMessageException;
import com.helger.as2lib.params.CompositeParameters;
import com.helger.as2lib.params.DateParameters;
import com.helger.as2lib.params.MessageParameters;
import com.helger.as2lib.processor.receiver.net.INetModuleHandler;
import com.helger.as2lib.util.IOUtil;
import com.phloc.commons.io.file.FilenameHelper;

public abstract class AbstractNetModule extends AbstractReceiverModule
{
  public static final String PARAM_ADDRESS = "address";
  public static final String PARAM_PORT = "port";
  public static final String PARAM_ERROR_DIRECTORY = "errordir";
  public static final String PARAM_ERRORS = "errors";
  public static final String DEFAULT_ERRORS = "$date.yyyyMMddhhmmss$";

  private MainThread m_aMainThread;

  @Override
  public void doStart () throws OpenAS2Exception
  {
    try
    {
      m_aMainThread = new MainThread (this, getParameterNotRequired (PARAM_ADDRESS), getParameterInt (PARAM_PORT));
      m_aMainThread.start ();
    }
    catch (final IOException ioe)
    {
      throw new WrappedException (ioe);
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
  public void initDynamicComponent (final ISession aSession, final Map <String, String> aOptions) throws OpenAS2Exception
  {
    super.initDynamicComponent (aSession, aOptions);

    // Ensure port parameter is present
    getParameterRequired (PARAM_PORT);
  }

  protected abstract INetModuleHandler getHandler ();

  public void handleError (final IMessage aMsg, final OpenAS2Exception aSrcEx)
  {
    aSrcEx.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
    aSrcEx.terminate ();

    try
    {
      final CompositeParameters aParams = new CompositeParameters (false).add ("date", new DateParameters ())
                                                                         .add ("msg", new MessageParameters (aMsg));

      final String sName = aParams.format (getParameter (PARAM_ERRORS, DEFAULT_ERRORS));
      final String sDirectory = getParameterRequired (PARAM_ERROR_DIRECTORY);

      final File aMsgFile = IOUtil.getUniqueFile (IOUtil.getDirectoryFile (sDirectory),
                                                  FilenameHelper.getAsSecureValidFilename (sName));
      final String sMsgText = aMsg.toString ();
      final FileOutputStream aFOS = new FileOutputStream (aMsgFile);

      aFOS.write (sMsgText.getBytes ());
      aFOS.close ();

      // make sure an error of this event is logged
      final InvalidMessageException im = new InvalidMessageException ("Stored invalid message to " +
                                                                      aMsgFile.getAbsolutePath ());
      im.terminate ();
    }
    catch (final OpenAS2Exception ex)
    {
      ex.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      ex.terminate ();
    }
    catch (final IOException ex)
    {
      final WrappedException we = new WrappedException (ex);
      we.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      we.terminate ();
    }
  }

  protected static final class ConnectionThread extends Thread
  {
    private final AbstractNetModule m_aOwner;
    private final Socket m_aSocket;

    public ConnectionThread (final AbstractNetModule aOwner, final Socket aSocket)
    {
      super ();
      m_aOwner = aOwner;
      m_aSocket = aSocket;
      start ();
    }

    public AbstractNetModule getOwner ()
    {
      return m_aOwner;
    }

    public Socket getSocket ()
    {
      return m_aSocket;
    }

    @Override
    public void run ()
    {
      final Socket s = getSocket ();

      getOwner ().getHandler ().handle (getOwner (), s);

      try
      {
        s.close ();
      }
      catch (final IOException ex)
      {
        new WrappedException (ex).terminate ();
      }
    }
  }

  protected static final class MainThread extends Thread
  {
    private static final Logger s_aLogger = LoggerFactory.getLogger (MainThread.class);

    private final AbstractNetModule m_aOwner;
    private final ServerSocket m_aSocket;
    private boolean m_bTerminated;

    public MainThread (final AbstractNetModule aOwner, final String sAddress, final int nPort) throws IOException
    {
      super ();
      m_aOwner = aOwner;
      m_aSocket = new ServerSocket ();
      if (sAddress != null)
        m_aSocket.bind (new InetSocketAddress (sAddress, nPort));
      else
        m_aSocket.bind (new InetSocketAddress (nPort));
    }

    public AbstractNetModule getOwner ()
    {
      return m_aOwner;
    }

    public ServerSocket getSocket ()
    {
      return m_aSocket;
    }

    public void setTerminated (final boolean bTerminated)
    {
      m_bTerminated = bTerminated;
      if (m_aSocket != null)
      {
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

    public boolean isTerminated ()
    {
      return m_bTerminated;
    }

    @Override
    public void run ()
    {
      while (!isTerminated ())
      {
        try
        {
          final Socket aConn = m_aSocket.accept ();
          aConn.setSoLinger (true, 60);
          new ConnectionThread (getOwner (), aConn);
        }
        catch (final IOException ex)
        {
          if (!isTerminated ())
            m_aOwner.forceStop (ex);
        }
      }

      s_aLogger.info ("exited");
    }

    public void terminate ()
    {
      setTerminated (true);
    }
  }
}
