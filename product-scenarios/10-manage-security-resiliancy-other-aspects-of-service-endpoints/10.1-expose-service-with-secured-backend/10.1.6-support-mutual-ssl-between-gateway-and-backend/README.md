# 10.1.6 Supporting mutual SSL between API Gateway and backend

## When to use this approach
When an API Developer want to expose their backend using mutual SSL through API Publisher where the server validates the identity of the client so that both parties trust each other.

## Sample use case
An API developer wants to expose their backend using mutual SSL through API Publisher where the server validates the identity of the client so that both parties trust each other. To establish a  secure connection with the backend service, API Manager needs to have the public key of the backend service in the truststore. Then configure API Manager to enable dynamic SSL profiles for HTTPS transport Sender and enable dynamic loading of the configuration. Then the developer would go to the API publisher and create and publish an API by entering API name, version, context, and at least one resource, and give the endpoint address to establish a secure connection with. 

## Supported versions
All

## Pre-requisites

## Development guidelines
Start wso2am-2.6.0 distribution by executing [APIM_HOME]/bin/wso2server.sh or [APIM_HOME]/bin/wso2server.bat 
Then user will need to login to API publisher as API creator. Once logged in, the user will need to enter the mandatory fields such API name, version, context, visibilty and one resource to create one basic API. Once entered these fields user will be able to save the details(Click on 'SAVE' button)
So that user should be able to see newly created API with status as 'CREATED'. Then user will need to update the endpoint URLs which the secure connection should be established with. Complete the rest of the flow up to publish the API. 

## Sample Configuration
1. Generate keys for the backend.
`keytool -keystore backend.jks -genkey -alias backend`
2. Export the certificate from the keystore. 
`keytool -export -keystore backend.jks -alias backend -file backend.crt`
3. Import the generated backend certificate to the API Manager truststore file.
`keytool -import -file backend.crt -alias backend -keystore <APIM_HOME>/repository/resources/security/client-truststore.jks`
4. Export the public certificate from API Manager's keystore. Use <APIM_HOME>/repository/resources/security/wso2carbon.jks file which is the default keystore of WSO2 API Manager.
`keytool -export -keystore wso2carbon.jks -alias wso2carbon -file wso2PubCert.cert`
5. Import the generated certificate to your backend truststore.
`keytool -import -file wso2PubCert.crt -alias wso2carbon -keystore backend-truststore.jks`
6. To configure APIM for Dynamic SSL Profiles for HTTPS transport Sender, you need to create a new XML file <APIM_HOME>/repository/deployment/server/multi_ssl_profiles.xml (this path is configurable) and copy the below configuration into it. This will configure client-truststore.jks as Trust Store for all connections to <localhost:port>. 

```xml
<parameter name="customSSLProfiles">
<!-- For SSL Handshake configure only trust store-->
 <profile>
  <servers>localhost:port</servers>
  <TrustStore>
    <Location>repository/resources/security/client-truststore.jks
    </Location>
    <Type>JKS</Type>
    <Password>wso2carbon</Password>
  </TrustStore>
</profile>
<!-- For Mutual SSL Handshake configure both trust store and key store--> 
 <profile>
     <servers>10.100.5.130:9444</servers>
     <TrustStore>
     <Location>repository/resources/security/client-truststore.jks
     </Location>
     <Type>JKS</Type>
    <Password>wso2carbon</Password>
     </TrustStore> 
     <KeyStore>
         <Location>repository/resources/security/wso2carbon.jks</Location>
         <Type>JKS</Type>
         <Password>xxxxxx</Password>
         <KeyPassword>xxxxxx</KeyPassword>
     </KeyStore>
</profile>
</parameter>
```

To enable dynamic loading of this configuration, add below configurations to the Transport Sender configuration (PassThroughHttpSSLSender) of API Manager (<APIM_HOME>/repository/conf/axis2.xml). Set above file’s path as the filePath parameter.
```xml
<parameter name="dynamicSSLProfilesConfig"> 
    <filePath>repository/deployment/server/multi_ssl_profiles.xml</filePath>
    <fileReadInterval>3600000</fileReadInterval> 
</parameter>
<parameter name="HostnameVerifier">AllowAll</parameter>
```
Now both the backend service and API Manager is configured to use default key stores and API Manager is configured to load dynamic SSL profiles. Restart API Manager.

## Deployment guidelines
API Manager 2.6.0 deployment required. Import the generated backend certificate to the API Manager truststore file. Export API Manager's public certificate to backend truststore.

## REST API (if available)

