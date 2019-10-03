# MediationPoliciesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdMediationPoliciesGet**](MediationPoliciesApi.md#apisApiIdMediationPoliciesGet) | **GET** /apis/{apiId}/mediation-policies | Get all mediation policies of an API 
[**apisApiIdMediationPoliciesMediationPolicyIdDelete**](MediationPoliciesApi.md#apisApiIdMediationPoliciesMediationPolicyIdDelete) | **DELETE** /apis/{apiId}/mediation-policies/{mediationPolicyId} | Delete an API specific mediation policy
[**apisApiIdMediationPoliciesMediationPolicyIdGet**](MediationPoliciesApi.md#apisApiIdMediationPoliciesMediationPolicyIdGet) | **GET** /apis/{apiId}/mediation-policies/{mediationPolicyId} | Get an API specific mediation policy
[**apisApiIdMediationPoliciesMediationPolicyIdPut**](MediationPoliciesApi.md#apisApiIdMediationPoliciesMediationPolicyIdPut) | **PUT** /apis/{apiId}/mediation-policies/{mediationPolicyId} | Update an API specific mediation policy
[**apisApiIdMediationPoliciesPost**](MediationPoliciesApi.md#apisApiIdMediationPoliciesPost) | **POST** /apis/{apiId}/mediation-policies | Add an API specific mediation policy
[**mediationPoliciesGet**](MediationPoliciesApi.md#mediationPoliciesGet) | **GET** /mediation-policies | Get all global level mediation policies 


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
//import org.wso2.am.integration.clients.publisher.api.v1.MediationPoliciesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

MediationPoliciesApi apiInstance = new MediationPoliciesApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String query = "query_example"; // String | -Not supported yet-
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    MediationListDTO result = apiInstance.apisApiIdMediationPoliciesGet(apiId, limit, offset, query, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MediationPoliciesApi#apisApiIdMediationPoliciesGet");
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
//import org.wso2.am.integration.clients.publisher.api.v1.MediationPoliciesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

MediationPoliciesApi apiInstance = new MediationPoliciesApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String mediationPolicyId = "mediationPolicyId_example"; // String | Mediation policy Id 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.apisApiIdMediationPoliciesMediationPolicyIdDelete(apiId, mediationPolicyId, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling MediationPoliciesApi#apisApiIdMediationPoliciesMediationPolicyIdDelete");
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
//import org.wso2.am.integration.clients.publisher.api.v1.MediationPoliciesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

MediationPoliciesApi apiInstance = new MediationPoliciesApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String mediationPolicyId = "mediationPolicyId_example"; // String | Mediation policy Id 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    MediationDTO result = apiInstance.apisApiIdMediationPoliciesMediationPolicyIdGet(apiId, mediationPolicyId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MediationPoliciesApi#apisApiIdMediationPoliciesMediationPolicyIdGet");
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

<a name="apisApiIdMediationPoliciesMediationPolicyIdPut"></a>
# **apisApiIdMediationPoliciesMediationPolicyIdPut**
> MediationDTO apisApiIdMediationPoliciesMediationPolicyIdPut(apiId, mediationPolicyId, body, ifMatch)

Update an API specific mediation policy

This operation can be used to update an existing mediation policy of an API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.MediationPoliciesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

MediationPoliciesApi apiInstance = new MediationPoliciesApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String mediationPolicyId = "mediationPolicyId_example"; // String | Mediation policy Id 
MediationDTO body = new MediationDTO(); // MediationDTO | Mediation policy object that needs to be updated 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    MediationDTO result = apiInstance.apisApiIdMediationPoliciesMediationPolicyIdPut(apiId, mediationPolicyId, body, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MediationPoliciesApi#apisApiIdMediationPoliciesMediationPolicyIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **mediationPolicyId** | **String**| Mediation policy Id  |
 **body** | [**MediationDTO**](MediationDTO.md)| Mediation policy object that needs to be updated  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**MediationDTO**](MediationDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdMediationPoliciesPost"></a>
# **apisApiIdMediationPoliciesPost**
> MediationDTO apisApiIdMediationPoliciesPost(body, apiId, ifMatch)

Add an API specific mediation policy

This operation can be used to add an API specifc mediation policy. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.MediationPoliciesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

MediationPoliciesApi apiInstance = new MediationPoliciesApi();
MediationDTO body = new MediationDTO(); // MediationDTO | mediation policy to upload
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    MediationDTO result = apiInstance.apisApiIdMediationPoliciesPost(body, apiId, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MediationPoliciesApi#apisApiIdMediationPoliciesPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**MediationDTO**](MediationDTO.md)| mediation policy to upload |
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**MediationDTO**](MediationDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="mediationPoliciesGet"></a>
# **mediationPoliciesGet**
> MediationListDTO mediationPoliciesGet(limit, offset, query, ifNoneMatch)

Get all global level mediation policies 

This operation provides you a list of available all global level mediation policies. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.MediationPoliciesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

MediationPoliciesApi apiInstance = new MediationPoliciesApi();
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String query = "query_example"; // String | -Not supported yet-
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    MediationListDTO result = apiInstance.mediationPoliciesGet(limit, offset, query, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MediationPoliciesApi#mediationPoliciesGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
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

