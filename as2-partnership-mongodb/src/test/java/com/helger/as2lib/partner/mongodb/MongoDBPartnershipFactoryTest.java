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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.bson.Document;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.AS2Exception;
import com.helger.as2lib.partner.Partnership;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ImmutableMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

public class MongoDBPartnershipFactoryTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MongoDBPartnershipFactoryTest.class);

  private static MongodExecutable s_aMongodExecutable;
  private static MongoClient s_aMongoClient;
  private static MongoDatabase s_aDatabase;
  private static MongoCollection <Document> s_aCollection;
  private static MongoDBPartnershipFactory s_aPartnershipFactory;

  @BeforeClass
  public static void setupSpec () throws IOException
  {
    final MongodStarter starter = MongodStarter.getDefaultInstance ();
    final int nPort = 12345;
    final MongodConfig mongodConfig = ImmutableMongodConfig.builder ()
                                                           .version (Version.Main.V4_4)
                                                           .net (new Net (nPort, Network.localhostIsIPv6 ()))
                                                           .build ();
    s_aMongodExecutable = starter.prepare (mongodConfig);
    s_aMongodExecutable.start ();
    s_aMongoClient = MongoClients.create (MongoClientSettings.builder ()
                                                             .applyConnectionString (new ConnectionString ("mongodb://localhost:" + nPort))
                                                             .build ());
    s_aDatabase = s_aMongoClient.getDatabase ("as2-lib-test");
    s_aCollection = s_aDatabase.getCollection ("partnerships");
    s_aPartnershipFactory = new MongoDBPartnershipFactory (s_aCollection, LOGGER);
    s_aDatabase.drop ();
  }

  @AfterClass
  public static void cleanupSpec ()
  {
    if (s_aMongoClient != null)
      s_aMongoClient.close ();
    if (s_aMongodExecutable != null)
      s_aMongodExecutable.stop ();
  }

  @Test
  public void testAddPartnership () throws AS2Exception
  {
    final Partnership partnership = new Partnership ("Test partnership");
    assertTrue (s_aPartnershipFactory.addPartnership (partnership).isChanged ());
    assertEquals (1, s_aCollection.countDocuments ());
    assertNotNull (s_aPartnershipFactory.getPartnershipByName ("Test partnership"));
  }
}
