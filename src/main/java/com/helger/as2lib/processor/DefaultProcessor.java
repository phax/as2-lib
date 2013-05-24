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
package com.helger.as2lib.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.processor.exception.NoModuleException;
import com.helger.as2lib.processor.exception.ProcessorException;
import com.helger.as2lib.processor.module.IProcessorActiveModule;
import com.helger.as2lib.processor.module.IProcessorModule;

public class DefaultProcessor extends AbstractProcessor
{
  private List <IProcessorModule> m_aModules;

  public void setModules (final List <IProcessorModule> modules)
  {
    m_aModules = modules;
  }

  public List <IProcessorModule> getModules ()
  {
    if (m_aModules == null)
      m_aModules = new ArrayList <IProcessorModule> ();
    return m_aModules;
  }

  public List <IProcessorActiveModule> getActiveModules ()
  {
    final List <IProcessorActiveModule> activeMods = new ArrayList <IProcessorActiveModule> ();
    for (final IProcessorModule aModule : getModules ())
      if (aModule instanceof IProcessorActiveModule)
        activeMods.add ((IProcessorActiveModule) aModule);
    return activeMods;
  }

  public void handle (final String action, final IMessage msg, final Map <String, Object> options) throws OpenAS2Exception
  {
    ProcessorException pex = null;
    boolean bModuleFound = false;

    for (final IProcessorModule module : getModules ())
    {
      if (module.canHandle (action, msg, options))
      {
        try
        {
          bModuleFound = true;
          module.handle (action, msg, options);
        }
        catch (final OpenAS2Exception oae)
        {
          if (pex == null)
            pex = new ProcessorException (this);
          pex.getCauses ().add (oae);
        }
      }
    }

    if (pex != null)
      throw pex;
    if (!bModuleFound)
      throw new NoModuleException (action, msg, options);
  }

  public void startActiveModules ()
  {
    for (final IProcessorActiveModule aModule : getActiveModules ())
    {
      try
      {
        aModule.start ();
      }
      catch (final OpenAS2Exception e)
      {
        e.terminate ();
      }
    }
  }

  public void stopActiveModules ()
  {
    for (final IProcessorActiveModule aModule : getActiveModules ())
    {
      try
      {
        aModule.stop ();
      }
      catch (final OpenAS2Exception e)
      {
        e.terminate ();
      }
    }
  }
}
