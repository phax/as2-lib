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
package com.helger.as2lib.client;

import com.helger.as2lib.crypto.ECryptoAlgorithm;

/**
 * @author oleo Date: May 12, 2010 Time: 5:16:57 PM
 */
public final class AS2ConnectionSettings
{
  public String p12FilePath;
  public String p12FilePassword;

  public String senderEmail = "email";
  public String senderAs2Id = "Sender";
  public String senderKeyAlias = "key_pair";

  public String receiverAs2Id = "Reciever";
  public String receiverKeyAlias = "trusted_key";
  public String receiverAs2Url = "http://172.16.148.1:8080/as2/HttpReceiver";

  public String partnershipName = "partnership name";
  public String mdnOptions = "signed-receipt-protocol=optional, pkcs7-signature; signed-receipt-micalg=optional, sha1";
  public ECryptoAlgorithm encrypt = ECryptoAlgorithm.CRYPT_3DES;
  public ECryptoAlgorithm sign = ECryptoAlgorithm.DIGEST_SHA1;

  public String messageIDFormat = "Test-$date.ddMMyyyyHHmmssZ$-$rand.1234$@$msg.sender.as2_id$_$msg.receiver.as2_id$";
}
