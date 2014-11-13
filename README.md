#as2-lib

AS2 is a transport protocol specified in [RFC 4130](http://www.ietf.org/rfc/rfc4130.txt).
This library is a fork of [OpenAS2](http://sourceforge.net/projects/openas2/) which did not 
release updates since 2010. I than split the project into a common library part (this project)
and a server part which contains a stand alone server. This project also contains a simple AS2 client which can be used to send messages to other AS2 servers. 

See the **[as2-peppol-servlet](https://github.com/phax/as2-peppol-servlet)** project for an integration into the Servlet specifications and for use with the [PEPPOL](www.peppol.eu) transport infrastructure including SBDH (Standard Business Document Header) handling. Or you may even have a look at my **[as2-peppol-server](https://github.com/phax/as2-peppol-server)** project for a stand alone server to receive PEPPOL AS2 messages. 

#Maven usage
Add the following to your pom.xml to use this artifact:
```
<dependency>
  <groupId>com.helger</groupId>
  <artifactId>as2-lib</artifactId>
  <version>1.0.4</version>
</dependency>
```

---

On Twitter: <a href="https://twitter.com/philiphelger">Follow @philiphelger</a>
