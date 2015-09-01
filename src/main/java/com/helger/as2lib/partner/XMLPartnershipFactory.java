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
package com.helger.as2lib.partner;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.params.InvalidParameterException;
import com.helger.as2lib.session.IAS2Session;
import com.helger.as2lib.util.IOHelper;
import com.helger.as2lib.util.IStringMap;
import com.helger.as2lib.util.StringMap;
import com.helger.as2lib.util.XMLHelper;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.microdom.IMicroDocument;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.MicroDocument;
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
  private static final String PARTNER_NAME = PartnerMap.PARTNER_NAME;
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
  public void initDynamicComponent (@Nonnull final IAS2Session session,
                                    @Nullable final IStringMap parameters) throws OpenAS2Exception
  {
    super.initDynamicComponent (session, parameters);

    refresh ();
  }

  public void refresh () throws OpenAS2Exception
  {
    try
    {
      load (FileHelper.getInputStream (getFilename ()));
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
            final Partnership aNewPartnership = loadPartnership (eRootNode, aNewPartners);
            if (aNewPartnerships.getPartnershipByName (aNewPartnership.getName ()) != null)
              throw new OpenAS2Exception ("Partnership with name '" +
                                          aNewPartnership.getName () +
                                          "' is defined more than once");
            aNewPartnerships.addPartnership (aNewPartnership);
          }
          else
            s_aLogger.warn ("Invalid element '" + sNodeName + "' in XML partnership file");
      }
    }

    setPartners (aNewPartners);
    setPartnerships (aNewPartnerships);
  }

  protected void loadPartnershipAttributes (@Nonnull final IMicroElement aNode,
                                 @Nonnull final Partnership aPartnership) throws OpenAS2Exception
  {
    final String sNodeName = "attribute";
    final String sNodeKeyName = "name";
    final String sNodeValueName = "value";
    final Map <String, String> aAttributes = XMLHelper.mapAttributeNodes (aNode,
                                                                          sNodeName,
                                                                          sNodeKeyName,
                                                                          sNodeValueName);
    aPartnership.addAllAttributes (aAttributes);
  }

  @Nonnull
  public StringMap loadPartner (@Nonnull final IMicroElement ePartner) throws OpenAS2Exception
  {
    // Name is required
    return XMLHelper.getAllAttrsWithLowercaseNameWithRequired (ePartner, PARTNER_NAME);
  }

  protected void loadPartnerIDs (@Nonnull final IMicroElement ePartnership,
                                 @Nonnull final IPartnerMap aAllPartners,
                                 @Nonnull final Partnership aPartnership,
                                 final boolean bIsSender) throws OpenAS2Exception
  {
    final String sPartnerType = bIsSender ? "sender" : "receiver";
    final IMicroElement ePartner = ePartnership.getFirstChildElement (sPartnerType);
    if (ePartner == null)
      throw new OpenAS2Exception ("Partnership '" +
                                  aPartnership.getName () +
                                  "' is missing '" +
                                  sPartnerType +
                                  "' child element");

    final IStringMap aPartnerAttrs = XMLHelper.getAllAttrsWithLowercaseName (ePartner);

    // check for a partner name, and look up in partners list if one is found
    final String sPartnerName = aPartnerAttrs.getAttributeAsString (PARTNER_NAME);
    if (sPartnerName != null)
    {
      // Resolve name from existing partners
      final IStringMap aPartner = aAllPartners.getPartnerOfName (sPartnerName);
      if (aPartner == null)
      {
        throw new OpenAS2Exception ("Partnership '" +
                                    aPartnership.getName () +
                                    "' has a non-existing " +
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

    // copy all other attributes to the partner id map - overwrite the ones
    // present in the partner element
    if (bIsSender)
      aPartnership.addSenderIDs (aPartnerAttrs.getAllAttributes ());
    else
      aPartnership.addReceiverIDs (aPartnerAttrs.getAllAttributes ());
  }

  @Nonnull
  public Partnership loadPartnership (@Nonnull final IMicroElement ePartnership,
                                      @Nonnull final IPartnerMap aAllPartners) throws OpenAS2Exception
  {
    // Name attribute is required
    final IStringMap aPartnershipAttrs = XMLHelper.getAllAttrsWithLowercaseNameWithRequired (ePartnership,
                                                                                             PARTNER_NAME);

    final Partnership aPartnership = new Partnership (aPartnershipAttrs.getAttributeAsString (PARTNER_NAME));

    // load the sender and receiver information
    loadPartnerIDs (ePartnership, aAllPartners, aPartnership, true);
    loadPartnerIDs (ePartnership, aAllPartners, aPartnership, false);

    // read in the partnership attributes
    loadPartnershipAttributes (ePartnership, aPartnership);

    return aPartnership;
  }

  /**
   * Store the current status of the partnerships to a file.
   *
   * @throws OpenAS2Exception
   *         In case of an error
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

      final File aSourceFile = new File (sFilename);
      IOHelper.getFileOperationManager ().renameFile (aSourceFile, aBackupFile);
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
                    .setAttribute ("name", aAttr.getKey ())
                    .setAttribute ("value", aAttr.getValue ());
    }
    if (MicroWriter.writeToFile (aDoc, new File (sFilename)).isFailure ())
      throw new OpenAS2Exception ("Failed to write to file " + sFilename);
  }
}
