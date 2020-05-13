# API Manager Product Scenarios

## [01. [SAMPLE] Rollout web services / API updates seamlessly.](https://github.com/wso2/product-apim/tree/product-scenarios/product-scenarios/1-api-updates-using-new-versions) 
This is a sample scenario.

Let's take a scenario where we need to push service update seamlessly without impacting existing users. If we have some service which exposed as API to outside and there are active users for that API we cannot simply push changes to that API as it breaks existing clients. When API creator created updated version of same existing API(version 1.0.0) he can create new version(version 1.1.0) of API. This new API can have few additional resources than the original API(which is version 1.0.0). If any new user need to use newly added API resources that user need to use 1.1.0 version of the API.


## [01. Managing Public, Partner and Private APIs](https://github.com/wso2/product-apim/tree/product-scenarios/product-scenarios/1-manage-public-partner-private-apis)
<bussiness use case>
An API Developer would want to use the API Management solution to manage Public, Private and Partner APIs. So that ..

## How to Build
### 1. Maven build
Maven build should be executed with the test suite specific to each product version to be tested. This can be provided by `-DsuiteXmlFile` build parameter.  
Path to testng.xml file should be relative to the maven module  
Ex:  
`mvn clean install -DsuiteXmlFile=src/test/resources/testng.xml`
### 2. Running test</span>.sh
`--input-dir` parameter should be provided. This directory should contain `deployment.properties` file including the property `ProductVersion`.
Ex:  
`test</span>.sh --input-dir ~/Documents/apimgt/`