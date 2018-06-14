Data Migration 1.6.0 to 1.7.0 
=============================

Super tenant migration
============================= 

1. Shutdown AM 1.6.0 if it is running. 

2. Backup your API Manager Databases of your AM 1.6.0 instance.

3. Execute relevant sql script in in here against your API Manager Database. 

4. Now point same WSO2 Carbon Database(User Store and Registry) and API Manager Databases of your AM 1.6.0 instance to AM 1.7.0. 
(Configure AM_1.6.0/repository/datasource/master-datasources.xml to point same databases configured in AM 1.6.0)

5. Move your synapse configurations to APIM_1.7.0. For that, copy and replace APIM_1.6.0/repository/deployment/server/synapse-configs/default directory to APIM_1.7.0/repository/deployment/server/synapse-configs/default. Do not replace _TokenAPI_.xml, _RevokeAPI_.xml and _AuthorizeAPI_.xml files in the default/api subdirectory.

6. Copy the <APIM_1.7.0_HOME>/dbscripts/migration-1.6.0_to_1.7.0/api-migration directory to <APIM_1.7.0_HOME>(The new directory path will now be <APIM_1.7.0_HOME>/api-migration).

7. Configure api-migration/build.xml with the information for the below property.

   apim.home= Path to AM pack location [In a distributed setup, give the Gateway node path]     

8. Go inside api-migration/ and execute "ant run". You should get a "BUILD SUCCESSFUL" message if it ran correctly. 

9. Start AM 1.7.0 and Login.

10. Change the registry extension file[api.rxt file] with the new one[which can be found from /rxt/api.rxt] using management console. Navigate to path 'Home-> Extensions-> Configure-> Artifact Types' from management console and click the link 'View/Edit' and replace above mentioned new api.rxt and save.

11. Change the registry extension file[documentation.rxt file] with the new one[which can be found from /rxt/documentation.rxt] using management console. Navigate to path 'Home-> Extensions-> Configure-> Artifact Types' from management console and click the link 'View/Edit' and replace above mentioned new documentation.rxt and save.

12. Copy the <APIM_1.7.0_HOME>/dbscripts/migration-1.6.0_to_1.7.0/swagger-resource-migration directory to <APIM_1.7.0_HOME>(The new directory path will now be <APIM_1.7.0_HOME>/swagger-resource-migration).

13. Configure swagger-resource-migration/build.xml with the information for the below properties.

   registry.home= Path to AM pack location [In a distributed setup, give the Publisher node path]   
   username= Username for the AM server
   password= Password for the AM server
   host= IP of running AM server [In a distributed setup, give the host of the Publisher node]   
   port= Port of running AM server [In a distributed setup, give the port of the Publisher node]   
   version= Version of AM server

14. Go inside swagger-resource-migration/ and execute "ant run". You should get a "BUILD SUCCESSFUL" message if it ran correctly.

15. Copy the <APIM_1.7.0_HOME>/dbscripts/migration-1.6.0_to_1.7.0/doc-file-migration directory to <APIM_1.7.0_HOME>(The new directory path will now be <APIM_1.7.0_HOME>/doc-file-migration).

16.Configure doc-file-migration/build.xml with the information for the below properties.

   registry.home= Path to AM pack location [In a distributed setup, give the Publisher node path]   
   username= Username for the AM server
   password= Password for the AM server
   host= IP of running AM server [In a distributed setup, give the host of the Publisher node]   
   port= Port of running AM server [In a distributed setup, give the port of the Publisher node]   
   version= Version of AM server

17 Go inside doc-file-migration/ and execute "ant run". You should get a "BUILD SUCCESSFUL" message if it ran correctly.

18. If you have configured any external stores under <ExternalAPIStores> configuration in AM_1.6.0/repository/conf/api-manager.xml, follow below steps.
	9.1 Login to management console and go to Resources -> Browse
	9.2 Load /_system/governance/apimgt/externalstores/external-api-stores.xml resource in the registry browser and configure your external stores there and save.

19. If you have configured any Google Analytics under <GoogleAnalyticsTracking> configuration in AM_1.6.0/repository/conf/api-manager.xml, follow below steps.
	10.1 Login to management console and go to Resources -> Browse
	10.2 Load /_system/governance/apimgt/statistics/ga-config.xml resource in the registry browser and configure your Google analytics there and save.

20. If you have configured any Workflows under <WorkFlowExtensions> configuration in AM_1.6.0/repository/conf/api-manager.xml, follow below steps.
	11.1 Login to management console and go to Resources -> Browse
	11.2 Load /_system/governance/apimgt/applicationdata/workflow-extensions.xml resource in the registry browser and configure your Workflows there and save.


Tenant Migration (Only needs to be done if you are migrating a multi-tenanted setup)
====================================================================================

1. Move your tenant synapse configurations to APIM_1.7.0. For that, copy and replace specific folders for tenants(shown as 1,2,...) from APIM_1.6.0/repository/tenants/ to APIM_1.7.0/repository/tenants. Do not replace _TokenAPI_.xml, _RevokeAPI_.xml and _AuthorizeAPI_.xml files in the default/api subdirectory.

2. Start AM 1.7.0 and Login to the respective tenant space

3. Change the registry extension file[api.rxt file] with the new one[which can be found from /rxt/api.rxt] using management console. Navigate to path 'Home-> Extensions-> Configure-> Artifact Types' from management console and click the link 'View/Edit' and replace above mentioned new api.rxt and save.

4. Change the registry extension file[documentation.rxt file] with the new one[which can be found from /rxt/documentation.rxt] using management console. Navigate to path 'Home-> Extensions-> Configure-> Artifact Types' from management console and click the link 'View/Edit' and replace above mentioned new documentation.rxt and save.

5. Configure swagger-resource-migration/build.xml with the information for the below properties. (swagger-resource-migration folder should be already copied to <APIM_1.7.0_HOME>)

   registry.home= Path to AM pack location [In a distributed setup, give the Publisher node path]   
   username= Username for the AM server - respective tenant space
   password= Password for the AM server - respective tenant space
   host= IP of running AM server [In a distributed setup, give the host of the Publisher node]   
   port= Port of running AM server [In a distributed setup, give the port of the Publisher node]   
   version= Version of AM server

6. Go inside swagger-resource-migration/ and execute "ant run". You should get a "BUILD SUCCESSFUL" message run correctly.

7. Configure doc-file-migration/build.xml with the information for the below properties. (doc-file-migration folder should be already copied to <APIM_1.7.0_HOME>)

   registry.home= Path to AM pack location [In a distributed setup, give the Publisher node path]   
   username= Username for the AM server - respective tenant space
   password= Password for the AM server - respective tenant space
   host= IP of running AM server [In a distributed setup, give the host of the Publisher node]   
   port= Port of running AM server [In a distributed setup, give the port of the Publisher node]   
   version= Version of AM server

8. Go inside doc-file-migration/ and execute "ant run". You should get a "BUILD SUCCESSFUL" message if it ran correctly.

9. For the respective tenant if you have configured any external stores under <ExternalAPIStores> configuration in AM_1.6.0/repository/conf/api-manager.xml, follow below steps.
	9.1 Login to management console and go to Resources -> Browse
	9.2 Load /_system/governance/apimgt/externalstores/external-api-stores.xml resource in the registry browser and configure your external stores there and save.

10. For the respective tenant if you have configured any Google Analytics under <GoogleAnalyticsTracking> configuration in AM_1.6.0/repository/conf/api-manager.xml, follow below steps.
	10.1 Login to management console and go to Resources -> Browse
	10.2 Load /_system/governance/apimgt/statistics/ga-config.xml resource in the registry browser and configure your Google analytics there and save.

11. For the respective tenant if you have configured any Workflows under <WorkFlowExtensions> configuration in AM_1.6.0/repository/conf/api-manager.xml, follow below steps.
	11.1 Login to management console and go to Resources -> Browse
	11.2 Load /_system/governance/apimgt/applicationdata/workflow-extensions.xml resource in the registry browser and configure your Workflows there and save.



