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

    /**
     * The acting actor's token for the given plane. For {@code devportal}, the standard devportal token
     * ({@code apim:subscribe}) covers comment add/list. For {@code publisher}, a comment-scoped token is minted
     * here (see {@link #PUBLISHER_COMMENT_SCOPES}).
     */
    private Map<String, String> planeAuthHeaders(String plane) throws IOException {
        return planeAuthHeaders(plane, false);
    }

    /**
     * Auth headers for a comment operation on the given plane. When {@code asModerator} is set, the token also
     * carries {@code apim:admin} — the "special scope added to moderate other comments" declared on the comment
     * PATCH/DELETE operations of both the publisher and devportal API definitions. Without it the ownership check
     * refuses a non-owner with 403 even when the caller is the tenant admin, so moderation is a property of the
     * TOKEN's scopes, not of the actor's role alone. It reaches DELETE only: the edit handlers on both planes gate
     * on ownership alone, with no admin-scope branch.
     */
    private Map<String, String> planeAuthHeaders(String plane, boolean asModerator) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String token;
        if ("publisher".equals(plane)) {
            token = commentToken(PUBLISHER_COMMENT_SCOPES, asModerator);
        } else {
            token = asModerator ? commentToken(DEVPORTAL_COMMENT_SCOPES, true) : Identity.devportalToken();
        }
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + token);
        return headers;
    }

    /**
     * Publisher-plane comment scopes. Comment management needs the dedicated {@code apim:comment_view}/
     * {@code apim:comment_manage} scopes, which the shared publisher token does NOT carry — so a comment-scoped
     * token is minted in this class rather than by widening the shared publisher-token scope list.
     */
    private static final String PUBLISHER_COMMENT_SCOPES = "apim:api_view apim:comment_view apim:comment_manage";

    /**
     * Devportal-plane comment scopes — the same set the shared devportal token carries. Only ever used with the
     * moderator scope appended; the plain devportal token comes from {@link Identity#devportalToken()}.
     */
    private static final String DEVPORTAL_COMMENT_SCOPES = "apim:app_manage apim:sub_manage apim:subscribe";

    /** The extra scope that authorises editing/deleting a comment owned by ANOTHER user. */
    private static final String MODERATOR_SCOPE = "apim:admin";

    /** Context key for a cached comment token of an actor, keyed by the exact scope set it was minted with. */
    private static String commentTokenKey(User actor, String scopes) {
        return "commentToken_" + actor.getUserName() + "_" + scopes;
    }

    /**
     * Mints (and caches) a comment token for the acting actor over the given scope set, using the actor's DCR
     * credentials (registered by the {@code I have valid access tokens as} composite). With
     * {@code asModerator} the moderator scope is appended — the actor must be an admin, since a non-admin is
     * denied {@code apim:admin} and would end up with a token the comment endpoints reject as unauthenticated.
     */
    private String commentToken(String scopes, boolean asModerator) throws IOException {
        String requestedScopes = asModerator ? scopes + " " + MODERATOR_SCOPE : scopes;
        User actor = Identity.actingActor();
        Object cached = TestContext.get(commentTokenKey(actor, requestedScopes));
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
        json.addProperty("scope", requestedScopes);
        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(Utils.getBaseUrl()),
                headers, json.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        // A scope the actor's roles do not permit is silently DROPPED from the issued token, and the endpoint then
        // answers 403 (the token still carries apim:comment_manage, so it authenticates and reaches the ownership
        // check). Asserting the grant here turns that misleading 403 into a clear "the actor cannot hold this scope".
        if (asModerator) {
            String granted = String.valueOf(Utils.extractValueFromPayload(response.getData(), "scope"));
            Assert.assertTrue(granted.contains(MODERATOR_SCOPE), "The " + MODERATOR_SCOPE + " scope was NOT granted "
                    + "to " + actor.getUserName() + " — comment moderation cannot be exercised with this token. "
                    + "Granted scopes: " + granted);
        }
        String token = Utils.extractValueFromPayload(response.getData(), "access_token").toString();
        TestContext.set(commentTokenKey(actor, requestedScopes), token);
        return token;
    }

    private static String commentBody(String content, String category) {
        return new JSONObject().put("content", content).put("category", category).toString();
    }

    /** Guards a 2xx response with a body before parsing, then extracts the {@code id} and stores it. */
    private void storeCommentId(HttpResponse response, String key) throws IOException {
        Assert.assertTrue(response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isBlank(),
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
        HttpResponse response = Requests.post(Utils.getAPIComments(Utils.getBaseUrl(), plane, apiId),
                planeAuthHeaders(plane), commentBody(content, category), Constants.CONTENT_TYPES.APPLICATION_JSON);
        storeCommentId(response, commentKey);
    }

    /** Non-asserting variant used for negatives (e.g. a non-admin cross-plane add). Publishes the response only. */
    @When("I attempt to add a {string} comment {string} with category {string} to API {string}")
    public void iAttemptToAddAComment(String plane, String content, String category, String apiKey)
            throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        Requests.post(Utils.getAPIComments(Utils.getBaseUrl(), plane, apiId), planeAuthHeaders(plane),
                commentBody(content, category), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Adds a REPLY (a comment with {@code parentCommentId} set to the given root) and stores its id. */
    @When("I add a {string} reply {string} to comment {string} of API {string} as {string}")
    public void iAddAReply(String plane, String content, String parentKey, String apiKey, String replyKey)
            throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        String parentId = TestContext.resolve(parentKey).toString();
        HttpResponse response = Requests.post(Utils.getAPIReplyToComment(Utils.getBaseUrl(), plane, apiId, parentId),
                planeAuthHeaders(plane), commentBody(content, "general"), Constants.CONTENT_TYPES.APPLICATION_JSON);
        storeCommentId(response, replyKey);
    }

    /** Retrieves ALL root comments of an API (paginated) on the given plane; publishes the list for assertion. */
    @When("I retrieve all {string} comments of API {string} with limit {int} offset {int}")
    public void iRetrieveAllComments(String plane, String apiKey, int limit, int offset) throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        Requests.get(Utils.getAPIComments(Utils.getBaseUrl(), plane, apiId, limit, offset), planeAuthHeaders(plane));
    }

    /** Retrieves a single comment together with a paginated slice of its replies; publishes it for assertion. */
    @When("I retrieve the {string} comment {string} of API {string} with reply limit {int} offset {int}")
    public void iRetrieveComment(String plane, String commentKey, String apiKey, int replyLimit, int replyOffset)
            throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        String commentId = TestContext.resolve(commentKey).toString();
        Requests.get(Utils.getAPIComment(Utils.getBaseUrl(), plane, apiId, commentId, replyLimit, replyOffset),
                planeAuthHeaders(plane));
    }

    /** Retrieves the paginated replies of a comment on the given plane; publishes the reply list for assertion. */
    @When("I retrieve the {string} replies of comment {string} of API {string} with limit {int} offset {int}")
    public void iRetrieveReplies(String plane, String commentKey, String apiKey, int limit, int offset)
            throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        String commentId = TestContext.resolve(commentKey).toString();
        Requests.get(Utils.getAPICommentReplies(Utils.getBaseUrl(), plane, apiId, commentId, limit, offset),
                planeAuthHeaders(plane));
    }

    /** Edits a comment's content/category (PATCH) on the given plane; publishes the response for assertion. */
    @When("I edit the {string} comment {string} of API {string} to content {string} category {string}")
    public void iEditComment(String plane, String commentKey, String apiKey, String content, String category)
            throws IOException {
        editComment(plane, commentKey, apiKey, content, category, false);
    }

    /**
     * Edits a comment as a MODERATOR — the same PATCH, but authenticated with a token that additionally carries
     * {@code apim:admin}. Exists to prove the scope does NOT extend to editing: both planes' comment-edit handlers
     * gate on ownership alone, so a non-owner is refused 403 even holding it (unlike delete, which does honour it).
     */
    @When("I edit the {string} comment {string} of API {string} to content {string} category {string} as a comment moderator")
    public void iEditCommentAsModerator(String plane, String commentKey, String apiKey, String content,
                                        String category) throws IOException {
        editComment(plane, commentKey, apiKey, content, category, true);
    }

    private void editComment(String plane, String commentKey, String apiKey, String content, String category,
                             boolean asModerator) throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        String commentId = TestContext.resolve(commentKey).toString();
        Requests.patch(Utils.getAPIComment(Utils.getBaseUrl(), plane, apiId, commentId),
                planeAuthHeaders(plane, asModerator), commentBody(content, category),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Deletes a comment on the given plane; publishes the response for assertion (delete cascades to replies). */
    @When("I delete the {string} comment {string} of API {string}")
    public void iDeleteComment(String plane, String commentKey, String apiKey) throws IOException {
        deleteComment(plane, commentKey, apiKey, false);
    }

    /** Deletes a comment as a MODERATOR — see {@link #iEditCommentAsModerator} for why the scope differs. */
    @When("I delete the {string} comment {string} of API {string} as a comment moderator")
    public void iDeleteCommentAsModerator(String plane, String commentKey, String apiKey) throws IOException {
        deleteComment(plane, commentKey, apiKey, true);
    }

    private void deleteComment(String plane, String commentKey, String apiKey, boolean asModerator)
            throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        String commentId = TestContext.resolve(commentKey).toString();
        Requests.delete(Utils.getAPIComment(Utils.getBaseUrl(), plane, apiId, commentId),
                planeAuthHeaders(plane, asModerator));
    }

    // ------------------------------ Ratings (devportal only) ------------------------------

    /** Adds/updates the acting user's rating of an API (PUT {@code /user-rating}); publishes the response. */
    @When("I set my rating of API {string} to {int}")
    public void iSetMyRating(String apiKey, int rating) throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        Requests.put(Utils.getAPIUserRating(Utils.getBaseUrl(), apiId), headers,
                new JSONObject().put("rating", rating).toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Deletes the acting user's rating of an API (DELETE {@code /user-rating}); publishes the response. */
    @When("I delete my rating of API {string}")
    public void iDeleteMyRating(String apiKey) throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        Requests.delete(Utils.getAPIUserRating(Utils.getBaseUrl(), apiId), headers);
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
        Requests.get(Utils.getSubscriptions(Utils.getBaseUrl(), apiId), headers);
    }
}
