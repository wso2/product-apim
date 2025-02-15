

# OperationPolicyDataDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**category** | **String** |  |  [optional]
**id** | **String** |  |  [optional]
**name** | **String** |  |  [optional]
**version** | **String** |  |  [optional]
**displayName** | **String** |  |  [optional]
**description** | **String** |  |  [optional]
**applicableFlows** | **List&lt;String&gt;** |  |  [optional]
**supportedGateways** | **List&lt;String&gt;** |  |  [optional]
**supportedApiTypes** | **List&lt;Object&gt;** | Supported API types as an array of strings, or an array of maps [HTTP, SOAP]  [{apiType: HTTP, subType: AI}, {apiType: SOAP}]  |  [optional]
**isAPISpecific** | **Boolean** |  |  [optional]
**md5** | **String** |  |  [optional]
**policyAttributes** | [**List&lt;OperationPolicySpecAttributeDTO&gt;**](OperationPolicySpecAttributeDTO.md) |  |  [optional]



