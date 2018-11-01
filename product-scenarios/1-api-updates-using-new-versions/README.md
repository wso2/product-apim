# 01 Rollout web service / API updates seamlessly. 

## Business Use Case Narrative
Let's take a scenario where we need to push service update seamlessly without impacting existing users.  If we have some service which exposed as API to outside and there are active users for that API we cannot simply push changes to that API as it breaks existing clients. When API creator created updated version of same existing API(version 1.0.0) he can create new version(version 1.1.0) of API. This new API can have few additional resources than the original API(which is version 1.0.0). If any new user need to use newly added API resources that user need to use 1.1.0 version of the API. 

## Persona
API Publisher - API publisher is main role which is responsible for managing APIs 
API Creator - API creator responsible for create and modify APIs according changes we are suppose to rollout. 


## Sub Scenarios
When it comes to API versining there can be muliple users associated with that story. If we take this particular example API publisher will perform all API creating, versioning, create new version, manage lifecycles of existing APIs. Then API subscriber will be able see those actions perform by API publisher. For an example if publisher created new version (1.1.0) of exsting API version (1.0.0) then subscriber will see this new version and should be able to test it and subscribe. And with subscriber notification feature API publisher can notify all users of existing version 1.0.0. 

### [1.1 Use API versioning to rollout API updates and changes.](https://github.com/wso2/product-apim/tree/product-scenarios/product-scenarios/1-api-updates-using-new-versions/1.1-manage-api-versions)


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


