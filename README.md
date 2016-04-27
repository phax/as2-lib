#as2-lib

[![Build Status](https://travis-ci.org/phax/as2-lib.svg?branch=master)](https://travis-ci.org/phax/as2-lib)
﻿
[![Join the chat at https://gitter.im/phax/as2-lib](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/phax/as2-lib?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

AS2 is a transport protocol specified in [RFC 4130](http://www.ietf.org/rfc/rfc4130.txt).
This library is a fork of [OpenAS2](http://sourceforge.net/projects/openas2/) which did not 
release updates since 2010 (as per August 2015 they are on GitHub at https://github.com/OpenAS2/OpenAs2App). I than split the project into a common library part (this project)
and a [server part](https://github.com/phax/as2-server) which contains a stand alone server. This project also contains a simple AS2 client which can be used to send messages to other AS2 servers.

This project is used in my following other projects:
  * **[as2-server](https://github.com/phax/as2-server)** - a stand alone AS2 server operating on a socket layer.
  * **[as2-peppol-client](https://github.com/phax/as2-peppol-client)** - a stand alone AS2 client that is capable of sending [PEPPOL](http://www.peppol.eu) compliant e-Procurement documents.
  * **[as2-peppol-servlet](https://github.com/phax/as2-peppol-servlet)** - integration into the Servlet specifications and for use with the [PEPPOL](http://www.peppol.eu) transport infrastructure including SBDH (Standard Business Document Header) handling.
  * **[as2-peppol-server](https://github.com/phax/as2-peppol-server)** - a stand alone Servlet based server to receive [PEPPOL](http://www.peppol.eu) AS2 messages.

Versions <= 1.1.0 are compatible with ph-commons < 6.0.
Versions >= 2.0.0 are compatible with ph-commons >= 6.0.

`as2-lib` is licensed under the FreeBSD License. The subproject `as2-partnership-mongodb` is licensed under the Apache 2 license. 

#News and noteworthy

  * 2016-04-27 Version 2.2.7
    * Added per partnership attribute `rfc3851_micalgs` to determine to use RFC 3851 MIC algorithm names instead of the default RFC 5751 MIC algorithm names (issue #18)
    * Fixed problem with special character escaping in MDN context (issue #19)
  * 2016-03-02 Version 2.2.6
    * Added support for dumping HTTP requests when using `as2-servlet` 
  * 2015-12-01 Version 2.2.5
    * Added a validity check so that expired certificates can no longer be used to sign, verify and encrypt messages. Decrypt is still possible.
    * Added the possibility to disable the autosave of the `PKCS12CertificateFactory` using the new boolean attribute `autosave`. This can now also easily be set in the client settings. (issue #17)
    * Fixed potential endless loop when a retry count was specified at a partnership (issue #16) 
  * 2015-11-11 Version 2.2.4
    * Fixed a minor issue where details of a ProcessingException were not passed into the MDN text
  * 2015-10-22 Version 2.2.3
    * Improved API for handling MDN errors (as2-lib issue #11)  
    * The signature verification of messages sent without `Content-Transfer-Encoding` was fixed (as2-lib issue #12)
    * Receiving a message for an unknown partnership now results in a correct error MDN (as2-server issue #16)
    * The new sub-project `as2-servlet` is now contained
  * 2015-10-19 Version 2.2.2
    * Updated to Bouncy Castle 1.53 (as2-lib issue #10)
  * 2015-10-08 Version 2.2.1
    * Extended API and some debug logging added
  * 2015-09-27 Version 2.2.0
    * added system properties (see below) for configuration and debugging purposes
    * added new resender modules: `ImmediateResenderModule` and `InMemoryResenderModule`
    * added the following new partnership attributes:
      * `content_transfer_encoding_receive` [receiver side] to define a fixed `Content-Transfer-Encoding` for receiving, even if none is specified.
      * `force_decrypt` [receiver side] to force decryption of incoming messages even if the `Content-Type` header claims the message is not encrypted (as a work-around for non spec-compliant senders)
      * `disable_decrypt` [receiver side] to disable decryption of incoming messages even if the `Content-Type` header claims the message is encrypted (as a work-around for non spec-compliant senders)
      * `force_verify` [receiver side] to force signature validation of incoming messages even if the `Content-Type` header claims the message is not signed (as a work-around for non spec-compliant senders)
      * `disable_verify` [receiver side] to disable signature verification of incoming messages even if the `Content-Type` header claims the message is signed (as a work-around for non spec-compliant senders)
      * `verify_use_cert_in_body_part` [receiver side] to define whether a certificate passed in the signed MIME body part shall be used to verify the signature (when `true`) or whether to always use the certificate provided in the partnership (when `false`). If not set the value of the AS2 session is used.
      * `disable_decompress` [receiver side] to disable decompression of incoming messages even if the `Content-Type` header claims the message is compressed (as a work-around for non spec-compliant senders)
      * `sign_include_cert_in_body_part` [sender side] to determine whether the certificate used for signing should be included in the signed content part (when `true`) or not (when `false`). The default value is `true`.
      * Added the sub-project `as2-partnership-mongodb` - thanks to @jochenberger for contributing it
  * 2015-08-20 Version 2.1.0
    * fixes a problem that implicitly SHA-1 was always used for signing, no matter what you specify
    * compression according to RFC 5402 is now supported so that this is no fully AS2 1.1 compatible

#Maven usage
Add the following to your `pom.xml` to use this artifact:
```
<dependency>
  <groupId>com.helger</groupId>
  <artifactId>as2-lib</artifactId>
  <version>2.2.6</version>
</dependency>
```

For the MongoDB partnership factory, add the following to your `pom.xml`:
```
<dependency>
  <groupId>com.helger</groupId>
  <artifactId>as2-partnership-mongodb</artifactId>
  <version>2.2.6</version>
</dependency>
```

For the receive servlet, add the following to your `pom.xml`:
```
<dependency>
  <groupId>com.helger</groupId>
  <artifactId>as2-servlet</artifactId>
  <version>2.2.6</version>
</dependency>
```

#Building
This project is build with Apache Maven 3.x. Simply call `mvn clean install` and you will end up with a JAR file in the `as2-lib/target` directory. This library is used as the basis for the standalone [as2-server](https://github.com/phax/as2-server) which is an pure Java Open Source AS2 server.

The `as2-lib` sub-project requires at least Java 1.6 and should be without problems. It is licensed under the FreeBSD license (as OpenAS2).

The `as2-partnership-mongodb` sub-project requires at least Java 1.8 and upon installing it the first time it downloads an embedded MongoDB from the official web site and extracts it. If this makes problems specify the `-DskipTests=true` parameter when calling Maven.

The `as2-servlet` sub-project requires at least Java 1.6 and should be without problems. It is licensed under the Apache 2 license (as OpenAS2).


If you only have Java 1.6 or 1.7 available perform the following commands to build only `as2-lib` (assuming you are in the root directory of this project):
```
mvn clean install -N
cd as2-lib
mvn clean install
```

This installs the parent POM first (`-N` means *not recursive*) and afterwards `as2-lib` is build as usual.

##as2-lib
###Package structure
This library manages the package `com.helger.as2lib` and all sub-packages:

  * com.helger.as2lib - contains the global AS2 settings
  * com.helger.as2lib.cert - contains the certificate factory
  * com.helger.as2lib.client - contains the AS2 client for sending messages
  * com.helger.as2lib.crypto - contains the BouncyCastle crypto code for signing, verification, encryption and decryption of messages
  * com.helger.as2lib.disposition - contains code to handle content disposition
  * com.helger.as2lib.exception - contains basic exception classes
  * com.helger.as2lib.message - contains the main message and MDN classes
  * com.helger.as2lib.params - generic code that allows for parameterization of certain message related configuration items
  * com.helger.as2lib.partner - contains the partnership and factory code
  * com.helger.as2lib.partner.xml - contains the XML based version of a partnership factory
  * com.helger.as2lib.processor - contains the basic building blocks for processing of messages
  * com.helger.as2lib.processor.module - contains the basic classes for "active" modules (modules that can be started and stopped)
  * com.helger.as2lib.processor.receiver - module for receiving messages
  * com.helger.as2lib.processor.receiver.net - module for receiving messages from Sockets
  * com.helger.as2lib.processor.resender - modules for re-sending messages
  * com.helger.as2lib.processor.sender - module for sending messages
  * com.helger.as2lib.processor.storage - module for storing messages
  * com.helger.as2lib.util - contains utility classes used in several places in this library or in derived projects
  * com.helger.as2lib.util.cert - utility classes for certificate handling
  * com.helger.as2lib.util.http - utility classes for HTTP connection handling
  * com.helger.as2lib.util.javamail - utility classes for javax.mail handling

###System Properties
The following system properties are available for global customization

  * boolean `AS2.useSecureRandom` - since 2.2.0 - determine whether the Java `SecureRandom` should be used or not. On some Unix/Linux systems the initialization of `SecureRandom` takes forever and this is how you easily disable it (`-DAS2.useSecureRandom=false`).
  * String `AS2.httpDumpDirectory` - since 2.2.0 - if this system property is defined, all incoming HTTP traffic is dumped "as is" into the specified directory (e.g. `-DAS2.httpDumpDirectory=/var/dump/as2-http`). The filename starts with "as2-", contains the current timestamp as milliseconds, followed by a dash and a unique index and finally has the extension ".http"
  * String `AS2.dumpDecryptedDirectory` - since 2.2.0 - if this system property is defined, all incoming decrypted MIME parts are dumped "as is" into the specified directory (e.g. `-DAS2.dumpDecryptedDirectory=/var/dump/as2-decrypted`). The filename starts with "as2-decrypted-", contains the current timestamp as milliseconds, followed by a dash and a unique index and finally has the extension ".part"

##as2-partnership-mongodb
This is an implementation of interface `com.helger.as2lib.partner.IPartnershipFactory` from as2-lib using MongoDB as the backend. It requires Java 1.8 for building and running.
Tests are done with Groovy and Spock.

This sub-project is licensed under the Apache 2 License.

##as2-servlet
A stand alone servlet that takes AS2 requests and handles them via a `AS2ServletReceiverModule`.

This sub-project is licensed under the Apache 2 License.

###Usage
To use this project you have to do the following - all described in more detail below:
  1. Add the `as2-servlet` project as a dependency to your project - e.g. via Maven
  2. Modify your `WEB-INF/web.xml` file so that it references the `com.helger.as2servlet.AS2ReceiveServlet`.
  3. Create an AS2 configuration file and store it in a folder that is fully writable to your project. The details of the configuration files are described below.
  4. Create a key store file (e.g.) called `server-certs.p12` located in the same folder as the configuration file. The keystore type must be `PKCS12`. It must at least contain your private key. The path and the password of the keystore must be set in the AS2 configuration file.

###WEB-INF/web.xml configuration  
Example `WEB-INF/web.xml` configuration:
```xml
  <servlet>
    <servlet-name>AS2ReceiveServlet</servlet-name>
    <servlet-class>com.helger.as2servlet.AS2ReceiveServlet</servlet-class>
    <init-param>
      <param-name>as2-servlet-config-filename</param-name>
      <param-value>as2-server-data/as2-server-config.xml</param-value>
    </init-param>
  </servlet>
  <servlet-mapping>
    <servlet-name>AS2ReceiveServlet</servlet-name>
    <url-pattern>/as2/*</url-pattern>
  </servlet-mapping>
```
As you can see, a configuration file called `as2-server-data/as2-server-config.xml` is referenced as an `init-param` of the servlet. The name of the `init-param` must be `as2-servlet-config-filename`. Please make sure to insert the correct **absolute path** to the configuration file inside the `param-value` element.

In this example the servlet is mapped to the path `/as2` meaning that messages must be targeted to this URL (e.g. `https://myserver/as2`). 


###AS2 Configuration file
A special XML configuration file must be used to configure the AS2 handling. It contains:
  * a reference to the keystore to be used (in element `certificates`)
  * a reference to a partnership factory (storing the exchange combinations) (in element `partnerships`)
  * a list of modules that are executed when a message is received (in elements `module`)

Within a configuration file, the macro `%home%` is replaced with the parent directory of the configuration file. This replacement happens only when a value starts with `%home%`.

Complete example configuration file:
 
```xml
<?xml version="1.0" encoding="utf-8"?>
<openas2>
  <!-- [required] The keystore to be used -->
  <certificates classname="com.helger.as2lib.cert.PKCS12CertificateFactory" 
                filename="%home%/server-certs.p12"
                password="peppol" />
  <!-- [required] The pro-forma partnership factory -->                  
  <partnerships classname="com.helger.as2servlet.util.AS2ServletPartnershipFactory"
                filename="%home%/server-partnerships.xml"
                disablebackup="true" />
 
  <!-- [required] the processing queue -->
  <processor classname="com.helger.as2lib.processor.DefaultMessageProcessor"
             pendingMDN="%home%/pendingMDN"
             pendingMDNinfo="%home%/pendinginfoMDN">
    <!-- [optional] Store sent MDNs to a file -->
    <module classname="com.helger.as2lib.processor.storage.MDNFileModule"
            filename="%home%/mdn/$date.yyyy$/$date.MM$/$mdn.msg.sender.as2_id$-$mdn.msg.receiver.as2_id$-$mdn.msg.headers.message-id$"      
            protocol="as2"
            tempdir="%home%/temp"/>
    <!-- [optional] Store received messages and headers to a file -->
    <module classname="com.helger.as2lib.processor.storage.MessageFileModule"
            filename="%home%/inbox/$date.yyyy$/$date.MM$/$msg.sender.as2_id$-$msg.receiver.as2_id$-$msg.headers.message-id$"
            header="%home%/inbox/msgheaders/$date.yyyy$/$date.MM$/$msg.sender.as2_id$-$msg.receiver.as2_id$-$msg.headers.message-id$"    
            protocol="as2"
            tempdir="%home%/temp"/>
    <!-- [required] The main receiver module that performs the message parsing.
         This module also sends synchronous MDNs back.
         Note: the port attribute is required but can be ignored in our case!
     -->            
    <module classname="com.helger.as2servlet.util.AS2ServletReceiverModule"      
            port="10080"
            errordir="%home%/inbox/error"
            errorformat="$msg.sender.as2_id$, $msg.receiver.as2_id$, $msg.headers.message-id$"/>
            
    <!-- To process the documents further than just storing them to disk, implement
         class AbstractProcessorModule and register the module here.
         See the phax/as2-peppol-servlet project on how to handle e.g. SBDH documents 
    -->                      
  </processor>
</openas2>
```

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodeingStyleguide.md) |
On Twitter: <a href="https://twitter.com/philiphelger">@philiphelger</a>
