

# ApplicationKeyDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**keyMappingId** | **String** | Key Manager Mapping UUID |  [optional] [readonly]
**keyManager** | **String** | Key Manager Name |  [optional]
**consumerKey** | **String** | Consumer key of the application |  [optional] [readonly]
**consumerSecret** | **String** | Consumer secret of the application |  [optional] [readonly]
**supportedGrantTypes** | **List&lt;String&gt;** | The grant types that are supported by the application |  [optional]
**callbackUrl** | **String** | Callback URL |  [optional]
**keyState** | **String** | Describes the state of the key generation. |  [optional]
**keyType** | [**KeyTypeEnum**](#KeyTypeEnum) | Describes to which endpoint the key belongs |  [optional]
**mode** | [**ModeEnum**](#ModeEnum) | Describe the which mode Application Mapped. |  [optional]
**groupId** | **String** | Application group id (if any). |  [optional]
**token** | [**ApplicationTokenDTO**](ApplicationTokenDTO.md) |  |  [optional]
**additionalProperties** | [**Object**](.md) | additionalProperties (if any). |  [optional]



## Enum: KeyTypeEnum

Name | Value
---- | -----
PRODUCTION | &quot;PRODUCTION&quot;
SANDBOX | &quot;SANDBOX&quot;



## Enum: ModeEnum

Name | Value
---- | -----
MAPPED | &quot;MAPPED&quot;
CREATED | &quot;CREATED&quot;



