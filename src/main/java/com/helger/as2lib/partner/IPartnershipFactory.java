/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2014 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.partner;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.helger.as2lib.IDynamicComponent;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.util.StringMap;
import com.helger.commons.annotations.ReturnsMutableCopy;

/**
 * original author unknown added getPartners method
 *
 * @author joseph mcverry
 */
public interface IPartnershipFactory extends IDynamicComponent
{
  String COMPID_PARTNERSHIP_FACTORY = "partnershipfactory";

  // throws an exception if the partnership doesn't exist
  Partnership getPartnership (Partnership aPartnership) throws OpenAS2Exception;

  void addPartnership (@Nonnull Partnership aPartnership);

  void removePartnership (@Nonnull Partnership aPartnership);

  /**
   * looks up and fills in any header info for a specific msg's partnership.
   *
   * @param aMsg
   *        The message in which the partnership should be updated. May not be
   *        <code>null</code> and must already contain a partnership with at
   *        least name or sender and receiver IDs.
   * @param bOverwrite
   *        <code>true</code> to also set the subject of the message
   * @throws OpenAS2Exception
   *         In case of an error
   */
  void updatePartnership (@Nonnull IMessage aMsg, boolean bOverwrite) throws OpenAS2Exception;

  // looks up and fills in any header info for a specific msg's partnership
  void updatePartnership (@Nonnull IMessageMDN aMdn, boolean bOverwrite) throws OpenAS2Exception;

  @Nonnull
  @ReturnsMutableCopy
  List <Partnership> getAllPartnerships ();

  @Deprecated
  Map <String, StringMap> getPartners ();
}
