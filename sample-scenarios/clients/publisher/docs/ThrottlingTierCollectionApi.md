# ThrottlingTierCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**tiersTierLevelGet**](ThrottlingTierCollectionApi.md#tiersTierLevelGet) | **GET** /tiers/{tierLevel} | Get all tiers
[**tiersTierLevelPost**](ThrottlingTierCollectionApi.md#tiersTierLevelPost) | **POST** /tiers/{tierLevel} | Create a Tier


<a name="tiersTierLevelGet"></a>
# **tiersTierLevelGet**
> TierList tiersTierLevelGet(tierLevel, limit, offset, accept, ifNoneMatch)

Get all tiers

This operation can be used to list the available tiers for a given tier level. Tier level should be specified as a path parameter and should be one of &#x60;api&#x60;, &#x60;application&#x60; and &#x60;resource&#x60;. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.ThrottlingTierCollectionApi;


ThrottlingTierCollectionApi apiInstance = new ThrottlingTierCollectionApi();
String tierLevel = "tierLevel_example"; // String | List API or Application or Resource type tiers. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    TierList result = apiInstance.tiersTierLevelGet(tierLevel, limit, offset, accept, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ThrottlingTierCollectionApi#tiersTierLevelGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **tierLevel** | **String**| List API or Application or Resource type tiers.  | [enum: api, application, resource]
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]

### Return type

[**TierList**](TierList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="tiersTierLevelPost"></a>
# **tiersTierLevelPost**
> Tier tiersTierLevelPost(body, tierLevel, contentType)

Create a Tier

This operation can be used to create a new throttling tier. The only supported tier level is &#x60;api&#x60; tiers. &#x60;POST https://127.0.0.1:9443/api/am/publisher/v0.11/tiers/api&#x60;  **IMPORTANT:** * This is only effective when Advanced Throttling is disabled in the Server. If enabled, we need to use Admin REST API for throttling tiers modification related operations. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.ThrottlingTierCollectionApi;


ThrottlingTierCollectionApi apiInstance = new ThrottlingTierCollectionApi();
Tier body = new Tier(); // Tier | Tier object that should to be added 
String tierLevel = "tierLevel_example"; // String | List API or Application or Resource type tiers. 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
try {
    Tier result = apiInstance.tiersTierLevelPost(body, tierLevel, contentType);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ThrottlingTierCollectionApi#tiersTierLevelPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**Tier**](Tier.md)| Tier object that should to be added  |
 **tierLevel** | **String**| List API or Application or Resource type tiers.  | [enum: api]
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]

### Return type

[**Tier**](Tier.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

