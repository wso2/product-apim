
# ExtendedSubscriptionDTO

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**subscriptionId** | **String** |  | 
**applicationInfo** | [**ApplicationDTO**](ApplicationDTO.md) |  | 
**policy** | **String** |  | 
**subscriptionStatus** | [**SubscriptionStatusEnum**](#SubscriptionStatusEnum) |  | 
**workflowId** | **String** |  |  [optional]


<a name="SubscriptionStatusEnum"></a>
## Enum: SubscriptionStatusEnum
Name | Value
---- | -----
BLOCKED | &quot;BLOCKED&quot;
PROD_ONLY_BLOCKED | &quot;PROD_ONLY_BLOCKED&quot;
UNBLOCKED | &quot;UNBLOCKED&quot;
ON_HOLD | &quot;ON_HOLD&quot;
REJECTED | &quot;REJECTED&quot;



