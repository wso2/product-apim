# ApiMediationPolicyApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdMediationPoliciesMediationPolicyIdContentGet**](ApiMediationPolicyApi.md#apisApiIdMediationPoliciesMediationPolicyIdContentGet) | **GET** /apis/{apiId}/mediation-policies/{mediationPolicyId}/content | Download an API specific mediation policy
[**apisApiIdMediationPoliciesMediationPolicyIdContentPut**](ApiMediationPolicyApi.md#apisApiIdMediationPoliciesMediationPolicyIdContentPut) | **PUT** /apis/{apiId}/mediation-policies/{mediationPolicyId}/content | Update an API specific mediation policy
[**apisApiIdMediationPoliciesMediationPolicyIdDelete**](ApiMediationPolicyApi.md#apisApiIdMediationPoliciesMediationPolicyIdDelete) | **DELETE** /apis/{apiId}/mediation-policies/{mediationPolicyId} | Delete an API specific mediation policy
[**apisApiIdMediationPoliciesMediationPolicyIdGet**](ApiMediationPolicyApi.md#apisApiIdMediationPoliciesMediationPolicyIdGet) | **GET** /apis/{apiId}/mediation-policies/{mediationPolicyId} | Get an API specific mediation policy


<a name="apisApiIdMediationPoliciesMediationPolicyIdContentGet"></a>
# **apisApiIdMediationPoliciesMediationPolicyIdContentGet**
> apisApiIdMediationPoliciesMediationPolicyIdContentGet(apiId, mediationPolicyId, ifNoneMatch)

Download an API specific mediation policy

This operation can be used to download a particular API specific mediation policy. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiMediationPolicyApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiMediationPolicyApi apiInstance = new ApiMediationPolicyApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String mediationPolicyId = "mediationPolicyId_example"; // String | Mediation policy Id 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    apiInstance.apisApiIdMediationPoliciesMediationPolicyIdContentGet(apiId, mediationPolicyId, ifNoneMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiMediationPolicyApi#apisApiIdMediationPoliciesMediationPolicyIdContentGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **mediationPolicyId** | **String**| Mediation policy Id  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdMediationPoliciesMediationPolicyIdContentPut"></a>
# **apisApiIdMediationPoliciesMediationPolicyIdContentPut**
> MediationDTO apisApiIdMediationPoliciesMediationPolicyIdContentPut(type, apiId, mediationPolicyId, file, inlineContent, ifMatch)

Update an API specific mediation policy

This operation can be used to update an existing mediation policy of an API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiMediationPolicyApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiMediationPolicyApi apiInstance = new ApiMediationPolicyApi();
String type = "type_example"; // String | Type of the mediation sequence(in/out/fault)
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String mediationPolicyId = "mediationPolicyId_example"; // String | Mediation policy Id 
File file = new File("/path/to/file.txt"); // File | Mediation Policy to upload
String inlineContent = "inlineContent_example"; // String | Inline content of the Mediation Policy
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    MediationDTO result = apiInstance.apisApiIdMediationPoliciesMediationPolicyIdContentPut(type, apiId, mediationPolicyId, file, inlineContent, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiMediationPolicyApi#apisApiIdMediationPoliciesMediationPolicyIdContentPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **type** | **String**| Type of the mediation sequence(in/out/fault) |
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **mediationPolicyId** | **String**| Mediation policy Id  |
 **file** | **File**| Mediation Policy to upload | [optional]
 **inlineContent** | **String**| Inline content of the Mediation Policy | [optional]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**MediationDTO**](MediationDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a name="apisApiIdMediationPoliciesMediationPolicyIdDelete"></a>
# **apisApiIdMediationPoliciesMediationPolicyIdDelete**
> apisApiIdMediationPoliciesMediationPolicyIdDelete(apiId, mediationPolicyId, ifMatch)

Delete an API specific mediation policy

This operation can be used to delete an existing API specific mediation policy providing the Id of the API and the Id of the mediation policy. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiMediationPolicyApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiMediationPolicyApi apiInstance = new ApiMediationPolicyApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String mediationPolicyId = "mediationPolicyId_example"; // String | Mediation policy Id 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.apisApiIdMediationPoliciesMediationPolicyIdDelete(apiId, mediationPolicyId, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiMediationPolicyApi#apisApiIdMediationPoliciesMediationPolicyIdDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **mediationPolicyId** | **String**| Mediation policy Id  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdMediationPoliciesMediationPolicyIdGet"></a>
# **apisApiIdMediationPoliciesMediationPolicyIdGet**
> MediationDTO apisApiIdMediationPoliciesMediationPolicyIdGet(apiId, mediationPolicyId, ifNoneMatch)

Get an API specific mediation policy

This operation can be used to retrieve a particular API specific mediation policy. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiMediationPolicyApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiMediationPolicyApi apiInstance = new ApiMediationPolicyApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String mediationPolicyId = "mediationPolicyId_example"; // String | Mediation policy Id 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    MediationDTO result = apiInstance.apisApiIdMediationPoliciesMediationPolicyIdGet(apiId, mediationPolicyId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiMediationPolicyApi#apisApiIdMediationPoliciesMediationPolicyIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **mediationPolicyId** | **String**| Mediation policy Id  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**MediationDTO**](MediationDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

