/*
 *Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.CommentDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.CommentListDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNull;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class PublisherCommentTest
        extends APIMIntegrationBaseTest {

    private String apiId;
    private String rootCommentIdToAddReplies;
    private String rootCommentIdForDevPortalTest;
    private HttpResponse getCommentWithRepliesResponse;
    private List<String> rootComments;
    private List<String> replies;
    private String replyToRootComment;
    private String updatedTime;
    private String replyToTestDevPortalVisibility;
    private String commentFromNonAdminUser;
    private RestAPIPublisherImpl apiPublisherClientCarbonSuperNewAdmin;
    private RestAPIPublisherImpl apiPublisherClientCarbonSuperNonAdminUser;
    private RestAPIStoreImpl apiStoreClientCarbonSuperNonAdminUser;
    private Gson getCommentsGson;

    @Factory(dataProvider = "userModeDataProvider")
    public PublisherCommentTest(TestUserMode userMode) {
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
        User nonAdminUser;
        String nonAdminUsername;
        getCommentsGson = new Gson();

        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            // Login to API Publisher and Store with CarbonSuper admin
            apiPublisherClientCarbonSuperNewAdmin = new RestAPIPublisherImpl(user.getUserNameWithoutDomain(),
                    user.getPassword(), user.getUserDomain(), publisherURLHttps);

            String apiCreatorPublisherDomain = publisherContext.getContextTenant().getDomain();
            String apiCreatorStoreDomain = storeContext.getContextTenant().getDomain();

            //Login to API Publisher adn Store with CarbonSuper normal user1
            String USER_KEY_NON_ADMIN_USER = "userKey1";
            nonAdminUser = publisherContext.getContextTenant().getTenantUser(USER_KEY_NON_ADMIN_USER);
            nonAdminUsername = nonAdminUser.getUserNameWithoutDomain();
            apiPublisherClientCarbonSuperNonAdminUser = new RestAPIPublisherImpl(nonAdminUsername,
                    nonAdminUser.getPassword(), apiCreatorPublisherDomain, publisherURLHttps);
            apiStoreClientCarbonSuperNonAdminUser = new RestAPIStoreImpl(nonAdminUsername,
                    nonAdminUser.getPassword(), apiCreatorStoreDomain, storeURLHttps);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Deploy and Verify API for Comments Test Cases")
    public void testPublisherDeployAPITest() throws Exception {
        String APIName = "CommentRatingAPI";
        String APIContext = "commentRating";
        String tags = "youtube, video, media";
        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";
        String description = "This is test API create by API manager integration test";
        String providerName = user.getUserName();
        String APIVersion = "1.0.0";

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
    }

    //-------------------------Test Comments------------------------------------------
        /* *
            In some databases(mysql, oracle), the timestamp format do not store milliseconds.
            So, a delay of 1 second is used when adding comments and replies and also when editing comments
            to have different CREATED_TIME and UPDATED_TIME values
        * */

    @Test(dependsOnMethods = {"testPublisherDeployAPITest"}, groups = {"wso2.am"}, description = "Test add root comments for API Test Cases")
    public void testPublisherAddRootCommentsToAPIByAdminTest() throws Exception {
        rootComments = new ArrayList<>();
        for (int i = 1; i < 3; i++) {
            HttpResponse addRootCommentResponse = restAPIPublisher
                    .addComment(apiId, "This is root comment " + i, "general", null);
            assertNotNull(addRootCommentResponse, "Error adding comment");
            assertEquals(addRootCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            CommentDTO addedRootCommentDTO = getCommentsGson
                    .fromJson(addRootCommentResponse.getData().replace("publisher", "PUBLISHER"), CommentDTO.class);
            String rootCommentId = addedRootCommentDTO.getId();
            assertNotNull(rootCommentId, "Comment Id is null");
            rootComments.add(rootCommentId);
            Thread.sleep(1000);
        }
        rootCommentIdToAddReplies = rootComments.get(0);
        rootCommentIdForDevPortalTest = rootComments.get(1);
    }

    @Test(dependsOnMethods = {"testPublisherAddRootCommentsToAPIByAdminTest"}, groups = {"wso2.am"}, description = "Test add replies to root comment Test Cases")
    public void testAddRepliesToRootCommentByAdminTest() throws Exception {
        replies = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            HttpResponse addReplyCommentResponse = restAPIPublisher
                    .addComment(apiId, "This is a reply " + i, "general", rootCommentIdToAddReplies);
            assertNotNull(addReplyCommentResponse, "Error adding comment");
            assertEquals(addReplyCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            CommentDTO addedCommentDTO = getCommentsGson.fromJson(addReplyCommentResponse.getData().replace("publisher", "PUBLISHER"), CommentDTO.class);
            String replyCommentId = addedCommentDTO.getId();
            assertNotNull(replyCommentId, "Comment Id is null");
            replies.add(replyCommentId);
            Thread.sleep(1000);
        }

        // Test add reply to next root comment to test DevPortal visibility
        HttpResponse addReplyCommentResponse = restAPIPublisher
                .addComment(apiId, "This is a reply", "general", rootCommentIdForDevPortalTest);
        assertNotNull(addReplyCommentResponse, "Error adding comment");
        assertEquals(addReplyCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        CommentDTO addedCommentDTO = getCommentsGson.fromJson(addReplyCommentResponse.getData().replace("publisher", "PUBLISHER"), CommentDTO.class);
        replyToTestDevPortalVisibility = addedCommentDTO.getId();
        assertNotNull(replyToTestDevPortalVisibility, "Comment Id is null");

        // Verify added comment with it's replies
        getCommentWithRepliesResponse = restAPIPublisher
                .getComment(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 3, 0);
        assertEquals(getCommentWithRepliesResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        CommentDTO commentWithRepliesCommentDTO = getCommentsGson.fromJson(getCommentWithRepliesResponse.getData().replace("publisher", "PUBLISHER"), CommentDTO.class);
        assertEquals(commentWithRepliesCommentDTO.getContent(), "This is root comment 1", "Comments do not match");
        assertEquals(commentWithRepliesCommentDTO.getCategory(), "general", "Comments do not match");
        assertEquals(commentWithRepliesCommentDTO.getEntryPoint(), CommentDTO.EntryPointEnum.PUBLISHER,
                "Comments do not match");
        assertNotNull(commentWithRepliesCommentDTO.getReplies());
        assertNotNull(commentWithRepliesCommentDTO.getReplies().getList());
        for (CommentDTO reply : commentWithRepliesCommentDTO.getReplies().getList()) {
            assertEquals(reply.getCategory(), "general", "Comments do not match");
            assertEquals(reply.getEntryPoint(), CommentDTO.EntryPointEnum.PUBLISHER, "Comments do not match");
            assertEquals(reply.getParentCommentId(), rootCommentIdToAddReplies, "Comments do not match");
            assertTrue(replies.contains(reply.getId()), "Comments do not match");
        }
        assertEquals(commentWithRepliesCommentDTO.getReplies().getList().size(), 3, "Replies limit does not match");
    }

    @Test(dependsOnMethods = {"testAddRepliesToRootCommentByAdminTest"}, groups = {"wso2.am"}, description = "Verify Pagination of Replies List of a Comment Test Case")
    public void testPublisherPaginatedCommentListTest() throws Exception {
        HttpResponse getCommentToVerifyPagination = restAPIPublisher
                .getComment(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 3, 1);
        assertEquals(getCommentToVerifyPagination.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        CommentDTO getCommentToVerifyPaginationCommentDTO = getCommentsGson.fromJson(getCommentToVerifyPagination.getData().replace("publisher", "PUBLISHER"), CommentDTO.class);
        assertNotNull(getCommentToVerifyPaginationCommentDTO.getReplies());
        assertNotNull(getCommentToVerifyPaginationCommentDTO.getReplies().getList());
        assertEquals(getCommentToVerifyPaginationCommentDTO.getReplies().getList().size(), 2,
                "Replies limit does not match");
        assertEquals(getCommentToVerifyPaginationCommentDTO.getReplies().getList().get(0).getContent(),
                "This is a reply 2", "Offset value does not captured");
        assertEquals(getCommentToVerifyPaginationCommentDTO.getReplies().getList().get(1).getContent(),
                "This is a reply 3", "Offset value does not captured");
    }

    @Test(dependsOnMethods = {"testPublisherPaginatedCommentListTest"}, groups = {"wso2.am"}, description = "Get  all comments Test Case")
    public void testPublisherGetAllCommentsTest() throws Exception {
        HttpResponse getCommentsResponse = restAPIPublisher.getComments(apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 5, 0);
        assertEquals(getCommentsResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        CommentListDTO commentListDTO = getCommentsGson.fromJson(getCommentsResponse.getData().replace("publisher", "PUBLISHER"), CommentListDTO.class);
        assertNotNull(commentListDTO.getCount());
        assertEquals(commentListDTO.getCount().intValue(), 2, "Root comments count do not match");
        assertNotNull(commentListDTO.getList());
        for (CommentDTO rootCommentDTO : commentListDTO.getList()) {
            assertEquals(rootCommentDTO.getCategory(), "general", "Comments do not match");
            assertEquals(rootCommentDTO.getEntryPoint(), CommentDTO.EntryPointEnum.PUBLISHER, "Comments do not match");
            assertNull(rootCommentDTO.getParentCommentId(), "Comments do not match");
            assertTrue(rootComments.contains(rootCommentDTO.getId()), "Comments do not match");
        }
    }

    @Test(dependsOnMethods = {"testPublisherGetAllCommentsTest"}, groups = {"wso2.am"}, description = "Verify Pagination of Root Comment List Test Case")
    public void testPublisherPaginatedRootCommentsTest() throws Exception {
        HttpResponse getRootCommentsToVerifyPagination = restAPIPublisher.getComments(apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 3, 1);
        assertEquals(getRootCommentsToVerifyPagination.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        CommentListDTO getRootCommentsToVerifyPaginationCommentDTO = getCommentsGson
                .fromJson(getRootCommentsToVerifyPagination.getData().replace("publisher", "PUBLISHER"), CommentListDTO.class);
        assertNotNull(getRootCommentsToVerifyPaginationCommentDTO.getList());
        assertEquals(getRootCommentsToVerifyPaginationCommentDTO.getList().size(), 1, "Comments limit does not match");
        assertEquals(getRootCommentsToVerifyPaginationCommentDTO.getList().get(0).getContent(), "This is root comment 1", "Offset value does not captured");
    }

    @Test(dependsOnMethods = {"testPublisherPaginatedRootCommentsTest"}, groups = {"wso2.am"}, description = "Verify Total Number of Comments from Pagination of Root Comment List Test Case")
    public void testPublisherTotalCommentsOfPaginatedRootCommentsTest() throws Exception {
        HttpResponse getRootCommentsToVerifyPagination = restAPIPublisher.getComments(apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 1, 0);
        assertEquals(getRootCommentsToVerifyPagination.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        CommentListDTO getRootCommentsToVerifyPaginationCommentDTO = getCommentsGson
                .fromJson(getRootCommentsToVerifyPagination.getData().replace("publisher", "PUBLISHER"), CommentListDTO.class);
        assertNotNull(getRootCommentsToVerifyPaginationCommentDTO.getList());
        assertEquals(getRootCommentsToVerifyPaginationCommentDTO.getList().size(), 1, "Comments limit does not match");
        assertNotNull(getRootCommentsToVerifyPaginationCommentDTO.getPagination());
        if (!Objects.isNull(getRootCommentsToVerifyPaginationCommentDTO.getPagination().getTotal())) {
            int totalComments = getRootCommentsToVerifyPaginationCommentDTO.getPagination().getTotal();
            assertEquals(totalComments, 2, "Total comments number does not match");
        }
    }

    @Test(dependsOnMethods = {"testPublisherTotalCommentsOfPaginatedRootCommentsTest"}, groups = {"wso2.am"}, description = "Get All Replies of Comment Test Case")
    public void testPublisherGetRepliesOfCommentTest() throws Exception {
        HttpResponse getRepliesResponse = restAPIPublisher
                .getReplies(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 5, 0);
        assertEquals(getRepliesResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        CommentListDTO replyListDTO = getCommentsGson.fromJson(getRepliesResponse.getData().replace("publisher", "PUBLISHER"), CommentListDTO.class);
        assertNotNull(replyListDTO.getCount());
        assertEquals(replyListDTO.getCount().intValue(), 3, "Replies count do not match");
        assertNotNull(replyListDTO.getList());
        for (CommentDTO replyDTO : replyListDTO.getList()) {
            assertEquals(replyDTO.getCategory(), "general", "Comments do not match");
            assertEquals(replyDTO.getEntryPoint(), CommentDTO.EntryPointEnum.PUBLISHER, "Comments do not match");
            assertEquals(replyDTO.getParentCommentId(), rootCommentIdToAddReplies, "Comments do not match");
            assertTrue(replies.contains(replyDTO.getId()), "Comments do not match");
        }
    }

    @Test(dependsOnMethods = {"testPublisherGetRepliesOfCommentTest"}, groups = {"wso2.am"}, description = "Verify Pagination of Replies List of Comment Test Case")
    public void testPublisherPaginationOfRepliesOfCommentTest() throws Exception {
        HttpResponse getRepliesToVerifyPagination = restAPIPublisher
                .getReplies(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 3, 1);
        assertEquals(getRepliesToVerifyPagination.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        CommentListDTO getRepliesToVerifyPaginationCommentListDTO = getCommentsGson
                .fromJson(getRepliesToVerifyPagination.getData().replace("publisher", "PUBLISHER"), CommentListDTO.class);
        assertNotNull(getRepliesToVerifyPaginationCommentListDTO.getList());
        assertEquals(getRepliesToVerifyPaginationCommentListDTO.getList().size(), 2, "Comments limit does not match");
        assertEquals(getRepliesToVerifyPaginationCommentListDTO.getList().get(0).getContent(), "This is a reply 2",
                "Offset value does not captured");
        assertEquals(getRepliesToVerifyPaginationCommentListDTO.getList().get(1).getContent(), "This is a reply 3",
                "Offset value does not captured");
    }

    @Test(dependsOnMethods = {"testPublisherPaginationOfRepliesOfCommentTest"}, groups = {"wso2.am"}, description = "Verify Total Number of Replies from Pagination of Replies List of Comment Test Case")
    public void testPublisherTotalRepliesOfPaginationOfRepliesOfCommentTest() throws Exception {
        HttpResponse getRepliesToVerifyPagination = restAPIPublisher
                .getReplies(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 1, 0);
        assertEquals(getRepliesToVerifyPagination.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        CommentListDTO getRepliesToVerifyPaginationCommentListDTO = getCommentsGson
                .fromJson(getRepliesToVerifyPagination.getData().replace("publisher", "PUBLISHER"), CommentListDTO.class);
        assertNotNull(getRepliesToVerifyPaginationCommentListDTO.getList());
        assertEquals(getRepliesToVerifyPaginationCommentListDTO.getList().size(), 1, "Replies limit does not match");
        assertNotNull(getRepliesToVerifyPaginationCommentListDTO.getPagination());
        if (!Objects.isNull(getRepliesToVerifyPaginationCommentListDTO.getPagination().getTotal())) {
            int totalReplies = getRepliesToVerifyPaginationCommentListDTO.getPagination().getTotal();
            assertEquals(totalReplies, 3, "Total number of replies does not match");
        }
    }

    @Test(dependsOnMethods = {"testPublisherPaginationOfRepliesOfCommentTest"}, groups = {"wso2.am"}, description = "Get  all comments from DevPortal Test Case")
    public void testVerifyDevPortalGetAllCommentsTest() throws Exception {
        HttpResponse getCommentsResponse = restAPIStore.getComments(apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 5, 0);
        assertEquals(getCommentsResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        CommentListDTO commentListDTO = getCommentsGson.fromJson(getCommentsResponse.getData().replace("publisher", "PUBLISHER"), CommentListDTO.class);
        assertNotNull(commentListDTO.getCount());
        assertEquals(commentListDTO.getCount().intValue(), 2, "Root comments count do not match");
        assertNotNull(commentListDTO.getList());
        for (CommentDTO rootCommentDTO : commentListDTO.getList()) {
            assertEquals(rootCommentDTO.getCategory(), "general", "Comments do not match");
            assertEquals(rootCommentDTO.getEntryPoint(), CommentDTO.EntryPointEnum.PUBLISHER, "Comments do not match");
            assertNull(rootCommentDTO.getParentCommentId(), "Comments do not match");
            assertTrue(rootComments.contains(rootCommentDTO.getId()), "Comments do not match");
        }
    }

    @Test(dependsOnMethods = {"testVerifyDevPortalGetAllCommentsTest"}, groups = {"wso2.am"}, description = "Verify Pagination of Root Comment List from DevPortal Test Case")
    public void testVerifyDevPortalPaginatedRootCommentsTest() throws Exception {
        HttpResponse getRootCommentsToVerifyPagination = restAPIStore.getComments(apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 3, 1);
        assertEquals(getRootCommentsToVerifyPagination.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        CommentListDTO getRootCommentsToVerifyPaginationCommentDTO = getCommentsGson
                .fromJson(getRootCommentsToVerifyPagination.getData().replace("publisher", "PUBLISHER"), CommentListDTO.class);
        assertNotNull(getRootCommentsToVerifyPaginationCommentDTO.getList());
        assertEquals(getRootCommentsToVerifyPaginationCommentDTO.getList().size(), 1, "Comments limit does not match");
        assertEquals(getRootCommentsToVerifyPaginationCommentDTO.getList().get(0).getContent(), "This is root comment 1", "Offset value does not captured");
    }

    @Test(dependsOnMethods = {"testVerifyDevPortalPaginatedRootCommentsTest"}, groups = {"wso2.am"}, description = "Get All Replies of Comment from DevPortal Test Case")
    public void testVerifyDevPortalGetRepliesOfCommentTest() throws Exception {
        HttpResponse getRepliesResponse = restAPIStore
                .getReplies(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 5, 0);
        assertEquals(getRepliesResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        CommentListDTO replyListDTO = getCommentsGson.fromJson(getRepliesResponse.getData().replace("publisher", "PUBLISHER"), CommentListDTO.class);
        assertNotNull(replyListDTO.getCount());
        assertEquals(replyListDTO.getCount().intValue(), 3, "Replies count do not match");
        assertNotNull(replyListDTO.getList());
        for (CommentDTO replyDTO : replyListDTO.getList()) {
            assertEquals(replyDTO.getCategory(), "general", "Comments do not match");
            assertEquals(replyDTO.getEntryPoint(), CommentDTO.EntryPointEnum.PUBLISHER, "Comments do not match");
            assertEquals(replyDTO.getParentCommentId(), rootCommentIdToAddReplies, "Comments do not match");
            assertTrue(replies.contains(replyDTO.getId()), "Comments do not match");
        }
    }

    @Test(dependsOnMethods = {"testVerifyDevPortalGetRepliesOfCommentTest"}, groups = {"wso2.am"}, description = "Verify Pagination of Replies List of Comment from DevPortal Test Case")
    public void testVerifyDevPortalPaginationOfRepliesOfCommentTest() throws Exception {
        HttpResponse getRepliesToVerifyPagination = restAPIStore
                .getReplies(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 3, 1);
        assertEquals(getRepliesToVerifyPagination.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        CommentListDTO getRepliesToVerifyPaginationCommentListDTO = getCommentsGson
                .fromJson(getRepliesToVerifyPagination.getData().replace("publisher", "PUBLISHER"), CommentListDTO.class);
        assertNotNull(getRepliesToVerifyPaginationCommentListDTO.getList());
        assertEquals(getRepliesToVerifyPaginationCommentListDTO.getList().size(), 2, "Comments limit does not match");
        assertEquals(getRepliesToVerifyPaginationCommentListDTO.getList().get(0).getContent(), "This is a reply 2",
                "Offset value does not captured");
        assertEquals(getRepliesToVerifyPaginationCommentListDTO.getList().get(1).getContent(), "This is a reply 3",
                "Offset value does not captured");
    }

    @DataProvider(name = "input-data-provider")
    public Object[][] inputDataProviderMethod() {
        return new Object[][]{
                //Edit the content only
                {"Edited root comment", "general"},
                //Edit the category only
                {"Edited root comment", "bug fix"},
                //Edit the category and content
                {"Edited root comment 1", "general bug fix"}
        };
    }

    //Edit a comment
    @Test(dependsOnMethods = {"testVerifyDevPortalPaginationOfRepliesOfCommentTest"}, groups = {"wso2.am"}, description = "Edit Comment Test Case", dataProvider = "input-data-provider")
    public void testPublisherEditCommentTest(String content, String category) throws Exception {
        // sleep added to wait between edit comments due to update time assertions
        Thread.sleep(2000);
        HttpResponse editCommentResponse = restAPIPublisher
                .editComment(rootCommentIdToAddReplies, apiId, content, category);
        assertNotNull(editCommentResponse, "Error adding comment");
        assertEquals(editCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        CommentDTO editCommentDTO = getCommentsGson
                .fromJson(editCommentResponse.getData().replace("publisher", "PUBLISHER"), CommentDTO.class);
        assertEquals(editCommentDTO.getContent(), content);
        assertEquals(editCommentDTO.getCategory(), category);
        assertNotEquals(editCommentDTO.getUpdatedTime(), null);
        if (!content.equals("Edited root comment") || !category.equals("general")) {
            assertNotEquals(editCommentDTO.getUpdatedTime(), updatedTime);
        }
        updatedTime = editCommentDTO.getUpdatedTime();
        //Edit - keep the category and content as it is
        if (content.equals("Edited root comment 1") && category.equals("general bug fix")) {
            Thread.sleep(1000);
            editCommentResponse = restAPIPublisher.editComment(rootCommentIdToAddReplies, apiId, content, category);
            assertNull(editCommentResponse.getData());
            assertEquals(editCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            editCommentResponse = restAPIPublisher
                    .getComment(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(),
                            false, 3, 0);
            assertEquals(getCommentWithRepliesResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Error retrieving comment");
            editCommentDTO = getCommentsGson.fromJson(editCommentResponse.getData().replace("publisher", "PUBLISHER"), CommentDTO.class);
            assertEquals(editCommentDTO.getContent(), content);
            assertEquals(editCommentDTO.getCategory(), category);
            assertNotEquals(editCommentDTO.getUpdatedTime(), null);
            assertEquals(editCommentDTO.getUpdatedTime(), updatedTime);
        }
    }

    @Test(dependsOnMethods = {"testPublisherEditCommentTest"}, groups = {"wso2.am"}, description = "Delete Comment Test Case")
    public void testPublisherDeleteCommentTest() throws Exception {
        HttpResponse deleteResponse = restAPIPublisher.removeComment(rootCommentIdToAddReplies, apiId);
        assertEquals(deleteResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        // Test check whether replies of above deleted root comment are deleted or not
        for (String reply : replies) {
            HttpResponse replyResponse = restAPIPublisher.getComment(reply, apiId, gatewayContextWrk.getContextTenant()
                    .getDomain(), false, 3, 0);
            assertEquals(replyResponse.getResponseCode(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Error retrieving comment");
        }
    }

    @Test(dependsOnMethods = {"testPublisherDeleteCommentTest"}, groups = {"wso2.am"}, description = "Delete Not Existing Comment Test Case")
    public void testPublisherDeleteNotExistingCommentTest() throws Exception {
        HttpResponse deleteResponse = restAPIPublisher.removeComment(rootCommentIdToAddReplies, apiId);
        assertEquals(deleteResponse.getResponseCode(), Response.Status.NOT_FOUND.getStatusCode(),
                "Response code mismatched");
    }

    @Test(dependsOnMethods = {"testPublisherDeleteNotExistingCommentTest"}, groups = {"wso2.am"}, description = "Test Add a Root Comment and Reply to Root Comment Test Cases")
    public void testPublisherAddNewRootCommentWithReplyTest() throws Exception {
        // Test add root comment to test non owner user behavior
        HttpResponse addRootCommentResponse = restAPIPublisher
                .addComment(apiId, "This is root comment", "general", null);
        assertNotNull(addRootCommentResponse, "Error adding comment");
        assertEquals(addRootCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        CommentDTO addedRootCommentDTO = getCommentsGson
                .fromJson(addRootCommentResponse.getData().replace("publisher", "PUBLISHER"), CommentDTO.class);
        rootCommentIdToAddReplies = addedRootCommentDTO.getId();
        assertNotNull(rootCommentIdToAddReplies, "Comment Id is null");
        Thread.sleep(1000);

        // Test add reply to root comment to test non owner user behavior
        HttpResponse addReplyCommentResponse = restAPIPublisher
                .addComment(apiId, "This is a reply", "general", rootCommentIdToAddReplies);
        assertNotNull(addReplyCommentResponse, "Error adding comment");
        assertEquals(addReplyCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        CommentDTO addedCommentDTO = getCommentsGson
                .fromJson(addReplyCommentResponse.getData().replace("publisher", "PUBLISHER"), CommentDTO.class);
        replyToRootComment = addedCommentDTO.getId();
        assertNotNull(replyToRootComment, "Comment Id is null");
    }

    @Test(dependsOnMethods = {"testPublisherAddNewRootCommentWithReplyTest"}, groups = {"wso2.am"}, description = "Edit Comment by Non Owner Non Admin User Test Case")
    public void testPublisherEditCommentByNonOwnerNonAdminUserTest() throws Exception {
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            HttpResponse editCommentResponse = apiPublisherClientCarbonSuperNonAdminUser
                    .editComment(rootCommentIdToAddReplies, apiId, "Edited root comment by non owner non admin user",
                            "general");
            assertNotNull(editCommentResponse, "Error adding comment");
            assertEquals(editCommentResponse.getResponseCode(), Response.Status.FORBIDDEN.getStatusCode(),
                    "Response code mismatched");
        }
    }

    @Test(dependsOnMethods = {"testPublisherEditCommentByNonOwnerNonAdminUserTest"}, groups = {"wso2.am"}, description = "Edit Comment by Non Owner Admin User Test Case")
    public void testPublisherEditCommentByNonOwnerAdminUserTest() throws Exception {
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            HttpResponse editCommentResponse = apiPublisherClientCarbonSuperNewAdmin
                    .editComment(rootCommentIdToAddReplies, apiId, "Edited root comment by non owner admin user",
                            "new_general");
            assertNotNull(editCommentResponse, "Error adding comment");
            assertEquals(editCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            CommentDTO editCommentDTO = getCommentsGson
                    .fromJson(editCommentResponse.getData().replace("publisher", "PUBLISHER"), CommentDTO.class);
            assertEquals(editCommentDTO.getContent(), "Edited root comment by non owner admin user");
            assertEquals(editCommentDTO.getCategory(), "new_general");
        }
    }

    @Test(dependsOnMethods = {"testPublisherEditCommentByNonOwnerAdminUserTest"}, groups = {"wso2.am"}, description = "Add New Comment by Non Admin User Test Case")
    public void testPublisherAddCommentByNonAdminUserTest() throws Exception {
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            Thread.sleep(1000);
            HttpResponse addRootCommentResponse = apiPublisherClientCarbonSuperNonAdminUser
                    .addComment(apiId, "This is root comment by non admin user", "general", null);
            assertNotNull(addRootCommentResponse, "Error adding comment");
            assertEquals(addRootCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            CommentDTO addedRootCommentDTO = getCommentsGson
                    .fromJson(addRootCommentResponse.getData().replace("publisher", "PUBLISHER"), CommentDTO.class);
            commentFromNonAdminUser = addedRootCommentDTO.getId();
            assertNotNull(commentFromNonAdminUser, "Comment Id of non admin user is null");
        }
    }

    @Test(dependsOnMethods = {"testPublisherAddCommentByNonAdminUserTest"}, groups = {"wso2.am"}, description = "Add Reply to Non Admin User Comment by Admin User Test Case")
    public void testPublisherAddReplyToNonAdminUserCommentByAdminUserTest() throws Exception {
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            Thread.sleep(1000);
            HttpResponse addReplyCommentResponse = restAPIPublisher
                    .addComment(apiId, "This is a reply from admin user", "general", commentFromNonAdminUser);
            assertNotNull(addReplyCommentResponse, "Error adding comment");
            assertEquals(addReplyCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            CommentDTO addedCommentDTO = getCommentsGson
                    .fromJson(addReplyCommentResponse.getData().replace("publisher", "PUBLISHER"), CommentDTO.class);
            String replyCommentId = addedCommentDTO.getId();
            assertNotNull(replyCommentId, "Comment Id is null");
        }
    }

    @Test(dependsOnMethods = {"testPublisherAddReplyToNonAdminUserCommentByAdminUserTest"}, groups = {"wso2.am"}, description = "Add Reply to Non Admin User Comment by Non Admin User Test Case")
    public void testPublisherAddReplyToAdminUserCommentByNonAdminUserTest() throws Exception {
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            Thread.sleep(1000);
            HttpResponse addReplyCommentResponse = apiPublisherClientCarbonSuperNonAdminUser
                    .addComment(apiId, "This is a reply from non admin user", "general", rootCommentIdForDevPortalTest);
            assertNotNull(addReplyCommentResponse, "Error adding comment");
            assertEquals(addReplyCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            CommentDTO addedCommentDTO = getCommentsGson
                    .fromJson(addReplyCommentResponse.getData().replace("publisher", "PUBLISHER"), CommentDTO.class);
            String replyCommentId = addedCommentDTO.getId();
            assertNotNull(replyCommentId, "Comment Id is null");
        }
    }

    @Test(dependsOnMethods = {"testPublisherAddReplyToAdminUserCommentByNonAdminUserTest"}, groups = {"wso2.am"}, description = "Add Reply to DevPortal Non Admin User Comment by Admin User Test Case")
    public void testDevPortalNonAdminUserAddReplyToCommentFromPublisherTest() throws Exception {
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            Thread.sleep(1000);
            HttpResponse addReplyCommentResponse = apiStoreClientCarbonSuperNonAdminUser
                    .addComment(apiId, "This is a reply from non admin user from DevPortal", "general",
                            rootCommentIdForDevPortalTest);
            assertNotNull(addReplyCommentResponse, "Error adding comment");
            assertEquals(addReplyCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            CommentDTO addedCommentDTO = getCommentsGson
                    .fromJson(addReplyCommentResponse.getData().replace("devPortal", "DEVPORTAL"), CommentDTO.class);
            String replyCommentId = addedCommentDTO.getId();
            assertNotNull(replyCommentId, "Comment Id is null");
        }
    }

    @Test(dependsOnMethods = {"testDevPortalNonAdminUserAddReplyToCommentFromPublisherTest"}, groups = {"wso2.am"}, description = "Add Reply to DevPortal Admin User Comment by Admin User Test Case")
    public void testDevPortalAdminUserAddReplyToCommentFromPublisherTest() throws Exception {
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            Thread.sleep(1000);
            HttpResponse addReplyCommentResponse = restAPIStore
                    .addComment(apiId, "This is a reply from admin user from DevPortal", "general",
                            rootCommentIdForDevPortalTest);
            assertNotNull(addReplyCommentResponse, "Error adding comment");
            assertEquals(addReplyCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            CommentDTO addedCommentDTO = getCommentsGson
                    .fromJson(addReplyCommentResponse.getData().replace("devPortal", "DEVPORTAL"), CommentDTO.class);
            String replyCommentId = addedCommentDTO.getId();
            assertNotNull(replyCommentId, "Comment Id is null");
        }
    }

    @Test(dependsOnMethods = {"testDevPortalAdminUserAddReplyToCommentFromPublisherTest"}, groups = {"wso2.am"}, description = "Delete Comment by Non Owner Non Admin User Test Case")
    public void testPublisherDeleteCommentByNonOwnerNonAdminUserTest() throws Exception {
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            HttpResponse deleteResponse = apiPublisherClientCarbonSuperNonAdminUser.removeComment(rootCommentIdToAddReplies, apiId);
            assertEquals(deleteResponse.getResponseCode(), Response.Status.FORBIDDEN.getStatusCode(),
                    "Response code mismatched");
        }
    }

    @Test(dependsOnMethods = {"testPublisherDeleteCommentByNonOwnerNonAdminUserTest"}, groups = {"wso2.am"}, description = "Delete Comment by Non Owner Admin User Test Case")
    public void testPublisherDeleteCommentByNonOwnerAdminUserTest() throws Exception {
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            HttpResponse deleteResponse = apiPublisherClientCarbonSuperNewAdmin.removeComment(rootCommentIdToAddReplies, apiId);
            assertEquals(deleteResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            // Test check whether replies of above deleted root comment are deleted or not
            HttpResponse replyResponse = restAPIPublisher
                    .getComment(replyToRootComment, apiId, gatewayContextWrk.getContextTenant().getDomain(),
                            false, 5, 0);
            assertEquals(replyResponse.getResponseCode(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Error retrieving comment");
        }
    }

    @Test(dependsOnMethods = {"testPublisherDeleteCommentByNonOwnerAdminUserTest"}, groups = {"wso2.am"}, description = "Delete Comment from DevPortal Test Case")
    public void testVerifyDevPortalAdminDeleteCommentTest() throws Exception {
        HttpResponse deleteResponse = restAPIStore.removeComment(rootCommentIdForDevPortalTest, apiId);
        assertEquals(deleteResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        // Test check whether reply of above deleted root comment is deleted or not
        HttpResponse replyResponse = restAPIStore.getComment(replyToTestDevPortalVisibility, apiId, gatewayContextWrk.getContextTenant()
                .getDomain(), false, 3, 0);
        assertEquals(replyResponse.getResponseCode(), Response.Status.NOT_FOUND.getStatusCode(),
                "Error retrieving comment");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
    }
}
