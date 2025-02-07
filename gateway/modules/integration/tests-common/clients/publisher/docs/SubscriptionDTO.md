

# SubscriptionDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**subscriptionId** | **String** |  | 
**applicationInfo** | [**ApplicationInfoDTO**](ApplicationInfoDTO.md) |  | 
**throttlingPolicy** | **String** |  | 
**subscriptionStatus** | [**SubscriptionStatusEnum**](#SubscriptionStatusEnum) |  | 



## Enum: SubscriptionStatusEnum

Name | Value
---- | -----
BLOCKED | &quot;BLOCKED&quot;
PROD_ONLY_BLOCKED | &quot;PROD_ONLY_BLOCKED&quot;
UNBLOCKED | &quot;UNBLOCKED&quot;
ON_HOLD | &quot;ON_HOLD&quot;
REJECTED | &quot;REJECTED&quot;
TIER_UPDATE_PENDING | &quot;TIER_UPDATE_PENDING&quot;
DELETE_PENDING | &quot;DELETE_PENDING&quot;



