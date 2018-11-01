# 1.1.1 Create new version from exsiting API

## When to use this approach
When user have already running API and need to do some changes of that API without having impact to existing users.

## Sample use case
 To address this requirement first step we need to follow is create copy of existing API. Then user can add any additional details that we need to add here for modified API. Once API creator done with expected changes API can publish. So it will apppear in API store and users will be able to use that.


## Prerequisites
A REST client or cURL to invoke API. Or user can use API publisher web application to test entire flow.

## Development
Start the wso2am-2.6.0 distribution by executing [APIM_HOME]/bin/wso2server.sh or [APIM_HOME]/bin/wso2server.bat
Then user will need to login to API publisher as API creator.
Once logged in user will need to create API(Mobile_Stock_API). Initial version of this API can be version 1.0.0.
After creating this API, API publisher need to login to API publisher UI and publish newly created API.
Now once API subscriber logged in to API store(Developer console) that user should be able to see newly created API.
Now API publisher need to login to API publisher UI and create new version of existing API(Mobile_Stock_API Version 1.0.0). 
Newly created API version would be version 2.0.0. 

## Sample Configuration
No additional configuration or data to be added to servers.

## Deployment
API Manager 2.6.0 deployment required. No additional artifact or data to be added to servers.

## Testing and Acceptance Criteria
Once user followed above mentioned steps he should be able to see 2 versions of APIs at the same time. API with initial version(which is 1.0.0) should be in published state while newly created API is in created state. Once user published newly created version both APIs should appear in API. Except version all other attributes should be same as original API. To test this flow user can assert against new version and attributes of newly created API.

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

