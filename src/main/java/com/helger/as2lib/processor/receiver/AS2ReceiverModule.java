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
package com.helger.as2lib.processor.receiver;

import com.helger.as2lib.message.CNetAttribute;
import com.helger.as2lib.params.MessageParameters;
import com.helger.as2lib.partner.CAS2Partnership;
import com.helger.as2lib.processor.receiver.net.AS2ReceiverHandler;
import com.helger.as2lib.processor.receiver.net.INetModuleHandler;

public class AS2ReceiverModule extends AbstractNetModule
{
  // Macros for responses
  public static final String MSG_SENDER = "$" + MessageParameters.KEY_SENDER + "." + CAS2Partnership.PID_AS2 + "$";
  public static final String MSG_RECEIVER = "$" + MessageParameters.KEY_RECEIVER + "." + CAS2Partnership.PID_AS2 + "$";
  public static final String MSG_DATE = "$" + MessageParameters.KEY_HEADERS + ".date" + "$";
  public static final String MSG_SUBJECT = "$" + MessageParameters.KEY_HEADERS + ".subject" + "$";
  public static final String MSG_SOURCE_ADDRESS = "$" +
                                                  MessageParameters.KEY_ATTRIBUTES +
                                                  "." +
                                                  CNetAttribute.MA_SOURCE_IP +
                                                  "$";
  public static final String DP_HEADER = "The message sent to Recipient " +
                                         MSG_RECEIVER +
                                         " on " +
                                         MSG_DATE +
                                         " with Subject " +
                                         MSG_SUBJECT +
                                         " has been received, ";
  public static final String DP_DECRYPTED = DP_HEADER +
                                            "the EDI Interchange was successfully decrypted and it's integrity was verified. ";
  public static final String DP_VERIFIED = DP_DECRYPTED +
                                           "In addition, the sender of the message, Sender " +
                                           MSG_SENDER +
                                           " at Location " +
                                           MSG_SOURCE_ADDRESS +
                                           " was authenticated as the originator of the message. ";

  // Response texts
  public static final String DISP_PARTNERSHIP_NOT_FOUND = DP_HEADER +
                                                          "but the Sender " +
                                                          MSG_SENDER +
                                                          " and/or Recipient " +
                                                          MSG_RECEIVER +
                                                          " are unknown.";
  public static final String DISP_PARSING_MIME_FAILED = DP_HEADER +
                                                        "but an error occured while parsing the MIME content.";
  public static final String DISP_DECRYPTION_ERROR = DP_HEADER + "but an error occured decrypting the content.";
  public static final String DISP_VERIFY_SIGNATURE_FAILED = DP_DECRYPTED +
                                                            "Authentication of the originator of the message failed.";
  public static final String DISP_STORAGE_FAILED = DP_VERIFIED +
                                                   " An error occured while storing the data to the file system.";
  public static final String DISP_SUCCESS = DP_VERIFIED +
                                            "There is no guarantee however that the EDI Interchange was syntactically correct, or was received by the EDI application/translator.";

  @Override
  protected INetModuleHandler getHandler ()
  {
    return new AS2ReceiverHandler (this);
  }

}
