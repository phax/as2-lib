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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.processor.sender.IProcessorSenderModule;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;

/**
 * An in-memory based resender module. Upon
 * {@link #handle(String, IMessage, Map)} the document is added to a queue there
 * is a background poller task that checks for resending (see {@link #resend()}
 * ). If resending fails an exception is thrown.
 *
 * @author Philip Helger
 */
public class InMemoryResenderModule extends AbstractActiveResenderModule
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (InMemoryResenderModule.class);

  private final Vector <ResendItem> m_aItems = new Vector <ResendItem> ();

  @Override
  public boolean canHandle (@Nonnull final String sAction,
                            @Nonnull final IMessage aMsg,
                            @Nullable final Map <String, Object> aOptions)
  {
    return sAction.equals (IProcessorResenderModule.DO_RESEND);
  }

  @Override
  public void handle (@Nonnull final String sAction,
                      @Nonnull final IMessage aMsg,
                      @Nullable final Map <String, Object> aOptions) throws OpenAS2Exception
  {
    String sResendAction = (String) aOptions.get (IProcessorResenderModule.OPTION_RESEND_ACTION);
    if (sResendAction == null)
    {
      s_aLogger.warn ("The resending action is missing - default to message sending!");
      sResendAction = IProcessorSenderModule.DO_SEND;
    }

    final String sRetries = (String) aOptions.get (IProcessorResenderModule.OPTION_RETRIES);
    int nRetries;
    if (sRetries != null)
      nRetries = Integer.parseInt (sRetries);
    else
    {
      s_aLogger.warn ("The resending retry count is missing - default to " +
                      IProcessorResenderModule.DEFAULT_RETRIES +
                      "!");
      nRetries = IProcessorResenderModule.DEFAULT_RETRIES;
    }

    final ResendItem aItem = new ResendItem (sResendAction, nRetries, aMsg, getResendDelayMS ());
    m_aItems.add (aItem);

    s_aLogger.info ("Message put in resend queue" + aMsg.getLoggingText ());
  }

  protected void resendItem (@Nonnull final ResendItem aItem) throws OpenAS2Exception
  {
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Resending item");

    IMessage aMsg = null;
    try
    {
      final String sAction = aItem.m_sAction;
      final String sRetries = Integer.toString (aItem.m_nRetries - 1);
      aMsg = aItem.m_aMsg;

      // Transmit the message
      s_aLogger.info ("Loaded message for resend" + aMsg.getLoggingText ());

      final Map <String, Object> aOptions = new HashMap <String, Object> ();
      aOptions.put (IProcessorResenderModule.OPTION_RETRIES, sRetries);
      getSession ().getMessageProcessor ().handle (sAction, aMsg, aOptions);

      // Finally remove from list
      m_aItems.remove (aItem);
    }
    catch (final OpenAS2Exception ex)
    {
      ex.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      throw ex;
    }
  }

  @Override
  public void resend ()
  {
    try
    {
      final List <ResendItem> aResendItems = new ArrayList <ResendItem> ();
      for (final ResendItem aItem : m_aItems)
        if (aItem.isTimeToSend ())
          aResendItems.add (aItem);

      // Resend all selected items
      for (final ResendItem aResendItem : aResendItems)
        resendItem (aResendItem);
    }
    catch (final OpenAS2Exception ex)
    {
      ex.terminate ();
      forceStop (ex);
    }
  }

  @Nonnegative
  public int getResendItemCount ()
  {
    return m_aItems.size ();
  }

  /**
   * Remove all resend items.
   */
  public void removeAllResendItems ()
  {
    final int nItems = getResendItemCount ();
    if (nItems > 0)
    {
      m_aItems.clear ();
      s_aLogger.info ("Removed " + nItems + " items from InMemoryResenderModule");
    }
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <ResendItem> getAllResendItems ()
  {
    return CollectionHelper.newList (m_aItems);
  }

  @Override
  public void doStop () throws OpenAS2Exception
  {
    final int nRemainingItems = getResendItemCount ();
    if (nRemainingItems > 0)
      s_aLogger.error ("InMemoryResenderModule is stopped but " +
                       nRemainingItems +
                       " items are still contained. They are discarded and will be lost!");

    super.doStop ();
  }
}
