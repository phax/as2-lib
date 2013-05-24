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
package com.helger.as2lib.processor.sender;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.processor.module.AbstractProcessorModule;
import com.helger.as2lib.processor.resender.IProcessorResenderModule;

public abstract class AbstractSenderModule extends AbstractProcessorModule implements IProcessorSenderModule
{
  // How many times should this message be sent?
  protected final int retries (@Nullable final Map <String, Object> options)
  {
    String left = options == null ? null : (String) options.get (IProcessorSenderModule.SOPT_RETRIES);
    if (left == null)
    {
      left = getParameterNotRequired (IProcessorSenderModule.SOPT_RETRIES);
      if (left == null)
        return IProcessorSenderModule.DEFAULT_RETRIES;
    }

    return Integer.parseInt (left);
  }

  protected final boolean doResend (final String how, final IMessage msg, final OpenAS2Exception cause, final int tries) throws OpenAS2Exception
  {
    if (tries <= 0)
      return false;
    final Map <String, Object> options = new HashMap <String, Object> ();
    options.put (IProcessorResenderModule.OPTION_CAUSE, cause);
    options.put (IProcessorResenderModule.OPTION_INITIAL_SENDER, this);
    options.put (IProcessorResenderModule.OPTION_RESEND_METHOD, how);
    options.put (IProcessorResenderModule.OPTION_RETRIES, Integer.toString (tries));
    getSession ().getProcessor ().handle (IProcessorResenderModule.DO_RESEND, msg, options);
    return true;
  }
}
