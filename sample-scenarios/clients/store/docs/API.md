
# API

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** | UUID of the api registry artifact  |  [optional]
**name** | **String** | Name of the API | 
**description** | **String** | A brief description about the API |  [optional]
**context** | **String** | A string that represents thecontext of the user&#39;s request | 
**version** | **String** | The version of the API | 
**provider** | **String** | If the provider value is not given user invoking the api will be used as the provider.  | 
**apiDefinition** | **String** | Swagger definition of the API which contains details about URI templates and scopes  | 
**wsdlUri** | **String** | WSDL URL if the API is based on a WSDL endpoint  |  [optional]
**status** | **String** | This describes in which status of the lifecycle the API is. | 
**isDefaultVersion** | **Boolean** |  |  [optional]
**transport** | **List&lt;String&gt;** |  |  [optional]
**tags** | **List&lt;String&gt;** | Search keywords related to the API |  [optional]
**tiers** | **List&lt;String&gt;** | The subscription tiers selected for the particular API |  [optional]
**thumbnailUrl** | **String** |  |  [optional]
**endpointURLs** | [**List&lt;APIEndpointURLs&gt;**](APIEndpointURLs.md) |  |  [optional]
**businessInformation** | [**APIBusinessInformation**](APIBusinessInformation.md) |  |  [optional]



