

# OperationPolicyDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**policyType** | [**PolicyTypeEnum**](#PolicyTypeEnum) |  | 
**parameters** | **Map&lt;String, String&gt;** |  |  [optional]



## Enum: PolicyTypeEnum

Name | Value
---- | -----
SET_HEADER | &quot;SET_HEADER&quot;
REMOVE_HEADER | &quot;REMOVE_HEADER&quot;
REWRITE_HTTP_METHOD | &quot;REWRITE_HTTP_METHOD&quot;
REWRITE_RESOURCE_PATH | &quot;REWRITE_RESOURCE_PATH&quot;
ADD_QUERY_PARAM | &quot;ADD_QUERY_PARAM&quot;
REMOVE_QUERY_PARAM | &quot;REMOVE_QUERY_PARAM&quot;
MOCK_RESPONSE | &quot;MOCK_RESPONSE&quot;
CHANGE_ENDPOINT | &quot;CHANGE_ENDPOINT&quot;



