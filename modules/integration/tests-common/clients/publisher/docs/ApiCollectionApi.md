# ApiCollectionApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1.0*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisGet**](ApiCollectionApi.md#apisGet) | **GET** /apis | Retrieve/Search APIs 
[**apisHead**](ApiCollectionApi.md#apisHead) | **HEAD** /apis | Check given API attibute name is already exist 
[**apisImportDefinitionPost**](ApiCollectionApi.md#apisImportDefinitionPost) | **POST** /apis/import-definition | Import API Definition
[**apisValidateDefinitionPost**](ApiCollectionApi.md#apisValidateDefinitionPost) | **POST** /apis/validate-definition | Validate API definition and retrieve a summary
[**searchGet**](ApiCollectionApi.md#searchGet) | **GET** /search | Retrieve/Search APIs and API Documents by content 


<a name="apisGet"></a>
# **apisGet**
> APIListDTO apisGet(limit, offset, xWSO2Tenant, query, ifNoneMatch, expand, accept, tenantDomain)

Retrieve/Search APIs 

This operation provides you a list of available APIs qualifying under a given search condition.  Each retrieved API is represented with a minimal amount of attributes. If you want to get complete details of an API, you need to use **Get details of an API** operation. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiCollectionApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiCollectionApi apiInstance = new ApiCollectionApi();
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String query = "query_example"; // String | **Search condition**.  You can search in attributes by using an **\"<attribute>:\"** modifier.  Eg. \"provider:wso2\" will match an API if the provider of the API is exactly \"wso2\". \"status:PUBLISHED\" will match an API if the API is in PUBLISHED state. \"label:external\" will match an API if it contains a Microgateway label called \"external\".  Additionally you can use wildcards.  Eg. \"provider:wso2*\" will match an API if the provider of the API starts with \"wso2\".  Supported attribute modifiers are [**version, context, status, description, subcontext, doc, provider, label**]  If no advanced attribute modifier has been specified, search will match the given query string against API Name. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
Boolean expand = true; // Boolean | Defines whether the returned response should contain full details of API 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String tenantDomain = "tenantDomain_example"; // String | Tenant domain, whose APIs should be retrieved. If not specified, the logged in user's tenant domain will be considered for this. 
try {
    APIListDTO result = apiInstance.apisGet(limit, offset, xWSO2Tenant, query, ifNoneMatch, expand, accept, tenantDomain);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiCollectionApi#apisGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **query** | **String**| **Search condition**.  You can search in attributes by using an **\&quot;&lt;attribute&gt;:\&quot;** modifier.  Eg. \&quot;provider:wso2\&quot; will match an API if the provider of the API is exactly \&quot;wso2\&quot;. \&quot;status:PUBLISHED\&quot; will match an API if the API is in PUBLISHED state. \&quot;label:external\&quot; will match an API if it contains a Microgateway label called \&quot;external\&quot;.  Additionally you can use wildcards.  Eg. \&quot;provider:wso2*\&quot; will match an API if the provider of the API starts with \&quot;wso2\&quot;.  Supported attribute modifiers are [**version, context, status, description, subcontext, doc, provider, label**]  If no advanced attribute modifier has been specified, search will match the given query string against API Name.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]
 **expand** | **Boolean**| Defines whether the returned response should contain full details of API  | [optional]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **tenantDomain** | **String**| Tenant domain, whose APIs should be retrieved. If not specified, the logged in user&#39;s tenant domain will be considered for this.  | [optional]

### Return type

[**APIListDTO**](APIListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisHead"></a>
# **apisHead**
> apisHead(query, ifNoneMatch)

Check given API attibute name is already exist 

Using this operation, you can check a given API context is already used. You need to provide the context name you want to check. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiCollectionApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiCollectionApi apiInstance = new ApiCollectionApi();
String query = "query_example"; // String | **Search condition**.  You can search in attributes by using an **\"<attribute>:\"** modifier.  Eg. \"provider:wso2\" will match an API if the provider of the API is exactly \"wso2\".  Additionally you can use wildcards.  Eg. \"provider:wso2*\" will match an API if the provider of the API starts with \"wso2\".  Supported attribute modifiers are [**version, context, lifeCycleStatus, description, subcontext, doc, provider**]  If no advanced attribute modifier has been specified, search will match the given query string against API Name. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    apiInstance.apisHead(query, ifNoneMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiCollectionApi#apisHead");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **query** | **String**| **Search condition**.  You can search in attributes by using an **\&quot;&lt;attribute&gt;:\&quot;** modifier.  Eg. \&quot;provider:wso2\&quot; will match an API if the provider of the API is exactly \&quot;wso2\&quot;.  Additionally you can use wildcards.  Eg. \&quot;provider:wso2*\&quot; will match an API if the provider of the API starts with \&quot;wso2\&quot;.  Supported attribute modifiers are [**version, context, lifeCycleStatus, description, subcontext, doc, provider**]  If no advanced attribute modifier has been specified, search will match the given query string against API Name.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="apisImportDefinitionPost"></a>
# **apisImportDefinitionPost**
> APIDTO apisImportDefinitionPost(type, file, url, additionalProperties, implementationType, ifMatch)

Import API Definition

This operation can be used to create api from api definition.  API definition can be either Swagger or a WSDL  WSDL can be speficied as a single file or a ZIP archive with WSDLs and reference XSDs etc. When the type is WSDL, it is a **must** to specify additionalProperties with API&#39;s name, version, context and endpoints. See the example for additionalProperties. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiCollectionApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiCollectionApi apiInstance = new ApiCollectionApi();
String type = "SWAGGER"; // String | Definition type to upload
File file = new File("/path/to/file.txt"); // File | Definition to uploadas a file
String url = "url_example"; // String | Definition url
String additionalProperties = "additionalProperties_example"; // String | Additional attributes specified as a stringified JSON with API's schema
String implementationType = "soap"; // String | Currently this is only used when creating an API using a WSDL.  If 'SOAP' is specified, the API will be created with only one resource 'POST /' which is to be used for SOAP operations.  If 'HTTP_BINDING' is specified, the API will be created with resources using HTTP binding operations which are extracted from the WSDL. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    APIDTO result = apiInstance.apisImportDefinitionPost(type, file, url, additionalProperties, implementationType, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiCollectionApi#apisImportDefinitionPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **type** | **String**| Definition type to upload | [optional] [default to SWAGGER] [enum: SWAGGER, WSDL]
 **file** | **File**| Definition to uploadas a file | [optional]
 **url** | **String**| Definition url | [optional]
 **additionalProperties** | **String**| Additional attributes specified as a stringified JSON with API&#39;s schema | [optional]
 **implementationType** | **String**| Currently this is only used when creating an API using a WSDL.  If &#39;SOAP&#39; is specified, the API will be created with only one resource &#39;POST /&#39; which is to be used for SOAP operations.  If &#39;HTTP_BINDING&#39; is specified, the API will be created with resources using HTTP binding operations which are extracted from the WSDL.  | [optional] [default to soap] [enum: soap, httpBinding]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**APIDTO**](APIDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a name="apisValidateDefinitionPost"></a>
# **apisValidateDefinitionPost**
> APIDefinitionValidationResponseDTO apisValidateDefinitionPost(type, url, file)

Validate API definition and retrieve a summary

This operation can be used to validate a swagger or WSDL definition and retrieve a summary. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiCollectionApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiCollectionApi apiInstance = new ApiCollectionApi();
String type = "SWAGGER"; // String | Definition type to upload
String url = "url_example"; // String | Definition url
File file = new File("/path/to/file.txt"); // File | Definition to upload as a file
try {
    APIDefinitionValidationResponseDTO result = apiInstance.apisValidateDefinitionPost(type, url, file);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiCollectionApi#apisValidateDefinitionPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **type** | **String**| Definition type to upload | [default to SWAGGER] [enum: SWAGGER, WSDL]
 **url** | **String**| Definition url | [optional]
 **file** | **File**| Definition to upload as a file | [optional]

### Return type

[**APIDefinitionValidationResponseDTO**](APIDefinitionValidationResponseDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

<a name="searchGet"></a>
# **searchGet**
> SearchResultListDTO searchGet(limit, offset, query, ifNoneMatch)

Retrieve/Search APIs and API Documents by content 

This operation provides you a list of available APIs and API Documents qualifying the given keyword match. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiCollectionApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiCollectionApi apiInstance = new ApiCollectionApi();
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
String query = "query_example"; // String | **Search**.  You can search by proving a keyword. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
try {
    SearchResultListDTO result = apiInstance.searchGet(limit, offset, query, ifNoneMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiCollectionApi#searchGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **query** | **String**| **Search**.  You can search by proving a keyword.  | [optional]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]

### Return type

[**SearchResultListDTO**](SearchResultListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

