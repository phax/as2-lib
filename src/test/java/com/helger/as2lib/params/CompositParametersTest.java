package com.helger.as2lib.params;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

import com.helger.as2lib.exception.InvalidParameterException;
import com.helger.as2lib.message.AS2Message;

public class CompositParametersTest
{
  @Test
  public void testBasic () throws InvalidParameterException
  {
    final AS2Message aMsg = new AS2Message ();
    aMsg.addHeader ("message-id", "12345");
    aMsg.getPartnership ().setSenderID ("as2_id", "s1");
    aMsg.getPartnership ().setReceiverID ("as2_id", "r1");
    final CompositeParameters aParams = new CompositeParameters (false).add ("date", new DateParameters ())
                                                                       .add ("msg", new MessageParameters (aMsg));

    final String sNow = new SimpleDateFormat ("yyyyMMddhhmmss").format (new Date ());

    // Note: the date assertions may fail if they are executed exactly at the
    // edge of a second!
    String sName = aParams.format ("$date.yyyyMMddhhmmss$");
    assertEquals (sNow, sName);

    sName = aParams.format ("any$date.yyyyMMddhhmmss$else");
    assertEquals ("any" + sNow + "else", sName);

    // No placeholders
    assertEquals ("sender.as2_id, receiver.as2_id, headers.message-id",
                  aParams.format ("sender.as2_id, receiver.as2_id, headers.message-id"));

    // No placeholders
    assertEquals ("s1, r1, 12345",
                  aParams.format ("$msg.sender.as2_id$, $msg.receiver.as2_id$, $msg.headers.message-id$"));
  }
}
