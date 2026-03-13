# ApiKeysApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**applicationsApplicationIdApiKeysKeyTypeGeneratePost**](ApiKeysApi.md#applicationsApplicationIdApiKeysKeyTypeGeneratePost) | **POST** /applications/{applicationId}/api-keys/{keyType}/generate | Generate API Key
[**applicationsApplicationIdApiKeysKeyTypeRevokePost**](ApiKeysApi.md#applicationsApplicationIdApiKeysKeyTypeRevokePost) | **POST** /applications/{applicationId}/api-keys/{keyType}/revoke | Revoke API Key
[**associateAPIKey**](ApiKeysApi.md#associateAPIKey) | **POST** /apis/{apiId}/api-keys/associate | Create an association for the API Key
[**associateAPIKeyToApp**](ApiKeysApi.md#associateAPIKeyToApp) | **POST** /applications/{applicationId}/api-keys/{keyType}/associate | Associate an API Key to the Application
[**dissociateAPIKey**](ApiKeysApi.md#dissociateAPIKey) | **POST** /apis/{apiId}/api-keys/dissociate | Remove an Association for the API Key
[**dissociateAPIKeyFromApp**](ApiKeysApi.md#dissociateAPIKeyFromApp) | **POST** /applications/{applicationId}/api-keys/{keyType}/dissociate | Remove the association of the API Key to the Application
[**generateApiBoundApiKey**](ApiKeysApi.md#generateApiBoundApiKey) | **POST** /apis/{apiId}/api-keys/generate | Generate API Key
[**getAPIBoundAPIKeys**](ApiKeysApi.md#getAPIBoundAPIKeys) | **GET** /apis/{apiId}/api-keys | Get API Key(s) of a Given API
[**getAPIKeyAssociationsForApp**](ApiKeysApi.md#getAPIKeyAssociationsForApp) | **GET** /applications/{applicationId}/api-keys/{keyType}/associations | Get API Key associations of a Given Type
[**getAppBoundAPIKeys**](ApiKeysApi.md#getAppBoundAPIKeys) | **GET** /applications/{applicationId}/api-keys/{keyType} | Get API Key(s) of a Given Type
[**getSubscribedAPIsWithAPIKeys**](ApiKeysApi.md#getSubscribedAPIsWithAPIKeys) | **GET** /applications/{applicationId}/api-keys/{keyType}/subscriptions | Get Subscribed APIs with API Key(s) of a Given Type
[**regenerateAPIBoundAPIKey**](ApiKeysApi.md#regenerateAPIBoundAPIKey) | **POST** /apis/{apiId}/api-keys/regenerate | Regenerate API Key
[**regenerateAppBoundAPIKey**](ApiKeysApi.md#regenerateAppBoundAPIKey) | **POST** /applications/{applicationId}/api-keys/{keyType}/regenerate | Regenerate API Key
[**revokeAPIBoundAPIKey**](ApiKeysApi.md#revokeAPIBoundAPIKey) | **POST** /apis/{apiId}/api-keys/revoke | Revoke API Key


<a name="applicationsApplicationIdApiKeysKeyTypeGeneratePost"></a>
# **applicationsApplicationIdApiKeysKeyTypeGeneratePost**
> APIKeyDTO applicationsApplicationIdApiKeysKeyTypeGeneratePost(applicationId, keyType, ifMatch, apIKeyGenerateRequestDTO)

Generate API Key

Generate a self contained API Key for the application 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApiKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiKeysApi apiInstance = new ApiKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    APIKeyGenerateRequestDTO apIKeyGenerateRequestDTO = new APIKeyGenerateRequestDTO(); // APIKeyGenerateRequestDTO | API Key generation request object 
    try {
      APIKeyDTO result = apiInstance.applicationsApplicationIdApiKeysKeyTypeGeneratePost(applicationId, keyType, ifMatch, apIKeyGenerateRequestDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiKeysApi#applicationsApplicationIdApiKeysKeyTypeGeneratePost");
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
 **keyType** | **String**| **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox).  | [enum: PRODUCTION, SANDBOX]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]
 **apIKeyGenerateRequestDTO** | [**APIKeyGenerateRequestDTO**](APIKeyGenerateRequestDTO.md)| API Key generation request object  | [optional]

### Return type

[**APIKeyDTO**](APIKeyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Api key generated.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="applicationsApplicationIdApiKeysKeyTypeRevokePost"></a>
# **applicationsApplicationIdApiKeysKeyTypeRevokePost**
> applicationsApplicationIdApiKeysKeyTypeRevokePost(applicationId, keyType, apIKeyRevokeRequestDTO, ifMatch)

Revoke API Key

Revoke a self contained API Key for the application 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApiKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiKeysApi apiInstance = new ApiKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
    APIKeyRevokeRequestDTO apIKeyRevokeRequestDTO = new APIKeyRevokeRequestDTO(); // APIKeyRevokeRequestDTO | API Key revoke request object 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      apiInstance.applicationsApplicationIdApiKeysKeyTypeRevokePost(applicationId, keyType, apIKeyRevokeRequestDTO, ifMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiKeysApi#applicationsApplicationIdApiKeysKeyTypeRevokePost");
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
 **keyType** | **String**| **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox).  | [enum: PRODUCTION, SANDBOX]
 **apIKeyRevokeRequestDTO** | [**APIKeyRevokeRequestDTO**](APIKeyRevokeRequestDTO.md)| API Key revoke request object  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

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
**200** | OK. Api key revoked successfully.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="associateAPIKey"></a>
# **associateAPIKey**
> APIKeyAssociationDTO associateAPIKey(apiId, apIAPIKeyAssociateRequestDTO, ifMatch)

Create an association for the API Key

Create an association for a self contained API Key for the API 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApiKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiKeysApi apiInstance = new ApiKeysApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    APIAPIKeyAssociateRequestDTO apIAPIKeyAssociateRequestDTO = new APIAPIKeyAssociateRequestDTO(); // APIAPIKeyAssociateRequestDTO | API Key association request object 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      APIKeyAssociationDTO result = apiInstance.associateAPIKey(apiId, apIAPIKeyAssociateRequestDTO, ifMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiKeysApi#associateAPIKey");
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
 **apIAPIKeyAssociateRequestDTO** | [**APIAPIKeyAssociateRequestDTO**](APIAPIKeyAssociateRequestDTO.md)| API Key association request object  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**APIKeyAssociationDTO**](APIKeyAssociationDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Api key associated successfully.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="associateAPIKeyToApp"></a>
# **associateAPIKeyToApp**
> APIKeyAssociationDTO associateAPIKeyToApp(applicationId, keyType, appAPIKeyAssociateRequestDTO, ifMatch)

Associate an API Key to the Application

Create an association to a self contained API Key for the application 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApiKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiKeysApi apiInstance = new ApiKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
    AppAPIKeyAssociateRequestDTO appAPIKeyAssociateRequestDTO = new AppAPIKeyAssociateRequestDTO(); // AppAPIKeyAssociateRequestDTO | API Key association request object 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      APIKeyAssociationDTO result = apiInstance.associateAPIKeyToApp(applicationId, keyType, appAPIKeyAssociateRequestDTO, ifMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiKeysApi#associateAPIKeyToApp");
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
 **keyType** | **String**| **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox).  | [enum: PRODUCTION, SANDBOX]
 **appAPIKeyAssociateRequestDTO** | [**AppAPIKeyAssociateRequestDTO**](AppAPIKeyAssociateRequestDTO.md)| API Key association request object  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**APIKeyAssociationDTO**](APIKeyAssociationDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Api key associated successfully.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="dissociateAPIKey"></a>
# **dissociateAPIKey**
> dissociateAPIKey(apiId, apIKeyDissociateRequestDTO, ifMatch)

Remove an Association for the API Key

Remove an association a self contained API Key for the API specified by the key UUID 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApiKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiKeysApi apiInstance = new ApiKeysApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    APIKeyDissociateRequestDTO apIKeyDissociateRequestDTO = new APIKeyDissociateRequestDTO(); // APIKeyDissociateRequestDTO | API Key dissociation request object 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      apiInstance.dissociateAPIKey(apiId, apIKeyDissociateRequestDTO, ifMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiKeysApi#dissociateAPIKey");
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
 **apIKeyDissociateRequestDTO** | [**APIKeyDissociateRequestDTO**](APIKeyDissociateRequestDTO.md)| API Key dissociation request object  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

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
**200** | OK. Removed association for the Api key successfully.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="dissociateAPIKeyFromApp"></a>
# **dissociateAPIKeyFromApp**
> dissociateAPIKeyFromApp(applicationId, keyType, apIKeyDissociateRequestDTO, ifMatch)

Remove the association of the API Key to the Application

Remove the association to a self contained API Key for the application 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApiKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiKeysApi apiInstance = new ApiKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
    APIKeyDissociateRequestDTO apIKeyDissociateRequestDTO = new APIKeyDissociateRequestDTO(); // APIKeyDissociateRequestDTO | API Key dissociation request object 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      apiInstance.dissociateAPIKeyFromApp(applicationId, keyType, apIKeyDissociateRequestDTO, ifMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiKeysApi#dissociateAPIKeyFromApp");
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
 **keyType** | **String**| **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox).  | [enum: PRODUCTION, SANDBOX]
 **apIKeyDissociateRequestDTO** | [**APIKeyDissociateRequestDTO**](APIKeyDissociateRequestDTO.md)| API Key dissociation request object  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

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
**200** | OK. Removed association for the API key successfully.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="generateApiBoundApiKey"></a>
# **generateApiBoundApiKey**
> APIKeyDTO generateApiBoundApiKey(apiId, apIAPIKeyGenerateRequestDTO, ifMatch)

Generate API Key

Generate a self contained API Key for the API 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApiKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiKeysApi apiInstance = new ApiKeysApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    APIAPIKeyGenerateRequestDTO apIAPIKeyGenerateRequestDTO = new APIAPIKeyGenerateRequestDTO(); // APIAPIKeyGenerateRequestDTO | API Key generation request object 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      APIKeyDTO result = apiInstance.generateApiBoundApiKey(apiId, apIAPIKeyGenerateRequestDTO, ifMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiKeysApi#generateApiBoundApiKey");
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
 **apIAPIKeyGenerateRequestDTO** | [**APIAPIKeyGenerateRequestDTO**](APIAPIKeyGenerateRequestDTO.md)| API Key generation request object  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**APIKeyDTO**](APIKeyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Api key generated.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="getAPIBoundAPIKeys"></a>
# **getAPIBoundAPIKeys**
> APIAPIKeyListDTO getAPIBoundAPIKeys(apiId, ifNoneMatch)

Get API Key(s) of a Given API

Retrieve self contained API Key(s) for the API. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApiKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiKeysApi apiInstance = new ApiKeysApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    try {
      APIAPIKeyListDTO result = apiInstance.getAPIBoundAPIKeys(apiId, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiKeysApi#getAPIBoundAPIKeys");
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
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**APIAPIKeyListDTO**](APIAPIKeyListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. API key(s) of a given API are returned.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="getAPIKeyAssociationsForApp"></a>
# **getAPIKeyAssociationsForApp**
> APIKeyAssociationListDTO getAPIKeyAssociationsForApp(applicationId, keyType, ifNoneMatch)

Get API Key associations of a Given Type

Retrieve API key associations for the application specifying the key type in the URI. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApiKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiKeysApi apiInstance = new ApiKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    try {
      APIKeyAssociationListDTO result = apiInstance.getAPIKeyAssociationsForApp(applicationId, keyType, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiKeysApi#getAPIKeyAssociationsForApp");
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
 **keyType** | **String**| **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox).  | [enum: PRODUCTION, SANDBOX]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**APIKeyAssociationListDTO**](APIKeyAssociationListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. API key associations of given type are returned.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="getAppBoundAPIKeys"></a>
# **getAppBoundAPIKeys**
> APIKeyListDTO getAppBoundAPIKeys(applicationId, keyType, ifNoneMatch)

Get API Key(s) of a Given Type

Retrieve self contained API Key(s) for the application specifying the key type in the URI. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApiKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiKeysApi apiInstance = new ApiKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    try {
      APIKeyListDTO result = apiInstance.getAppBoundAPIKeys(applicationId, keyType, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiKeysApi#getAppBoundAPIKeys");
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
 **keyType** | **String**| **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox).  | [enum: PRODUCTION, SANDBOX]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**APIKeyListDTO**](APIKeyListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. API key(s) of given type are returned.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="getSubscribedAPIsWithAPIKeys"></a>
# **getSubscribedAPIsWithAPIKeys**
> SubscribedAPIWithApiKeyListDTO getSubscribedAPIsWithAPIKeys(applicationId, keyType, ifNoneMatch)

Get Subscribed APIs with API Key(s) of a Given Type

Retrieve subscribed APIs with non-associated API Key(s) for the application specifying the key type in the URI. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApiKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiKeysApi apiInstance = new ApiKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
    String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec. 
    try {
      SubscribedAPIWithApiKeyListDTO result = apiInstance.getSubscribedAPIsWithAPIKeys(applicationId, keyType, ifNoneMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiKeysApi#getSubscribedAPIsWithAPIKeys");
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
 **keyType** | **String**| **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox).  | [enum: PRODUCTION, SANDBOX]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resourec.  | [optional]

### Return type

[**SubscribedAPIWithApiKeyListDTO**](SubscribedAPIWithApiKeyListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Subscribed APIs along with the non-associated API key(s) of given type are returned.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="regenerateAPIBoundAPIKey"></a>
# **regenerateAPIBoundAPIKey**
> APIKeyDTO regenerateAPIBoundAPIKey(apiId, apIKeyRenewRequestDTO, ifMatch)

Regenerate API Key

Regenerate a self contained API Key for the API specified by the key UUID 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApiKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiKeysApi apiInstance = new ApiKeysApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    APIKeyRenewRequestDTO apIKeyRenewRequestDTO = new APIKeyRenewRequestDTO(); // APIKeyRenewRequestDTO | API Key renewal request object 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      APIKeyDTO result = apiInstance.regenerateAPIBoundAPIKey(apiId, apIKeyRenewRequestDTO, ifMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiKeysApi#regenerateAPIBoundAPIKey");
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
 **apIKeyRenewRequestDTO** | [**APIKeyRenewRequestDTO**](APIKeyRenewRequestDTO.md)| API Key renewal request object  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**APIKeyDTO**](APIKeyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Api key regenerated successfully.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="regenerateAppBoundAPIKey"></a>
# **regenerateAppBoundAPIKey**
> APIKeyDTO regenerateAppBoundAPIKey(applicationId, keyType, apIKeyRenewRequestDTO, ifMatch)

Regenerate API Key

Regenerate a self contained API Key for the application specified by the key UUID 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApiKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiKeysApi apiInstance = new ApiKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
    APIKeyRenewRequestDTO apIKeyRenewRequestDTO = new APIKeyRenewRequestDTO(); // APIKeyRenewRequestDTO | API Key renewal request object 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      APIKeyDTO result = apiInstance.regenerateAppBoundAPIKey(applicationId, keyType, apIKeyRenewRequestDTO, ifMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiKeysApi#regenerateAppBoundAPIKey");
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
 **keyType** | **String**| **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox).  | [enum: PRODUCTION, SANDBOX]
 **apIKeyRenewRequestDTO** | [**APIKeyRenewRequestDTO**](APIKeyRenewRequestDTO.md)| API Key renewal request object  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**APIKeyDTO**](APIKeyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Api key regenerated successfully.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="revokeAPIBoundAPIKey"></a>
# **revokeAPIBoundAPIKey**
> revokeAPIBoundAPIKey(apiId, apIAPIKeyRevokeRequestDTO, ifMatch)

Revoke API Key

Revoke a self contained API Key for the API specified by the key UUID 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApiKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiKeysApi apiInstance = new ApiKeysApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    APIAPIKeyRevokeRequestDTO apIAPIKeyRevokeRequestDTO = new APIAPIKeyRevokeRequestDTO(); // APIAPIKeyRevokeRequestDTO | API Key revocation request object 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      apiInstance.revokeAPIBoundAPIKey(apiId, apIAPIKeyRevokeRequestDTO, ifMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiKeysApi#revokeAPIBoundAPIKey");
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
 **apIAPIKeyRevokeRequestDTO** | [**APIAPIKeyRevokeRequestDTO**](APIAPIKeyRevokeRequestDTO.md)| API Key revocation request object  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

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
**200** | OK. Api key revoked successfully.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

