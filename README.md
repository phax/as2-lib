# as2-lib

[![Build Status](https://travis-ci.org/phax/as2-lib.svg?branch=master)](https://travis-ci.org/phax/as2-lib)
﻿
[![Join the chat at https://gitter.im/phax/as2-lib](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/phax/as2-lib?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

AS2 is a transport protocol specified in [RFC 4130](http://www.ietf.org/rfc/rfc4130.txt).
AS2 version 1.1 adding compression is specified in [RFC 5402](http://www.ietf.org/rfc/rfc5402.txt).
The MDN is specified in [RFC 3798](http://www.ietf.org/rfc/rfc3798.txt).
Algorithm names are defined in [RFC 5751](https://www.ietf.org/rfc/rfc5751.txt) (S/MIME 3.2) which supersedes [RFC 3851](https://www.ietf.org/rfc/rfc3851.txt) (S/MIME 3.1);

See the **[Wiki](https://github.com/phax/as2-lib/wiki)** for all details.

This library is a fork of [OpenAS2](http://sourceforge.net/projects/openas2/) which did not 
release updates since 2010 (as per August 2015 they are on GitHub at https://github.com/OpenAS2/OpenAs2App). I than split the project into a common library part (the "as2-lib" submodule)
and a server part (the "as2-server" submodule) which contains a stand alone (socket) server. The library project also contains a simple AS2 client which can be used to send messages to other AS2 servers (as part of "as2-lib").

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
On Twitter: <a href="https://twitter.com/philiphelger">@philiphelger</a>
