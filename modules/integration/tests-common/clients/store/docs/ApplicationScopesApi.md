# ApplicationScopesApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**applicationsApplicationIdScopesGet**](ApplicationScopesApi.md#applicationsApplicationIdScopesGet) | **GET** /applications/{applicationId}/scopes | Get scopes of application 


<a name="applicationsApplicationIdScopesGet"></a>
# **applicationsApplicationIdScopesGet**
> ScopeListDTO applicationsApplicationIdScopesGet(applicationId, filterByUserRoles, ifNoneMatch)

Get scopes of application 

Get scopes associated with a particular application based on subscribed APIs 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationScopesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationScopesApi apiInstance = new ApplicationScopesApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
Boolean filterByUserRoles = true; // Boolean | Filter user by roles. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
try {
    ScopeListDTO result = apiInstance.applicationsApplicationIdScopesGet(applicationId, filterByUserRoles, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationScopesApi#applicationsApplicationIdScopesGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **filterByUserRoles** | **Boolean**| Filter user by roles.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**ScopeListDTO**](ScopeListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

