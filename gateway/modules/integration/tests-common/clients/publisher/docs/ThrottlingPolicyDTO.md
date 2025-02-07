

# ThrottlingPolicyDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**name** | **String** |  | 
**description** | **String** |  |  [optional]
**policyLevel** | [**PolicyLevelEnum**](#PolicyLevelEnum) |  |  [optional]
**displayName** | **String** |  |  [optional]
**attributes** | **Map&lt;String, String&gt;** | Custom attributes added to the policy policy  |  [optional]
**requestCount** | **Long** | Maximum number of requests which can be sent within a provided unit time  | 
**dataUnit** | **String** | Unit of data allowed to be transfered. Allowed values are \&quot;KB\&quot;, \&quot;MB\&quot; and \&quot;GB\&quot;  |  [optional]
**unitTime** | **Long** |  | 
**timeUnit** | **String** |  |  [optional]
**rateLimitCount** | **Integer** | Burst control request count |  [optional]
**rateLimitTimeUnit** | **String** | Burst control time unit |  [optional]
**quotaPolicyType** | [**QuotaPolicyTypeEnum**](#QuotaPolicyTypeEnum) | Default quota limit type |  [optional]
**tierPlan** | [**TierPlanEnum**](#TierPlanEnum) | This attribute declares whether this policy is available under commercial or free  | 
**stopOnQuotaReach** | **Boolean** | By making this attribute to false, you are capabale of sending requests even if the request count exceeded within a unit time  | 
**monetizationProperties** | **Map&lt;String, String&gt;** | Properties of a tier plan which are related to monetization |  [optional]



## Enum: PolicyLevelEnum

Name | Value
---- | -----
SUBSCRIPTION | &quot;subscription&quot;
API | &quot;api&quot;



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



