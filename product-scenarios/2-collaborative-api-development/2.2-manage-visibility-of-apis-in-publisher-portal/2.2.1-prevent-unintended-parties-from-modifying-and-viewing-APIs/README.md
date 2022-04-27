2.2.1 prevent unintended parties from modifying and viewing APIs

 ## When to use this approach
When an API creator want to prevent the visibility of api in publisher from unintended parties

 ## Sample use case
An API creator wants to restrict the accessibility of API for unintended parties, and to do that API creator should create the API with publisher access control for required roles.
Then the user with that required role only can visible the API in publisher

 ## Supported versions
All

 ## Pre-requisites
User can use API publisher web application to implement entire flow

 ## Development guidelines
Start wso2am-2.6.0 distribution by executing [APIM_HOME]/bin/wso2server.sh or [APIM_HOME]/bin/wso2server.bat 
Then user will need to login to API publisher as API creator and create API with restricting the publisher access control for particular roles.Once another user with that role logged in to the publisher, that user will see that created API and the user without that role will no be able to see the API in publisher.

 ## Sample Configuration
No additional configuration or data to be added to servers.

 ## Deployment guidelines
API Manager 2.6.0 deployment required. No additional artifact or data to be added to servers.

 ## REST API (if available)
https://docs.wso2.com/display/AM260/apidocs/publisher/