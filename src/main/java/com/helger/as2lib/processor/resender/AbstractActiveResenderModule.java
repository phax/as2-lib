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
package com.helger.as2lib.processor.resender;

import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.Nonnegative;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.params.InvalidParameterException;
import com.helger.as2lib.processor.module.AbstractActiveModule;
import com.helger.commons.CGlobal;

/**
 * Base class for an active resender module.
 *
 * @author Philip Helger
 */
public abstract class AbstractActiveResenderModule extends AbstractActiveModule implements IProcessorResenderModule
{
  // in seconds
  public static final String ATTR_RESEND_DELAY_SECONDS = "resenddelay";

  // TODO Resend set to 15 minutes. Implement a scaling resend time with
  // eventual permanent failure of transmission
  // 15 minutes
  public static final long DEFAULT_RESEND_DELAY_MS = 15 * CGlobal.MILLISECONDS_PER_MINUTE;

  private class PollTask extends TimerTask
  {
    @Override
    public void run ()
    {
      // Call resend of module class
      resend ();
    }
  }

  public static final long TICK_INTERVAL = 30 * CGlobal.MILLISECONDS_PER_SECOND;

  private Timer m_aTimer;

  /**
   * @return The defined delay until re-send in milliseconds.
   * @throws InvalidParameterException
   *         If an invalid value is configured.
   */
  @Nonnegative
  protected final long getResendDelayMS () throws InvalidParameterException
  {
    if (!containsAttribute (ATTR_RESEND_DELAY_SECONDS))
      return DEFAULT_RESEND_DELAY_MS;
    return getAttributeAsIntRequired (ATTR_RESEND_DELAY_SECONDS) * CGlobal.MILLISECONDS_PER_SECOND;
  }

  public abstract void resend ();

  @Override
  @OverridingMethodsMustInvokeSuper
  public void doStart () throws OpenAS2Exception
  {
    if (m_aTimer != null)
      throw new IllegalStateException ("Resendering timer is already running!");

    m_aTimer = new Timer (true);
    m_aTimer.scheduleAtFixedRate (new PollTask (), 0, TICK_INTERVAL);
  }

  @Override
  @OverridingMethodsMustInvokeSuper
  public void doStop () throws OpenAS2Exception
  {
    if (m_aTimer != null)
    {
      m_aTimer.cancel ();
      m_aTimer = null;
    }
  }
}
