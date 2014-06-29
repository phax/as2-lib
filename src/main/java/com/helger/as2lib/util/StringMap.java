/**
 * Copyright (C) 2006-2014 phloc systems
 * http://www.phloc.com
 * office[at]phloc[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.as2lib.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.phloc.commons.CGlobal;
import com.phloc.commons.ValueEnforcer;
import com.phloc.commons.annotations.OverrideOnDemand;
import com.phloc.commons.annotations.ReturnsMutableCopy;
import com.phloc.commons.collections.ContainerHelper;
import com.phloc.commons.collections.attrs.AbstractReadonlyAttributeContainer;
import com.phloc.commons.equals.EqualsUtils;
import com.phloc.commons.hash.HashCodeGenerator;
import com.phloc.commons.state.EChange;
import com.phloc.commons.state.EContinue;
import com.phloc.commons.string.ToStringGenerator;

/**
 * Base class for all kind of string-string mapping container. This
 * implementation is not thread-safe!
 * 
 * @author Philip Helger
 */
@NotThreadSafe
public class StringMap implements IStringMap, Serializable
{
  /**
   * attribute storage.
   */
  private final Map <String, String> m_aAttrs;

  public StringMap ()
  {
    m_aAttrs = new HashMap <String, String> ();
  }

  public StringMap (@Nonnull final Map <String, String> aMap)
  {
    if (aMap == null)
      throw new NullPointerException ("map");
    m_aAttrs = ContainerHelper.newMap (aMap);
  }

  public StringMap (@Nonnull final IStringMap aCont)
  {
    if (aCont == null)
      throw new NullPointerException ("cont");
    // Must already be a copy!
    m_aAttrs = aCont.getAllAttributes ();
  }

  public boolean containsAttribute (@Nullable final String sName)
  {
    // ConcurrentHashMap cannot handle null keys
    return sName != null && m_aAttrs.containsKey (sName);
  }

  @Nonnull
  @ReturnsMutableCopy
  public Map <String, String> getAllAttributes ()
  {
    return ContainerHelper.newMap (m_aAttrs);
  }

  @Nullable
  public String getAttributeObject (@Nullable final String sName)
  {
    // ConcurrentHashMap cannot handle null keys
    return sName == null ? null : m_aAttrs.get (sName);
  }

  @Nullable
  public final String getAttributeAsString (@Nullable final String sName)
  {
    return getAttributeObject (sName);
  }

  @Nullable
  public final String getAttributeAsString (@Nullable final String sName, @Nullable final String sDefault)
  {
    final String sValue = getAttributeObject (sName);
    return sValue != null ? sValue : sDefault;
  }

  public final int getAttributeAsInt (@Nullable final String sName)
  {
    return getAttributeAsInt (sName, CGlobal.ILLEGAL_UINT);
  }

  public final int getAttributeAsInt (@Nullable final String sName, final int nDefault)
  {
    final String sValue = getAttributeObject (sName);
    return AbstractReadonlyAttributeContainer.getAsInt (sName, sValue, nDefault);
  }

  public final long getAttributeAsLong (@Nullable final String sName)
  {
    return getAttributeAsLong (sName, CGlobal.ILLEGAL_ULONG);
  }

  public final long getAttributeAsLong (@Nullable final String sName, final long nDefault)
  {
    final String sValue = getAttributeObject (sName);
    return AbstractReadonlyAttributeContainer.getAsLong (sName, sValue, nDefault);
  }

  public final double getAttributeAsDouble (@Nullable final String sName)
  {
    return getAttributeAsDouble (sName, CGlobal.ILLEGAL_UINT);
  }

  public final double getAttributeAsDouble (@Nullable final String sName, final double dDefault)
  {
    final String sValue = getAttributeObject (sName);
    return AbstractReadonlyAttributeContainer.getAsDouble (sName, sValue, dDefault);
  }

  public final boolean getAttributeAsBoolean (@Nullable final String sName)
  {
    return getAttributeAsBoolean (sName, false);
  }

  public final boolean getAttributeAsBoolean (@Nullable final String sName, final boolean bDefault)
  {
    final String sValue = getAttributeObject (sName);
    return AbstractReadonlyAttributeContainer.getAsBoolean (sName, sValue, bDefault);
  }

  /**
   * Internal callback method that can be used to check constraints on an
   * attribute name or value.
   * 
   * @param sName
   *        The attribute name. Never <code>null</code>.
   * @param aValue
   *        The attribute value. Never <code>null</code>.
   * @return {@link EContinue#CONTINUE} to indicate that the name-value-pair is
   *         OK. May not be <code>null</code>.
   */
  @OverrideOnDemand
  @Nonnull
  protected EContinue onBeforeSetAttributeValue (@Nonnull final String sName, @Nonnull final String aValue)
  {
    return EContinue.CONTINUE;
  }

  @Nonnull
  public EChange setAttribute (@Nonnull final String sName, @Nullable final String aValue)
  {
    ValueEnforcer.notNull (sName, "Name");

    if (aValue == null)
      return removeAttribute (sName);

    // Callback for checks etc.
    if (onBeforeSetAttributeValue (sName, aValue).isBreak ())
      return EChange.UNCHANGED;

    final String aOldValue = m_aAttrs.put (sName, aValue);
    return EChange.valueOf (!EqualsUtils.equals (aValue, aOldValue));
  }

  @Nonnull
  public final EChange setAttribute (@Nonnull final String sName, final boolean dValue)
  {
    return setAttribute (sName, Boolean.toString (dValue));
  }

  @Nonnull
  public final EChange setAttribute (@Nonnull final String sName, final int nValue)
  {
    return setAttribute (sName, Integer.toString (nValue));
  }

  @Nonnull
  public final EChange setAttribute (@Nonnull final String sName, final long nValue)
  {
    return setAttribute (sName, Long.toString (nValue));
  }

  @Nonnull
  public final EChange setAttribute (@Nonnull final String sName, final double dValue)
  {
    return setAttribute (sName, Double.toString (dValue));
  }

  @Nonnull
  public final EChange setAttributes (@Nullable final Map <String, String> aValues)
  {
    EChange ret = clear ();
    if (aValues != null)
      for (final Map.Entry <String, String> aEntry : aValues.entrySet ())
        ret = ret.or (setAttribute (aEntry.getKey (), aEntry.getValue ()));
    return ret;
  }

  @Nonnull
  public final EChange setAttributes (@Nullable final IStringMap aValues)
  {
    return setAttributes (aValues != null ? aValues.getAllAttributes () : null);
  }

  /**
   * Internal callback method that can be used to avoid removal of an attribute.
   * 
   * @param sName
   *        The attribute name. Never <code>null</code>.
   * @return {@link EContinue#CONTINUE} to indicate that the name-value-pair is
   *         OK. May not be <code>null</code>.
   */
  @OverrideOnDemand
  @Nonnull
  protected EContinue onBeforeRemoveAttribute (@Nonnull final String sName)
  {
    return EContinue.CONTINUE;
  }

  @Nonnull
  public EChange removeAttribute (@Nullable final String sName)
  {
    if (sName == null)
      return EChange.UNCHANGED;

    // Callback method
    if (onBeforeRemoveAttribute (sName).isBreak ())
      return EChange.UNCHANGED;

    // Returned value may be null
    return EChange.valueOf (m_aAttrs.remove (sName) != null);
  }

  @Nonnull
  public Enumeration <String> getAttributeNames ()
  {
    // Build an enumerator on top of the set
    return ContainerHelper.getEnumeration (m_aAttrs.keySet ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public Set <String> getAllAttributeNames ()
  {
    return ContainerHelper.newSet (m_aAttrs.keySet ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <String> getAllAttributeValues ()
  {
    return ContainerHelper.newList (m_aAttrs.values ());
  }

  @Nonnegative
  public int getAttributeCount ()
  {
    return m_aAttrs.size ();
  }

  public boolean containsNoAttribute ()
  {
    return m_aAttrs.isEmpty ();
  }

  public boolean getAndSetAttributeFlag (@Nonnull final String sName)
  {
    final String aOldValue = getAttributeObject (sName);
    if (aOldValue != null)
    {
      // Attribute flag is already present
      return true;
    }
    // Attribute flag is not yet present -> set it
    setAttribute (sName, true);
    return false;
  }

  @Nonnull
  public EChange clear ()
  {
    if (m_aAttrs.isEmpty ())
      return EChange.UNCHANGED;
    m_aAttrs.clear ();
    return EChange.CHANGED;
  }

  @Nonnull
  public StringMap getClone ()
  {
    return new StringMap (m_aAttrs);
  }

  @Nonnull
  public Iterator <Entry <String, String>> iterator ()
  {
    return m_aAttrs.entrySet ().iterator ();
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final StringMap rhs = (StringMap) o;
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
    return new ToStringGenerator (this).append ("attrs", m_aAttrs).toString ();
  }
}
