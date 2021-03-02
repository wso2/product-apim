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

package org.wso2.am.integration.tests.other;

import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.CommentDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.CommentListDTO;
//import org.wso2.am.integration.clients.publisher.api.v1.dto.RatingDTO;
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
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotEquals;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE }) public class PublisherCommentTest
        extends APIMIntegrationBaseTest {

    private String apiId;

    @Factory(dataProvider = "userModeDataProvider")
    public PublisherCommentTest(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN },
//                new Object[] { TestUserMode.SUPER_TENANT_USER_STORE_USER },
//                new Object[] { TestUserMode.SUPER_TENANT_EMAIL_USER },
//                new Object[] { TestUserMode.TENANT_EMAIL_USER },
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
    }

    @Test(groups = { "wso2.am" }, description = "Comment Rating Test case")
    public void testPublisherCommentTest() throws Exception {
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

        // Test add five root comments
        List<String> rootComments = new ArrayList<String>();
        for (Integer i = 1; i < 6; i++) {
            HttpResponse addRootCommentResponse = restAPIPublisher.addComment(apiId, "This is root comment "+
                    i.toString(), "general", null);
            assertNotNull(addRootCommentResponse, "Error adding comment");
            assertEquals(addRootCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            Gson getCommentsGson = new Gson();
            String test1 = addRootCommentResponse.getData();
            CommentDTO addedRootCommentDTO = getCommentsGson.fromJson(addRootCommentResponse.getData(), CommentDTO.class);
            String rootCommentId = addedRootCommentDTO.getId();
            assertNotNull(rootCommentId, "Comment Id is null");
            rootComments.add(rootCommentId);
        }

        String rootCommentIdToAddReplies = rootComments.get(0);

        // Test add another four replies to above root comment
        List<String> replies = new ArrayList<String>();
        for (Integer i = 1; i < 5; i++) {
            HttpResponse addReplyCommentResponse = restAPIPublisher.addComment(apiId, "This is a reply "+
                    i.toString(), "general", rootCommentIdToAddReplies);
            assertNotNull(addReplyCommentResponse, "Error adding comment");
            assertEquals(addReplyCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            Gson getCommentsGson = new Gson();
            CommentDTO addedCommentDTO = getCommentsGson.fromJson(addReplyCommentResponse.getData(), CommentDTO.class);
            String replyCommentId = addedCommentDTO.getId();
            assertNotNull(replyCommentId, "Comment Id is null");
            replies.add(replyCommentId);
        }

        // Verify added comment with it's replies
        HttpResponse getCommentWithRepliesResponse = restAPIPublisher.getComment(rootCommentIdToAddReplies, apiId,
                gatewayContextWrk.getContextTenant().getDomain(), false, 3,0);
        assertEquals(getCommentWithRepliesResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getCommentWithRepliesGson = new Gson();
        CommentDTO commentWithRepliesCommentDTO = getCommentWithRepliesGson.fromJson(getCommentWithRepliesResponse
                .getData(), CommentDTO.class);
        assertEquals(commentWithRepliesCommentDTO.getContent(), "This is root comment 1",
                "Comments do not match");
        assertEquals(commentWithRepliesCommentDTO.getCategory(), "general", "Comments do not match");
        assertEquals(commentWithRepliesCommentDTO.getEntryPoint(), CommentDTO.EntryPointEnum.PUBLISHER,
                "Comments do not match");
        for (CommentDTO reply: commentWithRepliesCommentDTO.getReplies().getList()){
            assertEquals(reply.getCategory(), "general", "Comments do not match");
            assertEquals(reply.getEntryPoint(), CommentDTO.EntryPointEnum.PUBLISHER, "Comments do not match");
            assertEquals(reply.getParentCommentId(), rootCommentIdToAddReplies, "Comments do not match");
            assertEquals(replies.contains(reply.getId()), true, "Comments do not match");
        }

        // Get  all the comments of  API
        HttpResponse getCommentsResponse = restAPIPublisher.getComments(apiId, gatewayContextWrk.getContextTenant()
                .getDomain(),  false, 5, 0);
        assertEquals(getCommentsResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getCommentsGson = new Gson();
        CommentListDTO commentListDTO = getCommentsGson.fromJson(getCommentsResponse.getData(), CommentListDTO.class);
        assertEquals(commentListDTO.getCount().intValue(),5,"Root comments count do not match");
        for (CommentDTO rootCommentDTO: commentListDTO.getList()){
            assertEquals(rootCommentDTO.getCategory(), "general", "Comments do not match");
            assertEquals(rootCommentDTO.getEntryPoint(), CommentDTO.EntryPointEnum.PUBLISHER,
                    "Comments do not match");
            assertEquals(rootCommentDTO.getParentCommentId(), null, "Comments do not match");
            assertEquals(rootComments.contains(rootCommentDTO.getId()), true, "Comments do not match");
        }

        // Get all the replies of a given comment
        HttpResponse getRepliesResponse = restAPIPublisher.getReplies(rootCommentIdToAddReplies, apiId, gatewayContextWrk
                .getContextTenant().getDomain(), false, 5, 0);
        assertEquals(getRepliesResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getRepliesGson = new Gson();
        CommentListDTO replyListDTO = getRepliesGson.fromJson(getRepliesResponse.getData(), CommentListDTO.class);
        assertEquals(replyListDTO.getCount().intValue(),4,"Replies count do not match");
        for (CommentDTO replyDTO: replyListDTO.getList()){
            assertEquals(replyDTO.getCategory(), "general", "Comments do not match");
            assertEquals(replyDTO.getEntryPoint(), CommentDTO.EntryPointEnum.PUBLISHER, "Comments do not match");
            assertEquals(replyDTO.getParentCommentId(), rootCommentIdToAddReplies, "Comments do not match");
            assertEquals(replies.contains(replyDTO.getId()), true, "Comments do not match");
        }

        //Edit a comment
        //Edit the content only
        HttpResponse editCommentResponse = restAPIPublisher.editComment(rootCommentIdToAddReplies, apiId,
                "Edited root comment", "general");
        assertNotNull(editCommentResponse, "Error adding comment");
        assertEquals(editCommentResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Response code mismatched");
        Gson editCommentGson = new Gson();
        CommentDTO editCommentDTO = editCommentGson.fromJson(editCommentResponse.getData(), CommentDTO.class);
        assertEquals(editCommentDTO.getContent(), "Edited root comment");
        assertEquals(editCommentDTO.getCategory(), "general");
        assertNotEquals(editCommentDTO.getUpdatedTime(),null);
        String updatedTime = editCommentDTO.getUpdatedTime();
        //Edit the category only
        editCommentResponse = restAPIPublisher.editComment(rootCommentIdToAddReplies, apiId, "Edited root comment",
                "bug fix");
        assertNotNull(editCommentResponse, "Error adding comment");
        assertEquals(editCommentResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Response code mismatched");
        editCommentGson = new Gson();
        editCommentDTO = editCommentGson.fromJson(editCommentResponse.getData(), CommentDTO.class);
        assertEquals(editCommentDTO.getContent(), "Edited root comment");
        assertEquals(editCommentDTO.getCategory(), "bug fix");
        assertNotEquals(editCommentDTO.getUpdatedTime(),null);
        assertNotEquals(editCommentDTO.getUpdatedTime(),updatedTime);
        updatedTime = editCommentDTO.getUpdatedTime();
        //Edit the category and content
        editCommentResponse = restAPIPublisher.editComment(rootCommentIdToAddReplies, apiId,"Edited root comment 1",
                "general bug fix");
        assertNotNull(editCommentResponse, "Error adding comment");
        assertEquals(editCommentResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Response code mismatched");
        editCommentGson = new Gson();
        editCommentDTO = editCommentGson.fromJson(editCommentResponse.getData(), CommentDTO.class);
        assertEquals(editCommentDTO.getContent(), "Edited root comment 1");
        assertEquals(editCommentDTO.getCategory(), "general bug fix");
        assertNotEquals(editCommentDTO.getUpdatedTime(),null);
        assertNotEquals(editCommentDTO.getUpdatedTime(),updatedTime);
        updatedTime = editCommentDTO.getUpdatedTime();
        //Edit - keep the category and content as it is
        editCommentResponse = restAPIPublisher.editComment(rootCommentIdToAddReplies, apiId,"Edited root comment 1",
                "general bug fix");
        assertNotNull(editCommentResponse, "Error adding comment");
        assertEquals(editCommentResponse.getResponseCode(), Response.Status.NOT_MODIFIED.getStatusCode(),
                "Response code mismatched");
        editCommentResponse = restAPIPublisher.getComment(rootCommentIdToAddReplies, apiId, gatewayContextWrk
                .getContextTenant().getDomain(), false, 3,0);
        assertEquals(getCommentWithRepliesResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        editCommentGson = new Gson();
        editCommentDTO = editCommentGson.fromJson(editCommentResponse.getData(), CommentDTO.class);
        assertEquals(editCommentDTO.getContent(), "Edited root comment 1");
        assertEquals(editCommentDTO.getCategory(), "general bug fix");
        assertNotEquals(editCommentDTO.getUpdatedTime(),null);
        assertEquals(editCommentDTO.getUpdatedTime(),updatedTime);


        // Test delete comments
        HttpResponse deleteResponse = restAPIPublisher.removeComment(rootCommentIdToAddReplies, apiId);
        assertEquals(deleteResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        // Test check whether replies of above deleted root comment are deleted or not
        for (String reply: replies){
            HttpResponse replyResponse = restAPIPublisher.getComment(reply, apiId, gatewayContextWrk.getContextTenant()
                    .getDomain(), false, 3, 0);
            assertEquals(replyResponse.getResponseCode(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Error retrieving comment");
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }
}
