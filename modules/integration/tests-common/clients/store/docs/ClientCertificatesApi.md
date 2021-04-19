# ClientCertificatesApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v2*

Method | HTTP request | Description
------------- | ------------- | -------------
[**applicationsApplicationIdClientCertificatesGet**](ClientCertificatesApi.md#applicationsApplicationIdClientCertificatesGet) | **GET** /applications/{applicationId}/client-certificates | Retrive Uploaded Client Certificates
[**applicationsApplicationIdClientCertificatesPost**](ClientCertificatesApi.md#applicationsApplicationIdClientCertificatesPost) | **POST** /applications/{applicationId}/client-certificates | Upload a New Certificate
[**applicationsApplicationIdClientCertificatesUUIDContentGet**](ClientCertificatesApi.md#applicationsApplicationIdClientCertificatesUUIDContentGet) | **GET** /applications/{applicationId}/client-certificates/{UUID}/content | Download a Certificate
[**applicationsApplicationIdClientCertificatesUUIDDelete**](ClientCertificatesApi.md#applicationsApplicationIdClientCertificatesUUIDDelete) | **DELETE** /applications/{applicationId}/client-certificates/{UUID} | Delete a Certificates
[**applicationsApplicationIdClientCertificatesUUIDGet**](ClientCertificatesApi.md#applicationsApplicationIdClientCertificatesUUIDGet) | **GET** /applications/{applicationId}/client-certificates/{UUID} | Get the Certificate Information


<a name="applicationsApplicationIdClientCertificatesGet"></a>
# **applicationsApplicationIdClientCertificatesGet**
> ClientCertificatesDTO applicationsApplicationIdClientCertificatesGet(applicationId, limit, offset)

Retrive Uploaded Client Certificates

This operation can be used to retrieve and search the uploaded client certificates. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ClientCertificatesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ClientCertificatesApi apiInstance = new ClientCertificatesApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    try {
      ClientCertificatesDTO result = apiInstance.applicationsApplicationIdClientCertificatesGet(applicationId, limit, offset);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ClientCertificatesApi#applicationsApplicationIdClientCertificatesGet");
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
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]

### Return type

[**ClientCertificatesDTO**](ClientCertificatesDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with the list of matching certificate information in the body. |  * Content-Type - The content type of the body <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**500** | Internal Server Error. |  -  |

<a name="applicationsApplicationIdClientCertificatesPost"></a>
# **applicationsApplicationIdClientCertificatesPost**
> ClientCertMetadataDTO applicationsApplicationIdClientCertificatesPost(applicationId, certificate, name, type)

Upload a New Certificate

This operation can be used to upload a new certificate for an endpoint. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ClientCertificatesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ClientCertificatesApi apiInstance = new ClientCertificatesApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    File certificate = new File("/path/to/file"); // File | The certificate that needs to be uploaded.
    String name = "name_example"; // String | Display name for the certificate
    String type = "type_example"; // String | Type of the Gateway Environment
    try {
      ClientCertMetadataDTO result = apiInstance.applicationsApplicationIdClientCertificatesPost(applicationId, certificate, name, type);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ClientCertificatesApi#applicationsApplicationIdClientCertificatesPost");
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
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **certificate** | **File**| The certificate that needs to be uploaded. |
 **name** | **String**| Display name for the certificate |
 **type** | **String**| Type of the Gateway Environment |

### Return type

[**ClientCertMetadataDTO**](ClientCertMetadataDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. The Certificate added successfully.  |  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**500** | Internal Server Error. |  -  |

<a name="applicationsApplicationIdClientCertificatesUUIDContentGet"></a>
# **applicationsApplicationIdClientCertificatesUUIDContentGet**
> applicationsApplicationIdClientCertificatesUUIDContentGet(applicationId, alias)

Download a Certificate

This operation can be used to download a certificate which matches the given alias. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ClientCertificatesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ClientCertificatesApi apiInstance = new ClientCertificatesApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String alias = "alias_example"; // String | 
    try {
      apiInstance.applicationsApplicationIdClientCertificatesUUIDContentGet(applicationId, alias);
    } catch (ApiException e) {
      System.err.println("Exception when calling ClientCertificatesApi#applicationsApplicationIdClientCertificatesUUIDContentGet");
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
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **alias** | **String**|  |

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
**200** | OK.  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="applicationsApplicationIdClientCertificatesUUIDDelete"></a>
# **applicationsApplicationIdClientCertificatesUUIDDelete**
> applicationsApplicationIdClientCertificatesUUIDDelete(UUID, applicationId)

Delete a Certificates

This operation can be used to delete an uploaded certificate.

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ClientCertificatesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v2");

    ClientCertificatesApi apiInstance = new ClientCertificatesApi(defaultClient);
    String UUID = "UUID_example"; // String | The alias of the certificate that should be deleted.
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    try {
      apiInstance.applicationsApplicationIdClientCertificatesUUIDDelete(UUID, applicationId);
    } catch (ApiException e) {
      System.err.println("Exception when calling ClientCertificatesApi#applicationsApplicationIdClientCertificatesUUIDDelete");
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
 **UUID** | **String**| The alias of the certificate that should be deleted. |
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. The Certificate deleted successfully. |  * Content-Type - The content type of the body. <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="applicationsApplicationIdClientCertificatesUUIDGet"></a>
# **applicationsApplicationIdClientCertificatesUUIDGet**
> CertificateInfoDTO applicationsApplicationIdClientCertificatesUUIDGet(UUID, applicationId)

Get the Certificate Information

This operation can be used to get the information about a certificate. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ClientCertificatesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ClientCertificatesApi apiInstance = new ClientCertificatesApi(defaultClient);
    String UUID = "UUID_example"; // String | 
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    try {
      CertificateInfoDTO result = apiInstance.applicationsApplicationIdClientCertificatesUUIDGet(UUID, applicationId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ClientCertificatesApi#applicationsApplicationIdClientCertificatesUUIDGet");
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
 **UUID** | **String**|  |
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |

### Return type

[**CertificateInfoDTO**](CertificateInfoDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK.  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

