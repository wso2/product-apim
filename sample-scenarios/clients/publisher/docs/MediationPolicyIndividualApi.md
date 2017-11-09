# MediationPolicyIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**apisApiIdPoliciesMediationMediationPolicyIdDelete**](MediationPolicyIndividualApi.md#apisApiIdPoliciesMediationMediationPolicyIdDelete) | **DELETE** /apis/{apiId}/policies/mediation/{mediationPolicyId} | Delete an API specific mediation policy
[**apisApiIdPoliciesMediationMediationPolicyIdGet**](MediationPolicyIndividualApi.md#apisApiIdPoliciesMediationMediationPolicyIdGet) | **GET** /apis/{apiId}/policies/mediation/{mediationPolicyId} | Get an API specific mediation policy
[**apisApiIdPoliciesMediationMediationPolicyIdPut**](MediationPolicyIndividualApi.md#apisApiIdPoliciesMediationMediationPolicyIdPut) | **PUT** /apis/{apiId}/policies/mediation/{mediationPolicyId} | Update an API specific mediation policy


<a name="apisApiIdPoliciesMediationMediationPolicyIdDelete"></a>
# **apisApiIdPoliciesMediationMediationPolicyIdDelete**
> apisApiIdPoliciesMediationMediationPolicyIdDelete(apiId, mediationPolicyId, ifMatch, ifUnmodifiedSince)

Delete an API specific mediation policy

This operation can be used to delete an existing API specific mediation policy providing the Id of the API and the Id of the mediation policy. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.MediationPolicyIndividualApi;


MediationPolicyIndividualApi apiInstance = new MediationPolicyIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**. 
String mediationPolicyId = "mediationPolicyId_example"; // String | Mediation policy Id 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    apiInstance.apisApiIdPoliciesMediationMediationPolicyIdDelete(apiId, mediationPolicyId, ifMatch, ifUnmodifiedSince);
} catch (ApiException e) {
    System.err.println("Exception when calling MediationPolicyIndividualApi#apisApiIdPoliciesMediationMediationPolicyIdDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  |
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

<a name="apisApiIdPoliciesMediationMediationPolicyIdGet"></a>
# **apisApiIdPoliciesMediationMediationPolicyIdGet**
> Mediation apisApiIdPoliciesMediationMediationPolicyIdGet(apiId, mediationPolicyId, accept, ifNoneMatch, ifModifiedSince)

Get an API specific mediation policy

This operation can be used to retrieve a particular API specific mediation policy. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.MediationPolicyIndividualApi;


MediationPolicyIndividualApi apiInstance = new MediationPolicyIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**. 
String mediationPolicyId = "mediationPolicyId_example"; // String | Mediation policy Id 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    Mediation result = apiInstance.apisApiIdPoliciesMediationMediationPolicyIdGet(apiId, mediationPolicyId, accept, ifNoneMatch, ifModifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MediationPolicyIndividualApi#apisApiIdPoliciesMediationMediationPolicyIdGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  |
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

<a name="apisApiIdPoliciesMediationMediationPolicyIdPut"></a>
# **apisApiIdPoliciesMediationMediationPolicyIdPut**
> Mediation apisApiIdPoliciesMediationMediationPolicyIdPut(apiId, mediationPolicyId, body, contentType, ifMatch, ifUnmodifiedSince)

Update an API specific mediation policy

This operation can be used to update an existing mediation policy of an API. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.MediationPolicyIndividualApi;


MediationPolicyIndividualApi apiInstance = new MediationPolicyIndividualApi();
String apiId = "apiId_example"; // String | **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**. 
String mediationPolicyId = "mediationPolicyId_example"; // String | Mediation policy Id 
Mediation body = new Mediation(); // Mediation | Mediation policy object that needs to be updated 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    Mediation result = apiInstance.apisApiIdPoliciesMediationMediationPolicyIdPut(apiId, mediationPolicyId, body, contentType, ifMatch, ifUnmodifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MediationPolicyIndividualApi#apisApiIdPoliciesMediationMediationPolicyIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **apiId** | **String**| **API ID** consisting of the **UUID** of the API. Using the **UUID** in the API call is recommended. The combination of the provider of the API, name of the API and the version is also accepted as a valid API ID. Should be formatted as **provider-name-version**.  |
 **mediationPolicyId** | **String**| Mediation policy Id  |
 **body** | [**Mediation**](Mediation.md)| Mediation policy object that needs to be updated  |
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

