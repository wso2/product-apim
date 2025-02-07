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
import org.wso2.am.integration.clients.store.api.v1.dto.CommentDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.CommentListDTO;
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
public class DevPortalCommentTest
        extends APIMIntegrationBaseTest {

    private String apiId;
    private String rootCommentIdToAddReplies;
    private String rootCommentIdForPublisherTest;
    private HttpResponse getCommentWithRepliesResponse;
    private List<String> rootComments;
    private List<String> replies;
    private String replyToRootComment;
    private String updatedTime;
    private String replyToTestPublisherVisibility;
    private String commentFromNonAdminUser;
    private RestAPIStoreImpl apiStoreClientCarbonSuperNewAdmin;
    private RestAPIStoreImpl apiStoreClientCarbonSuperNonAdminUser;
    private RestAPIPublisherImpl apiPublisherClientCarbonSuperNonAdminUser;

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
        User nonAdminUser;
        String nonAdminUsername;

        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            // Login to API Publisher and Store with CarbonSuper admin
            apiStoreClientCarbonSuperNewAdmin = new RestAPIStoreImpl(user.getUserNameWithoutDomain(),
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
    public void testDevPortalDeployAPITest() throws Exception {
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

    @Test(dependsOnMethods = {"testDevPortalDeployAPITest"}, groups = {"wso2.am"}, description = "Test add root comments for API Test Cases")
    public void testDevPortalAddRootCommentsToAPIByAdminTest() throws Exception {
        rootComments = new ArrayList<>();
        for (int i = 1; i < 3; i++) {
            HttpResponse addRootCommentResponse = restAPIStore
                    .addComment(apiId, "This is root comment " + i, "general", null);
            assertNotNull(addRootCommentResponse, "Error adding comment");
            assertEquals(addRootCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            Gson getCommentsGson = new Gson();
            CommentDTO addedRootCommentDTO = getCommentsGson.fromJson(addRootCommentResponse.getData().replace("devPortal", "DEVPORTAL"), CommentDTO.class);
            String rootCommentId = addedRootCommentDTO.getId();
            assertNotNull(rootCommentId, "Comment Id is null");
            rootComments.add(rootCommentId);
            Thread.sleep(1000);
        }
        rootCommentIdToAddReplies = rootComments.get(0);
        rootCommentIdForPublisherTest = rootComments.get(1);
    }

    @Test(dependsOnMethods = {"testDevPortalAddRootCommentsToAPIByAdminTest"}, groups = {"wso2.am"}, description = "Test add replies to root comment Test Cases")
    public void testAddRepliesToRootCommentByAdminTest() throws Exception {
        replies = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            HttpResponse addReplyCommentResponse = restAPIStore
                    .addComment(apiId, "This is a reply " + i, "general", rootCommentIdToAddReplies);
            assertNotNull(addReplyCommentResponse, "Error adding comment");
            assertEquals(addReplyCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            Gson getCommentsGson = new Gson();
            CommentDTO addedCommentDTO = getCommentsGson.fromJson(addReplyCommentResponse.getData().replace("devPortal", "DEVPORTAL"), CommentDTO.class);
            String replyCommentId = addedCommentDTO.getId();
            assertNotNull(replyCommentId, "Comment Id is null");
            replies.add(replyCommentId);
            Thread.sleep(1000);
        }

        // Test add reply to root comment to test Publisher visibility
        HttpResponse addReplyCommentResponse = restAPIStore
                .addComment(apiId, "This is reply", "general", rootCommentIdForPublisherTest);
        assertNotNull(addReplyCommentResponse, "Error adding comment");
        assertEquals(addReplyCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        Gson getCommentsGson = new Gson();
        CommentDTO addedCommentDTO = getCommentsGson.fromJson(addReplyCommentResponse.getData().replace("devPortal", "DEVPORTAL"), CommentDTO.class);
        replyToTestPublisherVisibility = addedCommentDTO.getId();
        assertNotNull(replyToTestPublisherVisibility, "Comment Id is null");

        // Verify added comment with it's replies
        getCommentWithRepliesResponse = restAPIStore
                .getComment(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 3, 0);
        assertEquals(getCommentWithRepliesResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getCommentWithRepliesGson = new Gson();
        CommentDTO commentWithRepliesCommentDTO = getCommentWithRepliesGson.fromJson(getCommentWithRepliesResponse.getData().replace("devPortal", "DEVPORTAL"), CommentDTO.class);
        assertEquals(commentWithRepliesCommentDTO.getContent(), "This is root comment 1", "Comments do not match");
        assertEquals(commentWithRepliesCommentDTO.getCategory(), "general", "Comments do not match");
        assertEquals(commentWithRepliesCommentDTO.getEntryPoint(), CommentDTO.EntryPointEnum.DEVPORTAL,
                "Comments do not match");
        assertNotNull(commentWithRepliesCommentDTO.getReplies());
        assertNotNull(commentWithRepliesCommentDTO.getReplies().getList());
        for (CommentDTO reply : commentWithRepliesCommentDTO.getReplies().getList()) {
            assertEquals(reply.getCategory(), "general", "Comments do not match");
            assertEquals(reply.getEntryPoint(), CommentDTO.EntryPointEnum.DEVPORTAL, "Comments do not match");
            assertEquals(reply.getParentCommentId(), rootCommentIdToAddReplies, "Comments do not match");
            assertTrue(replies.contains(reply.getId()), "Comments do not match");
        }
        assertEquals(commentWithRepliesCommentDTO.getReplies().getList().size(), 3, "Replies limit does not match");
    }

    @Test(dependsOnMethods = {"testAddRepliesToRootCommentByAdminTest"}, groups = { "wso2.am" }, description = "Verify Pagination of Replies List of a Comment Test Case")
    public void testDevPortalPaginatedCommentListTest() throws Exception {
        HttpResponse getCommentToVerifyPagination = restAPIStore
                .getComment(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 3, 1);
        assertEquals(getCommentToVerifyPagination.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getCommentToVerifyPaginationGson = new Gson();
        CommentDTO getCommentToVerifyPaginationCommentDTO = getCommentToVerifyPaginationGson.fromJson(getCommentToVerifyPagination.getData().replace("devPortal", "DEVPORTAL"), CommentDTO.class);
        assertNotNull(getCommentToVerifyPaginationCommentDTO.getReplies());
        assertNotNull(getCommentToVerifyPaginationCommentDTO.getReplies().getList());
        assertEquals(getCommentToVerifyPaginationCommentDTO.getReplies().getList().size(), 2,
                "Replies limit does not match");
        assertEquals(getCommentToVerifyPaginationCommentDTO.getReplies().getList().get(0).getContent(),
                "This is a reply 2", "Offset value does not captured");
        assertEquals(getCommentToVerifyPaginationCommentDTO.getReplies().getList().get(1).getContent(),
                "This is a reply 3", "Offset value does not captured");
    }

    @Test(dependsOnMethods = {"testDevPortalPaginatedCommentListTest"}, groups = { "wso2.am" }, description = "Get  all comments Test Case")
    public void testDevPortalGetAllCommentsTest() throws Exception {
        HttpResponse getCommentsResponse = restAPIStore.getComments(apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 5, 0);
        assertEquals(getCommentsResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getCommentsGson = new Gson();
        CommentListDTO commentListDTO = getCommentsGson.fromJson(getCommentsResponse.getData().replace("devPortal", "DEVPORTAL"), CommentListDTO.class);
        assertNotNull(commentListDTO.getCount());
        assertEquals(commentListDTO.getCount().intValue(), 2, "Root comments count do not match");
        assertNotNull(commentListDTO.getList());
        for (CommentDTO rootCommentDTO : commentListDTO.getList()) {
            assertEquals(rootCommentDTO.getCategory(), "general", "Comments do not match");
            assertEquals(rootCommentDTO.getEntryPoint(), CommentDTO.EntryPointEnum.DEVPORTAL, "Comments do not match");
            assertNull(rootCommentDTO.getParentCommentId(), "Comments do not match");
            assertTrue(rootComments.contains(rootCommentDTO.getId()), "Comments do not match");
        }
    }

    @Test(dependsOnMethods = {"testDevPortalGetAllCommentsTest"}, groups = { "wso2.am" }, description = "Verify Pagination of Root Comment List Test Case")
    public void testDevPortalPaginatedRootCommentsTest() throws Exception {
        HttpResponse getRootCommentsToVerifyPagination = restAPIStore.getComments(apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 3, 1);
        assertEquals(getRootCommentsToVerifyPagination.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getRootCommentsToVerifyPaginationGson = new Gson();
        CommentListDTO getRootCommentsToVerifyPaginationCommentDTO = getRootCommentsToVerifyPaginationGson
                .fromJson(getRootCommentsToVerifyPagination.getData().replace("devPortal", "DEVPORTAL"), CommentListDTO.class);
        assertNotNull(getRootCommentsToVerifyPaginationCommentDTO.getList());
        assertEquals(getRootCommentsToVerifyPaginationCommentDTO.getList().size(), 1, "Comments limit does not match");
        assertEquals(getRootCommentsToVerifyPaginationCommentDTO.getList().get(0).getContent(), "This is root comment 1", "Offset value does not captured");
    }

    @Test(dependsOnMethods = {"testDevPortalPaginatedRootCommentsTest"}, groups = { "wso2.am" }, description = "Verify Total Number of Comments from Pagination of Root Comment List Test Case")
    public void testDevPortalTotalCommentsOfPaginatedRootCommentsTest() throws Exception {
        HttpResponse getRootCommentsToVerifyPagination = restAPIStore.getComments(apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 1, 0);
        assertEquals(getRootCommentsToVerifyPagination.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getRootCommentsToVerifyPaginationGson = new Gson();
        CommentListDTO getRootCommentsToVerifyPaginationCommentDTO = getRootCommentsToVerifyPaginationGson
                .fromJson(getRootCommentsToVerifyPagination.getData().replace("devPortal", "DEVPORTAL"), CommentListDTO.class);
        assertNotNull(getRootCommentsToVerifyPaginationCommentDTO.getList());
        assertEquals(getRootCommentsToVerifyPaginationCommentDTO.getList().size(), 1, "Comments limit does not match");
        assertNotNull(getRootCommentsToVerifyPaginationCommentDTO.getPagination());
        if (!Objects.isNull(getRootCommentsToVerifyPaginationCommentDTO.getPagination().getTotal())) {
            int totalComments = getRootCommentsToVerifyPaginationCommentDTO.getPagination().getTotal();
            assertEquals(totalComments, 2, "Total comments count does not match");
        }
    }

    @Test(dependsOnMethods = {"testDevPortalTotalCommentsOfPaginatedRootCommentsTest"},groups = { "wso2.am" }, description = "Get All Replies of Comment Test Case")
    public void testDevPortalGetRepliesOfCommentTest() throws Exception {
        HttpResponse getRepliesResponse = restAPIStore
                .getReplies(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 5, 0);
        assertEquals(getRepliesResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getRepliesGson = new Gson();
        CommentListDTO replyListDTO = getRepliesGson.fromJson(getRepliesResponse.getData().replace("devPortal", "DEVPORTAL"), CommentListDTO.class);
        assertNotNull(replyListDTO.getCount());
        assertEquals(replyListDTO.getCount().intValue(), 3, "Replies count do not match");
        assertNotNull(replyListDTO.getList());
        for (CommentDTO replyDTO : replyListDTO.getList()) {
            assertEquals(replyDTO.getCategory(), "general", "Comments do not match");
            assertEquals(replyDTO.getEntryPoint(), CommentDTO.EntryPointEnum.DEVPORTAL, "Comments do not match");
            assertEquals(replyDTO.getParentCommentId(), rootCommentIdToAddReplies, "Comments do not match");
            assertTrue(replies.contains(replyDTO.getId()), "Comments do not match");
        }
    }

    @Test(dependsOnMethods = {"testDevPortalGetRepliesOfCommentTest"}, groups = { "wso2.am" }, description = "Verify Pagination of Replies List of Comment Test Case")
    public void testDevPortalPaginationOfRepliesOfCommentTest() throws Exception {
        HttpResponse getRepliesToVerifyPagination = restAPIStore
                .getReplies(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 3, 1);
        assertEquals(getRepliesToVerifyPagination.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getRepliesToVerifyPaginationGson = new Gson();
        CommentListDTO getRepliesToVerifyPaginationCommentListDTO = getRepliesToVerifyPaginationGson
                .fromJson(getRepliesToVerifyPagination.getData().replace("devPortal", "DEVPORTAL"), CommentListDTO.class);
        assertNotNull(getRepliesToVerifyPaginationCommentListDTO.getList());
        assertEquals(getRepliesToVerifyPaginationCommentListDTO.getList().size(), 2, "Comments limit does not match");
        assertEquals(getRepliesToVerifyPaginationCommentListDTO.getList().get(0).getContent(), "This is a reply 2",
                "Offset value does not captured");
        assertEquals(getRepliesToVerifyPaginationCommentListDTO.getList().get(1).getContent(), "This is a reply 3",
                "Offset value does not captured");
    }

    @Test(dependsOnMethods = {"testDevPortalPaginationOfRepliesOfCommentTest"}, groups = { "wso2.am" }, description = "Verify Total Number of Replies of Pagination of Replies List of Comment Test Case")
    public void testDevPortalTotalRepliesOfPaginationOfRepliesOfCommentTest() throws Exception {
        HttpResponse getRepliesToVerifyPagination = restAPIStore
                .getReplies(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 1, 0);
        assertEquals(getRepliesToVerifyPagination.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getRepliesToVerifyPaginationGson = new Gson();
        CommentListDTO getRepliesToVerifyPaginationCommentListDTO = getRepliesToVerifyPaginationGson
                .fromJson(getRepliesToVerifyPagination.getData().replace("devPortal", "DEVPORTAL"), CommentListDTO.class);
        assertNotNull(getRepliesToVerifyPaginationCommentListDTO.getList());
        assertEquals(getRepliesToVerifyPaginationCommentListDTO.getList().size(), 1, "Replies limit does not match");
        assertNotNull(getRepliesToVerifyPaginationCommentListDTO.getPagination());
        if (!Objects.isNull(getRepliesToVerifyPaginationCommentListDTO.getPagination().getTotal())) {
            int totalReplies = getRepliesToVerifyPaginationCommentListDTO.getPagination().getTotal();
            assertEquals(totalReplies, 3, "Total replies count does not match");
        }
    }

    @Test(dependsOnMethods = {"testDevPortalTotalRepliesOfPaginationOfRepliesOfCommentTest"}, groups = { "wso2.am" }, description = "Get and verify all comments from Publisher Test Case")
    public void testVerifyPublisherGetAllCommentsTest() throws Exception {
        HttpResponse getCommentsResponse = restAPIPublisher.getComments(apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 5, 0);
        assertEquals(getCommentsResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getCommentsGson = new Gson();
        CommentListDTO commentListDTO = getCommentsGson
                .fromJson(getCommentsResponse.getData().replace("devPortal", "DEVPORTAL"), CommentListDTO.class);
        assertNotNull(commentListDTO.getCount());
        assertEquals(commentListDTO.getCount().intValue(), 2, "Root comments count do not match");
        assertNotNull(commentListDTO.getList());
        for (CommentDTO rootCommentDTO : commentListDTO.getList()) {
            assertEquals(rootCommentDTO.getCategory(), "general", "Comments do not match");
            assertEquals(rootCommentDTO.getEntryPoint(), CommentDTO.EntryPointEnum.DEVPORTAL, "Comments do not match");
            assertNull(rootCommentDTO.getParentCommentId(), "Comments do not match");
            assertTrue(rootComments.contains(rootCommentDTO.getId()), "Comments do not match");
        }
    }

    @Test(dependsOnMethods = {"testVerifyPublisherGetAllCommentsTest"}, groups = { "wso2.am" }, description = "Verify Pagination of Replies List of a Comment from Publisher Test Case")
    public void testVerifyPublisherPaginatedCommentListTest() throws Exception {
        HttpResponse getCommentToVerifyPagination = restAPIPublisher
                .getComment(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 3, 1);
        assertEquals(getCommentToVerifyPagination.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getCommentToVerifyPaginationGson = new Gson();
        CommentDTO getCommentToVerifyPaginationCommentDTO = getCommentToVerifyPaginationGson
                .fromJson(getCommentToVerifyPagination.getData().replace("devPortal", "DEVPORTAL"), CommentDTO.class);
        assertNotNull(getCommentToVerifyPaginationCommentDTO.getReplies());
        assertNotNull(getCommentToVerifyPaginationCommentDTO.getReplies().getList());
        assertEquals(getCommentToVerifyPaginationCommentDTO.getReplies().getList().size(), 2,
                "Replies limit does not match");
        assertEquals(getCommentToVerifyPaginationCommentDTO.getReplies().getList().get(0).getContent(),
                "This is a reply 2", "Offset value does not captured");
        assertEquals(getCommentToVerifyPaginationCommentDTO.getReplies().getList().get(1).getContent(),
                "This is a reply 3", "Offset value does not captured");
    }

    @Test(dependsOnMethods = {"testVerifyPublisherPaginatedCommentListTest"}, groups = { "wso2.am" }, description = "Get All Replies of Comment from Publisher Test Case")
    public void testVerifyPublisherGetRepliesOfCommentTest() throws Exception {
        HttpResponse getRepliesResponse = restAPIPublisher
                .getReplies(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 5, 0);
        assertEquals(getRepliesResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getRepliesGson = new Gson();
        CommentListDTO replyListDTO = getRepliesGson
                .fromJson(getRepliesResponse.getData().replace("devPortal", "DEVPORTAL"), CommentListDTO.class);
        assertNotNull(replyListDTO.getCount());
        assertEquals(replyListDTO.getCount().intValue(), 3, "Replies count do not match");
        assertNotNull(replyListDTO.getList());
        for (CommentDTO replyDTO : replyListDTO.getList()) {
            assertEquals(replyDTO.getCategory(), "general", "Comments do not match");
            assertEquals(replyDTO.getEntryPoint(), CommentDTO.EntryPointEnum.DEVPORTAL, "Comments do not match");
            assertEquals(replyDTO.getParentCommentId(), rootCommentIdToAddReplies, "Comments do not match");
            assertTrue(replies.contains(replyDTO.getId()), "Comments do not match");
        }
    }

    @Test(dependsOnMethods = {"testVerifyPublisherGetRepliesOfCommentTest"}, groups = { "wso2.am" }, description = "Verify Pagination of Replies List of Comment from Publisher Test Case")
    public void testVerifyPublisherPaginationOfRepliesOfCommentTest() throws Exception {
        HttpResponse getRepliesToVerifyPagination = restAPIPublisher
                .getReplies(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(), false, 3, 1);
        assertEquals(getRepliesToVerifyPagination.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error retrieving comment");
        Gson getRepliesToVerifyPaginationGson = new Gson();
        CommentListDTO getRepliesToVerifyPaginationCommentListDTO = getRepliesToVerifyPaginationGson
                .fromJson(getRepliesToVerifyPagination.getData().replace("devPortal", "DEVPORTAL"), CommentListDTO.class);
        assertNotNull(getRepliesToVerifyPaginationCommentListDTO.getList());
        assertEquals(getRepliesToVerifyPaginationCommentListDTO.getList().size(), 2, "Comments limit does not match");
        assertEquals(getRepliesToVerifyPaginationCommentListDTO.getList().get(0).getContent(), "This is a reply 2",
                "Offset value does not captured");
        assertEquals(getRepliesToVerifyPaginationCommentListDTO.getList().get(1).getContent(), "This is a reply 3",
                "Offset value does not captured");
    }

    @DataProvider (name = "input-data-provider")
    public Object[][] inputDataProviderMethod() {
        return new Object[][] {
                //Edit the content only
                {"Edited root comment", "general"},
                //Edit the category only
                {"Edited root comment", "bug fix"},
                //Edit the category and content
                {"Edited root comment 1", "general bug fix"}
        };
    }

    //Edit a comment
    @Test(dependsOnMethods = {"testVerifyPublisherPaginationOfRepliesOfCommentTest"}, groups = { "wso2.am" }, description = "Edit Comment Test Case", dataProvider = "input-data-provider")
    public void testDevPortalEditCommentTest(String content, String category) throws Exception {
        HttpResponse editCommentResponse = restAPIStore
                .editComment(rootCommentIdToAddReplies, apiId, content, category);
        assertNotNull(editCommentResponse, "Error adding comment");
        assertEquals(editCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        Gson editCommentGson = new Gson();
        CommentDTO editCommentDTO = editCommentGson
                .fromJson(editCommentResponse.getData().replace("devPortal", "DEVPORTAL"), CommentDTO.class);
        assertEquals(editCommentDTO.getContent(), content);
        assertEquals(editCommentDTO.getCategory(), category);
        assertNotEquals(editCommentDTO.getUpdatedTime(), null);
        if (!content.equals("Edited root comment") || !category.equals("general")) {
            assertNotEquals(editCommentDTO.getUpdatedTime(), updatedTime);
        }
        updatedTime = editCommentDTO.getUpdatedTime();
        Thread.sleep(1000);
        //Edit - keep the category and content as it is
        if (content.equals("Edited root comment 1") && category.equals("general bug fix")) {
            editCommentResponse = restAPIStore.editComment(rootCommentIdToAddReplies, apiId, content, category);
            assertNull(editCommentResponse.getData());
            assertEquals(editCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            editCommentResponse = restAPIStore
                    .getComment(rootCommentIdToAddReplies, apiId, gatewayContextWrk.getContextTenant().getDomain(),
                            false, 3, 0);
            assertEquals(getCommentWithRepliesResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Error retrieving comment");
            editCommentGson = new Gson();
            editCommentDTO = editCommentGson
                    .fromJson(editCommentResponse.getData().replace("devPortal", "DEVPORTAL"), CommentDTO.class);
            assertEquals(editCommentDTO.getContent(), content);
            assertEquals(editCommentDTO.getCategory(), category);
            assertNotEquals(editCommentDTO.getUpdatedTime(), null);
            assertEquals(editCommentDTO.getUpdatedTime(), updatedTime);
        }
    }

    @Test(dependsOnMethods = {"testDevPortalEditCommentTest"}, groups = { "wso2.am" }, description = "Delete Comment Test Case")
    public void testDevPortalDeleteCommentTest() throws Exception {
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

    @Test(dependsOnMethods = {"testDevPortalDeleteCommentTest"}, groups = { "wso2.am" }, description = "Delete Not Existing Comment Test Case")
    public void testDevPortalDeleteNotExistingCommentTest() throws Exception {
        HttpResponse deleteResponse = restAPIStore.removeComment(rootCommentIdToAddReplies, apiId);
        assertEquals(deleteResponse.getResponseCode(), Response.Status.NOT_FOUND.getStatusCode(),
                "Response code mismatched");
    }

    @Test(dependsOnMethods = {"testDevPortalDeleteNotExistingCommentTest"}, groups = {"wso2.am"}, description = "Test Add a Root Comment and Reply to Root Comment Test Cases")
    public void testDevPortalAddNewRootCommentWithReplyTest() throws Exception {
        // Test add root comment to test non owner user behavior
        HttpResponse addRootCommentResponse = restAPIStore
                .addComment(apiId, "This is root comment", "general", null);
        assertNotNull(addRootCommentResponse, "Error adding comment");
        assertEquals(addRootCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        Gson getCommentsGson = new Gson();
        CommentDTO addedRootCommentDTO = getCommentsGson
                .fromJson(addRootCommentResponse.getData().replace("devPortal", "DEVPORTAL"), CommentDTO.class);
        rootCommentIdToAddReplies = addedRootCommentDTO.getId();
        assertNotNull(rootCommentIdToAddReplies, "Comment Id is null");
        Thread.sleep(1000);

        // Test add reply to root comment to test non owner user behavior
        HttpResponse addReplyCommentResponse = restAPIStore
                .addComment(apiId, "This is reply", "general", rootCommentIdToAddReplies);
        assertNotNull(addReplyCommentResponse, "Error adding comment");
        assertEquals(addReplyCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        Gson getRepliesGson = new Gson();
        CommentDTO addedCommentDTO = getRepliesGson
                .fromJson(addReplyCommentResponse.getData().replace("devPortal", "DEVPORTAL"), CommentDTO.class);
        replyToRootComment = addedCommentDTO.getId();
        assertNotNull(replyToRootComment, "Comment Id is null");
    }

    @Test(dependsOnMethods = {"testDevPortalAddNewRootCommentWithReplyTest"}, groups = { "wso2.am" }, description = "Edit Comment by Non Owner Non Admin User Test Case")
    public void testDevPortalEditCommentByNonOwnerNonAdminUserTest() throws Exception {
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            HttpResponse editCommentResponse = apiStoreClientCarbonSuperNonAdminUser
                    .editComment(rootCommentIdToAddReplies, apiId, "Edited root comment by non owner non admin user",
                            "general");
            assertNotNull(editCommentResponse, "Error adding comment");
            assertEquals(editCommentResponse.getResponseCode(), Response.Status.FORBIDDEN.getStatusCode(),
                    "Response code mismatched");
        }
    }

    @Test(dependsOnMethods = {"testDevPortalEditCommentByNonOwnerNonAdminUserTest"}, groups = { "wso2.am" }, description = "Edit Comment by Non Owner Admin User Test Case")
    public void testDevPortalEditCommentByNonOwnerAdminUserTest() throws Exception {
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            HttpResponse editCommentResponse = apiStoreClientCarbonSuperNewAdmin
                    .editComment(rootCommentIdToAddReplies, apiId, "Edited root comment by non owner admin user",
                            "new_general");
            assertNotNull(editCommentResponse, "Error adding comment");
            assertEquals(editCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            Gson editCommentGson = new Gson();
            CommentDTO editCommentDTO = editCommentGson
                    .fromJson(editCommentResponse.getData().replace("devPortal", "DEVPORTAL"), CommentDTO.class);
            assertEquals(editCommentDTO.getContent(), "Edited root comment by non owner admin user");
            assertEquals(editCommentDTO.getCategory(), "new_general");
        }
    }

    @Test(dependsOnMethods = { "testDevPortalEditCommentByNonOwnerAdminUserTest" }, groups = { "wso2.am" }, description = "Add New Comment by Non Admin User Test Case")
    public void testDevPortalAddCommentByNonAdminUserTest() throws Exception {
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            Thread.sleep(1000);
            HttpResponse addRootCommentResponse = apiStoreClientCarbonSuperNonAdminUser
                    .addComment(apiId, "This is root comment by non admin user", "general", null);
            assertNotNull(addRootCommentResponse, "Error adding comment");
            assertEquals(addRootCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            Gson getCommentsGson = new Gson();
            CommentDTO addedRootCommentDTO = getCommentsGson
                    .fromJson(addRootCommentResponse.getData().replace("devPortal", "DEVPORTAL"), CommentDTO.class);
            commentFromNonAdminUser = addedRootCommentDTO.getId();
            assertNotNull(commentFromNonAdminUser, "Comment Id of non admin user is null");
        }
    }

    @Test(dependsOnMethods = { "testDevPortalAddCommentByNonAdminUserTest" }, groups = { "wso2.am" }, description = "Add Reply to Non Admin User Comment by Admin User Test Case")
    public void testDevPortalAddReplyToNonAdminUserCommentByAdminUserTest() throws Exception {
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            Thread.sleep(1000);
            HttpResponse addReplyCommentResponse = restAPIStore
                    .addComment(apiId, "This is a reply from admin user", "general", commentFromNonAdminUser);
            assertNotNull(addReplyCommentResponse, "Error adding comment");
            assertEquals(addReplyCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            Gson getCommentsGson = new Gson();
            CommentDTO addedCommentDTO = getCommentsGson
                    .fromJson(addReplyCommentResponse.getData().replace("devPortal", "DEVPORTAL"), CommentDTO.class);
            String replyCommentId = addedCommentDTO.getId();
            assertNotNull(replyCommentId, "Comment Id is null");
        }
    }

    @Test(dependsOnMethods = { "testDevPortalAddReplyToNonAdminUserCommentByAdminUserTest" }, groups = { "wso2.am" }, description = "Add Reply to Non Admin User Comment by Non Admin User Test Case")
    public void testDevPortalAddReplyToAdminUserCommentByNonAdminUserTest() throws Exception {
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            Thread.sleep(1000);
            HttpResponse addReplyCommentResponse = apiStoreClientCarbonSuperNonAdminUser
                    .addComment(apiId, "This is a reply from non admin user", "general", rootCommentIdForPublisherTest);
            assertNotNull(addReplyCommentResponse, "Error adding comment");
            assertEquals(addReplyCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            Gson getCommentsGson = new Gson();
            CommentDTO addedCommentDTO = getCommentsGson
                    .fromJson(addReplyCommentResponse.getData().replace("devPortal", "DEVPORTAL"), CommentDTO.class);
            String replyCommentId = addedCommentDTO.getId();
            assertNotNull(replyCommentId, "Comment Id is null");
        }
    }

    @Test(dependsOnMethods = { "testDevPortalAddReplyToAdminUserCommentByNonAdminUserTest" }, groups = { "wso2.am" }, description = "Add Reply to Publisher Non Admin User Comment by Admin User Test Case")
    public void testPublisherNonAdminUserAddReplyToCommentFromDevPortalTest() throws Exception {
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            Thread.sleep(1000);
            HttpResponse addReplyCommentResponse = apiPublisherClientCarbonSuperNonAdminUser
                    .addComment(apiId, "This is a reply from non admin user from Publisher", "general",
                            rootCommentIdForPublisherTest);
            assertNotNull(addReplyCommentResponse, "Error adding comment");
            assertEquals(addReplyCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            Gson getCommentsGson = new Gson();
            CommentDTO addedCommentDTO = getCommentsGson
                    .fromJson(addReplyCommentResponse.getData().replace("publisher", "PUBLISHER"), CommentDTO.class);
            String replyCommentId = addedCommentDTO.getId();
            assertNotNull(replyCommentId, "Comment Id is null");
        }
    }

    @Test(dependsOnMethods = { "testPublisherNonAdminUserAddReplyToCommentFromDevPortalTest" }, groups = { "wso2.am" }, description = "Add Reply to Publisher Admin User Comment by Admin User Test Case")
    public void testPublisherAdminUserAddReplyToCommentFromDevPortalTest() throws Exception {
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            Thread.sleep(1000);
            HttpResponse addReplyCommentResponse = restAPIPublisher
                    .addComment(apiId, "This is a reply from admin user from Publisher", "general",
                            rootCommentIdForPublisherTest);
            assertNotNull(addReplyCommentResponse, "Error adding comment");
            assertEquals(addReplyCommentResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            Gson getCommentsGson = new Gson();
            CommentDTO addedCommentDTO = getCommentsGson
                    .fromJson(addReplyCommentResponse.getData().replace("publisher", "PUBLISHER"), CommentDTO.class);
            String replyCommentId = addedCommentDTO.getId();
            assertNotNull(replyCommentId, "Comment Id is null");
        }
    }

    @Test(dependsOnMethods = {"testPublisherAdminUserAddReplyToCommentFromDevPortalTest"}, groups = { "wso2.am" }, description = "Delete Comment by Non Owner Non Admin User Test Case")
    public void testDevPortalDeleteCommentByNonOwnerNonAdminUserTest() throws Exception {
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            HttpResponse deleteResponse = apiStoreClientCarbonSuperNonAdminUser.removeComment(rootCommentIdToAddReplies, apiId);
            assertEquals(deleteResponse.getResponseCode(), Response.Status.FORBIDDEN.getStatusCode(),
                    "Response code mismatched");
        }
    }

    @Test(dependsOnMethods = {"testDevPortalDeleteCommentByNonOwnerNonAdminUserTest"}, groups = { "wso2.am" }, description = "Delete Comment by Non Owner Admin User Test Case")
    public void testDevPortalDeleteCommentByNonOwnerAdminUserTest() throws Exception {
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            HttpResponse deleteResponse = apiStoreClientCarbonSuperNewAdmin.removeComment(rootCommentIdToAddReplies, apiId);
            assertEquals(deleteResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
            // Test check whether replies of above deleted root comment are deleted or not
            HttpResponse replyResponse = restAPIStore
                    .getComment(replyToRootComment, apiId, gatewayContextWrk.getContextTenant().getDomain(),
                            false, 5, 0);
            assertEquals(replyResponse.getResponseCode(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Error retrieving comment");
        }
    }

    @Test(dependsOnMethods = {"testDevPortalDeleteCommentByNonOwnerAdminUserTest"}, groups = { "wso2.am" }, description = "Delete Comment from Publisher Test Case")
    public void testVerifyPublisherAdminDeleteCommentTest() throws Exception {
        HttpResponse deleteResponse = restAPIPublisher.removeComment(rootCommentIdForPublisherTest, apiId);
        assertEquals(deleteResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        // Test check whether reply of above deleted root comment is deleted or not
        HttpResponse replyResponse = restAPIStore.getComment(replyToTestPublisherVisibility, apiId, gatewayContextWrk.getContextTenant()
                    .getDomain(), false, 3, 0);
        assertEquals(replyResponse.getResponseCode(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Error retrieving comment");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
    }
}
