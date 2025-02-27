# UsersApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**changeUserPassword**](UsersApi.md#changeUserPassword) | **POST** /me/change-password | Change the Password of the user
[**organizationInformation**](UsersApi.md#organizationInformation) | **GET** /me/organization-information | Get the Organization information of the user


<a name="changeUserPassword"></a>
# **changeUserPassword**
> changeUserPassword(currentAndNewPasswordsDTO)

Change the Password of the user

Using this operation, logged-in user can change their password. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.UsersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    UsersApi apiInstance = new UsersApi(defaultClient);
    CurrentAndNewPasswordsDTO currentAndNewPasswordsDTO = new CurrentAndNewPasswordsDTO(); // CurrentAndNewPasswordsDTO | Current and new password of the user 
    try {
      apiInstance.changeUserPassword(currentAndNewPasswordsDTO);
    } catch (ApiException e) {
      System.err.println("Exception when calling UsersApi#changeUserPassword");
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
 **currentAndNewPasswordsDTO** | [**CurrentAndNewPasswordsDTO**](CurrentAndNewPasswordsDTO.md)| Current and new password of the user  |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. User password changed successfully |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |

<a name="organizationInformation"></a>
# **organizationInformation**
> OrganizationInfoDTO organizationInformation()

Get the Organization information of the user

Using this operation, logged-in user can get their organization information. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.UsersApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    UsersApi apiInstance = new UsersApi(defaultClient);
    try {
      OrganizationInfoDTO result = apiInstance.organizationInformation();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling UsersApi#organizationInformation");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**OrganizationInfoDTO**](OrganizationInfoDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Key Manager list returned  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |

