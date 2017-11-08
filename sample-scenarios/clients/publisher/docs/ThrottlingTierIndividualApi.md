# ThrottlingTierIndividualApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v0.11*

Method | HTTP request | Description
------------- | ------------- | -------------
[**tiersTierLevelTierNameDelete**](ThrottlingTierIndividualApi.md#tiersTierLevelTierNameDelete) | **DELETE** /tiers/{tierLevel}/{tierName} | Delete a Tier
[**tiersTierLevelTierNameGet**](ThrottlingTierIndividualApi.md#tiersTierLevelTierNameGet) | **GET** /tiers/{tierLevel}/{tierName} | Get details of a tier
[**tiersTierLevelTierNamePut**](ThrottlingTierIndividualApi.md#tiersTierLevelTierNamePut) | **PUT** /tiers/{tierLevel}/{tierName} | Update a Tier
[**tiersUpdatePermissionPost**](ThrottlingTierIndividualApi.md#tiersUpdatePermissionPost) | **POST** /tiers/update-permission | Update tier permission


<a name="tiersTierLevelTierNameDelete"></a>
# **tiersTierLevelTierNameDelete**
> tiersTierLevelTierNameDelete(tierName, tierLevel, ifMatch, ifUnmodifiedSince)

Delete a Tier

This operation can be used to delete an existing tier. The only supported tier level is &#x60;api&#x60; tiers. &#x60;DELETE https://127.0.0.1:9443/api/am/publisher/v0.11/tiers/api/Low&#x60;  **IMPORTANT:** * This is only effective when Advanced Throttling is disabled in the Server. If enabled, we need to use Admin REST API for throttling tiers modification related operations. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.ThrottlingTierIndividualApi;


ThrottlingTierIndividualApi apiInstance = new ThrottlingTierIndividualApi();
String tierName = "tierName_example"; // String | Tier name 
String tierLevel = "tierLevel_example"; // String | List API or Application or Resource type tiers. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    apiInstance.tiersTierLevelTierNameDelete(tierName, tierLevel, ifMatch, ifUnmodifiedSince);
} catch (ApiException e) {
    System.err.println("Exception when calling ThrottlingTierIndividualApi#tiersTierLevelTierNameDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **tierName** | **String**| Tier name  |
 **tierLevel** | **String**| List API or Application or Resource type tiers.  | [enum: api]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag (Will be supported in future).  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="tiersTierLevelTierNameGet"></a>
# **tiersTierLevelTierNameGet**
> Tier tiersTierLevelTierNameGet(tierName, tierLevel, accept, ifNoneMatch, ifModifiedSince)

Get details of a tier

This operation can be used to retrieve details of a single tier by specifying the tier level and tier name. Note that the scope of the API is mandatory while retreiving the access token with the following cURL command : &#x60;curl -k -d \\\&quot;grant_type&#x3D;password&amp;username&#x3D;username&amp;password&#x3D;password&amp;scope&#x3D;apim:tier_view\\\&quot; -H \\\&quot;Authorization: Basic &lt;token&gt;\\\&quot; https://127.0.0.1:8243/token&#x60;. You will receive the access token as the response, for example &#x60;\&quot;access_token\&quot;:\&quot;8644c013-7ff1-3217-b150-d7b92cae6be7\&quot;&#x60;. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.ThrottlingTierIndividualApi;


ThrottlingTierIndividualApi apiInstance = new ThrottlingTierIndividualApi();
String tierName = "tierName_example"; // String | Tier name 
String tierLevel = "tierLevel_example"; // String | List API or Application or Resource type tiers. 
String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future). 
try {
    Tier result = apiInstance.tiersTierLevelTierNameGet(tierName, tierLevel, accept, ifNoneMatch, ifModifiedSince);
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
 **tierLevel** | **String**| List API or Application or Resource type tiers.  | [enum: api, application, resource]
 **accept** | **String**| Media types acceptable for the response. Default is application/json.  | [optional] [default to application/json]
 **ifNoneMatch** | **String**| Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]
 **ifModifiedSince** | **String**| Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future).  | [optional]

### Return type

[**Tier**](Tier.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="tiersTierLevelTierNamePut"></a>
# **tiersTierLevelTierNamePut**
> Tier tiersTierLevelTierNamePut(tierName, body, tierLevel, contentType, ifMatch, ifUnmodifiedSince)

Update a Tier

This operation can be used to update an existing tier. The only supported tier level is &#x60;api&#x60; tiers. &#x60;PUT https://127.0.0.1:9443/api/am/publisher/v0.11/tiers/api/Low&#x60;  **IMPORTANT:** * This is only effective when Advanced Throttling is disabled in the Server. If enabled, we need to use Admin REST API for throttling tiers modification related operations. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.ThrottlingTierIndividualApi;


ThrottlingTierIndividualApi apiInstance = new ThrottlingTierIndividualApi();
String tierName = "tierName_example"; // String | Tier name 
Tier body = new Tier(); // Tier | Tier object that needs to be modified 
String tierLevel = "tierLevel_example"; // String | List API or Application or Resource type tiers. 
String contentType = "application/json"; // String | Media type of the entity in the body. Default is application/json. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
try {
    Tier result = apiInstance.tiersTierLevelTierNamePut(tierName, body, tierLevel, contentType, ifMatch, ifUnmodifiedSince);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ThrottlingTierIndividualApi#tiersTierLevelTierNamePut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **tierName** | **String**| Tier name  |
 **body** | [**Tier**](Tier.md)| Tier object that needs to be modified  |
 **tierLevel** | **String**| List API or Application or Resource type tiers.  | [enum: api]
 **contentType** | **String**| Media type of the entity in the body. Default is application/json.  | [default to application/json]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag (Will be supported in future).  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]

### Return type

[**Tier**](Tier.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="tiersUpdatePermissionPost"></a>
# **tiersUpdatePermissionPost**
> List&lt;Tier&gt; tiersUpdatePermissionPost(tierName, tierLevel, ifMatch, ifUnmodifiedSince, permissions)

Update tier permission

This operation can be used to update tier permissions which controls access for the particular tier based on the subscribers&#39; roles. 

### Example
```java
// Import classes:
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
//import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.ThrottlingTierIndividualApi;


ThrottlingTierIndividualApi apiInstance = new ThrottlingTierIndividualApi();
String tierName = "tierName_example"; // String | Name of the tier 
String tierLevel = "tierLevel_example"; // String | List API or Application or Resource type tiers. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag (Will be supported in future). 
String ifUnmodifiedSince = "ifUnmodifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header (Will be supported in future). 
TierPermission permissions = new TierPermission(); // TierPermission | 
try {
    List<Tier> result = apiInstance.tiersUpdatePermissionPost(tierName, tierLevel, ifMatch, ifUnmodifiedSince, permissions);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ThrottlingTierIndividualApi#tiersUpdatePermissionPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **tierName** | **String**| Name of the tier  |
 **tierLevel** | **String**| List API or Application or Resource type tiers.  | [enum: api, application, resource]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag (Will be supported in future).  | [optional]
 **ifUnmodifiedSince** | **String**| Validator for conditional requests; based on Last Modified header (Will be supported in future).  | [optional]
 **permissions** | [**TierPermission**](TierPermission.md)|  | [optional]

### Return type

[**List&lt;Tier&gt;**](Tier.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

