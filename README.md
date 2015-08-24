#as2-lib

AS2 is a transport protocol specified in [RFC 4130](http://www.ietf.org/rfc/rfc4130.txt).
This library is a fork of [OpenAS2](http://sourceforge.net/projects/openas2/) which did not 
release updates since 2010 (as per August 2015 they are on GitHub at https://github.com/OpenAS2/OpenAs2App). I than split the project into a common library part (this project)
and a [server part](https://github.com/phax/as2-server) which contains a stand alone server. This project also contains a simple AS2 client which can be used to send messages to other AS2 servers.

This project is used in my following other projects:
  * **[as2-server](https://github.com/phax/as2-server)** - a stand alone AS2 server operating on a socket layer.
  * **[as2-peppol-client](https://github.com/phax/as2-peppol-client)** - a stand alone AS2 client that is capable of sending [PEPPOL](www.peppol.eu) compliant e-Procurement documents.
  * **[as2-peppol-servlet](https://github.com/phax/as2-peppol-servlet)** - integration into the Servlet specifications and for use with the [PEPPOL](www.peppol.eu) transport infrastructure including SBDH (Standard Business Document Header) handling.
  * **[as2-peppol-server](https://github.com/phax/as2-peppol-server)** - a stand alone Servlet based server to receive [PEPPOL](www.peppol.eu) AS2 messages.

Versions <= 1.1.0 are compatible with ph-commons < 6.0.

Versions >= 2.0.0 are compatible with ph-commons >= 6.0.

This project is licensed under the FreeBSD License.

#News and noteworthy

  * 2015-08-20 Version 2.1.0 fixes a problem that implicitly SHA-1 was always used for signing, no matter what you specify. Also compression according to RFC 5402 is now supported.

#Maven usage
Add the following to your pom.xml to use this artifact:
```
<dependency>
  <groupId>com.helger</groupId>
  <artifactId>as2-lib</artifactId>
  <version>2.1.0</version>
</dependency>
```

#Package structure
This library manages the package `com.helger.as2lib` and all sub-packages:

  * com.helger.as2lib - contains the global AS2 settings
  * com.helger.as2lib.cert - contains the certificate factory
  * com.helger.as2lib.client - contains the AS2 client for sending messages
  * com.helger.as2lib.crypto - contains the BouncyCastle crypto code for signing, verification, encryption and decryption of messages
  * com.helger.as2lib.disposition - contains code to handle content disposition
  * com.helger.as2lib.exception - contains basic exception classes
  * com.helger.as2lib.message - contains the main message and MDN classes
  * com.helger.as2lib.params - generic code that allows for parameterization of certain message related configuration items
  * com.helger.as2lib.partner - contains the partner, partnership and factory code
  * com.helger.as2lib.processor - contains the basic building blocks for processing of messages
  * com.helger.as2lib.processor.module - contains the basic classes for "active" modules (modules that can be started and stopped)
  * com.helger.as2lib.processor.receiver - module for receiving messages
  * com.helger.as2lib.processor.receiver.net - module for receiving messages from Sockets
  * com.helger.as2lib.processor.resender - module for re-sending messages
  * com.helger.as2lib.processor.sender - module for sending messages
  * com.helger.as2lib.processor.storage - module for storing messages
  * com.helger.as2lib.util - contains utility classes used in several places in this library or in derived projects
  * com.helger.as2lib.util.cert - utility classes for certificate handling
  * com.helger.as2lib.util.http - utility classes for HTTP connection handling
  * com.helger.as2lib.util.javamail - utility classes for javax.mail handling

#Building
This project is build with Apache Maven 3.x. Simply call `mvn clean install` and you will end up with a JAR file in the `target` directory.

#System Properties
The following system properties are available for global customization

  * `AS2.useSecureRandom` - since 2.1.1 - determine whether the Java `SecureRandom` should be used or not. On some Unix/Linux systems the initialization of `SecureRandom` takes forever and this is how you easily disable it (`-DAS2.useSecureRandom=false`).
 

---

On Twitter: <a href="https://twitter.com/philiphelger">Follow @philiphelger</a>
