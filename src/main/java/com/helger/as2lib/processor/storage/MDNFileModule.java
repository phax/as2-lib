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
package com.helger.as2lib.processor.storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedException;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.params.AbstractParameterParser;
import com.helger.as2lib.params.CompositeParameters;
import com.helger.as2lib.params.DateParameters;
import com.helger.as2lib.params.InvalidParameterException;
import com.helger.as2lib.params.MessageMDNParameters;

public class MDNFileModule extends AbstractStorageModule
{

  public void handle (final String action, final IMessage msg, final Map <String, Object> options) throws OpenAS2Exception
  {
    // store mdn data
    if (msg.getMDN () == null)
    {
      throw new OpenAS2Exception ("Message has no MDN");
    }

    try
    {
      final File mdnFile = getFile (msg, getParameterRequired (PARAM_FILENAME), "");
      final InputStream in = getMDNStream (msg.getMDN ());
      store (mdnFile, in);
    }
    catch (final IOException ioe)
    {
      throw new WrappedException (ioe);
    }
  }

  @Override
  protected String getModuleAction ()
  {
    return DO_STOREMDN;
  }

  @Override
  protected String getFilename (final IMessage msg, final String fileParam, final String action) throws InvalidParameterException
  {
    final IMessageMDN mdn = msg.getMDN ();
    final CompositeParameters compParams = new CompositeParameters (false).add ("date", new DateParameters ())
                                                                          .add ("mdn", new MessageMDNParameters (mdn));

    return AbstractParameterParser.parse (fileParam, compParams);
  }

  protected InputStream getMDNStream (final IMessageMDN mdn)
  {
    final StringBuilder mdnBuf = new StringBuilder ();

    // write headers to the string buffer
    mdnBuf.append ("Headers:\r\n");

    final Enumeration <?> headers = mdn.getHeaders ().getAllHeaderLines ();
    String header;

    while (headers.hasMoreElements ())
    {
      header = (String) headers.nextElement ();
      mdnBuf.append (header).append ("\r\n");
    }

    mdnBuf.append ("\r\n");

    // write attributes to the string buffer
    mdnBuf.append ("Attributes:\r\n");

    final Iterator <Map.Entry <String, String>> attrIt = mdn.getAttributes ().entrySet ().iterator ();
    Map.Entry <String, String> attrEntry;

    while (attrIt.hasNext ())
    {
      attrEntry = attrIt.next ();
      mdnBuf.append (attrEntry.getKey ()).append (": ");
      mdnBuf.append (attrEntry.getValue ()).append ("\r\n");
    }
    // finaly, write the MDN text
    mdnBuf.append ("Text:\r\n");
    mdnBuf.append (mdn.getText ());

    return new ByteArrayInputStream (mdnBuf.toString ().getBytes ());
  }
}
