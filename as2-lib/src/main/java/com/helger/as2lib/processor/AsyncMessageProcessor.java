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
package com.helger.as2lib.processor;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.commons.annotation.CodingStyleguideUnaware;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.callback.exception.IExceptionCallback;

/**
 * An implementation of {@link AbstractMessageProcessor} that uses a separate
 * thread for performing the main actions.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class AsyncMessageProcessor extends AbstractMessageProcessor
{
  private static final class HandleObject
  {
    private final String m_sAction;
    private final IMessage m_aMsg;
    @CodingStyleguideUnaware
    private final Map <String, Object> m_aOptions;

    public HandleObject (@Nonnull final String sAction,
                         @Nonnull final IMessage aMsg,
                         @Nullable final Map <String, Object> aOptions)
    {
      m_sAction = sAction;
      m_aMsg = aMsg;
      m_aOptions = aOptions;
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (AsyncMessageProcessor.class);

  private final BlockingQueue <HandleObject> m_aQueue = new LinkedBlockingQueue <> ();
  private final CallbackList <IExceptionCallback <Throwable>> m_aExceptionCallbacks = new CallbackList <> ();

  public AsyncMessageProcessor ()
  {
    final Runnable aRunnable = () -> {
      // The temporary list that contains all objects to be delivered
      while (true)
      {
        try
        {
          // Block until the first object is in the queue
          final HandleObject aCurrentObject = m_aQueue.poll (1, TimeUnit.SECONDS);
          if (aCurrentObject != null)
            AsyncMessageProcessor.this.executeAction (aCurrentObject.m_sAction,
                                                      aCurrentObject.m_aMsg,
                                                      aCurrentObject.m_aOptions);
        }
        catch (final InterruptedException ex)
        {
          Thread.currentThread ().interrupt ();
          LOGGER.error ("Error taking elements from queue - queue has been interrupted!!!");
          break;
        }
        catch (final NoModuleException ex)
        {
          m_aExceptionCallbacks.forEach (x -> x.onException (ex));
          // No need to log
        }
        catch (final Exception ex)
        {
          if (m_aExceptionCallbacks.isNotEmpty ())
            m_aExceptionCallbacks.forEach (x -> x.onException (ex));
          else
            LOGGER.error ("Error executing action", ex);
        }
      }
    };
    final Thread aProcessorThread = new Thread (aRunnable, "AS2-AsyncMessageProcessor");
    aProcessorThread.setDaemon (true);
    aProcessorThread.start ();
  }

  @Nonnull
  @ReturnsMutableObject
  public final CallbackList <IExceptionCallback <Throwable>> exceptionCallbacks ()
  {
    return m_aExceptionCallbacks;
  }

  public void handle (@Nonnull final String sAction,
                      @Nonnull final IMessage aMsg,
                      @Nullable final Map <String, Object> aOptions) throws OpenAS2Exception
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("AsyncMessageProcessor.handle (" + sAction + "," + aMsg + "," + aOptions + ")");

    try
    {
      m_aQueue.put (new HandleObject (sAction, aMsg, aOptions));
    }
    catch (final InterruptedException ex)
    {
      Thread.currentThread ().interrupt ();
      throw new OpenAS2Exception ("Failed to queue action " + sAction, ex);
    }
  }
}
