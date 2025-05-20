# LinterCustomRulesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getLinterCustomRules**](LinterCustomRulesApi.md#getLinterCustomRules) | **GET** /linter-custom-rules | Get linter custom rules.


<a name="getLinterCustomRules"></a>
# **getLinterCustomRules**
> String getLinterCustomRules()

Get linter custom rules.

This operation can be used to get linter custom rules from tenant-config. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.LinterCustomRulesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    LinterCustomRulesApi apiInstance = new LinterCustomRulesApi(defaultClient);
    try {
      String result = apiInstance.getLinterCustomRules();
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling LinterCustomRulesApi#getLinterCustomRules");
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

**String**

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Linter Custom Rules Retrieved Successfully.  |  * Content-Type - The content type of the body.  <br>  |
**403** | Forbidden. The request must be conditional but no condition has been specified. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**500** | Internal Server Error. |  -  |

