

# APIProductInfoDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** | UUID of the api product  |  [optional] [readonly]
**name** | **String** | Name of the API Product |  [optional]
**context** | **String** |  |  [optional]
**description** | **String** | A brief description about the API |  [optional]
**provider** | **String** | If the provider value is not given, the user invoking the API will be used as the provider.  |  [optional]
**hasThumbnail** | **Boolean** |  |  [optional]
**state** | [**StateEnum**](#StateEnum) | State of the API product. Only published api products are visible on the Developer Portal  |  [optional]
**securityScheme** | **List&lt;String&gt;** | Types of API security, the current API secured with. It can be either OAuth2 or mutual SSL or both. If it is not set OAuth2 will be set as the security for the current API.  |  [optional]
**gatewayVendor** | **String** |  |  [optional]



## Enum: StateEnum

Name | Value
---- | -----
CREATED | &quot;CREATED&quot;
PUBLISHED | &quot;PUBLISHED&quot;



