# DefaultApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.16*

Method | HTTP request | Description
------------- | ------------- | -------------
[**botDataAddEmailPost**](DefaultApi.md#botDataAddEmailPost) | **POST** /botData/addEmail | Add an Email
[**botDataDeleteEmailDelete**](DefaultApi.md#botDataDeleteEmailDelete) | **DELETE** /botData/deleteEmail | Delete an configured email.
[**botDataGetEmailListGet**](DefaultApi.md#botDataGetEmailListGet) | **GET** /botData/getEmailList | Get all configured email list 


<a name="botDataAddEmailPost"></a>
# **botDataAddEmailPost**
> EmailDTO botDataAddEmailPost(body)

Add an Email

Here we can use this to configure email 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
EmailDTO body = new EmailDTO(); // EmailDTO | A email 
try {
    EmailDTO result = apiInstance.botDataAddEmailPost(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#botDataAddEmailPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**EmailDTO**](EmailDTO.md)| A email  |

### Return type

[**EmailDTO**](EmailDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="botDataDeleteEmailDelete"></a>
# **botDataDeleteEmailDelete**
> botDataDeleteEmailDelete(uuid)

Delete an configured email.

Delete an configured email from DB by pasing uuid. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
String uuid = "uuid_example"; // String | Pass the uuid to remove the email 
try {
    apiInstance.botDataDeleteEmailDelete(uuid);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#botDataDeleteEmailDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **uuid** | **String**| Pass the uuid to remove the email  |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="botDataGetEmailListGet"></a>
# **botDataGetEmailListGet**
> botDataGetEmailListGet(tenantDomain)

Get all configured email list 

Get all email list which configured to trigger for BotData api email alert 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
String tenantDomain = "tenantDomain_example"; // String | Pass the tenantDomain to get the email list and if not passed it will get from the logged user. 
try {
    apiInstance.botDataGetEmailListGet(tenantDomain);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#botDataGetEmailListGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **tenantDomain** | **String**| Pass the tenantDomain to get the email list and if not passed it will get from the logged user.  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

