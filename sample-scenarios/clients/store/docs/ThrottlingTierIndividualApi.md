# ThrottlingTierIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**tiersTierLevelTierNameGet**](ThrottlingTierIndividualApi.md#tiersTierLevelTierNameGet) | **GET** /tiers/{tierLevel}/{tierName} | Get details of a tier 


<a name="tiersTierLevelTierNameGet"></a>
# **tiersTierLevelTierNameGet**
> Tier tiersTierLevelTierNameGet(tierName, tierLevel, xWSO2Tenant, accept, ifNoneMatch, ifModifiedSince)

Get details of a tier 

This operation can be used to retrieve details of a single tier by specifying the tier level and tier name.  &#x60;X-WSO2-Tenant&#x60; header can be used to retrive tiers that belongs to a different tenant domain. If not specified super tenant will be used. If Authorization header is present in the request, the user&#39;s tenant associated with the access token will be used. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.ThrottlingTierIndividualApi;


ThrottlingTierIndividualApi apiInstance = new ThrottlingTierIndividualApi();
String tierName = "tierName_example"; // String | Tier name 
String tierLevel = "tierLevel_example"; // String | List API or Application type tiers. 
String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).  
try {
    Tier result = apiInstance.tiersTierLevelTierNameGet(tierName, tierLevel, xWSO2Tenant, accept, ifNoneMatch, ifModifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ThrottlingTierIndividualApi#tiersTierLevelTierNameGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **tierName** | **String**| Tier name  |
 **tierLevel** | **String**| List API or Application type tiers.  | [enum: api, application]
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from.  | [optional]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource.  | [optional]
 **ifModifiedSince** | **String**| Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).   | [optional]

### Return type

[**Tier**](Tier.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

