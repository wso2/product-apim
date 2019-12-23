# ValidationApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisValidateGraphqlSchemaPost**](ValidationApi.md#apisValidateGraphqlSchemaPost) | **POST** /apis/validate-graphql-schema | Validate GraphQL API definition and retrieve a summary
[**validateAPI**](ValidationApi.md#validateAPI) | **POST** /apis/validate | Check given API attibute name is already exist.
[**validateEndpoint**](ValidationApi.md#validateEndpoint) | **POST** /apis/validate-endpoint | Check whether given endpoint url is valid
[**validateOpenAPIDefinition**](ValidationApi.md#validateOpenAPIDefinition) | **POST** /apis/validate-openapi | Validate an OpenAPI Definition
[**validateWSDLDefinition**](ValidationApi.md#validateWSDLDefinition) | **POST** /apis/validate-wsdl | Validate a WSDL Definition


<a name="apisValidateGraphqlSchemaPost"></a>
# **apisValidateGraphqlSchemaPost**
> GraphQLValidationResponseDTO apisValidateGraphqlSchemaPost(file)

Validate GraphQL API definition and retrieve a summary

This operation can be used to validate a graphQL definition and retrieve a summary. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ValidationApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ValidationApi apiInstance = new ValidationApi();
File file = new File("/path/to/file.txt"); // File | Definition to upload as a file
try {
    GraphQLValidationResponseDTO result = apiInstance.apisValidateGraphqlSchemaPost(file);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ValidationApi#apisValidateGraphqlSchemaPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file** | **File**| Definition to upload as a file |

### Return type

[**GraphQLValidationResponseDTO**](GraphQLValidationResponseDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a name="validateAPI"></a>
# **validateAPI**
> validateAPI(query, ifNoneMatch)

Check given API attibute name is already exist.

Using this operation, you can check a given API context is already used. You need to provide the context name you want to check. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ValidationApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ValidationApi apiInstance = new ValidationApi();
String query = "query_example"; // String | **Search condition**.  You can search in attributes by using an **\"<attribute>:\"** modifier.  Eg. \"name:wso2\" will match an API if the provider of the API is exactly \"wso2\".  Supported attribute modifiers are [** version, context, name **]  If no advanced attribute modifier has been specified, search will match the given query string against API Name. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    apiInstance.validateAPI(query, ifNoneMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ValidationApi#validateAPI");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **query** | **String**| **Search condition**.  You can search in attributes by using an **\&quot;&lt;attribute&gt;:\&quot;** modifier.  Eg. \&quot;name:wso2\&quot; will match an API if the provider of the API is exactly \&quot;wso2\&quot;.  Supported attribute modifiers are [** version, context, name **]  If no advanced attribute modifier has been specified, search will match the given query string against API Name.  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="validateEndpoint"></a>
# **validateEndpoint**
> ApiEndpointValidationResponseDTO validateEndpoint(endpointUrl, apiId)

Check whether given endpoint url is valid

Using this operation, it is possible check whether the given API endpoint url is a valid url 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ValidationApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ValidationApi apiInstance = new ValidationApi();
String endpointUrl = "endpointUrl_example"; // String | API endpoint url
String apiId = "apiId_example"; // String | 
try {
    ApiEndpointValidationResponseDTO result = apiInstance.validateEndpoint(endpointUrl, apiId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ValidationApi#validateEndpoint");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **endpointUrl** | **String**| API endpoint url |
 **apiId** | **String**|  | [optional]

### Return type

[**ApiEndpointValidationResponseDTO**](ApiEndpointValidationResponseDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="validateOpenAPIDefinition"></a>
# **validateOpenAPIDefinition**
> OpenAPIDefinitionValidationResponseDTO validateOpenAPIDefinition(url, file, returnContent)

Validate an OpenAPI Definition

This operation can be used to validate an OpenAPI definition and retrieve a summary. Provide either &#x60;url&#x60; or &#x60;file&#x60; to specify the definition. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ValidationApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ValidationApi apiInstance = new ValidationApi();
String url = "url_example"; // String | OpenAPI definition url
File file = new File("/path/to/file.txt"); // File | OpenAPI definition as a file
Boolean returnContent = false; // Boolean | Specify whether to return the full content of the OpenAPI definition in the response. This is only applicable when using url based validation 
try {
    OpenAPIDefinitionValidationResponseDTO result = apiInstance.validateOpenAPIDefinition(url, file, returnContent);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ValidationApi#validateOpenAPIDefinition");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **url** | **String**| OpenAPI definition url | [optional]
 **file** | **File**| OpenAPI definition as a file | [optional]
 **returnContent** | **Boolean**| Specify whether to return the full content of the OpenAPI definition in the response. This is only applicable when using url based validation  | [optional] [default to false]

### Return type

[**OpenAPIDefinitionValidationResponseDTO**](OpenAPIDefinitionValidationResponseDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a name="validateWSDLDefinition"></a>
# **validateWSDLDefinition**
> WSDLValidationResponseDTO validateWSDLDefinition(url, file)

Validate a WSDL Definition

This operation can be used to validate a WSDL definition and retrieve a summary. Provide either &#x60;url&#x60; or &#x60;file&#x60; to specify the definition. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ValidationApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ValidationApi apiInstance = new ValidationApi();
String url = "url_example"; // String | Definition url
File file = new File("/path/to/file.txt"); // File | Definition to upload as a file
try {
    WSDLValidationResponseDTO result = apiInstance.validateWSDLDefinition(url, file);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ValidationApi#validateWSDLDefinition");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **url** | **String**| Definition url | [optional]
 **file** | **File**| Definition to upload as a file | [optional]

### Return type

[**WSDLValidationResponseDTO**](WSDLValidationResponseDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

