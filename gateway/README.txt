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
2. Execute the below command.
``bin/ballerina run service services.bsz``

WSO2 API Gateway ${project.version} distribution directory structure

    GW_HOME
    .
    ├── bin <folder>
    ├── bre <folder>
    │   ├── conf <folder>
    │   ├── lib <folder>
    │   └── security <folder>
    ├── handler <folder>
    ├── LICENSE.txt <folder>
    ├── logs <folder>
    ├── microgateway <folder>
    ├── microgateway.bsz <file>
    ├── org <folder>
    ├── README.txt <file>
    ├── services.bsz <folder>
    └── src <folder>


    - bin
      Contains various scripts .sh & .bat scripts

    - bre
      Contains Ballerina Runtime Environment related libraries, configuration and resources
      
       - conf
         Contains Ballerina Runtime Environment related libraries, configuration
        
       - lib
         Contains libraries required for Ballerina Runtime Environment
       
       - security
         Contains trust-store and keystore files

    - handler
      Contains request interceptors used for authentication, publishing statistics and etc.

    - logs
      Server logs.
    
    - microgateway
      Micro Gateway configurations.

    - org
      The package which contains published API ballerina files.

    - microgateway.bsz
      Ballerina package that contains required sources for gateway to work as a Micro Gateway.

    - services.bsz
      Ballerina package that contains required sources for gateway to work in default mode.

    - src
      Required native ballerina files

    - LICENSE.txt
      Apache License 2.0 and the relevant other licenses under which WSO2 GW is distributed

    - README.txt
      This document.
