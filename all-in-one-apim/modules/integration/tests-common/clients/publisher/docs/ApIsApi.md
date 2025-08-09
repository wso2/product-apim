# ApIsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdAsyncapiGet**](ApIsApi.md#apisApiIdAsyncapiGet) | **GET** /apis/{apiId}/asyncapi | Get AsyncAPI definition
[**apisApiIdAsyncapiPut**](ApIsApi.md#apisApiIdAsyncapiPut) | **PUT** /apis/{apiId}/asyncapi | Update AsyncAPI definition
[**apisApiIdEnvironmentsEnvIdKeysGet**](ApIsApi.md#apisApiIdEnvironmentsEnvIdKeysGet) | **GET** /apis/{apiId}/environments/{envId}/keys | get environment specific API properties
[**apisApiIdEnvironmentsEnvIdKeysPut**](ApIsApi.md#apisApiIdEnvironmentsEnvIdKeysPut) | **PUT** /apis/{apiId}/environments/{envId}/keys | Update environment specific API properties
[**createAPI**](ApIsApi.md#createAPI) | **POST** /apis | Create a New API
[**createNewAPIVersion**](ApIsApi.md#createNewAPIVersion) | **POST** /apis/copy-api | Create a New API Version
[**deleteAPI**](ApIsApi.md#deleteAPI) | **DELETE** /apis/{apiId} | Delete an API
[**generateInternalAPIKey**](ApIsApi.md#generateInternalAPIKey) | **POST** /apis/{apiId}/generate-key | Generate internal API Key to invoke APIS.
[**generateMockScripts**](ApIsApi.md#generateMockScripts) | **POST** /apis/{apiId}/generate-mock-scripts | Generate Mock Response Payloads
[**getAPI**](ApIsApi.md#getAPI) | **GET** /apis/{apiId} | Get Details of an API
[**getAPIResourcePaths**](ApIsApi.md#getAPIResourcePaths) | **GET** /apis/{apiId}/resource-paths | Get Resource Paths of an API
[**getAPISubscriptionPolicies**](ApIsApi.md#getAPISubscriptionPolicies) | **GET** /apis/{apiId}/subscription-policies | Get Details of the Subscription Throttling Policies of an API 
[**getAPISwagger**](ApIsApi.md#getAPISwagger) | **GET** /apis/{apiId}/swagger | Get Swagger Definition
[**getAPIThumbnail**](ApIsApi.md#getAPIThumbnail) | **GET** /apis/{apiId}/thumbnail | Get Thumbnail Image
[**getAllAPIs**](ApIsApi.md#getAllAPIs) | **GET** /apis | Retrieve/Search APIs 
[**getGeneratedMockScriptsOfAPI**](ApIsApi.md#getGeneratedMockScriptsOfAPI) | **GET** /apis/{apiId}/generated-mock-scripts | Get Generated Mock Response Payloads
[**getSequenceBackendContent**](ApIsApi.md#getSequenceBackendContent) | **GET** /apis/{apiId}/sequence-backend/{type}/content | Get Sequence of Custom Backend
[**getSequenceBackendData**](ApIsApi.md#getSequenceBackendData) | **GET** /apis/{apiId}/sequence-backend | Get Sequence Backends of the API
[**getWSDLInfoOfAPI**](ApIsApi.md#getWSDLInfoOfAPI) | **GET** /apis/{apiId}/wsdl-info | Get WSDL Meta Information
[**getWSDLOfAPI**](ApIsApi.md#getWSDLOfAPI) | **GET** /apis/{apiId}/wsdl | Get WSDL definition
[**importAsyncAPISpecification**](ApIsApi.md#importAsyncAPISpecification) | **POST** /apis/import-asyncapi | import an AsyncAPI Specification
[**importGraphQLSchema**](ApIsApi.md#importGraphQLSchema) | **POST** /apis/import-graphql-schema | Import a GraphQL SDL
[**importOpenAPIDefinition**](ApIsApi.md#importOpenAPIDefinition) | **POST** /apis/import-openapi | Import an OpenAPI Definition
[**importServiceFromCatalog**](ApIsApi.md#importServiceFromCatalog) | **POST** /apis/import-service | Import a Service from Service Catalog
[**importWSDLDefinition**](ApIsApi.md#importWSDLDefinition) | **POST** /apis/import-wsdl | Import a WSDL Definition
[**reimportServiceFromCatalog**](ApIsApi.md#reimportServiceFromCatalog) | **PUT** /apis/{apiId}/reimport-service | Update the Service that is used to create the API
[**sequenceBackendDelete**](ApIsApi.md#sequenceBackendDelete) | **DELETE** /apis/{apiId}/sequence-backend/{type} | Delete Sequence Backend of the API
[**sequenceBackendUpdate**](ApIsApi.md#sequenceBackendUpdate) | **PUT** /apis/{apiId}/sequence-backend | Upload Sequence Sequence as the Endpoint of the API
[**updateAPI**](ApIsApi.md#updateAPI) | **PUT** /apis/{apiId} | Update an API
[**updateAPISwagger**](ApIsApi.md#updateAPISwagger) | **PUT** /apis/{apiId}/swagger | Update Swagger Definition
[**updateAPIThumbnail**](ApIsApi.md#updateAPIThumbnail) | **PUT** /apis/{apiId}/thumbnail | Upload a Thumbnail Image
[**updateTopics**](ApIsApi.md#updateTopics) | **PUT** /apis/{apiId}/topics | Update Topics
[**updateWSDLOfAPI**](ApIsApi.md#updateWSDLOfAPI) | **PUT** /apis/{apiId}/wsdl | Update WSDL Definition


<a name="apisApiIdAsyncapiGet"></a>
# **apisApiIdAsyncapiGet**
> String apisApiIdAsyncapiGet(apiId, ifNoneMatch)

Get AsyncAPI definition

This operation can be used to retrieve the AsyncAPI definition of an API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      String result = apiInstance.apisApiIdAsyncapiGet(apiId, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#apisApiIdAsyncapiGet");
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
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

**String**

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Requested AsyncAPI definition of the API is returned  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Willl&#x3D; be supported in future).  <br>  * Last-Modified - Date and time the resource has beed modified the last time. Used by caches, or in conditional request (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="apisApiIdAsyncapiPut"></a>
# **apisApiIdAsyncapiPut**
> String apisApiIdAsyncapiPut(apiId, ifMatch, apiDefinition, url, file)

Update AsyncAPI definition

This operation can be used to update the AsyncAPI definition of an existing API. AsyncAPI definition to be updated is passed as a form data parameter &#39;apiDefinition&#39;. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    String apiDefinition = "apiDefinition_example"; // String | AsyncAPI definition of the API
    String url = "url_example"; // String | AsyncAPI definition URL of the API
    File file = new File("/path/to/file"); // File | AsyncAPI definition as a file
    try {
      String result = apiInstance.apisApiIdAsyncapiPut(apiId, ifMatch, apiDefinition, url, file);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#apisApiIdAsyncapiPut");
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
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]
 **apiDefinition** | **String**| AsyncAPI definition of the API | [optional]
 **url** | **String**| AsyncAPI definition URL of the API | [optional]
 **file** | **File**| AsyncAPI definition as a file | [optional]

### Return type

**String**

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with updated AsyncAPI definition  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has beed modified the last time. Use &#x3D;d by caches, or in conditional requests (Will be supported in future).  <br>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="apisApiIdEnvironmentsEnvIdKeysGet"></a>
# **apisApiIdEnvironmentsEnvIdKeysGet**
> Map&lt;String, String&gt; apisApiIdEnvironmentsEnvIdKeysGet(apiId, envId)

get environment specific API properties

This operation can be used to retrieve environment specific API properties from an existing API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String envId = "envId_example"; // String | **Env ID** consisting of the **UUID** of the gateway environment. 
    try {
      Map<String, String> result = apiInstance.apisApiIdEnvironmentsEnvIdKeysGet(apiId, envId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#apisApiIdEnvironmentsEnvIdKeysGet");
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
 **envId** | **String**| **Env ID** consisting of the **UUID** of the gateway environment.  |

### Return type

**Map&lt;String, String&gt;**

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with environment specific API properties  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="apisApiIdEnvironmentsEnvIdKeysPut"></a>
# **apisApiIdEnvironmentsEnvIdKeysPut**
> Map&lt;String, String&gt; apisApiIdEnvironmentsEnvIdKeysPut(apiId, envId, requestBody)

Update environment specific API properties

This operation can be used to update the environment specific API properties of an existing API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String envId = "envId_example"; // String | **Env ID** consisting of the **UUID** of the gateway environment. 
    Map<String, String> requestBody = new HashMap(); // Map<String, String> | 
    try {
      Map<String, String> result = apiInstance.apisApiIdEnvironmentsEnvIdKeysPut(apiId, envId, requestBody);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#apisApiIdEnvironmentsEnvIdKeysPut");
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
 **envId** | **String**| **Env ID** consisting of the **UUID** of the gateway environment.  |
 **requestBody** | [**Map&lt;String, String&gt;**](String.md)|  |

### Return type

**Map&lt;String, String&gt;**

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with environment specific API properties  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="createAPI"></a>
# **createAPI**
> APIDTO createAPI(APIDTO, openAPIVersion)

Create a New API

This operation can be used to create a new API specifying the details of the API in the payload. The new API will be in &#x60;CREATED&#x60; state.  There is a special capability for a user who has &#x60;APIM Admin&#x60; permission such that he can create APIs on behalf of other users. For that he can to specify &#x60;\&quot;provider\&quot; : \&quot;some_other_user\&quot;&#x60; in the payload so that the API&#39;s creator will be shown as &#x60;some_other_user&#x60; in the UI. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    APIDTO APIDTO = new APIDTO(); // APIDTO | API object that needs to be added
    String openAPIVersion = "v3"; // String | Open api version
    try {
      APIDTO result = apiInstance.createAPI(APIDTO, openAPIVersion);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#createAPI");
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
 **APIDTO** | [**APIDTO**](APIDTO.md)| API object that needs to be added |
 **openAPIVersion** | **String**| Open api version | [optional] [default to v3] [enum: v2, v3]

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created object as entity in the body. Location header contains URL of newly created entity.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |

<a name="createNewAPIVersion"></a>
# **createNewAPIVersion**
> APIDTO createNewAPIVersion(newVersion, apiId, defaultVersion, serviceVersion)

Create a New API Version

This operation can be used to create a new version of an existing API. The new version is specified as &#x60;newVersion&#x60; query parameter. New API will be in &#x60;CREATED&#x60; state. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String newVersion = "newVersion_example"; // String | Version of the new API.
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**. 
    Boolean defaultVersion = false; // Boolean | Specifies whether new API should be added as default version.
    String serviceVersion = "serviceVersion_example"; // String | Version of the Service that will used in creating new version
    try {
      APIDTO result = apiInstance.createNewAPIVersion(newVersion, apiId, defaultVersion, serviceVersion);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#createNewAPIVersion");
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
 **newVersion** | **String**| Version of the new API. |
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  |
 **defaultVersion** | **Boolean**| Specifies whether new API should be added as default version. | [optional] [default to false]
 **serviceVersion** | **String**| Version of the Service that will used in creating new version | [optional]

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created API as entity in the body. Location header contains URL of newly created API.  |  * Location - The URL of the newly created API.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="deleteAPI"></a>
# **deleteAPI**
> deleteAPI(apiId, ifMatch)

Delete an API

This operation can be used to delete an existing API proving the Id of the API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      apiInstance.deleteAPI(apiId, ifMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#deleteAPI");
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
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

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
**200** | OK. Resource successfully deleted.  |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**409** | Conflict. Specified resource already exists. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="generateInternalAPIKey"></a>
# **generateInternalAPIKey**
> APIKeyDTO generateInternalAPIKey(apiId)

Generate internal API Key to invoke APIS.

This operation can be used to generate internal api key which used to invoke API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    try {
      APIKeyDTO result = apiInstance.generateInternalAPIKey(apiId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#generateInternalAPIKey");
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

[**APIKeyDTO**](APIKeyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. apikey generated.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="generateMockScripts"></a>
# **generateMockScripts**
> String generateMockScripts(apiId, ifNoneMatch)

Generate Mock Response Payloads

This operation can be used to generate mock responses from examples of swagger definition of an API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      String result = apiInstance.generateMockScripts(apiId, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#generateMockScripts");
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
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

**String**

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Requested swagger document of the API is returned with example responses  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getAPI"></a>
# **getAPI**
> APIDTO getAPI(apiId, xWSO2Tenant, ifNoneMatch)

Get Details of an API

Using this operation, you can retrieve complete details of a single API. You need to provide the Id of the API to retrive it. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      APIDTO result = apiInstance.getAPI(apiId, xWSO2Tenant, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#getAPI");
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
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Requested API is returned  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getAPIResourcePaths"></a>
# **getAPIResourcePaths**
> ResourcePathListDTO getAPIResourcePaths(apiId, limit, offset, ifNoneMatch)

Get Resource Paths of an API

This operation can be used to retrieve resource paths defined for a specific api. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      ResourcePathListDTO result = apiInstance.getAPIResourcePaths(apiId, limit, offset, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#getAPIResourcePaths");
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
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**ResourcePathListDTO**](ResourcePathListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. ResourcePaths returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modified the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getAPISubscriptionPolicies"></a>
# **getAPISubscriptionPolicies**
> ThrottlingPolicyDTO getAPISubscriptionPolicies(apiId, xWSO2Tenant, ifNoneMatch)

Get Details of the Subscription Throttling Policies of an API 

This operation can be used to retrieve details of the subscription throttling policy of an API by specifying the API Id.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrive API subscription throttling policies that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      ThrottlingPolicyDTO result = apiInstance.getAPISubscriptionPolicies(apiId, xWSO2Tenant, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#getAPISubscriptionPolicies");
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
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**ThrottlingPolicyDTO**](ThrottlingPolicyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Throttling Policy returned  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests.  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getAPISwagger"></a>
# **getAPISwagger**
> String getAPISwagger(apiId, ifNoneMatch)

Get Swagger Definition

This operation can be used to retrieve the swagger definition of an API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      String result = apiInstance.getAPISwagger(apiId, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#getAPISwagger");
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
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

**String**

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Requested swagger document of the API is returned  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getAPIThumbnail"></a>
# **getAPIThumbnail**
> getAPIThumbnail(apiId, ifNoneMatch)

Get Thumbnail Image

This operation can be used to download a thumbnail image of an API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      apiInstance.getAPIThumbnail(apiId, ifNoneMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#getAPIThumbnail");
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
**200** | OK. Thumbnail image returned  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getAllAPIs"></a>
# **getAllAPIs**
> APIListDTO getAllAPIs(limit, offset, sortBy, sortOrder, xWSO2Tenant, query, ifNoneMatch, accept)

Retrieve/Search APIs 

This operation provides you a list of available APIs qualifying under a given search condition.  Each retrieved API is represented with a minimal amount of attributes. If you want to get complete details of an API, you need to use **Get details of an API** operation. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String sortBy = "createdTime"; // String | Criteria for sorting. 
    String sortOrder = "desc"; // String | Order of sorting(ascending/descending). 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
    String query = "query_example"; // String | **Search condition**.  You can search in attributes by using an **\"<attribute>:\"** modifier.  Eg. \"provider:wso2\" will match an API if the provider of the API contains \"wso2\". \"provider:\"wso2\"\" will match an API if the provider of the API is exactly \"wso2\". \"status:PUBLISHED\" will match an API if the API is in PUBLISHED state.  Also you can use combined modifiers Eg. name:pizzashack version:v1 will match an API if the name of the API is pizzashack and version is v1.  Supported attribute modifiers are [**version, context, name, status, description, doc, provider**]  If no advanced attribute modifier has been specified,  the API names containing the search term will be returned as a result.  Please note that you need to use encoded URL (URL encoding) if you are using a client which does not support URL encoding (such as curl) 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    String accept = "\"application/json\""; // String | Media types acceptable for the response. Default is application/json. 
    try {
      APIListDTO result = apiInstance.getAllAPIs(limit, offset, sortBy, sortOrder, xWSO2Tenant, query, ifNoneMatch, accept);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#getAllAPIs");
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
 **sortBy** | **String**| Criteria for sorting.  | [optional] [default to createdTime] [enum: apiName, version, createdTime, status]
 **sortOrder** | **String**| Order of sorting(ascending/descending).  | [optional] [default to desc] [enum: asc, desc]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **query** | **String**| **Search condition**.  You can search in attributes by using an **\&quot;&lt;attribute&gt;:\&quot;** modifier.  Eg. \&quot;provider:wso2\&quot; will match an API if the provider of the API contains \&quot;wso2\&quot;. \&quot;provider:\&quot;wso2\&quot;\&quot; will match an API if the provider of the API is exactly \&quot;wso2\&quot;. \&quot;status:PUBLISHED\&quot; will match an API if the API is in PUBLISHED state.  Also you can use combined modifiers Eg. name:pizzashack version:v1 will match an API if the name of the API is pizzashack and version is v1.  Supported attribute modifiers are [**version, context, name, status, description, doc, provider**]  If no advanced attribute modifier has been specified,  the API names containing the search term will be returned as a result.  Please note that you need to use encoded URL (URL encoding) if you are using a client which does not support URL encoding (such as curl)  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to &quot;application/json&quot;]

### Return type

[**APIListDTO**](APIListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of qualifying APIs is returned.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body. <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getGeneratedMockScriptsOfAPI"></a>
# **getGeneratedMockScriptsOfAPI**
> MockResponsePayloadListDTO getGeneratedMockScriptsOfAPI(apiId, ifNoneMatch)

Get Generated Mock Response Payloads

This operation can be used to get generated mock responses from examples of swagger definition of an API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      MockResponsePayloadListDTO result = apiInstance.getGeneratedMockScriptsOfAPI(apiId, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#getGeneratedMockScriptsOfAPI");
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
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**MockResponsePayloadListDTO**](MockResponsePayloadListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Requested swagger document of the API is returned with example responses  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Content-Type - The content type of the body.  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getSequenceBackendContent"></a>
# **getSequenceBackendContent**
> File getSequenceBackendContent(type, apiId)

Get Sequence of Custom Backend

This operation can be used to get Sequence of the Custom Backend

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String type = "type_example"; // String | Type of the Endpoint. SANDBOX or PRODUCTION 
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    try {
      File result = apiInstance.getSequenceBackendContent(type, apiId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#getSequenceBackendContent");
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
 **type** | **String**| Type of the Endpoint. SANDBOX or PRODUCTION  |
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |

### Return type

[**File**](File.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/xml, application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Requested API Custom Backend is returned  |  * Content-Type - The content type of the body.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getSequenceBackendData"></a>
# **getSequenceBackendData**
> SequenceBackendListDTO getSequenceBackendData(apiId)

Get Sequence Backends of the API

This operation can be used to get Sequence Backend data of the API

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    try {
      SequenceBackendListDTO result = apiInstance.getSequenceBackendData(apiId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#getSequenceBackendData");
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

[**SequenceBackendListDTO**](SequenceBackendListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Requested API Sequence Backend is returned  |  * Content-Type - The content type of the body.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getWSDLInfoOfAPI"></a>
# **getWSDLInfoOfAPI**
> WSDLInfoDTO getWSDLInfoOfAPI(apiId)

Get WSDL Meta Information

This operation can be used to retrieve the WSDL meta information of an API. It states whether the API is a SOAP API. If the API is a SOAP API, it states whether it has a single WSDL or a WSDL archive. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    try {
      WSDLInfoDTO result = apiInstance.getWSDLInfoOfAPI(apiId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#getWSDLInfoOfAPI");
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

[**WSDLInfoDTO**](WSDLInfoDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Requested WSDL meta information of the API is returned  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="getWSDLOfAPI"></a>
# **getWSDLOfAPI**
> getWSDLOfAPI(apiId, ifNoneMatch)

Get WSDL definition

This operation can be used to retrieve the WSDL definition of an API. It can be either a single WSDL file or a WSDL archive.  The type of the WSDL of the API is indicated at the \&quot;wsdlInfo\&quot; element of the API payload definition. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
    try {
      apiInstance.getWSDLOfAPI(apiId, ifNoneMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#getWSDLOfAPI");
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
**200** | OK. Requested WSDL document of the API is returned  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  |
**304** | Not Modified. Empty body because the client has already the latest version of the requested resource (Will be supported in future).  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |

<a name="importAsyncAPISpecification"></a>
# **importAsyncAPISpecification**
> APIDTO importAsyncAPISpecification(file, url, additionalProperties)

import an AsyncAPI Specification

This operation can be used to create and API from the AsyncAPI Specification. Provide either &#39;url&#39; or &#39;file&#39; to specify the definition. Specify additionalProperties with **at least** API&#39;s name, version, context and endpointConfig.

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    File file = new File("/path/to/file"); // File | Definition to upload as a file
    String url = "url_example"; // String | Definition url
    String additionalProperties = "additionalProperties_example"; // String | Additional attributes specified as a stringified JSON with API's schema
    try {
      APIDTO result = apiInstance.importAsyncAPISpecification(file, url, additionalProperties);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#importAsyncAPISpecification");
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
 **file** | **File**| Definition to upload as a file | [optional]
 **url** | **String**| Definition url | [optional]
 **additionalProperties** | **String**| Additional attributes specified as a stringified JSON with API&#39;s schema | [optional]

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created object as entity in the body. Location header contains URL of newly created entity. |  * Etag - Entity Tag of the respons resource. Used by caches, or in conditional requests (Will be supported in the future). <br>  * Location - The URL of the newly created resource. <br>  * Content-type - The content type of the body. <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |

<a name="importGraphQLSchema"></a>
# **importGraphQLSchema**
> APIDTO importGraphQLSchema(ifMatch, type, file, url, schema, additionalProperties)

Import a GraphQL SDL

This operation can be used to create api from api definition.APIMgtDAOTest  API definition is GraphQL Schema 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    String type = "type_example"; // String | Definition type to upload
    File file = new File("/path/to/file"); // File | Definition to uploads a file
    String url = "url_example"; // String | Definition url
    String schema = "schema_example"; // String | Definition schema
    String additionalProperties = "additionalProperties_example"; // String | Additional attributes specified as a stringified JSON with API's schema
    try {
      APIDTO result = apiInstance.importGraphQLSchema(ifMatch, type, file, url, schema, additionalProperties);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#importGraphQLSchema");
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
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]
 **type** | **String**| Definition type to upload | [optional]
 **file** | **File**| Definition to uploads a file | [optional]
 **url** | **String**| Definition url | [optional]
 **schema** | **String**| Definition schema | [optional]
 **additionalProperties** | **String**| Additional attributes specified as a stringified JSON with API&#39;s schema | [optional]

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created object as entity in the body. Location header contains URL of newly created entity.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |

<a name="importOpenAPIDefinition"></a>
# **importOpenAPIDefinition**
> APIDTO importOpenAPIDefinition(file, url, additionalProperties, inlineAPIDefinition)

Import an OpenAPI Definition

This operation can be used to create an API from an OpenAPI definition. Provide either &#x60;url&#x60; or &#x60;file&#x60; to specify the definition.  Specify additionalProperties with **at least** API&#39;s name, version, context and endpointConfig. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    File file = new File("/path/to/file"); // File | Definition to upload as a file
    String url = "url_example"; // String | Definition url
    String additionalProperties = "additionalProperties_example"; // String | Additional attributes specified as a stringified JSON with API's schema
    String inlineAPIDefinition = "inlineAPIDefinition_example"; // String | Inline content of the OpenAPI definition
    try {
      APIDTO result = apiInstance.importOpenAPIDefinition(file, url, additionalProperties, inlineAPIDefinition);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#importOpenAPIDefinition");
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
 **file** | **File**| Definition to upload as a file | [optional]
 **url** | **String**| Definition url | [optional]
 **additionalProperties** | **String**| Additional attributes specified as a stringified JSON with API&#39;s schema | [optional]
 **inlineAPIDefinition** | **String**| Inline content of the OpenAPI definition | [optional]

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created object as entity in the body. Location header contains URL of newly created entity.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |

<a name="importServiceFromCatalog"></a>
# **importServiceFromCatalog**
> APIDTO importServiceFromCatalog(serviceKey, APIDTO)

Import a Service from Service Catalog

This operation can be used to create an API from a Service from Service Catalog

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String serviceKey = Pizzashack-1.0.0; // String | ID of service that should be imported from Service Catalog
    APIDTO APIDTO = new APIDTO(); // APIDTO | 
    try {
      APIDTO result = apiInstance.importServiceFromCatalog(serviceKey, APIDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#importServiceFromCatalog");
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
 **serviceKey** | **String**| ID of service that should be imported from Service Catalog |
 **APIDTO** | [**APIDTO**](APIDTO.md)|  | [optional]

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created object as entity in the body. Location header contains the URL of the newly created entity.  |  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="importWSDLDefinition"></a>
# **importWSDLDefinition**
> APIDTO importWSDLDefinition(file, url, additionalProperties, implementationType)

Import a WSDL Definition

This operation can be used to create an API using a WSDL definition. Provide either &#x60;url&#x60; or &#x60;file&#x60; to specify the definition.  WSDL can be speficied as a single file or a ZIP archive with WSDLs and reference XSDs etc. Specify additionalProperties with **at least** API&#39;s name, version, context and endpointConfig. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    File file = new File("/path/to/file"); // File | WSDL definition as a file or archive  **Sample cURL to Upload WSDL File**  curl -k -H \\\"Authorization: Bearer ae4eae22-3f65-387b-a171-d37eaa366fa8\\\" -F file=@api.wsdl -F additionalProperties=@data.json \\\"https://127.0.0.1:9443/api/am/publisher/v4/apis/import-wsdl\\\"  **Sample cURL to Upload WSDL Archive**  curl -k -H \\\"Authorization: Bearer ae4eae22-3f65-387b-a171-d37eaa366fa8\\\" -F file=\\\"@wsdl.zip;type=application/zip\\\" -F additionalProperties=@data.json \\\"https://127.0.0.1:9443/api/am/publisher/v4/apis/import-wsdl\\\" 
    String url = "url_example"; // String | WSDL Definition url
    String additionalProperties = "additionalProperties_example"; // String | Additional attributes specified as a stringified JSON with API's schema
    String implementationType = "SOAP"; // String | If 'SOAP' is specified, the API will be created with only one resource 'POST /_*' which is to be used for SOAP operations.  If 'HTTP_BINDING' is specified, the API will be created with resources using HTTP binding operations which are extracted from the WSDL. 
    try {
      APIDTO result = apiInstance.importWSDLDefinition(file, url, additionalProperties, implementationType);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#importWSDLDefinition");
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
 **file** | **File**| WSDL definition as a file or archive  **Sample cURL to Upload WSDL File**  curl -k -H \\\&quot;Authorization: Bearer ae4eae22-3f65-387b-a171-d37eaa366fa8\\\&quot; -F file&#x3D;@api.wsdl -F additionalProperties&#x3D;@data.json \\\&quot;https://127.0.0.1:9443/api/am/publisher/v4/apis/import-wsdl\\\&quot;  **Sample cURL to Upload WSDL Archive**  curl -k -H \\\&quot;Authorization: Bearer ae4eae22-3f65-387b-a171-d37eaa366fa8\\\&quot; -F file&#x3D;\\\&quot;@wsdl.zip;type&#x3D;application/zip\\\&quot; -F additionalProperties&#x3D;@data.json \\\&quot;https://127.0.0.1:9443/api/am/publisher/v4/apis/import-wsdl\\\&quot;  | [optional]
 **url** | **String**| WSDL Definition url | [optional]
 **additionalProperties** | **String**| Additional attributes specified as a stringified JSON with API&#39;s schema | [optional]
 **implementationType** | **String**| If &#39;SOAP&#39; is specified, the API will be created with only one resource &#39;POST /_*&#39; which is to be used for SOAP operations.  If &#39;HTTP_BINDING&#39; is specified, the API will be created with resources using HTTP binding operations which are extracted from the WSDL.  | [optional] [default to SOAP] [enum: SOAPTOREST, SOAP]

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created object as entity in the body. Location header contains URL of newly created entity.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |

<a name="reimportServiceFromCatalog"></a>
# **reimportServiceFromCatalog**
> APIDTO reimportServiceFromCatalog(apiId)

Update the Service that is used to create the API

This operation can be used to re-import the Service used to create the API

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    try {
      APIDTO result = apiInstance.reimportServiceFromCatalog(apiId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#reimportServiceFromCatalog");
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

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with updated API object  |  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="sequenceBackendDelete"></a>
# **sequenceBackendDelete**
> sequenceBackendDelete(type, apiId)

Delete Sequence Backend of the API

This operation can be used to remove the Sequence Backend of the API

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String type = "type_example"; // String | Type of the Endpoint. SANDBOX or PRODUCTION 
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    try {
      apiInstance.sequenceBackendDelete(type, apiId);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#sequenceBackendDelete");
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
 **type** | **String**| Type of the Endpoint. SANDBOX or PRODUCTION  |
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |

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
**200** | OK. Resource successfully deleted.  |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**409** | Conflict. Specified resource already exists. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="sequenceBackendUpdate"></a>
# **sequenceBackendUpdate**
> APIDTO sequenceBackendUpdate(apiId, sequence, type)

Upload Sequence Sequence as the Endpoint of the API

This operation can be used to change the endpoint of the API to Sequence Backend

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    File sequence = new File("/path/to/file"); // File | The sequence that needs to be uploaded.
    String type = "type_example"; // String | Type of the Endpoint
    try {
      APIDTO result = apiInstance.sequenceBackendUpdate(apiId, sequence, type);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#sequenceBackendUpdate");
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
 **sequence** | **File**| The sequence that needs to be uploaded. | [optional]
 **type** | **String**| Type of the Endpoint | [optional]

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with updated API object  |  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**409** | Conflict. Specified resource already exists. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="updateAPI"></a>
# **updateAPI**
> APIDTO updateAPI(apiId, APIDTO, ifMatch)

Update an API

This operation can be used to update an existing API. But the properties &#x60;name&#x60;, &#x60;version&#x60;, &#x60;context&#x60;, &#x60;provider&#x60;, &#x60;state&#x60; will not be changed by this operation. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    APIDTO APIDTO = new APIDTO(); // APIDTO | API object that needs to be added
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      APIDTO result = apiInstance.updateAPI(apiId, APIDTO, ifMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#updateAPI");
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
 **APIDTO** | [**APIDTO**](APIDTO.md)| API object that needs to be added |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with updated API object  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**409** | Conflict. Specified resource already exists. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="updateAPISwagger"></a>
# **updateAPISwagger**
> String updateAPISwagger(apiId, ifMatch, apiDefinition, url, file)

Update Swagger Definition

This operation can be used to update the swagger definition of an existing API. Swagger definition to be updated is passed as a form data parameter &#x60;apiDefinition&#x60;. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    String apiDefinition = "apiDefinition_example"; // String | Swagger definition of the API
    String url = "url_example"; // String | Swagger definition URL of the API
    File file = new File("/path/to/file"); // File | Swagger definitio as a file
    try {
      String result = apiInstance.updateAPISwagger(apiId, ifMatch, apiDefinition, url, file);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#updateAPISwagger");
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
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]
 **apiDefinition** | **String**| Swagger definition of the API | [optional]
 **url** | **String**| Swagger definition URL of the API | [optional]
 **file** | **File**| Swagger definitio as a file | [optional]

### Return type

**String**

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with updated Swagger definition  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="updateAPIThumbnail"></a>
# **updateAPIThumbnail**
> FileInfoDTO updateAPIThumbnail(apiId, file, ifMatch)

Upload a Thumbnail Image

This operation can be used to upload a thumbnail image of an API. The thumbnail to be uploaded should be given as a form data parameter &#x60;file&#x60;. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    File file = new File("/path/to/file"); // File | Image to upload
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      FileInfoDTO result = apiInstance.updateAPIThumbnail(apiId, file, ifMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#updateAPIThumbnail");
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
 **file** | **File**| Image to upload |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**FileInfoDTO**](FileInfoDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Image updated  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Location - The URL of the uploaded thumbnail image of the API.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="updateTopics"></a>
# **updateTopics**
> APIDTO updateTopics(apiId, topicListDTO, ifMatch)

Update Topics

This operation can be used to update topics of an existing API.

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    TopicListDTO topicListDTO = new TopicListDTO(); // TopicListDTO | API object that needs to be added
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      APIDTO result = apiInstance.updateTopics(apiId, topicListDTO, ifMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#updateTopics");
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
 **topicListDTO** | [**TopicListDTO**](TopicListDTO.md)| API object that needs to be added |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with updated API object  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |

<a name="updateWSDLOfAPI"></a>
# **updateWSDLOfAPI**
> updateWSDLOfAPI(apiId, ifMatch, file, url)

Update WSDL Definition

This operation can be used to update the WSDL definition of an existing API. WSDL to be updated can be passed as either \&quot;url\&quot; or \&quot;file\&quot;. Only one of \&quot;url\&quot; or \&quot;file\&quot; can be used at the same time. \&quot;file\&quot; can be specified as a single WSDL file or as a zip file which has a WSDL and its dependencies (eg: XSDs) 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApIsApi apiInstance = new ApIsApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    File file = new File("/path/to/file"); // File | WSDL file or archive to upload
    String url = "url_example"; // String | WSDL Definition url
    try {
      apiInstance.updateWSDLOfAPI(apiId, ifMatch, file, url);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApIsApi#updateWSDLOfAPI");
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
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]
 **file** | **File**| WSDL file or archive to upload | [optional]
 **url** | **String**| WSDL Definition url | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with updated WSDL definition  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**403** | Forbidden. The request must be conditional but no condition has been specified. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

