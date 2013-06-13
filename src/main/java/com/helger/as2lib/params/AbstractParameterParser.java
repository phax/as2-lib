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

import java.util.List;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.phloc.commons.string.StringHelper;

public abstract class AbstractParameterParser
{
  public abstract void setParameter (String sKey, String sValue) throws InvalidParameterException;

  public abstract String getParameter (String sKey) throws InvalidParameterException;

  /**
   * Set parameters from a string, like
   * "msg.sender.as2_id=ME,msg.headers.content-type=application/X12"
   * 
   * @param sEncodedParams
   *        string to parse
   * @throws InvalidParameterException
   */
  public void setParameters (@Nonnull final String sEncodedParams) throws InvalidParameterException
  {
    final StringTokenizer aParams = new StringTokenizer (sEncodedParams, "=,", false);
    while (aParams.hasMoreTokens ())
    {
      final String sKey = aParams.nextToken ().trim ();
      if (!aParams.hasMoreTokens ())
        throw new InvalidParameterException ("Invalid value", this, sKey, null);

      final String sValue = aParams.nextToken ();
      setParameter (sKey, sValue);
    }
  }

  /**
   * Set parameters from a string separated by delimiters.
   * 
   * @param sFormat
   *        Comma separated list of parameters to set, like
   *        <code>msg.sender.as2_id,msg.receiver.as2_id,msg.header.content-type</code>
   * @param sDelimiters
   *        delimiters in string to parse, like "-."
   * @param sValue
   *        string to parse, like <code>"NORINCO-WALMART.application/X12"</code>
   * @throws OpenAS2Exception
   */
  public void setParameters (@Nullable final String sFormat,
                             @Nullable final String sDelimiters,
                             @Nonnull final String sValue) throws OpenAS2Exception
  {
    final List <String> aKeys = StringHelper.getExploded (',', sFormat);

    final StringTokenizer aValueTokens = new StringTokenizer (sValue, sDelimiters, false);
    for (final String sKey : aKeys)
    {
      if (!aValueTokens.hasMoreTokens ())
        throw new OpenAS2Exception ("Invalid value: Format=" + sFormat + ", value=" + sValue);

      if (sKey.length () > 0)
        setParameter (sKey, aValueTokens.nextToken ());
    }
  }

  /**
   * Static way (why?) of getting at format method.
   * 
   * @param sFormat
   *        the format to fill in
   * @param aParser
   *        the place to get the parsed info
   * @return the filled in format
   * @throws InvalidParameterException
   */
  public static String parse (@Nonnull final String sFormat, @Nonnull final AbstractParameterParser aParser) throws InvalidParameterException
  {
    return aParser.format (sFormat);
  }

  /**
   * Fill in a format string with information from a ParameterParser
   * 
   * @param sFormat
   *        the format string to fill in
   * @return the filled in format string.
   * @throws InvalidParameterException
   */
  @Nonnull
  public String format (@Nonnull final String sFormat) throws InvalidParameterException
  {
    final StringBuilder aResult = new StringBuilder ();
    for (int nNext = 0; nNext < sFormat.length (); ++nNext)
    {
      int nPrev = nNext;

      // Find start of $xxx$ sequence.
      nNext = sFormat.indexOf ('$', nPrev);
      if (nNext == -1)
      {
        aResult.append (sFormat.substring (nPrev, sFormat.length ()));
        break;
      }

      // Save text before $xxx$ sequence, if there is any
      if (nNext > nPrev)
        aResult.append (sFormat.substring (nPrev, nNext));

      // Find end of $xxx$ sequence
      nPrev = nNext + 1;
      nNext = sFormat.indexOf ('$', nPrev);
      if (nNext == -1)
        throw new InvalidParameterException ("Invalid key (missing closing $)");

      // If we have just $$ then output $, else we have $xxx$, lookup xxx
      if (nNext == nPrev)
        aResult.append ("$");
      else
        aResult.append (getParameter (sFormat.substring (nPrev, nNext)));
    }

    return aResult.toString ();
  }
}
