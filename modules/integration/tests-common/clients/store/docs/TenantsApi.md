# TenantsApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**tenantsGet**](TenantsApi.md#tenantsGet) | **GET** /tenants | Get get tenants by state 


<a name="tenantsGet"></a>
# **tenantsGet**
> TenantListDTO tenantsGet(state, limit, offset)

Get get tenants by state 

This operation is to get tenants by state 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.TenantsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

TenantsApi apiInstance = new TenantsApi();
String state = "active"; // String | The state represents the current state of the tenant  Supported states are [ active, inactive] 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
try {
    TenantListDTO result = apiInstance.tenantsGet(state, limit, offset);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling TenantsApi#tenantsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **state** | **String**| The state represents the current state of the tenant  Supported states are [ active, inactive]  | [optional] [default to active] [enum: active, inactive]
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]

### Return type

[**TenantListDTO**](TenantListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

