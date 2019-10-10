
# ThrottlingPolicyDTO

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**name** | **String** |  | 
**description** | **String** |  |  [optional]
**policyLevel** | [**PolicyLevelEnum**](#PolicyLevelEnum) |  |  [optional]
**attributes** | **Map&lt;String, String&gt;** | Custom attributes added to the throttling policy  |  [optional]
**requestCount** | **Long** | Maximum number of requests which can be sent within a provided unit time  | 
**unitTime** | **Long** |  | 
**tierPlan** | [**TierPlanEnum**](#TierPlanEnum) | This attribute declares whether this tier is available under commercial or free  | 
**stopOnQuotaReach** | **Boolean** | If this attribute is set to false, you are capabale of sending requests even if the request count exceeded within a unit time  | 
**monetizationAttributes** | [**MonetizationInfoDTO**](MonetizationInfoDTO.md) |  |  [optional]
**throttlingPolicyPermissions** | [**ThrottlingPolicyPermissionInfoDTO**](ThrottlingPolicyPermissionInfoDTO.md) |  |  [optional]


<a name="PolicyLevelEnum"></a>
## Enum: PolicyLevelEnum
Name | Value
---- | -----
APPLICATION | &quot;application&quot;
SUBSCRIPTION | &quot;subscription&quot;


<a name="TierPlanEnum"></a>
## Enum: TierPlanEnum
Name | Value
---- | -----
FREE | &quot;FREE&quot;
COMMERCIAL | &quot;COMMERCIAL&quot;



