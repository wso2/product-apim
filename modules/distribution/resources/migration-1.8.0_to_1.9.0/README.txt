Data Migration 1.8.0 to 1.9.0
=============================

1. Shutdown AM 1.8.0 if it is running.

2. Backup your API Manager Databases of your AM 1.8.0 instance.

3. Execute relevant sql script in in here against your API Manager Database.

4. Now point same WSO2 Carbon Database(User Store and Registry) and API Manager Databases of your AM 1.8.0 instance to AM 1.9.0.
(Configure AM_1.9.0/repository/datasource/master-datasources.xml to point same databases configured in AM 1.8.0)

5. Move your synapse configurations to APIM_1.9.0. For that, copy and replace APIM_1.8.0/repository/deployment/server/synapse-configs/default directory to APIM_1.9.0/repository/deployment/server/synapse-configs/default. Do not replace _TokenAPI_.xml, _RevokeAPI_.xml and _AuthorizeAPI_.xml files in the default/api subdirectory

6. Start AM 1.9.0 and Login.

7. To re-index log in to carbon console (ex: http://localhost:9443/carbon) and delete 'lastaccesstime' resource in '/_system/local/repository/components/org.wso2.carbon.registry/indexing' location. For that go to Home-> Resources->Browse and navigate to the above given location. You can delete the 'lastaccesstime' resource by selecting Actions-> Delete

8. shutdown AM 1.9.0 and delete <APIM_1.9.0_HOME>/repository/conf/solr directory and restart the server. 


Tenant Migration (Only needs to be done if you are migrating a multi-tenanted setup)
====================================================================================

1. Move your tenant synapse configurations to APIM_1.9.0. For that, copy and replace specific folders for tenants(shown as 1,2,...) from APIM_1.8.0/repository/tenants/ to APIM_1.9.0/repository/tenants. Do not replace _TokenAPI_.xml, _RevokeAPI_.xml and _AuthorizeAPI_.xml files in the default/api subdirectory.

2. Start AM 1.9.0.


