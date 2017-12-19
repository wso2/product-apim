# WSO2 API Management

WSO2 API Management is an Open Source API Management tool that allows APIs/Services to be Secured, Monitored and Rate Controlled. This tool allows API Developers to design, publish, version and manage API lifecycles. It allows application developers to discover and start consuming APIs. The lightweight API Gateway intercepts all API requests applying Security, Rate Limiting and Monitoring aspects on your APIs.

More information about this product can be found [here](http://wso2.com/api-management/)

## Building from source

1. Download and install JDK 8 update 77 or later
2. Get a clone or download the source from this repository (https://github.com/wso2/carbon-apimgt)
3. Run the Maven command ``mvn clean  install`` from within the carbon-apimgt directory.
4. Get a clone or download the source from this repository (https://github.com/wso2/product-apim)
5. Run the Maven command ``mvn clean install`` from within the product-apim directory.

## Running the product

### Running Apache ActiveMQ
1. Download and install Apache ActiveMQ 5.14.0 from [here](http://activemq.apache.org/activemq-5140-release.html)
2. Start ActiveMQ using `` ./bin/activemq start ``

### Running WSO2 Identity Server
1. Download the WSO2 Identity Server 5.4.0 Alpha10 or above from [here](http://wso2.com/identity-and-access-management#download)
2. Extract the downloaded wso2is-5.4.0.zip file
3. Go to the wso2is-5.4.0/bin directory and execute the wso2server.sh script using the command ``./wso2server.sh start``

### Running WSO2 API Manager Analytics
1. Download the Analytics distribution from [releases](https://github.com/wso2/product-apim/releases) OR if you built from source, find the binary from product-apim/analyzer/target.
2. Extract the distribution. Ex: unzip wso2apim-das-${project.version}.zip
3. Go to the wso2apim-das-${project.version}/bin directory and execute the carbon.sh script: ``./worker.sh``

### Running WSO2 API Manager
1. Download the product distribution from [releases](https://github.com/wso2/product-apim/releases) OR if you built from source, find the binary from product-apim/product/target.
2. Extract the distribution. Ex: unzip wso2apim-${project.version}.zip
3. Go to the default runtime dir wso2apim-${project.version}/wso2/default/bin directory and execute the carbon.sh script: ``./carbon.sh``

### Running WSO2 API Manager Gateway
1. Download the API Manager gateway distribution from [releases](https://github.com/wso2/product-apim/releases) OR if you built from source, find the binary from product-apim/gateway/target.
2. Extract the distribution. Ex: unzip wso2apim-gateway-${project.version}.zip
3. Go to the wso2apim-gateway-${project.version} directory and execute ``bin/ballerina run service services.bsz``

#### Note:

* Make sure you start the Gateway from ``wso2apim-gateway-3.0.0`` root directory; not inside ``/bin`` directory.
* Make sure you start the Gateway at the end, after starting all the other servers.
* After Publishing an API to gateway make sure that you restart the gateway by executing ``bin/ballerina run service services.bsz org/wso2/carbon/apimgt/gateway``

### Testing

* Open your web browser and type the URL https://localhost:9292/publisher to go to the API Publisher application which allows you to design and publish APIs.
* On the web browser type the URL https://localhost:9292/store to visit the API Store to discover and consume APIs.
