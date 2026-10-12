/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * API-Product specific glue that the shared step classes do not carry (ports the parts of
 * APIProductCreationTestCase / APIProductLifecycleTest / APIProductRevisionTestCase that assert on a PRODUCT
 * rather than on an API): the four-credential application setup legacy invoked every product with, the
 * publisher/devportal DTO-fidelity checks (verfiyApiProductInPublisher / verifyApiProductInPortal), re-validating
 * a product's own OpenAPI definition through the publisher validator, and the product↔member-API operations-count
 * equality. Kept in its own class so the shared step classes stay untouched; it drives them through their public
 * step methods rather than re-implementing their HTTP.
 */
public class ApiProductSteps {

    private final BaseSteps baseSteps = new BaseSteps();
    private final ApplicationBaseSteps applicationSteps = new ApplicationBaseSteps();
    private final APIInvocationSteps invocationSteps = new APIInvocationSteps();

    /** The leasing member's five resources (legacy leasing-api), invoked as verb + path under the product context. */
    private static final String[][] LEASING_OPERATIONS = {
            {"GET", "/assets"}, {"POST", "/assets"}, {"GET", "/assets/1"}, {"PUT", "/assets/1"},
            {"DELETE", "/assets/1"}};

    /**
     * Page size for the unfiltered listing reads. Generous enough that a shared block's accumulated APIs and
     * products all fit on one page, so a membership check never fails merely because the resource is on page 2.
     */
    private static final int PRODUCT_LIST_PAGE_SIZE = 1000;

    /** Context keys the four-credential composite publishes, one per credential legacy invoked a product with. */
    private static final String PRODUCTION_APP_TOKEN = "productionAppToken";
    private static final String SANDBOX_APP_TOKEN = "sandboxAppToken";
    private static final String PRODUCTION_USER_TOKEN = "productionUserToken";
    private static final String SANDBOX_USER_TOKEN = "sandboxUserToken";
    private static final String KEYGEN_CALLBACK_URL = "http://localhost";
    private static final String KEYGEN_VALIDITY = "3600";
    private static final String PRODUCTION_SUBSCRIBER_TOKEN = "productionSubscriberToken";
    private static final String SANDBOX_SUBSCRIBER_TOKEN = "sandboxSubscriberToken";

    /**
     * Sets up ONE application subscribed to the given API/product and obtains the FOUR credentials the legacy
     * product tests invoked with: a PRODUCTION application (client-credentials) token, a SANDBOX application
     * token, and a password-grant USER token against each of those two key mappings. Legacy asserted the product
     * answers all four identically; v2 previously exercised only the production application token.
     *
     * <p>Order is load-bearing: generating SANDBOX keys REPLACES {@code consumerKey}/{@code consumerSecret}/
     * {@code keyMappingId} in context, so the production pair's two tokens are taken first. Both key mappings
     * request the {@code password} grant so the user-token half is possible.
     *
     * @param apiIdKey          context key holding the API/API-product id to subscribe to
     * @param plan              subscription business plan (legacy used {@code Unlimited})
     * @param subscriptionIdKey context key to store the created subscription id under
     */
    @When("I have set up application with production and sandbox keys, subscribed to API {string} with plan {string}, and obtained the four credentials as {string}")
    public void iSetUpFourCredentials(String apiIdKey, String plan, String subscriptionIdKey) throws Exception {
        setUpFourCredentials(apiIdKey, plan, subscriptionIdKey, null, null);
    }

    /**
     * The four-credential composite, plus a PRODUCTION and a SANDBOX password-grant token for a SECOND resource
     * owner ({@code productionSubscriberToken} / {@code sandboxSubscriberToken}) minted with the same application
     * keys — legacy invoked products with tokens of least-privileged subscribers, not the application owner.
     */
    @When("I have set up application with production and sandbox keys, subscribed to API {string} with plan {string}, and obtained the four credentials as {string} plus user tokens for {string}")
    public void iSetUpFourCredentialsPlusUserTokens(String apiIdKey, String plan, String subscriptionIdKey,
                                                    String userTokenActor) throws Exception {
        setUpFourCredentials(apiIdKey, plan, subscriptionIdKey, userTokenActor, "");
    }

    /**
     * As above, but the second resource owner's tokens request {@code scope}, and the token response must grant
     * exactly that scope set (legacy asserted the echoed scope set equal to the requested one).
     */
    @When("I have set up application with production and sandbox keys, subscribed to API {string} with plan {string}, and obtained the four credentials as {string} plus user tokens for {string} with scope {string}")
    public void iSetUpFourCredentialsPlusScopedUserTokens(String apiIdKey, String plan, String subscriptionIdKey,
                                                          String userTokenActor, String scope) throws Exception {
        setUpFourCredentials(apiIdKey, plan, subscriptionIdKey, userTokenActor, scope);
    }

    private void setUpFourCredentials(String apiIdKey, String plan, String subscriptionIdKey,
                                      String userTokenActor, String userTokenScope) throws Exception {

        baseSteps.putJsonPayloadFromFile("artifacts/payloads/create_apim_test_app.json", "<createAppPayload>");
        applicationSteps.iCreateAnApplicationWithJsonPayload("<createAppPayload>");
        baseSteps.theResponseStatusCodeShouldBe(201);

        generateKeysOfType("PRODUCTION");

        baseSteps.putJsonPayloadInContext("<apiSubscriptionPayload>", "{\"applicationId\": \"{{applicationId}}\","
                + "\"apiId\": \"{{apiId}}\",\"throttlingPolicy\": \"" + plan + "\"}");
        applicationSteps.iSubscribeToApi(apiIdKey, "<createdAppId>", "<apiSubscriptionPayload>", subscriptionIdKey);
        baseSteps.theResponseStatusCodeShouldBe(201);
        assertSubscription(currentResponseJson("subscription"), TestContext.resolve(apiIdKey).toString(),
                TestContext.resolve("createdAppId").toString(), plan);

        captureApplicationToken(PRODUCTION_APP_TOKEN);
        captureUserToken(PRODUCTION_USER_TOKEN);
        if (userTokenActor != null) {
            captureNamedUserToken(userTokenActor, userTokenScope, PRODUCTION_SUBSCRIBER_TOKEN);
        }

        generateKeysOfType("SANDBOX");

        captureApplicationToken(SANDBOX_APP_TOKEN);
        captureUserToken(SANDBOX_USER_TOKEN);
        if (userTokenActor != null) {
            captureNamedUserToken(userTokenActor, userTokenScope, SANDBOX_SUBSCRIBER_TOKEN);
        }
    }

    /**
     * Legacy verifyInvocation: EVERY resource of the product's leasing member (create_apim_product_leasing_api.json)
     * is invoked through the product with each credential, and each call must answer the expected status and body.
     */
    @When("I invoke every operation of the product leasing member at {string} with access tokens {string} expecting status {int} and body {string}")
    public void iInvokeEveryLeasingOperation(String productBase, String tokenKeysCsv, int expectedStatus,
                                             String expectedBody) throws Exception {
        for (String tokenKey : tokenKeysCsv.split(",")) {
            for (String[] operation : LEASING_OPERATIONS) {
                invocationSteps.invokeApiByContextUntilStatus(productBase + operation[1], operation[0],
                        tokenKey.trim(), "", expectedStatus, 60);
                baseSteps.theResponseStatusCodeShouldBe(expectedStatus);
                baseSteps.responseShouldContainFieldValue(expectedBody);
            }
        }
    }

    /**
     * Legacy verifyInvocation with a body: the same request is sent with each credential and every response must be
     * the expected status with EXACTLY the expected body. Where an earlier state could also answer that status, gate
     * on the new body first — this step stops polling at the first matching status.
     */
    @When("I invoke {string} with method {string}, payload {string} and content type {string} using each of the access tokens {string} and expect status {int} with body exactly {string}")
    public void iInvokeWithEachTokenExpectingExactBody(String context, String method, String payloadKey,
                                                       String contentType, String tokenKeysCsv, int expectedStatus,
                                                       String expectedBody) throws Exception {
        String expected = Utils.resolveContextPlaceholders(expectedBody);
        for (String tokenKey : tokenKeysCsv.split(",")) {
            invocationSteps.invokeApiByContextWithContentTypeUntilStatus(context, method, tokenKey.trim(), payloadKey,
                    contentType, expectedStatus, 60);
            baseSteps.theResponseStatusCodeShouldBe(expectedStatus);
            HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
            Assert.assertEquals(response.getData(), expected, method + " " + context + " with token '" + tokenKey.trim()
                    + "' did not return exactly the expected body");
        }
    }

    /** Generates keys of the given keyType for the created application, with the password grant enabled. */
    private void generateKeysOfType(String keyType) throws Exception {
        baseSteps.putJsonPayloadInContext("<generateApplicationKeysPayload>", "{\"keyType\": \"" + keyType + "\","
                + "\"grantTypesToBeSupported\": [\"client_credentials\", \"password\"],"
                + "\"callbackUrl\": \"" + KEYGEN_CALLBACK_URL + "\",\"validityTime\": \"" + KEYGEN_VALIDITY + "\"}");
        applicationSteps.iGenerateClientCredentialsForApplication("<createdAppId>",
                "<generateApplicationKeysPayload>");
        baseSteps.theResponseStatusCodeShouldBe(200);
        JSONObject keys = currentResponseJson(keyType + " key generation");
        Assert.assertEquals(keys.optString("callbackUrl"), KEYGEN_CALLBACK_URL, "Generated keys callbackUrl mismatch: "
                + keys);
        JSONObject keyToken = keys.optJSONObject("token");
        Assert.assertNotNull(keyToken, "Generated keys carry no token: " + keys);
        Assert.assertEquals(toStringSet(keyToken.optJSONArray("tokenScopes")),
                new HashSet<>(java.util.Collections.singletonList("default")),
                "Key token scopes are not exactly {default}: " + keys);
        Assert.assertEquals(keyToken.optLong("validityTime"), Long.parseLong(KEYGEN_VALIDITY),
                "Key token validity mismatch: " + keys);
        Assert.assertEquals(keys.optString("keyType"), keyType, "Generated key type mismatch: " + keys);
        Assert.assertEquals(keys.optString("keyState"), "APPROVED", "Generated keys are not APPROVED: " + keys);
        Assert.assertEquals(toStringSet(keys.optJSONArray("supportedGrantTypes")),
                new HashSet<>(java.util.Arrays.asList("client_credentials", "password")),
                "Generated keys do not support exactly the requested grant types: " + keys);
    }

    /** Legacy verifySubscription: the subscription binds THIS api and application, on the plan, UNBLOCKED. */
    private void assertSubscription(JSONObject subscription, String apiId, String applicationId, String plan) {
        Assert.assertEquals(subscription.optString("apiId"), apiId, "Subscription apiId mismatch: " + subscription);
        Assert.assertEquals(subscription.optString("applicationId"), applicationId,
                "Subscription applicationId mismatch: " + subscription);
        Assert.assertEquals(subscription.optString("throttlingPolicy"), plan,
                "Subscription throttling policy mismatch: " + subscription);
        Assert.assertEquals(subscription.optString("status"), "UNBLOCKED",
                "Subscription is not UNBLOCKED: " + subscription);
        JSONObject apiInfo = subscription.optJSONObject("apiInfo");
        Assert.assertNotNull(apiInfo, "Subscription carries no apiInfo: " + subscription);
        Assert.assertEquals(apiInfo.optString("id"), apiId, "Subscription apiInfo.id mismatch: " + subscription);
        // Legacy verifySubscriptionApiInfo compared apiInfo with the devportal representation of the subscribed API.
        Map<String, String> devportalHeaders = new HashMap<>();
        devportalHeaders.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        JSONObject portal = readJsonWhenAvailableQuietly(Utils.getDevportalApiDetailURL(Utils.getBaseUrl(), apiId),
                devportalHeaders, "devportal API " + apiId);
        for (String field : new String[] {"name", "provider", "context", "lifeCycleStatus", "description"}) {
            Assert.assertEquals(apiInfo.optString(field, null), portal.optString(field, null),
                    "Subscription apiInfo." + field + " differs from the devportal representation: apiInfo="
                            + apiInfo + " devportal=" + portal);
        }
        JSONObject applicationInfo = subscription.optJSONObject("applicationInfo");
        Assert.assertNotNull(applicationInfo, "Subscription carries no applicationInfo: " + subscription);
        Assert.assertEquals(applicationInfo.optString("applicationId"), applicationId,
                "Subscription applicationInfo.applicationId mismatch: " + subscription);
        // A freshly created application's first subscription (legacy: the app's count before subscribing + 1).
        Assert.assertEquals(applicationInfo.optInt("subscriptionCount", -1), 1,
                "Subscription applicationInfo.subscriptionCount mismatch: " + subscription);
    }

    private JSONObject readJsonWhenAvailableQuietly(String url, Map<String, String> headers, String what) {
        try {
            return readJsonWhenAvailable(url, headers, what);
        } catch (Exception e) {
            throw new AssertionError("Failed to read " + what, e);
        }
    }

    /** Password-grant token for a NAMED actor with the current client credentials; a requested scope must be echoed. */
    private void captureNamedUserToken(String actorRef, String scope, String contextKey) throws Exception {
        applicationSteps.iRequestOAuthAccessTokenAsActorWithScope(actorRef, scope);
        baseSteps.theResponseStatusCodeShouldBe(200);
        if (scope != null && !scope.isBlank()) {
            JSONObject tokenResponse = currentResponseJson("password-grant token");
            Set<String> requested = new HashSet<>(java.util.Arrays.asList(
                    Utils.resolveContextPlaceholders(scope).trim().split("\\s+")));
            Set<String> granted = new HashSet<>(java.util.Arrays.asList(
                    tokenResponse.optString("scope").trim().split("\\s+")));
            Assert.assertEquals(granted, requested, "Token for " + actorRef + " was not granted exactly the requested "
                    + "scopes: " + tokenResponse);
        }
        stowGeneratedToken(contextKey);
    }

    private JSONObject currentResponseJson(String what) {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getData() != null && !response.getData().isBlank(),
                "No " + what + " response body to inspect");
        return new JSONObject(response.getData());
    }

    /** Requests an application (client-credentials) token for the CURRENT key mapping and stows it under a key. */
    private void captureApplicationToken(String contextKey) throws Exception {
        baseSteps.putJsonPayloadInContext("<createApplicationAccessTokenPayload>",
                "{\"consumerSecret\": \"{{appConsumerSecret}}\",\"validityPeriod\": 3600}");
        applicationSteps.iRequestAccessToken("<createdAppId>", "<createApplicationAccessTokenPayload>");
        baseSteps.theResponseStatusCodeShouldBe(200);
        stowGeneratedToken(contextKey);
    }

    /** Requests a password-grant token for the acting user with the CURRENT client credentials, and stows it. */
    private void captureUserToken(String contextKey) throws Exception {
        applicationSteps.iRequestOAuthAccessTokenWithScope("");
        baseSteps.theResponseStatusCodeShouldBe(200);
        stowGeneratedToken(contextKey);
    }

    /**
     * Copies the just-issued {@code generatedAccessToken} to a stable per-credential key. The shared token steps
     * all publish to that single key, so without this the second key mapping's tokens would overwrite the first
     * and only one credential could ever be invoked.
     */
    private void stowGeneratedToken(String contextKey) {
        Object token = TestContext.resolve("generatedAccessToken");
        Assert.assertNotNull(token, "No access token was issued to store as '" + contextKey + "'");
        TestContext.set(contextKey, token);
    }

    /**
     * Feeds an OpenAPI definition held in context (e.g. an API PRODUCT's own swagger, just retrieved) back through
     * the publisher OAS validator ({@code POST /apis/validate-openapi}). This round trip — not merely reading the
     * definition — was the legacy testAPIProductSwaggerDefinition's subject: a product's generated definition must
     * itself be a valid OpenAPI document. The validator's response ({@code isValid}) is published for assertion.
     *
     * @param definitionKey context key holding the definition text
     */
    @When("I validate the openapi definition captured as {string}")
    public void iValidateCapturedOpenApiDefinition(String definitionKey) throws IOException {

        String definition = TestContext.resolve(definitionKey).toString();
        Assert.assertFalse(definition.isBlank(), "The captured OpenAPI definition '" + definitionKey + "' is empty");
        File temp = File.createTempFile("product-swagger", ".json");
        temp.deleteOnExit();
        Files.write(temp.toPath(), definition.getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, File> files = new HashMap<>();
        files.put("file", temp);
        Requests.postMultipart(Utils.getValidateOpenAPIURL(Utils.getBaseUrl()), headers, files, new HashMap<>());
    }

    /**
     * Asserts the API product's entry for a member API carries the SAME NUMBER of operations as that API itself
     * (legacy testUpdateUnderlyingAPIofAPIProduct): after the underlying API is edited and the product re-saved,
     * the product's aggregated resource set must still mirror the API's. Both reads are intermediate (consumed
     * here), so neither is published as {@code httpResponse}.
     *
     * @param productIdKey context key holding the API product id
     * @param apiIdKey     context key holding the member API id
     */
    @Then("The API product {string} entry for API {string} should have the same operations count as the API")
    public void theProductEntryShouldMatchApiOperationsCount(String productIdKey, String apiIdKey) throws IOException {

        String productId = TestContext.resolve(productIdKey).toString();
        String apiId = TestContext.resolve(apiIdKey).toString();

        JSONObject product = readJson(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "api-products", productId),
                publisherHeaders(), "API product " + productId);
        JSONObject api = readJson(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", apiId),
                publisherHeaders(), "API " + apiId);

        JSONArray productApis = product.optJSONArray("apis");
        Assert.assertTrue(productApis != null && productApis.length() > 0,
                "API product " + productId + " lists no member APIs: " + product);
        JSONObject matching = null;
        for (int i = 0; i < productApis.length(); i++) {
            if (apiId.equals(productApis.getJSONObject(i).optString("apiId"))) {
                matching = productApis.getJSONObject(i);
                break;
            }
        }
        Assert.assertNotNull(matching,
                "API product " + productId + " has no entry for the underlying API " + apiId + ": " + product);
        JSONArray productOperations = matching.optJSONArray("operations");
        Assert.assertNotNull(productOperations,
                "The product's entry for API " + apiId + " has no operations: " + matching);
        JSONArray apiOperations = api.optJSONArray("operations");
        Assert.assertNotNull(apiOperations, "API " + apiId + " has no operations: " + api);
        Assert.assertEquals(productOperations.length(), apiOperations.length(),
                "The product's operations count for API " + apiId + " does not match the API's own count. "
                        + "Product entry: " + matching + " / API operations: " + apiOperations);
    }

    /**
     * Asserts the API product appears EXACTLY ONCE in the publisher product listing and that the listing entry's
     * info fields agree with the product's own GET representation — the port of the legacy
     * {@code verfiyApiProductInPublisher} (list membership + count 1 + info/DTO field agreement).
     *
     * @param productIdKey context key holding the API product id
     */
    @Then("The publisher product list should report API product {string} exactly once with the same info fields")
    public void thePublisherListShouldReportProductOnce(String productIdKey) throws Exception {

        String productId = TestContext.resolve(productIdKey).toString();
        JSONObject product = readJson(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "api-products", productId),
                publisherHeaders(), "API product " + productId);

        // The UNFILTERED collection, not the ?query=<name> search: that search form goes through the artifact
        // index and answers total:0 for a just-created product (verified against a container), so a listing
        // membership check written against it would assert nothing.
        JSONObject listing = readJsonUntil(Utils.getApiProductListURL(Utils.getBaseUrl(), PRODUCT_LIST_PAGE_SIZE),
                publisherHeaders(), "publisher product listing",
                body -> countEntriesWithId(body, productId) == 1);
        JSONObject entry = onlyEntryWithId(listing, productId, "publisher product listing");

        assertSameField(entry, product, "name", productId);
        assertSameField(entry, product, "context", productId);
        assertSameField(entry, product, "description", productId);
        assertSameField(entry, product, "provider", productId);
        assertSameField(entry, product, "state", productId);
        assertSameField(entry, product, "hasThumbnail", productId);
        assertSameStringSet(entry.optJSONArray("securityScheme"), product.optJSONArray("securityScheme"),
                "securityScheme", productId);
    }

    /**
     * Legacy createAPIProductInPublisher: the create response echoes the requested name, version 1.0.0, the
     * requested context (prefixed {@code /t/<domain>} for a tenant provider) and the provider.
     */
    @Then("The create response of API product {string} should echo its name, context, version and the provider of actor {string}")
    public void theCreateResponseShouldEchoRequest(String productIdKey, String providerActorRef) {

        JSONObject created = new JSONObject(TestContext.resolve(
                productIdKey + PublisherBaseSteps.PRODUCT_CREATE_RESPONSE_SUFFIX).toString());
        String requestedName = TestContext.resolve(productIdKey + PublisherBaseSteps.PRODUCT_REQUESTED_NAME_SUFFIX)
                .toString();
        String requestedContext = TestContext.resolve(
                productIdKey + PublisherBaseSteps.PRODUCT_REQUESTED_CONTEXT_SUFFIX).toString();
        String bareContext = requestedContext.startsWith("/") ? requestedContext : "/" + requestedContext;
        String tenantDomain = Identity.resolveActor(Utils.resolveContextPlaceholders(providerActorRef))
                .getUserDomain();
        String expectedContext = Constants.SUPER_TENANT_DOMAIN.equals(tenantDomain)
                ? bareContext : "/t/" + tenantDomain + bareContext;

        Assert.assertEquals(created.optString("name"), requestedName, "Create response name mismatch: " + created);
        Assert.assertEquals(created.optString("version"), "1.0.0", "Create response version mismatch: " + created);
        Assert.assertEquals(created.optString("context"), expectedContext,
                "Create response context mismatch: " + created);
        Assert.assertEquals(created.optString("provider"), PublisherBaseSteps.providerOf(providerActorRef),
                "Create response provider mismatch: " + created);
    }

    /** Checks the full product-create representation when an admin supplies an arbitrary provider value. */
    @Then("The create response of API product {string} should echo its name, context, version and requested provider for actor {string}")
    public void theCreateResponseShouldEchoExplicitProvider(String productIdKey, String actorRef) {
        JSONObject created = new JSONObject(TestContext.resolve(
                productIdKey + PublisherBaseSteps.PRODUCT_CREATE_RESPONSE_SUFFIX).toString());
        String requestedName = TestContext.resolve(productIdKey + PublisherBaseSteps.PRODUCT_REQUESTED_NAME_SUFFIX)
                .toString();
        String requestedContext = TestContext.resolve(
                productIdKey + PublisherBaseSteps.PRODUCT_REQUESTED_CONTEXT_SUFFIX).toString();
        String expectedProvider = TestContext.resolve(
                productIdKey + PublisherBaseSteps.PRODUCT_REQUESTED_PROVIDER_SUFFIX).toString();
        String bareContext = requestedContext.startsWith("/") ? requestedContext : "/" + requestedContext;
        String tenantDomain = Identity.resolveActor(Utils.resolveContextPlaceholders(actorRef)).getUserDomain();
        String expectedContext = Constants.SUPER_TENANT_DOMAIN.equals(tenantDomain)
                ? bareContext : "/t/" + tenantDomain + bareContext;

        Assert.assertEquals(created.optString("name"), requestedName, "Create response name mismatch: " + created);
        Assert.assertEquals(created.optString("version"), "1.0.0", "Create response version mismatch: " + created);
        Assert.assertEquals(created.optString("context"), expectedContext,
                "Create response context mismatch: " + created);
        Assert.assertEquals(created.optString("provider"), expectedProvider,
                "Create response provider mismatch: " + created);
    }

    /** Legacy verfiyApiProductInPublisher: the product read back by id equals its create response field by field. */
    @Then("The API product {string} read from the publisher should match its create response")
    public void theProductShouldMatchItsCreateResponse(String productIdKey) throws IOException {

        String productId = TestContext.resolve(productIdKey).toString();
        JSONObject created = new JSONObject(TestContext.resolve(
                productIdKey + PublisherBaseSteps.PRODUCT_CREATE_RESPONSE_SUFFIX).toString());
        JSONObject current = readJson(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "api-products", productId),
                publisherHeaders(), "API product " + productId);

        for (String field : new String[] {"id", "name", "context", "version", "description", "provider",
                "hasThumbnail", "state", "enableSchemaValidation", "responseCachingEnabled", "cacheTimeout",
                "visibility", "accessControl", "apiType", "authorizationHeader", "subscriptionAvailability",
                "apiThrottlingPolicy", "createdTime", "lastUpdatedTime", "isDefaultVersion"}) {
            if (created.has(field)) {
                assertSameField(current, created, field, productId);
            }
        }
        for (String field : new String[] {"visibleRoles", "visibleTenants", "accessControlRoles", "transport",
                "tags", "policies", "securityScheme", "subscriptionAvailableTenants"}) {
            assertSameStringSet(current.optJSONArray(field), created.optJSONArray(field), field, productId);
        }
        for (String field : new String[] {"businessInformation", "corsConfiguration", "monetization"}) {
            Object expected = created.isNull(field) ? null : created.get(field);
            Object actual = current.isNull(field) ? null : current.get(field);
            Assert.assertTrue(expected == null ? actual == null
                            : actual != null && new JSONObject(actual.toString()).similar(
                                    new JSONObject(expected.toString())),
                    "'" + field + "' differs between the create response and GET of " + productId
                            + ": created=" + expected + " current=" + actual);
        }
        Assert.assertEquals(apiIdsOf(current), apiIdsOf(created),
                "Member APIs differ between the create response and GET of " + productId);
        Assert.assertEquals(productScopesOf(current), productScopesOf(created),
                "Scopes (name, description, bindings) differ between the create response and GET of " + productId);
        Assert.assertEquals(additionalPropertiesOf(current), additionalPropertiesOf(created),
                "additionalProperties differ between the create response and GET of " + productId);
    }

    /**
     * Legacy verifyInvocation read the gateway URLs the devportal advertises for a DEPLOYED product: at least one
     * environment entry carrying an http or https gateway URL.
     */
    @Then("The devportal should advertise gateway endpoint URLs for API product {string}")
    public void theDevportalShouldAdvertiseEndpointUrls(String productIdKey) throws Exception {

        String productId = TestContext.resolve(productIdKey).toString();
        Map<String, String> devportalHeaders = new HashMap<>();
        devportalHeaders.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        JSONObject portalProduct = readJsonUntil(Utils.getDevportalApiDetailURL(Utils.getBaseUrl(), productId),
                devportalHeaders, "devportal API product " + productId, body -> {
                    JSONArray urls = body.optJSONArray("endpointURLs");
                    return urls != null && urls.length() > 0;
                });
        JSONObject urls = portalProduct.getJSONArray("endpointURLs").getJSONObject(0).optJSONObject("URLs");
        Assert.assertTrue(urls != null && (!urls.optString("https").isBlank() || !urls.optString("http").isBlank()),
                "Devportal endpointURLs carry no gateway URL for API product " + productId + ": "
                        + portalProduct.getJSONArray("endpointURLs"));
    }

    /** Devportal read of the product by id, polled until its lifeCycleStatus is the expected state. */
    @Then("The devportal should report API product {string} with lifecycle status {string}")
    public void theDevportalShouldReportLifecycleStatus(String productIdKey, String expectedStatus) throws Exception {

        String productId = TestContext.resolve(productIdKey).toString();
        Map<String, String> devportalHeaders = new HashMap<>();
        devportalHeaders.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        JSONObject portalProduct = readJsonUntil(Utils.getDevportalApiDetailURL(Utils.getBaseUrl(), productId),
                devportalHeaders, "devportal API product " + productId,
                body -> expectedStatus.equals(body.optString("lifeCycleStatus")));
        Assert.assertEquals(portalProduct.optString("id"), productId, "Devportal returned another resource");
    }

    /**
     * Asserts the API product is visible in the DEVPORTAL exactly once and that its devportal representation
     * agrees with the publisher product DTO — the port of the legacy {@code verifyApiProductInPortal}: same id,
     * name, description, provider; {@code lifeCycleStatus} equal to the product's {@code state}; the devportal
     * context is the product context with the version appended; the tier list equals the product's policies; and
     * the operation count equals the total across the product's member APIs.
     *
     * @param productIdKey context key holding the API product id
     */
    @Then("The devportal should report API product {string} exactly once with the same fields")
    public void theDevportalShouldReportProductOnce(String productIdKey) throws Exception {
        devportalShouldReportProductOnce(productIdKey, publisherHeaders());
    }

    /**
     * As above, for a consumer that cannot read the publisher plane: the acting actor reads the devportal, and the
     * reference publisher DTO is read with the named publisher's token.
     */
    @Then("The devportal should report API product {string} exactly once with the same fields as published by {string}")
    public void theDevportalShouldReportProductOnceAsPublishedBy(String productIdKey, String publisherActorRef)
            throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken(
                Identity.resolveActor(Utils.resolveContextPlaceholders(publisherActorRef))));
        devportalShouldReportProductOnce(productIdKey, headers);
    }

    private void devportalShouldReportProductOnce(String productIdKey, Map<String, String> publisherHeaders)
            throws Exception {

        String productId = TestContext.resolve(productIdKey).toString();
        JSONObject product = readJson(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "api-products", productId),
                publisherHeaders, "API product " + productId);

        Map<String, String> devportalHeaders = new HashMap<>();
        devportalHeaders.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        // DevPortal visibility of a just-published product is eventually consistent (the legacy helper polled
        // the listing up to 5 times), so both reads below wait for it rather than failing on a stale index.
        JSONObject portalProduct = readJsonWhenAvailable(
                Utils.getDevportalApiDetailURL(Utils.getBaseUrl(), productId), devportalHeaders,
                "devportal API product " + productId);
        assertSameField(portalProduct, product, "id", productId);
        assertSameField(portalProduct, product, "name", productId);
        assertSameField(portalProduct, product, "description", productId);
        assertSameField(portalProduct, product, "provider", productId);
        // Publisher calls this "state", devportal "lifeCycleStatus". optString returns "" when absent, so an
        // unguarded compare passes vacuously if both drop it. Pin non-blank first.
        String portalLifeCycleStatus = portalProduct.optString("lifeCycleStatus");
        String productState = product.optString("state");
        Assert.assertFalse(productState.isBlank(),
                "Publisher DTO carries no state for API product " + productId + ": " + product);
        Assert.assertFalse(portalLifeCycleStatus.isBlank(),
                "Devportal DTO carries no lifeCycleStatus for API product " + productId + ": " + portalProduct);
        Assert.assertEquals(portalLifeCycleStatus, productState,
                "Devportal lifeCycleStatus does not match the product's state for " + productId);
        // The devportal context carries the version: /<context>/<version> (or {version} substituted in place).
        String context = product.getString("context");
        String version = product.getString("version");
        String expectedContext = context.contains("{version}")
                ? context.replace("{version}", version) : context + "/" + version;
        Assert.assertEquals(portalProduct.optString("context"), expectedContext,
                "Devportal context does not carry the product version for " + productId);
        assertSameStringSet(portalProduct.optJSONArray("tiers") == null ? null
                        : tierNames(portalProduct.getJSONArray("tiers")),
                product.optJSONArray("policies"), "tiers/policies", productId);

        int productOperationCount = 0;
        JSONArray productApis = product.optJSONArray("apis");
        Assert.assertTrue(productApis != null && productApis.length() > 0,
                "API product " + productId + " lists no member APIs: " + product);
        for (int i = 0; i < productApis.length(); i++) {
            JSONArray operations = productApis.getJSONObject(i).optJSONArray("operations");
            productOperationCount += operations == null ? 0 : operations.length();
        }
        JSONArray portalOperations = portalProduct.optJSONArray("operations");
        Assert.assertNotNull(portalOperations, "Devportal product " + productId + " exposes no operations");
        Assert.assertEquals(portalOperations.length(), productOperationCount,
                "Devportal operation count does not match the product's aggregated resources for " + productId);

        Assert.assertEquals(operationKeys(portalOperations), productOperationKeys(productApis),
                "Devportal operations (verb + target) differ from the product's aggregated resources for "
                        + productId);
        assertSameStringSet(portalProduct.optJSONArray("securityScheme"), product.optJSONArray("securityScheme"),
                "securityScheme", productId);
        assertSameStringSet(portalProduct.optJSONArray("tags"), product.optJSONArray("tags"), "tags", productId);
        assertSameStringSet(portalProduct.optJSONArray("transport"), product.optJSONArray("transport"),
                "transport", productId);
        JSONObject portalBusiness = portalProduct.optJSONObject("businessInformation");
        JSONObject productBusiness = product.optJSONObject("businessInformation");
        for (String field : new String[] {"businessOwner", "businessOwnerEmail", "technicalOwner",
                "technicalOwnerEmail"}) {
            Assert.assertEquals(portalBusiness == null ? null : portalBusiness.optString(field, null),
                    productBusiness == null ? null : productBusiness.optString(field, null),
                    "Devportal businessInformation." + field + " differs for " + productId);
        }
        Assert.assertEquals(additionalPropertiesOf(portalProduct), additionalPropertiesOf(product),
                "Devportal additionalProperties differ for " + productId);
        Assert.assertEquals(portalScopesOf(portalProduct), productScopesOf(product),
                "Devportal scopes (name, description, roles) differ for " + productId);

        JSONObject listing = readJsonUntil(Utils.getDevportalApiListURL(Utils.getBaseUrl(), PRODUCT_LIST_PAGE_SIZE),
                devportalHeaders, "devportal listing", body -> countEntriesWithId(body, productId) == 1);
        JSONObject entry = onlyEntryWithId(listing, productId, "devportal listing");
        assertSameField(entry, product, "name", productId);
        assertSameField(entry, product, "description", productId);
        assertSameField(entry, product, "provider", productId);
        assertSameField(entry, product, "context", productId);
        Assert.assertEquals(entry.optString("lifeCycleStatus"), productState,
                "Devportal listing lifeCycleStatus does not match the product's state for " + productId);
        assertSameStringSet(entry.optJSONArray("throttlingPolicies"), product.optJSONArray("policies"),
                "throttlingPolicies/policies", productId);
    }

    private Set<String> operationKeys(JSONArray operations) {
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < operations.length(); i++) {
            JSONObject operation = operations.getJSONObject(i);
            keys.add(operation.optString("verb") + " " + operation.optString("target"));
        }
        return keys;
    }

    private Set<String> productOperationKeys(JSONArray productApis) {
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < productApis.length(); i++) {
            JSONArray operations = productApis.getJSONObject(i).optJSONArray("operations");
            if (operations != null) {
                keys.addAll(operationKeys(operations));
            }
        }
        return keys;
    }

    private Set<String> apiIdsOf(JSONObject product) {
        Set<String> ids = new HashSet<>();
        JSONArray apis = product.optJSONArray("apis");
        if (apis != null) {
            for (int i = 0; i < apis.length(); i++) {
                ids.add(apis.getJSONObject(i).optString("apiId"));
            }
        }
        return ids;
    }

    /** Publisher scopes[].scope → "name|description|sorted bindings", the devportal scopes[] counterpart. */
    private Set<String> productScopesOf(JSONObject product) {
        Set<String> scopes = new HashSet<>();
        JSONArray list = product.optJSONArray("scopes");
        if (list != null) {
            for (int i = 0; i < list.length(); i++) {
                JSONObject scope = list.getJSONObject(i).optJSONObject("scope");
                if (scope != null) {
                    scopes.add(scopeKey(scope.optString("name"), scope.optString("description"),
                            scope.optJSONArray("bindings")));
                }
            }
        }
        return scopes;
    }

    private Set<String> portalScopesOf(JSONObject portalProduct) {
        Set<String> scopes = new HashSet<>();
        JSONArray list = portalProduct.optJSONArray("scopes");
        if (list != null) {
            for (int i = 0; i < list.length(); i++) {
                JSONObject scope = list.getJSONObject(i);
                scopes.add(scopeKey(scope.optString("name"), scope.optString("description"),
                        scope.optJSONArray("roles")));
            }
        }
        return scopes;
    }

    private String scopeKey(String name, String description, JSONArray roles) {
        return name + "|" + description + "|" + new java.util.TreeSet<>(toStringSet(roles));
    }

    /** additionalProperties as a set of "name=value" (both planes expose a list of {name, value, display}). */
    private Set<String> additionalPropertiesOf(JSONObject resource) {
        Set<String> properties = new HashSet<>();
        JSONArray list = resource.optJSONArray("additionalProperties");
        if (list != null) {
            for (int i = 0; i < list.length(); i++) {
                JSONObject property = list.getJSONObject(i);
                properties.add(property.optString("name") + "=" + property.optString("value"));
            }
        }
        return properties;
    }

    /**
     * Asserts the client certificates uploaded to an API PRODUCT list the given alias
     * ({@code GET /apis/{productId}/client-certificates} — products have no separate certificates path, so the
     * apis path is addressed with the product's uuid, exactly as the legacy client did). Confirms the upload
     * actually attached to the product rather than merely returning a success status.
     *
     * @param productIdKey context key holding the API product id
     * @param alias        the certificate alias expected in the listing
     */
    @Then("The client certificates of API product {string} should list alias {string}")
    public void theProductClientCertificatesShouldListAlias(String productIdKey, String alias) throws IOException {
        String productId = TestContext.resolve(productIdKey).toString();
        assertCertificateListingHasAlias(Utils.getClientCertificatesURL(Utils.getBaseUrl(), productId), productId,
                alias);
    }

    /** As above, against the key-type-scoped listing ({@code /client-certs/{keyType}}). */
    @Then("The {string} client certificates of API product {string} should list alias {string}")
    public void theProductTypedClientCertificatesShouldListAlias(String keyType, String productIdKey, String alias)
            throws IOException {
        String productId = TestContext.resolve(productIdKey).toString();
        assertCertificateListingHasAlias(Utils.getClientCertificatesByKeyTypeURL(Utils.getBaseUrl(), productId,
                keyType), productId, alias);
    }

    private void assertCertificateListingHasAlias(String url, String productId, String alias) throws IOException {
        JSONObject listing = readJson(url, publisherHeaders(), "client certificates of API product " + productId);
        JSONArray certificates = listing.optJSONArray("certificates");
        Assert.assertNotNull(certificates,
                "Client-certificate listing for API product " + productId + " has no certificates array: " + listing);
        boolean found = false;
        for (int i = 0; i < certificates.length(); i++) {
            if (alias.equals(certificates.getJSONObject(i).optString("alias"))) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found, "Alias '" + alias + "' is not among the client certificates of API product "
                + productId + ": " + listing);
    }

    /** The publisher-plane auth headers for the acting actor. */
    private Map<String, String> publisherHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        return headers;
    }

    /**
     * Intermediate GET whose body is parsed locally (never published as {@code httpResponse}), guarded so a
     * failed/empty response fails with the status rather than an opaque JSONException.
     */
    private JSONObject readJson(String url, Map<String, String> headers, String what) throws IOException {
        HttpResponse response = SimpleHTTPClient.getInstance().doGet(url, headers);
        Assert.assertTrue(response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isBlank(),
                "Failed to read " + what + ": expected a 2xx response with a body, got "
                        + (response == null ? "no response"
                        : response.getResponseCode() + " / body=" + response.getData()));
        return new JSONObject(response.getData());
    }

    /**
     * Applies {@code accept} to the parsed body, treating a MALFORMED body as not-ready rather than fatal. A
     * JSONException is not retried by {@link Utils#retryUntil} (only IOException is), so parsing inline would let a
     * truncated 2xx during warm-up escape the loop as an opaque parse error instead of polling on.
     */
    private static boolean acceptsParsedBody(String body, java.util.function.Predicate<JSONObject> accept) {
        try {
            return accept.test(new JSONObject(body));
        } catch (org.json.JSONException malformedDuringWarmup) {
            return false;
        }
    }

    /**
     * As {@link #readJson} but waits (within the shared propagation window) for the resource to become readable —
     * for a devportal read of a just-published product, whose visibility is eventually consistent.
     */
    private JSONObject readJsonWhenAvailable(String url, Map<String, String> headers, String what) throws Exception {
        return readJsonUntil(url, headers, what, body -> true);
    }

    /**
     * Intermediate GET retried through the shared {@link Utils#retryUntil} envelope until it returns a 2xx body
     * satisfying {@code accept}, then parsed and asserted here. Nothing is published as {@code httpResponse}.
     */
    private JSONObject readJsonUntil(String url, Map<String, String> headers, String what,
                                     java.util.function.Predicate<JSONObject> accept) throws Exception {
        HttpResponse last = Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                () -> SimpleHTTPClient.getInstance().doGet(url, headers),
                response -> response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isBlank()
                        && acceptsParsedBody(response.getData(), accept));
        Assert.assertTrue(last != null && last.getResponseCode() >= 200 && last.getResponseCode() < 300
                        && last.getData() != null && !last.getData().isBlank(),
                "Failed to read " + what + " within the propagation window: got "
                        + (last == null ? "no response" : last.getResponseCode() + " / body=" + last.getData()));
        JSONObject body = new JSONObject(last.getData());
        Assert.assertTrue(accept.test(body), what + " never reached the expected shape: " + last.getData());
        return body;
    }

    /** Counts the {@code list} entries whose id matches (0 when there is no list at all). */
    private int countEntriesWithId(JSONObject listing, String id) {
        JSONArray list = listing.optJSONArray("list");
        if (list == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < list.length(); i++) {
            if (id.equals(list.getJSONObject(i).optString("id"))) {
                count++;
            }
        }
        return count;
    }

    /** Finds the single {@code list} entry whose id matches, asserting there is EXACTLY one (legacy count == 1). */
    private JSONObject onlyEntryWithId(JSONObject listing, String id, String what) {
        JSONArray list = listing.optJSONArray("list");
        Assert.assertNotNull(list, what + " has no list array: " + listing);
        JSONObject match = null;
        int count = 0;
        for (int i = 0; i < list.length(); i++) {
            if (id.equals(list.getJSONObject(i).optString("id"))) {
                count++;
                match = list.getJSONObject(i);
            }
        }
        Assert.assertEquals(count, 1, "Expected " + id + " exactly once in the " + what + ", found " + count
                + ": " + listing);
        return match;
    }

    /**
     * Asserts a field holds the same NON-BLANK value in a listing entry and in the resource's own representation.
     * optString yields null when absent, so a bare compare would pass vacuously if both representations dropped it.
     */
    private void assertSameField(JSONObject actual, JSONObject expected, String field, String id) {
        // PRESENCE, not non-blankness: an empty description is a legitimate value, so requiring non-blank fails
        // correct products. Requiring the key to EXIST still kills the vacuous case this guard exists for —
        // optString(field, null) returning null on BOTH sides, where null == null passed while testing nothing.
        Assert.assertTrue(expected.has(field),
                "Field '" + field + "' is absent from the reference representation of " + id + ": " + expected);
        String expectedValue = expected.optString(field, null);
        Assert.assertEquals(actual.optString(field, null), expectedValue,
                "Field '" + field + "' differs between the two representations of " + id
                        + ". actual=" + actual + " expected=" + expected);
    }

    /** Asserts two JSON string arrays hold the same set of values (order-independent, like the legacy checks). */
    private void assertSameStringSet(JSONArray actual, JSONArray expected, String what, String id) {
        Set<String> actualValues = toStringSet(actual);
        Set<String> expectedValues = toStringSet(expected);
        Assert.assertEquals(actualValues, expectedValues,
                "'" + what + "' differs between the two representations of " + id + ": actual=" + actualValues
                        + " expected=" + expectedValues);
    }

    private Set<String> toStringSet(JSONArray array) {
        Set<String> values = new HashSet<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                values.add(String.valueOf(array.get(i)));
            }
        }
        return values;
    }

    /**
     * Maps a devportal {@code tiers} array of objects to the set of tier names, for comparison with the
     * publisher product's {@code policies}. The devportal DTO names the field {@code tierName} (not {@code name}),
     * which is what the legacy {@code verifyPolicies} compared.
     */
    private JSONArray tierNames(JSONArray tiers) {
        JSONArray names = new JSONArray();
        for (int i = 0; i < tiers.length(); i++) {
            names.put(tiers.getJSONObject(i).optString("tierName"));
        }
        return names;
    }
}
