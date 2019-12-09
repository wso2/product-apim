# RolesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**validateSystemRole**](RolesApi.md#validateSystemRole) | **HEAD** /roles/{roleId} | Check given role name is already exist
[**validateUserRole**](RolesApi.md#validateUserRole) | **HEAD** /me/roles/{roleId} | Validate whether the logged-in user has the given role


<a name="validateSystemRole"></a>
# **validateSystemRole**
> validateSystemRole(roleId)

Check given role name is already exist

Using this operation, user can check a given role name exists or not. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.RolesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

RolesApi apiInstance = new RolesApi();
String roleId = "roleId_example"; // String | The Base 64 URL encoded role name with domain. If the given role is in secondary user-store, role ID should be derived as Base64URLEncode({user-store-name}/{role-name}). If the given role is in PRIMARY user-store, role ID can be derived as Base64URLEncode(role-name) 
try {
    apiInstance.validateSystemRole(roleId);
} catch (ApiException e) {
    System.err.println("Exception when calling RolesApi#validateSystemRole");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **roleId** | **String**| The Base 64 URL encoded role name with domain. If the given role is in secondary user-store, role ID should be derived as Base64URLEncode({user-store-name}/{role-name}). If the given role is in PRIMARY user-store, role ID can be derived as Base64URLEncode(role-name)  |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="validateUserRole"></a>
# **validateUserRole**
> validateUserRole(roleId)

Validate whether the logged-in user has the given role

Using this operation, logged-in user can check whether he has given role. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.RolesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

RolesApi apiInstance = new RolesApi();
String roleId = "roleId_example"; // String | The Base 64 URL encoded role name with domain. If the given role is in secondary user-store, role ID should be derived as Base64URLEncode({user-store-name}/{role-name}). If the given role is in PRIMARY user-store, role ID can be derived as Base64URLEncode(role-name) 
try {
    apiInstance.validateUserRole(roleId);
} catch (ApiException e) {
    System.err.println("Exception when calling RolesApi#validateUserRole");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **roleId** | **String**| The Base 64 URL encoded role name with domain. If the given role is in secondary user-store, role ID should be derived as Base64URLEncode({user-store-name}/{role-name}). If the given role is in PRIMARY user-store, role ID can be derived as Base64URLEncode(role-name)  |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

