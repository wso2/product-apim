# APIIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdGet**](APIIndividualApi.md#apisApiIdGet) | **GET** /apis/{apiId} | Get details of an API 
[**apisApiIdSwaggerGet**](APIIndividualApi.md#apisApiIdSwaggerGet) | **GET** /apis/{apiId}/swagger | Get swagger definition 
[**apisApiIdThumbnailGet**](APIIndividualApi.md#apisApiIdThumbnailGet) | **GET** /apis/{apiId}/thumbnail | Get thumbnail image
[**apisGenerateSdkPost**](APIIndividualApi.md#apisGenerateSdkPost) | **POST** /apis/generate-sdk/ | Generate SDK for an API 


<a name="apisApiIdGet"></a>
# **apisApiIdGet**
> API apisApiIdGet(apiId, accept, ifNoneMatch, ifModifiedSince, xWSO2Tenant)

Get details of an API 

Using this operation, you can retrieve complete details of a single API. You need to provide the Id of the API to retrive it.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrive an API of a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used.  **NOTE:** * This operation does not require an Authorization header by default. But if it is provided, it will be validated and checked for permissions of the user, hence you may be able to see APIs which are restricted for special permissions/roles. \\n 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.APIIndividualApi;


APIIndividualApi apiInstance = new APIIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).  
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
try {
    API result = apiInstance.apisApiIdGet(apiId, accept, ifNoneMatch, ifModifiedSince, xWSO2Tenant);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling APIIndividualApi#apisApiIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  |
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]
 **ifModifiedSince** | **String**| Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).   | [optional]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]

### Return type

[**API**](API.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdSwaggerGet"></a>
# **apisApiIdSwaggerGet**
> apisApiIdSwaggerGet(apiId, accept, ifNoneMatch, ifModifiedSince, xWSO2Tenant)

Get swagger definition 

You can use this operation to retrieve the swagger definition of an API.   &#x60;X-WSO2-Tenant&#x60; header can be used to retrive the swagger definition an API of a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used.   **NOTE:** * This operation does not require an Authorization header by default. But in order to see a restricted API&#39;s swagger definition, you need to provide Authorization header. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.APIIndividualApi;


APIIndividualApi apiInstance = new APIIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).  
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
try {
    apiInstance.apisApiIdSwaggerGet(apiId, accept, ifNoneMatch, ifModifiedSince, xWSO2Tenant);
} catch (ApiException e) {
    System.err.println("Exception when calling APIIndividualApi#apisApiIdSwaggerGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  |
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]
 **ifModifiedSince** | **String**| Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).   | [optional]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdThumbnailGet"></a>
# **apisApiIdThumbnailGet**
> apisApiIdThumbnailGet(apiId, xWSO2Tenant, accept, ifNoneMatch, ifModifiedSince)

Get thumbnail image

This operation can be used to download a thumbnail image of an API.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrive a thumbnail of an API that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used.  **NOTE:** * This operation does not require an Authorization header by default. But in order to see a restricted API&#39;s thumbnail, you need to provide Authorization header.         

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.APIIndividualApi;


APIIndividualApi apiInstance = new APIIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).  
try {
    apiInstance.apisApiIdThumbnailGet(apiId, xWSO2Tenant, accept, ifNoneMatch, ifModifiedSince);
} catch (ApiException e) {
    System.err.println("Exception when calling APIIndividualApi#apisApiIdThumbnailGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]
 **ifModifiedSince** | **String**| Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).   | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisGenerateSdkPost"></a>
# **apisGenerateSdkPost**
> apisGenerateSdkPost(apiId, language, xWSO2Tenant)

Generate SDK for an API 

This operation can be used to generate SDK for an API by providing the id of the API along with the preferred language.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrive the generated SDK of an API that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used.  **NOTE:** * This operation does not require an Authorization header by default. But in order to generate a restricted API&#39;s SDK, you need to provide Authorization header. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.APIIndividualApi;


APIIndividualApi apiInstance = new APIIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**. 
String language = "language_example"; // String | Programming language to generate SDK. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
try {
    apiInstance.apisGenerateSdkPost(apiId, language, xWSO2Tenant);
} catch (ApiException e) {
    System.err.println("Exception when calling APIIndividualApi#apisGenerateSdkPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  |
 **language** | **String**| Programming language to generate SDK.  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

