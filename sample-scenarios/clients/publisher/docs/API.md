
# API

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** | UUID of the api registry artifact  |  [optional]
**name** | **String** | Name of the API | 
**description** | **String** | A brief description about the API |  [optional]
**context** | **String** | A string that represents the context of the user&#39;s request | 
**version** | **String** | The version of the API | 
**provider** | **String** | If the provider value is not given user invoking the api will be used as the provider.  |  [optional]
**apiDefinition** | **String** | Swagger definition of the API which contains details about URI templates and scopes  |  [optional]
**wsdlUri** | **String** | WSDL URL if the API is based on a WSDL endpoint  |  [optional]
**status** | **String** | This describes in which status of the lifecycle the API is |  [optional]
**responseCaching** | **String** |  |  [optional]
**cacheTimeout** | **Integer** |  |  [optional]
**destinationStatsEnabled** | **String** |  |  [optional]
**isDefaultVersion** | **Boolean** |  | 
**type** | [**TypeEnum**](#TypeEnum) |  | 
**transport** | **List&lt;String&gt;** | Supported transports for the API (http and/or https).  | 
**tags** | **List&lt;String&gt;** | Search keywords related to the API |  [optional]
**tiers** | **List&lt;String&gt;** | The subscription tiers selected for the particular API | 
**maxTps** | [**APIMaxTps**](APIMaxTps.md) |  |  [optional]
**thumbnailUri** | **String** |  |  [optional]
**visibility** | [**VisibilityEnum**](#VisibilityEnum) |  | 
**visibleRoles** | **List&lt;String&gt;** | The user roles that are able to access the API |  [optional]
**visibleTenants** | **List&lt;String&gt;** |  |  [optional]
**endpointConfig** | **String** |  | 
**endpointSecurity** | [**APIEndpointSecurity**](APIEndpointSecurity.md) |  |  [optional]
**gatewayEnvironments** | **String** | Comma separated list of gateway environments.  |  [optional]
**sequences** | [**List&lt;Sequence&gt;**](Sequence.md) |  |  [optional]
**subscriptionAvailability** | [**SubscriptionAvailabilityEnum**](#SubscriptionAvailabilityEnum) |  |  [optional]
**subscriptionAvailableTenants** | **List&lt;String&gt;** |  |  [optional]
**businessInformation** | [**APIBusinessInformation**](APIBusinessInformation.md) |  |  [optional]
**corsConfiguration** | [**APICorsConfiguration**](APICorsConfiguration.md) |  |  [optional]


<a name="TypeEnum"></a>
## Enum: TypeEnum
Name | Value
---- | -----
HTTP | &quot;HTTP&quot;
WS | &quot;WS&quot;


<a name="VisibilityEnum"></a>
## Enum: VisibilityEnum
Name | Value
---- | -----
PUBLIC | &quot;PUBLIC&quot;
PRIVATE | &quot;PRIVATE&quot;
RESTRICTED | &quot;RESTRICTED&quot;
CONTROLLED | &quot;CONTROLLED&quot;


<a name="SubscriptionAvailabilityEnum"></a>
## Enum: SubscriptionAvailabilityEnum
Name | Value
---- | -----
CURRENT_TENANT | &quot;current_tenant&quot;
ALL_TENANTS | &quot;all_tenants&quot;
SPECIFIC_TENANTS | &quot;specific_tenants&quot;



