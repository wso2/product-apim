# RatingsApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apiProductsApiProductIdRatingsGet**](RatingsApi.md#apiProductsApiProductIdRatingsGet) | **GET** /api-products/{apiProductId}/ratings | Get API Product ratings
[**apiProductsApiProductIdRatingsRatingIdGet**](RatingsApi.md#apiProductsApiProductIdRatingsRatingIdGet) | **GET** /api-products/{apiProductId}/ratings/{ratingId} | Get a single API Product rating
[**apiProductsApiProductIdUserRatingPut**](RatingsApi.md#apiProductsApiProductIdUserRatingPut) | **PUT** /api-products/{apiProductId}/user-rating | Add or update logged in user&#39;s rating for an API Product
[**apisApiIdRatingsGet**](RatingsApi.md#apisApiIdRatingsGet) | **GET** /apis/{apiId}/ratings | Retrieve API ratings
[**apisApiIdUserRatingDelete**](RatingsApi.md#apisApiIdUserRatingDelete) | **DELETE** /apis/{apiId}/user-rating | Delete user API rating
[**apisApiIdUserRatingGet**](RatingsApi.md#apisApiIdUserRatingGet) | **GET** /apis/{apiId}/user-rating | Retrieve API rating of user
[**apisApiIdUserRatingPut**](RatingsApi.md#apisApiIdUserRatingPut) | **PUT** /apis/{apiId}/user-rating | Add or update logged in user&#39;s rating for an API


<a name="apiProductsApiProductIdRatingsGet"></a>
# **apiProductsApiProductIdRatingsGet**
> RatingListDTO apiProductsApiProductIdRatingsGet(apiProductId, limit, offset)

Get API Product ratings

Get the rating of an API Product. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.RatingsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

RatingsApi apiInstance = new RatingsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
try {
    RatingListDTO result = apiInstance.apiProductsApiProductIdRatingsGet(apiProductId, limit, offset);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling RatingsApi#apiProductsApiProductIdRatingsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product.  |
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]

### Return type

[**RatingListDTO**](RatingListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdRatingsRatingIdGet"></a>
# **apiProductsApiProductIdRatingsRatingIdGet**
> RatingDTO apiProductsApiProductIdRatingsRatingIdGet(apiProductId, ratingId, ifNoneMatch)

Get a single API Product rating

Get a specific rating of an API Product. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.RatingsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

RatingsApi apiInstance = new RatingsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. 
String ratingId = "ratingId_example"; // String | Rating Id 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
try {
    RatingDTO result = apiInstance.apiProductsApiProductIdRatingsRatingIdGet(apiProductId, ratingId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling RatingsApi#apiProductsApiProductIdRatingsRatingIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product.  |
 **ratingId** | **String**| Rating Id  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**RatingDTO**](RatingDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apiProductsApiProductIdUserRatingPut"></a>
# **apiProductsApiProductIdUserRatingPut**
> RatingDTO apiProductsApiProductIdUserRatingPut(apiProductId, body)

Add or update logged in user&#39;s rating for an API Product

Adds or updates a rating 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.RatingsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

RatingsApi apiInstance = new RatingsApi();
String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. 
RatingDTO body = new RatingDTO(); // RatingDTO | Rating object that should to be added 
try {
    RatingDTO result = apiInstance.apiProductsApiProductIdUserRatingPut(apiProductId, body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling RatingsApi#apiProductsApiProductIdUserRatingPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product.  |
 **body** | [**RatingDTO**](RatingDTO.md)| Rating object that should to be added  |

### Return type

[**RatingDTO**](RatingDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdRatingsGet"></a>
# **apisApiIdRatingsGet**
> RatingListDTO apisApiIdRatingsGet(apiId, limit, offset, xWSO2Tenant)

Retrieve API ratings

This operation can be used to retrieve the list of ratings of an API.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrieve ratings of an API that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.RatingsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

RatingsApi apiInstance = new RatingsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
try {
    RatingListDTO result = apiInstance.apisApiIdRatingsGet(apiId, limit, offset, xWSO2Tenant);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling RatingsApi#apisApiIdRatingsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]

### Return type

[**RatingListDTO**](RatingListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdUserRatingDelete"></a>
# **apisApiIdUserRatingDelete**
> apisApiIdUserRatingDelete(apiId, xWSO2Tenant, ifMatch)

Delete user API rating

This operation can be used to delete logged in user API rating.  &#x60;X-WSO2-Tenant&#x60; header can be used to delete the logged in user rating of an API that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.RatingsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

RatingsApi apiInstance = new RatingsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.apisApiIdUserRatingDelete(apiId, xWSO2Tenant, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling RatingsApi#apisApiIdUserRatingDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdUserRatingGet"></a>
# **apisApiIdUserRatingGet**
> RatingDTO apisApiIdUserRatingGet(apiId, xWSO2Tenant, ifNoneMatch)

Retrieve API rating of user

This operation can be used to get the user rating of an API.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrieve the logged in user rating of an API that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.RatingsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

RatingsApi apiInstance = new RatingsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
try {
    RatingDTO result = apiInstance.apisApiIdUserRatingGet(apiId, xWSO2Tenant, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling RatingsApi#apisApiIdUserRatingGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**RatingDTO**](RatingDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdUserRatingPut"></a>
# **apisApiIdUserRatingPut**
> RatingDTO apisApiIdUserRatingPut(apiId, body, xWSO2Tenant)

Add or update logged in user&#39;s rating for an API

This operation can be used to add or update an API rating.  &#x60;X-WSO2-Tenant&#x60; header can be used to add or update the logged in user rating of an API that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.RatingsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

RatingsApi apiInstance = new RatingsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
RatingDTO body = new RatingDTO(); // RatingDTO | Rating object that should to be added 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
try {
    RatingDTO result = apiInstance.apisApiIdUserRatingPut(apiId, body, xWSO2Tenant);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling RatingsApi#apisApiIdUserRatingPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **body** | [**RatingDTO**](RatingDTO.md)| Rating object that should to be added  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]

### Return type

[**RatingDTO**](RatingDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

