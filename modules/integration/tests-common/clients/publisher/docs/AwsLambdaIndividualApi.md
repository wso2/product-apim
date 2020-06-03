# AwsLambdaIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdAmznResourceNamesGet**](AwsLambdaIndividualApi.md#apisApiIdAmznResourceNamesGet) | **GET** /apis/{apiId}/amznResourceNames | Retrieve the ARNs of AWS Lambda functions


<a name="apisApiIdAmznResourceNamesGet"></a>
# **apisApiIdAmznResourceNamesGet**
> String apisApiIdAmznResourceNamesGet(apiId)

Retrieve the ARNs of AWS Lambda functions

This operation can be use to retrieve ARNs of AWS Lambda function for a given AWS credentials. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.AwsLambdaIndividualApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

AwsLambdaIndividualApi apiInstance = new AwsLambdaIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
try {
    String result = apiInstance.apisApiIdAmznResourceNamesGet(apiId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling AwsLambdaIndividualApi#apisApiIdAmznResourceNamesGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API.  |

### Return type

**String**

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

