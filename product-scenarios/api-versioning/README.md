# API versioning
## Introduction
API versioning is a key functionality that needs to be addressed when the users, who are using a specific API for a long time. When introducing the new version of API the old API should also be supported for a specific time period. The time period is offered to the users of the old API to modify the availability of the new API and to make the transition. When the given time period ends, the old API can be retired. This feature allows users to manage multiple versions of same API at a given time.

## Buisiness Usecase Narrative
Lets take a scenario where user runs multiple version of same APIs in same runtime. When API creator created updated version of same existing API(version 1.0.0) he may need to create new version(version 1.1.0) of API. This new API might have few additional resources than the original API(which is version 1.0.0). If any new user need to use newly added API resources that user need to use 1.1.0 version of the API. Also in addition to that there might be some other use cases as well. Please see below business requirements. 
* Ability to retire old API(version 1.0.0) and introduce new versions(1.1.0, 1.1.1 etc) of API to enhance its functionality
* Ensure that API updates don’t break when upgraded/versioned or moved between environments, geographies, data centers and the cloud
* A/B testing with old vs new APIs
* Ability to notify consumers of the old version(version 1.0.0) about the availability of the new API version(whenever version 1.1.0 available to use)
* Enforcing a grace period to upgrade to the new version of the API
* Transferring contracts with app developers to newer versions.

## Persona
When it comes to API versining there can be muliple users associated with that story. If we take this particular example API publisher will perform all API creating, versioning, create new version, manage lifecycles of existing APIs. Then API subscriber will be able see those actions perform by API publisher. For an example if publisher created new version (1.1.0) of exsting API version (1.0.0) then subscriber will see this new version and should be able to test it and subscribe. And with subscriber notification feature API publisher can notify all users of existing version 1.0.0.

## Implementation
ABC company is a mobile phone manufacturing company. They have a requirement to publish mobile phone prices through an API. When the industry grows with the prices, they need to publish some additional data such as a rating, user reviews of the mobile phones, etc. Users of the old API should know that there is a new API version released, and they need to be notified.
* We need an API to expose the mobile phone details of ABC company which will stand as the Old API
* We need to a new version of the above mentioned API to be state as the new API.
* We need the old API to be in published state and then once the new API version is created Old API should be in DEPRECATED state and the new versioned API should be in Published state.
* Subscribers of the Old API needs to get notified when the new API version is PUBLISHED.

### Prerequisites
In order to test this scenario we will need to have API Manager 2.6.0 deployment. This deployment can be all in one node deployment or it can be a distributed API Manager deployment. Since provided sample is shell script user who test this flow should be able to execute shell script.

### Development 
* Start the wso2am-2.6.0 distribution by executing [APIM_HOME]/bin/wso2server.sh or [APIM_HOME]/bin/wso2server.bat
* Then user will need to login to API publisher as API creator.
* Once logged in user will need to create API(Mobile_Stock_API). Initial version of this API can be version 1.0.0.
* After creating this API, API publisher need to log in to API publisher UI and publish newly created API.
* Now once API subscriber logged in to API store(Developer console) that user should be able to see newly created API.
* Now API publisher need to login to API publisher UI and create new version of existing API(Mobile_Stock_API Version 1.0.0). Newly created API version would be version 2.0.0. When new version of API create user can mandate re subscription to new version. Also publisher have capability to make new version of API default version.
* Now we have both version of API running at the same time and when subscriber login to API store he should be able to see both versions.
* Lets assume now we need to enforce users to only use version 2.0.0 of API. At this point API publisher can move version 1.0.0 to deprecated state. With that existing users will be able to use API. But any new users will not be able to subscribe API.
* After sometime we will need to completely retire version 1.0.0 of the API. So at this point publisher can move this API to retired state.

### Sample Configuration
No additional configuration or data to be added to servers.

### Deployment
API Manager 2.6.0 deployment required. No additional artifact or data to be added to servers.

### Testing and Acceptance Criteria
Once user followed above mentioned steps he should be able to see 2 versions of APIs at the same time. Then after deprecating version 1.0.0 it should appear as deprecated API in publisher UI and in store users should not allowed to subscribe to same API. After moving version 1.0.0 to retired state it will not appear on API store and users will not be able to use that.
When API publisher create new version, if he selected require resubscription then exisiting users will not be able to ues it without new subscription. And if published selected mark as default API then newly created API version will be default API(which means if someone invoke API without any version then they will routed to this API)

Below are the screenshots that shows the OLD and New API’s with there lifecycle states.

Old API version(1.0.0.) in deprecated mode while new API(version 2.0.0) in published mode.
![](images/image_0.png)

Old API in DEPRICATED state

![](images/image_1.png)

Old API in PUBLISHED state

![](images/image_2.png)

## API Reference
Users can create a new API version of API via publisher user interface or REST API. Following is the base URL of API copy resource. Users can invoke this API and create copy of existing API with different version.

``` 
POST https://apis.wso2.com/api/am/publisher/v0.14/apis/copy-api
```

OAuth 2.0 Scope
``` 
apim:api_create
```

Sample Request 
The new version is specified as newVersion query parameter. New API will be in CREATED state and API publisher need to publish it manually or via API call.
```
POST https://localhost:9443/api/am/publisher/v0.14/apis/copy-api?apiId=890a4f4d-09eb-4877-a323-57f6ce2ed79b&newVersion=2.0.0 Authorization: Bearer ae4eae22-3f65-387b-a171-d37eaa366fa8'
```

## See Also
As discussed above when new API version got created we might need to send notifications to subscribers or another external party. In that case users can effectively use API notification feature. Please view documentation[1] for API notification feature.

[1] - https://docs.wso2.com/display/AM260/Enabling+Notifications


