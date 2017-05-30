WSO2 API Management
-------------------

WSO2 API Management is an Open Source API Management tool that allows APIs/Services to be Secured, Monitored and Rate Controlled. This tool allows API Developers to design, pubilsh, version and manage API lifecycles. It allows application developers to discover and start consuming APIs. The lightweight API Gateway intercepts all API requests applying Security, Rate Limiting and Monitoring aspects on your APIs.

More information about this product can be found [here](http://wso2.com/api-management/)

## Building from source
-----------------------

1. Get a clone or download the source from this repository (https://github.com/wso2/carbon-apimgt)
2. Run the Maven command ``mvn clean  install`` from within the carbon-apimgt directory.
3. Get a clone or download the source from this repository (https://github.com/wso2/product-apim)
4. Run the Maven command ``mvn clean install`` from within the product-apim directory.

## Running the product
----------------------

1. Download the WSO2 Identity Server from [here](http://wso2.com/identity-and-access-management#download)
2. Extract the downloaded wso2is-5.3.0.zip file
3. Go to the wso2is-5.3.0/bin directory and execute the wso2server.sh script using the command ``./wso2server.sh start``
4. Find the API Manager product distribution from product-apim/product/target
5. Extract the distribution. Ex: unzip wso2apim-3.0.0.zip
6. Go to the wso2apim/bin directory and execute the carbon.sh script
7. Open your web browser and type the URL https://localhost:9292/publisher to go to the API Publisher application which allows you to design and publish APIs.
8. On the web browser type the URL https://localhost:9292/store to visit the API Store to discover and consume APIs.
