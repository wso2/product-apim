# ApiAuditApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdAuditapiGet**](ApiAuditApi.md#apisApiIdAuditapiGet) | **GET** /apis/{apiId}/auditapi | Retrieve the Security Audit Report of the Audit API


<a name="apisApiIdAuditapiGet"></a>
# **apisApiIdAuditapiGet**
> AuditReportDTO apisApiIdAuditapiGet(apiId, accept)

Retrieve the Security Audit Report of the Audit API

Retrieve the Security Audit Report of the Audit API 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ApiAuditApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiAuditApi apiInstance = new ApiAuditApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
try {
    AuditReportDTO result = apiInstance.apisApiIdAuditapiGet(apiId, accept);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiAuditApi#apisApiIdAuditapiGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]

### Return type

[**AuditReportDTO**](AuditReportDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

