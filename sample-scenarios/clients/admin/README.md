# org.wso2.carbon.apimgt.clients.admin.api

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
    <artifactId>org.wso2.carbon.apimgt.clients.admin.api</artifactId>
    <version>1.0.0</version>
    <scope>compile</scope>
</dependency>
```

### Gradle users

Add this dependency to your project's build file:

```groovy
compile "org.wso2.carbon.apimgt.samples:org.wso2.carbon.apimgt.clients.admin.api:1.0.0"
```

### Others

At first generate the JAR by executing:

    mvn package

Then manually install the following JARs:

* target/org.wso2.carbon.apimgt.clients.admin.api-1.0.0.jar
* target/lib/*.jar

## Getting Started

Please follow the [installation](#installation) instruction and execute the following Java code:

```java

import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.*;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.auth.*;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.model.*;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.AdvancedPolicyCollectionApi;

import java.io.File;
import java.util.*;

public class AdvancedPolicyCollectionApiExample {

    public static void main(String[] args) {
        
        AdvancedPolicyCollectionApi apiInstance = new AdvancedPolicyCollectionApi();
        String accept = "application/json"; // String | Media types acceptable for the response. Default is application/json. 
        String ifNoneMatch = "ifNoneMatch_example"; // String | Validator for conditional requests; based on the ETag of the formerly retrieved variant of the resource (Will be supported in future). 
        String ifModifiedSince = "ifModifiedSince_example"; // String | Validator for conditional requests; based on Last Modified header of the formerly retrieved variant of the resource (Will be supported in future). 
        try {
            AdvancedThrottlePolicyList result = apiInstance.throttlingPoliciesAdvancedGet(accept, ifNoneMatch, ifModifiedSince);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling AdvancedPolicyCollectionApi#throttlingPoliciesAdvancedGet");
            e.printStackTrace();
        }
    }
}

```

## Documentation for API Endpoints

All URIs are relative to *https://apis.wso2.com/api/am/admin/v0.11*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*AdvancedPolicyCollectionApi* | [**throttlingPoliciesAdvancedGet**](docs/AdvancedPolicyCollectionApi.md#throttlingPoliciesAdvancedGet) | **GET** /throttling/policies/advanced | Get all Advanced throttling policies.
*AdvancedPolicyCollectionApi* | [**throttlingPoliciesAdvancedPost**](docs/AdvancedPolicyCollectionApi.md#throttlingPoliciesAdvancedPost) | **POST** /throttling/policies/advanced | Add an Advanced Throttling Policy
*AdvancedPolicyIndividualApi* | [**throttlingPoliciesAdvancedPolicyIdDelete**](docs/AdvancedPolicyIndividualApi.md#throttlingPoliciesAdvancedPolicyIdDelete) | **DELETE** /throttling/policies/advanced/{policyId} | Delete an Advanced Throttling Policy
*AdvancedPolicyIndividualApi* | [**throttlingPoliciesAdvancedPolicyIdGet**](docs/AdvancedPolicyIndividualApi.md#throttlingPoliciesAdvancedPolicyIdGet) | **GET** /throttling/policies/advanced/{policyId} | Get an Advanced Policy
*AdvancedPolicyIndividualApi* | [**throttlingPoliciesAdvancedPolicyIdPut**](docs/AdvancedPolicyIndividualApi.md#throttlingPoliciesAdvancedPolicyIdPut) | **PUT** /throttling/policies/advanced/{policyId} | Update an Advanced Throttling Policy
*ApplicationPolicyCollectionApi* | [**throttlingPoliciesApplicationGet**](docs/ApplicationPolicyCollectionApi.md#throttlingPoliciesApplicationGet) | **GET** /throttling/policies/application | Get all Application Throttling Policies
*ApplicationPolicyCollectionApi* | [**throttlingPoliciesApplicationPost**](docs/ApplicationPolicyCollectionApi.md#throttlingPoliciesApplicationPost) | **POST** /throttling/policies/application | Add an Application Throttling Policy
*ApplicationPolicyIndividualApi* | [**throttlingPoliciesApplicationPolicyIdDelete**](docs/ApplicationPolicyIndividualApi.md#throttlingPoliciesApplicationPolicyIdDelete) | **DELETE** /throttling/policies/application/{policyId} | Delete an Application Throttling policy
*ApplicationPolicyIndividualApi* | [**throttlingPoliciesApplicationPolicyIdGet**](docs/ApplicationPolicyIndividualApi.md#throttlingPoliciesApplicationPolicyIdGet) | **GET** /throttling/policies/application/{policyId} | Get an Application Policy
*ApplicationPolicyIndividualApi* | [**throttlingPoliciesApplicationPolicyIdPut**](docs/ApplicationPolicyIndividualApi.md#throttlingPoliciesApplicationPolicyIdPut) | **PUT** /throttling/policies/application/{policyId} | Update an Application Throttling policy
*BlacklistCollectionApi* | [**throttlingBlacklistGet**](docs/BlacklistCollectionApi.md#throttlingBlacklistGet) | **GET** /throttling/blacklist | Get all blocking condtions
*BlacklistCollectionApi* | [**throttlingBlacklistPost**](docs/BlacklistCollectionApi.md#throttlingBlacklistPost) | **POST** /throttling/blacklist | Add a Blocking condition
*BlacklistIndividualApi* | [**throttlingBlacklistConditionIdDelete**](docs/BlacklistIndividualApi.md#throttlingBlacklistConditionIdDelete) | **DELETE** /throttling/blacklist/{conditionId} | Delete a Blocking condition
*BlacklistIndividualApi* | [**throttlingBlacklistConditionIdGet**](docs/BlacklistIndividualApi.md#throttlingBlacklistConditionIdGet) | **GET** /throttling/blacklist/{conditionId} | Get a Blocking Condition
*CustomRulesCollectionApi* | [**throttlingPoliciesCustomGet**](docs/CustomRulesCollectionApi.md#throttlingPoliciesCustomGet) | **GET** /throttling/policies/custom | Get all Custom Rules
*CustomRulesCollectionApi* | [**throttlingPoliciesCustomPost**](docs/CustomRulesCollectionApi.md#throttlingPoliciesCustomPost) | **POST** /throttling/policies/custom | Add a Custom Rule
*CustomRulesIndividualApi* | [**throttlingPoliciesCustomRuleIdDelete**](docs/CustomRulesIndividualApi.md#throttlingPoliciesCustomRuleIdDelete) | **DELETE** /throttling/policies/custom/{ruleId} | Delete a Custom Rule
*CustomRulesIndividualApi* | [**throttlingPoliciesCustomRuleIdGet**](docs/CustomRulesIndividualApi.md#throttlingPoliciesCustomRuleIdGet) | **GET** /throttling/policies/custom/{ruleId} | Get a Custom Rule
*CustomRulesIndividualApi* | [**throttlingPoliciesCustomRuleIdPut**](docs/CustomRulesIndividualApi.md#throttlingPoliciesCustomRuleIdPut) | **PUT** /throttling/policies/custom/{ruleId} | Update a Custom Rule
*MediationPolicyCollectionApi* | [**policiesMediationGet**](docs/MediationPolicyCollectionApi.md#policiesMediationGet) | **GET** /policies/mediation | Get all global mediation policies 
*MediationPolicyCollectionApi* | [**policiesMediationPost**](docs/MediationPolicyCollectionApi.md#policiesMediationPost) | **POST** /policies/mediation | Add a global mediation policy
*MediationPolicyIndividualApi* | [**policiesMediationMediationPolicyIdDelete**](docs/MediationPolicyIndividualApi.md#policiesMediationMediationPolicyIdDelete) | **DELETE** /policies/mediation/{mediationPolicyId} | Delete a global mediation policy
*MediationPolicyIndividualApi* | [**policiesMediationMediationPolicyIdGet**](docs/MediationPolicyIndividualApi.md#policiesMediationMediationPolicyIdGet) | **GET** /policies/mediation/{mediationPolicyId} | Get a global mediation policy
*MediationPolicyIndividualApi* | [**policiesMediationMediationPolicyIdPut**](docs/MediationPolicyIndividualApi.md#policiesMediationMediationPolicyIdPut) | **PUT** /policies/mediation/{mediationPolicyId} | Update a global mediation policy
*SubscriptionPolicyCollectionApi* | [**throttlingPoliciesSubscriptionGet**](docs/SubscriptionPolicyCollectionApi.md#throttlingPoliciesSubscriptionGet) | **GET** /throttling/policies/subscription | Get all Subscription Throttling Policies
*SubscriptionPolicyCollectionApi* | [**throttlingPoliciesSubscriptionPost**](docs/SubscriptionPolicyCollectionApi.md#throttlingPoliciesSubscriptionPost) | **POST** /throttling/policies/subscription | Add a Subscription Throttling Policy
*SubscriptionPolicyIndividualApi* | [**throttlingPoliciesSubscriptionPolicyIdDelete**](docs/SubscriptionPolicyIndividualApi.md#throttlingPoliciesSubscriptionPolicyIdDelete) | **DELETE** /throttling/policies/subscription/{policyId} | Delete a Subscription Policy
*SubscriptionPolicyIndividualApi* | [**throttlingPoliciesSubscriptionPolicyIdGet**](docs/SubscriptionPolicyIndividualApi.md#throttlingPoliciesSubscriptionPolicyIdGet) | **GET** /throttling/policies/subscription/{policyId} | Get a Subscription Policy
*SubscriptionPolicyIndividualApi* | [**throttlingPoliciesSubscriptionPolicyIdPut**](docs/SubscriptionPolicyIndividualApi.md#throttlingPoliciesSubscriptionPolicyIdPut) | **PUT** /throttling/policies/subscription/{policyId} | Update a Subscription Policy


## Documentation for Models

 - [AdvancedThrottlePolicy](docs/AdvancedThrottlePolicy.md)
 - [AdvancedThrottlePolicyInfo](docs/AdvancedThrottlePolicyInfo.md)
 - [AdvancedThrottlePolicyList](docs/AdvancedThrottlePolicyList.md)
 - [ApplicationThrottlePolicy](docs/ApplicationThrottlePolicy.md)
 - [ApplicationThrottlePolicyList](docs/ApplicationThrottlePolicyList.md)
 - [BandwidthLimit](docs/BandwidthLimit.md)
 - [BlockingCondition](docs/BlockingCondition.md)
 - [BlockingConditionList](docs/BlockingConditionList.md)
 - [ConditionalGroup](docs/ConditionalGroup.md)
 - [CustomAttribute](docs/CustomAttribute.md)
 - [CustomRule](docs/CustomRule.md)
 - [CustomRuleList](docs/CustomRuleList.md)
 - [Error](docs/Error.md)
 - [ErrorListItem](docs/ErrorListItem.md)
 - [HeaderCondition](docs/HeaderCondition.md)
 - [IPCondition](docs/IPCondition.md)
 - [JWTClaimsCondition](docs/JWTClaimsCondition.md)
 - [Mediation](docs/Mediation.md)
 - [MediationInfo](docs/MediationInfo.md)
 - [MediationList](docs/MediationList.md)
 - [QueryParameterCondition](docs/QueryParameterCondition.md)
 - [RequestCountLimit](docs/RequestCountLimit.md)
 - [SubscriptionThrottlePolicy](docs/SubscriptionThrottlePolicy.md)
 - [SubscriptionThrottlePolicyList](docs/SubscriptionThrottlePolicyList.md)
 - [ThrottleCondition](docs/ThrottleCondition.md)
 - [ThrottleLimit](docs/ThrottleLimit.md)
 - [ThrottlePolicy](docs/ThrottlePolicy.md)


## Documentation for Authorization

All endpoints do not require authorization.
Authentication schemes defined for the API:

## Recommendation

It's recommended to create an instance of `ApiClient` per thread in a multithreaded environment to avoid any potential issues.

## Author

architecture@wso2.com

