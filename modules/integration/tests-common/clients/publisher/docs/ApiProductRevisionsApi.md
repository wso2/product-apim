# ApiProductRevisionsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v2*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createAPIProductRevision**](ApiProductRevisionsApi.md#createAPIProductRevision) | **POST** /api-products/{apiProductId}/revisions | Create a new API Product revision
[**deleteAPIProductRevision**](ApiProductRevisionsApi.md#deleteAPIProductRevision) | **DELETE** /api-products/{apiProductId}/revisions/{revisionId} | Delete a revision of an API Product
[**deployAPIProductRevision**](ApiProductRevisionsApi.md#deployAPIProductRevision) | **POST** /api-products/{apiProductId}/deploy-revision | Deploy a revision
[**getAPIProductRevision**](ApiProductRevisionsApi.md#getAPIProductRevision) | **GET** /api-products/{apiProductId}/revisions/{revisionId} | Retrieve a revision of an API Product
[**getAPIProductRevisionDeployments**](ApiProductRevisionsApi.md#getAPIProductRevisionDeployments) | **GET** /api-products/{apiProductId}/deploy-revision | List available deployed revision deployment details of an API Product
[**getAPIProductRevisions**](ApiProductRevisionsApi.md#getAPIProductRevisions) | **GET** /api-products/{apiProductId}/revisions | List available revisions of an API Product
[**restoreAPIProductRevision**](ApiProductRevisionsApi.md#restoreAPIProductRevision) | **POST** /api-products/{apiProductId}/restore-revision | Restore a revision
[**undeployAPIProductRevision**](ApiProductRevisionsApi.md#undeployAPIProductRevision) | **POST** /api-products/{apiProductId}/undeploy-revision | Un-Deploy a revision


<a name="createAPIProductRevision"></a>
# **createAPIProductRevision**
> APIRevisionDTO createAPIProductRevision(apiProductId, apIRevisionDTO)

Create a new API Product revision

Create a new API Product revision 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductRevisionsApi apiInstance = new ApiProductRevisionsApi(defaultClient);
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
    APIRevisionDTO apIRevisionDTO = new APIRevisionDTO(); // APIRevisionDTO | API Product object that needs to be added
    try {
      APIRevisionDTO result = apiInstance.createAPIProductRevision(apiProductId, apIRevisionDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductRevisionsApi#createAPIProductRevision");
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
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **apIRevisionDTO** | [**APIRevisionDTO**](APIRevisionDTO.md)| API Product object that needs to be added | [optional]

### Return type

[**APIRevisionDTO**](APIRevisionDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created APIRevision object as the entity in the body.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="deleteAPIProductRevision"></a>
# **deleteAPIProductRevision**
> APIRevisionListDTO deleteAPIProductRevision(apiProductId, revisionId)

Delete a revision of an API Product

Delete a revision of an API Product 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductRevisionsApi apiInstance = new ApiProductRevisionsApi(defaultClient);
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
    String revisionId = "revisionId_example"; // String | Revision ID of an API 
    try {
      APIRevisionListDTO result = apiInstance.deleteAPIProductRevision(apiProductId, revisionId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductRevisionsApi#deleteAPIProductRevision");
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
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **revisionId** | **String**| Revision ID of an API  |

### Return type

[**APIRevisionListDTO**](APIRevisionListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of remaining API revisions are returned.  |  -  |
**204** | No Content. Successfully deleted the revision  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="deployAPIProductRevision"></a>
# **deployAPIProductRevision**
> deployAPIProductRevision(apiProductId, revisionId, apIRevisionDeploymentDTO)

Deploy a revision

Deploy a revision 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductRevisionsApi apiInstance = new ApiProductRevisionsApi(defaultClient);
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
    String revisionId = "revisionId_example"; // String | Revision ID of an API 
    List<APIRevisionDeploymentDTO> apIRevisionDeploymentDTO = Arrays.asList(); // List<APIRevisionDeploymentDTO> | Deployment object that needs to be added
    try {
      apiInstance.deployAPIProductRevision(apiProductId, revisionId, apIRevisionDeploymentDTO);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductRevisionsApi#deployAPIProductRevision");
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
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **revisionId** | **String**| Revision ID of an API  |
 **apIRevisionDeploymentDTO** | [**List&lt;APIRevisionDeploymentDTO&gt;**](APIRevisionDeploymentDTO.md)| Deployment object that needs to be added | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK.  |  -  |
**201** | Created. Successful response with the newly deployed APIRevisionDeployment List object as the entity in the body.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="getAPIProductRevision"></a>
# **getAPIProductRevision**
> APIRevisionDTO getAPIProductRevision(apiProductId, revisionId)

Retrieve a revision of an API Product

Retrieve a revision of an API Product 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductRevisionsApi apiInstance = new ApiProductRevisionsApi(defaultClient);
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
    String revisionId = "revisionId_example"; // String | Revision ID of an API 
    try {
      APIRevisionDTO result = apiInstance.getAPIProductRevision(apiProductId, revisionId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductRevisionsApi#getAPIProductRevision");
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
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **revisionId** | **String**| Revision ID of an API  |

### Return type

[**APIRevisionDTO**](APIRevisionDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. An API revision is returned.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="getAPIProductRevisionDeployments"></a>
# **getAPIProductRevisionDeployments**
> APIRevisionDeploymentListDTO getAPIProductRevisionDeployments(apiProductId)

List available deployed revision deployment details of an API Product

List available deployed revision deployment details of an API Product 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductRevisionsApi apiInstance = new ApiProductRevisionsApi(defaultClient);
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
    try {
      APIRevisionDeploymentListDTO result = apiInstance.getAPIProductRevisionDeployments(apiProductId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductRevisionsApi#getAPIProductRevisionDeployments");
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
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |

### Return type

[**APIRevisionDeploymentListDTO**](APIRevisionDeploymentListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of deployed revision deployment details are returned.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="getAPIProductRevisions"></a>
# **getAPIProductRevisions**
> APIRevisionListDTO getAPIProductRevisions(apiProductId, query)

List available revisions of an API Product

List available revisions of an API Product 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductRevisionsApi apiInstance = new ApiProductRevisionsApi(defaultClient);
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
    String query = "query_example"; // String | 
    try {
      APIRevisionListDTO result = apiInstance.getAPIProductRevisions(apiProductId, query);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductRevisionsApi#getAPIProductRevisions");
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
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **query** | **String**|  | [optional]

### Return type

[**APIRevisionListDTO**](APIRevisionListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of API Product revisions are returned.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="restoreAPIProductRevision"></a>
# **restoreAPIProductRevision**
> APIProductDTO restoreAPIProductRevision(apiProductId, revisionId)

Restore a revision

Restore a revision to the working copy of the API Product 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductRevisionsApi apiInstance = new ApiProductRevisionsApi(defaultClient);
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
    String revisionId = "revisionId_example"; // String | Revision ID of an API 
    try {
      APIProductDTO result = apiInstance.restoreAPIProductRevision(apiProductId, revisionId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductRevisionsApi#restoreAPIProductRevision");
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
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **revisionId** | **String**| Revision ID of an API  |

### Return type

[**APIProductDTO**](APIProductDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Restored. Successful response with the newly restored API Product object as the entity in the body.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="undeployAPIProductRevision"></a>
# **undeployAPIProductRevision**
> undeployAPIProductRevision(apiProductId, revisionId, apIRevisionDeploymentDTO)

Un-Deploy a revision

Un-Deploy a revision 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiProductRevisionsApi apiInstance = new ApiProductRevisionsApi(defaultClient);
    String apiProductId = "apiProductId_example"; // String | **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended. 
    String revisionId = "revisionId_example"; // String | Revision ID of an API 
    List<APIRevisionDeploymentDTO> apIRevisionDeploymentDTO = Arrays.asList(); // List<APIRevisionDeploymentDTO> | Deployment object that needs to be added
    try {
      apiInstance.undeployAPIProductRevision(apiProductId, revisionId, apIRevisionDeploymentDTO);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiProductRevisionsApi#undeployAPIProductRevision");
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
 **apiProductId** | **String**| **API Product ID** consisting of the **UUID** of the API Product. Using the **UUID** in the API call is recommended.  |
 **revisionId** | **String**| Revision ID of an API  |
 **apIRevisionDeploymentDTO** | [**List&lt;APIRevisionDeploymentDTO&gt;**](APIRevisionDeploymentDTO.md)| Deployment object that needs to be added | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK.  |  -  |
**201** | Created. Successful response with the newly undeployed APIRevisionDeploymentList object as the entity in the body.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

