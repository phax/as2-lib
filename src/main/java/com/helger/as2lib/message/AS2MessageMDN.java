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
package com.helger.as2lib.message;

import java.text.DecimalFormat;


import com.helger.as2lib.partner.CAS2Partnership;
import com.helger.as2lib.partner.CCustomIDPartnership;
import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.DateUtil;
import com.phloc.commons.random.VerySecureRandom;

public class AS2MessageMDN extends AbstractMessageMDN
{
  public static final String MDNA_REPORTING_UA = "REPORTING_UA";
  public static final String MDNA_ORIG_RECIPIENT = "ORIGINAL_RECIPIENT";
  public static final String MDNA_FINAL_RECIPIENT = "FINAL_RECIPIENT";
  public static final String MDNA_ORIG_MESSAGEID = "ORIGINAL_MESSAGE_ID";
  public static final String MDNA_DISPOSITION = "DISPOSITION";
  public static final String MDNA_MIC = "MIC";

  public AS2MessageMDN (final AS2Message msg)
  {
    super (msg);
    setHeader (CAS2Header.AS2_TO, msg.getHeader (CAS2Header.AS2_FROM));
    setHeader (CAS2Header.AS2_FROM, msg.getHeader (CAS2Header.AS2_TO));
  }

  @Override
  public String generateMessageID ()
  {
    final StringBuilder buf = new StringBuilder ();
    String dateFormat = getPartnership ().getAttribute (CCustomIDPartnership.PA_DATE_FORMAT);
    if (dateFormat == null)
    {
      dateFormat = "ddMMyyyyHHmmssZ";
    }
    buf.append ("<OPENAS2-").append (DateUtil.formatDate (dateFormat));

    final DecimalFormat randomFormatter = new DecimalFormat ("0000");
    buf.append ("-").append (randomFormatter.format (VerySecureRandom.getInstance ().nextInt (10000)));

    if (getMessage () != null)
    {
      final Partnership partnership = getMessage ().getPartnership ();
      final String senderID = partnership.getSenderID (CAS2Partnership.PID_AS2);
      final String receiverID = partnership.getReceiverID (CAS2Partnership.PID_AS2);

      buf.append ("@").append (receiverID);
      buf.append ("_").append (senderID);
    }

    buf.append (">");

    return buf.toString ();
  }
}
