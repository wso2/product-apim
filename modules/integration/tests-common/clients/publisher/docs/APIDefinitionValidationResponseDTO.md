
# APIDefinitionValidationResponseDTO

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**isValid** | **Boolean** | This attribute declares whether this definition is valid or not.  | 
**definitionType** | [**DefinitionTypeEnum**](#DefinitionTypeEnum) | This attribute declares whether this definition is a swagger or WSDL  |  [optional]
**wsdlInfo** | [**APIDefinitionValidationResponseWsdlInfoDTO**](APIDefinitionValidationResponseWsdlInfoDTO.md) |  |  [optional]


<a name="DefinitionTypeEnum"></a>
## Enum: DefinitionTypeEnum
Name | Value
---- | -----
SWAGGER | &quot;SWAGGER&quot;
WSDL | &quot;WSDL&quot;



