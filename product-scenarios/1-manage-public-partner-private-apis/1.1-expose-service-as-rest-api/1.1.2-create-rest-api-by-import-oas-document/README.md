# 1.1.2 Create REST API by importing an OAS document

## When to use this approach
When an API Developer want to expose their service as REST API which are Public, Private and Partner APIs

## Sample use case
An API developer wants to expose their service as public rest API, and to do that developer would go to the API publisher and create an API by importing OAS document as a file or a URL, and also the visibility as 'public'.

## Supported versions
All

## Prerequisites
User can use API publisher web application to implement entire flow

## Development guidelines
Start wso2am-2.6.0 distribution by executing [APIM_HOME]/bin/wso2server.sh or [APIM_HOME]/bin/wso2server.bat
Then user will need to login to API publisher as API creator. Once logged in,the user will need to upload or import OAS document of an API to be exposed.
Once entered these fields user will be abel to save the details(Click on 'SAVE' button)So that user should be able to see newly created API with status as 'CREATED'. User can now continue to update remaining fields to complete the flow up to publish the API.

## Sample Configuration
No additional configuration or data to be added to servers.

## Deployment
API Manager 2.6.0 deployment required. No additional artifact or data to be added to servers.

## Testing and Acceptance Criteria
OAS 2.0 document as a JSON file or URL to JSON file
OAS 3.0 document as a JSON file or URL to JSON file
OAS 2.0 document as a YAML file or URL to YAML file
OAS 3.0 document as a YAML file or URL to YAML file

## API Reference


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
N/A