/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013 Philip Helger ph[at]phloc[dot]com
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.phloc.commons.annotations.ReturnsMutableCopy;
import com.phloc.commons.microdom.IMicroElement;

public final class XMLUtil
{
  private XMLUtil ()
  {}

  @Nonnull
  @ReturnsMutableCopy
  public static Map <String, String> getAttrsWithLowercaseName (@Nonnull final IMicroElement aElement)
  {
    final Map <String, String> ret = new HashMap <String, String> ();
    for (final Map.Entry <String, String> aEntry : aElement.getAllAttributes ().entrySet ())
      ret.put (aEntry.getKey ().toLowerCase (Locale.US), aEntry.getValue ());
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public static Map <String, String> getAttrsWithLowercaseNameWithRequired (@Nonnull final IMicroElement aElement,
                                                                            final String... aRequiredAttributes) throws OpenAS2Exception
  {
    final Map <String, String> attributes = getAttrsWithLowercaseName (aElement);
    for (final String sRequiredAttribute : aRequiredAttributes)
      if (!attributes.containsKey (sRequiredAttribute))
        throw new OpenAS2Exception (aElement.getTagName () + " is missing required attribute: " + sRequiredAttribute);

    return attributes;
  }

  public static Map <String, String> mapAttributeNodes (final IMicroElement node,
                                                        final String nodeName,
                                                        final String nodeKeyName,
                                                        final String nodeValueName) throws OpenAS2Exception
  {
    final Map <String, String> attributes = new HashMap <String, String> ();
    for (final IMicroElement eChild : node.getAllChildElements (nodeName))
    {
      final String sName = eChild.getAttribute (nodeKeyName);
      if (sName == null)
        throw new OpenAS2Exception (eChild.toString () + " does not have key attribute: " + nodeKeyName);
      final String sValue = eChild.getAttribute (nodeValueName);
      if (sValue == null)
        throw new OpenAS2Exception (eChild.toString () + " does not have value attribute: " + nodeValueName);
      attributes.put (sName, sValue);
    }
    return attributes;
  }
}
