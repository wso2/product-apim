
# Tier

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**name** | **String** |  | 
**description** | **String** |  |  [optional]
**tierLevel** | [**TierLevelEnum**](#TierLevelEnum) |  |  [optional]
**attributes** | **Map&lt;String, String&gt;** | Custom attributes added to the tier policy  |  [optional]
**requestCount** | **Long** | Maximum number of requests which can be sent within a provided unit time  | 
**unitTime** | **Long** |  | 
**tierPlan** | [**TierPlanEnum**](#TierPlanEnum) | This attribute declares whether this tier is available under commercial or free  | 
**stopOnQuotaReach** | **Boolean** | If this attribute is set to false, you are capabale of sending requests even if the request count exceeded within a unit time  | 


<a name="TierLevelEnum"></a>
## Enum: TierLevelEnum
Name | Value
---- | -----
API | &quot;api&quot;
APPLICATION | &quot;application&quot;


<a name="TierPlanEnum"></a>
## Enum: TierPlanEnum
Name | Value
---- | -----
FREE | &quot;FREE&quot;
COMMERCIAL | &quot;COMMERCIAL&quot;



