# ApplicationCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.16*

Method | HTTP request | Description
------------- | ------------- | -------------
[**applicationsGet**](ApplicationCollectionApi.md#applicationsGet) | **GET** /applications | Retrieve/Search applications 


<a name="applicationsGet"></a>
# **applicationsGet**
> ApplicationListDTO applicationsGet(user, limit, offset, accept, ifNoneMatch, tenantDomain)

Retrieve/Search applications 

This operation can be used to retrieve list of applications that is belonged to the given user, If no user is provided then the application for the user associated with the provided access token will be returned. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.ApplicationCollectionApi;


ApplicationCollectionApi apiInstance = new ApplicationCollectionApi();
String user = "user_example"; // String | username of the application creator 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
String tenantDomain = "tenantDomain_example"; // String | Tenant domain of the applications to get. This has to be specified only if require to get applications of another tenant other than the requester's tenant. So, if not specified, the default will be set as the requester's tenant domain. This cross tenant Application access is allowed only for super tenant admin users only at a migration process. 
try {
    ApplicationListDTO result = apiInstance.applicationsGet(user, limit, offset, accept, ifNoneMatch, tenantDomain);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationCollectionApi#applicationsGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user** | **String**| username of the application creator  | [optional]
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]
 **tenantDomain** | **String**| Tenant domain of the applications to get. This has to be specified only if require to get applications of another tenant other than the requester&#39;s tenant. So, if not specified, the default will be set as the requester&#39;s tenant domain. This cross tenant Application access is allowed only for super tenant admin users only at a migration process.  | [optional]

### Return type

[**ApplicationListDTO**](ApplicationListDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

