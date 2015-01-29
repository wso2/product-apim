Migration Instructions
======================

APIM 1.4.0 release has new API Documentation feature based on Swagger. When new API is created in APIM 1.4.0, The API Definition for that API is saved as a registry resource. When API store is loading it looks for this registry resource. This migration client will add the registry resource for existing APIs. 

1. Configure build.xml with the information for the below properties.

   registry.home= Path to AM pack location [In a distributed setup, give the Store node path]   
   username= Username for the AM server
   password= Password for the AM server
   host= IP of running AM server [In a distributed setup, give the host of the Store node]   
   port= Port of running AM server [In a distributed setup, give the port of the Store node]   
   gateway_host= IP of AM Gateway server [In a standalone setup, give the same value given for the 'host']   
   gateway_port= Port of AM Gateway server [In a standalone setup, give the same value given for the 'port']   
   version= Version of AM server
   dbUrl =Database jdbc url of the AM database
   dbDriver =Database driver name of the AM database
   sql.dir= Path to JDBC driver jar location  
   dbUsername=AM database username
   dbPassword=AM database password   

3. Start AM server[In a distributed setup,it's the Store node]  if its not already started.
4. Run "ant run" to migrate.
5. Go to API Store and click on APIs. You should see the swagger UI loaded in Try-it Tab. 
