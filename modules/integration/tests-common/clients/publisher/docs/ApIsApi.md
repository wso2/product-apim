# ApIsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdDelete**](ApIsApi.md#apisApiIdDelete) | **DELETE** /apis/{apiId} | Delete an API
[**apisApiIdGet**](ApIsApi.md#apisApiIdGet) | **GET** /apis/{apiId} | Get details of an API
[**apisApiIdPut**](ApIsApi.md#apisApiIdPut) | **PUT** /apis/{apiId} | Update an API
[**apisApiIdResourcePathsGet**](ApIsApi.md#apisApiIdResourcePathsGet) | **GET** /apis/{apiId}/resource-paths | Get Resource Paths of an API
[**apisApiIdSubscriptionPoliciesGet**](ApIsApi.md#apisApiIdSubscriptionPoliciesGet) | **GET** /apis/{apiId}/subscription-policies | Get details of the subscription throttling policies of an API 
[**apisApiIdSwaggerGet**](ApIsApi.md#apisApiIdSwaggerGet) | **GET** /apis/{apiId}/swagger | Get swagger definition
[**apisApiIdSwaggerPut**](ApIsApi.md#apisApiIdSwaggerPut) | **PUT** /apis/{apiId}/swagger | Update swagger definition
[**apisApiIdThumbnailGet**](ApIsApi.md#apisApiIdThumbnailGet) | **GET** /apis/{apiId}/thumbnail | Get thumbnail image
[**apisCopyApiPost**](ApIsApi.md#apisCopyApiPost) | **POST** /apis/copy-api | Create a new API version
[**apisGet**](ApIsApi.md#apisGet) | **GET** /apis | Retrieve/Search APIs 
[**apisImportGraphqlSchemaPost**](ApIsApi.md#apisImportGraphqlSchemaPost) | **POST** /apis/import-graphql-schema | Import API Definition
[**apisPost**](ApIsApi.md#apisPost) | **POST** /apis | Create a new API
[**generateMockScripts**](ApIsApi.md#generateMockScripts) | **POST** /apis/{apiId}/generate-mock-scripts | Generate mock response payloads
[**getGeneratedMockScriptsOfAPI**](ApIsApi.md#getGeneratedMockScriptsOfAPI) | **GET** /apis/{apiId}/generated-mock-scripts | Generate mock response payloads
[**getWSDLInfoOfAPI**](ApIsApi.md#getWSDLInfoOfAPI) | **GET** /apis/{apiId}/wsdl-info | Get WSDL definition
[**getWSDLOfAPI**](ApIsApi.md#getWSDLOfAPI) | **GET** /apis/{apiId}/wsdl | Get WSDL definition
[**importOpenAPIDefinition**](ApIsApi.md#importOpenAPIDefinition) | **POST** /apis/import-openapi | Import an OpenAPI Definition
[**importWSDLDefinition**](ApIsApi.md#importWSDLDefinition) | **POST** /apis/import-wsdl | Import a WSDL Definition
[**updateAPIThumbnail**](ApIsApi.md#updateAPIThumbnail) | **PUT** /apis/{apiId}/thumbnail | Upload a thumbnail image
[**updateWSDLOfAPI**](ApIsApi.md#updateWSDLOfAPI) | **PUT** /apis/{apiId}/wsdl | Update WSDL definition


<a name="apisApiIdDelete"></a>
# **apisApiIdDelete**
> apisApiIdDelete(apiId, ifMatch)

Delete an API

This operation can be used to delete an existing API proving the Id of the API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.apisApiIdDelete(apiId, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#apisApiIdDelete");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdGet"></a>
# **apisApiIdGet**
> APIDTO apisApiIdGet(apiId, xWSO2Tenant, ifNoneMatch)

Get details of an API

Using this operation, you can retrieve complete details of a single API. You need to provide the Id of the API to retrive it. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    APIDTO result = apiInstance.apisApiIdGet(apiId, xWSO2Tenant, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#apisApiIdGet");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdPut"></a>
# **apisApiIdPut**
> APIDTO apisApiIdPut(apiId, body, ifMatch)

Update an API

This operation can be used to update an existing API. But the properties &#x60;name&#x60;, &#x60;version&#x60;, &#x60;context&#x60;, &#x60;provider&#x60;, &#x60;state&#x60; will not be changed by this operation. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
APIDTO body = new APIDTO(); // APIDTO | API object that needs to be added 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    APIDTO result = apiInstance.apisApiIdPut(apiId, body, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#apisApiIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **body** | [**APIDTO**](APIDTO.md)| API object that needs to be added  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdResourcePathsGet"></a>
# **apisApiIdResourcePathsGet**
> ResourcePathListDTO apisApiIdResourcePathsGet(apiId, limit, offset, ifNoneMatch)

Get Resource Paths of an API

This operation can be used to retrieve resource paths defined for a specific api. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    ResourcePathListDTO result = apiInstance.apisApiIdResourcePathsGet(apiId, limit, offset, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#apisApiIdResourcePathsGet");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdSubscriptionPoliciesGet"></a>
# **apisApiIdSubscriptionPoliciesGet**
> ThrottlingPolicyDTO apisApiIdSubscriptionPoliciesGet(apiId, xWSO2Tenant, ifNoneMatch)

Get details of the subscription throttling policies of an API 

This operation can be used to retrieve details of the subscription throttling policy of an API by specifying the API Id.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrive API subscription throttling policies that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    ThrottlingPolicyDTO result = apiInstance.apisApiIdSubscriptionPoliciesGet(apiId, xWSO2Tenant, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#apisApiIdSubscriptionPoliciesGet");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdSwaggerGet"></a>
# **apisApiIdSwaggerGet**
> String apisApiIdSwaggerGet(apiId, ifNoneMatch)

Get swagger definition

This operation can be used to retrieve the swagger definition of an API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    String result = apiInstance.apisApiIdSwaggerGet(apiId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#apisApiIdSwaggerGet");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisApiIdSwaggerPut"></a>
# **apisApiIdSwaggerPut**
> String apisApiIdSwaggerPut(apiId, apiDefinition, url, file, ifMatch)

Update swagger definition

This operation can be used to update the swagger definition of an existing API. Swagger definition to be updated is passed as a form data parameter &#x60;apiDefinition&#x60;. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String apiDefinition = "apiDefinition_example"; // String | Swagger definition of the API
String url = "url_example"; // String | Swagger definition URL of the API
File file = new File("/path/to/file.txt"); // File | Swagger definitio as a file
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    String result = apiInstance.apisApiIdSwaggerPut(apiId, apiDefinition, url, file, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#apisApiIdSwaggerPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **apiDefinition** | **String**| Swagger definition of the API | [optional]
 **url** | **String**| Swagger definition URL of the API | [optional]
 **file** | **File**| Swagger definitio as a file | [optional]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

**String**

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a name="apisApiIdThumbnailGet"></a>
# **apisApiIdThumbnailGet**
> apisApiIdThumbnailGet(apiId, ifNoneMatch)

Get thumbnail image

This operation can be used to download a thumbnail image of an API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    apiInstance.apisApiIdThumbnailGet(apiId, ifNoneMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#apisApiIdThumbnailGet");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisCopyApiPost"></a>
# **apisCopyApiPost**
> APIDTO apisCopyApiPost(newVersion, apiId, defaultVersion)

Create a new API version

This operation can be used to create a new version of an existing API. The new version is specified as &#x60;newVersion&#x60; query parameter. New API will be in &#x60;CREATED&#x60; state. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
String newVersion = "newVersion_example"; // String | Version of the new API.
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**. 
Boolean defaultVersion = false; // Boolean | Specifies whether new API should be added as default version.
try {
    APIDTO result = apiInstance.apisCopyApiPost(newVersion, apiId, defaultVersion);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#apisCopyApiPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **newVersion** | **String**| Version of the new API. |
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. The combination of the provider of the API, name of the API and the version is also accepted as a valid API I. Should be formatted as **provider-name-version**.  |
 **defaultVersion** | **Boolean**| Specifies whether new API should be added as default version. | [optional] [default to false]

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisGet"></a>
# **apisGet**
> APIListDTO apisGet(limit, offset, xWSO2Tenant, query, ifNoneMatch, expand, accept)

Retrieve/Search APIs 

This operation provides you a list of available APIs qualifying under a given search condition.  Each retrieved API is represented with a minimal amount of attributes. If you want to get complete details of an API, you need to use **Get details of an API** operation. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String query = "query_example"; // String | **Search condition**.  You can search in attributes by using an **\"<attribute>:\"** modifier.  Eg. \"provider:wso2\" will match an API if the provider of the API contains \"wso2\". \"provider:\"wso2\"\" will match an API if the provider of the API is exactly \"wso2\". \"status:PUBLISHED\" will match an API if the API is in PUBLISHED state. \"label:external\" will match an API if it contains a Microgateway label called \"external\".  Also you can use combined modifiers Eg. name:pizzashack version:v1 will match an API if the name of the API is pizzashack and version is v1.  Supported attribute modifiers are [**version, context, name, status, description, subcontext, doc, provider, label**]  If no advanced attribute modifier has been specified,  the API names containing the search term will be returned as a result.  Please note that you need to use encoded URL (URL encoding) if you are using a client which does not support URL encoding (such as curl) 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
Boolean expand = true; // Boolean | Defines whether the returned response should contain full details of API 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
try {
    APIListDTO result = apiInstance.apisGet(limit, offset, xWSO2Tenant, query, ifNoneMatch, expand, accept);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#apisGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **query** | **String**| **Search condition**.  You can search in attributes by using an **\&quot;&lt;attribute&gt;:\&quot;** modifier.  Eg. \&quot;provider:wso2\&quot; will match an API if the provider of the API contains \&quot;wso2\&quot;. \&quot;provider:\&quot;wso2\&quot;\&quot; will match an API if the provider of the API is exactly \&quot;wso2\&quot;. \&quot;status:PUBLISHED\&quot; will match an API if the API is in PUBLISHED state. \&quot;label:external\&quot; will match an API if it contains a Microgateway label called \&quot;external\&quot;.  Also you can use combined modifiers Eg. name:pizzashack version:v1 will match an API if the name of the API is pizzashack and version is v1.  Supported attribute modifiers are [**version, context, name, status, description, subcontext, doc, provider, label**]  If no advanced attribute modifier has been specified,  the API names containing the search term will be returned as a result.  Please note that you need to use encoded URL (URL encoding) if you are using a client which does not support URL encoding (such as curl)  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]
 **expand** | **Boolean**| Defines whether the returned response should contain full details of API  | [optional]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]

### Return type

[**APIListDTO**](APIListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisImportGraphqlSchemaPost"></a>
# **apisImportGraphqlSchemaPost**
> APIDTO apisImportGraphqlSchemaPost(type, file, additionalProperties, ifMatch)

Import API Definition

This operation can be used to create api from api definition.APIMgtDAOTest  API definition is GraphQL Schema 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
String type = "type_example"; // String | Definition type to upload
File file = new File("/path/to/file.txt"); // File | Definition to uploads a file
String additionalProperties = "additionalProperties_example"; // String | Additional attributes specified as a stringified JSON with API's schema
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    APIDTO result = apiInstance.apisImportGraphqlSchemaPost(type, file, additionalProperties, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#apisImportGraphqlSchemaPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **type** | **String**| Definition type to upload | [optional]
 **file** | **File**| Definition to uploads a file | [optional]
 **additionalProperties** | **String**| Additional attributes specified as a stringified JSON with API&#39;s schema | [optional]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a name="apisPost"></a>
# **apisPost**
> APIDTO apisPost(body, openAPIVersion)

Create a new API

This operation can be used to create a new API specifying the details of the API in the payload. The new API will be in &#x60;CREATED&#x60; state.  There is a special capability for a user who has &#x60;APIM Admin&#x60; permission such that he can create APIs on behalf of other users. For that he can to specify &#x60;\&quot;provider\&quot; : \&quot;some_other_user\&quot;&#x60; in the payload so that the API&#39;s creator will be shown as &#x60;some_other_user&#x60; in the UI. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
APIDTO body = new APIDTO(); // APIDTO | API object that needs to be added 
String openAPIVersion = "openAPIVersion_example"; // String | Open api version
try {
    APIDTO result = apiInstance.apisPost(body, openAPIVersion);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#apisPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**APIDTO**](APIDTO.md)| API object that needs to be added  |
 **openAPIVersion** | **String**| Open api version | [optional] [enum: V2, V3]

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="generateMockScripts"></a>
# **generateMockScripts**
> String generateMockScripts(apiId, ifNoneMatch)

Generate mock response payloads

This operation can be used to generate mock responses from examples of swagger definition of an API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    String result = apiInstance.generateMockScripts(apiId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#generateMockScripts");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getGeneratedMockScriptsOfAPI"></a>
# **getGeneratedMockScriptsOfAPI**
> MockResponsePayloadListDTO getGeneratedMockScriptsOfAPI(apiId, ifNoneMatch)

Generate mock response payloads

This operation can be used to generate mock responses from examples of swagger definition of an API. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    MockResponsePayloadListDTO result = apiInstance.getGeneratedMockScriptsOfAPI(apiId, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#getGeneratedMockScriptsOfAPI");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getWSDLInfoOfAPI"></a>
# **getWSDLInfoOfAPI**
> WSDLInfoDTO getWSDLInfoOfAPI(apiId)

Get WSDL definition

This operation can be used to retrieve the WSDL meta information of an API. It states whether the API is a SOAP API. If the API is a SOAP API, it states whether it has a single WSDL or a WSDL archive. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
try {
    WSDLInfoDTO result = apiInstance.getWSDLInfoOfAPI(apiId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#getWSDLInfoOfAPI");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getWSDLOfAPI"></a>
# **getWSDLOfAPI**
> getWSDLOfAPI(apiId, ifNoneMatch)

Get WSDL definition

This operation can be used to retrieve the WSDL definition of an API. It can be either a single WSDL file or a WSDL archive.  The type of the WSDL of the API is indicated at the \&quot;wsdlInfo\&quot; element of the API payload definition. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    apiInstance.getWSDLOfAPI(apiId, ifNoneMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#getWSDLOfAPI");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json, application/wsdl, application/zip

<a name="importOpenAPIDefinition"></a>
# **importOpenAPIDefinition**
> APIDTO importOpenAPIDefinition(file, url, additionalProperties)

Import an OpenAPI Definition

This operation can be used to create an API from an OpenAPI definition. Provide either &#x60;url&#x60; or &#x60;file&#x60; to specify the definition.  Specify additionalProperties with **at least** API&#39;s name, version, context and endpointConfig. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
File file = new File("/path/to/file.txt"); // File | Definition to upload as a file
String url = "url_example"; // String | Definition url
String additionalProperties = "additionalProperties_example"; // String | Additional attributes specified as a stringified JSON with API's schema
try {
    APIDTO result = apiInstance.importOpenAPIDefinition(file, url, additionalProperties);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#importOpenAPIDefinition");
    e.printStackTrace();
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

<a name="importWSDLDefinition"></a>
# **importWSDLDefinition**
> APIDTO importWSDLDefinition(file, url, additionalProperties, implementationType)

Import a WSDL Definition

This operation can be used to create an API using a WSDL definition. Provide either &#x60;url&#x60; or &#x60;file&#x60; to specify the definition.  WSDL can be speficied as a single file or a ZIP archive with WSDLs and reference XSDs etc. Specify additionalProperties with **at least** API&#39;s name, version, context and endpointConfig. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
File file = new File("/path/to/file.txt"); // File | WSDL definition as a file
String url = "url_example"; // String | WSDL Definition url
String additionalProperties = "additionalProperties_example"; // String | Additional attributes specified as a stringified JSON with API's schema
String implementationType = "SOAP"; // String | If 'SOAP' is specified, the API will be created with only one resource 'POST /_*' which is to be used for SOAP operations.  If 'HTTP_BINDING' is specified, the API will be created with resources using HTTP binding operations which are extracted from the WSDL. 
try {
    APIDTO result = apiInstance.importWSDLDefinition(file, url, additionalProperties, implementationType);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#importWSDLDefinition");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file** | **File**| WSDL definition as a file | [optional]
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

<a name="updateAPIThumbnail"></a>
# **updateAPIThumbnail**
> FileInfoDTO updateAPIThumbnail(apiId, file, ifMatch)

Upload a thumbnail image

This operation can be used to upload a thumbnail image of an API. The thumbnail to be uploaded should be given as a form data parameter &#x60;file&#x60;. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
File file = new File("/path/to/file.txt"); // File | Image to upload
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    FileInfoDTO result = apiInstance.updateAPIThumbnail(apiId, file, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#updateAPIThumbnail");
    e.printStackTrace();
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

<a name="updateWSDLOfAPI"></a>
# **updateWSDLOfAPI**
> updateWSDLOfAPI(apiId, file, url, ifMatch)

Update WSDL definition

This operation can be used to update the WSDL definition of an existing API. WSDL to be updated can be passed as either \&quot;url\&quot; or \&quot;file\&quot;. Only one of \&quot;url\&quot; or \&quot;file\&quot; can be used at the same time. \&quot;file\&quot; can be specified as a single WSDL file or as a zip file which has a WSDL and its dependencies (eg: XSDs) 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApIsApi apiInstance = new ApIsApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
File file = new File("/path/to/file.txt"); // File | WSDL file or archive to upload
String url = "url_example"; // String | WSDL Definition url
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.updateWSDLOfAPI(apiId, file, url, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApIsApi#updateWSDLOfAPI");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **file** | **File**| WSDL file or archive to upload | [optional]
 **url** | **String**| WSDL Definition url | [optional]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

