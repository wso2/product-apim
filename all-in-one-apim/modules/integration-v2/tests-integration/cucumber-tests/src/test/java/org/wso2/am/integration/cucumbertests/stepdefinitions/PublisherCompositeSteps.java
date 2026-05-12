/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PublisherCompositeSteps {

    private final BaseSteps baseSteps;
    private final PublisherBaseSteps publisherBaseSteps;
    private final String baseUrl;
    private static final Log log = LogFactory.getLog(PublisherCompositeSteps.class);

    public PublisherCompositeSteps() {

        baseSteps = new BaseSteps();
        publisherBaseSteps = new PublisherBaseSteps();
        baseUrl = TestContext.get(Constants.BASE_URL).toString();
    }

    /**
     * Composite step that creates an API, creates a revision, and deploys it.
     * This step combines multiple operations of creating and deploying an API
     *
     * @param payloadPath Path to the JSON file containing the API creation payload
     * @param apiID Context key where the created API ID will be stored
     */
    @Given("I have created an api from {string} as {string} and deployed it")
    public void iHaveCreatedAnApiFromAsAndDeployedIt(String payloadPath, String apiID) throws IOException, InterruptedException {

        baseSteps.putJsonPayloadFromFile(payloadPath, "<createApiPayload>");
        publisherBaseSteps.iCreateAnAPIWithPayloadAs("apis","<createApiPayload>");
        baseSteps.iWaitUntilStatus(201);
        baseSteps.iExtractResponseFieldAndStoreItAs("id", apiID);
        baseSteps.putJsonPayloadInContext("<createRevisionPayload>","{\"description\":\"Initial Revision\"}");
        publisherBaseSteps.iCreateResourceRevision("apis",apiID, "<createRevisionPayload>");
        baseSteps.iWaitUntilStatus(201);
        baseSteps.iExtractResponseFieldAndStoreItAs("id", "<revisionId>");
        baseSteps.waitForSeconds(3);
        baseSteps.putJsonPayloadInContext("<deployRevisionPayload>",
                "[{\"name\":\"{{gatewayEnvironment}}\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");
        publisherBaseSteps.iDeployApiRevisionGivenPayload("<revisionId>", "apis",apiID, "<deployRevisionPayload>");
        baseSteps.iWaitUntilStatus(201);
    }


    /**
     * Composite step that deploys a revision using a default deployment payload.
     * This step simplifies revision deployment by using a standard deployment configuration.
     *
     * @param revisionID Context key containing the revision ID to deploy
     * @param resourceType Type of resource (e.g., "apis", "api-products")
     * @param resourceID Context key containing the resource ID
     */
    @When("I deploy revision {string} of {string} resource {string}")
    public void iDeployRevision(String revisionID, String resourceType, String resourceID)  {
        baseSteps.putJsonPayloadInContext("<deployRevisionPayload>",
                "[{\"name\":\"{{gatewayEnvironment}}\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");
        publisherBaseSteps.iDeployApiRevisionGivenPayload(revisionID, resourceType, resourceID, "<deployRevisionPayload>");
    }

    /**
     * Composite step that creates a new revision and deploys the API.
     * This step combines revision creation and deployment into a single operation.
     *
     * @param apiID Context key containing the API ID to deploy
     */
    @Given("I deploy the API with id {string}")
    public void iDeployAPI(String apiID) throws IOException, InterruptedException{
        baseSteps.putJsonPayloadInContext("<createRevisionPayload>","{\"description\":\"new Revision\"}");
        publisherBaseSteps.iCreateResourceRevision("apis", apiID , "<createRevisionPayload>");
        baseSteps.iWaitUntilStatus(201);
        baseSteps.iExtractResponseFieldAndStoreItAs("id", "<revisionId>");
        baseSteps.waitForSeconds(3);
        baseSteps.putJsonPayloadInContext("<deployRevisionPayload>",
                "[{\"name\":\"{{gatewayEnvironment}}\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");
        publisherBaseSteps.iDeployApiRevisionGivenPayload("<revisionId>", "apis" ,apiID, "<deployRevisionPayload>");
    }

    /**
     * A composite step that executes the specified lifecycle transition for the given resource (API or API-Product)
     * and repeatedly verifies the resulting lifecycle state until the expected state is reached or
     * the retry limit is exceeded.
     *
     * @param resourceType  The type of resource (e.g., "apis", "api-products")
     * @param resourceIdKey The context key where the resource UUID is stored
     * @param action        The transition action (e.g., "Publish", "Demote to Created")
     * @param expectedState The final state to verify (e.g., "Published", "Created")
     * @throws Exception    If the transition is not verified within the maximum retry limit
     */
    @When("I execute lifecycle action {string} on {string} resource {string} and wait for state {string}")
    public void changeLifecycleAndWait(String action, String resourceType, String resourceIdKey, String expectedState)
            throws Exception {

        String resourceId = Utils.resolveFromContext(resourceIdKey).toString();
        String accessToken = TestContext.get("publisherAccessToken").toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + accessToken);

        String changeLifecycleUrl = Utils.getChangeLifecycleURL(baseUrl, resourceType, resourceId, action,
                null);

        boolean stateAchieved = false;
        String actualLifecycleState = "UNKNOWN";

        for (int attempt = 1; attempt <= Constants.MAX_RETRIES; attempt++) {
            log.info("Attempt " + attempt + "/" + Constants.MAX_RETRIES + ": Executing lifecycle action '" + action +
                    "' on " + resourceType + " " + resourceId);

            // Send the state change request
            HttpResponse changeResponse = SimpleHTTPClient.getInstance().doPost(changeLifecycleUrl, headers,
                    null, null);

            if (changeResponse == null || changeResponse.getResponseCode() != 200) {
                log.warn("Lifecycle change request responded with status code [" +
                        (changeResponse != null ? changeResponse.getResponseCode() : "null") +
                        "]. Data: " + (changeResponse != null ? changeResponse.getData() : "No data available") +
                        ", Proceeding to verification check...");
            }
            baseSteps.waitForSeconds(3);

            // Get the current lifecycle status
            publisherBaseSteps.IGetLifecycleStatusOf(resourceIdKey);
            baseSteps.iWaitUntilStatus(200);
            HttpResponse getLifecycleResponse = (HttpResponse) TestContext.get(Constants.HTTP_RESPONSE);
            actualLifecycleState = Utils.extractValueFromPayload(getLifecycleResponse.getData(), "state")
                    .toString();

            // Check if the current lifecycle state matches the expected state
            if (expectedState.equalsIgnoreCase(actualLifecycleState)) {
                log.info(resourceType + " " + resourceId + " successfully reached expected state: '"
                        + expectedState + "'");
                stateAchieved = true;
                break;
            }

            log.info("Attempt " + attempt + " failed. Current state: '" + actualLifecycleState + "'. Retrying flow...");
        }

        Assert.assertTrue(stateAchieved, "Resource " + resourceId + " failed to reach state '"
                + expectedState + "' after " + Constants.MAX_RETRIES + " attempts. Last known state: '"
                + actualLifecycleState + "'");
    }
}
