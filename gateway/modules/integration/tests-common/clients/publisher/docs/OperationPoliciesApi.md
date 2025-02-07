# OperationPoliciesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addCommonOperationPolicy**](OperationPoliciesApi.md#addCommonOperationPolicy) | **POST** /operation-policies | Add a new common operation policy
[**deleteCommonOperationPolicyByPolicyId**](OperationPoliciesApi.md#deleteCommonOperationPolicyByPolicyId) | **DELETE** /operation-policies/{operationPolicyId} | Delete a common operation policy
[**getAllCommonOperationPolicies**](OperationPoliciesApi.md#getAllCommonOperationPolicies) | **GET** /operation-policies | Get all common operation policies to all the APIs 
[**getCommonOperationPolicyByPolicyId**](OperationPoliciesApi.md#getCommonOperationPolicyByPolicyId) | **GET** /operation-policies/{operationPolicyId} | Get the details of a common operation policy by providing policy ID
[**getCommonOperationPolicyContentByPolicyId**](OperationPoliciesApi.md#getCommonOperationPolicyContentByPolicyId) | **GET** /operation-policies/{operationPolicyId}/content | Download a common operation policy


<a name="addCommonOperationPolicy"></a>
# **addCommonOperationPolicy**
> OperationPolicyDataDTO addCommonOperationPolicy(policySpecFile, synapsePolicyDefinitionFile, ccPolicyDefinitionFile)

Add a new common operation policy

This operation can be used to add a new common operation policy. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.OperationPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    OperationPoliciesApi apiInstance = new OperationPoliciesApi(defaultClient);
    File policySpecFile = new File("/path/to/file"); // File | Operation policy specification to upload
    File synapsePolicyDefinitionFile = new File("/path/to/file"); // File | Operation policy definition of synapse gateway to upload
    File ccPolicyDefinitionFile = new File("/path/to/file"); // File | Operation policy definition of choreo connect to upload
    try {
      OperationPolicyDataDTO result = apiInstance.addCommonOperationPolicy(policySpecFile, synapsePolicyDefinitionFile, ccPolicyDefinitionFile);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling OperationPoliciesApi#addCommonOperationPolicy");
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
 **policySpecFile** | **File**| Operation policy specification to upload | [optional]
 **synapsePolicyDefinitionFile** | **File**| Operation policy definition of synapse gateway to upload | [optional]
 **ccPolicyDefinitionFile** | **File**| Operation policy definition of choreo connect to upload | [optional]

### Return type

[**OperationPolicyDataDTO**](OperationPolicyDataDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | OK. Shared operation policy uploaded  |  * Location - The URL of the uploaded common operation policy of the API.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="deleteCommonOperationPolicyByPolicyId"></a>
# **deleteCommonOperationPolicyByPolicyId**
> deleteCommonOperationPolicyByPolicyId(operationPolicyId)

Delete a common operation policy

This operation can be used to delete an existing common opreation policy by providing the Id of the policy. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.OperationPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    OperationPoliciesApi apiInstance = new OperationPoliciesApi(defaultClient);
    String operationPolicyId = "operationPolicyId_example"; // String | Operation policy Id 
    try {
      apiInstance.deleteCommonOperationPolicyByPolicyId(operationPolicyId);
    } catch (ApiException e) {
      System.err.println("Exception when calling OperationPoliciesApi#deleteCommonOperationPolicyByPolicyId");
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
 **operationPolicyId** | **String**| Operation policy Id  |

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
**500** | Internal Server Error. |  -  |

<a name="getAllCommonOperationPolicies"></a>
# **getAllCommonOperationPolicies**
> OperationPolicyDataListDTO getAllCommonOperationPolicies(limit, offset, query)

Get all common operation policies to all the APIs 

This operation provides you a list of all common operation policies that can be used by any API 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.OperationPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    OperationPoliciesApi apiInstance = new OperationPoliciesApi(defaultClient);
    Integer limit = 56; // Integer | Maximum size of policy array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String query = "query_example"; // String | **Search condition**.  You can search in attributes by using an **\"<attribute>:\"** modifier.  Eg. \"name:addHeader\" will match an API Policy if the provider of the API Policy contains \"addHeader\". \"version:\"v1\"\" will match an API Policy if the provider of the API Policy contains \"v1\".  Also you can use combined modifiers Eg. name:addHeader&version:v1 will match an API Policy if the name of the API Policy is addHeader and version is v1.  Supported attribute modifiers are [**version, name**]  If query attributes are provided, this returns all API policies available under the given limit.  Please note that you need to use encoded URL (URL encoding) if you are using a client which does not support URL encoding (such as curl) 
    try {
      OperationPolicyDataListDTO result = apiInstance.getAllCommonOperationPolicies(limit, offset, query);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling OperationPoliciesApi#getAllCommonOperationPolicies");
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
 **limit** | **Integer**| Maximum size of policy array to return.  | [optional]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]
 **query** | **String**| **Search condition**.  You can search in attributes by using an **\&quot;&lt;attribute&gt;:\&quot;** modifier.  Eg. \&quot;name:addHeader\&quot; will match an API Policy if the provider of the API Policy contains \&quot;addHeader\&quot;. \&quot;version:\&quot;v1\&quot;\&quot; will match an API Policy if the provider of the API Policy contains \&quot;v1\&quot;.  Also you can use combined modifiers Eg. name:addHeader&amp;version:v1 will match an API Policy if the name of the API Policy is addHeader and version is v1.  Supported attribute modifiers are [**version, name**]  If query attributes are provided, this returns all API policies available under the given limit.  Please note that you need to use encoded URL (URL encoding) if you are using a client which does not support URL encoding (such as curl)  | [optional]

### Return type

[**OperationPolicyDataListDTO**](OperationPolicyDataListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of qualifying policies is returned.  |  * Content-Type - The content type of the body. <br>  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getCommonOperationPolicyByPolicyId"></a>
# **getCommonOperationPolicyByPolicyId**
> OperationPolicyDataDTO getCommonOperationPolicyByPolicyId(operationPolicyId)

Get the details of a common operation policy by providing policy ID

This operation can be used to retrieve a particular common operation policy. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.OperationPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    OperationPoliciesApi apiInstance = new OperationPoliciesApi(defaultClient);
    String operationPolicyId = "operationPolicyId_example"; // String | Operation policy Id 
    try {
      OperationPolicyDataDTO result = apiInstance.getCommonOperationPolicyByPolicyId(operationPolicyId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling OperationPoliciesApi#getCommonOperationPolicyByPolicyId");
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
 **operationPolicyId** | **String**| Operation policy Id  |

### Return type

[**OperationPolicyDataDTO**](OperationPolicyDataDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Operation policy returned.  |  * Content-Type - The content type of the body.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |
**406** | Not Acceptable. The requested media type is not supported. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getCommonOperationPolicyContentByPolicyId"></a>
# **getCommonOperationPolicyContentByPolicyId**
> File getCommonOperationPolicyContentByPolicyId(operationPolicyId)

Download a common operation policy

This operation can be used to download a selected common operation policy. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.OperationPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    OperationPoliciesApi apiInstance = new OperationPoliciesApi(defaultClient);
    String operationPolicyId = "operationPolicyId_example"; // String | Operation policy Id 
    try {
      File result = apiInstance.getCommonOperationPolicyContentByPolicyId(operationPolicyId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling OperationPoliciesApi#getCommonOperationPolicyContentByPolicyId");
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
 **operationPolicyId** | **String**| Operation policy Id  |

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
**200** | OK. Operation policy returned.  |  * Content-Type - The content type of the body.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

