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
package com.helger.as2lib.params;

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;


import com.helger.as2lib.exception.OpenAS2Exception;
import com.phloc.commons.string.StringHelper;

public abstract class AbstractParameterParser
{
  public abstract void setParameter (String key, String value) throws InvalidParameterException;

  public abstract String getParameter (String key) throws InvalidParameterException;

  /**
   * Set parameters from a string, like
   * "msg.sender.as2_id=ME,msg.headers.content-type=application/X12"
   * 
   * @param encodedParams
   *        string to parse
   * @throws InvalidParameterException
   */
  public void setParameters (final String encodedParams) throws InvalidParameterException
  {
    final StringTokenizer params = new StringTokenizer (encodedParams, "=,", false);
    while (params.hasMoreTokens ())
    {
      final String key = params.nextToken ().trim ();

      if (!params.hasMoreTokens ())
      {
        throw new InvalidParameterException ("Invalid value", this, key, null);
      }

      final String value = params.nextToken ();
      setParameter (key, value);
    }
  }

  /**
   * Set parameters from a string seperated by delimiters.
   * 
   * @param format
   *        Comma seperated list of parameters to set, like
   *        <code>msg.sender.as2_id,msg.receiver.as2_id,msg.header.content-type</code>
   * @param delimiters
   *        delimiters in string to parse, like "-."
   * @param value
   *        string to parse, like <code>"NORINCO-WALMART.application/X12"</code>
   * @throws OpenAS2Exception
   */
  public void setParameters (final String format, final String delimiters, final String value) throws OpenAS2Exception
  {
    final List <String> keys = StringHelper.getExploded (',', format);

    final StringTokenizer valueTokens = new StringTokenizer (value, delimiters, false);
    final Iterator <String> keyIt = keys.iterator ();
    while (keyIt.hasNext ())
    {
      if (!valueTokens.hasMoreTokens ())
        throw new OpenAS2Exception ("Invalid value: Format=" + format + ", value=" + value);

      final String key = keyIt.next ().trim ();
      if (key.length () > 0)
      {
        setParameter (key, valueTokens.nextToken ());
      }
    }
  }

  /**
   * Static way (why?) of getting at format method.
   * 
   * @param format
   *        the format to fill in
   * @param parser
   *        the place to get the parsed info
   * @return the filled in format
   * @throws InvalidParameterException
   */
  public static String parse (final String format, final AbstractParameterParser parser) throws InvalidParameterException
  {
    return parser.format (format);
  }

  /**
   * Fill in a format string with information from a ParameterParser
   * 
   * @param format
   *        the format string to fill in
   * @return the filled in format string.
   * @throws InvalidParameterException
   */
  public String format (final String format) throws InvalidParameterException
  {
    final StringBuilder result = new StringBuilder ();
    for (int next = 0; next < format.length (); ++next)
    {
      int prev = next;

      // Find start of $xxx$ sequence.
      next = format.indexOf ('$', prev);
      if (next == -1)
      {
        result.append (format.substring (prev, format.length ()));
        break;
      }

      // Save text before $xxx$ sequence, if there is any
      if (next > prev)
        result.append (format.substring (prev, next));

      // Find end of $xxx$ sequence
      prev = next + 1;
      next = format.indexOf ('$', prev);
      if (next == -1)
        throw new InvalidParameterException ("Invalid key (missing closing $)");

      // If we have just $$ then output $, else we have $xxx$, lookup xxx
      if (next == prev)
      {
        result.append ("$");
      }
      else
      {
        result.append (getParameter (format.substring (prev, next)));
      }
    }

    return result.toString ();
  }
}
