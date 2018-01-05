/**
 * Copyright (C) 2015-2018 jochenberger & Philip Helger
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

import org.bson.Document;
import org.slf4j.Logger;

import com.helger.as2lib.AbstractDynamicComponent;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.params.MessageParameters;
import com.helger.as2lib.partner.CPartnershipIDs;
import com.helger.as2lib.partner.IPartnershipFactory;
import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.partner.PartnershipNotFoundException;
import com.helger.commons.annotation.CodingStyleguideUnaware;
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
public class MongoDBPartnershipFactory extends AbstractDynamicComponent implements IPartnershipFactory {
  private static final long serialVersionUID = -2282798646250446937L;
  private static final String NAME_KEY = "name";
  private static final String RECEIVER_IDS = "receiver-ids";
  private static final String SENDER_IDS = "sender-ids";
  private static final String ATTRIBUTES = "attributes";

  private final MongoCollection <Document> partnerships;
  private final Logger logger;

  public MongoDBPartnershipFactory (final MongoCollection <Document> partnerships, final Logger logger) {
    this.logger = logger;
    partnerships.createIndex (new Document (NAME_KEY, Integer.valueOf (1)), new IndexOptions ().unique (true));
    this.partnerships = partnerships;
  }

  @Override
  public EChange addPartnership (final Partnership aPartnership) throws OpenAS2Exception {
    partnerships.insertOne (toDocument (aPartnership));
    return EChange.CHANGED;
  }

  @Override
  public EChange removePartnership (final Partnership aPartnership) throws OpenAS2Exception {
    final DeleteResult result = partnerships.deleteOne (new Document (NAME_KEY, aPartnership.getName ()));
    if (result.getDeletedCount () >= 1l) {
      return EChange.CHANGED;
    }
    return EChange.UNCHANGED;
  }

  @Override
  public void updatePartnership (final IMessage aMsg, final boolean bOverwrite) throws OpenAS2Exception {
    // Fill in any available partnership information
    final Partnership aPartnership = getPartnership (aMsg.partnership ());

    logger.debug ("Updating partnership {}", aPartnership);

    // Update partnership data of message with the stored ones
    aMsg.partnership ().copyFrom (aPartnership);

    // Set attributes
    if (bOverwrite) {
      final String sSubject = aPartnership.getAttribute (CPartnershipIDs.PA_SUBJECT);
      if (sSubject != null) {
        aMsg.setSubject (new MessageParameters (aMsg).format (sSubject));
      }
    }
  }

  @Override
  public void updatePartnership (final IMessageMDN aMdn, final boolean bOverwrite) throws OpenAS2Exception {
    final Partnership aPartnership = getPartnership (aMdn.partnership ());
    aMdn.partnership ().copyFrom (aPartnership);
  }

  @Override
  public Partnership getPartnership (final Partnership aPartnership) throws OpenAS2Exception {
    Partnership aRealPartnership = getPartnershipByName (aPartnership.getName ());
    if (aRealPartnership == null) {
      // Found no partnership by name
      aRealPartnership = getPartnershipByID (aPartnership.getAllSenderIDs (), aPartnership.getAllReceiverIDs ());
    }

    if (aRealPartnership == null) {
      throw new PartnershipNotFoundException (aPartnership);
    }
    return aRealPartnership;
  }

  private Partnership getPartnershipByID (final IStringMap allSenderIDs, final IStringMap allReceiverIDs) {
    Document filter = new Document ();
    for (final Map.Entry <String, String> entry : allSenderIDs.entrySet ()) {
      filter.append (SENDER_IDS + "." + entry.getKey (), entry.getValue ());
    }
    for (final Map.Entry <String, String> entry : allReceiverIDs.entrySet ()) {
      filter.append (RECEIVER_IDS + "." + entry.getKey (), entry.getValue ());
    }

    Partnership result = partnerships.find (filter).map (MongoDBPartnershipFactory::toPartnership).first ();
    if (result != null) {
      return result;
    }

    // try the other way around, maybe we're receiving a response
    // TODO is this really a good idea?
    filter = new Document ();
    for (final Map.Entry <String, String> entry : allSenderIDs.entrySet ()) {
      filter.append (RECEIVER_IDS + "." + entry.getKey (), entry.getValue ());
    }
    for (final Map.Entry <String, String> entry : allReceiverIDs.entrySet ()) {
      filter.append (SENDER_IDS + "." + entry.getKey (), entry.getValue ());
    }

    final Partnership inverseResult = partnerships.find (filter)
                                                  .map (MongoDBPartnershipFactory::toPartnership)
                                                  .first ();
    if (inverseResult != null) {
      result = new Partnership (inverseResult.getName () + "-inverse");
      result.setReceiverX509Alias (inverseResult.getSenderX509Alias ());
      result.setReceiverAS2ID (inverseResult.getSenderAS2ID ());
      result.setSenderX509Alias (inverseResult.getReceiverX509Alias ());
      result.setSenderAS2ID (inverseResult.getReceiverAS2ID ());

      return result;
    }
    return null;

  }

  @Override
  public Partnership getPartnershipByName (final String sName) {
    return partnerships.find (new Document (NAME_KEY, sName)).map (MongoDBPartnershipFactory::toPartnership).first ();
  }

  @Override
  public ICommonsSet <String> getAllPartnershipNames () {
    return partnerships.distinct (NAME_KEY, String.class).into (new CommonsHashSet <> ());
  }

  @Override
  public ICommonsList <Partnership> getAllPartnerships () {
    return partnerships.find ().map (MongoDBPartnershipFactory::toPartnership).into (new CommonsArrayList <> ());
  }

  private static Document toDocument (final IStringMap stringMap) {
    final Document document = new Document ();
    for (final Map.Entry <String, String> entry : stringMap.entrySet ()) {
      document.put (entry.getKey (), entry.getValue ());
    }
    return document;
  }

  private static Document toDocument (final Partnership partnership) {
    final Document document = new Document ();
    document.put (NAME_KEY, partnership.getName ());
    document.put (RECEIVER_IDS, toDocument (partnership.getAllReceiverIDs ()));
    document.put (SENDER_IDS, toDocument (partnership.getAllSenderIDs ()));
    document.put (ATTRIBUTES, toDocument (partnership.getAllAttributes ()));

    return document;
  }

  private static Partnership toPartnership (final Document document) {
    final Partnership partnership = new Partnership (document.getString (NAME_KEY));
    final Document senderIDs = (Document) document.get (SENDER_IDS);
    final ICommonsMap <String, String> senders = new CommonsHashMap <> (senderIDs.size ());
    for (final Map.Entry <String, Object> e : senderIDs.entrySet ()) {
      senders.put (e.getKey (), e.getValue ().toString ());
    }
    partnership.addSenderIDs (senders);

    final Document receiverIDs = (Document) document.get (RECEIVER_IDS);
    final ICommonsMap <String, String> receivers = new CommonsHashMap <> (receiverIDs.size ());
    for (final Map.Entry <String, Object> e : receiverIDs.entrySet ()) {
      receivers.put (e.getKey (), e.getValue ().toString ());
    }
    partnership.addReceiverIDs (receivers);

    final Document attributes = (Document) document.get (ATTRIBUTES);
    if (attributes != null) {
      final ICommonsMap <String, String> att = new CommonsHashMap <> (receiverIDs.size ());
      for (final Map.Entry <String, Object> e : attributes.entrySet ()) {
        att.put (e.getKey (), e.getValue ().toString ());
      }
      partnership.addAllAttributes (att);
    }
    return partnership;
  }
}
