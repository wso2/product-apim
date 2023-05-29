# ClientCertificatesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addAPIClientCertificate**](ClientCertificatesApi.md#addAPIClientCertificate) | **POST** /apis/{apiId}/client-certificates | Upload a New Certificate
[**deleteAPIClientCertificateByAlias**](ClientCertificatesApi.md#deleteAPIClientCertificateByAlias) | **DELETE** /apis/{apiId}/client-certificates/{alias} | Delete a Certificate
[**getAPIClientCertificateByAlias**](ClientCertificatesApi.md#getAPIClientCertificateByAlias) | **GET** /apis/{apiId}/client-certificates/{alias} | Get the Certificate Information
[**getAPIClientCertificateContentByAlias**](ClientCertificatesApi.md#getAPIClientCertificateContentByAlias) | **GET** /apis/{apiId}/client-certificates/{alias}/content | Download a Certificate
[**getAPIClientCertificates**](ClientCertificatesApi.md#getAPIClientCertificates) | **GET** /apis/{apiId}/client-certificates | Retrieve/ Search Uploaded Client Certificates
[**updateAPIClientCertificateByAlias**](ClientCertificatesApi.md#updateAPIClientCertificateByAlias) | **PUT** /apis/{apiId}/client-certificates/{alias} | Update a Certificate


<a name="addAPIClientCertificate"></a>
# **addAPIClientCertificate**
> ClientCertMetadataDTO addAPIClientCertificate(apiId, certificate, alias, tier)

Upload a New Certificate

This operation can be used to upload a new certificate for an endpoint. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ClientCertificatesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ClientCertificatesApi apiInstance = new ClientCertificatesApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    File certificate = new File("/path/to/file"); // File | The certificate that needs to be uploaded.
    String alias = "alias_example"; // String | Alias for the certificate
    String tier = "tier_example"; // String | api tier to which the certificate should be applied.
    try {
      ClientCertMetadataDTO result = apiInstance.addAPIClientCertificate(apiId, certificate, alias, tier);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ClientCertificatesApi#addAPIClientCertificate");
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
 **certificate** | **File**| The certificate that needs to be uploaded. |
 **alias** | **String**| Alias for the certificate |
 **tier** | **String**| api tier to which the certificate should be applied. |

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

<a name="deleteAPIClientCertificateByAlias"></a>
# **deleteAPIClientCertificateByAlias**
> deleteAPIClientCertificateByAlias(alias, apiId)

Delete a Certificate

This operation can be used to delete an uploaded certificate. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ClientCertificatesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ClientCertificatesApi apiInstance = new ClientCertificatesApi(defaultClient);
    String alias = "alias_example"; // String | The alias of the certificate that should be deleted. 
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    try {
      apiInstance.deleteAPIClientCertificateByAlias(alias, apiId);
    } catch (ApiException e) {
      System.err.println("Exception when calling ClientCertificatesApi#deleteAPIClientCertificateByAlias");
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
 **alias** | **String**| The alias of the certificate that should be deleted.  |
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |

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
**200** | OK. The Certificate deleted successfully.  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getAPIClientCertificateByAlias"></a>
# **getAPIClientCertificateByAlias**
> CertificateInfoDTO getAPIClientCertificateByAlias(alias, apiId)

Get the Certificate Information

This operation can be used to get the information about a certificate. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ClientCertificatesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ClientCertificatesApi apiInstance = new ClientCertificatesApi(defaultClient);
    String alias = "alias_example"; // String | 
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    try {
      CertificateInfoDTO result = apiInstance.getAPIClientCertificateByAlias(alias, apiId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ClientCertificatesApi#getAPIClientCertificateByAlias");
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
 **alias** | **String**|  |
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |

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

<a name="getAPIClientCertificateContentByAlias"></a>
# **getAPIClientCertificateContentByAlias**
> getAPIClientCertificateContentByAlias(apiId, alias)

Download a Certificate

This operation can be used to download a certificate which matches the given alias. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ClientCertificatesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ClientCertificatesApi apiInstance = new ClientCertificatesApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String alias = "alias_example"; // String | 
    try {
      apiInstance.getAPIClientCertificateContentByAlias(apiId, alias);
    } catch (ApiException e) {
      System.err.println("Exception when calling ClientCertificatesApi#getAPIClientCertificateContentByAlias");
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

<a name="getAPIClientCertificates"></a>
# **getAPIClientCertificates**
> ClientCertificatesDTO getAPIClientCertificates(apiId, limit, offset, alias)

Retrieve/ Search Uploaded Client Certificates

This operation can be used to retrieve and search the uploaded client certificates. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ClientCertificatesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ClientCertificatesApi apiInstance = new ClientCertificatesApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String alias = "alias_example"; // String | Alias for the client certificate
    try {
      ClientCertificatesDTO result = apiInstance.getAPIClientCertificates(apiId, limit, offset, alias);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ClientCertificatesApi#getAPIClientCertificates");
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
 **alias** | **String**| Alias for the client certificate | [optional]

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
**200** | OK. Successful response with the list of matching certificate information in the body.  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**500** | Internal Server Error. |  -  |

<a name="updateAPIClientCertificateByAlias"></a>
# **updateAPIClientCertificateByAlias**
> ClientCertMetadataDTO updateAPIClientCertificateByAlias(alias, apiId, certificate, tier)

Update a Certificate

This operation can be used to update an uploaded certificate. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ClientCertificatesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ClientCertificatesApi apiInstance = new ClientCertificatesApi(defaultClient);
    String alias = "alias_example"; // String | Alias for the certificate
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    File certificate = new File("/path/to/file"); // File | The certificate that needs to be uploaded.
    String tier = "tier_example"; // String | The tier of the certificate
    try {
      ClientCertMetadataDTO result = apiInstance.updateAPIClientCertificateByAlias(alias, apiId, certificate, tier);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ClientCertificatesApi#updateAPIClientCertificateByAlias");
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
 **alias** | **String**| Alias for the certificate |
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **certificate** | **File**| The certificate that needs to be uploaded. | [optional]
 **tier** | **String**| The tier of the certificate | [optional]

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
**200** | OK. The Certificate updated successfully.  |  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

