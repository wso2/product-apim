WSO2 API Gateway ${project.version}

This is WSO2 API Gateway powered by Ballerina. 


Building from the source

If you want to build APIM Gateway from the source code:

1. Install Java 8(http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
2. Install Apache Maven 3.x.x(https://maven.apache.org/download.cgi#)
3. Get a clone or download the source from this repository (https://github.com/wso2/product-apim).
4. Run the Maven command ``mvn clean install`` from the ``product-apim/gateway`` directory.
5. Extract the WSO2 APIM Gateway distribution created at `product-apim/gateway/target/wso2apim-gateway-${project.version}.zip` to your local directory.

Starting the server

1. Go to `product-apim/gateway/target/wso2apim-gateway-${project.version}` directory
1. Execute the below command.
``bin/ballerina run service services.bsz``

