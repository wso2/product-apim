
# ThrottleCondition

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | [**TypeEnum**](#TypeEnum) | Type of the thottling condition. Allowed values are HeaderCondition, IPCondition, JWTClaimsCondition, QueryParameterCondition  | 
**invertCondition** | **Boolean** | Specifies whether inversion of the condition to be matched against the request |  [optional]


<a name="TypeEnum"></a>
## Enum: TypeEnum
Name | Value
---- | -----
HEADERCONDITION | &quot;HeaderCondition&quot;
IPCONDITION | &quot;IPCondition&quot;
JWTCLAIMSCONDITION | &quot;JWTClaimsCondition&quot;
QUERYPARAMETERCONDITION | &quot;QueryParameterCondition&quot;



