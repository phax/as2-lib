/*
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2023 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.message;

import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;

import com.helger.as2lib.CAS2Info;
import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.util.AS2DateHelper;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.string.StringHelper;

public class AS2MessageMDN extends AbstractMessageMDN
{
  public static final String MDNA_REPORTING_UA = "REPORTING_UA";
  public static final String MDNA_ORIG_RECIPIENT = "ORIGINAL_RECIPIENT";
  public static final String MDNA_FINAL_RECIPIENT = "FINAL_RECIPIENT";
  public static final String MDNA_ORIG_MESSAGEID = "ORIGINAL_MESSAGE_ID";
  public static final String MDNA_DISPOSITION = "DISPOSITION";
  public static final String MDNA_MIC = "MIC";
  public static final String DEFAULT_DATE_FORMAT = "ddMMuuuuHHmmssZ";

  public AS2MessageMDN (@Nonnull final AS2Message aMsg)
  {
    super (aMsg);
    // Swap from and to
    headers ().setHeader (CHttpHeader.AS2_TO, aMsg.getAS2From ());
    headers ().setHeader (CHttpHeader.AS2_FROM, aMsg.getAS2To ());
  }

  @Override
  public String generateMessageID ()
  {
    final StringBuilder aSB = new StringBuilder ();
    final String sDateFormat = partnership ().getDateFormat (DEFAULT_DATE_FORMAT);
    aSB.append ('<').append (CAS2Info.NAME).append ('-').append (AS2DateHelper.getFormattedDateNow (sDateFormat));

    final int nRandom = ThreadLocalRandom.current ().nextInt (10_000);
    aSB.append ('-').append (StringHelper.getLeadingZero (nRandom, 4));

    // Message details
    final Partnership aPartnership = getMessage ().partnership ();
    final String sReceiverID = aPartnership.getReceiverAS2ID ();
    final String sSenderID = aPartnership.getSenderAS2ID ();
    aSB.append ('@').append (sReceiverID).append ('_').append (sSenderID);

    return aSB.append ('>').toString ();
  }
}
