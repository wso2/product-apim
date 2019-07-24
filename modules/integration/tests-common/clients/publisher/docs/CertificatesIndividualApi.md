# CertificatesIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**endpointCertificatesAliasContentGet**](CertificatesIndividualApi.md#endpointCertificatesAliasContentGet) | **GET** /endpoint-certificates/{alias}/content | Download a certificate.
[**endpointCertificatesAliasDelete**](CertificatesIndividualApi.md#endpointCertificatesAliasDelete) | **DELETE** /endpoint-certificates/{alias} | Delete a certificate.
[**endpointCertificatesAliasGet**](CertificatesIndividualApi.md#endpointCertificatesAliasGet) | **GET** /endpoint-certificates/{alias} | Get the certificate information.
[**endpointCertificatesAliasPut**](CertificatesIndividualApi.md#endpointCertificatesAliasPut) | **PUT** /endpoint-certificates/{alias} | Update a certificate.
[**endpointCertificatesPost**](CertificatesIndividualApi.md#endpointCertificatesPost) | **POST** /endpoint-certificates | Upload a new Certificate.


<a name="endpointCertificatesAliasContentGet"></a>
# **endpointCertificatesAliasContentGet**
> endpointCertificatesAliasContentGet(alias)

Download a certificate.

This operation can be used to download a certificate which matches the given alias. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.CertificatesIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

CertificatesIndividualApi apiInstance = new CertificatesIndividualApi();
String alias = "alias_example"; // String | 
try {
    apiInstance.endpointCertificatesAliasContentGet(alias);
} catch (ApiException e) {
    System.err.println("Exception when calling CertificatesIndividualApi#endpointCertificatesAliasContentGet");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="endpointCertificatesAliasDelete"></a>
# **endpointCertificatesAliasDelete**
> endpointCertificatesAliasDelete(alias)

Delete a certificate.

This operation can be used to delete an uploaded certificate. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.CertificatesIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

CertificatesIndividualApi apiInstance = new CertificatesIndividualApi();
String alias = "alias_example"; // String | The alias of the certificate that should be deleted. 
try {
    apiInstance.endpointCertificatesAliasDelete(alias);
} catch (ApiException e) {
    System.err.println("Exception when calling CertificatesIndividualApi#endpointCertificatesAliasDelete");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="endpointCertificatesAliasGet"></a>
# **endpointCertificatesAliasGet**
> CertificateInfoDTO endpointCertificatesAliasGet(alias)

Get the certificate information.

This operation can be used to get the information about a certificate. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.CertificatesIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

CertificatesIndividualApi apiInstance = new CertificatesIndividualApi();
String alias = "alias_example"; // String | 
try {
    CertificateInfoDTO result = apiInstance.endpointCertificatesAliasGet(alias);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CertificatesIndividualApi#endpointCertificatesAliasGet");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="endpointCertificatesAliasPut"></a>
# **endpointCertificatesAliasPut**
> CertMetadataDTO endpointCertificatesAliasPut(certificate, alias)

Update a certificate.

This operation can be used to update an uploaded certificate. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.CertificatesIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

CertificatesIndividualApi apiInstance = new CertificatesIndividualApi();
File certificate = new File("/path/to/file.txt"); // File | The certificate that needs to be uploaded.
String alias = "alias_example"; // String | Alias for the certificate
try {
    CertMetadataDTO result = apiInstance.endpointCertificatesAliasPut(certificate, alias);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CertificatesIndividualApi#endpointCertificatesAliasPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **certificate** | **File**| The certificate that needs to be uploaded. |
 **alias** | **String**| Alias for the certificate |

### Return type

[**CertMetadataDTO**](CertMetadataDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a name="endpointCertificatesPost"></a>
# **endpointCertificatesPost**
> CertMetadataDTO endpointCertificatesPost(certificate, alias, endpoint)

Upload a new Certificate.

This operation can be used to upload a new certificate for an endpoint. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.CertificatesIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

CertificatesIndividualApi apiInstance = new CertificatesIndividualApi();
File certificate = new File("/path/to/file.txt"); // File | The certificate that needs to be uploaded.
String alias = "alias_example"; // String | Alias for the certificate
String endpoint = "endpoint_example"; // String | Endpoint to which the certificate should be applied.
try {
    CertMetadataDTO result = apiInstance.endpointCertificatesPost(certificate, alias, endpoint);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CertificatesIndividualApi#endpointCertificatesPost");
    e.printStackTrace();
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

