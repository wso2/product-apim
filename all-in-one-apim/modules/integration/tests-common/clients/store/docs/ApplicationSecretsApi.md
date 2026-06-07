# ApplicationSecretsApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**generateConsumerSecret**](ApplicationSecretsApi.md#generateConsumerSecret) | **POST** /applications/{applicationId}/oauth-keys/{keyMappingId}/generate-secret | Generate a New Consumer Secret 
[**getConsumerSecrets**](ApplicationSecretsApi.md#getConsumerSecrets) | **GET** /applications/{applicationId}/oauth-keys/{keyMappingId}/secrets | Retrieve Consumer Secrets 
[**revokeConsumerSecret**](ApplicationSecretsApi.md#revokeConsumerSecret) | **POST** /applications/{applicationId}/oauth-keys/{keyMappingId}/revoke-secret | Revoke a Consumer Secret 


<a name="generateConsumerSecret"></a>
# **generateConsumerSecret**
> ConsumerSecretDTO generateConsumerSecret(applicationId, keyMappingId, consumerSecretCreationRequestDTO)

Generate a New Consumer Secret 

This operation can be used to generate a new consumer secret for an application for the given key type 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationSecretsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationSecretsApi apiInstance = new ApplicationSecretsApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyMappingId = "keyMappingId_example"; // String | OAuth Key Identifier consisting of the UUID of the Oauth Key Mapping. 
    ConsumerSecretCreationRequestDTO consumerSecretCreationRequestDTO = new ConsumerSecretCreationRequestDTO(); // ConsumerSecretCreationRequestDTO | Request payload containing details for creating a new consumer secret 
    try {
      ConsumerSecretDTO result = apiInstance.generateConsumerSecret(applicationId, keyMappingId, consumerSecretCreationRequestDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationSecretsApi#generateConsumerSecret");
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
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **keyMappingId** | **String**| OAuth Key Identifier consisting of the UUID of the Oauth Key Mapping.  |
 **consumerSecretCreationRequestDTO** | [**ConsumerSecretCreationRequestDTO**](ConsumerSecretCreationRequestDTO.md)| Request payload containing details for creating a new consumer secret  |

### Return type

[**ConsumerSecretDTO**](ConsumerSecretDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created object as entity in the body. Location header contains URL of newly created entity.  |  * Location - Location to the newly created secret entity.  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**403** | Forbidden. The user does not have permission to perform this action. |  -  |
**500** | Internal Server Error. |  -  |

<a name="getConsumerSecrets"></a>
# **getConsumerSecrets**
> ConsumerSecretListDTO getConsumerSecrets(applicationId, keyMappingId)

Retrieve Consumer Secrets 

This operation can be used to retrieve consumer secrets of an application for the given key type 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationSecretsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationSecretsApi apiInstance = new ApplicationSecretsApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyMappingId = "keyMappingId_example"; // String | OAuth Key Identifier consisting of the UUID of the Oauth Key Mapping. 
    try {
      ConsumerSecretListDTO result = apiInstance.getConsumerSecrets(applicationId, keyMappingId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationSecretsApi#getConsumerSecrets");
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
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **keyMappingId** | **String**| OAuth Key Identifier consisting of the UUID of the Oauth Key Mapping.  |

### Return type

[**ConsumerSecretListDTO**](ConsumerSecretListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Consumer secrets are retrieved.  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**403** | Forbidden. The user does not have permission to perform this action. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

<a name="revokeConsumerSecret"></a>
# **revokeConsumerSecret**
> revokeConsumerSecret(applicationId, keyMappingId, consumerSecretDeletionRequestDTO)

Revoke a Consumer Secret 

This operation can be used to revoke a consumer secret for an application for the give key type 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationSecretsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationSecretsApi apiInstance = new ApplicationSecretsApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyMappingId = "keyMappingId_example"; // String | OAuth Key Identifier consisting of the UUID of the Oauth Key Mapping. 
    ConsumerSecretDeletionRequestDTO consumerSecretDeletionRequestDTO = new ConsumerSecretDeletionRequestDTO(); // ConsumerSecretDeletionRequestDTO | Request payload containing details for revoking a new consumer secret 
    try {
      apiInstance.revokeConsumerSecret(applicationId, keyMappingId, consumerSecretDeletionRequestDTO);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationSecretsApi#revokeConsumerSecret");
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
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **keyMappingId** | **String**| OAuth Key Identifier consisting of the UUID of the Oauth Key Mapping.  |
 **consumerSecretDeletionRequestDTO** | [**ConsumerSecretDeletionRequestDTO**](ConsumerSecretDeletionRequestDTO.md)| Request payload containing details for revoking a new consumer secret  |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**204** | OK. Consumer secret deleted.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**403** | Forbidden. The user does not have permission to perform this action. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

