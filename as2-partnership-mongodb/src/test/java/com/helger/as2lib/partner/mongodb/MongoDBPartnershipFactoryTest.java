/**
 * Copyright (C) 2015-2020 jochenberger & Philip Helger
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
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

public class MongoDBPartnershipFactoryTest {
  private static final Logger LOGGER = LoggerFactory.getLogger (MongoDBPartnershipFactoryTest.class);

  private static MongodExecutable mongodExecutable;
  private static MongoClient mongo;
  private static MongoDBPartnershipFactory mongoDBPartnershipFactory;
  private static MongoDatabase database;
  private static MongoCollection <Document> collection;

  @BeforeClass
  public static void setupSpec () throws IOException {
    final MongodStarter starter = MongodStarter.getDefaultInstance ();
    final int port = 12345;
    final IMongodConfig mongodConfig = new MongodConfigBuilder ().version (Version.Main.PRODUCTION)
                                                                 .net (new Net (port, Network.localhostIsIPv6 ()))
                                                                 .build ();
    mongodExecutable = starter.prepare (mongodConfig);
    mongodExecutable.start ();
    mongo = MongoClients.create (MongoClientSettings.builder ()
                                                    .applyConnectionString (new ConnectionString ("mongodb://localhost:" +
                                                                                                  port))
                                                    .build ());
    database = mongo.getDatabase ("as2-lib-test");
    collection = database.getCollection ("partnerships");
    mongoDBPartnershipFactory = new MongoDBPartnershipFactory (collection, LOGGER);
    database.drop ();
  }

  @AfterClass
  public static void cleanupSpec () {
    if (mongo != null)
      mongo.close ();
    if (mongodExecutable != null)
      mongodExecutable.stop ();
  }

  @Test
  public void testAddPartnership () throws AS2Exception {
    final Partnership partnership = new Partnership ("Test partnership");
    assertTrue (mongoDBPartnershipFactory.addPartnership (partnership).isChanged ());
    assertEquals (1, collection.countDocuments ());
    assertNotNull (mongoDBPartnershipFactory.getPartnershipByName ("Test partnership"));
  }
}
