
PRE-REQUESTIES
===============
1)WSO2 Application Server 4.1.2
2)WSO2 API Manager
  Change default token expiration time in identity.xml to one year (3153600)

INSTRUCTIONS
==============

1) Define following APIs in the API Publisher app & push to gateway. (will be deploying the actual service implementation from step 3 onwards)

   Delivery API

        API Name= pizzaShack
        Context = /pizzashack/delivery
        Version = 1.0.0
        Production Endpoint URL=http://localhost:9765/pizzashack-api-1.0.0/api/delivery
        API Resources =Keep the default values 

   Order API
        
        API Name= pizzashack-order
        Context = /pizzashack/order
        Version = 1.0.0
        Production Endpoint URL=http://localhost:9765/pizzashack-api-1.0.0/api/order
        API Resources =Keep the default values 

   Menu API

        API Name= pizzashack-menu
        Context = /pizzashack/menu
        Version = 1.0.0
        Production Endpoint URL=http://localhost:9765/pizzashack-api-1.0.0/api/menu
        API Resources =Keep the default values 
	

2) Subscribe through default application & generate production key.

3) Replace the consumer key & secret generated in step 2 in /pizza-shack-web/src/main/webapp/WEB-INF/web.xml

4) The Server Url in  pizza-shack-web Web.xml to be to 8280 (assuming offset 0)

5) Run maven to deploy the application.‘mvn clean install’ at the directory '/pizza-shack-web' and '/pizza-shack-api'

6) Deploy pizza-shack-web/target/pizzashack.war  & pizza-shack-api/target/pizzashack-api-1.0.0.war into App Server

7) App Server should run with port offset 2 .

8) Access the application @ http://localhost:9765/pizzashack

9) Login using admin/admin (or any user account you used for subscription)

