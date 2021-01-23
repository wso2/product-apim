

# SubscriptionDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**subscriptionId** | **String** | The UUID of the subscription |  [optional] [readonly]
**applicationId** | **String** | The UUID of the application | 
**apiId** | **String** | The unique identifier of the API. |  [optional]
**apiInfo** | [**APIInfoDTO**](APIInfoDTO.md) |  |  [optional]
**applicationInfo** | [**ApplicationInfoDTO**](ApplicationInfoDTO.md) |  |  [optional]
**throttlingPolicy** | **String** |  | 
**requestedThrottlingPolicy** | **String** |  |  [optional]
**status** | [**StatusEnum**](#StatusEnum) |  |  [optional]
**redirectionParams** | **String** | A url and other parameters the subscriber can be redirected. |  [optional] [readonly]



## Enum: StatusEnum

Name | Value
---- | -----
BLOCKED | &quot;BLOCKED&quot;
PROD_ONLY_BLOCKED | &quot;PROD_ONLY_BLOCKED&quot;
UNBLOCKED | &quot;UNBLOCKED&quot;
ON_HOLD | &quot;ON_HOLD&quot;
REJECTED | &quot;REJECTED&quot;
TIER_UPDATE_PENDING | &quot;TIER_UPDATE_PENDING&quot;



