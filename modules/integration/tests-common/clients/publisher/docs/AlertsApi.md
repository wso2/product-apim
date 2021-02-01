# AlertsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v2*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getPublisherAlertTypes**](AlertsApi.md#getPublisherAlertTypes) | **GET** /alert-types | Get the list of API Publisher alert types. 


<a name="getPublisherAlertTypes"></a>
# **getPublisherAlertTypes**
> AlertTypesListDTO getPublisherAlertTypes()

Get the list of API Publisher alert types. 

This operation is used to get the list of supportd alert types for the &#39;publisher&#39; agent. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.AlertsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v2");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    AlertsApi apiInstance = new AlertsApi(defaultClient);
    try {
      AlertTypesListDTO result = apiInstance.getPublisherAlertTypes();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling AlertsApi#getPublisherAlertTypes");
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

[**AlertTypesListDTO**](AlertTypesListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. The list of publisher alert types are returned.  |  * Content-Type - The content type of the body.  <br>  |
**500** | Internal Server Error. |  -  |

