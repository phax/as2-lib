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

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.partner.Partnership;
import com.mongodb.MongoClient;
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
    mongo = new MongoClient ("localhost", port);
    database = mongo.getDatabase ("test");
    collection = database.getCollection ("test");
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
  public void testAddPartnership () throws OpenAS2Exception {
    final Partnership partnership = new Partnership ("Test partnership");
    assertTrue (mongoDBPartnershipFactory.addPartnership (partnership).isChanged ());
    assertEquals (1, collection.countDocuments ());
    assertNotNull (mongoDBPartnershipFactory.getPartnershipByName ("Test partnership"));
  }
}
