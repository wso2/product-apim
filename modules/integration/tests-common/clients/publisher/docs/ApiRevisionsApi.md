# ApiRevisionsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v2*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createAPIRevision**](ApiRevisionsApi.md#createAPIRevision) | **POST** /apis/{apiId}/revisions | Create a new API revision
[**deleteAPIRevision**](ApiRevisionsApi.md#deleteAPIRevision) | **DELETE** /apis/{apiId}/revisions/{revisionId} | Delete a revision of an API
[**deployAPIRevision**](ApiRevisionsApi.md#deployAPIRevision) | **POST** /apis/{apiId}/deploy-revision | Deploy a revision
[**getAPIRevision**](ApiRevisionsApi.md#getAPIRevision) | **GET** /apis/{apiId}/revisions/{revisionId} | Retrieve a revision of an API
[**getAPIRevisionDeployments**](ApiRevisionsApi.md#getAPIRevisionDeployments) | **GET** /apis/{apiId}/deploy-revision | List available deployed revision deployment details of an API
[**getAPIRevisions**](ApiRevisionsApi.md#getAPIRevisions) | **GET** /apis/{apiId}/revisions | List available revisions of an API
[**restoreAPIRevision**](ApiRevisionsApi.md#restoreAPIRevision) | **POST** /apis/{apiId}/restore-revision | Restore a revision
[**undeployAPIRevision**](ApiRevisionsApi.md#undeployAPIRevision) | **POST** /apis/{apiId}/undeploy-revision | Un-Deploy a revision


<a name="createAPIRevision"></a>
# **createAPIRevision**
> APIRevisionDTO createAPIRevision(apiId, apIRevisionDTO)

Create a new API revision

Create a new API revision 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiRevisionsApi apiInstance = new ApiRevisionsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    APIRevisionDTO apIRevisionDTO = new APIRevisionDTO(); // APIRevisionDTO | API object that needs to be added
    try {
      APIRevisionDTO result = apiInstance.createAPIRevision(apiId, apIRevisionDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiRevisionsApi#createAPIRevision");
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
 **apIRevisionDTO** | [**APIRevisionDTO**](APIRevisionDTO.md)| API object that needs to be added | [optional]

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

<a name="deleteAPIRevision"></a>
# **deleteAPIRevision**
> APIRevisionListDTO deleteAPIRevision(apiId, revisionId)

Delete a revision of an API

Delete a revision of an API 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiRevisionsApi apiInstance = new ApiRevisionsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String revisionId = "revisionId_example"; // String | Revision ID of an API 
    try {
      APIRevisionListDTO result = apiInstance.deleteAPIRevision(apiId, revisionId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiRevisionsApi#deleteAPIRevision");
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

<a name="deployAPIRevision"></a>
# **deployAPIRevision**
> deployAPIRevision(apiId, revisionId, apIRevisionDeploymentDTO)

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
import org.wso2.am.integration.clients.publisher.api.v1.ApiRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiRevisionsApi apiInstance = new ApiRevisionsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String revisionId = "revisionId_example"; // String | Revision ID of an API 
    List<APIRevisionDeploymentDTO> apIRevisionDeploymentDTO = Arrays.asList(); // List<APIRevisionDeploymentDTO> | Deployment object that needs to be added
    try {
      apiInstance.deployAPIRevision(apiId, revisionId, apIRevisionDeploymentDTO);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiRevisionsApi#deployAPIRevision");
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

<a name="getAPIRevision"></a>
# **getAPIRevision**
> APIRevisionDTO getAPIRevision(apiId, revisionId)

Retrieve a revision of an API

Retrieve a revision of an API 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiRevisionsApi apiInstance = new ApiRevisionsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String revisionId = "revisionId_example"; // String | Revision ID of an API 
    try {
      APIRevisionDTO result = apiInstance.getAPIRevision(apiId, revisionId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiRevisionsApi#getAPIRevision");
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

<a name="getAPIRevisionDeployments"></a>
# **getAPIRevisionDeployments**
> APIRevisionDeploymentListDTO getAPIRevisionDeployments(apiId)

List available deployed revision deployment details of an API

List available deployed revision deployment details of an API 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiRevisionsApi apiInstance = new ApiRevisionsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    try {
      APIRevisionDeploymentListDTO result = apiInstance.getAPIRevisionDeployments(apiId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiRevisionsApi#getAPIRevisionDeployments");
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

<a name="getAPIRevisions"></a>
# **getAPIRevisions**
> APIRevisionListDTO getAPIRevisions(apiId, query)

List available revisions of an API

List available revisions of an API 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiRevisionsApi apiInstance = new ApiRevisionsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String query = "query_example"; // String | 
    try {
      APIRevisionListDTO result = apiInstance.getAPIRevisions(apiId, query);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiRevisionsApi#getAPIRevisions");
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
**200** | OK. List of API revisions are returned.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="restoreAPIRevision"></a>
# **restoreAPIRevision**
> APIDTO restoreAPIRevision(apiId, revisionId)

Restore a revision

Restore a revision to the working copy of the API 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiRevisionsApi apiInstance = new ApiRevisionsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String revisionId = "revisionId_example"; // String | Revision ID of an API 
    try {
      APIDTO result = apiInstance.restoreAPIRevision(apiId, revisionId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiRevisionsApi#restoreAPIRevision");
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
 **revisionId** | **String**| Revision ID of an API  |

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Restored. Successful response with the newly restored API object as the entity in the body.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="undeployAPIRevision"></a>
# **undeployAPIRevision**
> undeployAPIRevision(apiId, revisionId, apIRevisionDeploymentDTO)

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
import org.wso2.am.integration.clients.publisher.api.v1.ApiRevisionsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiRevisionsApi apiInstance = new ApiRevisionsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String revisionId = "revisionId_example"; // String | Revision ID of an API 
    List<APIRevisionDeploymentDTO> apIRevisionDeploymentDTO = Arrays.asList(); // List<APIRevisionDeploymentDTO> | Deployment object that needs to be added
    try {
      apiInstance.undeployAPIRevision(apiId, revisionId, apIRevisionDeploymentDTO);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiRevisionsApi#undeployAPIRevision");
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

