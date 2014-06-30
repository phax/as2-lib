package com.helger.as2lib.util;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.phloc.commons.ICloneable;
import com.phloc.commons.annotations.ReturnsMutableCopy;

public interface IStringMap extends ICloneable <IStringMap>, Iterable <Map.Entry <String, String>>
{
  boolean containsAttribute (@Nullable String sName);

  @Nonnull
  @ReturnsMutableCopy
  Map <String, String> getAllAttributes ();

  @Nullable
  String getAttributeObject (@Nullable String sName);

  @Nullable
  String getAttributeAsString (@Nullable String sName);

  @Nullable
  String getAttributeAsString (@Nullable String sName, @Nullable String sDefault);

  int getAttributeAsInt (@Nullable String sName);

  int getAttributeAsInt (@Nullable String sName, int nDefault);

  long getAttributeAsLong (@Nullable String sName);

  long getAttributeAsLong (@Nullable String sName, long nDefault);

  double getAttributeAsDouble (@Nullable String sName);

  double getAttributeAsDouble (@Nullable String sName, double dDefault);

  boolean getAttributeAsBoolean (@Nullable String sName);

  boolean getAttributeAsBoolean (@Nullable String sName, boolean bDefault);

  @Nonnull
  @ReturnsMutableCopy
  Enumeration <String> getAttributeNames ();

  @Nonnull
  @ReturnsMutableCopy
  Set <String> getAllAttributeNames ();

  @Nonnull
  @ReturnsMutableCopy
  Collection <String> getAllAttributeValues ();

  @Nonnegative
  int getAttributeCount ();

  boolean containsNoAttribute ();

  boolean getAndSetAttributeFlag (String sName);

  Iterator <Entry <String, String>> iterator ();
}
