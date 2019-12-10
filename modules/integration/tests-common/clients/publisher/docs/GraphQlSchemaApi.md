# GraphQlSchemaApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdGraphqlSchemaPut**](GraphQlSchemaApi.md#apisApiIdGraphqlSchemaPut) | **PUT** /apis/{apiId}/graphql-schema | Add a Schema to a GraphQL API


<a name="apisApiIdGraphqlSchemaPut"></a>
# **apisApiIdGraphqlSchemaPut**
> apisApiIdGraphqlSchemaPut(apiId, schemaDefinition, ifMatch)

Add a Schema to a GraphQL API

This operation can be used to add a GraphQL Schema definition to an existing GraphQL API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.GraphQlSchemaApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

GraphQlSchemaApi apiInstance = new GraphQlSchemaApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String schemaDefinition = "schemaDefinition_example"; // String | schema definition of the GraphQL API
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.apisApiIdGraphqlSchemaPut(apiId, schemaDefinition, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling GraphQlSchemaApi#apisApiIdGraphqlSchemaPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **schemaDefinition** | **String**| schema definition of the GraphQL API |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

