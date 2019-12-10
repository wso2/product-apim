# GraphQlSchemaIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdGraphqlSchemaGet**](GraphQlSchemaIndividualApi.md#apisApiIdGraphqlSchemaGet) | **GET** /apis/{apiId}/graphql-schema | Get the Schema of a GraphQL API


<a name="apisApiIdGraphqlSchemaGet"></a>
# **apisApiIdGraphqlSchemaGet**
> GraphQLSchemaDTO apisApiIdGraphqlSchemaGet(apiId, accept, ifNoneMatch)

Get the Schema of a GraphQL API

This operation can be used to retrieve the Schema definition of a GraphQL API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.GraphQlSchemaIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

GraphQlSchemaIndividualApi apiInstance = new GraphQlSchemaIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    GraphQLSchemaDTO result = apiInstance.apisApiIdGraphqlSchemaGet(apiId, accept, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling GraphQlSchemaIndividualApi#apisApiIdGraphqlSchemaGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**GraphQLSchemaDTO**](GraphQLSchemaDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

