# GraphQlPoliciesApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdGraphqlPoliciesComplexityGet**](GraphQlPoliciesApi.md#apisApiIdGraphqlPoliciesComplexityGet) | **GET** /apis/{apiId}/graphql-policies/complexity | Get the Complexity Related Details of an API
[**apisApiIdGraphqlPoliciesComplexityTypesGet**](GraphQlPoliciesApi.md#apisApiIdGraphqlPoliciesComplexityTypesGet) | **GET** /apis/{apiId}/graphql-policies/complexity/types | Retrieve Types and Fields of a GraphQL Schema


<a name="apisApiIdGraphqlPoliciesComplexityGet"></a>
# **apisApiIdGraphqlPoliciesComplexityGet**
> GraphQLQueryComplexityInfoDTO apisApiIdGraphqlPoliciesComplexityGet(apiId)

Get the Complexity Related Details of an API

This operation can be used to retrieve complexity related details belonging to an API by providing the API id. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.GraphQlPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    GraphQlPoliciesApi apiInstance = new GraphQlPoliciesApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    try {
      GraphQLQueryComplexityInfoDTO result = apiInstance.apisApiIdGraphqlPoliciesComplexityGet(apiId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling GraphQlPoliciesApi#apisApiIdGraphqlPoliciesComplexityGet");
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
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |

### Return type

[**GraphQLQueryComplexityInfoDTO**](GraphQLQueryComplexityInfoDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Requested complexity details returned.  |  * Content-Type - The content of the body.  <br>  |
**404** | Not Found. Requested API does not contain any complexity details.  |  -  |

<a name="apisApiIdGraphqlPoliciesComplexityTypesGet"></a>
# **apisApiIdGraphqlPoliciesComplexityTypesGet**
> GraphQLSchemaTypeListDTO apisApiIdGraphqlPoliciesComplexityTypesGet(apiId)

Retrieve Types and Fields of a GraphQL Schema

This operation can be used to retrieve all types and fields of the GraphQL Schema by providing the API id. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.GraphQlPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    GraphQlPoliciesApi apiInstance = new GraphQlPoliciesApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    try {
      GraphQLSchemaTypeListDTO result = apiInstance.apisApiIdGraphqlPoliciesComplexityTypesGet(apiId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling GraphQlPoliciesApi#apisApiIdGraphqlPoliciesComplexityTypesGet");
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
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |

### Return type

[**GraphQLSchemaTypeListDTO**](GraphQLSchemaTypeListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Types and fields returned successfully.  |  * Content-Type - The content of the body.  <br>  |
**404** | Not Found. Retrieving types and fields failed.  |  -  |

