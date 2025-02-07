# EndpointCertificatesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addEndpointCertificate**](EndpointCertificatesApi.md#addEndpointCertificate) | **POST** /endpoint-certificates | Upload a new Certificate.
[**deleteEndpointCertificateByAlias**](EndpointCertificatesApi.md#deleteEndpointCertificateByAlias) | **DELETE** /endpoint-certificates/{alias} | Delete a certificate.
[**getCertificateUsageByAlias**](EndpointCertificatesApi.md#getCertificateUsageByAlias) | **GET** /endpoint-certificates/{alias}/usage | Retrieve all the APIs that use a given certificate by the alias
[**getEndpointCertificateByAlias**](EndpointCertificatesApi.md#getEndpointCertificateByAlias) | **GET** /endpoint-certificates/{alias} | Get the Certificate Information
[**getEndpointCertificateContentByAlias**](EndpointCertificatesApi.md#getEndpointCertificateContentByAlias) | **GET** /endpoint-certificates/{alias}/content | Download a Certificate
[**getEndpointCertificates**](EndpointCertificatesApi.md#getEndpointCertificates) | **GET** /endpoint-certificates | Retrieve/Search Uploaded Certificates
[**updateEndpointCertificateByAlias**](EndpointCertificatesApi.md#updateEndpointCertificateByAlias) | **PUT** /endpoint-certificates/{alias} | Update a certificate.


<a name="addEndpointCertificate"></a>
# **addEndpointCertificate**
> CertMetadataDTO addEndpointCertificate(certificate, alias, endpoint)

Upload a new Certificate.

This operation can be used to upload a new certificate for an endpoint. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.EndpointCertificatesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    EndpointCertificatesApi apiInstance = new EndpointCertificatesApi(defaultClient);
    File certificate = new File("/path/to/file"); // File | The certificate that needs to be uploaded.
    String alias = "alias_example"; // String | Alias for the certificate
    String endpoint = "endpoint_example"; // String | Endpoint to which the certificate should be applied.
    try {
      CertMetadataDTO result = apiInstance.addEndpointCertificate(certificate, alias, endpoint);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling EndpointCertificatesApi#addEndpointCertificate");
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
 **certificate** | **File**| The certificate that needs to be uploaded. |
 **alias** | **String**| Alias for the certificate |
 **endpoint** | **String**| Endpoint to which the certificate should be applied. |

### Return type

[**CertMetadataDTO**](CertMetadataDTO.md)

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

<a name="deleteEndpointCertificateByAlias"></a>
# **deleteEndpointCertificateByAlias**
> deleteEndpointCertificateByAlias(alias)

Delete a certificate.

This operation can be used to delete an uploaded certificate. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.EndpointCertificatesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    EndpointCertificatesApi apiInstance = new EndpointCertificatesApi(defaultClient);
    String alias = "alias_example"; // String | The alias of the certificate that should be deleted. 
    try {
      apiInstance.deleteEndpointCertificateByAlias(alias);
    } catch (ApiException e) {
      System.err.println("Exception when calling EndpointCertificatesApi#deleteEndpointCertificateByAlias");
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

<a name="getCertificateUsageByAlias"></a>
# **getCertificateUsageByAlias**
> APIMetadataListDTO getCertificateUsageByAlias(alias, limit, offset)

Retrieve all the APIs that use a given certificate by the alias

This operation can be used to retrieve/identify apis that use a known certificate. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.EndpointCertificatesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    EndpointCertificatesApi apiInstance = new EndpointCertificatesApi(defaultClient);
    String alias = "alias_example"; // String | 
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    try {
      APIMetadataListDTO result = apiInstance.getCertificateUsageByAlias(alias, limit, offset);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling EndpointCertificatesApi#getCertificateUsageByAlias");
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
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]

### Return type

[**APIMetadataListDTO**](APIMetadataListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of qualifying APIs is returned.  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getEndpointCertificateByAlias"></a>
# **getEndpointCertificateByAlias**
> CertificateInfoDTO getEndpointCertificateByAlias(alias)

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
import org.wso2.am.integration.clients.publisher.api.v1.EndpointCertificatesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    EndpointCertificatesApi apiInstance = new EndpointCertificatesApi(defaultClient);
    String alias = "alias_example"; // String | 
    try {
      CertificateInfoDTO result = apiInstance.getEndpointCertificateByAlias(alias);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling EndpointCertificatesApi#getEndpointCertificateByAlias");
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

<a name="getEndpointCertificateContentByAlias"></a>
# **getEndpointCertificateContentByAlias**
> getEndpointCertificateContentByAlias(alias)

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
import org.wso2.am.integration.clients.publisher.api.v1.EndpointCertificatesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    EndpointCertificatesApi apiInstance = new EndpointCertificatesApi(defaultClient);
    String alias = "alias_example"; // String | 
    try {
      apiInstance.getEndpointCertificateContentByAlias(alias);
    } catch (ApiException e) {
      System.err.println("Exception when calling EndpointCertificatesApi#getEndpointCertificateContentByAlias");
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

<a name="getEndpointCertificates"></a>
# **getEndpointCertificates**
> CertificatesDTO getEndpointCertificates(limit, offset, alias, endpoint)

Retrieve/Search Uploaded Certificates

This operation can be used to retrieve and search the uploaded certificates. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.EndpointCertificatesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    EndpointCertificatesApi apiInstance = new EndpointCertificatesApi(defaultClient);
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String alias = "alias_example"; // String | Alias for the certificate
    String endpoint = "endpoint_example"; // String | Endpoint of which the certificate is uploaded
    try {
      CertificatesDTO result = apiInstance.getEndpointCertificates(limit, offset, alias, endpoint);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling EndpointCertificatesApi#getEndpointCertificates");
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
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **alias** | **String**| Alias for the certificate | [optional]
 **endpoint** | **String**| Endpoint of which the certificate is uploaded | [optional]

### Return type

[**CertificatesDTO**](CertificatesDTO.md)

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
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="updateEndpointCertificateByAlias"></a>
# **updateEndpointCertificateByAlias**
> CertMetadataDTO updateEndpointCertificateByAlias(alias, certificate)

Update a certificate.

This operation can be used to update an uploaded certificate. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.EndpointCertificatesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    EndpointCertificatesApi apiInstance = new EndpointCertificatesApi(defaultClient);
    String alias = "alias_example"; // String | Alias for the certificate
    File certificate = new File("/path/to/file"); // File | The certificate that needs to be uploaded.
    try {
      CertMetadataDTO result = apiInstance.updateEndpointCertificateByAlias(alias, certificate);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling EndpointCertificatesApi#updateEndpointCertificateByAlias");
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
 **certificate** | **File**| The certificate that needs to be uploaded. |

### Return type

[**CertMetadataDTO**](CertMetadataDTO.md)

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

