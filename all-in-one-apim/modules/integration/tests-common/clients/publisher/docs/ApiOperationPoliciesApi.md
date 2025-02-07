# ApiOperationPoliciesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addAPISpecificOperationPolicy**](ApiOperationPoliciesApi.md#addAPISpecificOperationPolicy) | **POST** /apis/{apiId}/operation-policies | Add an API specific operation policy
[**deleteAPISpecificOperationPolicyByPolicyId**](ApiOperationPoliciesApi.md#deleteAPISpecificOperationPolicyByPolicyId) | **DELETE** /apis/{apiId}/operation-policies/{operationPolicyId} | Delete an API Specific Operation Policy
[**getAPISpecificOperationPolicyContentByPolicyId**](ApiOperationPoliciesApi.md#getAPISpecificOperationPolicyContentByPolicyId) | **GET** /apis/{apiId}/operation-policies/{operationPolicyId}/content | Download an API Specific Operation Policy
[**getAllAPISpecificOperationPolicies**](ApiOperationPoliciesApi.md#getAllAPISpecificOperationPolicies) | **GET** /apis/{apiId}/operation-policies | Get all API specific operation policies for an API 
[**getOperationPolicyForAPIByPolicyId**](ApiOperationPoliciesApi.md#getOperationPolicyForAPIByPolicyId) | **GET** /apis/{apiId}/operation-policies/{operationPolicyId} | Get policy details of an API specific policy


<a name="addAPISpecificOperationPolicy"></a>
# **addAPISpecificOperationPolicy**
> OperationPolicyDataDTO addAPISpecificOperationPolicy(apiId, policySpecFile, synapsePolicyDefinitionFile, ccPolicyDefinitionFile)

Add an API specific operation policy

This operation can be used to add an API specifc operation policy. This policy cannot be used in other APIs. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiOperationPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiOperationPoliciesApi apiInstance = new ApiOperationPoliciesApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    File policySpecFile = new File("/path/to/file"); // File | Policy specification to upload
    File synapsePolicyDefinitionFile = new File("/path/to/file"); // File | Operation policy definition of synapse gateway to upload
    File ccPolicyDefinitionFile = new File("/path/to/file"); // File | Operation policy definition of choreo connect to upload
    try {
      OperationPolicyDataDTO result = apiInstance.addAPISpecificOperationPolicy(apiId, policySpecFile, synapsePolicyDefinitionFile, ccPolicyDefinitionFile);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiOperationPoliciesApi#addAPISpecificOperationPolicy");
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
 **policySpecFile** | **File**| Policy specification to upload | [optional]
 **synapsePolicyDefinitionFile** | **File**| Operation policy definition of synapse gateway to upload | [optional]
 **ccPolicyDefinitionFile** | **File**| Operation policy definition of choreo connect to upload | [optional]

### Return type

[**OperationPolicyDataDTO**](OperationPolicyDataDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | OK. Operation policy uploaded  |  * Location - The URL of the uploaded operation policy of the API.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="deleteAPISpecificOperationPolicyByPolicyId"></a>
# **deleteAPISpecificOperationPolicyByPolicyId**
> deleteAPISpecificOperationPolicyByPolicyId(apiId, operationPolicyId)

Delete an API Specific Operation Policy

This operation can be used to delete an existing API specific opreation policy by providing the Id of the API and the Id of the policy. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiOperationPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiOperationPoliciesApi apiInstance = new ApiOperationPoliciesApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String operationPolicyId = "operationPolicyId_example"; // String | Operation policy Id 
    try {
      apiInstance.deleteAPISpecificOperationPolicyByPolicyId(apiId, operationPolicyId);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiOperationPoliciesApi#deleteAPISpecificOperationPolicyByPolicyId");
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
 **operationPolicyId** | **String**| Operation policy Id  |

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
**500** | Internal Server Error. |  -  |

<a name="getAPISpecificOperationPolicyContentByPolicyId"></a>
# **getAPISpecificOperationPolicyContentByPolicyId**
> File getAPISpecificOperationPolicyContentByPolicyId(apiId, operationPolicyId)

Download an API Specific Operation Policy

This operation can be used to download a particular API specific operation policy. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiOperationPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiOperationPoliciesApi apiInstance = new ApiOperationPoliciesApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String operationPolicyId = "operationPolicyId_example"; // String | Operation policy Id 
    try {
      File result = apiInstance.getAPISpecificOperationPolicyContentByPolicyId(apiId, operationPolicyId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiOperationPoliciesApi#getAPISpecificOperationPolicyContentByPolicyId");
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
 **operationPolicyId** | **String**| Operation policy Id  |

### Return type

[**File**](File.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/zip, application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Operation policy returned.  |  * Content-Type - The content type of the body.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getAllAPISpecificOperationPolicies"></a>
# **getAllAPISpecificOperationPolicies**
> OperationPolicyDataListDTO getAllAPISpecificOperationPolicies(apiId, limit, offset, query)

Get all API specific operation policies for an API 

This operation provides you a list of all applicabale operation policies for an API 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiOperationPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiOperationPoliciesApi apiInstance = new ApiOperationPoliciesApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String query = "query_example"; // String | -Not supported yet-
    try {
      OperationPolicyDataListDTO result = apiInstance.getAllAPISpecificOperationPolicies(apiId, limit, offset, query);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiOperationPoliciesApi#getAllAPISpecificOperationPolicies");
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
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **query** | **String**| -Not supported yet- | [optional]

### Return type

[**OperationPolicyDataListDTO**](OperationPolicyDataListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of qualifying policies is returned.  |  * Content-Type - The content type of the body. <br>  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getOperationPolicyForAPIByPolicyId"></a>
# **getOperationPolicyForAPIByPolicyId**
> OperationPolicyDataDTO getOperationPolicyForAPIByPolicyId(apiId, operationPolicyId)

Get policy details of an API specific policy

This operation can be used to retrieve a particular API specific operation policy. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiOperationPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiOperationPoliciesApi apiInstance = new ApiOperationPoliciesApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String operationPolicyId = "operationPolicyId_example"; // String | Operation policy Id 
    try {
      OperationPolicyDataDTO result = apiInstance.getOperationPolicyForAPIByPolicyId(apiId, operationPolicyId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiOperationPoliciesApi#getOperationPolicyForAPIByPolicyId");
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
 **operationPolicyId** | **String**| Operation policy Id  |

### Return type

[**OperationPolicyDataDTO**](OperationPolicyDataDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Operation policy returned.  |  * Content-Type - The content type of the body.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |
**500** | Internal Server Error. |  -  |

