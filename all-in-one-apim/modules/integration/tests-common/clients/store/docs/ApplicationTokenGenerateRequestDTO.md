

# ApplicationTokenGenerateRequestDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**consumerSecret** | **String** | Consumer secret of the application |  [optional]
**validityPeriod** | **Long** | Token validity period |  [optional]
**scopes** | **List&lt;String&gt;** | Allowed scopes (space seperated) for the access token |  [optional]
**revokeToken** | **String** | Token to be revoked, if any |  [optional]
**grantType** | [**GrantTypeEnum**](#GrantTypeEnum) |  |  [optional]
**additionalProperties** | [**Object**](.md) | Additional parameters if Authorization server needs any |  [optional]



## Enum: GrantTypeEnum

Name | Value
---- | -----
CLIENT_CREDENTIALS | &quot;CLIENT_CREDENTIALS&quot;
TOKEN_EXCHANGE | &quot;TOKEN_EXCHANGE&quot;



