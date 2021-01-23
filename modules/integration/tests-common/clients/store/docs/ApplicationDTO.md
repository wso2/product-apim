

# ApplicationDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**applicationId** | **String** |  |  [optional] [readonly]
**name** | **String** |  | 
**throttlingPolicy** | **String** |  | 
**description** | **String** |  |  [optional]
**tokenType** | [**TokenTypeEnum**](#TokenTypeEnum) | Type of the access token generated for this application.  **OAUTH:** A UUID based access token **JWT:** A self-contained, signed JWT based access token which is issued by default.  |  [optional]
**status** | **String** |  |  [optional] [readonly]
**groups** | **List&lt;String&gt;** |  |  [optional]
**subscriptionCount** | **Integer** |  |  [optional] [readonly]
**keys** | [**List&lt;ApplicationKeyDTO&gt;**](ApplicationKeyDTO.md) |  |  [optional] [readonly]
**attributes** | **Map&lt;String, String&gt;** |  |  [optional]
**subscriptionScopes** | [**List&lt;ScopeInfoDTO&gt;**](ScopeInfoDTO.md) |  |  [optional]
**owner** | **String** | Application created user  |  [optional] [readonly]
**hashEnabled** | **Boolean** |  |  [optional] [readonly]



## Enum: TokenTypeEnum

Name | Value
---- | -----
OAUTH | &quot;OAUTH&quot;
JWT | &quot;JWT&quot;



