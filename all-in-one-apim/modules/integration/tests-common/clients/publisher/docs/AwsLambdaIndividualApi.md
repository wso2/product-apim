# AwsLambdaIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getAmazonResourceNamesOfAPI**](AwsLambdaIndividualApi.md#getAmazonResourceNamesOfAPI) | **GET** /apis/{apiId}/amznResourceNames | Retrieve the ARNs of AWS Lambda Functions


<a name="getAmazonResourceNamesOfAPI"></a>
# **getAmazonResourceNamesOfAPI**
> String getAmazonResourceNamesOfAPI(apiId)

Retrieve the ARNs of AWS Lambda Functions

This operation can be use to retrieve ARNs of AWS Lambda function for a given AWS credentials. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.AwsLambdaIndividualApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    AwsLambdaIndividualApi apiInstance = new AwsLambdaIndividualApi(defaultClient);
    String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. 
    try {
      String result = apiInstance.getAmazonResourceNamesOfAPI(apiId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling AwsLambdaIndividualApi#getAmazonResourceNamesOfAPI");
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
**200** | OK. Requested ARN List of the API is returned  |  * Content-Type - The content type of the body.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |

