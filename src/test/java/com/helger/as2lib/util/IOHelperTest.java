package com.helger.as2lib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Test class for class {@link IOHelper}.
 *
 * @author Philip Helger
 */
public final class IOHelperTest
{
  @Test
  public void testGetFilenameFromMessageID ()
  {
    assertNull (IOHelper.getFilenameFromMessageID (""));
    assertNull (IOHelper.getFilenameFromMessageID ("<<<<>>>>"));
    assertEquals ("a", IOHelper.getFilenameFromMessageID ("a"));
    assertEquals ("a", IOHelper.getFilenameFromMessageID ("<a>"));
    assertEquals ("a", IOHelper.getFilenameFromMessageID ("<<<<<a>>>>>"));
    assertEquals ("a@b.c", IOHelper.getFilenameFromMessageID ("<<<<<a@b.c>>>>>"));
  }
}
