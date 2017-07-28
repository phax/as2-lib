/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2017 Philip Helger philip[at]helger[dot]com
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

import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.session.IAS2Session;
import com.helger.as2lib.util.IStringMap;

public abstract class AbstractActivePollingModule extends AbstractActiveReceiverModule
{
  public static final String ATTR_POLLING_INTERVAL = "interval";
  private static final Logger s_aLogger = LoggerFactory.getLogger (PollTask.class);

  private Timer m_aTimer;
  private boolean m_bBusy;

  @Override
  @OverridingMethodsMustInvokeSuper
  public void initDynamicComponent (@Nonnull final IAS2Session aSession,
                                    @Nullable final IStringMap aOptions) throws OpenAS2Exception
  {
    super.initDynamicComponent (aSession, aOptions);
    getAttributeAsStringRequired (ATTR_POLLING_INTERVAL);
  }

  public void setInterval (final int nSeconds)
  {
    putIn (ATTR_POLLING_INTERVAL, nSeconds);
  }

  public int getInterval ()
  {
    return getAsInt (ATTR_POLLING_INTERVAL, 0);
  }

  public abstract void poll ();

  @Override
  public void doStart () throws OpenAS2Exception
  {
    m_aTimer = new Timer (true);
    m_aTimer.scheduleAtFixedRate (new PollTask (), 0, getInterval () * 1000);
  }

  @Override
  public void doStop () throws OpenAS2Exception
  {
    if (m_aTimer != null)
    {
      m_aTimer.cancel ();
      m_aTimer = null;
    }
  }

  private class PollTask extends TimerTask
  {
    @Override
    public void run ()
    {
      if (!isBusy ())
      {
        setBusy (true);
        poll ();
        setBusy (false);
      }
      else
      {
        s_aLogger.info ("Miss tick");
      }
    }
  }

  public boolean isBusy ()
  {
    return m_bBusy;
  }

  public void setBusy (final boolean bBusy)
  {
    m_bBusy = bBusy;
  }
}
