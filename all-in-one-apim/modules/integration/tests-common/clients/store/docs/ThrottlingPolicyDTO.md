

# ThrottlingPolicyDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**name** | **String** |  | 
**description** | **String** |  |  [optional]
**policyLevel** | [**PolicyLevelEnum**](#PolicyLevelEnum) |  |  [optional]
**attributes** | **Map&lt;String, String&gt;** | Custom attributes added to the throttling policy  |  [optional]
**requestCount** | **Long** | Maximum number of requests which can be sent within a provided unit time  | 
**dataUnit** | **String** | Unit of data allowed to be transfered. Allowed values are \&quot;KB\&quot;, \&quot;MB\&quot; and \&quot;GB\&quot;  |  [optional]
**unitTime** | **Long** |  | 
**timeUnit** | **String** |  |  [optional]
**rateLimitCount** | **Integer** | Burst control request count |  [optional]
**rateLimitTimeUnit** | **String** | Burst control time unit |  [optional]
**quotaPolicyType** | [**QuotaPolicyTypeEnum**](#QuotaPolicyTypeEnum) | Default quota limit type |  [optional]
**tierPlan** | [**TierPlanEnum**](#TierPlanEnum) | This attribute declares whether this tier is available under commercial or free  | 
**stopOnQuotaReach** | **Boolean** | If this attribute is set to false, you are capabale of sending requests even if the request count exceeded within a unit time  | 
**monetizationAttributes** | [**MonetizationInfoDTO**](MonetizationInfoDTO.md) |  |  [optional]
**throttlingPolicyPermissions** | [**ThrottlingPolicyPermissionInfoDTO**](ThrottlingPolicyPermissionInfoDTO.md) |  |  [optional]



## Enum: PolicyLevelEnum

Name | Value
---- | -----
APPLICATION | &quot;application&quot;
SUBSCRIPTION | &quot;subscription&quot;



## Enum: QuotaPolicyTypeEnum

Name | Value
---- | -----
REQUESTCOUNT | &quot;REQUESTCOUNT&quot;
BANDWIDTHVOLUME | &quot;BANDWIDTHVOLUME&quot;



## Enum: TierPlanEnum

Name | Value
---- | -----
FREE | &quot;FREE&quot;
COMMERCIAL | &quot;COMMERCIAL&quot;



