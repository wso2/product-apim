# SettingsApi

All URIs are relative to *https://apis.wso2.com/api/service-catalog/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getSettings**](SettingsApi.md#getSettings) | **GET** /settings | Retrieve service catalog API settings


<a name="getSettings"></a>
# **getSettings**
> SettingsDTO getSettings()

Retrieve service catalog API settings

Retrieve Service Catalog API settings 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.service.catalog.api.ApiClient;
import org.wso2.am.integration.clients.service.catalog.api.ApiException;
import org.wso2.am.integration.clients.service.catalog.api.Configuration;
import org.wso2.am.integration.clients.service.catalog.api.models.*;
import org.wso2.am.integration.clients.service.catalog.api.v1.SettingsApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/service-catalog/v1");

    SettingsApi apiInstance = new SettingsApi(defaultClient);
    try {
      SettingsDTO result = apiInstance.getSettings();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling SettingsApi#getSettings");
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

[**SettingsDTO**](SettingsDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Settings returned  |  -  |
**401** | Unauthorized. The user is not authorized. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

