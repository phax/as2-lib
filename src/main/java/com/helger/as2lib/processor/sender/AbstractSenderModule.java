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
package com.helger.as2lib.processor.sender;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.processor.module.AbstractProcessorModule;
import com.helger.as2lib.processor.resender.IProcessorResenderModule;

public abstract class AbstractSenderModule extends AbstractProcessorModule implements IProcessorSenderModule
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AbstractSenderModule.class);

  /**
   * How many times should this message be sent?
   *
   * @param aPartnership
   *        The partnership to retrieve the value from. If a retry count is
   *        available in the partnership it takes precedence over the ones
   *        defined in the options or attributes. May be <code>null</code>.
   * @param aOptions
   *        Options to choose from. May be <code>null</code>.
   * @return 0 to indicate no retry.
   */
  @Nonnegative
  protected final int getRetryCount (@Nullable final Partnership aPartnership,
                                     @Nullable final Map <String, Object> aOptions)
  {
    String sTriesLeft = null;

    if (aPartnership != null)
    {
      // Get from partnership
      sTriesLeft = aPartnership.getAttribute (IProcessorResenderModule.OPTION_RETRIES);
    }

    if (sTriesLeft == null && aOptions != null)
    {
      // Provided in the options?
      sTriesLeft = (String) aOptions.get (IProcessorResenderModule.OPTION_RETRIES);
    }

    if (sTriesLeft == null)
    {
      // No. Provided as an attribute?
      sTriesLeft = getAttributeAsString (IProcessorResenderModule.OPTION_RETRIES);
    }

    if (sTriesLeft == null)
    {
      // Not provided. Use default.
      return IProcessorResenderModule.DEFAULT_RETRIES;
    }

    // Avoid returning negative values
    return Math.max (Integer.parseInt (sTriesLeft), 0);
  }

  /**
   * @param sHow
   *        Handler action name to use. May not be <code>null</code>.
   * @param aMsg
   *        The message to be resend. May be an AS2 message or an MDN.
   * @param aCause
   *        The error cause.
   * @param nTries
   *        The number of retries left.
   * @return <code>true</code> if the message was scheduled for re-sending.
   * @throws OpenAS2Exception
   *         In case of an error
   */
  protected final boolean doResend (@Nonnull final String sHow,
                                    @Nonnull final IMessage aMsg,
                                    @Nullable final OpenAS2Exception aCause,
                                    final int nTries) throws OpenAS2Exception
  {
    if (nTries <= 0)
      return false;

    final Map <String, Object> aOptions = new HashMap <String, Object> ();
    aOptions.put (IProcessorResenderModule.OPTION_CAUSE, aCause);
    aOptions.put (IProcessorResenderModule.OPTION_INITIAL_SENDER, this);
    aOptions.put (IProcessorResenderModule.OPTION_RESEND_METHOD, sHow);
    aOptions.put (IProcessorResenderModule.OPTION_RETRIES, Integer.toString (nTries));
    getSession ().getMessageProcessor ().handle (IProcessorResenderModule.DO_RESEND, aMsg, aOptions);

    s_aLogger.info ("Scheduled message " + aMsg.getMessageID () + " for re-sending");
    return true;
  }
}
