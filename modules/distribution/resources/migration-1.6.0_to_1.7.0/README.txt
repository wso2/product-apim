Data Migration 1.7.0 to 1.7.0 
=============================

1. Shutdown AM 1.6.0 if it is running. 

2. Backup your API Manager Databases of your AM 1.6.0 instance.

3. Execute relevant sql script in in here against your API Manager Database. 

4. Now point same WSO2 Carbon Database(User Store and Registry) and API Manager Databases of your AM 1.6.0 instance to AM 1.7.0. 
(Configure AM_1.6.0/repository/datasource/master-datasources.xml to point same databases configured in AM 1.6.0)

5. Move all your synapse configurations to APIM_1.6.0. For that, copy and replace APIM_1.6.0/repository/deployment/server/synapse-config/default directory to APIM_1.7.0/repository/deployment/server/synapse-config/default

6. Start AM 1.7.0 and Login.

7. Change the registry extension file[api.rxt file] with the new one[which can be found from /rxt/api.rxt] using management console. Navigate to path 'Home-> Extensions-> Configure-> Artifact Types' from management console and click the link 'View/Edit' and replace above mentioned new api.rxt and save.

8. Change the registry extension file[documentation.rxt file] with the new one[which can be found from /rxt/documentation.rxt] using management console. Navigate to path 'Home-> Extensions-> Configure-> Artifact Types' from management console and click the link 'View/Edit' and replace above mentioned new documentation.rxt and save.


8. Configure swagger-resource-migration/build.xml with the information for the below properties.

   registry.home= Path to AM pack location [In a distributed setup, give the Publisher node path]   
   username= Username for the AM server
   password= Password for the AM server
   host= IP of running AM server [In a distributed setup, give the host of the Publisher node]   
   port= Port of running AM server [In a distributed setup, give the port of the Publisher node]   
   version= Version of AM server

9. Go inside swagger-resource-migration/ and execute "ant run". You should get a "BUILD SUCCESSFUL" message if it ran correctly.

10. If you have configured any external stores under <ExternalAPIStores> configuration in AM_1.6.0/repository/conf/api-manager.xml, follow below steps.
	9.1 Login to management console and go to Resources -> Browse
	9.2 Load /_system/governance/apimgt/externalstores/external-api-stores.xml resource in the registry browser and configure your external stores there and save.

11. If you have configured any Google Analytics under <GoogleAnalyticsTracking> configuration in AM_1.6.0/repository/conf/api-manager.xml, follow below steps.
	10.1 Login to management console and go to Resources -> Browse
	10.2 Load /_system/governance/apimgt/statistics/ga-config.xml resource in the registry browser and configure your Google analytics there and save.

12. If you have configured any Workflows under <WorkFlowExtensions> configuration in AM_1.6.0/repository/conf/api-manager.xml, follow below steps.
	11.1 Login to management console and go to Resources -> Browse
	11.2 Load /_system/governance/apimgt/applicationdata/workflow-extensions.xml resource in the registry browser and configure your Workflows there and save.
