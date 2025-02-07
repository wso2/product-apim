# GatewayPoliciesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addGatewayPoliciesToFlows**](GatewayPoliciesApi.md#addGatewayPoliciesToFlows) | **POST** /gateway-policies | Engage gateway policies to the request, response, fault flows
[**deleteGatewayPolicyByPolicyId**](GatewayPoliciesApi.md#deleteGatewayPolicyByPolicyId) | **DELETE** /gateway-policies/{gatewayPolicyMappingId} | Delete a gateway policy mapping
[**engageGlobalPolicy**](GatewayPoliciesApi.md#engageGlobalPolicy) | **POST** /gateway-policies/{gatewayPolicyMappingId}/deploy | Engage gateway policy mapping to the gateways
[**getAllGatewayPolicies**](GatewayPoliciesApi.md#getAllGatewayPolicies) | **GET** /gateway-policies | Get all gateway policies mapping information 
[**getGatewayPolicyMappingContentByPolicyMappingId**](GatewayPoliciesApi.md#getGatewayPolicyMappingContentByPolicyMappingId) | **GET** /gateway-policies/{gatewayPolicyMappingId} | Retrieve information of a selected gateway policy mapping
[**updateGatewayPoliciesToFlows**](GatewayPoliciesApi.md#updateGatewayPoliciesToFlows) | **PUT** /gateway-policies/{gatewayPolicyMappingId} | Update gateway policies added to the request, response, fault flows


<a name="addGatewayPoliciesToFlows"></a>
# **addGatewayPoliciesToFlows**
> GatewayPolicyMappingInfoDTO addGatewayPoliciesToFlows(gatewayPolicyMappingsDTO)

Engage gateway policies to the request, response, fault flows

This operation can be used to apply gateway policies to the request, response, fault flows. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.GatewayPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    GatewayPoliciesApi apiInstance = new GatewayPoliciesApi(defaultClient);
    GatewayPolicyMappingsDTO gatewayPolicyMappingsDTO = new GatewayPolicyMappingsDTO(); // GatewayPolicyMappingsDTO | Policy details object that needs to be added.
    try {
      GatewayPolicyMappingInfoDTO result = apiInstance.addGatewayPoliciesToFlows(gatewayPolicyMappingsDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling GatewayPoliciesApi#addGatewayPoliciesToFlows");
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
 **gatewayPolicyMappingsDTO** | [**GatewayPolicyMappingsDTO**](GatewayPolicyMappingsDTO.md)| Policy details object that needs to be added. |

### Return type

[**GatewayPolicyMappingInfoDTO**](GatewayPolicyMappingInfoDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | OK. Policy mapping created successfully.  |  * Location - The URL of the created gateway policy mapping.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="deleteGatewayPolicyByPolicyId"></a>
# **deleteGatewayPolicyByPolicyId**
> deleteGatewayPolicyByPolicyId(gatewayPolicyMappingId)

Delete a gateway policy mapping

This operation can be used to delete an existing gateway policy mapping by providing the Id of the policy mapping. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.GatewayPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    GatewayPoliciesApi apiInstance = new GatewayPoliciesApi(defaultClient);
    String gatewayPolicyMappingId = "gatewayPolicyMappingId_example"; // String | Gateway policy mapping Id 
    try {
      apiInstance.deleteGatewayPolicyByPolicyId(gatewayPolicyMappingId);
    } catch (ApiException e) {
      System.err.println("Exception when calling GatewayPoliciesApi#deleteGatewayPolicyByPolicyId");
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
 **gatewayPolicyMappingId** | **String**| Gateway policy mapping Id  |

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
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |
**500** | Internal Server Error. |  -  |

<a name="engageGlobalPolicy"></a>
# **engageGlobalPolicy**
> List&lt;GatewayPolicyDeploymentDTO&gt; engageGlobalPolicy(gatewayPolicyMappingId, gatewayPolicyDeploymentDTO)

Engage gateway policy mapping to the gateways

This operation can be used to engage gateway policy mapping to the gateway/s. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.GatewayPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    GatewayPoliciesApi apiInstance = new GatewayPoliciesApi(defaultClient);
    String gatewayPolicyMappingId = "gatewayPolicyMappingId_example"; // String | Gateway policy mapping Id 
    List<GatewayPolicyDeploymentDTO> gatewayPolicyDeploymentDTO = Arrays.asList(); // List<GatewayPolicyDeploymentDTO> | Policy details object that needs to be added.
    try {
      List<GatewayPolicyDeploymentDTO> result = apiInstance.engageGlobalPolicy(gatewayPolicyMappingId, gatewayPolicyDeploymentDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling GatewayPoliciesApi#engageGlobalPolicy");
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
 **gatewayPolicyMappingId** | **String**| Gateway policy mapping Id  |
 **gatewayPolicyDeploymentDTO** | [**List&lt;GatewayPolicyDeploymentDTO&gt;**](GatewayPolicyDeploymentDTO.md)| Policy details object that needs to be added. |

### Return type

[**List&lt;GatewayPolicyDeploymentDTO&gt;**](GatewayPolicyDeploymentDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Gateway policy mapping engaged successfully.  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getAllGatewayPolicies"></a>
# **getAllGatewayPolicies**
> GatewayPolicyMappingDataListDTO getAllGatewayPolicies(limit, offset, query)

Get all gateway policies mapping information 

This operation provides you a list of all gateway policies mapping information. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.GatewayPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    GatewayPoliciesApi apiInstance = new GatewayPoliciesApi(defaultClient);
    Integer limit = 56; // Integer | Maximum size of policy array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    String query = "query_example"; // String | **Search condition**.  You can search in attributes by using an **\"gatewayLabel:\"** modifier.  Eg. The entry \"gatewayLabel:gateway1\" will result in a match with a Gateway Policy Mapping only if the policy mapping is deployed on \"gateway1\".  If query attribute is provided, this returns the Gateway policy Mapping available under the given limit.  Please note that you need to use encoded URL (URL encoding) if you are using a client which does not support URL encoding (such as curl) 
    try {
      GatewayPolicyMappingDataListDTO result = apiInstance.getAllGatewayPolicies(limit, offset, query);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling GatewayPoliciesApi#getAllGatewayPolicies");
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
 **query** | **String**| **Search condition**.  You can search in attributes by using an **\&quot;gatewayLabel:\&quot;** modifier.  Eg. The entry \&quot;gatewayLabel:gateway1\&quot; will result in a match with a Gateway Policy Mapping only if the policy mapping is deployed on \&quot;gateway1\&quot;.  If query attribute is provided, this returns the Gateway policy Mapping available under the given limit.  Please note that you need to use encoded URL (URL encoding) if you are using a client which does not support URL encoding (such as curl)  | [optional]

### Return type

[**GatewayPolicyMappingDataListDTO**](GatewayPolicyMappingDataListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. List of gateway policies is returned.  |  * Content-Type - The content type of the body. <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getGatewayPolicyMappingContentByPolicyMappingId"></a>
# **getGatewayPolicyMappingContentByPolicyMappingId**
> GatewayPolicyMappingsDTO getGatewayPolicyMappingContentByPolicyMappingId(gatewayPolicyMappingId)

Retrieve information of a selected gateway policy mapping

This operation can be used to retrieve information of a selected gateway policy mapping. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.GatewayPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    GatewayPoliciesApi apiInstance = new GatewayPoliciesApi(defaultClient);
    String gatewayPolicyMappingId = "gatewayPolicyMappingId_example"; // String | Gateway policy mapping Id 
    try {
      GatewayPolicyMappingsDTO result = apiInstance.getGatewayPolicyMappingContentByPolicyMappingId(gatewayPolicyMappingId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling GatewayPoliciesApi#getGatewayPolicyMappingContentByPolicyMappingId");
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
 **gatewayPolicyMappingId** | **String**| Gateway policy mapping Id  |

### Return type

[**GatewayPolicyMappingsDTO**](GatewayPolicyMappingsDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Gateway policy mapping information returned.  |  * Content-Type - The content type of the body.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="updateGatewayPoliciesToFlows"></a>
# **updateGatewayPoliciesToFlows**
> GatewayPolicyMappingsDTO updateGatewayPoliciesToFlows(gatewayPolicyMappingId, gatewayPolicyMappingsDTO)

Update gateway policies added to the request, response, fault flows

This operation can be used to update already added gateway policies to the request, response, fault flows. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.GatewayPoliciesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    GatewayPoliciesApi apiInstance = new GatewayPoliciesApi(defaultClient);
    String gatewayPolicyMappingId = "gatewayPolicyMappingId_example"; // String | Gateway policy mapping Id 
    GatewayPolicyMappingsDTO gatewayPolicyMappingsDTO = new GatewayPolicyMappingsDTO(); // GatewayPolicyMappingsDTO | Policy details object that needs to be updated.
    try {
      GatewayPolicyMappingsDTO result = apiInstance.updateGatewayPoliciesToFlows(gatewayPolicyMappingId, gatewayPolicyMappingsDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling GatewayPoliciesApi#updateGatewayPoliciesToFlows");
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
 **gatewayPolicyMappingId** | **String**| Gateway policy mapping Id  |
 **gatewayPolicyMappingsDTO** | [**GatewayPolicyMappingsDTO**](GatewayPolicyMappingsDTO.md)| Policy details object that needs to be updated. |

### Return type

[**GatewayPolicyMappingsDTO**](GatewayPolicyMappingsDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Policy mapping updated successfully.  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

