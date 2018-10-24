/**
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.apimgt.rest.integration.tests.publisher;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.wso2.carbon.apimgt.core.models.APIStatus;
import org.wso2.carbon.apimgt.core.util.APIUtils;
import org.wso2.carbon.apimgt.rest.integration.tests.AMIntegrationTestConstants;
import org.wso2.carbon.apimgt.rest.integration.tests.exceptions.AMIntegrationTestException;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.api.APICollectionApi;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.api.APIIndividualApi;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.api.CommentCollectionApi;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.api.CommentIndividualApi;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.API;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.Comment;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.CommentList;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.FileInfo;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.APIInfo;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.APIList;
import org.wso2.carbon.apimgt.rest.integration.tests.util.SampleTestObjectCreator;
import org.wso2.carbon.apimgt.rest.integration.tests.util.TestUtil;

import java.io.File;
import java.time.Instant;
import java.util.UUID;

public class APILifeCycleTestCaseIT {

    private API api;
    private Comment comment;

    @Test
    public void testCreateApi() throws AMIntegrationTestException {

        APICollectionApi apiCollectionApi = TestUtil.getPublisherApiClient("user1", TestUtil.getUser("user1"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APICollectionApi.class);
        api = SampleTestObjectCreator.ApiToCreate("api1-lifecycle", "1.0.0", "/apiLifecycle");
        api = apiCollectionApi.apisPost(api);
        Assert.assertNotNull(api.getId());
    }

    @Test(dependsOnMethods = {"testCreateApi"})
    public void testUpdateApi() throws AMIntegrationTestException {

        APIIndividualApi apiIndividualApi = TestUtil.getPublisherApiClient("user1", TestUtil.getUser("user1"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APIIndividualApi.class);
        api.addPoliciesItem("Gold");
        api.addTransportItem("http");
        api = apiIndividualApi.apisApiIdPut(api.getId(), api, "", "");
    }

    @Test(dependsOnMethods = {"testUpdateApi"})
    public void testUpdateImage() throws AMIntegrationTestException {

        APIIndividualApi apiIndividualApi = TestUtil.getPublisherApiClient("user1", TestUtil.getUser("user1"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APIIndividualApi.class);
        FileInfo fileInfo = apiIndividualApi.apisApiIdThumbnailPost(api.getId(), new File(Thread.currentThread()
                .getContextClassLoader().getResource("img1.jpg").getPath()), null, null);
        apiIndividualApi.apisApiIdThumbnailGet(api.getId(), null, null);

    }

    @Test(dependsOnMethods = {"testUpdateImage"})
    public void testMakeApiProtoType() throws AMIntegrationTestException {

        APIIndividualApi apiIndividualApi = TestUtil.getPublisherApiClient("user3", TestUtil.getUser("user3"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APIIndividualApi.class);
        apiIndividualApi.apisChangeLifecyclePost(APIStatus.PROTOTYPED.getStatus(), api.getId(),
                AMIntegrationTestConstants.DEFAULT_LIFE_CYCLE_CHECK_LIST, "", "");
        Assert.assertEquals(apiIndividualApi.apisApiIdGet(api.getId(), "", "").getLifeCycleStatus(), APIStatus
                .PROTOTYPED.getStatus());
    }

    @Test(dependsOnMethods = {"testMakeApiProtoType"})
    public void testMakeApiPublishedNegative() throws AMIntegrationTestException {

        APIIndividualApi apiIndividualApi = TestUtil.getPublisherApiClient("user1", TestUtil.getUser("user1"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APIIndividualApi.class);
        try {
            apiIndividualApi.apisChangeLifecyclePost(APIStatus.PUBLISHED.getStatus(), api.getId(),
                    AMIntegrationTestConstants.DEFAULT_LIFE_CYCLE_CHECK_LIST, "", "");
            Assert.fail("Fail due to change lifecycle from non publisher user get success");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test(dependsOnMethods = {"testMakeApiProtoType"})
    public void testMakeApiPublished() throws AMIntegrationTestException {

        APIIndividualApi apiIndividualApi = TestUtil.getPublisherApiClient("user3", TestUtil.getUser("user3"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APIIndividualApi.class);
        apiIndividualApi.apisChangeLifecyclePost(APIStatus.PUBLISHED.getStatus(), api.getId(),
                AMIntegrationTestConstants.DEFAULT_LIFE_CYCLE_CHECK_LIST, "", "");
        Assert.assertEquals(apiIndividualApi.apisApiIdGet(api.getId(), "", "").getLifeCycleStatus(), APIStatus
                .PUBLISHED.getStatus());
    }

    @Test(dependsOnMethods = {"testMakeApiPublished"})
    public void testCopyApiVersion() throws AMIntegrationTestException {

        APIIndividualApi apiIndividualApi = TestUtil.getPublisherApiClient("user1", TestUtil.getUser("user1"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APIIndividualApi.class);
        APICollectionApi apiCollectionApi = TestUtil.getPublisherApiClient("user1", TestUtil.getUser("user1"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APICollectionApi.class);

        apiIndividualApi.apisCopyApiPost("v2.0.0", api.getId());
        org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.APIList apiList = apiCollectionApi.apisGet(2,
                0, "name:api1-lifecycle", "", false);
        Assert.assertNotNull(apiList);
        Assert.assertTrue(apiList.getCount().intValue() > 1);
    }

    @Test(dependsOnMethods = {"testCopyApiVersion"})
    public void testMakeApiDeprecated() throws AMIntegrationTestException {

        APIIndividualApi apiIndividualApi = TestUtil.getPublisherApiClient("user3", TestUtil.getUser("user3"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APIIndividualApi.class);
        apiIndividualApi.apisChangeLifecyclePost(APIStatus.DEPRECATED.getStatus(), api.getId(),
                AMIntegrationTestConstants.DEFAULT_LIFE_CYCLE_CHECK_LIST, "", "");
        Assert.assertEquals(apiIndividualApi.apisApiIdGet(api.getId(), "", "").getLifeCycleStatus(), APIStatus
                .DEPRECATED.getStatus());
        org.wso2.carbon.apimgt.rest.integration.tests.store.api.APICollectionApi apiCollectionApi = TestUtil
                .getStoreApiClient("user4", TestUtil.getUser("user4"), AMIntegrationTestConstants.DEFAULT_SCOPES)
                .buildClient(org.wso2.carbon.apimgt.rest.integration.tests.store.api.APICollectionApi.class);
        APIList apiList = apiCollectionApi.apisGet(10, 0, "", "", "");
        Assert.assertNotNull(apiList);
        Assert.assertNotNull(apiList.getList());
        for (APIInfo apiInfo : apiList.getList()) {
            Assert.assertNotEquals(api.getId(), apiInfo.getId());
        }
    }

    @Test(dependsOnMethods = {"testMakeApiDeprecated"})
    public void testMakeApiRetired() throws AMIntegrationTestException {

        APIIndividualApi apiIndividualApi = TestUtil.getPublisherApiClient("user3", TestUtil.getUser("user3"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APIIndividualApi.class);
        apiIndividualApi.apisChangeLifecyclePost(APIStatus.RETIRED.getStatus(), api.getId(),
                AMIntegrationTestConstants.DEFAULT_LIFE_CYCLE_CHECK_LIST, "", "");
    }

    @Test(dependsOnMethods = {"testCreateApi"})
    public void testGetExpandedAPI() throws AMIntegrationTestException {
        APICollectionApi apiCollectionApi = TestUtil
                .getPublisherApiClient("user1", TestUtil.getUser("user1"), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APICollectionApi.class);
        org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.APIList apiList = apiCollectionApi
                .apisGet(2, 0, "name:api1-lifecycle,version:1.0.0", "", true);
        Assert.assertNotNull(apiList);
        Assert.assertTrue(apiList.getCount() == 1);
        API api = (API) apiList.getList().get(0);
        Assert.assertTrue(api.getTransport().contains("http"));  // This is only available in the expanded api object.
    }
    @Test(dependsOnMethods = "testCreateApi")
    public void testApisApiIdCommentsPost() throws AMIntegrationTestException {
        CommentIndividualApi commentIndividualApi = TestUtil.getPublisherApiClient("user1", TestUtil.getUser("user1"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(CommentIndividualApi.class);
        String apiId = api.getId();
        Instant time = APIUtils.getCurrentUTCTime();
        comment = new Comment();
        comment.setApiId(apiId);
        comment.setCommentText("this is a sample comment");
        comment.setCategory("sample category");
        comment.setParentCommentId(UUID.randomUUID().toString());
        comment.setEntryPoint("APIPublisher");
        comment.setUsername("admin");
        comment.setCreatedBy("admin");
        comment.setLastUpdatedBy("admin");
        comment.setCreatedTime(time.toString());
        comment.setLastUpdatedTime(time.toString());
        comment = commentIndividualApi.apisApiIdCommentsPost(apiId, comment);
        Assert.assertNotNull(comment.getCommentId());
    }

    @Test(dependsOnMethods = "testApisApiIdCommentsPost")
    public void testApisApiIdCommentsCommentIdPut() throws AMIntegrationTestException {
        CommentIndividualApi commentIndividualApi = TestUtil.getPublisherApiClient("user1", TestUtil.getUser("user1"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(CommentIndividualApi.class);
        String apiId = api.getId();
        comment.setCategory("Updated Category");
        comment.setCommentText("Updated comment");
        comment = commentIndividualApi.apisApiIdCommentsCommentIdPut(comment.getCommentId(), apiId, comment, "", "");
        Assert.assertNotNull(comment);
    }

    @Test(dependsOnMethods = "testApisApiIdCommentsCommentIdPut")
    public void testApisApiIdCommentsCommentIdGet() throws AMIntegrationTestException {
        CommentIndividualApi commentIndividualApi = TestUtil.getPublisherApiClient("user1", TestUtil.getUser("user1"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(CommentIndividualApi.class);
        Comment resultComment = commentIndividualApi.apisApiIdCommentsCommentIdGet(comment.getCommentId(), api.getId(),null,null);
        Assert.assertEquals(comment.getCommentId(),resultComment.getCommentId());
    }

    @Test(dependsOnMethods = "testApisApiIdCommentsCommentIdPut")
    public void testApisApiIdCommentsGet() throws AMIntegrationTestException {
        CommentCollectionApi commentCollectionApi = TestUtil.getPublisherApiClient("user1", TestUtil.getUser("user1"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(CommentCollectionApi.class);
        CommentList commentList = commentCollectionApi.apisApiIdCommentsGet(api.getId(),25,0);
        Assert.assertNotEquals(commentList.getCount(),0);
    }

    @Test(dependsOnMethods = "testApisApiIdCommentsCommentIdGet")
    public void testApisApiIdCommentsCommentIdDelete() throws AMIntegrationTestException {
        CommentIndividualApi commentIndividualApi = TestUtil.getPublisherApiClient("user1", TestUtil.getUser("user1"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(CommentIndividualApi.class);
        commentIndividualApi.apisApiIdCommentsCommentIdDelete(comment.getCommentId(), api.getId(),"","");
        CommentCollectionApi commentCollectionApi = TestUtil.getPublisherApiClient("user1", TestUtil.getUser("user1"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(CommentCollectionApi.class);
        CommentList commentList = commentCollectionApi.apisApiIdCommentsGet(api.getId(),25,0);
        Assert.assertEquals(commentList.getCount().toString(),"0");
    }

    @AfterClass
    public void destroy() throws AMIntegrationTestException {

        APIIndividualApi apiIndividualApi = TestUtil.getPublisherApiClient("user1", TestUtil.getUser("user1"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APIIndividualApi.class);
        APICollectionApi apiCollectionApi = TestUtil.getPublisherApiClient("user1", TestUtil.getUser("user1"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APICollectionApi.class);

        org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.APIList apiList = apiCollectionApi.apisGet(10,
                0, "name:api1-lifecycle", "",false);
        for (org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.APIInfo apiInfo : apiList.getList()) {
            if (api.getName().equals(apiInfo.getName())) {
                apiIndividualApi.apisApiIdDelete(apiInfo.getId(), "", "");
            }
        }
    }
}
