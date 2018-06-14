WSO2 API Migration Tool - Migrate API Manager 
===========================================================

This is used to migrate APIs created using different versions of WSO2 API Manager.

Follow the steps below:
    - Visit https://docs.wso2.com/display/AM250/Upgrading+from+the+Previous+Release
    - Follow the given instructions

This client can be used for,
    - Database migrations
    - Registry resource migrations (Swagger, RXT and other docs in the registry)
    - File system migrations

    - Start the server with -Dmigrate=<MIGRATE_VERSION> for all the migrations

For example -migrate=1.9 for migrate to API Manager 1.9.0
    - Start the server with -DmigrateDB=1.9 for migrate only the database resources
    - Start the server with -DmigrateReg=1.9 for migrate only the registry resources
    - Start the server with -DmigrateFS=1.9 for migrate only the file system resources
    - Start the server with -Dcleanup=true to cleanup old resources.

Make sure you run this command after a successful migration. Otherwise, you will lose all of your resources.


