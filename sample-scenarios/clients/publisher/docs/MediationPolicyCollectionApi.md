# MediationPolicyCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdPoliciesMediationGet**](MediationPolicyCollectionApi.md#apisApiIdPoliciesMediationGet) | **GET** /apis/{apiId}/policies/mediation | Get all mediation policies of an API 
[**apisApiIdPoliciesMediationPost**](MediationPolicyCollectionApi.md#apisApiIdPoliciesMediationPost) | **POST** /apis/{apiId}/policies/mediation | Add an API specific mediation policy
[**policiesMediationGet**](MediationPolicyCollectionApi.md#policiesMediationGet) | **GET** /policies/mediation | Get all global level mediation policies 


<a name="apisApiIdPoliciesMediationGet"></a>
# **apisApiIdPoliciesMediationGet**
> MediationList apisApiIdPoliciesMediationGet(apiId, limit, offset, query, accept, ifNoneMatch)

Get all mediation policies of an API 

This operation provides you a list of available mediation policies of an API. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.MediationPolicyCollectionApi;


MediationPolicyCollectionApi apiInstance = new MediationPolicyCollectionApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String query = "query_example"; // String | -Not supported yet-
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    MediationList result = apiInstance.apisApiIdPoliciesMediationGet(apiId, limit, offset, query, accept, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MediationPolicyCollectionApi#apisApiIdPoliciesMediationGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  |
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **query** | **String**| -Not supported yet- | [optional]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]

### Return type

[**MediationList**](MediationList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdPoliciesMediationPost"></a>
# **apisApiIdPoliciesMediationPost**
> Mediation apisApiIdPoliciesMediationPost(body, apiId, contentType, ifMatch, ifUnmodifiedSince)

Add an API specific mediation policy

This operation can be used to add an API specifc mediation policy. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.MediationPolicyCollectionApi;


MediationPolicyCollectionApi apiInstance = new MediationPolicyCollectionApi();
Mediation body = new Mediation(); // Mediation | mediation policy to upload
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**. 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    Mediation result = apiInstance.apisApiIdPoliciesMediationPost(body, apiId, contentType, ifMatch, ifUnmodifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MediationPolicyCollectionApi#apisApiIdPoliciesMediationPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**Mediation**](Mediation.md)| mediation policy to upload |
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag (Will be supported in future).  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

[**Mediation**](Mediation.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="policiesMediationGet"></a>
# **policiesMediationGet**
> MediationList policiesMediationGet(limit, offset, query, accept, ifNoneMatch)

Get all global level mediation policies 

This operation provides you a list of available all global level mediation policies. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.MediationPolicyCollectionApi;


MediationPolicyCollectionApi apiInstance = new MediationPolicyCollectionApi();
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String query = "query_example"; // String | -Not supported yet-
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    MediationList result = apiInstance.policiesMediationGet(limit, offset, query, accept, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MediationPolicyCollectionApi#policiesMediationGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **query** | **String**| -Not supported yet- | [optional]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]

### Return type

[**MediationList**](MediationList.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

