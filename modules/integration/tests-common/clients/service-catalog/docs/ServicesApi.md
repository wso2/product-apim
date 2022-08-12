# ServicesApi

All URIs are relative to *https://apis.wso2.com/api/service-catalog/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addService**](ServicesApi.md#addService) | **POST** /services | Add a new service to Service Catalog
[**deleteService**](ServicesApi.md#deleteService) | **DELETE** /services/{serviceId} | Delete a service
[**exportService**](ServicesApi.md#exportService) | **GET** /services/export | Export a service
[**getServiceById**](ServicesApi.md#getServiceById) | **GET** /services/{serviceId} | Get details of a service
[**getServiceDefinition**](ServicesApi.md#getServiceDefinition) | **GET** /services/{serviceId}/definition | Retrieve a service definition
[**getServiceUsage**](ServicesApi.md#getServiceUsage) | **GET** /services/{serviceId}/usage | Retrieve the API Info that use the given service
[**importService**](ServicesApi.md#importService) | **POST** /services/import | Import a service
[**searchServices**](ServicesApi.md#searchServices) | **GET** /services | Retrieve/search services
[**updateService**](ServicesApi.md#updateService) | **PUT** /services/{serviceId} | Update a service


<a name="addService"></a>
# **addService**
> ServiceDTO addService(serviceMetadata, definitionFile, inlineContent)

Add a new service to Service Catalog

Add a new service to the service catalog of the user&#39;s organization (or tenant) by specifying the details of the service along with its definition. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.service.catalog.api.ApiClient;
import org.wso2.am.integration.clients.service.catalog.api.ApiException;
import org.wso2.am.integration.clients.service.catalog.api.Configuration;
import org.wso2.am.integration.clients.service.catalog.api.auth.*;
import org.wso2.am.integration.clients.service.catalog.api.models.*;
import org.wso2.am.integration.clients.service.catalog.api.v1.ServicesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/service-catalog/v1");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ServicesApi apiInstance = new ServicesApi(defaultClient);
    ServiceDTO serviceMetadata = new ServiceDTO(); // ServiceDTO | 
    File definitionFile = new File("/path/to/file"); // File | 
    String inlineContent = "inlineContent_example"; // String | Inline content of the document
    try {
      ServiceDTO result = apiInstance.addService(serviceMetadata, definitionFile, inlineContent);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ServicesApi#addService");
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
 **serviceMetadata** | [**ServiceDTO**](ServiceDTO.md)|  |
 **definitionFile** | **File**|  | [optional]
 **inlineContent** | **String**| Inline content of the document | [optional]

### Return type

[**ServiceDTO**](ServiceDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created service as the response payload  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |
**500** | Internal Server Error. |  -  |

<a name="deleteService"></a>
# **deleteService**
> deleteService(serviceId)

Delete a service

Delete a service by providing the service id 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.service.catalog.api.ApiClient;
import org.wso2.am.integration.clients.service.catalog.api.ApiException;
import org.wso2.am.integration.clients.service.catalog.api.Configuration;
import org.wso2.am.integration.clients.service.catalog.api.auth.*;
import org.wso2.am.integration.clients.service.catalog.api.models.*;
import org.wso2.am.integration.clients.service.catalog.api.v1.ServicesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/service-catalog/v1");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ServicesApi apiInstance = new ServicesApi(defaultClient);
    String serviceId = "serviceId_example"; // String | uuid of the service
    try {
      apiInstance.deleteService(serviceId);
    } catch (ApiException e) {
      System.err.println("Exception when calling ServicesApi#deleteService");
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
 **serviceId** | **String**| uuid of the service |

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
**204** | Successfully deleted the catalog entry.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**409** | Conflict. Specified resource already exists. |  -  |
**500** | Internal Server Error. |  -  |

<a name="exportService"></a>
# **exportService**
> File exportService(name, version)

Export a service

Export a service as an archived zip file. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.service.catalog.api.ApiClient;
import org.wso2.am.integration.clients.service.catalog.api.ApiException;
import org.wso2.am.integration.clients.service.catalog.api.Configuration;
import org.wso2.am.integration.clients.service.catalog.api.auth.*;
import org.wso2.am.integration.clients.service.catalog.api.models.*;
import org.wso2.am.integration.clients.service.catalog.api.v1.ServicesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/service-catalog/v1");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ServicesApi apiInstance = new ServicesApi(defaultClient);
    String name = "name_example"; // String | Name of the service to export 
    String version = "version_example"; // String | Version of the service to export 
    try {
      File result = apiInstance.exportService(name, version);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ServicesApi#exportService");
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
 **name** | **String**| Name of the service to export  |
 **version** | **String**| Version of the service to export  |

### Return type

[**File**](File.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/zip, application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful response as the exported service as a zipped archive.  |  * ETag -  <br>  |
**401** | Unauthorized. The user is not authorized. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getServiceById"></a>
# **getServiceById**
> ServiceDTO getServiceById(serviceId)

Get details of a service

Get details of a service using the id of the service. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.service.catalog.api.ApiClient;
import org.wso2.am.integration.clients.service.catalog.api.ApiException;
import org.wso2.am.integration.clients.service.catalog.api.Configuration;
import org.wso2.am.integration.clients.service.catalog.api.auth.*;
import org.wso2.am.integration.clients.service.catalog.api.models.*;
import org.wso2.am.integration.clients.service.catalog.api.v1.ServicesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/service-catalog/v1");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ServicesApi apiInstance = new ServicesApi(defaultClient);
    String serviceId = "serviceId_example"; // String | uuid of the service
    try {
      ServiceDTO result = apiInstance.getServiceById(serviceId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ServicesApi#getServiceById");
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
 **serviceId** | **String**| uuid of the service |

### Return type

[**ServiceDTO**](ServiceDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Requested service in the service catalog is returned.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getServiceDefinition"></a>
# **getServiceDefinition**
> String getServiceDefinition(serviceId)

Retrieve a service definition

Retrieve the definition of a service identified by the service id. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.service.catalog.api.ApiClient;
import org.wso2.am.integration.clients.service.catalog.api.ApiException;
import org.wso2.am.integration.clients.service.catalog.api.Configuration;
import org.wso2.am.integration.clients.service.catalog.api.auth.*;
import org.wso2.am.integration.clients.service.catalog.api.models.*;
import org.wso2.am.integration.clients.service.catalog.api.v1.ServicesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/service-catalog/v1");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ServicesApi apiInstance = new ServicesApi(defaultClient);
    String serviceId = "serviceId_example"; // String | uuid of the service
    try {
      String result = apiInstance.getServiceDefinition(serviceId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ServicesApi#getServiceDefinition");
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
 **serviceId** | **String**| uuid of the service |

### Return type

**String**

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/yaml

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful response with the definition file as entity in the body.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getServiceUsage"></a>
# **getServiceUsage**
> APIListDTO getServiceUsage(serviceId)

Retrieve the API Info that use the given service

Retrieve the id, name, context and version of the APIs that used by the service 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.service.catalog.api.ApiClient;
import org.wso2.am.integration.clients.service.catalog.api.ApiException;
import org.wso2.am.integration.clients.service.catalog.api.Configuration;
import org.wso2.am.integration.clients.service.catalog.api.auth.*;
import org.wso2.am.integration.clients.service.catalog.api.models.*;
import org.wso2.am.integration.clients.service.catalog.api.v1.ServicesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/service-catalog/v1");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ServicesApi apiInstance = new ServicesApi(defaultClient);
    String serviceId = "serviceId_example"; // String | uuid of the service
    try {
      APIListDTO result = apiInstance.getServiceUsage(serviceId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ServicesApi#getServiceUsage");
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
 **serviceId** | **String**| uuid of the service |

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
**200** | List of APIs that uses the service in the service catalog is returned.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="importService"></a>
# **importService**
> ServiceInfoListDTO importService(file, overwrite, verifier)

Import a service

Import  a service by providing an archived service 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.service.catalog.api.ApiClient;
import org.wso2.am.integration.clients.service.catalog.api.ApiException;
import org.wso2.am.integration.clients.service.catalog.api.Configuration;
import org.wso2.am.integration.clients.service.catalog.api.auth.*;
import org.wso2.am.integration.clients.service.catalog.api.models.*;
import org.wso2.am.integration.clients.service.catalog.api.v1.ServicesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/service-catalog/v1");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ServicesApi apiInstance = new ServicesApi(defaultClient);
    File file = new File("/path/to/file"); // File | Zip archive consisting of exported Application Configuration. 
    Boolean overwrite = false; // Boolean | Whether to overwrite if there is any existing service with the same name and version. 
    String verifier = "verifier_example"; // String | 
    try {
      ServiceInfoListDTO result = apiInstance.importService(file, overwrite, verifier);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ServicesApi#importService");
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
 **file** | **File**| Zip archive consisting of exported Application Configuration.  |
 **overwrite** | **Boolean**| Whether to overwrite if there is any existing service with the same name and version.  | [optional] [default to false]
 **verifier** | **String**|  | [optional]

### Return type

[**ServiceInfoListDTO**](ServiceInfoListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful response with the imported service metadata.  |  * ETag -  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**500** | Internal Server Error. |  -  |

<a name="searchServices"></a>
# **searchServices**
> ServiceListDTO searchServices(name, version, definitionType, key, shrink, sortBy, sortOrder, limit, offset)

Retrieve/search services

Retrieve or search services in the service catalog of the user&#39;s organization or tenant. Search is supported using the name, version, definitionType and key of the service. Search based on the definition type and key of the service will always be an exact search. If you want to execute an exact search for either name or version the parameter should be given inside double quotation. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.service.catalog.api.ApiClient;
import org.wso2.am.integration.clients.service.catalog.api.ApiException;
import org.wso2.am.integration.clients.service.catalog.api.Configuration;
import org.wso2.am.integration.clients.service.catalog.api.auth.*;
import org.wso2.am.integration.clients.service.catalog.api.models.*;
import org.wso2.am.integration.clients.service.catalog.api.v1.ServicesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/service-catalog/v1");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ServicesApi apiInstance = new ServicesApi(defaultClient);
    String name = "name_example"; // String | Filter services by the name of the service 
    String version = "version_example"; // String | Filter services by version of the service 
    String definitionType = "definitionType_example"; // String | Filter services by definitionType 
    String key = "key_example"; // String | Comma seperated keys of the services to check 
    Boolean shrink = false; // Boolean | If this set to true, a minimal set of fields will be provided for each service including the md5 
    String sortBy = "sortBy_example"; // String | 
    String sortOrder = "sortOrder_example"; // String | 
    Integer limit = 25; // Integer | Maximum limit of items to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    try {
      ServiceListDTO result = apiInstance.searchServices(name, version, definitionType, key, shrink, sortBy, sortOrder, limit, offset);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ServicesApi#searchServices");
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
 **name** | **String**| Filter services by the name of the service  | [optional]
 **version** | **String**| Filter services by version of the service  | [optional]
 **definitionType** | **String**| Filter services by definitionType  | [optional] [enum: OAS, WSDL1, WSDL2, GRAPHQL_SDL, ASYNC_API]
 **key** | **String**| Comma seperated keys of the services to check  | [optional]
 **shrink** | **Boolean**| If this set to true, a minimal set of fields will be provided for each service including the md5  | [optional] [default to false]
 **sortBy** | **String**|  | [optional] [enum: name, definitionType]
 **sortOrder** | **String**|  | [optional] [enum: asc, desc]
 **limit** | **Integer**| Maximum limit of items to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]

### Return type

[**ServiceListDTO**](ServiceListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Paginated matched list of services returned.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**500** | Internal Server Error. |  -  |

<a name="updateService"></a>
# **updateService**
> ServiceDTO updateService(serviceId, serviceMetadata, definitionFile, inlineContent)

Update a service

Update a service&#39;s details and definition 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.service.catalog.api.ApiClient;
import org.wso2.am.integration.clients.service.catalog.api.ApiException;
import org.wso2.am.integration.clients.service.catalog.api.Configuration;
import org.wso2.am.integration.clients.service.catalog.api.auth.*;
import org.wso2.am.integration.clients.service.catalog.api.models.*;
import org.wso2.am.integration.clients.service.catalog.api.v1.ServicesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/service-catalog/v1");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ServicesApi apiInstance = new ServicesApi(defaultClient);
    String serviceId = "serviceId_example"; // String | uuid of the service
    ServiceDTO serviceMetadata = new ServiceDTO(); // ServiceDTO | 
    File definitionFile = new File("/path/to/file"); // File | 
    String inlineContent = "inlineContent_example"; // String | Inline content of the document
    try {
      ServiceDTO result = apiInstance.updateService(serviceId, serviceMetadata, definitionFile, inlineContent);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ServicesApi#updateService");
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
 **serviceId** | **String**| uuid of the service |
 **serviceMetadata** | [**ServiceDTO**](ServiceDTO.md)|  |
 **definitionFile** | **File**|  | [optional]
 **inlineContent** | **String**| Inline content of the document | [optional]

### Return type

[**ServiceDTO**](ServiceDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Updated. Successful response with the newly updated service as entity in the body.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

