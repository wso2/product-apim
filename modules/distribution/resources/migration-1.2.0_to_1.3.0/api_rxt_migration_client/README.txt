Migration Instructions
======================

1. Change the registry extension file[.rxt file] with the new one[which can be found from /rxt/api.rxt] using management console.Navigate to path 'Home-> Extensions-> Configure-> Artifact Types' from management console and click the link 'View' and replace above mentioned new api.rxt and save.
NOTE : You need to replace only the <Content> section of the new api.rxt.

2. Run the relevant db-script from 'migration-1.2.0_to_1.3.0'.

3. Configure build.xml with the information for the below properties.
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

4. Start AM server[In a distributed setup,it's the Publisher node]  if its not already started.
5. Run "ant run" to migrate.
6. Sign-out and Sign-in to management console to check whether the data migrated.
7. Check from the database,the newly added data has populated to new table 'AM_API_URL_MAPPING'. 
