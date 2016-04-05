## OpenMRS Open Concept Lab module

### Project page

[OCL Subscription Module (Design Page)](https://wiki.openmrs.org/pages/viewpage.action?pageId=70877277)

### Development board

[Development board](https://issues.openmrs.org/secure/RapidBoard.jspa?rapidView=93)

### Setup Development Environment for Open Concept Lab Module

#### 1.Dependencies:
- MySQL 5.6
- JDK 1.8
- Maven

#### 2.Setup OpenMRS server

Run this command to start OpenMRS server setup:
````sh
$ mvn openmrs-sdk:setup -DserverId=refapp -Dversion=2.3
# Note: Pick default values for everything except MySQL username and password
````
Run server:
````sh
$ mvn clean install openmrs-sdk:install -DserverId=refapp
$ mvn openmrs-sdk:run -DserverId=refapp
````

If there are any issues with server setup, check out <b>[OpenMRS SDK documentation](https://wiki.openmrs.org/display/docs/OpenMRS+SDK)</b>

#### 3.Installing Open Concept Lab module:

a) Clone [Open Concept Lab module project](https://github.com/openmrs/openmrs-module-openconceptlab/):
````sh
$ git clone https://github.com/openmrs/openmrs-module-openconceptlab.git
````
b) Copy *.omod file from `/omod/target` directory to `~/openmrs/refapp/modules` directory and restart server.

Alternatively you can upload `*.omod` file via Reference Application's <b>Advanced Administration</b> -> <b>Manage Modules</b> panel. This way to add module doesn't require manual server restarting.

Linux users can run this script to automate module copying (server still needs to be restarted after deploy):
````sh
#!/bin/bash
mvn clean package -DskipTests
if [ -d ~/openmrs/refapp/modules ];
then
    cp omod/target/*.omod ~/openmrs/refapp/modules
else
    mkdir -p ~/openmrs/refapp/modules
    cp omod/target/*.omod ~/openmrs/refapp/modules
fi
echo Done copying *.omod to server modules
````

c) OCL Module is now available at System Administration Panel or at '/openmrs/openconceptlab/status.page'