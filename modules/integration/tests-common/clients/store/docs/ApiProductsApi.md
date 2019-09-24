# ApiProductsApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apiProductsApiProductIdGet**](ApiProductsApi.md#apiProductsApiProductIdGet) | **GET** /api-products/{apiProductId}/ | Get details of an API Product 
[**apiProductsApiProductIdSwaggerGet**](ApiProductsApi.md#apiProductsApiProductIdSwaggerGet) | **GET** /api-products/{apiProductId}/swagger | Get swagger definition 
[**apiProductsApiProductIdThumbnailGet**](ApiProductsApi.md#apiProductsApiProductIdThumbnailGet) | **GET** /api-products/{apiProductId}/thumbnail | Get thumbnail image
[**apiProductsGet**](ApiProductsApi.md#apiProductsGet) | **GET** /api-products | Retrieve/Search API products 


<a name="apiProductsApiProductIdGet"></a>
# **apiProductsApiProductIdGet**
> APIProductDTO apiProductsApiProductIdGet(apiProductId, ifNoneMatch, xWSO2Tenant)

Get details of an API Product 

Using this operation, you can retrieve complete details of a single API Product. You need to provide the Id of the API Product to retrive it.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrive an API of a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used.  **NOTE:** * This operation does not require an Authorization header by default. But if it is provided, it will be validated and checked for permissions of the user, hence you may be able to see APIs which are restricted for special permissions/roles. \\n 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApiProductsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductsApi apiInstance = new ApiProductsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
try {
    APIProductDTO result = apiInstance.apiProductsApiProductIdGet(apiProductId, ifNoneMatch, xWSO2Tenant);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductsApi#apiProductsApiProductIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product.  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]

### Return type

[**APIProductDTO**](APIProductDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdSwaggerGet"></a>
# **apiProductsApiProductIdSwaggerGet**
> apiProductsApiProductIdSwaggerGet(apiProductId, labelName, environmentName, ifNoneMatch, xWSO2Tenant)

Get swagger definition 

You can use this operation to retrieve the swagger definition of an API product.   &#x60;X-WSO2-Tenant&#x60; header can be used to retrive the swagger definition an API product of a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used.  **NOTE:** * This operation does not require an Authorization header by default. But in order to see a restricted API&#39;s swagger definition, you need to provide Authorization header. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApiProductsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductsApi apiInstance = new ApiProductsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. 
String labelName = "labelName_example"; // String | Name of the API microgateway labels 
String environmentName = "environmentName_example"; // String | Name of the API gateway environment. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
try {
    apiInstance.apiProductsApiProductIdSwaggerGet(apiProductId, labelName, environmentName, ifNoneMatch, xWSO2Tenant);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductsApi#apiProductsApiProductIdSwaggerGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product.  |
 **labelName** | **String**| Name of the API microgateway labels  | [optional]
 **environmentName** | **String**| Name of the API gateway environment.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdThumbnailGet"></a>
# **apiProductsApiProductIdThumbnailGet**
> apiProductsApiProductIdThumbnailGet(apiProductId, xWSO2Tenant, ifNoneMatch)

Get thumbnail image

This operation can be used to download a thumbnail image of an API Product.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrive a thumbnail of an API that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used.  **NOTE:** * This operation does not require an Authorization header by default. But in order to see a restricted API&#39;s thumbnail, you need to provide Authorization header. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApiProductsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductsApi apiInstance = new ApiProductsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
try {
    apiInstance.apiProductsApiProductIdThumbnailGet(apiProductId, xWSO2Tenant, ifNoneMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductsApi#apiProductsApiProductIdThumbnailGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product.  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsGet"></a>
# **apiProductsGet**
> APIProductListDTO apiProductsGet(limit, offset, xWSO2Tenant, query, ifNoneMatch)

Retrieve/Search API products 

This operation provides you a list of available API products qualifying under a given search condition.  Each retrieved API product is represented with a minimal amount of attributes. If you want to get complete details of an API, you need to use **Get details of an API Product** operation.  This operation supports retriving APIs of other tenants. The required tenant domain need to be specified as a header &#x60;X-WSO2-Tenant&#x60;. If not specified super tenant&#39;s APIs will be retrieved. If you used an Authorization header, the user&#39;s tenant 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApiProductsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiProductsApi apiInstance = new ApiProductsApi();
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String query = "query_example"; // String | **Search condition**.  You can search in attributes by using an **\"<attribute>:\"** modifier.  Eg. \"provider:wso2\" will match an API if the provider of the API is exactly \"wso2\".  Additionally you can use wildcards.  Eg. \"provider:wso2*\" will match an API if the provider of the API starts with \"wso2\".  Supported attribute modifiers are [**version, context, status, description, subcontext, doc, provider, tag**]  If no advanced attribute modifier has been specified, search will match the given query string against API Name. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
try {
    APIProductListDTO result = apiInstance.apiProductsGet(limit, offset, xWSO2Tenant, query, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiProductsApi#apiProductsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **query** | **String**| **Search condition**.  You can search in attributes by using an **\&quot;&lt;attribute&gt;:\&quot;** modifier.  Eg. \&quot;provider:wso2\&quot; will match an API if the provider of the API is exactly \&quot;wso2\&quot;.  Additionally you can use wildcards.  Eg. \&quot;provider:wso2*\&quot; will match an API if the provider of the API starts with \&quot;wso2\&quot;.  Supported attribute modifiers are [**version, context, status, description, subcontext, doc, provider, tag**]  If no advanced attribute modifier has been specified, search will match the given query string against API Name.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**APIProductListDTO**](APIProductListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

