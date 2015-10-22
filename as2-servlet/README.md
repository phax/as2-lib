#as2-peppol-servlet

A stand alone servlet that takes AS2 requests with OpenPEPPOL StandardBusinessDocuments and handles them via SPI. This is not a self-contained package, but a good starting point for handling PEPPOL AS2 messages.

An example application that uses *as2-peppol-servlet* for receiving PEPPOL AS2 messages is my **[as2-peppol-server](https://github.com/phax/as2-peppol-server)** project. It may serve as a practical starting point.

This package depends on **[ph-commons](https://github.com/phax/ph-commons)**, **[ph-sbdh](https://github.com/phax/ph-sbdh)** and **[as2-lib](https://github.com/phax/as2-lib)**. This transitively includes Bouncy Castle (1.53) and javax.mail (1.5.4) among other libraries.

*as2-peppol-servlet* handles incoming AS2 messages, and parses them as OASIS Standard Business Documents (SBD). It does not contain extraction of the SBD content or even handling of the UBL content since the purpose of this project is reusability. For validating the SBD against PEPPOL rules, the project **[peppol-sbdh](https://github.com/phax/peppol-sbdh)** is available and for handling UBL 2.0 or 2.1 files you may have a look at my **[ph-ubl](https://github.com/phax/ph-ubl)**.

This project is licensed under the Apache 2 License.

Versions <= 1.0.1 are compatible with ph-commons < 6.0.
Versions >= 2.0.0 are compatible with ph-commons >= 6.0.

#Usage
To use this project you have to do the following - all described in more detail below:
  1. Add this project as a dependency to your project - e.g. via Maven
  2. Modify your `WEB-INF/web.xml` file so that it references the `AS2PeppolReceiveServlet`.
  3. Create an AS2 configuration file and store it in a folder that is fully writable to your project. The details of the configuration files are described below.
  4. Create a key store file (e.g.) called `server-certs.p12` located in the same folder as the configuration file. The keystore type must be `PKCS12`. It must contain your PEPPOL AP certificate and the alias of the only entry must be the CN-value of your certificate's subject (e.g. `APP_1000000001`). The path and the password of the keystore must be set in the AS2 configuration file.
  5. Inside your project create an SPI implementation of the `com.helger.as2servlet.sbd.IAS2IncomingSBDHandlerSPI` interface to handling incoming SBD documents.

##Add project via Maven
Add the following to your pom.xml to use this artifact:

```xml
<dependency>
  <groupId>com.helger</groupId>
  <artifactId>as2-peppol-servlet</artifactId>
  <version>2.2.1</version>
</dependency>
```

##WEB-INF/web.xml configuration  
Example `WEB-INF/web.xml` configuration:
```xml
  <servlet>
    <servlet-name>AS2PeppolReceiveServlet</servlet-name>
    <servlet-class>com.helger.as2servlet.AS2PeppolReceiveServlet</servlet-class>
    <init-param>
      <param-name>as2-servlet-config-filename</param-name>
      <param-value>as2-server-data/as2-server-config.xml</param-value>
    </init-param>
  </servlet>
  <servlet-mapping>
    <servlet-name>AS2PeppolReceiveServlet</servlet-name>
    <url-pattern>/as2/*</url-pattern>
  </servlet-mapping>
```
As you can see, a configuration file called `as2-server-data/as2-server-config.xml` is referenced as an `init-param` of the servlet. The name of the `init-param` must be `as2-servlet-config-filename`. Please make sure to insert the correct absolute path to the configuration file inside the `param-value` element.

In this example the servlet is mapped to the path `/as2` meaning that messages must be targeted to this URL (e.g. `https://myserver/as2`). 
  
##AS2 Configuration file

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
            filename="%home%/inbox/$msg.sender.as2_id$-$msg.receiver.as2_id$-$msg.headers.message-id$"
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
    <!-- [required] Process incoming SBD documents -->
    <module classname="com.helger.as2servlet.sbd.AS2ServletSBDModule" />      
  </processor>
</openas2>
```

##SPI implementation

SPI stands for "Service provider interface" and is a Java standard feature to enable loose but typed coupling. [Read more on SPI](http://docs.oracle.com/javase/tutorial/ext/basics/spi.html)

A [dummy SPI implementation](https://github.com/phax/as2-peppol-servlet/blob/master/src/test/java/com/helger/as2servlet/sbd/MockIncomingSBDHandler.java) is contained in the test code of this project. Additionally you need to create a file `META-INF/services/com.helger.as2servlet.sbd.IAS2IncomingSBDHandlerSPI` (in the `src/main/resources/` folder when using Maven) which contains a single line referencing the implementation class. An [example file](https://github.com/phax/as2-peppol-servlet/blob/master/src/test/resources/META-INF/services/com.helger.as2servlet.sbd.IAS2IncomingSBDHandlerSPI) is located in the test resources of this project.

# Known issues

  * PEPPOL AS2 specs requires that duplicate incoming message IDs are handled specially, by ignoring multiple transmissions of the same message ID
  * The certificate check of the sender's certificate must be improved 

---

On Twitter: <a href="https://twitter.com/philiphelger">Follow @philiphelger</a>
