# AlertsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getPublisherAlertTypes**](AlertsApi.md#getPublisherAlertTypes) | **GET** /alert-types | Get the list of API Publisher alert types. 


<a name="getPublisherAlertTypes"></a>
# **getPublisherAlertTypes**
> AlertTypesListDTO getPublisherAlertTypes()

Get the list of API Publisher alert types. 

This operation is used to get the list of supportd alert types for the &#39;publisher&#39; agent. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.AlertsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

AlertsApi apiInstance = new AlertsApi();
try {
    AlertTypesListDTO result = apiInstance.getPublisherAlertTypes();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling AlertsApi#getPublisherAlertTypes");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AlertTypesListDTO**](AlertTypesListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

