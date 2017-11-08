# org.wso2.carbon.apimgt.clients.store.api

## Requirements

Building the API client library requires [Maven](https://maven.apache.org/) to be installed.

## Installation

To install the API client library to your local Maven repository, simply execute:

```shell
mvn install
```

To deploy it to a remote Maven repository instead, configure the settings of the repository and execute:

```shell
mvn deploy
```

Refer to the [official documentation](https://maven.apache.org/plugins/maven-deploy-plugin/usage.html) for more information.

### Maven users

Add this dependency to your project's POM:

```xml
<dependency>
    <groupId>org.wso2.carbon.apimgt.samples</groupId>
    <artifactId>org.wso2.carbon.apimgt.clients.store.api</artifactId>
    <version>1.0.0</version>
    <scope>compile</scope>
</dependency>
```

### Gradle users

Add this dependency to your project's build file:

```groovy
compile "org.wso2.carbon.apimgt.samples:org.wso2.carbon.apimgt.clients.store.api:1.0.0"
```

### Others

At first generate the JAR by executing:

    mvn package

Then manually install the following JARs:

* target/org.wso2.carbon.apimgt.clients.store.api-1.0.0.jar
* target/lib/*.jar

## Getting Started

Please follow the [installation](#installation) instruction and execute the following Java code:

```java

import org.wso2.carbon.apimgt.samples.utils.store.rest.client.*;
import org.wso2.carbon.apimgt.samples.utils.store.rest.client.auth.*;
import org.wso2.carbon.apimgt.samples.utils.store.rest.client.model.*;
import org.wso2.carbon.apimgt.samples.utils.store.rest.client.api.APICollectionApi;

import java.io.File;
import java.util.*;

public class APICollectionApiExample {

    public static void main(String[] args) {
        
        APICollectionApi apiInstance = new APICollectionApi();
        Integer limit = 25; // Integer | Maximum size of resource array to return. 
        Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
        String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retirieved from. 
        String query = "query_example"; // String | **Search condition**.  You can search in attributes by using an **\"<attribute>:\"** modifier.  Eg. \"provider:wso2\" will match an API if the provider of the API is exactly \"wso2\".  Additionally you can use wildcards.  Eg. \"provider:wso2*\" will match an API if the provider of the API starts with \"wso2\".  Supported attribute modifiers are [**version, context, status, description, subcontext, doc, provider, tag**]  If no advanced attribute modifier has been specified, search will match the given query string against API Name. 
        String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
        String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource. 
        try {
            APIList result = apiInstance.apisGet(limit, offset, xWSO2Tenant, query, accept, ifNoneMatch);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling APICollectionApi#apisGet");
            e.printStackTrace();
        }
    }
}

```

## Documentation for API Endpoints

All URIs are relative to *https://apis.wso2.com/api/am/store/v0.11*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*APICollectionApi* | [**apisGet**](docs/APICollectionApi.md#apisGet) | **GET** /apis | Retrieve/Search APIs 
*APIIndividualApi* | [**apisApiIdGet**](docs/APIIndividualApi.md#apisApiIdGet) | **GET** /apis/{apiId} | Get details of an API 
*APIIndividualApi* | [**apisApiIdSwaggerGet**](docs/APIIndividualApi.md#apisApiIdSwaggerGet) | **GET** /apis/{apiId}/swagger | Get swagger definition 
*APIIndividualApi* | [**apisApiIdThumbnailGet**](docs/APIIndividualApi.md#apisApiIdThumbnailGet) | **GET** /apis/{apiId}/thumbnail | Get thumbnail image
*APIIndividualApi* | [**apisGenerateSdkPost**](docs/APIIndividualApi.md#apisGenerateSdkPost) | **POST** /apis/generate-sdk/ | Generate SDK for an API 
*ApplicationCollectionApi* | [**applicationsGet**](docs/ApplicationCollectionApi.md#applicationsGet) | **GET** /applications | Retrieve/Search applications 
*ApplicationIndividualApi* | [**applicationsApplicationIdDelete**](docs/ApplicationIndividualApi.md#applicationsApplicationIdDelete) | **DELETE** /applications/{applicationId} | Remove an application 
*ApplicationIndividualApi* | [**applicationsApplicationIdGet**](docs/ApplicationIndividualApi.md#applicationsApplicationIdGet) | **GET** /applications/{applicationId} | Get details of an application 
*ApplicationIndividualApi* | [**applicationsApplicationIdKeysKeyTypeGet**](docs/ApplicationIndividualApi.md#applicationsApplicationIdKeysKeyTypeGet) | **GET** /applications/{applicationId}/keys/{keyType} | Get key details of a given type 
*ApplicationIndividualApi* | [**applicationsApplicationIdKeysKeyTypePut**](docs/ApplicationIndividualApi.md#applicationsApplicationIdKeysKeyTypePut) | **PUT** /applications/{applicationId}/keys/{keyType} | Update grant types and callback url of an application 
*ApplicationIndividualApi* | [**applicationsApplicationIdPut**](docs/ApplicationIndividualApi.md#applicationsApplicationIdPut) | **PUT** /applications/{applicationId} | Update an application 
*ApplicationIndividualApi* | [**applicationsGenerateKeysPost**](docs/ApplicationIndividualApi.md#applicationsGenerateKeysPost) | **POST** /applications/generate-keys | Generate keys for application 
*ApplicationIndividualApi* | [**applicationsPost**](docs/ApplicationIndividualApi.md#applicationsPost) | **POST** /applications | Create a new application 
*ApplicationKeysApi* | [**applicationsApplicationIdKeysKeyTypeGet**](docs/ApplicationKeysApi.md#applicationsApplicationIdKeysKeyTypeGet) | **GET** /applications/{applicationId}/keys/{keyType} | Get key details of a given type 
*ApplicationKeysApi* | [**applicationsApplicationIdKeysKeyTypePut**](docs/ApplicationKeysApi.md#applicationsApplicationIdKeysKeyTypePut) | **PUT** /applications/{applicationId}/keys/{keyType} | Update grant types and callback url of an application 
*DocumentCollectionApi* | [**apisApiIdDocumentsGet**](docs/DocumentCollectionApi.md#apisApiIdDocumentsGet) | **GET** /apis/{apiId}/documents | Get a list of documents of an API 
*DocumentIndividualApi* | [**apisApiIdDocumentsDocumentIdContentGet**](docs/DocumentIndividualApi.md#apisApiIdDocumentsDocumentIdContentGet) | **GET** /apis/{apiId}/documents/{documentId}/content | Get the content of an API document 
*DocumentIndividualApi* | [**apisApiIdDocumentsDocumentIdGet**](docs/DocumentIndividualApi.md#apisApiIdDocumentsDocumentIdGet) | **GET** /apis/{apiId}/documents/{documentId} | Get a document of an API 
*SubscriptionCollectionApi* | [**subscriptionsGet**](docs/SubscriptionCollectionApi.md#subscriptionsGet) | **GET** /subscriptions | Get all subscriptions 
*SubscriptionIndividualApi* | [**subscriptionsPost**](docs/SubscriptionIndividualApi.md#subscriptionsPost) | **POST** /subscriptions | Add a new subscription 
*SubscriptionIndividualApi* | [**subscriptionsSubscriptionIdDelete**](docs/SubscriptionIndividualApi.md#subscriptionsSubscriptionIdDelete) | **DELETE** /subscriptions/{subscriptionId} | Remove a subscription 
*SubscriptionIndividualApi* | [**subscriptionsSubscriptionIdGet**](docs/SubscriptionIndividualApi.md#subscriptionsSubscriptionIdGet) | **GET** /subscriptions/{subscriptionId} | Get details of a subscription 
*SubscriptionMultitpleApi* | [**subscriptionsMultiplePost**](docs/SubscriptionMultitpleApi.md#subscriptionsMultiplePost) | **POST** /subscriptions/multiple | Add new subscriptions 
*TagCollectionApi* | [**tagsGet**](docs/TagCollectionApi.md#tagsGet) | **GET** /tags | Get all tags 
*ThrottlingTierCollectionApi* | [**tiersTierLevelGet**](docs/ThrottlingTierCollectionApi.md#tiersTierLevelGet) | **GET** /tiers/{tierLevel} | Get available tiers 
*ThrottlingTierIndividualApi* | [**tiersTierLevelTierNameGet**](docs/ThrottlingTierIndividualApi.md#tiersTierLevelTierNameGet) | **GET** /tiers/{tierLevel}/{tierName} | Get details of a tier 


## Documentation for Models

 - [API](docs/API.md)
 - [APIBusinessInformation](docs/APIBusinessInformation.md)
 - [APIEndpointURLs](docs/APIEndpointURLs.md)
 - [APIEnvironmentURLs](docs/APIEnvironmentURLs.md)
 - [APIInfo](docs/APIInfo.md)
 - [APIList](docs/APIList.md)
 - [Application](docs/Application.md)
 - [ApplicationInfo](docs/ApplicationInfo.md)
 - [ApplicationKey](docs/ApplicationKey.md)
 - [ApplicationKeyGenerateRequest](docs/ApplicationKeyGenerateRequest.md)
 - [ApplicationList](docs/ApplicationList.md)
 - [Document](docs/Document.md)
 - [DocumentList](docs/DocumentList.md)
 - [Error](docs/Error.md)
 - [ErrorListItem](docs/ErrorListItem.md)
 - [Subscription](docs/Subscription.md)
 - [SubscriptionList](docs/SubscriptionList.md)
 - [Tag](docs/Tag.md)
 - [TagList](docs/TagList.md)
 - [Tier](docs/Tier.md)
 - [TierList](docs/TierList.md)
 - [Token](docs/Token.md)


## Documentation for Authorization

All endpoints do not require authorization.
Authentication schemes defined for the API:

## Recommendation

It's recommended to create an instance of `ApiClient` per thread in a multithreaded environment to avoid any potential issues.

## Author

architecture@wso2.com

