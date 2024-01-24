/*
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2024 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.partner.xml;

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import com.helger.as2lib.exception.AS2Exception;
import com.helger.as2lib.partner.AS2PartnershipNotFoundException;
import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.partner.SelfFillingPartnershipFactory;
import com.helger.commons.annotation.OverrideOnDemand;

/**
 * A special {@link XMLPartnershipFactory} that adds a new partnership if it is
 * not yet existing.
 *
 * @author Philip Helger
 */
public class SelfFillingXMLPartnershipFactory extends XMLPartnershipFactory
{
  /**
   * Callback method that is invoked every time a new partnership is
   * automatically added. This method is called BEFORE the main add-process is
   * started.
   *
   * @param aPartnership
   *        The partnership that will be added. Never <code>null</code>.
   * @throws AS2Exception
   *         In case of an error.
   */
  @OverrideOnDemand
  @OverridingMethodsMustInvokeSuper
  protected void onBeforeAddPartnership (@Nonnull final Partnership aPartnership) throws AS2Exception
  {
    SelfFillingPartnershipFactory.ensureUsablePartnership (aPartnership);
  }

  @Override
  @Nonnull
  public Partnership getPartnership (@Nonnull final Partnership aPartnership) throws AS2Exception
  {
    try
    {
      return super.getPartnership (aPartnership);
    }
    catch (final AS2PartnershipNotFoundException ex)
    {
      onBeforeAddPartnership (aPartnership);

      // Create a new one
      addPartnership (aPartnership);
      return aPartnership;
    }
  }
}
