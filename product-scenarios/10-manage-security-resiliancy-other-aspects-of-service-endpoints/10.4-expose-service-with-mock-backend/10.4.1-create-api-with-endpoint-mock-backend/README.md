# 10.4.1 Create an API with an endpoint to mock the backend service

## When to use this approach
When an API Developer want to expose an API via a mock endpoint which does not have a proper backend implemented yet but the developers need an early implementation of the API that they can try out without a subscription or monetization, and provide feedback to improve.

## Sample use case
An API developer wants to expose an API via a mock endpoint for the purpose of early promotion and testing. To do that developer would go to the API publisher and create a Prototype REST or WebSocket API by entering API name, version, context, and at least one resource, and provide an endpoint to a prototype API backend.

## Supported versions
All

## Pre-requisites
User can use API publisher web application to implement entire flow

## Development guidelines
Start wso2am-2.6.0 distribution by executing [APIM_HOME]/bin/wso2server.sh or [APIM_HOME]/bin/wso2server.bat 
Then user will need to login to API publisher as API creator. Once logged in, the user will need to create either a REST or WebSocket prototype API. Enter the mandatory fields such API name, version, context, visibility and one resource to create one basic API. Once entered these fields user will be able to save the details(Click on 'SAVE' button)
So that user should be able to see newly created API with status as 'CREATED'. Then user will need to go to **Implement** tab and select **Endpoint** as implementation method and specify the endpoint URL of the mock backend. Then click **Deploy as a Prototype**.

## Sample Configuration
No additional configuration or data to be added to servers.

## Deployment guidelines
API Manager 2.6.0 deployment required. No additional artifact or data to be added to servers.

## REST API (if available)
