/*
 *Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.am.integration.tests.comments;

import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.store.api.v1.dto.CommentDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.CommentListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.RatingDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class DevPortalCommentTest
        extends APIMIntegrationBaseTest {

    String apiId;
    String rootCommentIdToAddReplies;
    HttpResponse getCommentWithRepliesResponse;
    Gson getCommentWithRepliesGson;
    List<String> rootComments;
    List<String> replies;

    @Factory(dataProvider = "userModeDataProvider")
    public DevPortalCommentTest(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
                new Object[]{TestUserMode.SUPER_TENANT_USER_STORE_USER},
                new Object[]{TestUserMode.SUPER_TENANT_EMAIL_USER},
                new Object[]{TestUserMode.TENANT_EMAIL_USER},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
    }

    @Test(groups = {"wso2.am"}, description = "Deploy and Verify API for Comments Test Cases")
    public void testDevPortalDeployAPITest() throws Exception {
        String APIName = "CommentRatingAPI";
        String APIContext = "commentRating";
        String tags = "youtube, video, media";
        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";
        String description = "This is test API create by API manager integration test";
        String providerName = user.getUserName();
        String APIVersion = "1.0.0";
        String apiData;

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setProvider(providerName);

        //add api
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = serviceResponse.getData();

        //publish the api
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);

        //get api
        HttpResponse serviceGetResponse = restAPIPublisher.getAPI(apiId);
        apiData = serviceGetResponse.getData();
        JSONObject apiObject = new JSONObject(apiData);

        // Test tags
        JSONArray tagsList = (JSONArray) apiObject.get("tags");

        for (int i = 0; i < tagsList.length(); i++) {
            assertTrue(tags.contains(tagsList.getString(i)), "API tag data mismatched");
        }

        //-------------------------Test Comments------------------------------------------
        /* *
            In some databases(mysql, oracle), the timestamp format do not store milliseconds.
            So, a delay of 1 second is used when adding comments and replies and also when editing comments
            to have different CREATED_TIME and UPDATED_TIME values
        * */

        // Test add two root comments
        rootComments = new ArrayList<String>();
        for (Integer i = 1; i < 3; i++) {
            HttpResponse addRootCommentResponse = restAPIStore
                    .addComment(apiId, "This is root comment " + i.toString(), "general", null);
            assertNotNull(addRootCommentResponse, "Error adding comment");
            assertEquals(addRootCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            Gson getCommentsGson = new Gson();
            CommentDTO addedRootCommentDTO = getCommentsGson.fromJson(addRootCommentResponse.getData().replace("devportal", "DEVPORTAL"), CommentDTO.class);
            String rootCommentId = addedRootCommentDTO.getId();
            assertNotNull(rootCommentId, "Comment Id is null");
            rootComments.add(rootCommentId);
            Thread.sleep(1000);
        }
        rootCommentIdToAddReplies = rootComments.get(0);

        // Test add another three replies to above root comment
        replies = new ArrayList<String>();
        for (Integer i = 1; i < 4; i++) {
            HttpResponse addReplyCommentResponse = restAPIStore
                    .addComment(apiId, "This is a reply " + i.toString(), "general", rootCommentIdToAddReplies);
            assertNotNull(addReplyCommentResponse, "Error adding comment");
            assertEquals(addReplyCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            Gson getCommentsGson = new Gson();
            CommentDTO addedCommentDTO = getCommentsGson.fromJson(addReplyCommentResponse.getData().replace("devportal", "DEVPORTAL"), CommentDTO.class);
            String replyCommentId = addedCommentDTO.getId();
            assertNotNull(replyCommentId, "Comment Id is null");
            replies.add(replyCommentId);
            Thread.sleep(1000);
        }

        // Verify added comment with it's replies
        getCommentWithRepliesResponse = restAPIStore
                .getComment(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 3, 0);
        assertEquals(getCommentWithRepliesResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        getCommentWithRepliesGson = new Gson();
        CommentDTO commentWithRepliesCommentDTO = getCommentWithRepliesGson.fromJson(getCommentWithRepliesResponse.getData().replace("devportal", "DEVPORTAL"), CommentDTO.class);
        assertEquals(commentWithRepliesCommentDTO.getContent(), "This is root comment 1", "Comments do not match");
        assertEquals(commentWithRepliesCommentDTO.getCategory(), "general", "Comments do not match");
        assertEquals(commentWithRepliesCommentDTO.getEntryPoint(), CommentDTO.EntryPointEnum.DEVPORTAL,
                "Comments do not match");
        for (CommentDTO reply : commentWithRepliesCommentDTO.getReplies().getList()) {
            assertEquals(reply.getCategory(), "general", "Comments do not match");
            assertEquals(reply.getEntryPoint(), CommentDTO.EntryPointEnum.DEVPORTAL, "Comments do not match");
            assertEquals(reply.getParentCommentId(), rootCommentIdToAddReplies, "Comments do not match");
            assertEquals(replies.contains(reply.getId()), true, "Comments do not match");
        }
        assertEquals(commentWithRepliesCommentDTO.getReplies().getList().size(), 3, "Replies limit does not match");
    }

    @Test(dependsOnMethods = {"testDevPortalDeployAPITest"}, groups = { "wso2.am" }, description = "Verify Pagination of Replies List of a Comment Test Case")
    public void testDevPortalPaginatedCommentListTest() throws Exception {
        // Verify Pagination of replies list of a comment
        HttpResponse getCommentToVerifyPagination = restAPIStore
                .getComment(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 3, 1);
        assertEquals(getCommentToVerifyPagination.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getCommentToVerifyPaginationGson = new Gson();
        CommentDTO getCommentToVerifyPaginationCommentDTO = getCommentToVerifyPaginationGson.fromJson(getCommentToVerifyPagination.getData().replace("devportal", "DEVPORTAL"), CommentDTO.class);
        assertEquals(getCommentToVerifyPaginationCommentDTO.getReplies().getList().size(), 2,
                "Replies limit does not match");
        assertEquals(getCommentToVerifyPaginationCommentDTO.getReplies().getList().get(0).getContent(),
                "This is a reply 2", "Offset value does not captured");
        assertEquals(getCommentToVerifyPaginationCommentDTO.getReplies().getList().get(1).getContent(),
                "This is a reply 3", "Offset value does not captured");
    }

    @Test(dependsOnMethods = {"testDevPortalPaginatedCommentListTest"}, groups = { "wso2.am" }, description = "Get  all comments Test Case")
    public void testDevPortalGetAllCommentsTest() throws Exception {
        // Get  all the comments of  API
        HttpResponse getCommentsResponse = restAPIStore.getComments(apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 5, 0);
        assertEquals(getCommentsResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getCommentsGson = new Gson();
        CommentListDTO commentListDTO = getCommentsGson.fromJson(getCommentsResponse.getData().replace("devportal", "DEVPORTAL"), CommentListDTO.class);
        assertEquals(commentListDTO.getCount().intValue(), 2, "Root comments count do not match");
        for (CommentDTO rootCommentDTO : commentListDTO.getList()) {
            assertEquals(rootCommentDTO.getCategory(), "general", "Comments do not match");
            assertEquals(rootCommentDTO.getEntryPoint(), CommentDTO.EntryPointEnum.DEVPORTAL, "Comments do not match");
            assertEquals(rootCommentDTO.getParentCommentId(), null, "Comments do not match");
            assertEquals(rootComments.contains(rootCommentDTO.getId()), true, "Comments do not match");
        }
    }

    @Test(dependsOnMethods = {"testDevPortalGetAllCommentsTest"}, groups = { "wso2.am" }, description = "Verify Pagination of Root Comment List Test Case")
    public void testDevPortalPaginatedRootCommentsTest() throws Exception {
        // Verify Pagination of root comment list
        HttpResponse getRootCommentsToVerifyPagination = restAPIStore.getComments(apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 3, 1);
        assertEquals(getRootCommentsToVerifyPagination.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getRootCommentsToVerifyPaginationGson = new Gson();
        CommentListDTO getRootCommentsToVerifyPaginationCommentDTO = getRootCommentsToVerifyPaginationGson
                .fromJson(getRootCommentsToVerifyPagination.getData().replace("devportal", "DEVPORTAL"), CommentListDTO.class);
        assertEquals(getRootCommentsToVerifyPaginationCommentDTO.getList().size(), 1, "Comments limit does not match");
        assertEquals(getRootCommentsToVerifyPaginationCommentDTO.getList().get(0).getContent(), "This is root comment 1", "Offset value does not captured");
    }

    @Test(dependsOnMethods = {"testDevPortalPaginatedRootCommentsTest"},groups = { "wso2.am" }, description = "Get All Replies of Comment Test Case")
    public void testDevPortalGetRepliesOfCommentTest() throws Exception {
        // Get all the replies of a given comment
        HttpResponse getRepliesResponse = restAPIStore
                .getReplies(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 5, 0);
        assertEquals(getRepliesResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getRepliesGson = new Gson();
        CommentListDTO replyListDTO = getRepliesGson.fromJson(getRepliesResponse.getData().replace("devportal", "DEVPORTAL"), CommentListDTO.class);
        assertEquals(replyListDTO.getCount().intValue(), 3, "Replies count do not match");
        for (CommentDTO replyDTO : replyListDTO.getList()) {
            assertEquals(replyDTO.getCategory(), "general", "Comments do not match");
            assertEquals(replyDTO.getEntryPoint(), CommentDTO.EntryPointEnum.DEVPORTAL, "Comments do not match");
            assertEquals(replyDTO.getParentCommentId(), rootCommentIdToAddReplies, "Comments do not match");
            assertEquals(replies.contains(replyDTO.getId()), true, "Comments do not match");
        }
    }

    @Test(dependsOnMethods = {"testDevPortalGetRepliesOfCommentTest"}, groups = { "wso2.am" }, description = "Verify Pagination of Replies List of Comment Test Case")
    public void testDevPortalPaginationOfRepliesOfCommentTest() throws Exception {
        // Verify Pagination of replies list of a comment
        HttpResponse getRepliesToVerifyPagination = restAPIStore
                .getReplies(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 3, 1);
        assertEquals(getRepliesToVerifyPagination.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getRepliesToVerifyPaginationGson = new Gson();
        CommentListDTO getRepliesToVerifyPaginationCommenListDTO = getRepliesToVerifyPaginationGson
                .fromJson(getRepliesToVerifyPagination.getData().replace("devportal", "DEVPORTAL"), CommentListDTO.class);
        assertEquals(getRepliesToVerifyPaginationCommenListDTO.getList().size(), 2, "Comments limit does not match");
        assertEquals(getRepliesToVerifyPaginationCommenListDTO.getList().get(0).getContent(), "This is a reply 2",
                "Offset value does not captured");
        assertEquals(getRepliesToVerifyPaginationCommenListDTO.getList().get(1).getContent(), "This is a reply 3",
                "Offset value does not captured");
    }

    //Edit a comment
    @Test(dependsOnMethods = {"testDevPortalPaginationOfRepliesOfCommentTest"}, groups = { "wso2.am" }, description = "Edit Comment Test Case")
    public void testDevPortalEditCommentTest() throws Exception {
        //Edit the content only
        HttpResponse editCommentResponse = restAPIStore
                .editComment(rootCommentIdToAddReplies, apiId, "Edited root comment", "general");
        assertNotNull(editCommentResponse, "Error adding comment");
        assertEquals(editCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        Gson editCommentGson = new Gson();
        CommentDTO editCommentDTO = editCommentGson.fromJson(editCommentResponse.getData().replace("devportal", "DEVPORTAL"), CommentDTO.class);
        assertEquals(editCommentDTO.getContent(), "Edited root comment");
        assertEquals(editCommentDTO.getCategory(), "general");
        assertNotEquals(editCommentDTO.getUpdatedTime(), null);
        String updatedTime = editCommentDTO.getUpdatedTime();
        //Edit the category only
        Thread.sleep(1000);
        editCommentResponse = restAPIStore
                .editComment(rootCommentIdToAddReplies, apiId, "Edited root comment", "bug fix");
        assertNotNull(editCommentResponse, "Error adding comment");
        assertEquals(editCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        editCommentGson = new Gson();
        editCommentDTO = editCommentGson.fromJson(editCommentResponse.getData().replace("devportal", "DEVPORTAL"), CommentDTO.class);
        assertEquals(editCommentDTO.getContent(), "Edited root comment");
        assertEquals(editCommentDTO.getCategory(), "bug fix");
        assertNotEquals(editCommentDTO.getUpdatedTime(), null);
        assertNotEquals(editCommentDTO.getUpdatedTime(), updatedTime);
        updatedTime = editCommentDTO.getUpdatedTime();
        //Edit the category and content
        Thread.sleep(1000);
        editCommentResponse = restAPIStore
                .editComment(rootCommentIdToAddReplies, apiId, "Edited root comment 1", "general bug fix");
        assertNotNull(editCommentResponse, "Error adding comment");
        assertEquals(editCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        editCommentGson = new Gson();
        editCommentDTO = editCommentGson.fromJson(editCommentResponse.getData().replace("devportal", "DEVPORTAL"), CommentDTO.class);
        assertEquals(editCommentDTO.getContent(), "Edited root comment 1");
        assertEquals(editCommentDTO.getCategory(), "general bug fix");
        assertNotEquals(editCommentDTO.getUpdatedTime(), null);
        assertNotEquals(editCommentDTO.getUpdatedTime(), updatedTime);
        updatedTime = editCommentDTO.getUpdatedTime();
        //Edit - keep the category and content as it is
        Thread.sleep(1000);
        editCommentResponse = restAPIStore
                .editComment(rootCommentIdToAddReplies, apiId, "Edited root comment 1", "general bug fix");
        assertEquals(editCommentResponse.getData(), null);
        assertEquals(editCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        editCommentResponse = restAPIStore
                .getComment(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 3, 0);
        assertEquals(getCommentWithRepliesResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        editCommentGson = new Gson();
        editCommentDTO = editCommentGson.fromJson(editCommentResponse.getData().replace("devportal", "DEVPORTAL"), CommentDTO.class);
        assertEquals(editCommentDTO.getContent(), "Edited root comment 1");
        assertEquals(editCommentDTO.getCategory(), "general bug fix");
        assertNotEquals(editCommentDTO.getUpdatedTime(), null);
        assertEquals(editCommentDTO.getUpdatedTime(), updatedTime);
    }

    @Test(dependsOnMethods = {"testDevPortalEditCommentTest"}, groups = { "wso2.am" }, description = "Delete Comment Test Case")
    public void testDevPortalDeleteCommentTest() throws Exception {
        // Test delete comments
        HttpResponse deleteResponse = restAPIStore.removeComment(rootCommentIdToAddReplies, apiId);
        assertEquals(deleteResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        // Test check whether replies of above deleted root comment are deleted or not
        for (String reply : replies) {
            HttpResponse replyResponse = restAPIStore.getComment(reply, apiId, gatewayContextWrk.getContextTenant()
                    .getDomain(), false, 3, 0);
            assertEquals(replyResponse.getResponseCode(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Error retrieving comment");
        }
    }

    @Test(dependsOnMethods = {"testDevPortalDeleteCommentTest"}, groups = { "wso2.am" }, description = "Test and Verify Added Rating Test Case")
    public void testDevPortalAddedRatingTest() throws Exception {
        //Test and verify added rating
        Integer rating = 4;
        HttpResponse ratingAddResponse = restAPIStore.addRating(apiId, 4, gatewayContextWrk.getContextTenant().getDomain());
        assertEquals(ratingAddResponse.getResponseCode(), Response.Status.OK.getStatusCode(), "Error adding rating");
        RatingDTO ratingDTO = getCommentWithRepliesGson.fromJson(ratingAddResponse.getData(), RatingDTO.class);
        assertEquals(ratingDTO.getRating(), rating, "Ratings do not match");

        //Test delete rating
        HttpResponse deleteRatingResponse = restAPIStore.removeRating(apiId, gatewayContextWrk.getContextTenant().getDomain());
        assertEquals(deleteRatingResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }
}