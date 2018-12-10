# 4.1.2 Deleting an application

## When to use this approach
When an application needs to be deleted

## Sample use case
An Application developer wants to delete an application, and to do that developer would go to the API store and delete 
the desired application. If the application has subscriptions to APIs they will be removed.

## Supported versions
All

## Pre-requisites
User can use API store and API publisher web application to implement the flow

## Development guidelines
Start wso2am-2.6.0 distribution by executing [APIM_HOME]/bin/wso2server.sh or [APIM_HOME]/bin/wso2server.bat 
Then user will need to login to API store. Once logged in, user can delete applications owned by him. 

## Sample Configuration
No additional configuration or data to be added to servers.

## Deployment guidelines
API Manager 2.6.0 deployment required. No additional artifact or data to be added to servers.

## REST API (if available)
https://docs.wso2.com/display/AM260/apidocs/store/
https://docs.wso2.com/display/AM260/apidocs/publisher/
