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

import io.cucumber.java.en.And;
import io.cucumber.java.en.When;

import java.io.IOException;

public class ApplicationCompositeSteps {

    private final BaseSteps baseSteps;
    private final ApplicationBaseSteps applicationBaseSteps;

    public ApplicationCompositeSteps() {

        baseSteps = new BaseSteps();
        applicationBaseSteps = new ApplicationBaseSteps();
    }

    /**
     * Composite step definition that
     *      creates an application,
     *      generates keys,
     *      subscribes to an API,
     * and obtains an access token.
     *
     * @param apiIdContextKey      Input: Context key containing the API ID
     * @param subscriptionStoreKey Output: Context key to store the subscription ID
     * @param accessTokenStoreKey  Output: Context key to store the access token
     */
    @When("I have set up application with keys, subscribed to API {string}, store subscription id as {string} and access token as {string}")
    public void iSetupApplicationSubscribeAndGetToken(String apiIdContextKey, String subscriptionStoreKey,
                                                      String accessTokenStoreKey) throws Exception {

        // Create Application (stores as 'createdAppId')
        iCreateTestApplication("createdAppId");

        // Generate Keys (stores as 'appConsumerSecret' and 'keyMappingId')
        iGenerateKeysForApplication("createdAppId", "appConsumerSecret", "keyMappingId");

        // Subscribe to the API
        iSubscribeToResource(apiIdContextKey, "createdAppId", subscriptionStoreKey);

        // Obtain Access Token
        iObtainAccessToken("", "createdAppId", "keyMappingId", "appConsumerSecret", accessTokenStoreKey);
    }

    /**
     * Creates an application from a template file and stores its ID.
     *
     * @param appIdStoreKey Output: Context key to store the created Application ID
     */
    @When("I create a test application and store the id as {string}")
    public void iCreateTestApplication(String appIdStoreKey) throws IOException {

        // Load payload and create the application
        baseSteps.putJsonPayloadFromFile("artifacts/payloads/create_apim_test_app.json", "createAppPayload");
        applicationBaseSteps.iCreateAnApplicationWithJsonPayload("createAppPayload");

        // Verify creation and store the ID in the given context key
        baseSteps.theResponseStatusCodeShouldBe(201);
        baseSteps.iExtractResponseFieldAndStoreItAs("applicationId", appIdStoreKey);
    }

    /**
     * Generates production keys for an existing application.
     *
     * @param appIdContextKey      Input: Context key where the Application ID is stored
     * @param consumerSecretStoreKey Output: Context key to store the generated Consumer Secret
     * @param keyMappingStoreKey   Output: Context key to store the Key Mapping ID
     */
    @And("I generate keys for application {string} and store consumer secret as {string} and key mapping id as {string}")
    public void iGenerateKeysForApplication(String appIdContextKey, String consumerSecretStoreKey,
                                            String keyMappingStoreKey) throws Exception {

        // Prepare the key generation payload
        String payload = "{\"keyType\": \"PRODUCTION\", \"grantTypesToBeSupported\": [\"client_credentials\"]}";
        baseSteps.putJsonPayloadInContext("generateKeysPayload", payload);

        // Request key generation
        applicationBaseSteps.iGenerateClientCredentialsForApplication(appIdContextKey, "generateKeysPayload");
        baseSteps.theResponseStatusCodeShouldBe(200);

        // Extract and store values into given context keys
        baseSteps.iExtractResponseFieldAndStoreItAs("consumerSecret", consumerSecretStoreKey);
        baseSteps.iExtractResponseFieldAndStoreItAs("keyMappingId", keyMappingStoreKey);
    }

    /**
     * Subscribes an application to an API and stores the Subscription ID.
     *
     * @param apiIdContextKey      Input: Key where API ID is stored
     * @param appIdContextKey      Input: Key where Application ID is stored
     * @param subscriptionStoreKey Output: Key to store the new Subscription ID
     */
    @And("I subscribe to resource {string} using application {string} and store subscription as {string}")
    public void iSubscribeToResource(String apiIdContextKey, String appIdContextKey,
                                     String subscriptionStoreKey) throws Exception {

        // Prepare Subscription Payload (Placeholders resolved by iSubscribeToApi)
        baseSteps.putJsonPayloadInContext("apiSubscriptionPayload",
                "{\"applicationId\": \"{{applicationId}}\", \"apiId\": \"{{apiId}}\", " +
                        "\"throttlingPolicy\": \"Bronze\"}");

        // Map the user-provided context keys to the names expected by the payload resolution
        baseSteps.iPutValueInContextAs(apiIdContextKey, "<apiId>");
        baseSteps.iPutValueInContextAs(appIdContextKey, "<applicationId>");

        applicationBaseSteps.iSubscribeToApi("apiSubscriptionPayload");
        baseSteps.theResponseStatusCodeShouldBe(201);

        // Extract and store the subscription ID
        baseSteps.iExtractResponseFieldAndStoreItAs("subscriptionId", subscriptionStoreKey);
    }

    /**
     * Obtains an OAuth2 access token for an application and stores it.
     *
     * @param appIdContextKey          Input: Key where Application ID is stored
     * @param keyMappingIdContextKey   Input: Key where Key Mapping ID is stored
     * @param consumerSecretContextKey Input: Key where Consumer Secret is stored
     * @param accessTokenStoreKey      Output: Key to store the generated Access Token
     * @param scope                    Input: Optional scope for the token
     */
    @And("I obtain an access token with scope {string} for application {string} using key mapping {string} and consumer secret {string} and store as {string}")
    public void iObtainAccessToken(String scope, String appIdContextKey, String keyMappingIdContextKey,
                                   String consumerSecretContextKey, String accessTokenStoreKey) throws Exception {

        // Prepare Token Payload
        String tokenPayload;
        if (scope != null && !scope.isEmpty()) {
            tokenPayload = "{\"consumerSecret\": \"{{appConsumerSecret}}\", \"validityPeriod\": 3600, " +
                    "\"scopes\": [\"" + scope + "\"]}";
        } else {
            tokenPayload = "{\"consumerSecret\": \"{{appConsumerSecret}}\", \"validityPeriod\": 3600}";
        }
        baseSteps.putJsonPayloadInContext("tokenRequestPayload", tokenPayload);
        baseSteps.iPutValueInContextAs(consumerSecretContextKey, "<appConsumerSecret>");

        // Request the token
        applicationBaseSteps.iRequestAccessToken(appIdContextKey, "tokenRequestPayload",
                keyMappingIdContextKey);
        baseSteps.theResponseStatusCodeShouldBe(200);

        // Extract and store the access token
        baseSteps.iExtractResponseFieldAndStoreItAs("accessToken", accessTokenStoreKey);
    }
}