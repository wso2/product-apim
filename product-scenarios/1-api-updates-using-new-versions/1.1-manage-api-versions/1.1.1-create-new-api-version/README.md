# 1.1.1 Create new version from exsiting API

## When to use this approach
When user have already running API and need to do some changes of that API first step would be create copy of existing API. Then user can add any additional details that we need to add here for modified API. Once API creator done with expected changes API can publish. So it will apppear in API store and users will be able to use that.

## Sample use case
Exposing a SOAP web service as a REST API by doing a SOAP to JSON conversion.

## Prerequisites
A REST client like cURL to invoke the ESB API.

## Development
Start the wso2am-2.6.0 distribution by executing [APIM_HOME]/bin/wso2server.sh or [APIM_HOME]/bin/wso2server.bat
Then user will need to login to API publisher as API creator.
Once logged in user will need to create API(Mobile_Stock_API). Initial version of this API can be version 1.0.0.
After creating this API, API publisher need to login to API publisher UI and publish newly created API.
Now once API subscriber logged in to API store(Developer console) that user should be able to see newly created API.
Now API publisher need to login to API publisher UI and create new version of existing API(Mobile_Stock_API Version 1.0.0). Newly created API version would be version 2.0.0. When new version of API create user can mandate re subscription to new version. Also publisher have capability to make new version of API default version.
Now we have both version of API running at the same time and when subscriber login to API store he should be able to see both versions.
Let's assume now we need to enforce users to only use version 2.0.0 of API. At this point API publisher can move version 1.0.0 to deprecated state. With that existing users will be able to use API. But any new users will not be able to subscribe API.
After sometime we will need to completely retire version 1.0.0 of the API. So at this point publisher can move this API to retired state.

## Sample Configuration
No additional configuration or data to be added to servers.

## Deployment
API Manager 2.6.0 deployment required. No additional artifact or data to be added to servers.

## Testing and Acceptance Criteria
Once user followed above mentioned steps he should be able to see 2 versions of APIs at the same time. Then after deprecating version 1.0.0 it should appear as deprecated API in publisher UI and in store users should not allowed to subscribe to same API. After moving version 1.0.0 to retired state it will not appear on API store and users will not be able to use that. When API publisher create new version, if he selected require re subscription then existing users will not be able to use it without new

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

Sample Request The new version is specified as newVersion query parameter. New API will be in CREATED state and API publisher need to publish it manually or via API call.
```
POST https://localhost:9443/api/am/publisher/v0.14/apis/copy-api?apiId=890a4f4d-09eb-4877-a323-57f6ce2ed79b&newVersion=2.0.0 Authorization: Bearer ae4eae22-3f65-387b-a171-d37eaa366fa8'
```

## See Also
As discussed above when new API version got created we might need to send notifications to subscribers or another external party. In that case users can effectively use API notification feature. Please view documentation[1] for API notification feature.
[1] - https://docs.wso2.com/display/AM260/Enabling+Notifications

