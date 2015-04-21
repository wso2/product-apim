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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Base test class for all API Manager lifecycle test cases. This class contents the all the
 * common variables and t methods.
 */
public class APIManagerLifecycleBaseTest extends APIMIntegrationBaseTest {

    protected static final int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_UNAUTHORIZED = Response.Status.UNAUTHORIZED.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE =
            Response.Status.SERVICE_UNAVAILABLE.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_FORBIDDEN = Response.Status.FORBIDDEN.getStatusCode();

    protected static final String HTTP_RESPONSE_DATA_API_BLOCK =
            "<am:code>700700</am:code><am:message>API blocked</am:message>";
    protected static final String HTTP_RESPONSE_DATA_INVALID_CREDENTIALS =
            "<ams:message>Invalid Credentials</ams:message>";
    protected static final String HTTP_RESPONSE_DATA_NOT_FOUND =
            "<am:code>404</am:code><am:type>Status report</am:type><am:message>Not Found</am:message>";
    protected static final int GOLD_INVOCATION_LIMIT_PER_MIN = 20;
    protected static final int SILVER_INVOCATION_LIMIT_PER_MIN = 5;
    protected static final String TIER_GOLD = "Gold";
    protected static final String TIER_SILVER = "Silver";
    protected static final String MESSAGE_THROTTLED_OUT =
            "<amt:code>900800</amt:code><amt:message>Message Throttled Out</amt:message><amt:description>" +
                    "You have exceeded your quota</amt:description>";
    protected static final int THROTTLING_UNIT_TIME = 60000;
    protected static final int THROTTLING_ADDITIONAL_WAIT_TIME = 5000;
    protected static String API_BASE_URL;

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {
        super.init();


        API_BASE_URL = gatewayUrls.getWebAppURLNhttp();


    }


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
     * Subscribe  a API
     *
     * @param apiIdentifier   - Instance of APIIdentifier object  that include the  API Name,
     *                        API Version and API Provider
     * @param storeRestClient - Instance of APIPublisherRestClient
     * @return HttpResponse - Response of the API subscribe action
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Exception throws by the  method call of subscribe()
     *                                            in APIStoreRestClient.java
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


    /**
     * Generate the access token
     *
     * @param storeRestClient - Instance of storeRestClient
     * @param applicationName - Application name
     * @return String - Access Token as a String.
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Exception throws by the  method
     *                                                                               call of generateApplicationKey()
     *                                                                               in APIStoreRestClient.java
     */
    protected String getAccessToken(APIStoreRestClient storeRestClient, String applicationName)
            throws APIManagerIntegrationTestException {
        try {
            APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(applicationName);
            String responseString = storeRestClient.generateApplicationKey(generateAppKeyRequest).getData();
            JSONObject response = new JSONObject(responseString);
            return response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Exception when get access token", e);
        }

    }

    /**
     * Delete a API from API Publisher.
     *
     * @param apiIdentifier       - Instance of APIIdentifier object  that include the  API Name,
     *                            API Version and API Provider.
     * @param publisherRestClient - Instance of APIPublisherRestClient.
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Exception throws by the method
     *                                                                               call of deleteApi()
     *                                                                               in APIPublisherRestClient.java.
     */
    protected void deleteAPI(APIIdentifier apiIdentifier, APIPublisherRestClient publisherRestClient)
            throws APIManagerIntegrationTestException {
        try {
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

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Exception when delete a API", e);
        }
    }

    /**
     * Retrieve  the value from JSON object bu using the key.
     *
     * @param httpResponse - Response that containing the JSON object in it response data.
     * @param key          - key of the JSON value the need to retrieve.
     * @return String - The value of provided key as a String
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Exception throws when
     *                                                                               resolving the JSON object in the HTTP response
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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Exception throws when resolving
     *                                                                               the JSON object in the HTTP response
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
                    "Exception thrown when resolving the JSON object in the HTTP response ", e);
        }

    }

    /**
     * Publish a API.
     *
     * @param apiIdentifier           - Instance of APIIdentifier object  that include the  API Name,v
     *                                API Version and API Provider
     * @param publisherRestClient     - Instance of APIPublisherRestClient
     * @param isRequireReSubscription - If publish with re-subscription required option true else false.
     * @return HttpResponse - Response of the API Publishing activity
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException -  Exception throws by the method
     *                                                                               call of changeAPILifeCycleStatusToPublish() in
     *                                                                               APIPublisherRestClient.java.
     */
    protected HttpResponse publishAPI(APIIdentifier apiIdentifier, APIPublisherRestClient publisherRestClient,
                                      boolean isRequireReSubscription) throws APIManagerIntegrationTestException {
        APILifeCycleStateRequest publishUpdateRequest =
                new APILifeCycleStateRequest(apiIdentifier.getApiName(), apiIdentifier.getProviderName(),
                        APILifeCycleState.PUBLISHED);
        publishUpdateRequest.setVersion(apiIdentifier.getVersion());
        return publisherRestClient.changeAPILifeCycleStatusToPublish(apiIdentifier, isRequireReSubscription);

    }

    /**
     * Create and publish a API.
     *
     * @param apiIdentifier           - Instance of APIIdentifier object  that include the  API Name,
     *                                API Version and API Provider
     * @param apiCreationRequestBean  - Instance of APICreationRequestBean with all needed API information
     * @param publisherRestClient     - Instance of APIPublisherRestClient
     * @param isRequireReSubscription - If publish with re-subscription required option true else false.
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Exception throws by API
     *                                                                               create and publish activities.
     */
    public void createAndPublishAPI(APIIdentifier apiIdentifier, APICreationRequestBean apiCreationRequestBean,
                                    APIPublisherRestClient publisherRestClient,
                                    boolean isRequireReSubscription) throws APIManagerIntegrationTestException {
        //Create the API
        HttpResponse createAPIResponse = publisherRestClient.addAPI(apiCreationRequestBean);
        if (createAPIResponse.getResponseCode() == HTTP_RESPONSE_CODE_OK &&
                getValueFromJSON(createAPIResponse, "error").equals("false")) {
            //Publish the API
            HttpResponse publishAPIResponse = publishAPI(apiIdentifier, publisherRestClient, isRequireReSubscription);
            if (!(publishAPIResponse.getResponseCode() == HTTP_RESPONSE_CODE_OK &&
                    verifyAPIStatusChange(publishAPIResponse, APILifeCycleState.CREATED,
                            APILifeCycleState.PUBLISHED))) {
                throw new APIManagerIntegrationTestException("Error in API Publishing" +
                        getAPIIdentifierString(apiIdentifier) +
                        "Response Code:" + publishAPIResponse.getResponseCode() +
                        " Response Data :" + publishAPIResponse.getData());
            }

        } else {
            throw new APIManagerIntegrationTestException("Error in API Creation." +
                    getAPIIdentifierString(apiIdentifier) +
                    "Response Code:" + createAPIResponse.getResponseCode() +
                    " Response Data :" + createAPIResponse.getData());
        }


    }


    /**
     * Create and publish a API with re-subscription not required.
     *
     * @param apiIdentifier          - Instance of APIIdentifier object  that include the  API Name,
     *                               API Version and API Provider
     * @param apiCreationRequestBean - Instance of APICreationRequestBean with all needed API information
     * @param publisherRestClient    - Instance of APIPublisherRestClient
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Exception throws by API create
     *                                                                               and publish activities.
     */
    protected void createAndPublishAPIWithoutRequireReSubscription(APIIdentifier apiIdentifier, APICreationRequestBean apiCreationRequestBean,
                                                                   APIPublisherRestClient publisherRestClient)
            throws APIManagerIntegrationTestException {
        createAndPublishAPI(apiIdentifier, apiCreationRequestBean, publisherRestClient, false);
    }


    /**
     * Copy and API and create a new version.
     *
     * @param apiIdentifier       - Instance of APIIdentifier object  that include the  API Name,
     *                            API Version and API Provider
     * @param newAPIVersion       - New API version need to create
     * @param publisherRestClient - Instance of APIPublisherRestClient
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Exception throws by
     *                                                                               API copy activities.
     */

    protected void copyAPI(APIIdentifier apiIdentifier, String newAPIVersion,
                           APIPublisherRestClient publisherRestClient) throws APIManagerIntegrationTestException {
        try {
            //Copy API to version  to newVersion
            HttpResponse httpResponseCopyAPI =
                    publisherRestClient.copyAPI(apiIdentifier.getProviderName(), apiIdentifier.getApiName(),
                            apiIdentifier.getVersion(), newAPIVersion, "");
            if (!(httpResponseCopyAPI.getResponseCode() == HTTP_RESPONSE_CODE_OK &&
                    getValueFromJSON(httpResponseCopyAPI, "error").equals("false"))) {
                throw new APIManagerIntegrationTestException("Error in API Copy." +
                        getAPIIdentifierString(apiIdentifier) + "  New API Version :" + newAPIVersion +
                        "Response Code:" + httpResponseCopyAPI.getResponseCode() +
                        " Response Data :" + httpResponseCopyAPI.getData());

            }

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Exception thrown when copy a API", e);
        }

    }


    /**
     * Copy and publish the copied API.
     *
     * @param apiIdentifier           - Instance of APIIdentifier object  that include the  API Name,
     *                                API Version and API Provider
     * @param newAPIVersion           - New API version need to create
     * @param publisherRestClient     - Instance of APIPublisherRestClient
     * @param isRequireReSubscription - If publish with re-subscription required option true else false.
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException -Exception throws by copyAPI()
     *                                                                               and publishAPI() method calls
     */
    protected void copyAndPublishCopiedAPI(APIIdentifier apiIdentifier, String newAPIVersion, APIPublisherRestClient
            publisherRestClient, boolean isRequireReSubscription) throws APIManagerIntegrationTestException {

        copyAPI(apiIdentifier, newAPIVersion, publisherRestClient);
        APIIdentifier copiedAPIIdentifier =
                new APIIdentifier(apiIdentifier.getProviderName(), apiIdentifier.getApiName(), newAPIVersion);
        publishAPI(copiedAPIIdentifier, publisherRestClient, isRequireReSubscription);

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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Exception throws by API create
     *                                                                               publish and subscribe a API activities.
     */
    protected void createPublishAndSubscribeToAPI(APIIdentifier apiIdentifier, APICreationRequestBean apiCreationRequestBean,
                                                  APIPublisherRestClient publisherRestClient,
                                                  APIStoreRestClient storeRestClient, String applicationName)
            throws APIManagerIntegrationTestException {
        createAndPublishAPI(apiIdentifier, apiCreationRequestBean, publisherRestClient, false);
        HttpResponse httpResponseSubscribeAPI = subscribeToAPI(apiIdentifier, applicationName, storeRestClient);
        if (!(httpResponseSubscribeAPI.getResponseCode() == HTTP_RESPONSE_CODE_OK &&
                getValueFromJSON(httpResponseSubscribeAPI, "error").equals("false"))) {
            throw new APIManagerIntegrationTestException("Error in API Subscribe." +
                    getAPIIdentifierString(apiIdentifier) +
                    "Response Code:" + httpResponseSubscribeAPI.getResponseCode() +
                    " Response Data :" + httpResponseSubscribeAPI.getData());

        }
    }

    /**
     * Read the file content and return the content as String.
     *
     * @param fileLocation - Location of the file.¬
     * @return String - content of the file.
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - exception throws when reading the file.
     */
    protected String readFile(String fileLocation) throws APIManagerIntegrationTestException {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(fileLocation)));
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (IOException ioE) {
            throw new APIManagerIntegrationTestException("IOException when reading the file from:" + fileLocation, ioE);
        }
    }
}
