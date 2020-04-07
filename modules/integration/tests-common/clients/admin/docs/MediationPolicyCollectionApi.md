# MediationPolicyCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.16*

Method | HTTP request | Description
------------- | ------------- | -------------
[**policiesMediationGet**](MediationPolicyCollectionApi.md#policiesMediationGet) | **GET** /policies/mediation | Get all global mediation policies 
[**policiesMediationPost**](MediationPolicyCollectionApi.md#policiesMediationPost) | **POST** /policies/mediation | Add a global mediation policy


<a name="policiesMediationGet"></a>
# **policiesMediationGet**
> MediationListDTO policiesMediationGet(limit, offset, query, accept, ifNoneMatch)

Get all global mediation policies 

This operation provides you a list of available all global level mediation policies. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.MediationPolicyCollectionApi;


MediationPolicyCollectionApi apiInstance = new MediationPolicyCollectionApi();
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String query = "query_example"; // String | -Not supported yet-
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    MediationListDTO result = apiInstance.policiesMediationGet(limit, offset, query, accept, ifNoneMatch);
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

[**MediationListDTO**](MediationListDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="policiesMediationPost"></a>
# **policiesMediationPost**
> MediationDTO policiesMediationPost(body, contentType, ifMatch, ifUnmodifiedSince)

Add a global mediation policy

This operation can be used to add a new global mediation policy. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.MediationPolicyCollectionApi;


MediationPolicyCollectionApi apiInstance = new MediationPolicyCollectionApi();
MediationDTO body = new MediationDTO(); // MediationDTO | mediation policy to upload
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    MediationDTO result = apiInstance.policiesMediationPost(body, contentType, ifMatch, ifUnmodifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MediationPolicyCollectionApi#policiesMediationPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**MediationDTO**](MediationDTO.md)| mediation policy to upload |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag (Will be supported in future).  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

[**MediationDTO**](MediationDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

