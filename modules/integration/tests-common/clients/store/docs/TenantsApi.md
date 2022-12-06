# TenantsApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**tenantsGet**](TenantsApi.md#tenantsGet) | **GET** /tenants | Get Tenants by State 


<a name="tenantsGet"></a>
# **tenantsGet**
> TenantListDTO tenantsGet(state, limit, offset)

Get Tenants by State 

This operation is to get tenants by state 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.TenantsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    TenantsApi apiInstance = new TenantsApi(defaultClient);
    String state = "active"; // String | The state represents the current state of the tenant  Supported states are [ active, inactive] 
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    try {
      TenantListDTO result = apiInstance.tenantsGet(state, limit, offset);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling TenantsApi#tenantsGet");
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
 **state** | **String**| The state represents the current state of the tenant  Supported states are [ active, inactive]  | [optional] [default to active] [enum: active, inactive]
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]

### Return type

[**TenantListDTO**](TenantListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Tenant names returned.  |  * Content-Type - The content type of the body.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

