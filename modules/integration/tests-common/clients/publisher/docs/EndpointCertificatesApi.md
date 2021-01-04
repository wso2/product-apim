# EndpointCertificatesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**endpointCertificatesAliasContentGet**](EndpointCertificatesApi.md#endpointCertificatesAliasContentGet) | **GET** /endpoint-certificates/{alias}/content | Download a Certificate
[**endpointCertificatesAliasDelete**](EndpointCertificatesApi.md#endpointCertificatesAliasDelete) | **DELETE** /endpoint-certificates/{alias} | Delete a certificate.
[**endpointCertificatesAliasGet**](EndpointCertificatesApi.md#endpointCertificatesAliasGet) | **GET** /endpoint-certificates/{alias} | Get the Certificate Information
[**endpointCertificatesAliasPut**](EndpointCertificatesApi.md#endpointCertificatesAliasPut) | **PUT** /endpoint-certificates/{alias} | Update a certificate.
[**endpointCertificatesGet**](EndpointCertificatesApi.md#endpointCertificatesGet) | **GET** /endpoint-certificates | Retrieve/Search Uploaded Certificates
[**endpointCertificatesPost**](EndpointCertificatesApi.md#endpointCertificatesPost) | **POST** /endpoint-certificates | Upload a new Certificate.


<a name="endpointCertificatesAliasContentGet"></a>
# **endpointCertificatesAliasContentGet**
> endpointCertificatesAliasContentGet(alias)

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v1");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    EndpointCertificatesApi apiInstance = new EndpointCertificatesApi(defaultClient);
    String alias = "alias_example"; // String | 
    try {
      apiInstance.endpointCertificatesAliasContentGet(alias);
    } catch (ApiException e) {
      System.err.println("Exception when calling EndpointCertificatesApi#endpointCertificatesAliasContentGet");
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

<a name="endpointCertificatesAliasDelete"></a>
# **endpointCertificatesAliasDelete**
> endpointCertificatesAliasDelete(alias)

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v1");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    EndpointCertificatesApi apiInstance = new EndpointCertificatesApi(defaultClient);
    String alias = "alias_example"; // String | The alias of the certificate that should be deleted. 
    try {
      apiInstance.endpointCertificatesAliasDelete(alias);
    } catch (ApiException e) {
      System.err.println("Exception when calling EndpointCertificatesApi#endpointCertificatesAliasDelete");
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

<a name="endpointCertificatesAliasGet"></a>
# **endpointCertificatesAliasGet**
> CertificateInfoDTO endpointCertificatesAliasGet(alias)

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v1");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    EndpointCertificatesApi apiInstance = new EndpointCertificatesApi(defaultClient);
    String alias = "alias_example"; // String | 
    try {
      CertificateInfoDTO result = apiInstance.endpointCertificatesAliasGet(alias);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling EndpointCertificatesApi#endpointCertificatesAliasGet");
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

<a name="endpointCertificatesAliasPut"></a>
# **endpointCertificatesAliasPut**
> CertMetadataDTO endpointCertificatesAliasPut(alias, certificate)

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v1");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    EndpointCertificatesApi apiInstance = new EndpointCertificatesApi(defaultClient);
    String alias = "alias_example"; // String | Alias for the certificate
    File certificate = new File("/path/to/file"); // File | The certificate that needs to be uploaded.
    try {
      CertMetadataDTO result = apiInstance.endpointCertificatesAliasPut(alias, certificate);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling EndpointCertificatesApi#endpointCertificatesAliasPut");
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

<a name="endpointCertificatesGet"></a>
# **endpointCertificatesGet**
> CertificatesDTO endpointCertificatesGet(limit, offset, alias, endpoint)

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v1");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    EndpointCertificatesApi apiInstance = new EndpointCertificatesApi(defaultClient);
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String alias = "alias_example"; // String | Alias for the certificate
    String endpoint = "endpoint_example"; // String | Endpoint of which the certificate is uploaded
    try {
      CertificatesDTO result = apiInstance.endpointCertificatesGet(limit, offset, alias, endpoint);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling EndpointCertificatesApi#endpointCertificatesGet");
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

<a name="endpointCertificatesPost"></a>
# **endpointCertificatesPost**
> CertMetadataDTO endpointCertificatesPost(certificate, alias, endpoint)

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
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v1");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    EndpointCertificatesApi apiInstance = new EndpointCertificatesApi(defaultClient);
    File certificate = new File("/path/to/file"); // File | The certificate that needs to be uploaded.
    String alias = "alias_example"; // String | Alias for the certificate
    String endpoint = "endpoint_example"; // String | Endpoint to which the certificate should be applied.
    try {
      CertMetadataDTO result = apiInstance.endpointCertificatesPost(certificate, alias, endpoint);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling EndpointCertificatesApi#endpointCertificatesPost");
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

