# ClientCertificatesCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**clientCertificatesGet**](ClientCertificatesCollectionApi.md#clientCertificatesGet) | **GET** /client-certificates | Retrieve/ Search uploaded Client Certificates.


<a name="clientCertificatesGet"></a>
# **clientCertificatesGet**
> ClientCertificatesDTO clientCertificatesGet(limit, offset, alias, apiId)

Retrieve/ Search uploaded Client Certificates.

This operation can be used to retrieve and search the uploaded client certificates. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ClientCertificatesCollectionApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ClientCertificatesCollectionApi apiInstance = new ClientCertificatesCollectionApi();
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String alias = "alias_example"; // String | Alias for the client certificate
String apiId = "apiId_example"; // String | UUID of the API
try {
    ClientCertificatesDTO result = apiInstance.clientCertificatesGet(limit, offset, alias, apiId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ClientCertificatesCollectionApi#clientCertificatesGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **alias** | **String**| Alias for the client certificate | [optional]
 **apiId** | **String**| UUID of the API | [optional]

### Return type

[**ClientCertificatesDTO**](ClientCertificatesDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

