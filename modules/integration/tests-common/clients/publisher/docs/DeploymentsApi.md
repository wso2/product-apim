# DeploymentsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**deploymentsGet**](DeploymentsApi.md#deploymentsGet) | **GET** /deployments | Retrieve Deployment Environments Details


<a name="deploymentsGet"></a>
# **deploymentsGet**
> DeploymentListDTO deploymentsGet()

Retrieve Deployment Environments Details

This operation can be used to retrieve cloud clusters information defines in tenant-conf.json file.  With that you can deploy an API to selected cloud environments. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.DeploymentsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    DeploymentsApi apiInstance = new DeploymentsApi(defaultClient);
    try {
      DeploymentListDTO result = apiInstance.deploymentsGet();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling DeploymentsApi#deploymentsGet");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**DeploymentListDTO**](DeploymentListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with the list of deployment environments information in the body.  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

