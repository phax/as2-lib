package com.helger.as2lib.partner.xml;

import java.util.Map;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.name.IHasName;

/**
 * Read-only interface for a single partner that is used in a partnership.
 *
 * @author Philip Helger
 * @since 2.2.0
 */
public interface IPartner extends IHasName, Iterable <Map.Entry <String, String>>
{
  /**
   * @return All contained attributes. Neither <code>null</code> nor empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  Map <String, String> getAllAttributes ();
}
