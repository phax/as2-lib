/*
 * Copyright (C) 2015-2025 jochenberger & Philip Helger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.as2lib.partner.mongodb;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bson.Document;
import org.slf4j.Logger;

import com.helger.as2lib.AbstractDynamicComponent;
import com.helger.as2lib.exception.AS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.params.MessageParameters;
import com.helger.as2lib.partner.AS2PartnershipNotFoundException;
import com.helger.as2lib.partner.CPartnershipIDs;
import com.helger.as2lib.partner.IPartnershipFactory;
import com.helger.as2lib.partner.Partnership;
import com.helger.commons.annotation.CodingStyleguideUnaware;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.attr.IStringMap;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.state.EChange;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.result.DeleteResult;

/**
 * MongoDB based implementation of {@link IPartnershipFactory}
 *
 * @author jochenberger
 */
@CodingStyleguideUnaware
public class MongoDBPartnershipFactory extends AbstractDynamicComponent implements IPartnershipFactory
{
  private static final String NAME_KEY = "name";
  private static final String RECEIVER_IDS = "receiver-ids";
  private static final String SENDER_IDS = "sender-ids";
  private static final String ATTRIBUTES = "attributes";

  private final MongoCollection <Document> m_aPartnerships;
  private final Logger m_aLogger;

  public MongoDBPartnershipFactory (@Nonnull final MongoCollection <Document> aPartnerships,
                                    @Nonnull final Logger aLogger)
  {
    m_aLogger = aLogger;
    aPartnerships.createIndex (new Document (NAME_KEY, Integer.valueOf (1)), new IndexOptions ().unique (true));
    m_aPartnerships = aPartnerships;
  }

  @Nonnull
  private static Document _toBson (@Nonnull final IStringMap aStringMap)
  {
    final Document ret = new Document ();
    for (final Map.Entry <String, String> aEntry : aStringMap.entrySet ())
      ret.put (aEntry.getKey (), aEntry.getValue ());
    return ret;
  }

  @Nonnull
  private static Document _toBson (@Nonnull final Partnership aPartnership)
  {
    final Document ret = new Document ();
    ret.put (NAME_KEY, aPartnership.getName ());
    ret.put (RECEIVER_IDS, _toBson (aPartnership.getAllReceiverIDs ()));
    ret.put (SENDER_IDS, _toBson (aPartnership.getAllSenderIDs ()));
    ret.put (ATTRIBUTES, _toBson (aPartnership.getAllAttributes ()));
    return ret;
  }

  @Nonnull
  public EChange addPartnership (@Nonnull final Partnership aPartnership) throws AS2Exception
  {
    m_aPartnerships.insertOne (_toBson (aPartnership));
    return EChange.CHANGED;
  }

  @Nonnull
  public EChange removePartnership (@Nonnull final Partnership aPartnership) throws AS2Exception
  {
    final DeleteResult aDeleteResult = m_aPartnerships.deleteOne (new Document (NAME_KEY, aPartnership.getName ()));
    if (aDeleteResult.getDeletedCount () >= 1L)
      return EChange.CHANGED;

    return EChange.UNCHANGED;
  }

  public void updatePartnership (@Nonnull final IMessage aMsg, final boolean bOverwrite) throws AS2Exception
  {
    // Fill in any available partnership information
    final Partnership aPartnership = getPartnership (aMsg.partnership ());

    if (m_aLogger.isDebugEnabled ())
      m_aLogger.debug ("Updating partnership {}", aPartnership);

    // Update partnership data of message with the stored ones
    aMsg.partnership ().copyFrom (aPartnership);

    // Set attributes
    if (bOverwrite)
    {
      final String sSubject = aPartnership.getAttribute (CPartnershipIDs.PA_SUBJECT);
      if (sSubject != null)
      {
        aMsg.setSubject (new MessageParameters (aMsg).format (sSubject));
      }
    }
  }

  public void updatePartnership (@Nonnull final IMessageMDN aMdn, final boolean bOverwrite) throws AS2Exception
  {
    final Partnership aPartnership = getPartnership (aMdn.partnership ());
    aMdn.partnership ().copyFrom (aPartnership);
  }

  @Nullable
  private Partnership _getPartnershipByID (final IStringMap aAllSenderIDs, final IStringMap aAllReceiverIDs)
  {
    Document aFilter = new Document ();
    for (final Map.Entry <String, String> aEntry : aAllSenderIDs.entrySet ())
      aFilter.append (SENDER_IDS + "." + aEntry.getKey (), aEntry.getValue ());

    for (final Map.Entry <String, String> aEntry : aAllReceiverIDs.entrySet ())
      aFilter.append (RECEIVER_IDS + "." + aEntry.getKey (), aEntry.getValue ());

    Partnership ret = m_aPartnerships.find (aFilter).map (MongoDBPartnershipFactory::_toPartnership).first ();
    if (ret != null)
      return ret;

    // try the other way around, maybe we're receiving a response
    // TODO is this really a good idea?
    aFilter = new Document ();
    for (final Map.Entry <String, String> entry : aAllSenderIDs.entrySet ())
      aFilter.append (RECEIVER_IDS + "." + entry.getKey (), entry.getValue ());
    for (final Map.Entry <String, String> entry : aAllReceiverIDs.entrySet ())
      aFilter.append (SENDER_IDS + "." + entry.getKey (), entry.getValue ());

    final Partnership aInverseResult = m_aPartnerships.find (aFilter)
                                                      .map (MongoDBPartnershipFactory::_toPartnership)
                                                      .first ();
    if (aInverseResult != null)
    {
      // Create an inverse partnership
      ret = new Partnership (aInverseResult.getName () + "-inverse");
      ret.setReceiverX509Alias (aInverseResult.getSenderX509Alias ());
      ret.setReceiverAS2ID (aInverseResult.getSenderAS2ID ());
      ret.setSenderX509Alias (aInverseResult.getReceiverX509Alias ());
      ret.setSenderAS2ID (aInverseResult.getReceiverAS2ID ());
      return ret;
    }
    return null;
  }

  @Nonnull
  public Partnership getPartnership (@Nonnull final Partnership aPartnership) throws AS2Exception
  {
    Partnership aRealPartnership = getPartnershipByName (aPartnership.getName ());
    if (aRealPartnership == null)
    {
      // Found no partnership by name
      aRealPartnership = _getPartnershipByID (aPartnership.getAllSenderIDs (), aPartnership.getAllReceiverIDs ());
    }

    if (aRealPartnership == null)
      throw new AS2PartnershipNotFoundException (aPartnership);

    return aRealPartnership;
  }

  @Nullable
  public Partnership getPartnershipByName (@Nullable final String sName)
  {
    if (sName == null)
      return null;
    return m_aPartnerships.find (new Document (NAME_KEY, sName))
                          .map (MongoDBPartnershipFactory::_toPartnership)
                          .first ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsSet <String> getAllPartnershipNames ()
  {
    return m_aPartnerships.distinct (NAME_KEY, String.class).into (new CommonsHashSet <> ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <Partnership> getAllPartnerships ()
  {
    return m_aPartnerships.find ().map (MongoDBPartnershipFactory::_toPartnership).into (new CommonsArrayList <> ());
  }

  @Nonnull
  private static Partnership _toPartnership (@Nonnull final Document aBson)
  {
    final Partnership ret = new Partnership (aBson.getString (NAME_KEY));
    final Document aBsonSenderIDs = (Document) aBson.get (SENDER_IDS);
    final ICommonsMap <String, String> aSenderIDs = new CommonsHashMap <> (aBsonSenderIDs.size ());
    for (final Map.Entry <String, Object> aEntry : aBsonSenderIDs.entrySet ())
      aSenderIDs.put (aEntry.getKey (), aEntry.getValue ().toString ());
    ret.addSenderIDs (aSenderIDs);

    final Document aBsonReceiverIDs = (Document) aBson.get (RECEIVER_IDS);
    final ICommonsMap <String, String> aReceiverIDs = new CommonsHashMap <> (aBsonReceiverIDs.size ());
    for (final Map.Entry <String, Object> aEntry : aBsonReceiverIDs.entrySet ())
      aReceiverIDs.put (aEntry.getKey (), aEntry.getValue ().toString ());
    ret.addReceiverIDs (aReceiverIDs);

    final Document aBsonAttributes = (Document) aBson.get (ATTRIBUTES);
    if (aBsonAttributes != null)
    {
      final ICommonsMap <String, String> aAttrs = new CommonsHashMap <> (aBsonReceiverIDs.size ());
      for (final Map.Entry <String, Object> aEntry : aBsonAttributes.entrySet ())
        aAttrs.put (aEntry.getKey (), aEntry.getValue ().toString ());
      ret.addAllAttributes (aAttrs);
    }
    return ret;
  }
}
