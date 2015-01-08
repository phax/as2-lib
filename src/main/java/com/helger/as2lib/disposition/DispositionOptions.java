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
package com.helger.as2lib.disposition;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.crypto.ECryptoAlgorithm;
import com.helger.as2lib.crypto.ECryptoAlgorithmMode;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.commons.IHasStringRepresentation;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * Parser and domain object for disposition options.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class DispositionOptions implements IHasStringRepresentation
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

  private String m_sMICAlg;
  private String m_sMICAlgImportance;
  private String m_sProtocol;
  private String m_sProtocolImportance;

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
   * Set the MIC algorithm
   *
   * @param sMICAlg
   *        The MIC algorithm. May be <code>null</code>.
   * @return this
   */
  @Nonnull
  public DispositionOptions setMICAlg (@Nullable final String sMICAlg)
  {
    m_sMICAlg = sMICAlg;
    return this;
  }

  /**
   * Set the MIC algorithm
   *
   * @param eMICAlg
   *        The digesting MIC algorithm. May be <code>null</code>.
   * @return this
   */
  @Nonnull
  public DispositionOptions setMICAlg (@Nullable final ECryptoAlgorithm eMICAlg)
  {
    if (eMICAlg != null && eMICAlg.getCryptAlgorithmMode () != ECryptoAlgorithmMode.DIGEST)
      throw new IllegalArgumentException ("Only digesting algorithms can be used here. " + eMICAlg + " is invalid!");
    return setMICAlg (eMICAlg == null ? null : eMICAlg.getID ());
  }

  /**
   * @return The MIC algorithm. May be <code>null</code>.
   */
  @Nullable
  public String getMICAlg ()
  {
    return m_sMICAlg;
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
    if (StringHelper.hasText (m_sMICAlgImportance) && StringHelper.hasText (m_sMICAlg))
    {
      if (aSB.length () > 0)
        aSB.append ("; ");
      aSB.append (SIGNED_RECEIPT_MICALG).append ('=').append (m_sMICAlgImportance).append (", ").append (m_sMICAlg);
    }

    return aSB.toString ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("protocolImportance", m_sProtocolImportance)
                                       .append ("protocol", m_sProtocol)
                                       .append ("MICAlgImportance", m_sMICAlgImportance)
                                       .append ("MICAlg", m_sMICAlg)
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
            final String [] aValues = StringHelper.getExplodedArray (',', aParts[1].trim ());
            if (aValues.length >= 2)
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
