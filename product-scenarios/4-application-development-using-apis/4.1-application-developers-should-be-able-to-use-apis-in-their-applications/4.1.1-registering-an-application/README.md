# 4.1.1 Registering an application

## When to use this approach
When an Application want to use APIs in their applications

## Sample use case
An Application developer wants to create an application, and to do that developer would go to the API store and create an application by providing the mandatory and non-mandatory values.

## Supported versions
All

## Pre-requisites
User can use API publisher web application to implement entire flow

## Development guidelines
Start wso2am-2.6.0 distribution by executing [APIM_HOME]/bin/wso2server.sh or [APIM_HOME]/bin/wso2server.bat 
Then user will need to login to API store. Once logged in, user can create an application by providing application name, policy, token type and description(optional).Once created 
application will be listed under 'Applications' in API store. 

## Sample Configuration
No additional configuration or data to be added to servers.

## Deployment guidelines
API Manager 2.6.0 deployment required. No additional artifact or data to be added to servers.

## REST API (if available)
https://docs.wso2.com/display/AM260/apidocs/store/
