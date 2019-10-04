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
package com.helger.as2.cmdprocessor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import com.helger.as2.cmd.CommandResult;
import com.helger.as2.cmd.ICommand;
import com.helger.as2.util.CommandTokenizer;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.session.IAS2Session;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.collection.attr.IStringMap;
import com.helger.commons.collection.attr.StringMap;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsLinkedHashSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsOrderedSet;
import com.helger.commons.io.stream.NonBlockingBufferedReader;
import com.helger.commons.io.stream.NonBlockingBufferedWriter;
import com.helger.commons.string.StringHelper;

/**
 * actual socket command processor takes commands from socket/port and passes
 * them to the OpenAS2Server message format &lt;command userid="abc"
 * pasword="xyz"&gt;the actual command&lt;/command&gt; when inited the valid
 * userid and password is passed, then as each command is processed the
 * processCommand method verifies the two fields correctness
 *
 * @author joseph mcverry
 */
public class SocketCommandProcessor extends AbstractCommandProcessor
{
  public static final String ATTR_PORTID = "portid";
  public static final String ATTR_USERID = "userid";
  public static final String ATTR_PASSWORD = "password";

  private NonBlockingBufferedReader m_aReader;
  private NonBlockingBufferedWriter m_aWriter;
  private SSLServerSocket m_aSSLServerSocket;

  private String m_sUserID;
  private String m_sPassword;
  private SocketCommandParser m_aParser;

  public SocketCommandProcessor ()
  {}

  @OverrideOnDemand
  @Nullable
  protected String [] getEnabledAnonymousCipherSuites (@Nonnull final String [] aEnabled,
                                                       @Nonnull final String [] aSupported)
  {
    final ICommonsOrderedSet <String> ret = new CommonsLinkedHashSet <> ();
    for (final String sSupported : aSupported)
    {
      // No SSL - usually TLS
      // Only anonymous
      // No DES
      // No MD5
      if (!sSupported.startsWith ("SSL") &&
          sSupported.indexOf ("_anon_") >= 0 &&
          (sSupported.indexOf ("_AES_") >= 0 || sSupported.indexOf ("_3DES_") >= 0) &&
          sSupported.indexOf ("_SHA") >= 0)
      {
        ret.add (sSupported);
      }
    }
    ret.addAll (aEnabled);
    return ret.toArray (new String [0]);
  }

  @Override
  public void initDynamicComponent (@Nonnull final IAS2Session aSession,
                                    @Nullable final IStringMap aParams) throws OpenAS2Exception
  {
    final StringMap aParameters = aParams == null ? new StringMap () : new StringMap (aParams);
    final String sPort = aParameters.getAsString (ATTR_PORTID);
    try
    {
      final int nPort = Integer.parseInt (sPort);

      final SSLServerSocketFactory aSSLServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault ();
      m_aSSLServerSocket = (SSLServerSocket) aSSLServerSocketFactory.createServerSocket (nPort);
      final String [] aEnabledCipherSuites = getEnabledAnonymousCipherSuites (m_aSSLServerSocket.getEnabledCipherSuites (),
                                                                              m_aSSLServerSocket.getSupportedCipherSuites ());
      m_aSSLServerSocket.setEnabledCipherSuites (aEnabledCipherSuites);
    }
    catch (final IOException e)
    {
      throw new OpenAS2Exception (e);
    }
    catch (final NumberFormatException e)
    {
      throw new OpenAS2Exception ("Error converting portid parameter '" + sPort + "': " + e);
    }

    m_sUserID = aParameters.getAsString (ATTR_USERID);
    if (StringHelper.hasNoText (m_sUserID))
      throw new OpenAS2Exception ("missing 'userid' parameter");

    m_sPassword = aParameters.getAsString (ATTR_PASSWORD);
    if (StringHelper.hasNoText (m_sPassword))
      throw new OpenAS2Exception ("missing 'password' parameter");

    try
    {
      m_aParser = new SocketCommandParser ();
    }
    catch (final Exception e)
    {
      throw new OpenAS2Exception (e);
    }
  }

  @Override
  public void processCommand () throws OpenAS2Exception
  {
    try (final SSLSocket socket = (SSLSocket) m_aSSLServerSocket.accept ())
    {
      socket.setSoTimeout (2000);
      m_aReader = new NonBlockingBufferedReader (new InputStreamReader (socket.getInputStream ()));
      m_aWriter = new NonBlockingBufferedWriter (new OutputStreamWriter (socket.getOutputStream ()));

      final String line = m_aReader.readLine ();

      m_aParser.parse (line);

      if (!m_aParser.getUserid ().equals (m_sUserID))
      {
        m_aWriter.write ("Bad userid/password");
        throw new OpenAS2Exception ("Bad userid");
      }

      if (!m_aParser.getPassword ().equals (m_sPassword))
      {
        m_aWriter.write ("Bad userid/password");
        throw new OpenAS2Exception ("Bad password");
      }

      final String str = m_aParser.getCommandText ();
      if (str != null && str.length () > 0)
      {
        final CommandTokenizer cmdTkn = new CommandTokenizer (str);

        if (cmdTkn.hasMoreTokens ())
        {
          final String sCommandName = cmdTkn.nextToken ().toLowerCase ();

          if (sCommandName.equals (StreamCommandProcessor.EXIT_COMMAND))
          {
            terminate ();
          }
          else
          {
            final ICommonsList <String> params = new CommonsArrayList <> ();

            while (cmdTkn.hasMoreTokens ())
            {
              params.add (cmdTkn.nextToken ());
            }

            final ICommand aCommand = getCommand (sCommandName);
            if (aCommand != null)
            {
              final CommandResult aResult = aCommand.execute (params.toArray ());

              if (aResult.getType ().isSuccess ())
              {
                m_aWriter.write (aResult.getAsXMLString ());
              }
              else
              {
                m_aWriter.write ("\r\n" + StreamCommandProcessor.COMMAND_ERROR + "\r\n");
                m_aWriter.write (aResult.getResultAsString ());
              }
            }
            else
            {
              m_aWriter.write (StreamCommandProcessor.COMMAND_NOT_FOUND + "> " + sCommandName + "\r\n");
              m_aWriter.write ("List of commands:" + "\r\n");
              for (final String sCurCmd : getAllCommands ().keySet ())
                m_aWriter.write (sCurCmd + "\r\n");
            }
          }
        }

      }
      m_aWriter.flush ();
    }
    catch (final IOException ex)
    {
      throw WrappedOpenAS2Exception.wrap (ex);
    }
  }

  @Override
  public void run ()
  {
    try
    {
      while (true)
      {
        processCommand ();
      }
    }
    catch (final OpenAS2Exception e)
    {
      e.printStackTrace ();
    }
  }
}
