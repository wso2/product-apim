# MediationPolicyIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**policiesMediationMediationPolicyIdDelete**](MediationPolicyIndividualApi.md#policiesMediationMediationPolicyIdDelete) | **DELETE** /policies/mediation/{mediationPolicyId} | Delete a global mediation policy
[**policiesMediationMediationPolicyIdGet**](MediationPolicyIndividualApi.md#policiesMediationMediationPolicyIdGet) | **GET** /policies/mediation/{mediationPolicyId} | Get a global mediation policy
[**policiesMediationMediationPolicyIdPut**](MediationPolicyIndividualApi.md#policiesMediationMediationPolicyIdPut) | **PUT** /policies/mediation/{mediationPolicyId} | Update a global mediation policy


<a name="policiesMediationMediationPolicyIdDelete"></a>
# **policiesMediationMediationPolicyIdDelete**
> policiesMediationMediationPolicyIdDelete(mediationPolicyId, ifMatch, ifUnmodifiedSince)

Delete a global mediation policy

This operation can be used to delete an existing global mediation policy providing the Id of the mediation policy. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.MediationPolicyIndividualApi;


MediationPolicyIndividualApi apiInstance = new MediationPolicyIndividualApi();
String mediationPolicyId = "mediationPolicyId_example"; // String | Mediation policy Id 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    apiInstance.policiesMediationMediationPolicyIdDelete(mediationPolicyId, ifMatch, ifUnmodifiedSince);
} catch (ApiException e) {
    System.err.println("Exception when calling MediationPolicyIndividualApi#policiesMediationMediationPolicyIdDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **mediationPolicyId** | **String**| Mediation policy Id  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag (Will be supported in future).  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="policiesMediationMediationPolicyIdGet"></a>
# **policiesMediationMediationPolicyIdGet**
> Mediation policiesMediationMediationPolicyIdGet(mediationPolicyId, accept, ifNoneMatch, ifModifiedSince)

Get a global mediation policy

This operation can be used to retrieve a particular global mediation policy. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.MediationPolicyIndividualApi;


MediationPolicyIndividualApi apiInstance = new MediationPolicyIndividualApi();
String mediationPolicyId = "mediationPolicyId_example"; // String | Mediation policy Id 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    Mediation result = apiInstance.policiesMediationMediationPolicyIdGet(mediationPolicyId, accept, ifNoneMatch, ifModifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MediationPolicyIndividualApi#policiesMediationMediationPolicyIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **mediationPolicyId** | **String**| Mediation policy Id  |
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]
 **ifModifiedSince** | **String**| Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]

### Return type

[**Mediation**](Mediation.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="policiesMediationMediationPolicyIdPut"></a>
# **policiesMediationMediationPolicyIdPut**
> Mediation policiesMediationMediationPolicyIdPut(mediationPolicyId, body, contentType, ifMatch, ifUnmodifiedSince)

Update a global mediation policy

This operation can be used to update an existing global mediation policy. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.MediationPolicyIndividualApi;


MediationPolicyIndividualApi apiInstance = new MediationPolicyIndividualApi();
String mediationPolicyId = "mediationPolicyId_example"; // String | Mediation policy Id 
Mediation body = new Mediation(); // Mediation | Mediation policy object that needs to be added 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    Mediation result = apiInstance.policiesMediationMediationPolicyIdPut(mediationPolicyId, body, contentType, ifMatch, ifUnmodifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MediationPolicyIndividualApi#policiesMediationMediationPolicyIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **mediationPolicyId** | **String**| Mediation policy Id  |
 **body** | [**Mediation**](Mediation.md)| Mediation policy object that needs to be added  |
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag (Will be supported in future).  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

[**Mediation**](Mediation.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

