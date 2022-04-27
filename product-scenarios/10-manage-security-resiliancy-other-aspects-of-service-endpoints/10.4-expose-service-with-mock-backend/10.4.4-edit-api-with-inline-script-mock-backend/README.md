# 10.4.4 Edit an API with an inline script to mock the backend service

## When to use this approach
When an API Developer want to update an API which is deployed via an inline script which does not have a proper backend implemented yet but the developers need an early implementation of the API for early promotion and testing

## Sample use case
An API developer wants to update an API deployed via an inline script. To do that developer would go to the API publisher and edit the Prototyped API by updating the inline script to a prototype API backend.

## Supported versions
All

## Pre-requisites
User can use API publisher web application to implement entire flow

## Development guidelines
Start wso2am-2.6.0 distribution by executing [APIM_HOME]/bin/wso2server.sh or [APIM_HOME]/bin/wso2server.bat 
Then user will need to login to API publisher as API creator. Once logged in, the user will need to edit the prototype API. Go to **Implement** tab and update the inline script of the mock backend. Then click **Save** and **Deploy as a Prototype**.

## Sample Configuration
No additional configuration or data to be added to servers.

## Deployment guidelines
API Manager 2.6.0 deployment required. No additional artifact or data to be added to servers.

## REST API (if available)
