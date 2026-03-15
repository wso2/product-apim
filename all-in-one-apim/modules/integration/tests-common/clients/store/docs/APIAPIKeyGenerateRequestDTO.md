

# APIAPIKeyGenerateRequestDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**keyName** | **String** | API Key name |  [optional]
**keyType** | [**KeyTypeEnum**](#KeyTypeEnum) | Type of the API key |  [optional]
**validityPeriod** | **Integer** | API key validity period |  [optional]
**additionalProperties** | [**Object**](.md) | Additional parameters if Authorization server needs any |  [optional]



## Enum: KeyTypeEnum

Name | Value
---- | -----
PRODUCTION | &quot;PRODUCTION&quot;
SANDBOX | &quot;SANDBOX&quot;



