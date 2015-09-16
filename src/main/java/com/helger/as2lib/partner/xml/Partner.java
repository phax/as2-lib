package com.helger.as2lib.partner.xml;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import com.helger.as2lib.util.IStringMap;
import com.helger.as2lib.util.StringMap;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;

/**
 * This class represents a single partner. A partnership consists of 2 partners
 * - a sender and a receiver.
 *
 * @author Philip Helger
 * @since 2.2.0
 */
public class Partner implements IPartner
{
  public static final String PARTNER_NAME = "name";

  private final StringMap m_aAttrs;

  public Partner (@Nonnull final IStringMap aAttrs)
  {
    m_aAttrs = new StringMap (aAttrs);
    if (!m_aAttrs.containsAttribute (PARTNER_NAME))
      throw new IllegalArgumentException ("The provided attributes are missing the required '" +
                                          PARTNER_NAME +
                                          "' attribute!");
  }

  @Nonnull
  public String getName ()
  {
    return m_aAttrs.getAttributeAsString (PARTNER_NAME);
  }

  @Nonnull
  @ReturnsMutableCopy
  public Map <String, String> getAllAttributes ()
  {
    return m_aAttrs.getAllAttributes ();
  }

  @Nonnull
  public Iterator <Entry <String, String>> iterator ()
  {
    return m_aAttrs.iterator ();
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final Partner rhs = (Partner) o;
    return m_aAttrs.equals (rhs.m_aAttrs);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aAttrs).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Attrs", m_aAttrs).toString ();
  }
}
