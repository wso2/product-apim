

# ServiceDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** |  |  [optional] [readonly]
**name** | **String** |  | 
**description** | **String** |  |  [optional]
**version** | **String** |  | 
**serviceKey** | **String** |  |  [optional]
**serviceUrl** | **String** |  | 
**definitionType** | [**DefinitionTypeEnum**](#DefinitionTypeEnum) | The type of the provided API definition | 
**securityType** | [**SecurityTypeEnum**](#SecurityTypeEnum) | The security type of the endpoint |  [optional]
**mutualSSLEnabled** | **Boolean** | Whether Mutual SSL is enabled for the endpoint |  [optional]
**usage** | **Integer** | Number of usages of the service in APIs |  [optional] [readonly]
**createdTime** | **String** |  |  [optional] [readonly]
**lastUpdatedTime** | **String** |  |  [optional] [readonly]
**md5** | **String** |  |  [optional]
**definitionUrl** | **String** |  |  [optional]



## Enum: DefinitionTypeEnum

Name | Value
---- | -----
OAS2 | &quot;OAS2&quot;
OAS3 | &quot;OAS3&quot;
WSDL1 | &quot;WSDL1&quot;
WSDL2 | &quot;WSDL2&quot;
GRAPHQL_SDL | &quot;GRAPHQL_SDL&quot;
ASYNC_API | &quot;ASYNC_API&quot;



## Enum: SecurityTypeEnum

Name | Value
---- | -----
BASIC | &quot;BASIC&quot;
DIGEST | &quot;DIGEST&quot;
OAUTH2 | &quot;OAUTH2&quot;
X509 | &quot;X509&quot;
API_KEY | &quot;API_KEY&quot;
NONE | &quot;NONE&quot;



