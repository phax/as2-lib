package com.helger.as2lib.disposition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.annotation.concurrent.Immutable;

import org.junit.Test;

import com.helger.as2lib.exception.AS2Exception;

/**
 * Test class for class {@link DispositionType}.
 *
 * @author Philip Helger
 */
@Immutable
public final class DispositionTypeTest
{
  @Test
  public void testSuccess () throws AS2Exception
  {
    final DispositionType a = DispositionType.createSuccess ();
    assertEquals (DispositionType.ACTION_AUTOMATIC_ACTION, a.getAction ());
    assertEquals (DispositionType.MDNACTION_MDN_SENT_AUTOMATICALLY, a.getMDNAction ());
    assertEquals (DispositionType.STATUS_PROCESSED, a.getStatus ());
    assertNull (a.getStatusDescription ());
    assertNull (a.getStatusModifier ());

    final DispositionType a2 = DispositionType.createFromString (a.getAsString ());
    assertTrue (a.getAsString ().equalsIgnoreCase (a2.getAsString ()));
  }

  @Test
  public void testError () throws AS2Exception
  {
    final DispositionType a = DispositionType.createError ("Bla Blu");
    assertEquals (DispositionType.ACTION_AUTOMATIC_ACTION, a.getAction ());
    assertEquals (DispositionType.MDNACTION_MDN_SENT_AUTOMATICALLY, a.getMDNAction ());
    assertEquals (DispositionType.STATUS_PROCESSED, a.getStatus ());
    assertEquals (DispositionType.STATUS_MODIFIER_ERROR, a.getStatusModifier ());
    assertEquals ("Bla Blu", a.getStatusDescription ());

    final DispositionType a2 = DispositionType.createFromString (a.getAsString ());
    assertTrue (a.getAsString ().equalsIgnoreCase (a2.getAsString ()));
  }

  @Test
  public void testCrappy () throws AS2Exception
  {
    final DispositionType a = new DispositionType ("a", "b", "c", "d", "e");
    assertEquals ("a", a.getAction ());
    assertEquals ("b", a.getMDNAction ());
    assertEquals ("c", a.getStatus ());
    assertEquals ("d", a.getStatusModifier ());
    assertEquals ("e", a.getStatusDescription ());

    final DispositionType a2 = DispositionType.createFromString (a.getAsString ());
    assertTrue (a.getAsString ().equalsIgnoreCase (a2.getAsString ()));
  }
}
