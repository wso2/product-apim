# ExportConfigurationApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**exportApisGet**](ExportConfigurationApi.md#exportApisGet) | **GET** /export/apis | Export information related to an API.


<a name="exportApisGet"></a>
# **exportApisGet**
> File exportApisGet(query, limit, offset)

Export information related to an API.

This operation can be used to export information related to a particular API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ExportConfigurationApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ExportConfigurationApi apiInstance = new ExportConfigurationApi();
String query = "query_example"; // String | API search query 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
try {
    File result = apiInstance.exportApisGet(query, limit, offset);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ExportConfigurationApi#exportApisGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **query** | **String**| API search query  |
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]

### Return type

[**File**](File.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/zip

