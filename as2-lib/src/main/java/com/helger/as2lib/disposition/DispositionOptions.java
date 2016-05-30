/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2016 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.disposition;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * Parser and domain object for disposition options. This is usually used in the
 * HTTP header "Disposition-Notification-Options".
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class DispositionOptions
{
  /** Protocol attribute */
  public static final String SIGNED_RECEIPT_PROTOCOL = "signed-receipt-protocol";
  /** MicAlg attribute */
  public static final String SIGNED_RECEIPT_MICALG = "signed-receipt-micalg";

  public static final String IMPORTANCE_REQUIRED = "required";
  public static final String IMPORTANCE_OPTIONAL = "optional";

  /** Default protocol value */
  public static final String PROTOCOL_PKCS7_SIGNATURE = "pkcs7-signature";

  private static final Logger s_aLogger = LoggerFactory.getLogger (DispositionOptions.class);

  private String m_sProtocolImportance;
  private String m_sProtocol;
  private String m_sMICAlgImportance;
  private final ICommonsList <ECryptoAlgorithmSign> m_aMICAlgs = new CommonsArrayList<> ();

  public DispositionOptions ()
  {}

  /**
   * Check if the passed importance value is a standard one (<code>null</code>,
   * "optional" or "required").
   *
   * @param sImportance
   *        The value to be checked.
   */
  private static void _checkImportance (@Nullable final String sImportance)
  {
    if (sImportance != null && !sImportance.equals (IMPORTANCE_REQUIRED) && !sImportance.equals (IMPORTANCE_OPTIONAL))
      s_aLogger.warn ("Non-standard importance value '" + sImportance + "' used!");
  }

  /**
   * Set the protocol importance.
   *
   * @param sProtocolImportance
   *        The importance to set. May be <code>null</code>.
   * @return this
   */
  @Nonnull
  public DispositionOptions setProtocolImportance (@Nullable final String sProtocolImportance)
  {
    _checkImportance (sProtocolImportance);
    m_sProtocolImportance = sProtocolImportance;
    return this;
  }

  /**
   * @return the protocol importance (<code>null</code> or "required" or
   *         "optional"). May be <code>null</code>.
   */
  @Nullable
  public String getProtocolImportance ()
  {
    return m_sProtocolImportance;
  }

  public boolean isProtocolRequired ()
  {
    return IMPORTANCE_REQUIRED.equals (m_sProtocolImportance);
  }

  public boolean isProtocolOptional ()
  {
    return IMPORTANCE_OPTIONAL.equals (m_sProtocolImportance);
  }

  /**
   * Set the protocol
   *
   * @param sProtocol
   *        The protocol name (e.g. "pkcs7-signature"). May be <code>null</code>
   *        .
   * @return this
   */
  @Nonnull
  public DispositionOptions setProtocol (@Nullable final String sProtocol)
  {
    m_sProtocol = sProtocol;
    return this;
  }

  /**
   * @return The protocol. Currently only "pkcs7-signature" or <code>null</code>
   *         is supported.
   */
  @Nullable
  public String getProtocol ()
  {
    return m_sProtocol;
  }

  /**
   * Set the MIC algorithm importance
   *
   * @param sMICAlgImportance
   *        The importance. May be <code>null</code>.
   * @return this
   */
  @Nonnull
  public DispositionOptions setMICAlgImportance (@Nullable final String sMICAlgImportance)
  {
    _checkImportance (sMICAlgImportance);
    m_sMICAlgImportance = sMICAlgImportance;
    return this;
  }

  /**
   * @return the MIC algorithm importance (<code>null</code> or "required" or
   *         "optional").
   */
  @Nullable
  public String getMICAlgImportance ()
  {
    return m_sMICAlgImportance;
  }

  public boolean isMICAlgRequired ()
  {
    return IMPORTANCE_REQUIRED.equals (m_sMICAlgImportance);
  }

  public boolean isMICAlgOptional ()
  {
    return IMPORTANCE_OPTIONAL.equals (m_sMICAlgImportance);
  }

  /**
   * Set the MIC algorithm(s) to use. The passed string is parsed as a comma
   * separated list. This overwrites all existing MIC algorithms. If any of the
   * contained MIC algorithms is not supported by this library, a log message is
   * emitted but no Exception is thrown.
   *
   * @param sMICAlgs
   *        The MIC algorithm(s). May be <code>null</code>.
   * @return this
   */
  @Nonnull
  public DispositionOptions setMICAlg (@Nullable final String sMICAlgs)
  {
    m_aMICAlgs.clear ();
    if (StringHelper.hasText (sMICAlgs))
    {
      final List <String> aMICAlgs = StringHelper.getExploded (',', sMICAlgs.trim ());
      for (final String sMICAlg : aMICAlgs)
      {
        // trim and lowercase
        final String sRealMICAlg = sMICAlg.trim ().toLowerCase (Locale.US);

        final ECryptoAlgorithmSign eMICAlg = ECryptoAlgorithmSign.getFromIDOrNull (sRealMICAlg);
        if (eMICAlg == null)
        {
          // Ignore all unsupported MIC algorithms and continue
          s_aLogger.warn ("The passed MIC algorithm '" + sRealMICAlg + "' is unsupported!");
        }
        else
        {
          m_aMICAlgs.add (eMICAlg);
        }
      }
    }
    return this;
  }

  /**
   * Set the MIC algorithm to use. This overwrites all existing MIC algorithms.
   *
   * @param aMICAlgs
   *        The digesting MIC algorithm(s). May be <code>null</code>.
   * @return this
   */
  @Nonnull
  public DispositionOptions setMICAlg (@Nullable final ECryptoAlgorithmSign... aMICAlgs)
  {
    m_aMICAlgs.clear ();
    if (aMICAlgs != null)
      for (final ECryptoAlgorithmSign eMICAlg : aMICAlgs)
        if (eMICAlg != null)
          m_aMICAlgs.add (eMICAlg);
    return this;
  }

  /**
   * Set the MIC algorithm to use. This overwrites all existing MIC algorithms.
   *
   * @param aMICAlgs
   *        The digesting MIC algorithm(s). May be <code>null</code>.
   * @return this
   */
  @Nonnull
  public DispositionOptions setMICAlg (@Nullable final Iterable <? extends ECryptoAlgorithmSign> aMICAlgs)
  {
    m_aMICAlgs.clear ();
    if (aMICAlgs != null)
      for (final ECryptoAlgorithmSign eMICAlg : aMICAlgs)
        if (eMICAlg != null)
          m_aMICAlgs.add (eMICAlg);
    return this;
  }

  /**
   * @return All MIC algorithms contained. Never <code>null</code> but maybe
   *         empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  public List <ECryptoAlgorithmSign> getAllMICAlgs ()
  {
    return CollectionHelper.newList (m_aMICAlgs);
  }

  /**
   * @return The first MIC algorithm contained in the list. May be
   *         <code>null</code> if no MIC algorithm is set.
   */
  @Nullable
  public ECryptoAlgorithmSign getFirstMICAlg ()
  {
    return CollectionHelper.getFirstElement (m_aMICAlgs);
  }

  /**
   * @return The number of contained MIC algorithms. Always &ge; 0.
   */
  @Nonnegative
  public int getMICAlgCount ()
  {
    return m_aMICAlgs.size ();
  }

  /**
   * @return <code>true</code> if at least one MIC algorithm is present,
   *         <code>false</code> if none is present.
   */
  public boolean hasMICAlg ()
  {
    return !m_aMICAlgs.isEmpty ();
  }

  /**
   * @return The MIC algorithm(s) as a comma delimited string. May be
   *         <code>null</code>.
   */
  @Nullable
  public String getMICAlgAsString ()
  {
    if (m_aMICAlgs.isEmpty ())
      return null;

    final StringBuilder aSB = new StringBuilder ();
    for (final ECryptoAlgorithmSign eMICAlg : m_aMICAlgs)
    {
      if (aSB.length () > 0)
        aSB.append (", ");
      aSB.append (eMICAlg.getID ());
    }
    return aSB.toString ();
  }

  @Nonnull
  public String getAsString ()
  {
    final StringBuilder aSB = new StringBuilder ();
    if (StringHelper.hasText (m_sProtocolImportance) && StringHelper.hasText (m_sProtocol))
    {
      aSB.append (SIGNED_RECEIPT_PROTOCOL)
         .append ('=')
         .append (m_sProtocolImportance)
         .append (", ")
         .append (m_sProtocol);
    }
    if (StringHelper.hasText (m_sMICAlgImportance) && !m_aMICAlgs.isEmpty ())
    {
      if (aSB.length () > 0)
        aSB.append ("; ");
      aSB.append (SIGNED_RECEIPT_MICALG)
         .append ('=')
         .append (m_sMICAlgImportance)
         .append (", ")
         .append (getMICAlgAsString ());
    }

    return aSB.toString ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ProtocolImportance", m_sProtocolImportance)
                                       .append ("Protocol", m_sProtocol)
                                       .append ("MICAlgImportance", m_sMICAlgImportance)
                                       .append ("MICAlgs", m_aMICAlgs)
                                       .toString ();
  }

  /**
   * Parse Strings like <code>signed-receipt-protocol=optional, pkcs7-signature;
   * signed-receipt-micalg=optional, sha1</code>
   *
   * @param sOptions
   *        The string to parse. May be <code>null</code> in which case an empty
   *        object will be returned.
   * @return Never <code>null</code>.
   * @throws OpenAS2Exception
   *         In the very unlikely case of a programming error in
   *         {@link StringTokenizer}.
   */
  @Nonnull
  public static DispositionOptions createFromString (@Nullable final String sOptions) throws OpenAS2Exception
  {
    final DispositionOptions ret = new DispositionOptions ();
    if (StringHelper.hasTextAfterTrim (sOptions))
    {
      try
      {
        // Split options into parameters by ";"
        for (final String sParameter : StringHelper.getExplodedArray (';', sOptions.trim ()))
        {
          // Split parameter into name and value by "="
          final String [] aParts = StringHelper.getExplodedArray ('=', sParameter.trim (), 2);
          if (aParts.length == 2)
          {
            final String sAttribute = aParts[0].trim ();

            // Split the value into importance and the main values by ","
            final String [] aValues = StringHelper.getExplodedArray (',', aParts[1].trim (), 2);
            if (aValues.length == 2)
            {
              if (sAttribute.equalsIgnoreCase (SIGNED_RECEIPT_PROTOCOL))
              {
                ret.setProtocolImportance (aValues[0].trim ());
                ret.setProtocol (aValues[1].trim ());
              }
              else
                if (sAttribute.equalsIgnoreCase (SIGNED_RECEIPT_MICALG))
                {
                  ret.setMICAlgImportance (aValues[0].trim ());
                  ret.setMICAlg (aValues[1].trim ());
                }
                else
                  s_aLogger.warn ("Unsupported disposition attribute '" +
                                  sAttribute +
                                  "' with value '" +
                                  aParts[1].trim () +
                                  "' found!");
            }
            else
              s_aLogger.warn ("Failed to split disposition options parameter '" +
                              sParameter +
                              "' value '" +
                              aParts[1].trim () +
                              "' into importance and values");
          }
          else
            s_aLogger.warn ("Failed to split disposition options parameter '" +
                            sParameter +
                            "' into attribute and values");
        }
      }
      catch (final NoSuchElementException ex)
      {
        throw new OpenAS2Exception ("Invalid disposition options format: " + sOptions);
      }
    }
    return ret;
  }
}
