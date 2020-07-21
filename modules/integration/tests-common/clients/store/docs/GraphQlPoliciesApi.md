# GraphQlPoliciesApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdGraphqlPoliciesComplexityGet**](GraphQlPoliciesApi.md#apisApiIdGraphqlPoliciesComplexityGet) | **GET** /apis/{apiId}/graphql-policies/complexity | Get the complexity related details of an API
[**apisApiIdGraphqlPoliciesComplexityTypesGet**](GraphQlPoliciesApi.md#apisApiIdGraphqlPoliciesComplexityTypesGet) | **GET** /apis/{apiId}/graphql-policies/complexity/types | Retrieve types and fields of a GraphQL Schema


<a name="apisApiIdGraphqlPoliciesComplexityGet"></a>
# **apisApiIdGraphqlPoliciesComplexityGet**
> apisApiIdGraphqlPoliciesComplexityGet(apiId)

Get the complexity related details of an API

This operation can be used to retrieve complexity related details belonging to an API by providing the API id. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.GraphQlPoliciesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

GraphQlPoliciesApi apiInstance = new GraphQlPoliciesApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
try {
    apiInstance.apisApiIdGraphqlPoliciesComplexityGet(apiId);
} catch (ApiException e) {
    System.err.println("Exception when calling GraphQlPoliciesApi#apisApiIdGraphqlPoliciesComplexityGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdGraphqlPoliciesComplexityTypesGet"></a>
# **apisApiIdGraphqlPoliciesComplexityTypesGet**
> GraphQLSchemaTypeListDTO apisApiIdGraphqlPoliciesComplexityTypesGet(apiId)

Retrieve types and fields of a GraphQL Schema

This operation can be used to retrieve all types and fields of the GraphQL Schema by providing the API id. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.GraphQlPoliciesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

GraphQlPoliciesApi apiInstance = new GraphQlPoliciesApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
try {
    GraphQLSchemaTypeListDTO result = apiInstance.apisApiIdGraphqlPoliciesComplexityTypesGet(apiId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling GraphQlPoliciesApi#apisApiIdGraphqlPoliciesComplexityTypesGet");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

