/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2014 Philip Helger ph[at]phloc[dot]com
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

import com.helger.commons.CGlobal;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotations.OverrideOnDemand;
import com.helger.commons.annotations.ReturnsMutableCopy;
import com.helger.commons.collections.ContainerHelper;
import com.helger.commons.collections.attrs.AbstractReadonlyAttributeContainer;
import com.helger.commons.equals.EqualsUtils;
import com.helger.commons.hash.HashCodeGenerator;
import com.helger.commons.state.EChange;
import com.helger.commons.state.EContinue;
import com.helger.commons.string.ToStringGenerator;

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
    ValueEnforcer.notNull (aMap, "Map");
    m_aAttrs = ContainerHelper.newMap (aMap);
  }

  public StringMap (@Nonnull final IStringMap aCont)
  {
    ValueEnforcer.notNull (aCont, "Cont");
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
  public final EChange addAttributes (@Nullable final Map <String, String> aValues)
  {
    EChange ret = EChange.UNCHANGED;
    if (aValues != null)
      for (final Map.Entry <String, String> aEntry : aValues.entrySet ())
        ret = ret.or (setAttribute (aEntry.getKey (), aEntry.getValue ()));
    return ret;
  }

  @Nonnull
  public final EChange addAttributes (@Nullable final IStringMap aValues)
  {
    return addAttributes (aValues != null ? aValues.getAllAttributes () : null);
  }

  @Nonnull
  public final EChange setAttributes (@Nullable final Map <String, String> aValues)
  {
    EChange ret = clear ();
    ret = ret.or (addAttributes (aValues));
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
  @ReturnsMutableCopy
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
