# WSO2 API Migration Client
This is used to migrate APIs created using different versions of WSO2 API Manager. This client only supports for the versions later that 1.6.0
This Client is only supported for immediate version migrations at a time.

This client can be used to, 
    - Database Migrations
    - Registry Resource Migrations (Swagger, RXT and other docs in the registry)
    - File System Migrations

How to use,
    - Start the server with -Dmigrate=<MIGRATE_VERSION> for all the migrations (For example -Dmigrate=1.9 for migrate to API Manager 1.9.0)
    - Start the server with -DmigrateDB=true for migrate only the database resources
    - Start the server with -DmigrateReg=true for migrate only the registry resources
    - Start the server with -DmigrateFS=true for migrate only the file system resources
    - Start the server with -Dcleanup=true to cleanup old resources. Make sure you run this command after a successful migration. Otherwise you will lose all of your resources.
    
