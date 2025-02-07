# ApiMediationPolicyApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**deleteAPIMediationPolicyByPolicyId**](ApiMediationPolicyApi.md#deleteAPIMediationPolicyByPolicyId) | **DELETE** /apis/{apiId}/mediation-policies/{mediationPolicyId} | Delete an API Specific Mediation Policy
[**getAPIMediationPolicyByPolicyId**](ApiMediationPolicyApi.md#getAPIMediationPolicyByPolicyId) | **GET** /apis/{apiId}/mediation-policies/{mediationPolicyId} | Get an API Specific Mediation Policy
[**getAPIMediationPolicyContentByPolicyId**](ApiMediationPolicyApi.md#getAPIMediationPolicyContentByPolicyId) | **GET** /apis/{apiId}/mediation-policies/{mediationPolicyId}/content | Download an API Specific Mediation Policy
[**updateAPIMediationPolicyContentByPolicyId**](ApiMediationPolicyApi.md#updateAPIMediationPolicyContentByPolicyId) | **PUT** /apis/{apiId}/mediation-policies/{mediationPolicyId}/content | Update an API Specific Mediation Policy


<a name="deleteAPIMediationPolicyByPolicyId"></a>
# **deleteAPIMediationPolicyByPolicyId**
> deleteAPIMediationPolicyByPolicyId(apiId, mediationPolicyId, ifMatch)

Delete an API Specific Mediation Policy

This operation can be used to delete an existing API specific mediation policy providing the Id of the API and the Id of the mediation policy. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiMediationPolicyApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiMediationPolicyApi apiInstance = new ApiMediationPolicyApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String mediationPolicyId = "mediationPolicyId_example"; // String | Mediation policy Id 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      apiInstance.deleteAPIMediationPolicyByPolicyId(apiId, mediationPolicyId, ifMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiMediationPolicyApi#deleteAPIMediationPolicyByPolicyId");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
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

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Resource successfully deleted.  |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="getAPIMediationPolicyByPolicyId"></a>
# **getAPIMediationPolicyByPolicyId**
> MediationDTO getAPIMediationPolicyByPolicyId(apiId, mediationPolicyId, ifNoneMatch)

Get an API Specific Mediation Policy

This operation can be used to retrieve a particular API specific mediation policy. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiMediationPolicyApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiMediationPolicyApi apiInstance = new ApiMediationPolicyApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String mediationPolicyId = "mediationPolicyId_example"; // String | Mediation policy Id 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      MediationDTO result = apiInstance.getAPIMediationPolicyByPolicyId(apiId, mediationPolicyId, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiMediationPolicyApi#getAPIMediationPolicyByPolicyId");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
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

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Mediation policy returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getAPIMediationPolicyContentByPolicyId"></a>
# **getAPIMediationPolicyContentByPolicyId**
> getAPIMediationPolicyContentByPolicyId(apiId, mediationPolicyId, ifNoneMatch)

Download an API Specific Mediation Policy

This operation can be used to download a particular API specific mediation policy. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiMediationPolicyApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiMediationPolicyApi apiInstance = new ApiMediationPolicyApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String mediationPolicyId = "mediationPolicyId_example"; // String | Mediation policy Id 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      apiInstance.getAPIMediationPolicyContentByPolicyId(apiId, mediationPolicyId, ifNoneMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiMediationPolicyApi#getAPIMediationPolicyContentByPolicyId");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
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

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Mediation policy returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="updateAPIMediationPolicyContentByPolicyId"></a>
# **updateAPIMediationPolicyContentByPolicyId**
> MediationDTO updateAPIMediationPolicyContentByPolicyId(apiId, mediationPolicyId, type, ifMatch, file, inlineContent)

Update an API Specific Mediation Policy

This operation can be used to update an existing mediation policy of an API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiMediationPolicyApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiMediationPolicyApi apiInstance = new ApiMediationPolicyApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String mediationPolicyId = "mediationPolicyId_example"; // String | Mediation policy Id 
    String type = "type_example"; // String | Type of the mediation sequence(in/out/fault)
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    File file = new File("/path/to/file"); // File | Mediation Policy to upload
    String inlineContent = "inlineContent_example"; // String | Inline content of the Mediation Policy
    try {
      MediationDTO result = apiInstance.updateAPIMediationPolicyContentByPolicyId(apiId, mediationPolicyId, type, ifMatch, file, inlineContent);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiMediationPolicyApi#updateAPIMediationPolicyContentByPolicyId");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **mediationPolicyId** | **String**| Mediation policy Id  |
 **type** | **String**| Type of the mediation sequence(in/out/fault) |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]
 **file** | **File**| Mediation Policy to upload | [optional]
 **inlineContent** | **String**| Inline content of the Mediation Policy | [optional]

### Return type

[**MediationDTO**](MediationDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with updated API object  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

