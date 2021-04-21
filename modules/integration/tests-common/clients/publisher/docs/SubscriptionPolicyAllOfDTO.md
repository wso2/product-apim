

# SubscriptionPolicyAllOfDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**policyId** | **Integer** | Id of policy |  [optional]
**uuid** | **String** | policy uuid |  [optional]
**policyName** | **String** | Name of policy |  [optional]
**displayName** | **String** | Display name of the policy |  [optional]
**description** | **String** | Description of the policy |  [optional]
**isDeployed** | **Boolean** | Indicates whether the policy is deployed successfully or not. |  [optional]
**tenantId** | **Integer** | Throttling policy tenant domain id |  [optional]
**tenantDomain** | **String** | Throttling policy tenant domain |  [optional]
**defaultLimit** | [**ThrottleLimitDTO**](ThrottleLimitDTO.md) |  | 
**rateLimitCount** | **Integer** | Burst control request count |  [optional]
**rateLimitTimeUnit** | **String** | Burst control time unit |  [optional]
**subscriberCount** | **Integer** | Number of subscriptions allowed |  [optional]
**customAttributes** | [**List&lt;CustomAttributeDTO&gt;**](CustomAttributeDTO.md) | Custom attributes added to the Subscription Throttling Policy  |  [optional]
**stopOnQuotaReach** | **Boolean** | This indicates the action to be taken when a user goes beyond the allocated quota. If checked, the user&#39;s requests will be dropped. If unchecked, the requests will be allowed to pass through.  |  [optional]
**billingPlan** | **String** | define whether this is Paid or a Free plan. Allowed values are FREE or COMMERCIAL.  |  [optional]
**permissions** | [**SubscriptionThrottlePolicyPermissionDTO**](SubscriptionThrottlePolicyPermissionDTO.md) |  |  [optional]



