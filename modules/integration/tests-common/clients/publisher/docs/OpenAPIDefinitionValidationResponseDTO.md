

# OpenAPIDefinitionValidationResponseDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**isValid** | **Boolean** | This attribute declares whether this definition is valid or not.  | 
**content** | **String** | OpenAPI definition content.  |  [optional]
**info** | [**OpenAPIDefinitionValidationResponseInfoDTO**](OpenAPIDefinitionValidationResponseInfoDTO.md) |  |  [optional]
**errors** | [**List&lt;ErrorListItemDTO&gt;**](ErrorListItemDTO.md) | If there are more than one error list them out. For example, list out validation errors by each field.  |  [optional]



