IMPORTANT: 
You must have the proper JCE policy files install with your JRE for encryption to work correctly.
Go to http://java.sun.com/j2se/downloads.html
Select the version of java you use
At the bottom of the page under "Other Downloads", download the JCE unlimited strength jurisdiction 
policy file and follow the directions with it for installation.


To quickly start OpenAS2:

In windows:
- go to the <project> directory
- double-click on .\bin\start-openas2.bat
- type "cert" or "part" at the command prompt to get lists of some useful commands
- type "exit" to shut down the server


To import a public certificate:

#>cert import <x509 alias used in partnerships> <.cer filename>


To import a public certificate and it's private key:

#>cert import <x509 alias> <.p12 filename> <password to access private key in .p12 file>




