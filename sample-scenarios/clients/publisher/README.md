# org.wso2.carbon.apimgt.clients.publisher.api

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
    <artifactId>org.wso2.carbon.apimgt.clients.publisher.api</artifactId>
    <version>1.0.0</version>
    <scope>compile</scope>
</dependency>
```

### Gradle users

Add this dependency to your project's build file:

```groovy
compile "org.wso2.carbon.apimgt.samples:org.wso2.carbon.apimgt.clients.publisher.api:1.0.0"
```

### Others

At first generate the JAR by executing:

    mvn package

Then manually install the following JARs:

* target/org.wso2.carbon.apimgt.clients.publisher.api-1.0.0.jar
* target/lib/*.jar

## Getting Started

Please follow the [installation](#installation) instruction and execute the following Java code:

```java

import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.*;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.auth.*;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.model.*;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.APICollectionApi;

import java.io.File;
import java.util.*;

public class APICollectionApiExample {

    public static void main(String[] args) {
        
        APICollectionApi apiInstance = new APICollectionApi();
        Integer limit = 25; // Integer | Maximum size of resource array to return. 
        Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
        String query = "query_example"; // String | **Search condition**.  You can search in attributes by using an **\"<attribute>:\"** modifier.  Eg. \"provider:wso2\" will match an API if the provider of the API is exactly \"wso2\".  Additionally you can use wildcards.  Eg. \"provider:wso2*\" will match an API if the provider of the API starts with \"wso2\".  Supported attribute modifiers are [**version, context, status, description, subcontext, doc, provider**]  If no advanced attribute modifier has been specified,  the API names containing the search term will be returned as a result. 
        String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
        String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
        try {
            APIList result = apiInstance.apisGet(limit, offset, query, accept, ifNoneMatch);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling APICollectionApi#apisGet");
            e.printStackTrace();
        }
    }
}

```

## Documentation for API Endpoints

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v0.11*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*APICollectionApi* | [**apisGet**](docs/APICollectionApi.md#apisGet) | **GET** /apis | Retrieve/Search APIs 
*APICollectionApi* | [**apisPost**](docs/APICollectionApi.md#apisPost) | **POST** /apis | Create a new API
*APIIndividualApi* | [**apisApiIdDelete**](docs/APIIndividualApi.md#apisApiIdDelete) | **DELETE** /apis/{apiId} | Delete an API
*APIIndividualApi* | [**apisApiIdGet**](docs/APIIndividualApi.md#apisApiIdGet) | **GET** /apis/{apiId} | Get details of an API
*APIIndividualApi* | [**apisApiIdPut**](docs/APIIndividualApi.md#apisApiIdPut) | **PUT** /apis/{apiId} | Update an API
*APIIndividualApi* | [**apisApiIdSwaggerGet**](docs/APIIndividualApi.md#apisApiIdSwaggerGet) | **GET** /apis/{apiId}/swagger | Get swagger definition
*APIIndividualApi* | [**apisApiIdSwaggerPut**](docs/APIIndividualApi.md#apisApiIdSwaggerPut) | **PUT** /apis/{apiId}/swagger | Update swagger definition
*APIIndividualApi* | [**apisApiIdThumbnailGet**](docs/APIIndividualApi.md#apisApiIdThumbnailGet) | **GET** /apis/{apiId}/thumbnail | Get thumbnail image
*APIIndividualApi* | [**apisApiIdThumbnailPost**](docs/APIIndividualApi.md#apisApiIdThumbnailPost) | **POST** /apis/{apiId}/thumbnail | Upload a thumbnail image
*APIIndividualApi* | [**apisChangeLifecyclePost**](docs/APIIndividualApi.md#apisChangeLifecyclePost) | **POST** /apis/change-lifecycle | Change API Status
*APIIndividualApi* | [**apisCopyApiPost**](docs/APIIndividualApi.md#apisCopyApiPost) | **POST** /apis/copy-api | Create a new API version
*ApplicationIndividualApi* | [**applicationsApplicationIdGet**](docs/ApplicationIndividualApi.md#applicationsApplicationIdGet) | **GET** /applications/{applicationId} | Get details of an application
*DocumentCollectionApi* | [**apisApiIdDocumentsGet**](docs/DocumentCollectionApi.md#apisApiIdDocumentsGet) | **GET** /apis/{apiId}/documents | Get a list of documents of an API
*DocumentCollectionApi* | [**apisApiIdDocumentsPost**](docs/DocumentCollectionApi.md#apisApiIdDocumentsPost) | **POST** /apis/{apiId}/documents | Add a new document to an API
*DocumentIndividualApi* | [**apisApiIdDocumentsDocumentIdContentGet**](docs/DocumentIndividualApi.md#apisApiIdDocumentsDocumentIdContentGet) | **GET** /apis/{apiId}/documents/{documentId}/content | Get the content of an API document
*DocumentIndividualApi* | [**apisApiIdDocumentsDocumentIdContentPost**](docs/DocumentIndividualApi.md#apisApiIdDocumentsDocumentIdContentPost) | **POST** /apis/{apiId}/documents/{documentId}/content | Upload the content of an API document
*DocumentIndividualApi* | [**apisApiIdDocumentsDocumentIdDelete**](docs/DocumentIndividualApi.md#apisApiIdDocumentsDocumentIdDelete) | **DELETE** /apis/{apiId}/documents/{documentId} | Delete a document of an API
*DocumentIndividualApi* | [**apisApiIdDocumentsDocumentIdGet**](docs/DocumentIndividualApi.md#apisApiIdDocumentsDocumentIdGet) | **GET** /apis/{apiId}/documents/{documentId} | Get a document of an API
*DocumentIndividualApi* | [**apisApiIdDocumentsDocumentIdPut**](docs/DocumentIndividualApi.md#apisApiIdDocumentsDocumentIdPut) | **PUT** /apis/{apiId}/documents/{documentId} | Update a document of an API
*EnvironmentCollectionApi* | [**environmentsGet**](docs/EnvironmentCollectionApi.md#environmentsGet) | **GET** /environments | Get all gateway environments
*MediationPolicyCollectionApi* | [**apisApiIdPoliciesMediationGet**](docs/MediationPolicyCollectionApi.md#apisApiIdPoliciesMediationGet) | **GET** /apis/{apiId}/policies/mediation | Get all mediation policies of an API 
*MediationPolicyCollectionApi* | [**apisApiIdPoliciesMediationPost**](docs/MediationPolicyCollectionApi.md#apisApiIdPoliciesMediationPost) | **POST** /apis/{apiId}/policies/mediation | Add an API specific mediation policy
*MediationPolicyCollectionApi* | [**policiesMediationGet**](docs/MediationPolicyCollectionApi.md#policiesMediationGet) | **GET** /policies/mediation | Get all global level mediation policies 
*MediationPolicyIndividualApi* | [**apisApiIdPoliciesMediationMediationPolicyIdDelete**](docs/MediationPolicyIndividualApi.md#apisApiIdPoliciesMediationMediationPolicyIdDelete) | **DELETE** /apis/{apiId}/policies/mediation/{mediationPolicyId} | Delete an API specific mediation policy
*MediationPolicyIndividualApi* | [**apisApiIdPoliciesMediationMediationPolicyIdGet**](docs/MediationPolicyIndividualApi.md#apisApiIdPoliciesMediationMediationPolicyIdGet) | **GET** /apis/{apiId}/policies/mediation/{mediationPolicyId} | Get an API specific mediation policy
*MediationPolicyIndividualApi* | [**apisApiIdPoliciesMediationMediationPolicyIdPut**](docs/MediationPolicyIndividualApi.md#apisApiIdPoliciesMediationMediationPolicyIdPut) | **PUT** /apis/{apiId}/policies/mediation/{mediationPolicyId} | Update an API specific mediation policy
*SubscriptionCollectionApi* | [**subscriptionsGet**](docs/SubscriptionCollectionApi.md#subscriptionsGet) | **GET** /subscriptions | Get all Subscriptions
*SubscriptionIndividualApi* | [**subscriptionsBlockSubscriptionPost**](docs/SubscriptionIndividualApi.md#subscriptionsBlockSubscriptionPost) | **POST** /subscriptions/block-subscription | Block a subscription
*SubscriptionIndividualApi* | [**subscriptionsSubscriptionIdGet**](docs/SubscriptionIndividualApi.md#subscriptionsSubscriptionIdGet) | **GET** /subscriptions/{subscriptionId} | Get details of a subscription
*SubscriptionIndividualApi* | [**subscriptionsUnblockSubscriptionPost**](docs/SubscriptionIndividualApi.md#subscriptionsUnblockSubscriptionPost) | **POST** /subscriptions/unblock-subscription | Unblock a Subscription
*ThrottlingTierCollectionApi* | [**tiersTierLevelGet**](docs/ThrottlingTierCollectionApi.md#tiersTierLevelGet) | **GET** /tiers/{tierLevel} | Get all tiers
*ThrottlingTierCollectionApi* | [**tiersTierLevelPost**](docs/ThrottlingTierCollectionApi.md#tiersTierLevelPost) | **POST** /tiers/{tierLevel} | Create a Tier
*ThrottlingTierIndividualApi* | [**tiersTierLevelTierNameDelete**](docs/ThrottlingTierIndividualApi.md#tiersTierLevelTierNameDelete) | **DELETE** /tiers/{tierLevel}/{tierName} | Delete a Tier
*ThrottlingTierIndividualApi* | [**tiersTierLevelTierNameGet**](docs/ThrottlingTierIndividualApi.md#tiersTierLevelTierNameGet) | **GET** /tiers/{tierLevel}/{tierName} | Get details of a tier
*ThrottlingTierIndividualApi* | [**tiersTierLevelTierNamePut**](docs/ThrottlingTierIndividualApi.md#tiersTierLevelTierNamePut) | **PUT** /tiers/{tierLevel}/{tierName} | Update a Tier
*ThrottlingTierIndividualApi* | [**tiersUpdatePermissionPost**](docs/ThrottlingTierIndividualApi.md#tiersUpdatePermissionPost) | **POST** /tiers/update-permission | Update tier permission
*WorkflowsIndividualApi* | [**workflowsUpdateWorkflowStatusPost**](docs/WorkflowsIndividualApi.md#workflowsUpdateWorkflowStatusPost) | **POST** /workflows/update-workflow-status | Update workflow status
*WsdlIndividualApi* | [**apisApiIdWsdlGet**](docs/WsdlIndividualApi.md#apisApiIdWsdlGet) | **GET** /apis/{apiId}/wsdl | Get the WSDL of an API
*WsdlIndividualApi* | [**apisApiIdWsdlPost**](docs/WsdlIndividualApi.md#apisApiIdWsdlPost) | **POST** /apis/{apiId}/wsdl | Add a WSDL to an API


## Documentation for Models

 - [API](docs/API.md)
 - [APIBusinessInformation](docs/APIBusinessInformation.md)
 - [APICorsConfiguration](docs/APICorsConfiguration.md)
 - [APIEndpointSecurity](docs/APIEndpointSecurity.md)
 - [APIInfo](docs/APIInfo.md)
 - [APIList](docs/APIList.md)
 - [APIMaxTps](docs/APIMaxTps.md)
 - [Application](docs/Application.md)
 - [Document](docs/Document.md)
 - [DocumentList](docs/DocumentList.md)
 - [Environment](docs/Environment.md)
 - [EnvironmentEndpoints](docs/EnvironmentEndpoints.md)
 - [EnvironmentList](docs/EnvironmentList.md)
 - [Error](docs/Error.md)
 - [ErrorListItem](docs/ErrorListItem.md)
 - [ExtendedSubscription](docs/ExtendedSubscription.md)
 - [FileInfo](docs/FileInfo.md)
 - [Mediation](docs/Mediation.md)
 - [MediationInfo](docs/MediationInfo.md)
 - [MediationList](docs/MediationList.md)
 - [Sequence](docs/Sequence.md)
 - [Subscription](docs/Subscription.md)
 - [SubscriptionList](docs/SubscriptionList.md)
 - [Tier](docs/Tier.md)
 - [TierList](docs/TierList.md)
 - [TierPermission](docs/TierPermission.md)
 - [Workflow](docs/Workflow.md)
 - [Wsdl](docs/Wsdl.md)


## Documentation for Authorization

All endpoints do not require authorization.
Authentication schemes defined for the API:

## Recommendation

It's recommended to create an instance of `ApiClient` per thread in a multithreaded environment to avoid any potential issues.

## Author

architecture@wso2.com

