/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2015 Philip Helger philip[at]helger[dot]com
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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.IDynamicComponent;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.session.IAS2Session;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.lang.GenericReflection;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.IMicroQName;

@Immutable
public final class XMLHelper
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (XMLHelper.class);
  private static final String DOLLAR_HOME_DOLLAR = "%home%";

  private XMLHelper ()
  {}

  /**
   * Get all attributes of the passed element as a map with a lowercase
   * attribute name.
   *
   * @param aElement
   *        The source element to extract the attributes from. May not be
   *        <code>null</code>.
   * @return A new map and never <code>null</code> but maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  public static StringMap getAllAttrsWithLowercaseName (@Nonnull final IMicroElement aElement)
  {
    ValueEnforcer.notNull (aElement, "Element");

    final StringMap ret = new StringMap ();
    final Map <IMicroQName, String> aAttrs = aElement.getAllQAttributes ();
    if (aAttrs != null)
      for (final Map.Entry <IMicroQName, String> aEntry : aAttrs.entrySet ())
        ret.setAttribute (aEntry.getKey ().getName ().toLowerCase (Locale.US), aEntry.getValue ());
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public static StringMap getAllAttrsWithLowercaseNameWithRequired (@Nonnull final IMicroElement aElement,
                                                                    @Nonnull final String... aRequiredAttributes) throws OpenAS2Exception
  {
    final StringMap aAttributes = getAllAttrsWithLowercaseName (aElement);
    for (final String sRequiredAttribute : aRequiredAttributes)
      if (!aAttributes.containsAttribute (sRequiredAttribute))
        throw new OpenAS2Exception (aElement.getTagName () +
                                    " is missing required attribute '" +
                                    sRequiredAttribute +
                                    "'");
    return aAttributes;
  }

  /**
   * @param aNode
   *        Start node. May not be <code>null</code>.
   * @param sNodeName
   *        The element name to be queried relative to the start node.
   * @param sNodeKeyName
   *        The attribute name of the key.
   * @param sNodeValueName
   *        The attribute name of the value.
   * @return The non-<code>null</code> {@link Map}.
   * @throws OpenAS2Exception
   *         In case a node is missing a key or value attribute.
   */
  @Nonnull
  @ReturnsMutableCopy
  public static Map <String, String> mapAttributeNodes (@Nonnull final IMicroElement aNode,
                                                        @Nonnull final String sNodeName,
                                                        @Nonnull final String sNodeKeyName,
                                                        @Nonnull final String sNodeValueName) throws OpenAS2Exception
  {
    ValueEnforcer.notNull (aNode, "Node");
    ValueEnforcer.notNull (sNodeName, "NodeName");
    ValueEnforcer.notNull (sNodeKeyName, "NodeKeyName");
    ValueEnforcer.notNull (sNodeValueName, "NodeValueName");

    final Map <String, String> ret = new LinkedHashMap <String, String> ();
    int nIndex = 0;
    for (final IMicroElement eChild : aNode.getAllChildElements (sNodeName))
    {
      final String sName = eChild.getAttributeValue (sNodeKeyName);
      if (sName == null)
        throw new OpenAS2Exception (sNodeName + "[" + nIndex + "] does not have key attribute '" + sNodeKeyName + "'");

      final String sValue = eChild.getAttributeValue (sNodeValueName);
      if (sValue == null)
        throw new OpenAS2Exception (sNodeName +
                                    "[" +
                                    nIndex +
                                    "] does not have value attribute '" +
                                    sNodeValueName +
                                    "'");

      ret.put (sName, sValue);
      ++nIndex;
    }
    return ret;
  }

  private static void _updateDirectories (@Nonnull final StringMap aAttributes,
                                          @Nullable final String sBaseDirectory) throws OpenAS2Exception
  {
    for (final Map.Entry <String, String> attrEntry : aAttributes)
    {
      final String sValue = attrEntry.getValue ();
      if (sValue.startsWith (DOLLAR_HOME_DOLLAR))
      {
        if (sBaseDirectory == null)
          throw new OpenAS2Exception ("Base directory isn't set");
        aAttributes.setAttribute (attrEntry.getKey (),
                                  sBaseDirectory + sValue.substring (DOLLAR_HOME_DOLLAR.length ()));
      }
    }
  }

  @Nonnull
  public static <T extends IDynamicComponent> T createComponent (@Nonnull final IMicroElement aElement,
                                                                 @Nonnull final Class <T> aClass,
                                                                 @Nonnull final IAS2Session aSession,
                                                                 @Nullable final String sBaseDirectory) throws OpenAS2Exception
  {
    ValueEnforcer.notNull (aElement, "Element");
    ValueEnforcer.notNull (aClass, "Class");
    ValueEnforcer.notNull (aSession, "Session");

    // Read 'classname' attribute
    final String sClassName = aElement.getAttributeValue ("classname");
    if (sClassName == null)
      throw new OpenAS2Exception ("Missing 'classname' attribute");

    try
    {
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Trying to instantiate '" + sClassName + "' as a " + aClass);

      // Instantiate class
      final T aObj = GenericReflection.newInstance (sClassName, aClass);
      if (aObj == null)
        throw new OpenAS2Exception ("Failed to instantiate '" + sClassName + "' as " + aClass.getName ());

      // Read all parameters
      final StringMap aParameters = XMLHelper.getAllAttrsWithLowercaseName (aElement);
      if (sBaseDirectory != null)
      {
        // Replace %home% with session base directory
        _updateDirectories (aParameters, sBaseDirectory);
      }

      // Init component
      aObj.initDynamicComponent (aSession, aParameters);

      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Finished initializing " + aObj);

      return aObj;
    }
    catch (final OpenAS2Exception ex)
    {
      throw ex;
    }
    catch (final Exception ex)
    {
      throw new WrappedOpenAS2Exception ("Error creating component: " + sClassName, ex);
    }
  }
}
