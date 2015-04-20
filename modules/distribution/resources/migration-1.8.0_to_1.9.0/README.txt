WSO2 API Migration Tool - Migrate APIManager 1.8.0 to 1.9.0
===========================================================

This bundle is used to migrate swagger documents from swagger version 1.2 to 2.0

Follow the steps below.
- Find the pom.xml inside the swagger-doc-migration directory
- Run "mvn clean install" command to build the migration client
- Find the bundle "org.wso2.carbon.apimgt.migration-1.0.0.jar" inside the target directory
- Copy the bundle to AM 1.8.0 <APIM_HOME>/repository/components/dropins
- Start the server with -Dmigrate=1.8 to migrate swagger resources
- This operation will transfer swagger resources from v1.2 to v2.0

Notes
- The mysql.sql in the migrate directories should be run against the API_M database.
- Set apim.home in build.xml of ./migration-1.6.0_to_1.7.0/api-migration/ (check other build.xml files as well)
- To run the rxt migration script which I shared you need to install the library xmlstarlet. Make sure to mention this in the doc as well. 

