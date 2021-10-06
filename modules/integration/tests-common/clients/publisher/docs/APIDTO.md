

# APIDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** | UUID of the api registry artifact  |  [optional] [readonly]
**name** | **String** |  | 
**description** | **String** |  |  [optional]
**context** | **String** |  | 
**version** | **String** |  | 
**provider** | **String** | If the provider value is not given user invoking the api will be used as the provider.  |  [optional]
**lifeCycleStatus** | **String** |  |  [optional]
**wsdlInfo** | [**WSDLInfoDTO**](WSDLInfoDTO.md) |  |  [optional]
**wsdlUrl** | **String** |  |  [optional] [readonly]
**responseCachingEnabled** | **Boolean** |  |  [optional]
**cacheTimeout** | **Integer** |  |  [optional]
**hasThumbnail** | **Boolean** |  |  [optional]
**isDefaultVersion** | **Boolean** |  |  [optional]
**isRevision** | **Boolean** |  |  [optional]
**revisionedApiId** | **String** | UUID of the api registry artifact  |  [optional] [readonly]
**revisionId** | **Integer** |  |  [optional]
**enableSchemaValidation** | **Boolean** |  |  [optional]
**type** | [**TypeEnum**](#TypeEnum) | The api creation type to be used. Accepted values are HTTP, WS, SOAPTOREST, GRAPHQL, WEBSUB, SSE |  [optional]
**transport** | **List&lt;String&gt;** | Supported transports for the API (http and/or https).  |  [optional]
**tags** | **List&lt;String&gt;** |  |  [optional]
**policies** | **List&lt;String&gt;** |  |  [optional]
**apiThrottlingPolicy** | **String** | The API level throttling policy selected for the particular API |  [optional]
**authorizationHeader** | **String** | Name of the Authorization header used for invoking the API. If it is not set, Authorization header name specified in tenant or system level will be used.  |  [optional]
**securityScheme** | **List&lt;String&gt;** | Types of API security, the current API secured with. It can be either OAuth2 or mutual SSL or both. If it is not set OAuth2 will be set as the security for the current API.  |  [optional]
**maxTps** | [**APIMaxTpsDTO**](APIMaxTpsDTO.md) |  |  [optional]
**visibility** | [**VisibilityEnum**](#VisibilityEnum) | The visibility level of the API. Accepts one of the following. PUBLIC, PRIVATE, RESTRICTED. |  [optional]
**visibleRoles** | **List&lt;String&gt;** | The user roles that are able to access the API in Developer Portal |  [optional]
**visibleTenants** | **List&lt;String&gt;** |  |  [optional]
**mediationPolicies** | [**List&lt;MediationPolicyDTO&gt;**](MediationPolicyDTO.md) |  |  [optional]
**subscriptionAvailability** | [**SubscriptionAvailabilityEnum**](#SubscriptionAvailabilityEnum) | The subscription availability. Accepts one of the following. CURRENT_TENANT, ALL_TENANTS or SPECIFIC_TENANTS. |  [optional]
**subscriptionAvailableTenants** | **List&lt;String&gt;** |  |  [optional]
**additionalProperties** | [**List&lt;APIAdditionalPropertiesDTO&gt;**](APIAdditionalPropertiesDTO.md) | Map of custom properties of API |  [optional]
**monetization** | [**APIMonetizationInfoDTO**](APIMonetizationInfoDTO.md) |  |  [optional]
**accessControl** | [**AccessControlEnum**](#AccessControlEnum) | Is the API is restricted to certain set of publishers or creators or is it visible to all the publishers and creators. If the accessControl restriction is none, this API can be modified by all the publishers and creators, if not it can only be viewable/modifiable by certain set of publishers and creators,  based on the restriction.  |  [optional]
**accessControlRoles** | **List&lt;String&gt;** | The user roles that are able to view/modify as API publisher or creator. |  [optional]
**businessInformation** | [**APIBusinessInformationDTO**](APIBusinessInformationDTO.md) |  |  [optional]
**corsConfiguration** | [**APICorsConfigurationDTO**](APICorsConfigurationDTO.md) |  |  [optional]
**websubSubscriptionConfiguration** | [**WebsubSubscriptionConfigurationDTO**](WebsubSubscriptionConfigurationDTO.md) |  |  [optional]
**workflowStatus** | **String** |  |  [optional]
**createdTime** | **String** |  |  [optional]
**lastUpdatedTime** | **String** |  |  [optional]
**endpointConfig** | [**Object**](.md) | Endpoint configuration of the API. This can be used to provide different types of endpoints including Simple REST Endpoints, Loadbalanced and Failover.  &#x60;Simple REST Endpoint&#x60;   {     \&quot;endpoint_type\&quot;: \&quot;http\&quot;,     \&quot;sandbox_endpoints\&quot;:       {        \&quot;url\&quot;: \&quot;https://localhost:9443/am/sample/pizzashack/v1/api/\&quot;     },     \&quot;production_endpoints\&quot;:       {        \&quot;url\&quot;: \&quot;https://localhost:9443/am/sample/pizzashack/v1/api/\&quot;     }   }  &#x60;Loadbalanced Endpoint&#x60;    {     \&quot;endpoint_type\&quot;: \&quot;load_balance\&quot;,     \&quot;algoCombo\&quot;: \&quot;org.apache.synapse.endpoints.algorithms.RoundRobin\&quot;,     \&quot;sessionManagement\&quot;: \&quot;\&quot;,     \&quot;sandbox_endpoints\&quot;:       [                 {           \&quot;url\&quot;: \&quot;https://localhost:9443/am/sample/pizzashack/v1/api/1\&quot;        },                 {           \&quot;endpoint_type\&quot;: \&quot;http\&quot;,           \&quot;template_not_supported\&quot;: false,           \&quot;url\&quot;: \&quot;https://localhost:9443/am/sample/pizzashack/v1/api/2\&quot;        }     ],     \&quot;production_endpoints\&quot;:       [                 {           \&quot;url\&quot;: \&quot;https://localhost:9443/am/sample/pizzashack/v1/api/3\&quot;        },                 {           \&quot;endpoint_type\&quot;: \&quot;http\&quot;,           \&quot;template_not_supported\&quot;: false,           \&quot;url\&quot;: \&quot;https://localhost:9443/am/sample/pizzashack/v1/api/4\&quot;        }     ],     \&quot;sessionTimeOut\&quot;: \&quot;\&quot;,     \&quot;algoClassName\&quot;: \&quot;org.apache.synapse.endpoints.algorithms.RoundRobin\&quot;   }  &#x60;Failover Endpoint&#x60;    {     \&quot;production_failovers\&quot;:[        {           \&quot;endpoint_type\&quot;:\&quot;http\&quot;,           \&quot;template_not_supported\&quot;:false,           \&quot;url\&quot;:\&quot;https://localhost:9443/am/sample/pizzashack/v1/api/1\&quot;        }     ],     \&quot;endpoint_type\&quot;:\&quot;failover\&quot;,     \&quot;sandbox_endpoints\&quot;:{        \&quot;url\&quot;:\&quot;https://localhost:9443/am/sample/pizzashack/v1/api/2\&quot;     },     \&quot;production_endpoints\&quot;:{        \&quot;url\&quot;:\&quot;https://localhost:9443/am/sample/pizzashack/v1/api/3\&quot;     },     \&quot;sandbox_failovers\&quot;:[        {           \&quot;endpoint_type\&quot;:\&quot;http\&quot;,           \&quot;template_not_supported\&quot;:false,           \&quot;url\&quot;:\&quot;https://localhost:9443/am/sample/pizzashack/v1/api/4\&quot;        }     ]   }  &#x60;Default Endpoint&#x60;    {     \&quot;endpoint_type\&quot;:\&quot;default\&quot;,     \&quot;sandbox_endpoints\&quot;:{        \&quot;url\&quot;:\&quot;default\&quot;     },     \&quot;production_endpoints\&quot;:{        \&quot;url\&quot;:\&quot;default\&quot;     }   }  &#x60;Endpoint from Endpoint Registry&#x60;   {     \&quot;endpoint_type\&quot;: \&quot;Registry\&quot;,     \&quot;endpoint_id\&quot;: \&quot;{registry-name:entry-name:version}\&quot;,   }  |  [optional]
**endpointImplementationType** | [**EndpointImplementationTypeEnum**](#EndpointImplementationTypeEnum) |  |  [optional]
**scopes** | [**List&lt;APIScopeDTO&gt;**](APIScopeDTO.md) |  |  [optional]
**operations** | [**List&lt;APIOperationsDTO&gt;**](APIOperationsDTO.md) |  |  [optional]
**threatProtectionPolicies** | [**APIThreatProtectionPoliciesDTO**](APIThreatProtectionPoliciesDTO.md) |  |  [optional]
**categories** | **List&lt;String&gt;** | API categories  |  [optional]
**keyManagers** | [**Object**](.md) | API Key Managers  |  [optional] [readonly]
**serviceInfo** | [**APIServiceInfoDTO**](APIServiceInfoDTO.md) |  |  [optional]
**advertiseInfo** | [**AdvertiseInfoDTO**](AdvertiseInfoDTO.md) |  |  [optional]



## Enum: TypeEnum

Name | Value
---- | -----
HTTP | &quot;HTTP&quot;
WS | &quot;WS&quot;
SOAPTOREST | &quot;SOAPTOREST&quot;
SOAP | &quot;SOAP&quot;
GRAPHQL | &quot;GRAPHQL&quot;
WEBSUB | &quot;WEBSUB&quot;
SSE | &quot;SSE&quot;



## Enum: VisibilityEnum

Name | Value
---- | -----
PUBLIC | &quot;PUBLIC&quot;
PRIVATE | &quot;PRIVATE&quot;
RESTRICTED | &quot;RESTRICTED&quot;



## Enum: SubscriptionAvailabilityEnum

Name | Value
---- | -----
CURRENT_TENANT | &quot;CURRENT_TENANT&quot;
ALL_TENANTS | &quot;ALL_TENANTS&quot;
SPECIFIC_TENANTS | &quot;SPECIFIC_TENANTS&quot;



## Enum: AccessControlEnum

Name | Value
---- | -----
NONE | &quot;NONE&quot;
RESTRICTED | &quot;RESTRICTED&quot;



## Enum: EndpointImplementationTypeEnum

Name | Value
---- | -----
INLINE | &quot;INLINE&quot;
ENDPOINT | &quot;ENDPOINT&quot;



