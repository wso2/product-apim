# ClientCertificatesIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**clientCertificatesAliasContentGet**](ClientCertificatesIndividualApi.md#clientCertificatesAliasContentGet) | **GET** /client-certificates/{alias}/content | Download a certificate.
[**clientCertificatesAliasDelete**](ClientCertificatesIndividualApi.md#clientCertificatesAliasDelete) | **DELETE** /client-certificates/{alias} | Delete a certificate.
[**clientCertificatesAliasGet**](ClientCertificatesIndividualApi.md#clientCertificatesAliasGet) | **GET** /client-certificates/{alias} | Get the certificate information.
[**clientCertificatesAliasPut**](ClientCertificatesIndividualApi.md#clientCertificatesAliasPut) | **PUT** /client-certificates/{alias} | Update a certificate.
[**clientCertificatesPost**](ClientCertificatesIndividualApi.md#clientCertificatesPost) | **POST** /client-certificates | Upload a new certificate.


<a name="clientCertificatesAliasContentGet"></a>
# **clientCertificatesAliasContentGet**
> clientCertificatesAliasContentGet(alias)

Download a certificate.

This operation can be used to download a certificate which matches the given alias. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ClientCertificatesIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ClientCertificatesIndividualApi apiInstance = new ClientCertificatesIndividualApi();
String alias = "alias_example"; // String | 
try {
    apiInstance.clientCertificatesAliasContentGet(alias);
} catch (ApiException e) {
    System.err.println("Exception when calling ClientCertificatesIndividualApi#clientCertificatesAliasContentGet");
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

<a name="clientCertificatesAliasDelete"></a>
# **clientCertificatesAliasDelete**
> clientCertificatesAliasDelete(alias)

Delete a certificate.

This operation can be used to delete an uploaded certificate. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ClientCertificatesIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ClientCertificatesIndividualApi apiInstance = new ClientCertificatesIndividualApi();
String alias = "alias_example"; // String | The alias of the certificate that should be deleted. 
try {
    apiInstance.clientCertificatesAliasDelete(alias);
} catch (ApiException e) {
    System.err.println("Exception when calling ClientCertificatesIndividualApi#clientCertificatesAliasDelete");
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

<a name="clientCertificatesAliasGet"></a>
# **clientCertificatesAliasGet**
> CertificateInfoDTO clientCertificatesAliasGet(alias)

Get the certificate information.

This operation can be used to get the information about a certificate. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ClientCertificatesIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ClientCertificatesIndividualApi apiInstance = new ClientCertificatesIndividualApi();
String alias = "alias_example"; // String | 
try {
    CertificateInfoDTO result = apiInstance.clientCertificatesAliasGet(alias);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ClientCertificatesIndividualApi#clientCertificatesAliasGet");
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

<a name="clientCertificatesAliasPut"></a>
# **clientCertificatesAliasPut**
> ClientCertMetadataDTO clientCertificatesAliasPut(alias, certificate, tier)

Update a certificate.

This operation can be used to update an uploaded certificate. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ClientCertificatesIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ClientCertificatesIndividualApi apiInstance = new ClientCertificatesIndividualApi();
String alias = "alias_example"; // String | Alias for the certificate
File certificate = new File("/path/to/file.txt"); // File | The certificate that needs to be uploaded.
String tier = "tier_example"; // String | The tier of the certificate
try {
    ClientCertMetadataDTO result = apiInstance.clientCertificatesAliasPut(alias, certificate, tier);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ClientCertificatesIndividualApi#clientCertificatesAliasPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **alias** | **String**| Alias for the certificate |
 **certificate** | **File**| The certificate that needs to be uploaded. | [optional]
 **tier** | **String**| The tier of the certificate | [optional]

### Return type

[**ClientCertMetadataDTO**](ClientCertMetadataDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a name="clientCertificatesPost"></a>
# **clientCertificatesPost**
> ClientCertMetadataDTO clientCertificatesPost(certificate, alias, apiId, tier)

Upload a new certificate.

This operation can be used to upload a new certificate for an endpoint. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ClientCertificatesIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ClientCertificatesIndividualApi apiInstance = new ClientCertificatesIndividualApi();
File certificate = new File("/path/to/file.txt"); // File | The certificate that needs to be uploaded.
String alias = "alias_example"; // String | Alias for the certificate
String apiId = "apiId_example"; // String | apiId to which the certificate should be applied.
String tier = "tier_example"; // String | apiId to which the certificate should be applied.
try {
    ClientCertMetadataDTO result = apiInstance.clientCertificatesPost(certificate, alias, apiId, tier);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ClientCertificatesIndividualApi#clientCertificatesPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **certificate** | **File**| The certificate that needs to be uploaded. |
 **alias** | **String**| Alias for the certificate |
 **apiId** | **String**| apiId to which the certificate should be applied. |
 **tier** | **String**| apiId to which the certificate should be applied. |

### Return type

[**ClientCertMetadataDTO**](ClientCertMetadataDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

