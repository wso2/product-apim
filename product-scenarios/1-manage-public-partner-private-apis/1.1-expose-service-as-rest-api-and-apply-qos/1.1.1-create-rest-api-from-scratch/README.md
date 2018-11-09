# 1.1.1 Create REST API from scratch

## When to use this approach
When an API Developer want to expose their service as REST API which are Public, Private and Partner APIs

## Sample use case
An API developer wants to expose their service as public rest API, and to do that developer would go to the API publisher and create an API by entering API name, version, context, and atleast one resource, and also the visibility as 'public'. 

## Supported versions
All

## Pre-requisites
User can use API publisher web application to implement entire flow

## Development guidelines
Start wso2am-2.6.0 distribution by executing [APIM_HOME]/bin/wso2server.sh or [APIM_HOME]/bin/wso2server.bat 
Then user will need to login to API publisher as API creator. Once logged in, the user will need to enter the mandatory fields such API name, version, context, visibilty and one resource to create one basic API. Once entered these fields user will be abel to save the details(Click on 'SAVE' button)
So that user should be able to see newly created API with status as 'CREATED'. User can now continue to update remaining fields to complete the flow up to publish the API.

## Sample Configuration
No additional configuration or data to be added to servers.

## Deployment guidelines
API Manager 2.6.0 deployment required. No additional artifact or data to be added to servers.

## REST API (if available)
https://docs.wso2.com/display/AM260/apidocs/publisher/
