/*
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2025 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2.app.cert;

import java.io.File;
import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2.util.EFileMonitorEvent;
import com.helger.as2.util.FileMonitor;
import com.helger.as2.util.IFileMonitorListener;
import com.helger.as2lib.cert.CertificateFactory;
import com.helger.as2lib.exception.AS2Exception;
import com.helger.as2lib.params.AS2InvalidParameterException;

public class ServerCertificateFactory extends CertificateFactory implements IFileMonitorListener
{
  public static final String ATTR_INTERVAL = "interval";
  private static final Logger LOGGER = LoggerFactory.getLogger (ServerCertificateFactory.class);

  private FileMonitor m_aFileMonitor;

  @Override
  public void load (@Nonnull @WillClose final InputStream aIS, @Nonnull final char [] password) throws AS2Exception
  {
    super.load (aIS, password);
    getFileMonitor ();
  }

  public FileMonitor getFileMonitor () throws AS2InvalidParameterException
  {
    boolean bCreateMonitor = m_aFileMonitor == null && attrs ().getAsString (ATTR_INTERVAL) != null;
    if (!bCreateMonitor && m_aFileMonitor != null)
    {
      // Create a new monitor if the filename changed
      final String sFilename = m_aFileMonitor.getFilename ();
      bCreateMonitor = sFilename != null && !sFilename.equals (getFilename ());
    }

    if (bCreateMonitor)
    {
      if (m_aFileMonitor != null)
        m_aFileMonitor.stop ();

      final int nInterval = getAttributeAsIntRequired (ATTR_INTERVAL);
      final File file = new File (getFilename ());
      m_aFileMonitor = new FileMonitor (file, nInterval);
      m_aFileMonitor.addListener (this);
    }

    return m_aFileMonitor;
  }

  /**
   * Overwrite the File Monitor to be used.
   *
   * @param aFileMonitor
   *        The file monitor to use. May be <code>null</code>.
   */
  public void setFileMonitor (@Nullable final FileMonitor aFileMonitor)
  {
    m_aFileMonitor = aFileMonitor;
  }

  public void onFileMonitorEvent (final FileMonitor monitor, final File file, final EFileMonitorEvent eEvent)
  {
    switch (eEvent)
    {
      case EVENT_MODIFIED:
        try
        {
          load ();
          LOGGER.info ("- Certificates Reloaded -");
        }
        catch (final AS2Exception oae)
        {
          oae.terminate ();
        }
        break;
    }
  }

  @Override
  public boolean equals (final Object o)
  {
    // No relevant field added
    return super.equals (o);
  }

  @Override
  public int hashCode ()
  {
    // No relevant field added
    return super.hashCode ();
  }
}
