WSO2 API Migration Tool - Migrate APIManager 1.8.0 to 1.9.0
===========================================================

This is used to migrate APIs created using different versions of WSO2 API Manager.
This Client is only supported for APIM 1.8.0 to 1.9.0 Migrations

Follow the steps below
    - Visit https://docs.wso2.com/display/AM190/Upgrading+from+the+Previous+Release
    - Follow the given instructions

This client can be used to,
    - Database Migrations
    - Registry Resource Migrations (Swagger, RXT and other docs in the registry)
    - File System Migrations (Synapse configs, Synapse APIs etc)

How to use,
    - Start the server with -Dmigrate=<MIGRATE_VERSION> for all the migrations
            For example -Dmigrate=1.9 for migrate to API Manager 1.9.0
    - Start the server with -DmigrateDB=true for migrate only the database resources
    - Start the server with -DmigrateReg=true for migrate only the registry resources
    - Start the server with -DmigrateFS=true for migrate only the file system resources
    - Start the server with -Dcleanup=true to cleanup old resources.
            Make sure you run this command after a successful migration. Otherwise you will lose all of your resources.


