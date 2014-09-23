/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2014 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.partner;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.InvalidParameterException;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.session.ISession;
import com.helger.as2lib.util.IStringMap;
import com.helger.as2lib.util.StringMap;
import com.helger.as2lib.util.XMLUtil;
import com.helger.commons.io.file.FileOperations;
import com.helger.commons.io.file.FileUtils;
import com.helger.commons.microdom.IMicroDocument;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.impl.MicroDocument;
import com.helger.commons.microdom.serialize.MicroReader;
import com.helger.commons.microdom.serialize.MicroWriter;
import com.helger.commons.string.StringHelper;

/**
 * original author unknown this release added logic to store partnerships and
 * provide methods for partner/partnership command line processor
 *
 * @author joseph mcverry
 */
public class XMLPartnershipFactory extends AbstractPartnershipFactory
{
  public static final String ATTR_FILENAME = "filename";
  public static final String ATTR_DISABLE_BACKUP = "disablebackup";
  private static final String PARTNER_NAME = "name";
  private static final Logger s_aLogger = LoggerFactory.getLogger (XMLPartnershipFactory.class);

  public void setFilename (final String filename)
  {
    setAttribute (ATTR_FILENAME, filename);
  }

  public String getFilename () throws InvalidParameterException
  {
    return getAttributeAsStringRequired (ATTR_FILENAME);
  }

  @Override
  public void initDynamicComponent (@Nonnull final ISession session, @Nullable final IStringMap parameters) throws OpenAS2Exception
  {
    super.initDynamicComponent (session, parameters);

    refresh ();
  }

  public void refresh () throws OpenAS2Exception
  {
    try
    {
      load (FileUtils.getInputStream (getFilename ()));
    }
    catch (final Exception ex)
    {
      throw WrappedOpenAS2Exception.wrap (ex);
    }
  }

  protected void load (@Nullable @WillClose final InputStream aIS) throws OpenAS2Exception
  {
    final PartnerMap aNewPartners = new PartnerMap ();
    final PartnershipMap aNewPartnerships = new PartnershipMap ();

    if (aIS != null)
    {
      final IMicroDocument aDocument = MicroReader.readMicroXML (aIS);
      final IMicroElement root = aDocument.getDocumentElement ();

      for (final IMicroElement eRootNode : root.getAllChildElements ())
      {
        final String sNodeName = eRootNode.getTagName ();

        if (sNodeName.equals ("partner"))
        {
          final StringMap aNewPartner = loadPartner (eRootNode);
          aNewPartners.addPartner (aNewPartner);
        }
        else
          if (sNodeName.equals ("partnership"))
          {
            final Partnership aNewPartnership = loadPartnership (eRootNode, aNewPartners, aNewPartnerships);
            aNewPartnerships.addPartnership (aNewPartnership);
          }
          else
            s_aLogger.warn ("Invalid element '" + sNodeName + "' in XML partnership file");
      }
    }

    setPartners (aNewPartners);
    setPartnerships (aNewPartnerships);
  }

  protected void loadAttributes (final IMicroElement node, final Partnership partnership) throws OpenAS2Exception
  {
    final IStringMap nodes = XMLUtil.mapAttributeNodes (node, "attribute", PARTNER_NAME, "value");
    partnership.addAllAttributes (nodes);
  }

  @Nonnull
  public StringMap loadPartner (@Nonnull final IMicroElement aElement) throws OpenAS2Exception
  {
    return XMLUtil.getAttrsWithLowercaseNameWithRequired (aElement, PARTNER_NAME);
  }

  protected void loadPartnerIDs (@Nonnull final IMicroElement aElement,
                                 @Nonnull final IPartnerMap aAllPartners,
                                 @Nonnull final Partnership aPartnership,
                                 final boolean bIsSender) throws OpenAS2Exception
  {
    final String sPartnerType = bIsSender ? "sender" : "receiver";
    final IMicroElement aPartnerNode = aElement.getFirstChildElement (sPartnerType);
    if (aPartnerNode == null)
      throw new OpenAS2Exception ("Partnership '" + aPartnership.getName () + "' is missing sender");

    final IStringMap aPartnerAttr = XMLUtil.getAttrsWithLowercaseName (aPartnerNode);

    // check for a partner name, and look up in partners list if one is found
    final String sPartnerName = aPartnerAttr.getAttributeAsString (PARTNER_NAME);
    if (sPartnerName != null)
    {
      final IStringMap aPartner = aAllPartners.getPartnerOfName (sPartnerName);
      if (aPartner == null)
      {
        throw new OpenAS2Exception ("Partnership '" +
                                    aPartnership.getName () +
                                    "' has an undefined " +
                                    sPartnerType +
                                    ": '" +
                                    sPartnerName +
                                    "'");
      }

      if (bIsSender)
        aPartnership.addSenderIDs (aPartner.getAllAttributes ());
      else
        aPartnership.addReceiverIDs (aPartner.getAllAttributes ());
    }

    // copy all other attributes to the partner id map
    if (bIsSender)
      aPartnership.addSenderIDs (aPartnerAttr.getAllAttributes ());
    else
      aPartnership.addReceiverIDs (aPartnerAttr.getAllAttributes ());
  }

  @Nonnull
  public Partnership loadPartnership (@Nonnull final IMicroElement aElement,
                                      @Nonnull final IPartnerMap aAllPartners,
                                      @Nonnull final IPartnershipMap aAllPartnerships) throws OpenAS2Exception
  {
    final IStringMap aPartnershipAttrs = XMLUtil.getAttrsWithLowercaseNameWithRequired (aElement, PARTNER_NAME);
    final String sPartnershipName = aPartnershipAttrs.getAttributeAsString (PARTNER_NAME);

    if (aAllPartnerships.getPartnershipByName (sPartnershipName) != null)
      throw new OpenAS2Exception ("Partnership is defined more than once: " + sPartnershipName);

    final Partnership aPartnership = new Partnership (sPartnershipName);

    // load the sender and receiver information
    loadPartnerIDs (aElement, aAllPartners, aPartnership, true);
    loadPartnerIDs (aElement, aAllPartners, aPartnership, false);

    // read in the partnership attributes
    loadAttributes (aElement, aPartnership);

    return aPartnership;
  }

  /**
   * Store the current status of the partnerships to a file.
   *
   * @throws OpenAS2Exception
   */
  public void storePartnership () throws OpenAS2Exception
  {
    final String sFilename = getFilename ();

    if (!containsAttribute (ATTR_DISABLE_BACKUP))
    {
      long nIndex = 0;
      File aBackupFile;
      do
      {
        aBackupFile = new File (sFilename + '.' + StringHelper.getLeadingZero (nIndex, 7));
        nIndex++;
      } while (aBackupFile.exists ());

      s_aLogger.info ("backing up " + sFilename + " to " + aBackupFile.getName ());

      final File fr = new File (sFilename);
      FileOperations.renameFile (fr, aBackupFile);
    }

    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement ePartnerships = aDoc.appendElement ("partnerships");
    for (final IStringMap aAttrs : getAllPartners ())
    {
      final IMicroElement ePartner = ePartnerships.appendElement ("partner");
      for (final Map.Entry <String, String> aAttr : aAttrs)
        ePartner.setAttribute (aAttr.getKey (), aAttr.getValue ());
    }

    for (final Partnership aPartnership : getAllPartnerships ())
    {
      final IMicroElement ePartnership = ePartnerships.appendElement ("partnership");
      ePartnership.setAttribute (PARTNER_NAME, aPartnership.getName ());

      final IMicroElement eSender = ePartnership.appendElement ("sender");
      for (final Map.Entry <String, String> aAttr : aPartnership.getAllSenderIDs ())
        eSender.setAttribute (aAttr.getKey (), aAttr.getValue ());

      final IMicroElement eReceiver = ePartnership.appendElement ("receiver");
      for (final Map.Entry <String, String> aAttr : aPartnership.getAllReceiverIDs ())
        eReceiver.setAttribute (aAttr.getKey (), aAttr.getValue ());

      for (final Map.Entry <String, String> aAttr : aPartnership.getAllAttributes ())
        ePartnership.appendElement ("attribute")
                    .setAttribute (PARTNER_NAME, aAttr.getKey ())
                    .setAttribute ("value", aAttr.getValue ());
    }
    if (MicroWriter.writeToFile (aDoc, new File (sFilename)).isFailure ())
      throw new OpenAS2Exception ("Failed to write to file " + sFilename);
  }
}
