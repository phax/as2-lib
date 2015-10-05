import org.bson.Document;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.partner.Partnership
import com.helger.as2lib.partner.mongodb.MongoDBPartnershipFactory;
import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network
import spock.lang.Shared;
import spock.lang.Specification

class MongoDBPartnershipFactorySpec extends Specification {
  
  @Shared
  MongodExecutable mongodExecutable;
  @Shared
  MongoClient mongo;
  @Shared
  MongoDBPartnershipFactory mongoDBPartnershipFactory
  @Shared
  MongoDatabase database
  @Shared
  MongoCollection<Document> collection
  
  def setupSpec(){
    final MongodStarter starter = MongodStarter.getDefaultInstance()
    final int port = 12345;
    final IMongodConfig mongodConfig = new MongodConfigBuilder().version(Version.Main.PRODUCTION)
                                                                 .net(new Net(port, Network.localhostIsIPv6()))
                                                                 .build()
    mongodExecutable = starter.prepare (mongodConfig)
    mongodExecutable.start()
    mongo = new MongoClient ("localhost", port)
    database = mongo.getDatabase("test")
    collection = database.getCollection("test")
    mongoDBPartnershipFactory = new MongoDBPartnershipFactory(collection,
      LoggerFactory.getLogger(MongoDBPartnershipFactory))
  }
  
  void cleanupSpec(){
    mongo?.close()
    mongodExecutable?.stop()
  }
  
  void setup(){
    database?.drop()
  }
  
  def "add a Partnership"(){
    given:
    Partnership partnership = new Partnership("Test partnership").with{
      
      it
    }
    when:
    mongoDBPartnershipFactory.addPartnership(partnership)
    then:
    collection.count() == 1
    mongoDBPartnershipFactory.getPartnershipByName("Test partnership") != null
    
  }
  
}