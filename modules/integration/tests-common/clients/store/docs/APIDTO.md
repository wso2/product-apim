
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
**wsdlUri** | **String** | WSDL URL if the API is based on a WSDL endpoint  |  [optional]
**lifeCycleStatus** | **String** | This describes in which status of the lifecycle the API is. | 
**isDefaultVersion** | **Boolean** |  |  [optional]
**transport** | **List&lt;String&gt;** |  |  [optional]
**authorizationHeader** | **String** | Name of the Authorization header used for invoking the API. If it is not set, Authorization header name specified in tenant or system level will be used.  |  [optional]
**securityScheme** | **List&lt;String&gt;** | Types of API security, the current API secured with. It can be either OAuth2 or mutual SSL or both. If it is not set OAuth2 will be set as the security for the current API.  |  [optional]
**tags** | **List&lt;String&gt;** | Search keywords related to the API |  [optional]
**tiers** | **List&lt;String&gt;** | The subscription tiers selected for the particular API |  [optional]
**hasThumbnail** | **Boolean** |  |  [optional]
**additionalProperties** | **Map&lt;String, String&gt;** | Custom(user defined) properties of API  |  [optional]
**endpointURLs** | [**List&lt;APIEndpointURLsDTO&gt;**](APIEndpointURLsDTO.md) |  |  [optional]
**businessInformation** | [**APIBusinessInformationDTO**](APIBusinessInformationDTO.md) |  |  [optional]
**labels** | [**List&lt;LabelDTO&gt;**](LabelDTO.md) | Labels of micro-gateway environments attached to the API.  |  [optional]
**environmentList** | **List&lt;String&gt;** | The environment list configured with non empty endpoint URLs for the particular API. |  [optional]
**scopes** | [**List&lt;ScopeInfoDTO&gt;**](ScopeInfoDTO.md) |  |  [optional]



