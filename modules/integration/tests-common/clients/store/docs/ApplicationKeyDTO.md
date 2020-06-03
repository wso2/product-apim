
# ApplicationKeyDTO

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**keyMappingId** | **String** | Key Manager Mapping UUID |  [optional]
**keyManager** | **String** | Key Manager Name |  [optional]
**consumerKey** | **String** | Consumer key of the application |  [optional]
**consumerSecret** | **String** | Consumer secret of the application |  [optional]
**supportedGrantTypes** | **List&lt;String&gt;** | The grant types that are supported by the application |  [optional]
**callbackUrl** | **String** | Callback URL |  [optional]
**keyState** | **String** | Describes the state of the key generation. |  [optional]
**keyType** | [**KeyTypeEnum**](#KeyTypeEnum) | Describes to which endpoint the key belongs |  [optional]
**groupId** | **String** | Application group id (if any). |  [optional]
**token** | [**ApplicationTokenDTO**](ApplicationTokenDTO.md) |  |  [optional]
**additionalProperties** | **Object** | additionalProperties (if any). |  [optional]


<a name="KeyTypeEnum"></a>
## Enum: KeyTypeEnum
Name | Value
---- | -----
PRODUCTION | &quot;PRODUCTION&quot;
SANDBOX | &quot;SANDBOX&quot;



