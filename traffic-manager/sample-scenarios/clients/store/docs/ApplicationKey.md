
# ApplicationKey

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**consumerKey** | **String** | The consumer key associated with the application and identifying the client |  [optional]
**consumerSecret** | **String** | The client secret that is used to authenticate the client with the authentication server |  [optional]
**supportedGrantTypes** | **List&lt;String&gt;** | The grant types that are supported by the application |  [optional]
**callbackUrl** | **String** | Callback URL |  [optional]
**keyState** | **String** | Describes the state of the key generation. |  [optional]
**keyType** | [**KeyTypeEnum**](#KeyTypeEnum) | Describes to which endpoint the key belongs |  [optional]
**groupId** | **String** | Application group id (if any). |  [optional]
**token** | [**Token**](Token.md) |  |  [optional]


<a name="KeyTypeEnum"></a>
## Enum: KeyTypeEnum
Name | Value
---- | -----
PRODUCTION | &quot;PRODUCTION&quot;
SANDBOX | &quot;SANDBOX&quot;



