# RatingsApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdRatingsGet**](RatingsApi.md#apisApiIdRatingsGet) | **GET** /apis/{apiId}/ratings | Get API ratings
[**apisApiIdRatingsRatingIdGet**](RatingsApi.md#apisApiIdRatingsRatingIdGet) | **GET** /apis/{apiId}/ratings/{ratingId} | Get a single API rating
[**apisApiIdUserRatingPut**](RatingsApi.md#apisApiIdUserRatingPut) | **PUT** /apis/{apiId}/user-rating | Add or update logged in user&#39;s rating for an API


<a name="apisApiIdRatingsGet"></a>
# **apisApiIdRatingsGet**
> RatingListDTO apisApiIdRatingsGet(apiId, limit, offset)

Get API ratings

Get the rating of an API. 

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
try {
    RatingListDTO result = apiInstance.apisApiIdRatingsGet(apiId, limit, offset);
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

### Return type

[**RatingListDTO**](RatingListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdRatingsRatingIdGet"></a>
# **apisApiIdRatingsRatingIdGet**
> RatingDTO apisApiIdRatingsRatingIdGet(apiId, ratingId, ifNoneMatch)

Get a single API rating

Get a specific rating of an API. 

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
String ratingId = "ratingId_example"; // String | Rating Id 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
try {
    RatingDTO result = apiInstance.apisApiIdRatingsRatingIdGet(apiId, ratingId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling RatingsApi#apisApiIdRatingsRatingIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **ratingId** | **String**| Rating Id  |
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
> RatingDTO apisApiIdUserRatingPut(apiId, body)

Add or update logged in user&#39;s rating for an API

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
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
RatingDTO body = new RatingDTO(); // RatingDTO | Rating object that should to be added 
try {
    RatingDTO result = apiInstance.apisApiIdUserRatingPut(apiId, body);
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

### Return type

[**RatingDTO**](RatingDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

