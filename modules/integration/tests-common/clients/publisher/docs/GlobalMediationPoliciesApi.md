# GlobalMediationPoliciesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getAllGlobalMediationPolicies**](GlobalMediationPoliciesApi.md#getAllGlobalMediationPolicies) | **GET** /mediation-policies | Get all global level mediation policies 


<a name="getAllGlobalMediationPolicies"></a>
# **getAllGlobalMediationPolicies**
> MediationListDTO getAllGlobalMediationPolicies(limit, offset, query, ifNoneMatch)

Get all global level mediation policies 

This operation provides you a list of available all global level mediation policies. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.GlobalMediationPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    GlobalMediationPoliciesApi apiInstance = new GlobalMediationPoliciesApi(defaultClient);
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String query = "query_example"; // String | -Not supported yet-
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      MediationListDTO result = apiInstance.getAllGlobalMediationPolicies(limit, offset, query, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling GlobalMediationPoliciesApi#getAllGlobalMediationPolicies");
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
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **query** | **String**| -Not supported yet- | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**MediationListDTO**](MediationListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of mediation policies is returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body. <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

