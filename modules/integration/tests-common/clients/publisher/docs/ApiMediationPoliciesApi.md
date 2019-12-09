# ApiMediationPoliciesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdMediationPoliciesGet**](ApiMediationPoliciesApi.md#apisApiIdMediationPoliciesGet) | **GET** /apis/{apiId}/mediation-policies | Get all mediation policies of an API 
[**apisApiIdMediationPoliciesPost**](ApiMediationPoliciesApi.md#apisApiIdMediationPoliciesPost) | **POST** /apis/{apiId}/mediation-policies | Add an API specific mediation policy


<a name="apisApiIdMediationPoliciesGet"></a>
# **apisApiIdMediationPoliciesGet**
> MediationListDTO apisApiIdMediationPoliciesGet(apiId, limit, offset, query, ifNoneMatch)

Get all mediation policies of an API 

This operation provides you a list of available mediation policies of an API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiMediationPoliciesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiMediationPoliciesApi apiInstance = new ApiMediationPoliciesApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String query = "query_example"; // String | -Not supported yet-
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    MediationListDTO result = apiInstance.apisApiIdMediationPoliciesGet(apiId, limit, offset, query, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiMediationPoliciesApi#apisApiIdMediationPoliciesGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **query** | **String**| -Not supported yet- | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**MediationListDTO**](MediationListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdMediationPoliciesPost"></a>
# **apisApiIdMediationPoliciesPost**
> MediationDTO apisApiIdMediationPoliciesPost(type, apiId, mediationPolicyFile, inlineContent, ifMatch)

Add an API specific mediation policy

This operation can be used to add an API specifc mediation policy. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiMediationPoliciesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiMediationPoliciesApi apiInstance = new ApiMediationPoliciesApi();
String type = "type_example"; // String | Type of the mediation sequence
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
File mediationPolicyFile = new File("/path/to/file.txt"); // File | Mediation Policy to upload
String inlineContent = "inlineContent_example"; // String | Inline content of the Mediation Policy
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    MediationDTO result = apiInstance.apisApiIdMediationPoliciesPost(type, apiId, mediationPolicyFile, inlineContent, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiMediationPoliciesApi#apisApiIdMediationPoliciesPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **type** | **String**| Type of the mediation sequence |
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **mediationPolicyFile** | **File**| Mediation Policy to upload | [optional]
 **inlineContent** | **String**| Inline content of the Mediation Policy | [optional]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**MediationDTO**](MediationDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

