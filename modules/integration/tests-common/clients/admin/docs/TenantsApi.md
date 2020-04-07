# TenantsApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.16*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getCustomUrlInfoByTenantDomain**](TenantsApi.md#getCustomUrlInfoByTenantDomain) | **GET** /custom-urls/{tenantDomain} | Get custom-url info of a tenant domain 
[**getTenantInfoByUsername**](TenantsApi.md#getTenantInfoByUsername) | **GET** /tenant-info/{username} | Get tenant id of the user 


<a name="getCustomUrlInfoByTenantDomain"></a>
# **getCustomUrlInfoByTenantDomain**
> CustomUrlInfoDTO getCustomUrlInfoByTenantDomain(tenantDomain)

Get custom-url info of a tenant domain 

This operation is to get custom-url information of the provided tenant-domain 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.TenantsApi;


TenantsApi apiInstance = new TenantsApi();
String tenantDomain = "tenantDomain_example"; // String | The tenant domain name. 
try {
    CustomUrlInfoDTO result = apiInstance.getCustomUrlInfoByTenantDomain(tenantDomain);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling TenantsApi#getCustomUrlInfoByTenantDomain");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **tenantDomain** | **String**| The tenant domain name.  |

### Return type

[**CustomUrlInfoDTO**](CustomUrlInfoDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getTenantInfoByUsername"></a>
# **getTenantInfoByUsername**
> TenantInfoDTO getTenantInfoByUsername(username)

Get tenant id of the user 

This operation is to get tenant id of the provided user 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.TenantsApi;


TenantsApi apiInstance = new TenantsApi();
String username = "username_example"; // String | The state represents the current state of the tenant  Supported states are [ active, inactive] 
try {
    TenantInfoDTO result = apiInstance.getTenantInfoByUsername(username);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling TenantsApi#getTenantInfoByUsername");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **username** | **String**| The state represents the current state of the tenant  Supported states are [ active, inactive]  |

### Return type

[**TenantInfoDTO**](TenantInfoDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

