# AlertSubscriptionsApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getSubscribedAlertTypes**](AlertSubscriptionsApi.md#getSubscribedAlertTypes) | **GET** /alert-subscriptions | Get the list of API Publisher alert types subscribed by the user. 
[**subscribeToAlerts**](AlertSubscriptionsApi.md#subscribeToAlerts) | **PUT** /alert-subscriptions | Subscribe to the selected alert types by the user. 
[**unsubscribeAllAlerts**](AlertSubscriptionsApi.md#unsubscribeAllAlerts) | **DELETE** /alert-subscriptions | Unsubscribe user from all the alert types. 


<a name="getSubscribedAlertTypes"></a>
# **getSubscribedAlertTypes**
> AlertsInfoDTO getSubscribedAlertTypes()

Get the list of API Publisher alert types subscribed by the user. 

This operation is used to get the list of subscribed alert types by the user. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.AlertSubscriptionsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

AlertSubscriptionsApi apiInstance = new AlertSubscriptionsApi();
try {
    AlertsInfoDTO result = apiInstance.getSubscribedAlertTypes();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling AlertSubscriptionsApi#getSubscribedAlertTypes");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**AlertsInfoDTO**](AlertsInfoDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="subscribeToAlerts"></a>
# **subscribeToAlerts**
> AlertsInfoResponseDTO subscribeToAlerts(body)

Subscribe to the selected alert types by the user. 

This operation is used to get the list of subscribed alert types by the user. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.AlertSubscriptionsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

AlertSubscriptionsApi apiInstance = new AlertSubscriptionsApi();
AlertsInfoDTO body = new AlertsInfoDTO(); // AlertsInfoDTO | The alerts list and the email list to subscribe.
try {
    AlertsInfoResponseDTO result = apiInstance.subscribeToAlerts(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling AlertSubscriptionsApi#subscribeToAlerts");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**AlertsInfoDTO**](AlertsInfoDTO.md)| The alerts list and the email list to subscribe. |

### Return type

[**AlertsInfoResponseDTO**](AlertsInfoResponseDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="unsubscribeAllAlerts"></a>
# **unsubscribeAllAlerts**
> unsubscribeAllAlerts()

Unsubscribe user from all the alert types. 

This operation is used to unsubscribe the respective user from all the alert types. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.AlertSubscriptionsApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

AlertSubscriptionsApi apiInstance = new AlertSubscriptionsApi();
try {
    apiInstance.unsubscribeAllAlerts();
} catch (ApiException e) {
    System.err.println("Exception when calling AlertSubscriptionsApi#unsubscribeAllAlerts");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

