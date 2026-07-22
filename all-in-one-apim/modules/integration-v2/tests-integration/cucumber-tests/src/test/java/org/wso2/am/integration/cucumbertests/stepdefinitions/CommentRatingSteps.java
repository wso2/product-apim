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

import com.google.gson.JsonObject;
import io.cucumber.java.en.When;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Step definitions for API comments and per-user ratings — features not previously ported to integration-v2.
 * Comments live on BOTH the publisher and devportal planes (a comment carries an {@code entryPoint} of
 * {@code publisher} or {@code devPortal}), so each comment step takes a {@code plane} argument and authenticates
 * with that plane's token for the acting actor; ratings are devportal-only. Comments and ratings are
 * sub-resources of the API and cascade-delete with it, so no separate cleanup registration is needed.
 */
public class CommentRatingSteps {

    private final BaseSteps baseSteps = new BaseSteps();

    private String getBaseUrl() {
        return baseSteps.getBaseUrl();
    }

    /**
     * The acting actor's token for the given plane. For {@code devportal}, the standard devportal token
     * ({@code apim:subscribe}) covers comment add/list. For {@code publisher}, comment management needs the
     * dedicated {@code apim:comment_view}/{@code apim:comment_manage} scopes, which the shared publisher token
     * does NOT carry — so a comment-scoped publisher token is minted here (self-contained in this class, keyed by
     * the acting actor's DCR credentials) rather than by widening the shared publisher-token scope list.
     */
    private Map<String, String> planeAuthHeaders(String plane) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String token = "publisher".equals(plane) ? publisherCommentToken() : Identity.devportalToken();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + token);
        return headers;
    }

    /** Context key for the cached comment-scoped publisher token of an actor. */
    private static String commentTokenKey(User actor) {
        return "publisherCommentToken_" + actor.getUserName();
    }

    /**
     * Mints (and caches) a publisher-plane token carrying {@code apim:comment_view apim:comment_manage} for the
     * acting actor, using the actor's DCR credentials (registered by the {@code I have valid access tokens as}
     * composite). Kept local to comments so the shared publisher-token scope list is untouched.
     */
    private String publisherCommentToken() throws IOException {
        User actor = Identity.actingActor();
        Object cached = TestContext.get(commentTokenKey(actor));
        if (cached != null) {
            return cached.toString();
        }
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Basic " + TestContext.get(Identity.dcrCredentialsKey(actor)).toString());
        JsonObject json = new JsonObject();
        json.addProperty("grant_type", "password");
        json.addProperty("username", actor.getUserName());
        json.addProperty("password", actor.getPassword());
        json.addProperty("scope", "apim:api_view apim:comment_view apim:comment_manage");
        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(getBaseUrl()),
                headers, json.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        String token = Utils.extractValueFromPayload(response.getData(), "access_token").toString();
        TestContext.set(commentTokenKey(actor), token);
        return token;
    }

    private static String commentBody(String content, String category) {
        return new JSONObject().put("content", content).put("category", category).toString();
    }

    /** Guards a 2xx response with a body before parsing, then extracts the {@code id} and stores it. */
    private void storeCommentId(HttpResponse response, String key) throws IOException {
        Assert.assertTrue(response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isEmpty(),
                "Failed to add comment: expected a 2xx response with a body, got "
                        + (response == null ? "no response" : response.getResponseCode() + " / body=" + response.getData()));
        TestContext.set(key, Utils.extractValueFromPayload(response.getData(), "id").toString());
    }

    /**
     * Adds a ROOT comment to an API on the given plane and stores the created comment id. Publishes the response
     * for assertion. (The category defaults to {@code general}; a comment's {@code entryPoint} is set by the plane.)
     */
    @When("I add a {string} comment {string} with category {string} to API {string} as {string}")
    public void iAddAComment(String plane, String content, String category, String apiKey, String commentKey)
            throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        HttpResponse response = Requests.post(Utils.getAPIComments(getBaseUrl(), plane, apiId),
                planeAuthHeaders(plane), commentBody(content, category), Constants.CONTENT_TYPES.APPLICATION_JSON);
        storeCommentId(response, commentKey);
    }

    /** Non-asserting variant used for negatives (e.g. a non-admin cross-plane add). Publishes the response only. */
    @When("I attempt to add a {string} comment {string} with category {string} to API {string}")
    public void iAttemptToAddAComment(String plane, String content, String category, String apiKey)
            throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        Requests.post(Utils.getAPIComments(getBaseUrl(), plane, apiId), planeAuthHeaders(plane),
                commentBody(content, category), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Adds a REPLY (a comment with {@code parentCommentId} set to the given root) and stores its id. */
    @When("I add a {string} reply {string} to comment {string} of API {string} as {string}")
    public void iAddAReply(String plane, String content, String parentKey, String apiKey, String replyKey)
            throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        String parentId = TestContext.resolve(parentKey).toString();
        HttpResponse response = Requests.post(Utils.getAPIReplyToComment(getBaseUrl(), plane, apiId, parentId),
                planeAuthHeaders(plane), commentBody(content, "general"), Constants.CONTENT_TYPES.APPLICATION_JSON);
        storeCommentId(response, replyKey);
    }

    /** Retrieves ALL root comments of an API (paginated) on the given plane; publishes the list for assertion. */
    @When("I retrieve all {string} comments of API {string} with limit {int} offset {int}")
    public void iRetrieveAllComments(String plane, String apiKey, int limit, int offset) throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        Requests.get(Utils.getAPIComments(getBaseUrl(), plane, apiId, limit, offset), planeAuthHeaders(plane));
    }

    /** Retrieves a single comment together with a paginated slice of its replies; publishes it for assertion. */
    @When("I retrieve the {string} comment {string} of API {string} with reply limit {int} offset {int}")
    public void iRetrieveComment(String plane, String commentKey, String apiKey, int replyLimit, int replyOffset)
            throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        String commentId = TestContext.resolve(commentKey).toString();
        Requests.get(Utils.getAPIComment(getBaseUrl(), plane, apiId, commentId, replyLimit, replyOffset),
                planeAuthHeaders(plane));
    }

    /** Retrieves the paginated replies of a comment on the given plane; publishes the reply list for assertion. */
    @When("I retrieve the {string} replies of comment {string} of API {string} with limit {int} offset {int}")
    public void iRetrieveReplies(String plane, String commentKey, String apiKey, int limit, int offset)
            throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        String commentId = TestContext.resolve(commentKey).toString();
        Requests.get(Utils.getAPICommentReplies(getBaseUrl(), plane, apiId, commentId, limit, offset),
                planeAuthHeaders(plane));
    }

    /** Edits a comment's content/category (PATCH) on the given plane; publishes the response for assertion. */
    @When("I edit the {string} comment {string} of API {string} to content {string} category {string}")
    public void iEditComment(String plane, String commentKey, String apiKey, String content, String category)
            throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        String commentId = TestContext.resolve(commentKey).toString();
        Requests.patch(Utils.getAPIComment(getBaseUrl(), plane, apiId, commentId), planeAuthHeaders(plane),
                commentBody(content, category), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Deletes a comment on the given plane; publishes the response for assertion (delete cascades to replies). */
    @When("I delete the {string} comment {string} of API {string}")
    public void iDeleteComment(String plane, String commentKey, String apiKey) throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        String commentId = TestContext.resolve(commentKey).toString();
        Requests.delete(Utils.getAPIComment(getBaseUrl(), plane, apiId, commentId), planeAuthHeaders(plane));
    }

    // ------------------------------ Ratings (devportal only) ------------------------------

    /** Adds/updates the acting user's rating of an API (PUT {@code /user-rating}); publishes the response. */
    @When("I set my rating of API {string} to {int}")
    public void iSetMyRating(String apiKey, int rating) throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        Requests.put(Utils.getAPIUserRating(getBaseUrl(), apiId), headers,
                new JSONObject().put("rating", rating).toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Deletes the acting user's rating of an API (DELETE {@code /user-rating}); publishes the response. */
    @When("I delete my rating of API {string}")
    public void iDeleteMyRating(String apiKey) throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        Requests.delete(Utils.getAPIUserRating(getBaseUrl(), apiId), headers);
    }

    // ------------------------------ API overview: subscriptions ------------------------------

    /**
     * Retrieves the subscriptions of an API from the PUBLISHER plane ({@code GET /subscriptions?apiId=}) — the
     * "Users" view of an API's overview (which applications/subscribers are subscribed). Publishes the list for
     * assertion (a subscription count or a subscriber name). Used by the API-overview documentation scenario.
     */
    @When("I retrieve the subscriptions of API {string}")
    public void iRetrieveTheSubscriptionsOfApi(String apiKey) throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.get(Utils.getSubscriptions(getBaseUrl(), apiId), headers);
    }
}
