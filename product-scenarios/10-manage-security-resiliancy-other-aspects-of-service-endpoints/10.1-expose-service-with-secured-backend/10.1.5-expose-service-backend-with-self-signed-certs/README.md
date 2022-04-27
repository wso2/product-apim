# 10.1.5 Expose a service backend with self signed certificates

## When to use this approach
When an API Developer want to expose their backend with a self signed certificate (or a certificate which is not signed by a CA) and when the user wants to upload such backend certificate dynamically through API Publisher.

## Sample use case
An API developer wants to expose their backend service with a self signed certificate (or a certificate which is not signed by a CA). To do that developer would go to the API publisher and create an API by entering API name, version, context, and at least one resource, and upload a certificate with an alias for the selected endpoint. 

## Supported versions
All

## Pre-requisites

## Development guidelines
Start wso2am-2.6.0 distribution by executing [APIM_HOME]/bin/wso2server.sh or [APIM_HOME]/bin/wso2server.bat 
Then user will need to login to API publisher as API creator. Once logged in, the user will need to enter the mandatory fields such API name, version, context, visibilty and one resource to create one basic API. Once entered these fields user will be able to save the details(Click on 'SAVE' button)
So that user should be able to see newly created API with status as 'CREATED'. Then user will need to update the endpoint implementation details by giving production or sandbox endpoint URLs. Click **Manage Certificates** and click **Add New Certificate**. Give an alias for the certificate and select an endpoint from the list. Upload the certificate and update the remaining fields to complete the flow up to publish the API. 

## Sample Configuration
Use the PassThroughHTTPSSLSender configurations available by default in  <API-M_HOME>/repository/conf/axis2/axis2.xml and default keystore and truststores. 

## Deployment guidelines
API Manager 2.6.0 deployment required. No additional artifact or data to be added to servers.

## REST API (if available)

