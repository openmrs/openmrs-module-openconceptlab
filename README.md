## OpenMRS Open Concept Lab module

[![Build Status](https://travis-ci.org/openmrs/openmrs-module-openconceptlab.svg?branch=master)](https://travis-ci.org/openmrs/openmrs-module-openconceptlab) [![Codacy Badge](https://api.codacy.com/project/badge/grade/5653fb10f19049db9864a447c399ce76)](https://www.codacy.com/app/openmrs/openmrs-module-openconceptlab) [![Codacy Badge](https://api.codacy.com/project/badge/coverage/5653fb10f19049db9864a447c399ce76)](https://www.codacy.com/app/openmrs/openmrs-module-openconceptlab)

### Project page

[OCL Subscription Module (Design Page)](https://wiki.openmrs.org/pages/viewpage.action?pageId=70877277)

### Development board

[Development board](https://issues.openmrs.org/secure/RapidBoard.jspa?rapidView=93)

### Development environment

Please make sure you have the following installed:
- MySQL 5.6.x
- JDK 1.7
- Maven
- OpenMRS SDK, see [installation instructions](https://wiki.openmrs.org/display/docs/OpenMRS+SDK#OpenMRSSDK-Installation)

Next run this command to setup OpenMRS server (just the first time):
````sh
$ mvn openmrs-sdk:setup -DserverId=refapp -Ddistro=referenceapplication:2.4
# Note: Pick default values for everything except MySQL username and password
````
If there are any issues with setting up the server, check out <b>[OpenMRS SDK documentation](https://wiki.openmrs.org/display/docs/OpenMRS+SDK)</b>

Deploy OWA module on the server:
````sh
$ mvn openmrs-sdk:deploy -DserverId=refapp -DartifactId=owa -Dversion=1.6.3
````

You also need to deploy webservices.rest module:
````sh
$ mvn openmrs-sdk:deploy -DartifactId=webservices.rest -Dversion=2.17-SNAPSHOT -DserverId=refapp
````

Fork and clone [Open Concept Lab module project](https://github.com/openmrs/openmrs-module-openconceptlab/):
````sh
$ mvn openmrs-sdk:clone -DartifactId=openconceptlab
$ cd openmrs-module-openconceptlab
````

Build and install the module on the server:
````sh
$ mvn clean install openmrs-sdk:deploy -DserverId=refapp
````

You can also configure the server to automatically reload classes and pages from the omod directory:
````sh
$ mvn openmrs-sdk:watch -DserverId=refapp
````

Run the server:
````sh
$ mvn openmrs-sdk:run -DserverId=refapp
````

Once you run the command you need to wait for Jetty Started message, open up a browser ang go to:

[http://localhost:8080/openmrs](http://localhost:8080/openmrs)

The server can be stopped from the terminal, in which it is running with the Ctrl and C (Ctrl + C) keys combination.

Every time you make changes in code in the api directory, you need to build and install the module and restart the server.

Alternatively you can upload `*.omod` file via <b>Advanced Administration</b> -> <b>Manage Modules</b> panel. This way you will not have to restart the server.

OCL Module is now available from the Advanced System Administration or at '/openmrs/openconceptlab/status.page'
