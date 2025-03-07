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
package com.helger.as2.cmd.partner;

import com.helger.as2.cmd.AbstractCommand;
import com.helger.as2.cmd.CommandResult;
import com.helger.as2.cmd.ECommandResultType;
import com.helger.as2lib.exception.AS2Exception;
import com.helger.as2lib.partner.IPartnershipFactory;
import com.helger.as2lib.partner.IRefreshablePartnershipFactory;

/**
 * reloads the partnership store
 */
public class RefreshPartnershipsCommand extends AbstractCommand
{
  @Override
  public String getDefaultDescription ()
  {
    return "Refresh the current partnerships from storage";
  }

  @Override
  public String getDefaultName ()
  {
    return "refresh";
  }

  @Override
  public String getDefaultUsage ()
  {
    return "refresh";
  }

  @Override
  public CommandResult execute (final Object [] params)
  {
    try
    {
      final IPartnershipFactory partnerFx = getSession ().getPartnershipFactory ();
      if (partnerFx instanceof IRefreshablePartnershipFactory)
      {
        ((IRefreshablePartnershipFactory) partnerFx).refreshPartnershipFactory ();

        return new CommandResult (ECommandResultType.TYPE_OK, "Refreshed partnerships");
      }
      return new CommandResult (ECommandResultType.TYPE_COMMAND_NOT_SUPPORTED, "Not supported by current certificate store");
    }
    catch (final AS2Exception oae)
    {
      oae.terminate ();
      return new CommandResult (oae);
    }
  }
}
