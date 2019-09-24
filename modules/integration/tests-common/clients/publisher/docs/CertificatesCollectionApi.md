# CertificatesCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**endpointCertificatesGet**](CertificatesCollectionApi.md#endpointCertificatesGet) | **GET** /endpoint-certificates | Retrieve/Search uploaded certificates.


<a name="endpointCertificatesGet"></a>
# **endpointCertificatesGet**
> CertificatesDTO endpointCertificatesGet(limit, offset, alias, endpoint)

Retrieve/Search uploaded certificates.

This operation can be used to retrieve and search the uploaded certificates. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.CertificatesCollectionApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

CertificatesCollectionApi apiInstance = new CertificatesCollectionApi();
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String alias = "alias_example"; // String | Alias for the certificate
String endpoint = "endpoint_example"; // String | Endpoint of which the certificate is uploaded
try {
    CertificatesDTO result = apiInstance.endpointCertificatesGet(limit, offset, alias, endpoint);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling CertificatesCollectionApi#endpointCertificatesGet");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

