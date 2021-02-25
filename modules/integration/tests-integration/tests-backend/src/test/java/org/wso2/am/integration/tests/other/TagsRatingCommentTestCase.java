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
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE }) public class TagsRatingCommentTestCase
        extends APIMIntegrationBaseTest {

    private String apiId;

    @Factory(dataProvider = "userModeDataProvider")
    public TagsRatingCommentTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
//                new Object[] { TestUserMode.TENANT_ADMIN },
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
    public void testTagsRatingCommentTestCase() throws Exception {
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

        // Test add comments
        HttpResponse addCommentResponse = restAPIStore.addComment(apiId, "This is a test comment", "general", null);
        assertNotNull(addCommentResponse, "Error adding comment");
        assertEquals(addCommentResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),"Response code mismatched");
        String rootCommentId = addCommentResponse.getData();
        assertNotNull(rootCommentId, "Comment Id is null");

//        // Verify added comment
//        HttpResponse getCommentResponse = restAPIStore.getComment(rootCommentId, apiId, gatewayContextWrk.getContextTenant().getDomain(), false);
//        assertEquals(getCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),"Error retrieving comment");
//        Gson g = new Gson();
//        CommentDTO rootCommentDTO = g.fromJson(getCommentResponse.getData(), CommentDTO.class);
//        assertEquals(rootCommentDTO.getContent(), "This is a test comment", "Comments do not match");
//        assertEquals(rootCommentDTO.getCategory(), "general", "Comments do not match");
//        assertEquals(rootCommentDTO.getEntryPoint(), CommentDTO.EntryPointEnum.DEVPORTAL, "Comments do not match");

//        // Test add reply to above root comment
//        HttpResponse addReply1CommentResponse = restAPIStore.addComment(apiId, "This is a reply 1", "general", rootCommentId);
//        assertNotNull(addReply1CommentResponse, "Error adding comment");
//        assertEquals(addReply1CommentResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(), "Response code mismatched");
//        String reply1CommentId = addReply1CommentResponse.getData();
//        assertNotNull(reply1CommentId, "Comment Id is null");
//
//
//        // Verify added reply
//        HttpResponse getReply1CommentResponse = restAPIStore.getComment(reply1CommentId, apiId, gatewayContextWrk.getContextTenant().getDomain(), false);
//        assertEquals(getReply1CommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),"Error retrieving comment");
//        Gson g1 = new Gson();
//        CommentDTO reply1CommentDTO = g1.fromJson(getReply1CommentResponse.getData(), CommentDTO.class);
//        assertEquals(reply1CommentDTO.getContent(), "This is a reply 1", "Comments do not match");
//        assertEquals(reply1CommentDTO.getCategory(), "general", "Comments do not match");
//        assertEquals(reply1CommentDTO.getEntryPoint(), CommentDTO.EntryPointEnum.DEVPORTAL, "Comments do not match");
//        assertEquals(reply1CommentDTO.getParentCommentId(), rootCommentId, "Comments do not match");

        // Test add another three replies to above root comment
        List<String> replies = new ArrayList<String>();
//        String[] VALUES = new String[] {"AB","BC","CD","AE"};
        for (Integer i = 1; i < 5; i++) {
            HttpResponse addReplyCommentResponse = restAPIStore.addComment(apiId, "This is a reply "+i.toString(), "general", rootCommentId);
            assertNotNull(addReplyCommentResponse, "Error adding comment");
            assertEquals(addReplyCommentResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(), "Response code mismatched");
            String replyCommentId = addReplyCommentResponse.getData();
            assertNotNull(replyCommentId, "Comment Id is null");
            replies.add(replyCommentId);
        }
        // Verify added comment with it's replies
        HttpResponse getCommentWithRepliesResponse = restAPIStore.getComment(rootCommentId, apiId, gatewayContextWrk.getContextTenant().getDomain(), false);
        assertEquals(getCommentWithRepliesResponse.getResponseCode(), Response.Status.OK.getStatusCode(),"Error retrieving comment");
        Gson getCommentWithRepliesGson = new Gson();
        CommentDTO commentWithRepliesCommentDTO = getCommentWithRepliesGson.fromJson(getCommentWithRepliesResponse.getData(), CommentDTO.class);
        assertEquals(commentWithRepliesCommentDTO.getContent(), "This is a test comment", "Comments do not match");
        assertEquals(commentWithRepliesCommentDTO.getCategory(), "general", "Comments do not match");
        assertEquals(commentWithRepliesCommentDTO.getEntryPoint(), CommentDTO.EntryPointEnum.DEVPORTAL, "Comments do not match");
//        CommentListDTO a = commentWithRepliesCommentDTO.getReplies();
//        List<CommentDTO> bb = commentWithRepliesCommentDTO.getReplies().getList();
        for (CommentDTO reply: commentWithRepliesCommentDTO.getReplies().getList()){
            assertEquals(reply.getCategory(), "general", "Comments do not match");
            assertEquals(reply.getEntryPoint(), CommentDTO.EntryPointEnum.DEVPORTAL, "Comments do not match");
            assertEquals(reply.getParentCommentId(), rootCommentId, "Comments do not match");
//            Arrays.asList(yourArray).contains(yourValue)
            boolean a = replies.contains(reply.getId());
            assertEquals(replies.contains(reply.getId()), true, "Comments do not match");
//            assertEquals(reply1CommentDTO.getContent(), "This is a reply 1", "Comments do not match");

        }



        // Test delete comments
        HttpResponse deleteResponse = restAPIStore.removeComment(rootCommentId, apiId);
        assertEquals(deleteResponse.getResponseCode(), Response.Status.OK.getStatusCode(), "Response code mismatched");

        //-------------------------Test Ratings------------------------------------------

        //Test and verify added rating
        Integer rating = 4;
        HttpResponse ratingAddResponse = restAPIStore
                .addRating(apiId, 4, gatewayContextWrk.getContextTenant().getDomain());
        assertEquals(ratingAddResponse.getResponseCode(), Response.Status.OK.getStatusCode(), "Error adding rating");
        RatingDTO ratingDTO = getCommentWithRepliesGson.fromJson(ratingAddResponse.getData(), RatingDTO.class);
        assertEquals(ratingDTO.getRating(), rating, "Ratings do not match");

        //Test delete rating
        HttpResponse deleteRatingResponse = restAPIStore
                .removeRating(apiId, gatewayContextWrk.getContextTenant().getDomain());
        assertEquals(deleteRatingResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }
}
