package com.helger.as2lib.partner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.util.IStringMap;
import com.helger.as2lib.util.StringMap;

/**
 * This class represents a single partner. A partnership consists of 2 partners
 * - a sender and a receiver.
 *
 * @author Philip Helger
 */
public class Partner extends StringMap implements IPartner
{
  public static final String PARTNER_NAME = "name";

  public Partner (@Nonnull final IStringMap aAttrs)
  {
    super (aAttrs);
    if (!containsAttribute (PARTNER_NAME))
      throw new IllegalArgumentException ("The provided attributes are missing the required '" +
                                          PARTNER_NAME +
                                          "' attribute!");
  }

  @Nullable
  public String getName ()
  {
    return getAttributeAsString (PARTNER_NAME);
  }
}
