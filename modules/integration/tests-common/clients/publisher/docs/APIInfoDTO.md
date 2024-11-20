

# APIInfoDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** |  |  [optional]
**name** | **String** |  |  [optional]
**description** | **String** |  |  [optional]
**context** | **String** |  |  [optional]
**additionalProperties** | [**List&lt;APIInfoAdditionalPropertiesDTO&gt;**](APIInfoAdditionalPropertiesDTO.md) | Map of custom properties of API |  [optional]
**additionalPropertiesMap** | [**Map&lt;String, APIInfoAdditionalPropertiesMapDTO&gt;**](APIInfoAdditionalPropertiesMapDTO.md) |  |  [optional]
**version** | **String** |  |  [optional]
**provider** | **String** | If the provider value is not given, the user invoking the API will be used as the provider.  |  [optional]
**type** | **String** |  |  [optional]
**audience** | [**AudienceEnum**](#AudienceEnum) | The audience of the API. Accepted values are PUBLIC, SINGLE |  [optional]
**audiences** | **List&lt;String&gt;** | The audiences of the API for jwt validation. Accepted values are any String values |  [optional]
**lifeCycleStatus** | **String** |  |  [optional]
**workflowStatus** | **String** |  |  [optional]
**hasThumbnail** | **Boolean** |  |  [optional]
**securityScheme** | **List&lt;String&gt;** |  |  [optional]
**createdTime** | **String** |  |  [optional]
**updatedTime** | **String** |  |  [optional]
**gatewayVendor** | **String** |  |  [optional]
**advertiseOnly** | **Boolean** |  |  [optional]



## Enum: AudienceEnum

Name | Value
---- | -----
PUBLIC | &quot;PUBLIC&quot;
SINGLE | &quot;SINGLE&quot;



