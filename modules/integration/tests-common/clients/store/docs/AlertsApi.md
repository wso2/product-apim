# AlertsApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getStoreAlertTypes**](AlertsApi.md#getStoreAlertTypes) | **GET** /alert-types | Get the list of API Store alert types. 


<a name="getStoreAlertTypes"></a>
# **getStoreAlertTypes**
> AlertTypesListDTO getStoreAlertTypes()

Get the list of API Store alert types. 

This operation is used to get the list of supportd alert types for the &#39;subscriber&#39; agent. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.AlertsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

AlertsApi apiInstance = new AlertsApi();
try {
    AlertTypesListDTO result = apiInstance.getStoreAlertTypes();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling AlertsApi#getStoreAlertTypes");
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

