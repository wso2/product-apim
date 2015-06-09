Data Migration 1.5.0 to 1.6.0 
=============================

1. Shutdown AM 1.5.0 if it is running. 

2. Backup your API Manager Databases of your AM 1.5.0 instance.

3. Execute relevant sql script in in here against your API Manager Database. 

4. Now point same WSO2 Carbon Database(User Store and Registry) and API Manager Databases of your AM 1.5.0 instance to AM 1.6.0. 
(Configure AM_1.5.0/repository/datasource/master-datasources.xml to point same databases configured in AM 1.4.0)

5. Move all your synapse configurations to APIM_1.6.0. For that, copy and replace APIM_1.5.0/repository/deployment/server/synapse-configs/default directory to APIM_1.6.0/repository/deployment/server/synapse-configs/default

6. Start AM 1.6.0 and Login.

7. Change the registry extension file[.rxt file] with the new one[which can be found from /rxt/api.rxt] using management console. Navigate to path 'Home-> Extensions-> Configure-> Artifact Types' from management console and click the link 'View/Edit' and replace above mentioned new api.rxt and save.

7. Configure endpoint-migration/build.xml with the information for the below properties.

   registry.home= Path to AM pack location [In a distributed setup, give the Publisher node path]   
   username= Username for the AM server
   password= Password for the AM server
   host= IP of running AM server [In a distributed setup, give the host of the Publisher node]   
   port= Port of running AM server [In a distributed setup, give the port of the Publisher node]   
   version= Version of AM server

8. Go inside endpoint-migration/ and execute "ant run". You should get a "BUILD SUCCESSFUL" message if it ran correctly.


