package com.helger.as2lib.partner.mongodb;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

public class EmbeddedMongoTest {
  private MongodExecutable mongodExecutable;
  private MongoClient mongo;

  // static {
  // System.setProperty ("http.proxyHost", "172.30.9.12");
  // System.setProperty ("http.proxyPort", "8080");
  // }

  @Before
  public void beforeEach () throws Exception {
    final MongodStarter starter = MongodStarter.getDefaultInstance ();
    final int port = 12345;
    final IMongodConfig mongodConfig = new MongodConfigBuilder ().version (Version.Main.PRODUCTION)
                                                                 .net (new Net (port, Network.localhostIsIPv6 ()))
                                                                 .build ();
    mongodExecutable = starter.prepare (mongodConfig);
    mongodExecutable.start ();
    mongo = new MongoClient ("localhost", port);
  }

  @After
  public void afterEach () throws Exception {
    if (this.mongodExecutable != null)
      this.mongodExecutable.stop ();
  }

  @Test
  public void shouldCreateNewObjectInEmbeddedMongoDb () {
    // given
    final DB db = mongo.getDB ("test");
    final DBCollection col = db.createCollection ("testCol", new BasicDBObject ());
    col.save (new BasicDBObject ("testDoc", new Date ()));

    // then
    assertEquals (1, col.getCount ());
  }
}
