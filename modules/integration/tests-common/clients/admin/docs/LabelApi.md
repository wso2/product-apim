# LabelApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.16*

Method | HTTP request | Description
------------- | ------------- | -------------
[**labelsLabelIdDelete**](LabelApi.md#labelsLabelIdDelete) | **DELETE** /labels/{labelId} | Delete a Label
[**labelsLabelIdPut**](LabelApi.md#labelsLabelIdPut) | **PUT** /labels/{labelId} | Update a Label
[**labelsPost**](LabelApi.md#labelsPost) | **POST** /labels | Add a Label


<a name="labelsLabelIdDelete"></a>
# **labelsLabelIdDelete**
> labelsLabelIdDelete(labelId, ifMatch, ifUnmodifiedSince)

Delete a Label

Delete a Label by label Id 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.LabelApi;


LabelApi apiInstance = new LabelApi();
String labelId = "labelId_example"; // String | Label UUID 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    apiInstance.labelsLabelIdDelete(labelId, ifMatch, ifUnmodifiedSince);
} catch (ApiException e) {
    System.err.println("Exception when calling LabelApi#labelsLabelIdDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **labelId** | **String**| Label UUID  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag (Will be supported in future).  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="labelsLabelIdPut"></a>
# **labelsLabelIdPut**
> LabelDTO labelsLabelIdPut(labelId, body)

Update a Label

Update a Label by label Id 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.LabelApi;


LabelApi apiInstance = new LabelApi();
String labelId = "labelId_example"; // String | Label UUID 
LabelDTO body = new LabelDTO(); // LabelDTO | Label object with updated information 
try {
    LabelDTO result = apiInstance.labelsLabelIdPut(labelId, body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling LabelApi#labelsLabelIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **labelId** | **String**| Label UUID  |
 **body** | [**LabelDTO**](LabelDTO.md)| Label object with updated information  |

### Return type

[**LabelDTO**](LabelDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="labelsPost"></a>
# **labelsPost**
> LabelDTO labelsPost(body)

Add a Label

Add a new gateway Label 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.admin.api.ApiException;
//import org.wso2.am.integration.clients.admin.api.v1.LabelApi;


LabelApi apiInstance = new LabelApi();
LabelDTO body = new LabelDTO(); // LabelDTO | Label object that should to be added 
try {
    LabelDTO result = apiInstance.labelsPost(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling LabelApi#labelsPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**LabelDTO**](LabelDTO.md)| Label object that should to be added  |

### Return type

[**LabelDTO**](LabelDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

