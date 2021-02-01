/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.api.lifecycle;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Base test class for all API Manager lifecycle test cases. This class contents the all the
 * common variables and t methods.
 */
public class APIManagerLifecycleBaseTest extends APIMIntegrationBaseTest {
    private static final Log log = LogFactory.getLog(APIManagerLifecycleBaseTest.class);
    protected static final String CARBON_HOME = FrameworkPathUtil.getCarbonHome();
    protected static final int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_CREATED = Response.Status.CREATED.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_UNAUTHORIZED = Response.Status.UNAUTHORIZED.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE =
            Response.Status.SERVICE_UNAVAILABLE.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_TOO_MANY_REQUESTS = 429; // Define manually since value is not available in enum
    protected static final int HTTP_RESPONSE_CODE_FORBIDDEN = Response.Status.FORBIDDEN.getStatusCode();
    protected static final String HTTP_RESPONSE_DATA_API_BLOCK =
            "<am:code>700700</am:code><am:message>API blocked</am:message>";
    protected static final String HTTP_RESPONSE_DATA_NOT_FOUND =
            "<am:code>404</am:code><am:type>Status report</am:type><am:message>Not Found</am:message>";
    protected static final String HTTP_RESPONSE_DATA_API_FORBIDDEN =
            "<ams:code>900908</ams:code><ams:message>Resource forbidden </ams:message>";
    protected static final int GOLD_INVOCATION_LIMIT_PER_MIN = 20;
    protected static final int SILVER_INVOCATION_LIMIT_PER_MIN = 5;
    protected static final String TIER_UNLIMITED = "Unlimited";
    protected static final String TIER_GOLD = "Gold";
    protected static final String TIER_SILVER = "Silver";
    protected static final String MESSAGE_THROTTLED_OUT =
            "<amt:code>900800</amt:code><amt:message>Message throttled out</amt:message><amt:description>" +
                    "You have exceeded your quota</amt:description>";
    protected static final String MESSAGE_THROTTLED_OUT_RESOURCE =
            "<amt:code>900802</amt:code><amt:message>Message throttled out</amt:message><amt:description>" +
                    "You have exceeded your quota</amt:description>";
    protected static final int THROTTLING_UNIT_TIME = 60000;
    protected static final int THROTTLING_ADDITIONAL_WAIT_TIME = 5000;
    protected static final String API_NAME = "APITest";
    protected static final String API_CONTEXT = "{version}/api";
    protected static final String API_TAGS = "testTag1, testTag2, testTag3";
    protected static final String API_VERSION_1_0_0 = "1.0.0";
    protected static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    protected static final String APPLICATION_NAME = "ApplicationTest";

    /**
     * Return a String with combining the value of API Name,API Version and API Provider Name as key:value format
     *
     * @param apiIdentifier - Instance of APIIdentifier object  that include the  API Name,API Version and API Provider
     *                      Name to create the String
     * @return String - with API Name,API Version and API Provider Name as key:value format
     */
    protected String getAPIIdentifierString(APIIdentifier apiIdentifier) {
        return " API Name:" + apiIdentifier.getApiName() + " API Version:" + apiIdentifier.getVersion() +
                " API Provider Name :" + apiIdentifier.getProviderName() + " ";

    }

    /**
     * Return a String with combining the value of API Name,API Version and API Provider Name as key:value format
     *
     * @param apiRequest - Instance of APIRequest object  that include the  API Name,API Version and API Provider
     *                   Name to create the String
     * @return String - with API Name,API Version and API Provider Name as key:value format
     */
    protected String getAPIIdentifierStringFromAPIRequest(APIRequest apiRequest) {
        return " API Name:" + apiRequest.getName() + " API Version:" + apiRequest.getVersion() +
                " API Provider Name :" + apiRequest.getProvider() + " ";

    }

    /**
     * Subscribe  a API
     *
     * @param apiIdentifier   - Instance of APIIdentifier object  that include the  API Name,
     *                        API Version and API Provider
     * @param storeRestClient - Instance of APIPublisherRestClient
     * @return HttpResponse - Response of the API subscribe action
     * @throws APIManagerIntegrationTestException - Exception throws by the  method call of subscribe() in
     *                                            APIStoreRestClient.java
     */
    protected HttpResponse subscribeToAPI(APIIdentifier apiIdentifier, String applicationName,
                                          APIStoreRestClient storeRestClient) throws APIManagerIntegrationTestException {
        SubscriptionRequest subscriptionRequest =
                new SubscriptionRequest(apiIdentifier.getApiName(), apiIdentifier.getProviderName());
        subscriptionRequest.setVersion(apiIdentifier.getVersion());
        subscriptionRequest.setApplicationName(applicationName);
        if ((apiIdentifier.getTier() != null) && (!apiIdentifier.getTier().equals(""))) {
            subscriptionRequest.setTier(apiIdentifier.getTier());
        }
        return storeRestClient.subscribeToAPI(subscriptionRequest);
    }

    protected SubscriptionDTO subscribeToAPI(String apiID, String applicationID, String tier,
            RestAPIStoreImpl storeRestClient)
            throws org.wso2.am.integration.clients.store.api.ApiException {
        return storeRestClient.subscribeToAPI(apiID, applicationID, tier);
    }

    /**
     * Subscribe  a API
     *
     * @param apiId           - UUID of the API
     * @param applicationId   - UUID of the application
     * @param storeRestClient - Instance of APIPublisherRestClient
     * @return HttpResponse - Response of the API subscribe action
     * @throws APIManagerIntegrationTestException - Exception throws by the  method call of subscribe() in
     *                                            APIStoreRestClient.java
     */
    protected HttpResponse subscribeToAPIUsingRest(String apiId, String applicationId, String tier,
           RestAPIStoreImpl storeRestClient) throws APIManagerIntegrationTestException {
        return storeRestClient.createSubscription(apiId, applicationId, tier);
    }

    /**
     * Generate the access token
     *
     * @param storeRestClient - Instance of storeRestClient
     * @param applicationName - Application name
     * @return ApplicationKeyBean - ApplicationKeyBean that contains access token, consumer key and consumer secret
     * @throws APIManagerIntegrationTestException - Exception throws by the  method call of generateApplicationKey()
     *                                            in APIStoreRestClient.java
     */

    protected ApplicationKeyBean generateApplicationKeys(APIStoreRestClient storeRestClient, String applicationName)
            throws APIManagerIntegrationTestException {

        try {
            ApplicationKeyBean applicationKeyBean = new ApplicationKeyBean();
            APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(applicationName);
            String responseString = storeRestClient.generateApplicationKey(generateAppKeyRequest).getData();
            JSONObject response = new JSONObject(responseString);
            log.info("Token response: " + response.toString());
            applicationKeyBean.setAccessToken(response.getJSONObject("data").getJSONObject("key").
                    get("accessToken").toString());
            applicationKeyBean.setConsumerKey(response.getJSONObject("data").getJSONObject("key").
                    get("consumerKey").toString());
            applicationKeyBean.setConsumerSecret(response.getJSONObject("data").getJSONObject("key").
                    get("consumerSecret").toString());
            return applicationKeyBean;
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Exception when get access token", e);
        }

    }


    /**
     * Delete a API from API Publisher.
     *
     * @param apiIdentifier       - Instance of APIIdentifier object  that include the  API Name, API Version and
     *                            API Provider.
     * @param publisherRestClient - Instance of APIPublisherRestClient.
     * @throws APIManagerIntegrationTestException - Exception throws by the method call of deleteApi() in
     *                                            APIPublisherRestClient.java.
     */
    protected void deleteAPI(APIIdentifier apiIdentifier, APIPublisherRestClient publisherRestClient)
            throws APIManagerIntegrationTestException {

        HttpResponse deleteHTTPResponse =
                publisherRestClient.deleteAPI(apiIdentifier.getApiName(), apiIdentifier.getVersion(),
                        apiIdentifier.getProviderName());
        if (!(deleteHTTPResponse.getResponseCode() == HTTP_RESPONSE_CODE_OK &&
                getValueFromJSON(deleteHTTPResponse, "error").equals("false"))) {
            throw new APIManagerIntegrationTestException("Error in API Deletion." +
                    getAPIIdentifierString(apiIdentifier) + " API Context :" + deleteHTTPResponse +
                    "Response Code:" + deleteHTTPResponse.getResponseCode() +
                    " Response Data :" + deleteHTTPResponse.getData());
        }
    }

    /**
     *
     * @param apiID
     * @param publisherRestClient
     * @throws ApiException
     */
    protected void deleteAPI(String apiID, RestAPIPublisherImpl publisherRestClient) throws ApiException {
        publisherRestClient.deleteAPI(apiID);
    }

    /**
     * Retrieve  the value from JSON object bu using the key.
     *
     * @param httpResponse - Response that containing the JSON object in it response data.
     * @param key          - key of the JSON value the need to retrieve.
     * @return String - The value of provided key as a String
     * @throws APIManagerIntegrationTestException - Exception throws when resolving the JSON object in the HTTP response
     */
    protected String getValueFromJSON(HttpResponse httpResponse, String key) throws APIManagerIntegrationTestException {
        try {
            JSONObject jsonObject = new JSONObject(httpResponse.getData());
            return jsonObject.get(key).toString();
        } catch (JSONException e) {
            throw new APIManagerIntegrationTestException("Exception thrown when resolving the JSON object in the HTTP " +
                    "response ", e);
        }
    }

    /**
     * verify the API status change. this method will check the latest lifecycle status change
     * is correct according to the given old status and new status.
     *
     * @param httpResponse - Response returned in the the  API lifecycle status change action
     * @param oldStatus    - Status of the API before the change
     * @param newStatus    - Status of the API after the change
     * @return boolean - true if the given status change is correct, if not false
     * @throws APIManagerIntegrationTestException - Exception throws when resolving the JSON object in the HTTP response
     */
    public boolean verifyAPIStatusChange(HttpResponse httpResponse, APILifeCycleState oldStatus,
                                         APILifeCycleState newStatus) throws APIManagerIntegrationTestException {
        boolean isStatusChangeCorrect = false;
        try {
            JSONObject jsonRootObject = new JSONObject(httpResponse.getData());
            JSONArray jsonArray = (JSONArray) jsonRootObject.get("lcs");
            JSONObject latestChange = (JSONObject) jsonArray.get(0);
            // Retrieve the latest API life cycle status change information if  there are more than one
            // lifecycle status change activities  available in the api
            if (jsonArray.length() > 0) {
                for (int index = 1; index < jsonArray.length(); index++) {
                    if (Long.parseLong(((JSONObject) jsonArray.get(index)).get("date").toString()) >
                            Long.parseLong(latestChange.get("date").toString())) {
                        latestChange = (JSONObject) jsonArray.get(index);
                    }
                }
            }
            // Check the given status change information is correct in latest lifecycle status change action.
            if (latestChange.get("oldStatus").toString().equals(oldStatus.getState()) &&
                    latestChange.get("newStatus").toString().equals(newStatus.getState())) {
                isStatusChangeCorrect = true;
            }
            return isStatusChangeCorrect;
        } catch (JSONException e) {
            throw new APIManagerIntegrationTestException(
                    "Exception thrown when resolving the JSON object in the HTTP response to verify the status change." +
                            " HTTP response data: " + httpResponse.getData() + " HTTP response message: " +
                            httpResponse.getResponseMessage() + " HTTP response code: " + httpResponse.getResponseCode(), e);
        }
    }

    /**
     * Publish a API.
     *
     * @param apiIdentifier           - Instance of APIIdentifier object  that include the  API Name,
     *                                API Version and API Provider
     * @param publisherRestClient     - Instance of APIPublisherRestClient
     * @param isRequireReSubscription - If publish with re-subscription required option true else false.
     * @return HttpResponse - Response of the API Publishing activity
     * @throws APIManagerIntegrationTestException -  Exception throws by the method call of
     *                                            changeAPILifeCycleStatusToPublish() in APIPublisherRestClient.java.
     */
    protected HttpResponse publishAPI(APIIdentifier apiIdentifier, APIPublisherRestClient publisherRestClient,
                                      boolean isRequireReSubscription) throws APIManagerIntegrationTestException {
        APILifeCycleStateRequest publishUpdateRequest =
                new APILifeCycleStateRequest(apiIdentifier.getApiName(), apiIdentifier.getProviderName(),
                        APILifeCycleState.PUBLISHED_JAG);
        publishUpdateRequest.setVersion(apiIdentifier.getVersion());
        return publisherRestClient.changeAPILifeCycleStatusToPublish(apiIdentifier, isRequireReSubscription);

    }

    /**
     * @param apiID
     * @param publisherRestClient
     * @param isRequireReSubscription
     * @return
     * @throws APIManagerIntegrationTestException
     */
    protected HttpResponse publishAPI(String apiID, RestAPIPublisherImpl publisherRestClient,
            boolean isRequireReSubscription) throws APIManagerIntegrationTestException {
        try {
            return publisherRestClient.changeAPILifeCycleStatusToPublish(apiID, isRequireReSubscription);
        } catch (ApiException e) {
            throw new APIManagerIntegrationTestException("Error occurred while publishing API", e);
        }
    }

    /**
     * Publish an API using REST.
     *
     * @param apiId                   - UUID of the API,
     * @param publisherRestClient     - Instance of APIPublisherRestClient
     * @param isRequireReSubscription - If publish with re-subscription required option true else false.
     * @return HttpResponse - Response of the API Publishing activity
     * @throws APIManagerIntegrationTestException -  Exception throws by the method call of
     *                                            changeAPILifeCycleStatusToPublish() in APIPublisherRestClient.java.
     */
    protected HttpResponse publishAPIUsingRest(String apiId, RestAPIPublisherImpl publisherRestClient,
                                               boolean isRequireReSubscription) throws APIManagerIntegrationTestException, ApiException {
        String lifecycleChecklist = null;
        if (isRequireReSubscription) {
            lifecycleChecklist = "Requires re-subscription when publishing the API:true";
        }
        return publisherRestClient
                .changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), lifecycleChecklist);


    }

    /**
     * Create and publish a API.
     *
     * @param apiIdentifier           - Instance of APIIdentifier object  that include the  API Name,
     *                                API Version and API Provider
     * @param apiCreationRequestBean  - Instance of APICreationRequestBean with all needed API information
     * @param publisherRestClient     - Instance of APIPublisherRestClient
     * @param isRequireReSubscription - If publish with re-subscription required option true else false.
     * @throws APIManagerIntegrationTestException - Exception throws by API create and publish activities.
     */
    public void createAndPublishAPI(APIIdentifier apiIdentifier, APICreationRequestBean apiCreationRequestBean,
                                    APIPublisherRestClient publisherRestClient,
                                    boolean isRequireReSubscription) throws APIManagerIntegrationTestException {
        //Create the API
        HttpResponse createAPIResponse = publisherRestClient.addAPI(apiCreationRequestBean);
        if (createAPIResponse.getResponseCode() == HTTP_RESPONSE_CODE_OK &&
                getValueFromJSON(createAPIResponse, "error").equals("false")) {
            log.info("API Created :" + getAPIIdentifierString(apiIdentifier));
            // Create Revision and Deploy to Gateway
            try {
                createAPIRevisionAndDeployUsingRest(createAPIResponse.getData(), restAPIPublisher);
            } catch (JSONException | ApiException e) {
                throw new APIManagerIntegrationTestException("Error in creating and deploying API Revision", e);
            }
            //Publish the API
            HttpResponse publishAPIResponse = publishAPI(apiIdentifier, publisherRestClient, isRequireReSubscription);
            if (!(publishAPIResponse.getResponseCode() == HTTP_RESPONSE_CODE_OK &&
                    verifyAPIStatusChange(publishAPIResponse, APILifeCycleState.CREATED_JAG,
                            APILifeCycleState.PUBLISHED_JAG))) {
                throw new APIManagerIntegrationTestException("Error in API Publishing" +
                        getAPIIdentifierString(apiIdentifier) + "Response Code:" + publishAPIResponse.getResponseCode() +
                        " Response Data :" + publishAPIResponse.getData());
            }
            log.info("API Published :" + getAPIIdentifierString(apiIdentifier));
        } else {
            throw new APIManagerIntegrationTestException("Error in API Creation." +
                    getAPIIdentifierString(apiIdentifier) +
                    "Response Code:" + createAPIResponse.getResponseCode() +
                    " Response Data :" + createAPIResponse.getData());
        }
    }

    /**
     *
     * @param apiCreationRequestBean
     * @param publisherRestClient
     * @param isRequireReSubscription
     * @throws APIManagerIntegrationTestException
     * @throws ApiException
     */
    public APIDTO createAndPublishAPI(APICreationRequestBean apiCreationRequestBean,
            RestAPIPublisherImpl publisherRestClient, boolean isRequireReSubscription)
            throws APIManagerIntegrationTestException, ApiException {
        //Create the API
        APIDTO apidto = publisherRestClient.addAPI(apiCreationRequestBean);
        if (apidto != null) {
            log.info("API Created :" + apiCreationRequestBean.getName());
//            // Create Revision and Deploy to Gateway
//            try {
//                createAPIRevisionAndDeployUsingRest(apidto.getId(), publisherRestClient);
//            } catch (JSONException e) {
//                throw new APIManagerIntegrationTestException("Error in creating and deploying API Revision", e);
//            }
            //Publish the API
            HttpResponse publishAPIResponse = publishAPI(apidto.getId(), publisherRestClient, isRequireReSubscription);
            if (!(publishAPIResponse.getResponseCode() == HTTP_RESPONSE_CODE_OK)) {
                throw new APIManagerIntegrationTestException(
                        "Error in API Publishing" + apiCreationRequestBean.getName() + "Response Code:"
                                + publishAPIResponse.getResponseCode() + " Response Data :" + publishAPIResponse
                                .getData());
            }
            log.info("API Published :" + apiCreationRequestBean.getName());
            return apidto;
        } else {
            throw new APIManagerIntegrationTestException("Error in API Creation." + apiCreationRequestBean.getName());
        }
    }

    /**
     * Create and publish a API.
     *
     * @param apiRequest              - Instance of APIRequest
     * @param publisherRestClient     - Instance of RestAPIPublisherImpl
     * @param isRequireReSubscription - If publish with re-subscription required option true else false.
     * @throws APIManagerIntegrationTestException - Exception throws by API create and publish activities.
     */
    public String createAndPublishAPIUsingRest(APIRequest apiRequest,
                                               RestAPIPublisherImpl publisherRestClient,
                                               boolean isRequireReSubscription) throws APIManagerIntegrationTestException, ApiException {
        //Create the API
        HttpResponse createAPIResponse = publisherRestClient.addAPI(apiRequest);
        if (createAPIResponse.getResponseCode() == HTTP_RESPONSE_CODE_CREATED && !StringUtils.isEmpty(createAPIResponse.getData())) {
            log.info("API Created :" + getAPIIdentifierStringFromAPIRequest(apiRequest));
            // Create Revision and Deploy to Gateway
            try {
                createAPIRevisionAndDeployUsingRest(createAPIResponse.getData(), publisherRestClient);
            } catch (JSONException e) {
                throw new APIManagerIntegrationTestException("Error in creating and deploying API Revision", e);
            }
            //Publish the API
            HttpResponse publishAPIResponse = publishAPIUsingRest(createAPIResponse.getData(), publisherRestClient, isRequireReSubscription);
            if (!(publishAPIResponse.getResponseCode() == HTTP_RESPONSE_CODE_OK &&
                    APILifeCycleState.PUBLISHED.getState().equals(publishAPIResponse.getData()))) {
                throw new APIManagerIntegrationTestException("Error in API Publishing" +
                        getAPIIdentifierStringFromAPIRequest(apiRequest) + "Response Code:" + publishAPIResponse.getResponseCode() +
                        " Response Data :" + publishAPIResponse.getData());
            }
            log.info("API Published :" + getAPIIdentifierStringFromAPIRequest(apiRequest));
            return createAPIResponse.getData();
        } else {
            throw new APIManagerIntegrationTestException("Error in API Creation." +
                    getAPIIdentifierStringFromAPIRequest(apiRequest) +
                    "Response Code:" + createAPIResponse.getResponseCode() +
                    " Response Data :" + createAPIResponse.getData());
        }
    }

    /**
     * Create and publish a API with re-subscription not required.
     *
     * @param apiCreationRequestBean - Instance of APICreationRequestBean with all needed API information
     * @param publisherRestClient    - Instance of APIPublisherRestClient
     * @throws APIManagerIntegrationTestException - Exception throws by API create  and publish activities.
     */
    protected void createAndPublishAPIWithoutRequireReSubscription(APICreationRequestBean apiCreationRequestBean,
            RestAPIPublisherImpl publisherRestClient) throws APIManagerIntegrationTestException, ApiException {
        createAndPublishAPI(apiCreationRequestBean, publisherRestClient, false);
    }

    /**
     * Create and publish a API with re-subscription not required.
     *
     * @param apiRequest          - Instance of APIRequest
     * @param publisherRestClient - Instance of RestAPIPublisherImpl
     * @throws APIManagerIntegrationTestException - Exception throws by API create  and publish activities.
     */
    protected String createAndPublishAPIWithoutRequireReSubscriptionUsingRest(APIRequest apiRequest,
                                                                              RestAPIPublisherImpl publisherRestClient) throws APIManagerIntegrationTestException, ApiException {
        return createAndPublishAPIUsingRest(apiRequest, publisherRestClient, false);
    }


    /**
     * Copy and API and create a new version.
     *
     * @param apiIdentifier       - Instance of APIIdentifier object  that include the  API Name, API Version and API Provider
     * @param newAPIVersion       - New API version need to create
     * @param publisherRestClient - Instance of APIPublisherRestClient
     * @throws APIManagerIntegrationTestException - Exception throws by API copy activities.
     */
    protected void copyAPI(APIIdentifier apiIdentifier, String newAPIVersion,
                           APIPublisherRestClient publisherRestClient) throws APIManagerIntegrationTestException {
        //Copy API to version  to newVersion
        HttpResponse httpResponseCopyAPI =
                publisherRestClient.copyAPI(apiIdentifier.getProviderName(),
                        apiIdentifier.getApiName(), apiIdentifier.getVersion(), newAPIVersion, "");
        if (!(httpResponseCopyAPI.getResponseCode() == HTTP_RESPONSE_CODE_OK &&
                getValueFromJSON(httpResponseCopyAPI, "error").equals("false"))) {
            throw new APIManagerIntegrationTestException("Error in API Copy." +
                    getAPIIdentifierString(apiIdentifier) + "  New API Version :" + newAPIVersion +
                    "Response Code:" + httpResponseCopyAPI.getResponseCode() +
                    " Response Data :" + httpResponseCopyAPI.getData());
        }
    }

    /**
     * @param apiID
     * @param newAPIVersion
     * @param publisherRestClient
     * @throws APIManagerIntegrationTestException
     * @throws ApiException
     */
    protected APIDTO copyAPI(String apiID, String newAPIVersion, RestAPIPublisherImpl publisherRestClient)
            throws ApiException {
        //Copy API to version  to newVersion
        APIDTO apidto = publisherRestClient.copyAPIWithReturnDTO(newAPIVersion, apiID, false);
        return apidto;
    }

    /**
     * Copy and publish the copied API.
     *
     * @param newAPIVersion           - New API version need to create
     * @param publisherRestClient     - Instance of APIPublisherRestClient
     * @param isRequireReSubscription - If publish with re-subscription required option true else false.
     * @throws APIManagerIntegrationTestException -Exception throws by copyAPI() and publishAPI() method calls
     */
    protected void copyAndPublishCopiedAPI(String apiID, String newAPIVersion, RestAPIPublisherImpl publisherRestClient,
            boolean isRequireReSubscription) throws APIManagerIntegrationTestException, ApiException {
        APIDTO apidto = copyAPI(apiID, newAPIVersion, publisherRestClient);
        publishAPI(apidto.getId(), publisherRestClient, isRequireReSubscription);
    }

    /**
     * Create publish and subscribe a API.
     *
     * @param apiIdentifier          - Instance of APIIdentifier object  that include the  API Name,
     *                               API Version and API Provider
     * @param apiCreationRequestBean - Instance of APICreationRequestBean with all needed API information
     * @param publisherRestClient    -  Instance of APIPublisherRestClient
     * @param storeRestClient        - Instance of APIStoreRestClient
     * @param applicationName        - Name of the Application that the API need to subscribe.
     * @throws APIManagerIntegrationTestException - Exception throws by API create publish and subscribe a API activities.
     */
    protected void createPublishAndSubscribeToAPI(APIIdentifier apiIdentifier, APICreationRequestBean apiCreationRequestBean,
                                                  APIPublisherRestClient publisherRestClient,
                                                  APIStoreRestClient storeRestClient, String applicationName)
            throws APIManagerIntegrationTestException, XPathExpressionException {
        createAndPublishAPI(apiIdentifier, apiCreationRequestBean, publisherRestClient, false);
        waitForAPIDeploymentSync(user.getUserName(), apiIdentifier.getApiName(), apiIdentifier.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        HttpResponse httpResponseSubscribeAPI = subscribeToAPI(apiIdentifier, applicationName, storeRestClient);
        if (!(httpResponseSubscribeAPI.getResponseCode() == HTTP_RESPONSE_CODE_OK &&
                getValueFromJSON(httpResponseSubscribeAPI, "error").equals("false"))) {
            throw new APIManagerIntegrationTestException("Error in API Subscribe." +
                    getAPIIdentifierString(apiIdentifier) +
                    "Response Code:" + httpResponseSubscribeAPI.getResponseCode() +
                    " Response Data :" + httpResponseSubscribeAPI.getData());
        }
        log.info("API Subscribed :" + getAPIIdentifierString(apiIdentifier));
    }

    /**
     * @param apiIdentifier
     * @param apiCreationRequestBean
     * @param publisherRestClient
     * @param storeRestClient
     * @throws APIManagerIntegrationTestException
     * @throws ApiException
     */
    protected APIDTO createPublishAndSubscribeToAPI(APIIdentifier apiIdentifier,
            APICreationRequestBean apiCreationRequestBean, RestAPIPublisherImpl publisherRestClient,
            RestAPIStoreImpl storeRestClient, String applicationID, String tier)
            throws APIManagerIntegrationTestException, ApiException,
            org.wso2.am.integration.clients.store.api.ApiException, XPathExpressionException {
        APIDTO apidto = createAndPublishAPI(apiCreationRequestBean, publisherRestClient, false);
        waitForAPIDeploymentSync(user.getUserName(), apiIdentifier.getApiName(), apiIdentifier.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        SubscriptionDTO httpResponseSubscribeAPI = subscribeToAPI(apidto.getId(), applicationID, tier, storeRestClient);
        log.info("API Subscribed :" + getAPIIdentifierString(apiIdentifier));
        return apidto;
    }

    /**
     * Create publish and subscribe a API using REST API.
     *
     * @param apiRequest          - Instance of APIRequest with all needed API information
     * @param publisherRestClient -  Instance of APIPublisherRestClient
     * @param storeRestClient     - Instance of APIStoreRestClient
     * @param applicationId       - UUID of the Application that the API need to subscribe.
     * @param tier                - Tier that needs to be subscribed.
     * @throws APIManagerIntegrationTestException - Exception throws by API create publish and subscribe a API activities.
     */
    protected String createPublishAndSubscribeToAPIUsingRest(APIRequest apiRequest,
                                                             RestAPIPublisherImpl publisherRestClient, RestAPIStoreImpl storeRestClient, String applicationId,
                                                             String tier)
            throws APIManagerIntegrationTestException, ApiException, XPathExpressionException {
        String apiId = createAndPublishAPIUsingRest(apiRequest, publisherRestClient, false);
        waitForAPIDeploymentSync(user.getUserName(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        HttpResponse httpResponseSubscribeAPI = subscribeToAPIUsingRest(apiId, applicationId, tier, storeRestClient);
        if (!(httpResponseSubscribeAPI.getResponseCode() == HTTP_RESPONSE_CODE_OK &&
                !StringUtils.isEmpty(httpResponseSubscribeAPI.getData()))) {
            throw new APIManagerIntegrationTestException("Error in API Subscribe." +
                    getAPIIdentifierStringFromAPIRequest(apiRequest) +
                    "Response Code:" + httpResponseSubscribeAPI.getResponseCode());
        }
        log.info("API Subscribed :" + getAPIIdentifierStringFromAPIRequest(apiRequest));
        return apiId;
    }

    /**
     * Read the file content and return the content as String.
     *
     * @param fileLocation - Location of the file.
     * @return String - content of the file.
     * @throws APIManagerIntegrationTestException - exception throws when reading the file.
     */
    protected String readFile(String fileLocation) throws APIManagerIntegrationTestException {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(new File(fileLocation)));
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (IOException ioE) {
            throw new APIManagerIntegrationTestException("IOException when reading the file from:" + fileLocation, ioE);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    log.warn("Error when closing the buffer reade which used to reed the file:" + fileLocation +
                            ". Error:" + e.getMessage());
                }
            }
        }
    }

    /**
     * Delete a API from API Publisher.
     *
     * @param apiIdentifier       - Instance of APIIdentifier object that include the API Name, API Version and
     *                            API Provider.
     * @param publisherRestClient - Instance of RestAPIPublisherImpl.
     * @throws APIManagerIntegrationTestException - Exception throws by the method call of deleteApi() in
     *                                            APIPublisherRestClient.java.
     */
    protected void deleteAPI(String apiID, APIIdentifier apiIdentifier, RestAPIPublisherImpl publisherRestClient)
            throws APIManagerIntegrationTestException {

        try {
            HttpResponse deleteHTTPResponse = publisherRestClient.deleteAPI(apiID);
            if (!(deleteHTTPResponse.getResponseCode() == HTTP_RESPONSE_CODE_OK)) {
                throw new APIManagerIntegrationTestException("Error in API Deletion." +
                        getAPIIdentifierString(apiIdentifier) + " API Context :" + deleteHTTPResponse +
                        "Response Code:" + deleteHTTPResponse.getResponseCode() +
                        " Response Data :" + deleteHTTPResponse.getData());
            }
        } catch (ApiException e) {
            throw new APIManagerIntegrationTestException("Error when deleting API with ID: " + apiID, e);
        }
    }
}
