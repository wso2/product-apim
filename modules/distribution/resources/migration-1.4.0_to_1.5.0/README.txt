Data Migration 1.4.0 to 1.5.0 
=============================

1. Shutdown AM 1.4.0 if it is running. 

2. Backup your WSO2 Carbon Database(User Store and Registry) and API Manager Databases of your AM 1.4.0 instance.

3. Execute relevant sql script in 'userstore_db' directory against your WSO2 Carbon Database. This will migrate tables and data in your jdbc user store

4. Execute relevant sql script in 'apimgt_db' directory against your API Manager Database. 

5. Now point same WSO2 Carbon Database(User Store and Registry) and API Manager Databases of your AM 1.4.0 instance to AM 1.5.0. 
(Configure AM_1.5.0/repository/datasource/master-datasources.xml to point same databases configured in AM 1.4.0)

6. Move all your synapse configurations to APIM_1.5.0. For that, copy and replace APIM_1.4.0/repository/deployment/server/synapse-configs/default directory to APIM_1.5.0/repository/deployment/server/synapse-configs/default

7. Open the AM_1.5.0/repository/conf/user-mgt.xml file and add the property

<Property name="CaseSensitiveAuthorizationRules">true</Property> to existing 'AuthorizationManager' configuration.

ex:

        <AuthorizationManager class="org.wso2.carbon.user.core.authorization.JDBCAuthorizationManager">
            <Property name="AdminRoleManagementPermissions">/permission</Property>
	    <Property name="AuthorizationCacheEnabled">true</Property>
	    <Property name="CaseSensitiveAuthorizationRules">true</Property>
        </AuthorizationManager>

8. Start APIM 1.5.0

