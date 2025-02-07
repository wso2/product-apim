
# SubscriptionThrottlePolicy

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**policyId** | **String** | Id of policy |  [optional]
**policyName** | **String** | Name of policy | 
**displayName** | **String** | Display name of the policy |  [optional]
**description** | **String** | Description of the policy |  [optional]
**isDeployed** | **Boolean** | Indicates whether the policy is deployed successfully or not. |  [optional]
**defaultLimit** | [**ThrottleLimit**](ThrottleLimit.md) |  |  [optional]
**rateLimitCount** | **Integer** | Burst control request count |  [optional]
**rateLimitTimeUnit** | **String** | Burst control time unit |  [optional]
**customAttributes** | [**List&lt;CustomAttribute&gt;**](CustomAttribute.md) | Custom attributes added to the Subscription Throttling Policy  |  [optional]
**stopOnQuotaReach** | **Boolean** | This indicates the action to be taken when a user goes beyond the allocated quota. If checked, the user&#39;s requests will be dropped. If unchecked, the requests will be allowed to pass through.  |  [optional]
**billingPlan** | **String** | define whether this is Paid or a Free plan. Allowed values are FREE or COMMERCIAL.  |  [optional]



