# ClientCertificatesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdClientCertificatesAliasContentGet**](ClientCertificatesApi.md#apisApiIdClientCertificatesAliasContentGet) | **GET** /apis/{apiId}/client-certificates/{alias}/content | Download a certificate.
[**apisApiIdClientCertificatesAliasDelete**](ClientCertificatesApi.md#apisApiIdClientCertificatesAliasDelete) | **DELETE** /apis/{apiId}/client-certificates/{alias} | Delete a certificate.
[**apisApiIdClientCertificatesAliasGet**](ClientCertificatesApi.md#apisApiIdClientCertificatesAliasGet) | **GET** /apis/{apiId}/client-certificates/{alias} | Get the certificate information.
[**apisApiIdClientCertificatesAliasPut**](ClientCertificatesApi.md#apisApiIdClientCertificatesAliasPut) | **PUT** /apis/{apiId}/client-certificates/{alias} | Update a certificate.
[**apisApiIdClientCertificatesGet**](ClientCertificatesApi.md#apisApiIdClientCertificatesGet) | **GET** /apis/{apiId}/client-certificates | Retrieve/ Search uploaded Client Certificates.
[**apisApiIdClientCertificatesPost**](ClientCertificatesApi.md#apisApiIdClientCertificatesPost) | **POST** /apis/{apiId}/client-certificates | Upload a new certificate.


<a name="apisApiIdClientCertificatesAliasContentGet"></a>
# **apisApiIdClientCertificatesAliasContentGet**
> apisApiIdClientCertificatesAliasContentGet(apiId, alias)

Download a certificate.

This operation can be used to download a certificate which matches the given alias. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ClientCertificatesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ClientCertificatesApi apiInstance = new ClientCertificatesApi();
String apiId = "apiId_example"; // String | The api identifier
String alias = "alias_example"; // String | 
try {
    apiInstance.apisApiIdClientCertificatesAliasContentGet(apiId, alias);
} catch (ApiException e) {
    System.err.println("Exception when calling ClientCertificatesApi#apisApiIdClientCertificatesAliasContentGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| The api identifier |
 **alias** | **String**|  |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdClientCertificatesAliasDelete"></a>
# **apisApiIdClientCertificatesAliasDelete**
> apisApiIdClientCertificatesAliasDelete(alias, apiId)

Delete a certificate.

This operation can be used to delete an uploaded certificate. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ClientCertificatesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ClientCertificatesApi apiInstance = new ClientCertificatesApi();
String alias = "alias_example"; // String | The alias of the certificate that should be deleted. 
String apiId = "apiId_example"; // String | The api identifier
try {
    apiInstance.apisApiIdClientCertificatesAliasDelete(alias, apiId);
} catch (ApiException e) {
    System.err.println("Exception when calling ClientCertificatesApi#apisApiIdClientCertificatesAliasDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **alias** | **String**| The alias of the certificate that should be deleted.  |
 **apiId** | **String**| The api identifier |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdClientCertificatesAliasGet"></a>
# **apisApiIdClientCertificatesAliasGet**
> CertificateInfoDTO apisApiIdClientCertificatesAliasGet(alias, apiId)

Get the certificate information.

This operation can be used to get the information about a certificate. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ClientCertificatesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ClientCertificatesApi apiInstance = new ClientCertificatesApi();
String alias = "alias_example"; // String | 
String apiId = "apiId_example"; // String | The api identifier
try {
    CertificateInfoDTO result = apiInstance.apisApiIdClientCertificatesAliasGet(alias, apiId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ClientCertificatesApi#apisApiIdClientCertificatesAliasGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **alias** | **String**|  |
 **apiId** | **String**| The api identifier |

### Return type

[**CertificateInfoDTO**](CertificateInfoDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdClientCertificatesAliasPut"></a>
# **apisApiIdClientCertificatesAliasPut**
> ClientCertMetadataDTO apisApiIdClientCertificatesAliasPut(alias, apiId, certificate, tier)

Update a certificate.

This operation can be used to update an uploaded certificate. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ClientCertificatesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ClientCertificatesApi apiInstance = new ClientCertificatesApi();
String alias = "alias_example"; // String | Alias for the certificate
String apiId = "apiId_example"; // String | The api identifier
File certificate = new File("/path/to/file.txt"); // File | The certificate that needs to be uploaded.
String tier = "tier_example"; // String | The tier of the certificate
try {
    ClientCertMetadataDTO result = apiInstance.apisApiIdClientCertificatesAliasPut(alias, apiId, certificate, tier);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ClientCertificatesApi#apisApiIdClientCertificatesAliasPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **alias** | **String**| Alias for the certificate |
 **apiId** | **String**| The api identifier |
 **certificate** | **File**| The certificate that needs to be uploaded. | [optional]
 **tier** | **String**| The tier of the certificate | [optional]

### Return type

[**ClientCertMetadataDTO**](ClientCertMetadataDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a name="apisApiIdClientCertificatesGet"></a>
# **apisApiIdClientCertificatesGet**
> ClientCertificatesDTO apisApiIdClientCertificatesGet(apiId, limit, offset, alias)

Retrieve/ Search uploaded Client Certificates.

This operation can be used to retrieve and search the uploaded client certificates. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ClientCertificatesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ClientCertificatesApi apiInstance = new ClientCertificatesApi();
String apiId = "apiId_example"; // String | UUID of the API
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String alias = "alias_example"; // String | Alias for the client certificate
try {
    ClientCertificatesDTO result = apiInstance.apisApiIdClientCertificatesGet(apiId, limit, offset, alias);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ClientCertificatesApi#apisApiIdClientCertificatesGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| UUID of the API |
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **alias** | **String**| Alias for the client certificate | [optional]

### Return type

[**ClientCertificatesDTO**](ClientCertificatesDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdClientCertificatesPost"></a>
# **apisApiIdClientCertificatesPost**
> ClientCertMetadataDTO apisApiIdClientCertificatesPost(certificate, alias, apiId, tier)

Upload a new certificate.

This operation can be used to upload a new certificate for an endpoint. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ClientCertificatesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ClientCertificatesApi apiInstance = new ClientCertificatesApi();
File certificate = new File("/path/to/file.txt"); // File | The certificate that needs to be uploaded.
String alias = "alias_example"; // String | Alias for the certificate
String apiId = "apiId_example"; // String | apiId to which the certificate should be applied.
String tier = "tier_example"; // String | apiId to which the certificate should be applied.
try {
    ClientCertMetadataDTO result = apiInstance.apisApiIdClientCertificatesPost(certificate, alias, apiId, tier);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ClientCertificatesApi#apisApiIdClientCertificatesPost");
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

