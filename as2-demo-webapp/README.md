# as2-demo-webbapp

### Running the demo under Apache Karaf 4

Copy the project config folder, containing the certs.p12 and config.xml files, to the Karaf root folder.

Add Bouncycastle as security provider by adding the following jars to the Karaf folder lib/ext:

* bcmail-jdk15on-1.60.jar
* bcpkix-jdk15on-1.60.jar
* bcprov-jdk15on-1.60.jar
* bcprov-ext-jdk15on-1.60.jar

In the Karaf etc folder edit the file config.properties by adding:

    #
    # Security providers
    #
    org.apache.karaf.security.providers = org.bouncycastle.jce.provider.BouncyCastleProvider
    
Install the javax.servlet-api 3.1.0 by dropping the jar file into the Karaf deploy folder.

Start up Karaf and on the console enter:

    karaf@root()>feature:install http-whiteboard
    karaf@root()>feature:install pax-war
    
Drop the as2-demo-webbapp WAR file into the Karaf deploy folder.

To test the error page execute a HTTP GET request method with a browser and the URL:

    http://localhost:8181/demo/as2
    
For AS2-Servlet execution use the HTTP POST request method. 