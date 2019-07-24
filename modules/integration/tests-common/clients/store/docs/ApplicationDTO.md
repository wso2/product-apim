
# ApplicationDTO

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**applicationId** | **String** |  |  [optional]
**name** | **String** |  | 
**subscriber** | **String** | If subscriber is not given user invoking the API will be taken as the subscriber.  |  [optional]
**throttlingPolicy** | **String** |  | 
**description** | **String** |  |  [optional]
**tokenType** | [**TokenTypeEnum**](#TokenTypeEnum) | Type of the access token generated for this application.  **OAUTH:** A UUID based access token which is issued by default. **JWT:** A self-contained, signed JWT based access token. **Note:** This can be only used in Microgateway environments.  |  [optional]
**status** | **String** |  |  [optional]
**groups** | **List&lt;String&gt;** |  |  [optional]
**subscriptionCount** | **Integer** |  |  [optional]
**keys** | [**List&lt;ApplicationKeyDTO&gt;**](ApplicationKeyDTO.md) |  |  [optional]
**attributes** | **Map&lt;String, String&gt;** |  |  [optional]
**subscriptionScopes** | **List&lt;String&gt;** |  |  [optional]


<a name="TokenTypeEnum"></a>
## Enum: TokenTypeEnum
Name | Value
---- | -----
OAUTH | &quot;OAUTH&quot;
JWT | &quot;JWT&quot;



