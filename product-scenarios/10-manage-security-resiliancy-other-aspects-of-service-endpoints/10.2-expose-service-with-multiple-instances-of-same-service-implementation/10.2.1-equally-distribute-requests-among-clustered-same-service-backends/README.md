# 10.2.1 Equally distribute the requests among clustered backends of same service

## When to use this approach
When an API Developer want to equally distribute the request among clustered backends of same service.

## Sample use case
An API developer wants to make sure that the incoming API requests are equally distributed among the clustered backends of same service implementation based on a load balancing algorithm. To do that developer would go to the API publisher and create an API by entering API name, version, context, and at least one resource, and specify load balance endpoints with session management configurations.

## Supported versions
All

## Pre-requisites
User can use API publisher web application to implement entire flow

## Development guidelines
Start wso2am-2.6.0 distribution by executing [APIM_HOME]/bin/wso2server.sh or [APIM_HOME]/bin/wso2server.bat 
Then user will need to login to API publisher as API creator. Once logged in, the user will need to enter the mandatory fields such API name, version, context, visibility and one resource to create one basic API. Once entered these fields user will be able to save the details(Click on 'SAVE' button)
So that user should be able to see newly created API with status as 'CREATED'. Then user will need to update the endpoint implementation details by configuring load balancing endpoints. Update the remaining fields to complete the flow up to publish the API. 

## Sample Configuration
No additional configuration or data to be added to servers.

## Deployment guidelines
API Manager 2.6.0 deployment required. No additional artifact or data to be added to servers.

## REST API (if available)
