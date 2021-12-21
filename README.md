# as2-lib

[![javadoc](https://javadoc.io/badge2/com.helger.as2/as2-lib/javadoc.svg)](https://javadoc.io/doc/com.helger.as2/as2-lib)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.helger.as2/as2-lib-parent-pom/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.helger.as2/as2-lib-parent-pom) 

AS2 is a transport protocol specified in [RFC 4130](http://www.ietf.org/rfc/rfc4130.txt).
AS2 version 1.1 adding compression is specified in [RFC 5402](http://www.ietf.org/rfc/rfc5402.txt).
The MDN is specified in [RFC 3798](http://www.ietf.org/rfc/rfc3798.txt).
Algorithm names are defined in [RFC 5751](https://www.ietf.org/rfc/rfc5751.txt) (S/MIME 3.2) which supersedes [RFC 3851](https://www.ietf.org/rfc/rfc3851.txt) (S/MIME 3.1);

See the **[Wiki](https://github.com/phax/as2-lib/wiki)** for all details.
It also contains [License details](https://github.com/phax/as2-lib/wiki/Licensing). 

This library is a fork of [OpenAS2](http://sourceforge.net/projects/openas2/) which did not 
release updates since 2010 (as per August 2015 they are on GitHub at https://github.com/OpenAS2/OpenAs2App). I then split the project into a common library part (the "as2-lib" submodule)
and a server part (the "as2-server" submodule) which contains a stand alone (socket) server. The library project also contains a simple AS2 client which can be used to send messages to other AS2 servers (as part of "as2-lib").

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
On Twitter: <a href="https://twitter.com/philiphelger">@philiphelger</a> |
Kindly supported by [YourKit Java Profiler](https://www.yourkit.com)
