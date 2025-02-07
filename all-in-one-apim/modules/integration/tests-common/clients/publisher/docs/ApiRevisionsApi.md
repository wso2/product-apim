# ApiRevisionsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createAPIRevision**](ApiRevisionsApi.md#createAPIRevision) | **POST** /apis/{apiId}/revisions | Create API Revision
[**deleteAPIRevision**](ApiRevisionsApi.md#deleteAPIRevision) | **DELETE** /apis/{apiId}/revisions/{revisionId} | Delete Revision
[**deleteAPIRevisionDeploymentPendingTask**](ApiRevisionsApi.md#deleteAPIRevisionDeploymentPendingTask) | **DELETE** /apis/{apiId}/cancel-revision-workflow/{revisionId}/{envName} | Delete Pending Revision Deployment Workflow Tasks
[**deployAPIRevision**](ApiRevisionsApi.md#deployAPIRevision) | **POST** /apis/{apiId}/deploy-revision | Deploy Revision
[**getAPIRevision**](ApiRevisionsApi.md#getAPIRevision) | **GET** /apis/{apiId}/revisions/{revisionId} | Retrieve Revision
[**getAPIRevisionDeployments**](ApiRevisionsApi.md#getAPIRevisionDeployments) | **GET** /apis/{apiId}/deployments | List Deployments
[**getAPIRevisions**](ApiRevisionsApi.md#getAPIRevisions) | **GET** /apis/{apiId}/revisions | List Revisions
[**restoreAPIRevision**](ApiRevisionsApi.md#restoreAPIRevision) | **POST** /apis/{apiId}/restore-revision | Restore API Revision
[**undeployAPIRevision**](ApiRevisionsApi.md#undeployAPIRevision) | **POST** /apis/{apiId}/undeploy-revision | UnDeploy Revision
[**updateAPIDeployment**](ApiRevisionsApi.md#updateAPIDeployment) | **PUT** /apis/{apiId}/deployments/{deploymentId} | Update Deployment


<a name="createAPIRevision"></a>
# **createAPIRevision**
> APIRevisionDTO createAPIRevision(apiId, apIRevisionDTO)

Create API Revision

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
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

Delete Revision

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
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

<a name="deleteAPIRevisionDeploymentPendingTask"></a>
# **deleteAPIRevisionDeploymentPendingTask**
> deleteAPIRevisionDeploymentPendingTask(apiId, revisionId, envName)

Delete Pending Revision Deployment Workflow Tasks

This operation can be used to remove pending revision deployment requests that are in pending state 

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiRevisionsApi apiInstance = new ApiRevisionsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String revisionId = "revisionId_example"; // String | Revision ID of an API 
    String envName = "envName_example"; // String | Environment name of an Revision 
    try {
      apiInstance.deleteAPIRevisionDeploymentPendingTask(apiId, revisionId, envName);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiRevisionsApi#deleteAPIRevisionDeploymentPendingTask");
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
 **envName** | **String**| Environment name of an Revision  |

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
**200** | OK. Revision deployment pending task removed successfully.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="deployAPIRevision"></a>
# **deployAPIRevision**
> List&lt;APIRevisionDeploymentDTO&gt; deployAPIRevision(apiId, revisionId, apIRevisionDeploymentDTO)

Deploy Revision

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiRevisionsApi apiInstance = new ApiRevisionsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String revisionId = "revisionId_example"; // String | Revision ID of an API 
    List<APIRevisionDeploymentDTO> apIRevisionDeploymentDTO = Arrays.asList(); // List<APIRevisionDeploymentDTO> | Deployment object that needs to be added
    try {
      List<APIRevisionDeploymentDTO> result = apiInstance.deployAPIRevision(apiId, revisionId, apIRevisionDeploymentDTO);
      System.out.println(result);
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
 **revisionId** | **String**| Revision ID of an API  | [optional]
 **apIRevisionDeploymentDTO** | [**List&lt;APIRevisionDeploymentDTO&gt;**](APIRevisionDeploymentDTO.md)| Deployment object that needs to be added | [optional]

### Return type

[**List&lt;APIRevisionDeploymentDTO&gt;**](APIRevisionDeploymentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Created. Successful response with the newly deployed APIRevisionDeployment List object as the entity in the body.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="getAPIRevision"></a>
# **getAPIRevision**
> APIRevisionDTO getAPIRevision(apiId, revisionId)

Retrieve Revision

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
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

List Deployments

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
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

List Revisions

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
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

Restore API Revision

Restore a revision to the current API of the API 

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
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
 **revisionId** | **String**| Revision ID of an API  | [optional]

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
> undeployAPIRevision(apiId, revisionId, revisionNumber, allEnvironments, apIRevisionDeploymentDTO)

UnDeploy Revision

UnDeploy a revision 

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiRevisionsApi apiInstance = new ApiRevisionsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String revisionId = "revisionId_example"; // String | Revision ID of an API 
    String revisionNumber = "revisionNumber_example"; // String | Revision Number of an API 
    Boolean allEnvironments = false; // Boolean | 
    List<APIRevisionDeploymentDTO> apIRevisionDeploymentDTO = Arrays.asList(); // List<APIRevisionDeploymentDTO> | Deployment object that needs to be added
    try {
      apiInstance.undeployAPIRevision(apiId, revisionId, revisionNumber, allEnvironments, apIRevisionDeploymentDTO);
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
 **revisionId** | **String**| Revision ID of an API  | [optional]
 **revisionNumber** | **String**| Revision Number of an API  | [optional]
 **allEnvironments** | **Boolean**|  | [optional] [default to false]
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
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="updateAPIDeployment"></a>
# **updateAPIDeployment**
> APIRevisionDeploymentDTO updateAPIDeployment(apiId, deploymentId, apIRevisionDeploymentDTO)

Update Deployment

Update deployment devportal visibility 

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiRevisionsApi apiInstance = new ApiRevisionsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String deploymentId = "deploymentId_example"; // String | Base64 URL encoded value of the name of an environment 
    APIRevisionDeploymentDTO apIRevisionDeploymentDTO = new APIRevisionDeploymentDTO(); // APIRevisionDeploymentDTO | Deployment object that needs to be updated
    try {
      APIRevisionDeploymentDTO result = apiInstance.updateAPIDeployment(apiId, deploymentId, apIRevisionDeploymentDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiRevisionsApi#updateAPIDeployment");
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
 **deploymentId** | **String**| Base64 URL encoded value of the name of an environment  |
 **apIRevisionDeploymentDTO** | [**APIRevisionDeploymentDTO**](APIRevisionDeploymentDTO.md)| Deployment object that needs to be updated | [optional]

### Return type

[**APIRevisionDeploymentDTO**](APIRevisionDeploymentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Created. Successful response with the newly updated APIRevisionDeployment List object as the entity in the body.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

