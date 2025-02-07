# GraphQlPoliciesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getGraphQLPolicyComplexityOfAPI**](GraphQlPoliciesApi.md#getGraphQLPolicyComplexityOfAPI) | **GET** /apis/{apiId}/graphql-policies/complexity | Get the Complexity Related Details of an API
[**getGraphQLPolicyComplexityTypesOfAPI**](GraphQlPoliciesApi.md#getGraphQLPolicyComplexityTypesOfAPI) | **GET** /apis/{apiId}/graphql-policies/complexity/types | Retrieve Types and Fields of a GraphQL Schema
[**updateGraphQLPolicyComplexityOfAPI**](GraphQlPoliciesApi.md#updateGraphQLPolicyComplexityOfAPI) | **PUT** /apis/{apiId}/graphql-policies/complexity | Update Complexity Related Details of an API


<a name="getGraphQLPolicyComplexityOfAPI"></a>
# **getGraphQLPolicyComplexityOfAPI**
> GraphQLQueryComplexityInfoDTO getGraphQLPolicyComplexityOfAPI(apiId)

Get the Complexity Related Details of an API

This operation can be used to retrieve complexity related details belonging to an API by providing the API id. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.GraphQlPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    GraphQlPoliciesApi apiInstance = new GraphQlPoliciesApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    try {
      GraphQLQueryComplexityInfoDTO result = apiInstance.getGraphQLPolicyComplexityOfAPI(apiId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling GraphQlPoliciesApi#getGraphQLPolicyComplexityOfAPI");
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
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="getGraphQLPolicyComplexityTypesOfAPI"></a>
# **getGraphQLPolicyComplexityTypesOfAPI**
> GraphQLSchemaTypeListDTO getGraphQLPolicyComplexityTypesOfAPI(apiId)

Retrieve Types and Fields of a GraphQL Schema

This operation can be used to retrieve all types and fields of the GraphQL Schema by providing the API id. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.GraphQlPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    GraphQlPoliciesApi apiInstance = new GraphQlPoliciesApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    try {
      GraphQLSchemaTypeListDTO result = apiInstance.getGraphQLPolicyComplexityTypesOfAPI(apiId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling GraphQlPoliciesApi#getGraphQLPolicyComplexityTypesOfAPI");
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
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="updateGraphQLPolicyComplexityOfAPI"></a>
# **updateGraphQLPolicyComplexityOfAPI**
> updateGraphQLPolicyComplexityOfAPI(apiId, graphQLQueryComplexityInfoDTO)

Update Complexity Related Details of an API

This operation can be used to update complexity details belonging to an API by providing the id of the API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.GraphQlPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    GraphQlPoliciesApi apiInstance = new GraphQlPoliciesApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    GraphQLQueryComplexityInfoDTO graphQLQueryComplexityInfoDTO = new GraphQLQueryComplexityInfoDTO(); // GraphQLQueryComplexityInfoDTO | Role-depth mapping that needs to be added
    try {
      apiInstance.updateGraphQLPolicyComplexityOfAPI(apiId, graphQLQueryComplexityInfoDTO);
    } catch (ApiException e) {
      System.err.println("Exception when calling GraphQlPoliciesApi#updateGraphQLPolicyComplexityOfAPI");
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
 **graphQLQueryComplexityInfoDTO** | [**GraphQLQueryComplexityInfoDTO**](GraphQLQueryComplexityInfoDTO.md)| Role-depth mapping that needs to be added | [optional]

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
**200** | Created. Complexity details created successfully.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

