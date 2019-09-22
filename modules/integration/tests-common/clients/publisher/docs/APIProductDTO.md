
# APIProductDTO

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** | UUID of the api product  |  [optional]
**name** | **String** | Name of the API Product | 
**context** | **String** |  |  [optional]
**description** | **String** | A brief description about the API |  [optional]
**provider** | **String** | If the provider value is not given, the user invoking the API will be used as the provider.  |  [optional]
**thumbnailUri** | **String** |  |  [optional]
**state** | [**StateEnum**](#StateEnum) | State of the API product. Only published api products are visible on the store  |  [optional]
**visibility** | [**VisibilityEnum**](#VisibilityEnum) | The visibility level of the API. Accepts one of the following. PUBLIC, PRIVATE, RESTRICTED. |  [optional]
**visibleRoles** | **List&lt;String&gt;** | The user roles that are able to access the API |  [optional]
**visibleTenants** | **List&lt;String&gt;** |  |  [optional]
**accessControl** | [**AccessControlEnum**](#AccessControlEnum) | Defines whether the API Product is restricted to certain set of publishers or creators or is it visible to all the publishers and creators. If the accessControl restriction is none, this API Product can be modified by all the publishers and creators, if not it can only be viewable/modifiable by certain set of publishers and creators, based on the restriction.  |  [optional]
**accessControlRoles** | **List&lt;String&gt;** | The user roles that are able to view/modify as API Product publisher or creator. |  [optional]
**gatewayEnvironments** | **List&lt;String&gt;** | List of gateway environments the API Product is available  |  [optional]
**apiType** | **String** | The api type to be used. Accepted values are API, API PRODUCT |  [optional]
**transport** | **List&lt;String&gt;** | Supported transports for the API (http and/or https).  |  [optional]
**tags** | **List&lt;String&gt;** |  |  [optional]
**policies** | **List&lt;String&gt;** |  |  [optional]
**authorizationHeader** | **String** | Name of the Authorization header used for invoking the API. If it is not set, Authorization header name specified in tenant or system level will be used.  |  [optional]
**securityScheme** | **List&lt;String&gt;** | Types of API security, the current API secured with. It can be either OAuth2 or mutual SSL or both. If it is not set OAuth2 will be set as the security for the current API.  |  [optional]
**subscriptionAvailability** | [**SubscriptionAvailabilityEnum**](#SubscriptionAvailabilityEnum) | The subscription availability. Accepts one of the following. current_tenant, all_tenants or specific_tenants. |  [optional]
**subscriptionAvailableTenants** | **List&lt;String&gt;** |  |  [optional]
**additionalProperties** | **Map&lt;String, String&gt;** | Map of custom properties of API |  [optional]
**businessInformation** | [**APIProductBusinessInformationDTO**](APIProductBusinessInformationDTO.md) |  |  [optional]
**corsConfiguration** | [**APICorsConfigurationDTO**](APICorsConfigurationDTO.md) |  |  [optional]
**createdTime** | **String** |  |  [optional]
**lastUpdatedTime** | **String** |  |  [optional]
**apis** | [**List&lt;ProductAPIDTO&gt;**](ProductAPIDTO.md) | APIs and resources in the API Product.  |  [optional]
**scopes** | [**List&lt;ScopeDTO&gt;**](ScopeDTO.md) |  |  [optional]


<a name="StateEnum"></a>
## Enum: StateEnum
Name | Value
---- | -----
CREATED | &quot;CREATED&quot;
PUBLISHED | &quot;PUBLISHED&quot;


<a name="VisibilityEnum"></a>
## Enum: VisibilityEnum
Name | Value
---- | -----
PUBLIC | &quot;PUBLIC&quot;
PRIVATE | &quot;PRIVATE&quot;
RESTRICTED | &quot;RESTRICTED&quot;


<a name="AccessControlEnum"></a>
## Enum: AccessControlEnum
Name | Value
---- | -----
NONE | &quot;NONE&quot;
RESTRICTED | &quot;RESTRICTED&quot;


<a name="SubscriptionAvailabilityEnum"></a>
## Enum: SubscriptionAvailabilityEnum
Name | Value
---- | -----
CURRENT_TENANT | &quot;current_tenant&quot;
ALL_TENANTS | &quot;all_tenants&quot;
SPECIFIC_TENANTS | &quot;specific_tenants&quot;



