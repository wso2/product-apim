
# ApplicationKeyGenerateRequestDTO

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**keyType** | [**KeyTypeEnum**](#KeyTypeEnum) |  | 
**grantTypesToBeSupported** | **List&lt;String&gt;** | Grant types that should be supported by the application | 
**callbackUrl** | **String** | Callback URL |  [optional]
**scopes** | **List&lt;String&gt;** | Allowed scopes for the access token |  [optional]
**validityTime** | **String** |  |  [optional]
**clientId** | **String** | Client ID for generating access token. |  [optional]
**clientSecret** | **String** | Client secret for generating access token. This is given together with the client Id. |  [optional]


<a name="KeyTypeEnum"></a>
## Enum: KeyTypeEnum
Name | Value
---- | -----
PRODUCTION | &quot;PRODUCTION&quot;
SANDBOX | &quot;SANDBOX&quot;



