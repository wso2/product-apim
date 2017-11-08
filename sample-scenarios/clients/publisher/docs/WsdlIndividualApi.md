# WsdlIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdWsdlGet**](WsdlIndividualApi.md#apisApiIdWsdlGet) | **GET** /apis/{apiId}/wsdl | Get the WSDL of an API
[**apisApiIdWsdlPost**](WsdlIndividualApi.md#apisApiIdWsdlPost) | **POST** /apis/{apiId}/wsdl | Add a WSDL to an API


<a name="apisApiIdWsdlGet"></a>
# **apisApiIdWsdlGet**
> Wsdl apisApiIdWsdlGet(apiId, accept, ifNoneMatch, ifModifiedSince)

Get the WSDL of an API

This operation can be used to retrieve the WSDL definition of an API. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.WsdlIndividualApi;


WsdlIndividualApi apiInstance = new WsdlIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    Wsdl result = apiInstance.apisApiIdWsdlGet(apiId, accept, ifNoneMatch, ifModifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling WsdlIndividualApi#apisApiIdWsdlGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  |
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]
 **ifModifiedSince** | **String**| Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]

### Return type

[**Wsdl**](Wsdl.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdWsdlPost"></a>
# **apisApiIdWsdlPost**
> apisApiIdWsdlPost(apiId, body, contentType, ifMatch, ifUnmodifiedSince)

Add a WSDL to an API

This operation can be used to add a WSDL definition to an existing API. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.WsdlIndividualApi;


WsdlIndividualApi apiInstance = new WsdlIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**. 
Wsdl body = new Wsdl(); // Wsdl | JSON payload including WSDL definition that needs to be added 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    apiInstance.apisApiIdWsdlPost(apiId, body, contentType, ifMatch, ifUnmodifiedSince);
} catch (ApiException e) {
    System.err.println("Exception when calling WsdlIndividualApi#apisApiIdWsdlPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  |
 **body** | [**Wsdl**](Wsdl.md)| JSON payload including WSDL definition that needs to be added  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag (Will be supported in future).  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

