

# MCPServerDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** | UUID of the MCP Server |  [optional] [readonly]
**name** | **String** |  | 
**displayName** | **String** | Human-friendly name shown in UI. Length limited to DB column size. |  [optional]
**description** | **String** |  |  [optional]
**context** | **String** |  | 
**endpointConfig** | [**Object**](.md) | Endpoint configuration of the backend.  |  [optional]
**version** | **String** |  | 
**provider** | **String** | If the provider value is not given user invoking the MCP Server will be used as the provider. |  [optional]
**lifeCycleStatus** | **String** |  |  [optional]
**hasThumbnail** | **Boolean** |  |  [optional]
**isDefaultVersion** | **Boolean** |  |  [optional]
**isRevision** | **Boolean** |  |  [optional]
**revisionedMCPServerId** | **String** | UUID of the artifact |  [optional] [readonly]
**revisionId** | **Integer** |  |  [optional]
**enableSchemaValidation** | **Boolean** |  |  [optional]
**audiences** | **List&lt;String&gt;** | The audiences of the MCP Server for jwt validation. Accepted values are any String values |  [optional]
**transport** | **List&lt;String&gt;** | Supported transports for the MCP Server (http and/or https). |  [optional]
**tags** | **List&lt;String&gt;** |  |  [optional]
**policies** | **List&lt;String&gt;** |  |  [optional]
**organizationPolicies** | [**List&lt;OrganizationPoliciesDTO&gt;**](OrganizationPoliciesDTO.md) |  |  [optional]
**throttlingPolicy** | **String** | The MCP Server level throttling policy selected. |  [optional]
**authorizationHeader** | **String** | Name of the Authorization header used for invoking the MCP Server. If it is not set,  Authorization header name specified in tenant or system level will be used.  |  [optional]
**apiKeyHeader** | **String** | Name of the API key header used for invoking the MCP Server. If it is not set, default value&#x60;apiKey&#x60;  will be used.  |  [optional]
**securityScheme** | **List&lt;String&gt;** | Types of API security, the current MCP Server secured with. It can be either OAuth2 or mutual SSLor both. If it is not set OAuth2 will be set as the security.  |  [optional]
**maxTps** | [**MaxTpsDTO**](MaxTpsDTO.md) |  |  [optional]
**visibility** | [**VisibilityEnum**](#VisibilityEnum) | The visibility level of the MCP Server. Accepts one of the following: PUBLIC, PRIVATE, RESTRICTED.  |  [optional]
**visibleRoles** | **List&lt;String&gt;** | The user roles that are able to access the MCP Server in Developer Portal |  [optional]
**visibleTenants** | **List&lt;String&gt;** |  |  [optional]
**visibleOrganizations** | **List&lt;String&gt;** | The organizations that are able to access the MCP server in Developer Portal |  [optional]
**mcpServerPolicies** | [**MCPServerOperationPoliciesDTO**](MCPServerOperationPoliciesDTO.md) |  |  [optional]
**subscriptionAvailability** | [**SubscriptionAvailabilityEnum**](#SubscriptionAvailabilityEnum) | The subscription availability. Accepts one of the following: CURRENT_TENANT, ALL_TENANTS, or  SPECIFIC_TENANTS.  |  [optional]
**subscriptionAvailableTenants** | **List&lt;String&gt;** |  |  [optional]
**additionalPropertiesMap** | [**Map&lt;String, APIInfoAdditionalPropertiesMapDTO&gt;**](APIInfoAdditionalPropertiesMapDTO.md) |  |  [optional]
**monetization** | [**APIMonetizationInfoDTO**](APIMonetizationInfoDTO.md) |  |  [optional]
**accessControl** | [**AccessControlEnum**](#AccessControlEnum) | Is the MCP server restricted to certain publishers or creators or is it visible to all publishers and  creators. If the accessControl restriction is NONE, this can be modified by all publishers and creators. Otherwise, it can only be viewable/modifiable by a specific set of users based on the restriction.  |  [optional]
**accessControlRoles** | **List&lt;String&gt;** | The user roles that are able to view/modify as publisher or creator. |  [optional]
**businessInformation** | [**APIBusinessInformationDTO**](APIBusinessInformationDTO.md) |  |  [optional]
**corsConfiguration** | [**APICorsConfigurationDTO**](APICorsConfigurationDTO.md) |  |  [optional]
**workflowStatus** | **String** |  |  [optional]
**protocolVersion** | **String** |  |  [optional]
**createdTime** | **String** |  |  [optional]
**lastUpdatedTimestamp** | **String** |  |  [optional]
**lastUpdatedTime** | **String** |  |  [optional]
**subtypeConfiguration** | [**SubtypeConfigurationDTO**](SubtypeConfigurationDTO.md) |  |  [optional]
**scopes** | [**List&lt;MCPServerScopeDTO&gt;**](MCPServerScopeDTO.md) |  |  [optional]
**operations** | [**List&lt;MCPServerOperationDTO&gt;**](MCPServerOperationDTO.md) |  |  [optional]
**categories** | **List&lt;String&gt;** | MCP Server categories |  [optional]
**keyManagers** | [**Object**](.md) | Key Managers |  [optional] [readonly]
**gatewayVendor** | **String** |  |  [optional]
**gatewayType** | **String** | The gateway type selected for the policies. Accepts one of the following: wso2/synapse, wso2/apk, AWS.  |  [optional]
**initiatedFromGateway** | **Boolean** | Whether the MCP Server is initiated from the gateway or not. This is used to identify whether the MCP Server is created from the publisher or discovered from the gateway.  |  [optional] [readonly]



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



