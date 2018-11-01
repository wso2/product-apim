# 1.1.2 Create new API version from exsiting API and let users to invoke it without version(Default API)

## When to use this approach
When user have already running multiple versions of API and need to do expose one API as default version. Which means users can invoke API without any version.

## Scenario
In API Manager users can invoke APIs without having any version. This version is called default version. When user running multiple API versions at same time he can mark one API as default API. So all APIs without version will direct to this default version. When users use API versioning to rollout API updates and changes they can create new version of existing API. 

## Prerequisites
User can use API publisher web application to test entire flow.

## Development
Start the wso2am-2.6.0 distribution by executing [APIM_HOME]/bin/wso2server.sh or [APIM_HOME]/bin/wso2server.bat
Then user will need to login to API publisher as API creator.
Once logged in user will need to create API(Mobile_Stock_API). Initial version of this API can be version 1.0.0.
After creating this API, API publisher need to login to API publisher UI and publish newly created API.
Now once API subscriber logged in to API store(Developer console) that user should be able to see newly created API.
Now API publisher need to login to API publisher UI and create new version of existing API(Mobile_Stock_API Version 1.0.0). 
Newly created API version would be version 2.0.0.
While creating this new version we can mark that API as default version. 
Then publish new version as well.

## Sample Configuration
No additional configuration or data to be added to servers.

## Deployment
API Manager 2.6.0 deployment required. No additional artifact or data to be added to servers.

## Testing and Acceptance Criteria
Once user followed above mentioned steps he should be able to see 2 versions of APIs at the same time. Now user can subscribe to both APIs from different applications and start consuming them. Then user can invoke API without having version in URL(with tokens obtained for both subscriptions). Now user will be able to see second API invocation success. That means when invoke API without version it dispatched to version 2.0.0 

## API Reference
Users can create a new API version of API via publisher user interface or REST API. Following is the base URL of API copy resource. Users can invoke this API and create copy of existing API with different version.
```
POST https://apis.wso2.com/api/am/publisher/v0.14/apis/copy-api
```

OAuth 2.0 Scope
```
apim:api_create
```

Sample Request The new version is specified as newVersion query parameter. New API will be in CREATED state and API publisher need to publish it manually or via API call.
```
POST https://localhost:9443/api/am/publisher/v0.14/apis/copy-api?apiId=890a4f4d-09eb-4877-a323-57f6ce2ed79b&newVersion=2.0.0 Authorization: Bearer ae4eae22-3f65-387b-a171-d37eaa366fa8'
```

## See Also
As discussed above when new API version got created we might need to send notifications to subscribers or another external party. In that case users can effectively use API notification feature. Please view documentation[1] for API notification feature.
[1] - https://docs.wso2.com/display/AM260/Enabling+Notifications

