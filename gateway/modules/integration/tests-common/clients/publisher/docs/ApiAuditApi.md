# ApiAuditApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getAuditReportOfAPI**](ApiAuditApi.md#getAuditReportOfAPI) | **GET** /apis/{apiId}/auditapi | Retrieve the Security Audit Report of the Audit API


<a name="getAuditReportOfAPI"></a>
# **getAuditReportOfAPI**
> AuditReportDTO getAuditReportOfAPI(apiId, accept)

Retrieve the Security Audit Report of the Audit API

Retrieve the Security Audit Report of the Audit API 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ApiAuditApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiAuditApi apiInstance = new ApiAuditApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    String accept = "\"application/json\""; // String | Media types acceptable for the response. Default is application/json. 
    try {
      AuditReportDTO result = apiInstance.getAuditReportOfAPI(apiId, accept);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiAuditApi#getAuditReportOfAPI");
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
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to &quot;application/json&quot;]

### Return type

[**AuditReportDTO**](AuditReportDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. The Security Audit Report has been returned.  |  * Content-Type - The content of the body.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |

