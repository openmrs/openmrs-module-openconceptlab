## OpenMRS Open Concept Lab module

### Project page

[OCL Subscription Module (Design Page)](https://wiki.openmrs.org/pages/viewpage.action?pageId=70877277)

### Development board

[Development board](https://issues.openmrs.org/secure/RapidBoard.jspa?rapidView=93)

### Setup Development Environment for Open Concept Lab Module

#### 1.Dependencies:
- MySQL
- Maven
- JDK 1.8

#### 2.Setup OpenMRS server

Run this command to start OpenMRS server setup:
````sh
$ mvn openmrs-sdk:setup -DserverId=refapp -Dversion=2.3
# Note: Pick default values for everything except MySQL username and password
````

If there are any issues with server setup, check out <b>[OpenMRS SDK documentation](https://wiki.openmrs.org/display/docs/OpenMRS+SDK)</b>

#### 3.Installing Reference Application on server

a) Clone <b>[Reference Application project](https://github.com/openmrs/openmrs-distro-referenceapplication)</b> from GitHub:
````sh
$ git clone https://github.com/openmrs/openmrs-distro-referenceapplication.git
````
b) Inside Reference Application project directory:
````sh
$ mvn clean install -DskipTests
````
c) Run server:
````sh
$ mvn openmrs-sdk:run
# Note: Don't visit localhost:8080/openmrs yet!
````
d) Build *.omod packages:
````sh
mvn clean package -DskipTests
````
e) Copy all `*.omod` packages from `/package/target/distro` to local OpenMRS module repository (default `home/.OpenMRS/modules`)

Linux users can run this script inside Reference Application project directory to automate package building and copying:
````sh
#!/bin/bash
mvn clean package -DskipTests
if [ -d ~/.OpenMRS/modules ];
then
    rm ~/.OpenMRS/modules/*
else
    mkdir -p ~/.OpenMRS/modules
fi
cp package/target/distro/*.omod ~/.OpenMRS/modules
echo Done copying omods to module repo
````

f) Now go to `http://localhost:8080/openmrs` and wait for server setup (it takes about 15 minutes)
<i>Note: Refresh page if you can't see instalation progress (this bug is caused by browser cache)</i>

More information about setting up Development Environment for Reference Application:
https://wiki.openmrs.org/display/projects/Setting+Up+a+Development+Environment+for+OpenMRS+2.x

#### 5.Installing Open Concept Lab module:

a) Clone [Open Concept Lab module project](https://github.com/openmrs/openmrs-module-openconceptlab/):
````sh
$ git clone https://github.com/openmrs/openmrs-module-openconceptlab.git
````
b) Inside OCL module project directory:
````sh
$ mvn clean install -DskipTests
````
c) Copy *.omod file from `/omod/target` directory to `~/openmrs/refapp/modules` directory and restart server.

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

d) OCL Module is now available at <b>System Administration</b> -> <b>Advanced Administration</b> -> <b>Open Concept Lab</b> or at '/openmrs/openconceptlab/status.page'

Note: Its possible to add this module to Reference Application's Main Menu or Configure Metadata panel, see:
- [Adding module to Reference Application's Main Menu](https://wiki.openmrs.org/pages/viewpage.action?pageId=93359610#DevelopinganHTML+JSOpenWebAppQuickly-LinkingtoYourOpenWebAppfromtheHomeScreen)
- [Adding module to Configure Metadata panel](https://wiki.openmrs.org/display/projects/Adding+new+Groups+and+Links+to+Configure+Metadata+Menu)
