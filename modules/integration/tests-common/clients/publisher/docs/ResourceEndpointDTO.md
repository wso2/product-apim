

# ResourceEndpointDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** |  |  [optional]
**name** | **String** |  | 
**endpointType** | [**EndpointTypeEnum**](#EndpointTypeEnum) |  | 
**url** | **String** |  | 
**securityConfig** | **Map&lt;String, String&gt;** |  |  [optional]
**generalConfig** | **Map&lt;String, String&gt;** |  |  [optional]
**usageCount** | **Integer** | Usage count of Resource Endpoint  |  [optional] [readonly]



## Enum: EndpointTypeEnum

Name | Value
---- | -----
HTTP | &quot;HTTP&quot;
ADDRESS | &quot;ADDRESS&quot;



