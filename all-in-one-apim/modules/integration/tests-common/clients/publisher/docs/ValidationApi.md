# ValidationApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**validateAPI**](ValidationApi.md#validateAPI) | **POST** /apis/validate | Check Given API Context Name already Exists
[**validateAsyncAPISpecification**](ValidationApi.md#validateAsyncAPISpecification) | **POST** /apis/validate-asyncapi | Validate an AsyncAPI Specification
[**validateEndpoint**](ValidationApi.md#validateEndpoint) | **POST** /apis/validate-endpoint | Check Whether Given Endpoint URL is Valid
[**validateGraphQLSchema**](ValidationApi.md#validateGraphQLSchema) | **POST** /apis/validate-graphql-schema | Validate a GraphQL SDL
[**validateOpenAPIDefinition**](ValidationApi.md#validateOpenAPIDefinition) | **POST** /apis/validate-openapi | Validate an OpenAPI Definition
[**validateWSDLDefinition**](ValidationApi.md#validateWSDLDefinition) | **POST** /apis/validate-wsdl | Validate a WSDL Definition


<a name="validateAPI"></a>
# **validateAPI**
> validateAPI(query, ifNoneMatch)

Check Given API Context Name already Exists

Using this operation, you can check a given API context is already used. You need to provide the context name you want to check. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ValidationApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ValidationApi apiInstance = new ValidationApi(defaultClient);
    String query = "query_example"; // String | **Search condition**.  You can search in attributes by using an **\"<attribute>:\"** modifier.  Eg. \"name:wso2\" will match an API if the provider of the API is exactly \"wso2\".  Supported attribute modifiers are [** version, context, name **]  If no advanced attribute modifier has been specified, search will match the given query string against API Name. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      apiInstance.validateAPI(query, ifNoneMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling ValidationApi#validateAPI");
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
 **query** | **String**| **Search condition**.  You can search in attributes by using an **\&quot;&lt;attribute&gt;:\&quot;** modifier.  Eg. \&quot;name:wso2\&quot; will match an API if the provider of the API is exactly \&quot;wso2\&quot;.  Supported attribute modifiers are [** version, context, name **]  If no advanced attribute modifier has been specified, search will match the given query string against API Name.  |
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. API definition validation information is returned  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="validateAsyncAPISpecification"></a>
# **validateAsyncAPISpecification**
> AsyncAPISpecificationValidationResponseDTO validateAsyncAPISpecification(returnContent, url, file)

Validate an AsyncAPI Specification

This operation can be used to validate and AsyncAPI Specification and retrieve a summary. Provide either &#39;url&#39; or &#39;file&#39; to specify the definition.

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ValidationApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ValidationApi apiInstance = new ValidationApi(defaultClient);
    Boolean returnContent = false; // Boolean | Specify whether to return the full content of the AsyncAPI specification in the response. This is only applicable when using url based validation
    String url = "url_example"; // String | AsyncAPI definition url
    File file = new File("/path/to/file"); // File | AsyncAPI definition as a file
    try {
      AsyncAPISpecificationValidationResponseDTO result = apiInstance.validateAsyncAPISpecification(returnContent, url, file);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ValidationApi#validateAsyncAPISpecification");
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
 **returnContent** | **Boolean**| Specify whether to return the full content of the AsyncAPI specification in the response. This is only applicable when using url based validation | [optional] [default to false]
 **url** | **String**| AsyncAPI definition url | [optional]
 **file** | **File**| AsyncAPI definition as a file | [optional]

### Return type

[**AsyncAPISpecificationValidationResponseDTO**](AsyncAPISpecificationValidationResponseDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. API definition validation information is returned |  * Content-Type - The content type of the body. <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="validateEndpoint"></a>
# **validateEndpoint**
> ApiEndpointValidationResponseDTO validateEndpoint(endpointUrl, apiId)

Check Whether Given Endpoint URL is Valid

Using this operation, it is possible check whether the given API endpoint url is a valid url 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ValidationApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ValidationApi apiInstance = new ValidationApi(defaultClient);
    String endpointUrl = "endpointUrl_example"; // String | API endpoint url
    String apiId = "apiId_example"; // String | 
    try {
      ApiEndpointValidationResponseDTO result = apiInstance.validateEndpoint(endpointUrl, apiId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ValidationApi#validateEndpoint");
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
 **endpointUrl** | **String**| API endpoint url |
 **apiId** | **String**|  | [optional]

### Return type

[**ApiEndpointValidationResponseDTO**](ApiEndpointValidationResponseDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. API definition validation information is returned  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="validateGraphQLSchema"></a>
# **validateGraphQLSchema**
> GraphQLValidationResponseDTO validateGraphQLSchema(useIntrospection, file, url)

Validate a GraphQL SDL

This operation can be used to validate a graphQL definition and retrieve a summary. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ValidationApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ValidationApi apiInstance = new ValidationApi(defaultClient);
    Boolean useIntrospection = false; // Boolean | Specify whether to use Introspection to obtain the GraphQL Schema 
    File file = new File("/path/to/file"); // File | Definition to upload as a file
    String url = "url_example"; // String | Definition to upload using url
    try {
      GraphQLValidationResponseDTO result = apiInstance.validateGraphQLSchema(useIntrospection, file, url);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ValidationApi#validateGraphQLSchema");
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
 **useIntrospection** | **Boolean**| Specify whether to use Introspection to obtain the GraphQL Schema  | [optional] [default to false]
 **file** | **File**| Definition to upload as a file | [optional]
 **url** | **String**| Definition to upload using url | [optional]

### Return type

[**GraphQLValidationResponseDTO**](GraphQLValidationResponseDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. API definition validation information is returned  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="validateOpenAPIDefinition"></a>
# **validateOpenAPIDefinition**
> OpenAPIDefinitionValidationResponseDTO validateOpenAPIDefinition(returnContent, url, file, inlineAPIDefinition)

Validate an OpenAPI Definition

This operation can be used to validate an OpenAPI definition and retrieve a summary. Provide either &#x60;url&#x60; or &#x60;file&#x60; to specify the definition. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ValidationApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ValidationApi apiInstance = new ValidationApi(defaultClient);
    Boolean returnContent = false; // Boolean | Specify whether to return the full content of the OpenAPI definition in the response. This is only applicable when using url based validation 
    String url = "url_example"; // String | OpenAPI definition url
    File file = new File("/path/to/file"); // File | OpenAPI definition as a file
    String inlineAPIDefinition = "inlineAPIDefinition_example"; // String | Inline content of the OpenAPI definition
    try {
      OpenAPIDefinitionValidationResponseDTO result = apiInstance.validateOpenAPIDefinition(returnContent, url, file, inlineAPIDefinition);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ValidationApi#validateOpenAPIDefinition");
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
 **returnContent** | **Boolean**| Specify whether to return the full content of the OpenAPI definition in the response. This is only applicable when using url based validation  | [optional] [default to false]
 **url** | **String**| OpenAPI definition url | [optional]
 **file** | **File**| OpenAPI definition as a file | [optional]
 **inlineAPIDefinition** | **String**| Inline content of the OpenAPI definition | [optional]

### Return type

[**OpenAPIDefinitionValidationResponseDTO**](OpenAPIDefinitionValidationResponseDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. API definition validation information is returned  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="validateWSDLDefinition"></a>
# **validateWSDLDefinition**
> WSDLValidationResponseDTO validateWSDLDefinition(url, file)

Validate a WSDL Definition

This operation can be used to validate a WSDL definition and retrieve a summary. Provide either &#x60;url&#x60; or &#x60;file&#x60; to specify the definition. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ValidationApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ValidationApi apiInstance = new ValidationApi(defaultClient);
    String url = "url_example"; // String | Definition url
    File file = new File("/path/to/file"); // File | Definition to upload as a file
    try {
      WSDLValidationResponseDTO result = apiInstance.validateWSDLDefinition(url, file);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ValidationApi#validateWSDLDefinition");
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
 **url** | **String**| Definition url | [optional]
 **file** | **File**| Definition to upload as a file | [optional]

### Return type

[**WSDLValidationResponseDTO**](WSDLValidationResponseDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. API definition validation information is returned  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

