Migration Instructions
======================

API comments and ratings were kept in registry before the AM-1.4.0 release. In AM-1.4.0 release, those have moved to api-manager database. This migration client will add the existing comments and ratings of API's to the database.

1. If you haven't already run the relavant database migration scripts found in '{AM_HOME}/dbscripts/migration-1.3.0_to_1.4.0/' directory, first run this script. It will create 'AM_API_COMMENTS' and 'AM_API_RATINGS' tables, that will required for this migration client and additionlly it will do some other database migrations as well. 

2. Configure build.xml with the information for the below properties.
   registry.home= Path to AM pack location [In a distributed setup,give the Publisher node path]   
   username= Username for the AM server
   password= Password for the AM server
   host= IP of running AM server
   port= Port of running AM server
   version= Version of AM server
   sql.dir= Path to JDBC driver jar location  
   dbUrl =Database jdbc url of the AM database
   dbDriver =Database driver name of the AM database
   dbUsername=AM database username
   dbPassword=AM database password   

3. Start AM server[In a distributed setup,it's the Publisher node]  if its not already started.
4. Run "ant run" to migrate.
5. Check from the database,'AM_API_COMMENTS' and 'AM_API_RATINGS' tables should have populated with data.
