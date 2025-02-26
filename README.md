## OpenMRS Open Concept Lab module

[![Build Status](https://github.com/openmrs/openmrs-module-openconceptlab/actions/workflows/maven.yml/badge.svg)](https://github.com/openmrs/openmrs-module-openconceptlab/actions/workflows/maven.yml)[![Codacy Badge](https://api.codacy.com/project/badge/grade/5653fb10f19049db9864a447c399ce76)](https://www.codacy.com/app/openmrs/openmrs-module-openconceptlab) [![Codacy Badge](https://api.codacy.com/project/badge/coverage/5653fb10f19049db9864a447c399ce76)](https://www.codacy.com/app/openmrs/openmrs-module-openconceptlab)


The Open Concept Lab module is a module to import concepts to OpenMRS.
The concepts from Open Concept Lab and Dictionary Manager can be imported either using a subscription url and unique token or offline from file.
The module fetches all the items from the subscription URL updating the existing concepts, and can store subscription URL in global properties.

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

Once you run the command you need to wait for Jetty Started message, open up a browser and go to:

[http://localhost:8080/openmrs](http://localhost:8080/openmrs)

The server can be stopped from the terminal, in which it is running with the Ctrl and C (Ctrl + C) keys combination.

Every time you make changes in code in the api directory, you need to build and install the module and restart the server.

Alternatively you can upload `*.omod` file via <b>Advanced Administration</b> -> <b>Manage Modules</b> panel. This way you will not have to restart the server.

OCL Module is now available from the Advanced System Administration or at `/openmrs/openconceptlab/status.page`

### Setup Subscription

You need to set up the subscription before using the module (except for 'Import at server startup').
You will need an account on the Open Concept Lab server, which you can create at https://app.openconceptlab.org/#/accounts/signup/
Login and go to  your profile page by clicking your username in the page header and copy your API token, which can be found on the bottom left.
Now determine the collection URL or source URL you want to subscribe to, create a version and copy the subscription URL
Enter URL and token at `/openmrs/owa/openconceptlab/index.html#/subscription` page and save changes.
Click Subscribe and follow the next section then <b>Import from subscription server</b>.

### OpenMRS Custom Validation Schema

OCL provides a “custom validation schema” feature for OpenMRS that allows the OCL API to perform extra validation checks on concepts. You might get errors while importing concepts to OpenMRS if it is not validated with validator schema. The OpenMRS concept validator rule includes:

For any concept…
5. Only one fully specified name per locale
1. Must not have more than one preferred name per locale
2. All names (except short names) must be unique within the concept
3. Must not have more than one short name per locale
4. Short name must not be marked as locale preferred
6. At least one fully specified name (across all locales)
7. Valid values for class, data type, name type, and locale
8. All concepts should have a UUID (currently OCL’s external ID)
9. Concept UUID should not exceed 38 characters in length
10. Concept UUID must be unique (ideally universally… but certainly within any source/collection)

For a dictionary…
1. Fully specified names must be unique across all names (except short names and index terms) in a locale
2. Multiple concepts cannot share the same preferred name in the same locale
3. All concepts should have a unique external_id (OpenMRS UUID) not exceeding 38 chars
