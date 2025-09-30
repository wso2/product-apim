

# MCPServerOperationDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** |  |  [optional]
**target** | **String** |  |  [optional]
**feature** | [**FeatureEnum**](#FeatureEnum) | Operation type for MCP Server (e.g., TOOL) |  [optional]
**authType** | **String** |  |  [optional]
**throttlingPolicy** | **String** |  |  [optional]
**scopes** | **List&lt;String&gt;** |  |  [optional]
**payloadSchema** | **String** |  |  [optional]
**uriMapping** | **String** |  |  [optional]
**schemaDefinition** | **String** |  |  [optional]
**description** | **String** |  |  [optional]
**operationPolicies** | [**APIOperationPoliciesDTO**](APIOperationPoliciesDTO.md) |  |  [optional]
**backendOperationMapping** | [**BackendOperationMappingDTO**](BackendOperationMappingDTO.md) |  |  [optional]
**apiOperationMapping** | [**APIOperationMappingDTO**](APIOperationMappingDTO.md) |  |  [optional]



## Enum: FeatureEnum

Name | Value
---- | -----
TOOL | &quot;TOOL&quot;



