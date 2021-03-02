# 10.1.4 Expose a service which is secured via Digest Authentication while not exposing endpoint password as plain text  in API definition XML 

## When to use this approach
When an API Developer want to expose their backend service which is secured via Digest Authentication and developer does not want to expose the password as plain text in api definition xml.

## Sample use case
An API developer wants to expose their backend service which secured via Digest Authentication, and to do that developer would go to the API publisher and create an API by entering API name, version, context, and at least one resource, and specify secured endpoint authentication mechanism as Digest Auth and provide username and password for the endpoint. To encrypt the password in api definition xml, user would need to enable secure vault configuration in api-manager.xml and run the available cipher tool.

## Supported versions
All

## Pre-requisites
User needs to enable secure vault configuration by setting the element <EnableSecureVault> in the <APIM_HOME>/repository/conf/api-manager.xml file to true. 
Run the cipher tool available in the <APIM_HOME>/bin directory. Assume using the default keystore, give wso2carbon as the primary keystore password when prompted.
<br/>
`sh ciphertool.sh -Dconfigure`


## Development guidelines
Start wso2am-2.6.0 distribution by executing [APIM_HOME]/bin/wso2server.sh or [APIM_HOME]/bin/wso2server.bat 
Then user will need to login to API publisher as API creator. Once logged in, the user will need to enter the mandatory fields such API name, version, context, visibilty and one resource to create one basic API. Once entered these fields user will be able to save the details(Click on 'SAVE' button)
So that user should be able to see newly created API with status as 'CREATED'. Then user will need to update the endpoint implementation details by giving production or sandbox endpoint URLs and selecting **Secured** as the endpoint security scheme. Then select the **Digest Auth** option as authentication type for the endpoint and give its credentials. Update the remaining fields to complete the flow up to publish the API. 

## Sample Configuration
Shut down the server if it is already running and set the element <EnableSecureVault> in the <APIM_HOME>/repository/conf/api-manager.xml file to true. By default, the system stores passwords in configuration files in plain text because this value is set to false. 

Run the cipher tool available in the <APIM_HOME>/bin directory. If you are running Windows, it is the ciphertool.bat file. If you are using the default keystore, give wso2carbon as the primary keystore password when prompted.
<br/>
`sh ciphertool.sh -Dconfigure`

Restart the server (Publisher) after the above steps have been performed. From there onwards, the Digest Authentication header which is written to the API definition xml file will be encrypted. 

## Deployment guidelines
API Manager 2.6.0 deployment required. No additional artifact or data to be added to servers.

## REST API (if available)

