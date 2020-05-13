
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
**unitTime** | **Long** |  | 
**timeUnit** | **String** |  |  [optional]
**tierPlan** | [**TierPlanEnum**](#TierPlanEnum) | This attribute declares whether this policy is available under commercial or free  | 
**stopOnQuotaReach** | **Boolean** | By making this attribute to false, you are capabale of sending requests even if the request count exceeded within a unit time  | 
**monetizationProperties** | **Map&lt;String, String&gt;** | Properties of a tier plan which are related to monetization |  [optional]


<a name="PolicyLevelEnum"></a>
## Enum: PolicyLevelEnum
Name | Value
---- | -----
SUBSCRIPTION | &quot;subscription&quot;
API | &quot;api&quot;


<a name="TierPlanEnum"></a>
## Enum: TierPlanEnum
Name | Value
---- | -----
FREE | &quot;FREE&quot;
COMMERCIAL | &quot;COMMERCIAL&quot;



