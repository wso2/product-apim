# UsersApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**changeUserPassword**](UsersApi.md#changeUserPassword) | **POST** /me/change-password | Change the password of the user


<a name="changeUserPassword"></a>
# **changeUserPassword**
> changeUserPassword(body)

Change the password of the user

Using this operation, logged-in user can change his/her password. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.UsersApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

UsersApi apiInstance = new UsersApi();
CurrentAndNewPasswordsDTO body = new CurrentAndNewPasswordsDTO(); // CurrentAndNewPasswordsDTO | Current and new password of the user 
try {
    apiInstance.changeUserPassword(body);
} catch (ApiException e) {
    System.err.println("Exception when calling UsersApi#changeUserPassword");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**CurrentAndNewPasswordsDTO**](CurrentAndNewPasswordsDTO.md)| Current and new password of the user  |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

