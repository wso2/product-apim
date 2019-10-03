
# APIProductDTO

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** | UUID of the api product  |  [optional]
**name** | **String** | Name of the API product | 
**description** | **String** | A brief description about the API product |  [optional]
**context** | **String** | A string that represents thecontext of the user&#39;s request |  [optional]
**provider** | **String** | If the provider value is not given user invoking the api will be used as the provider.  | 
**apiDefinition** | **String** | Swagger definition of the API product which contains details about URI templates and scopes  | 
**tiers** | **List&lt;String&gt;** | The subscription tiers selected for the particular API product |  [optional]
**thumbnailUrl** | **String** |  |  [optional]
**additionalProperties** | **Map&lt;String, String&gt;** | Custom(user defined) properties of API product  |  [optional]
**endpointURLs** | [**List&lt;APIProductEndpointURLsDTO&gt;**](APIProductEndpointURLsDTO.md) |  |  [optional]
**businessInformation** | [**APIProductBusinessInformationDTO**](APIProductBusinessInformationDTO.md) |  |  [optional]
**environmentList** | **List&lt;String&gt;** | The environment list configured with non empty endpoint URLs for the particular API. |  [optional]



