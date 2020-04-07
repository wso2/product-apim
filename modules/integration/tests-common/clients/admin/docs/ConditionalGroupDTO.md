
# ConditionalGroupDTO

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**description** | **String** | Description of the Conditional Group |  [optional]
**conditions** | [**List&lt;ThrottleConditionDTO&gt;**](ThrottleConditionDTO.md) | Individual throttling conditions. They can be defined as either HeaderCondition, IPCondition, JWTClaimsCondition, QueryParameterCondition Please see schemas of each of those throttling condition in Definitions section.  | 
**limit** | [**ThrottleLimitDTO**](ThrottleLimitDTO.md) |  | 



