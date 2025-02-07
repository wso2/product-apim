# SignUpApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**selfSignupPost**](SignUpApi.md#selfSignupPost) | **POST** /self-signup | Register a new user


<a name="selfSignupPost"></a>
# **selfSignupPost**
> UserDTO selfSignupPost(body)

Register a new user

User self signup API 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.SignUpApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

SignUpApi apiInstance = new SignUpApi();
UserDTO body = new UserDTO(); // UserDTO | User object to represent the new user 
try {
    UserDTO result = apiInstance.selfSignupPost(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling SignUpApi#selfSignupPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**UserDTO**](UserDTO.md)| User object to represent the new user  |

### Return type

[**UserDTO**](UserDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

