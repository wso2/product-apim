

# APIDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** | UUID of the api  |  [optional]
**name** | **String** | Name of the API | 
**description** | **String** | A brief description about the API |  [optional]
**context** | **String** | A string that represents thecontext of the user&#39;s request | 
**version** | **String** | The version of the API | 
**provider** | **String** | If the provider value is not given user invoking the api will be used as the provider.  | 
**apiDefinition** | **String** | Swagger definition of the API which contains details about URI templates and scopes  |  [optional]
**wsdlUri** | **String** | WSDL URL if the API is based on a WSDL endpoint  |  [optional]
**lifeCycleStatus** | **String** | This describes in which status of the lifecycle the API is. | 
**isDefaultVersion** | **Boolean** |  |  [optional]
**type** | **String** | This describes the transport type of the API |  [optional]
**transport** | **List&lt;String&gt;** |  |  [optional]
**operations** | [**List&lt;APIOperationsDTO&gt;**](APIOperationsDTO.md) |  |  [optional]
**authorizationHeader** | **String** | Name of the Authorization header used for invoking the API. If it is not set, Authorization header name specified in tenant or system level will be used.  |  [optional]
**securityScheme** | **List&lt;String&gt;** | Types of API security, the current API secured with. It can be either OAuth2 or mutual SSL or both. If it is not set OAuth2 will be set as the security for the current API.  |  [optional]
**tags** | **List&lt;String&gt;** | Search keywords related to the API |  [optional]
**tiers** | [**List&lt;APITiersDTO&gt;**](APITiersDTO.md) | The subscription tiers selected for the particular API |  [optional]
**hasThumbnail** | **Boolean** |  |  [optional]
**additionalProperties** | [**List&lt;APIAdditionalPropertiesDTO&gt;**](APIAdditionalPropertiesDTO.md) | Custom(user defined) properties of API  |  [optional]
**monetization** | [**APIMonetizationInfoDTO**](APIMonetizationInfoDTO.md) |  |  [optional]
**ingressURLs** | [**List&lt;APIIngressURLsDTO&gt;**](APIIngressURLsDTO.md) |  |  [optional]
**endpointURLs** | [**List&lt;APIEndpointURLsDTO&gt;**](APIEndpointURLsDTO.md) |  |  [optional]
**businessInformation** | [**APIBusinessInformationDTO**](APIBusinessInformationDTO.md) |  |  [optional]
**labels** | [**List&lt;LabelDTO&gt;**](LabelDTO.md) | Labels of micro-gateway environments attached to the API.  |  [optional]
**environmentList** | **List&lt;String&gt;** | The environment list configured with non empty endpoint URLs for the particular API. |  [optional]
**scopes** | [**List&lt;ScopeInfoDTO&gt;**](ScopeInfoDTO.md) |  |  [optional]
**avgRating** | **String** | The average rating of the API |  [optional]
**advertiseInfo** | [**AdvertiseInfoDTO**](AdvertiseInfoDTO.md) |  |  [optional]
**isSubscriptionAvailable** | **Boolean** |  |  [optional]
**categories** | **List&lt;String&gt;** | API categories  |  [optional]
**keyManagers** | [**Object**](.md) | API Key Managers  |  [optional]
**createdTime** | **String** |  |  [optional]
**lastUpdatedTime** | **String** |  |  [optional]
**gatewayVendor** | **String** |  |  [optional]
**asyncTransportProtocols** | **List&lt;String&gt;** | Supported transports for the aync API.  |  [optional]



