Data Migration 1.7.0 to 1.8.0
=============================

1. Shutdown AM 1.7.0 if it is running.

2. Backup your API Manager Databases of your AM 1.7.0 instance.

3. Execute relevant sql script in in here against your API Manager Database.

4. Now point same WSO2 Carbon Database(User Store and Registry) and API Manager Databases of your AM 1.7.0 instance to AM 1.8.0.
(Configure AM_1.8.0/repository/datasource/master-datasources.xml to point same databases configured in AM 1.7.0)

5. Move your synapse configurations to APIM_1.8.0. For that, copy and replace APIM_1.7.0/repository/deployment/server/synapse-configs/default directory to APIM_1.8.0/repository/deployment/server/synapse-configs/default. Do not replace _TokenAPI_.xml, _RevokeAPI_.xml and _AuthorizeAPI_.xml files in the default/api subdirectory

6. Start AM 1.8.0 and Login.

7. Copy the <APIM_1.8.0_HOME>/dbscripts/migration-1.7.0_to_1.8.0/swagger-doc-migration directory to <APIM_1.8.0_HOME>(The new directory path will now be <APIM_1..0_HOME>/swagger-doc-migration).

8. Configure swagger-doc-migration/build.xml with the information for the below properties.

   registry.home= Path to AM pack location   
   username= Username for the AM server
   password= Password for the AM server
   host= IP of running AM server [In a distributed setup, give the host of the Publisher node]   
   port= Port of running AM server [In a distributed setup, give the port of the Publisher node]   
   version= Version of AM server

9. Go inside swagger-doc-migration/ and execute "ant run". You should get a "BUILD SUCCESSFUL" message if run correctly.

10. Re-index log in to carbon console (ex: http://localhost:9443/carbon) and delete 'lastaccesstime' resource in '/_system/local/repository/components/org.wso2.carbon.registry/indexing' location. To do this, go to Home-> Resources->Browse and navigate to the above given location. You can delete the 'lastaccesstime' resource by selecting Actions-> Delete

11. shutdown AM 1.8.0 and delete <APIM_1.8.0_HOME>/repository/conf/solr directory and restart the server. 


Tenant Migration (Only needs to be done if you are migrating a multi-tenanted setup)
====================================================================================

1. Move your tenant synapse configurations to APIM_1.8.0. To do that, copy and replace specific folders for tenants(shown as 1,2,...) from APIM_1.7.0/repository/tenants/ to APIM_1.8.0/repository/tenants. Do not replace _TokenAPI_.xml, _RevokeAPI_.xml and _AuthorizeAPI_.xml files in the default/api subdirectory.

2. Start AM 1.8.0.

3. Configure swagger-doc-migration/build.xml with the information for the below properties. (swagger-doc-migration folder should be already copied to <APIM_1.8.0_HOME>)

   registry.home= Path to AM pack location
   username= Username for the AM server - respective tenant space
   password= Password for the AM server - respective tenant space
   host= IP of running AM server [In a distributed setup, give the host of the Publisher node]   
   port= Port of running AM server [In a distributed setup, give the port of the Publisher node]   
   version= Version of AM server

4. Go inside swagger-doc-migration/ and execute "ant run". You should get a "BUILD SUCCESSFUL" message if it ran correctly.
