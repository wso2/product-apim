package org.wso2.carbon.apimgt.rest.integration.tests.store;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.carbon.apimgt.core.util.APIUtils;
import org.wso2.carbon.apimgt.rest.integration.tests.AMIntegrationTestConstants;
import org.wso2.carbon.apimgt.rest.integration.tests.exceptions.AMIntegrationTestException;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.api.APICollectionApi;
import org.wso2.carbon.apimgt.rest.integration.tests.store.api.CommentCollectionApi;
import org.wso2.carbon.apimgt.rest.integration.tests.store.api.CommentIndividualApi;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Comment;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.API;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.CommentList;
import org.wso2.carbon.apimgt.rest.integration.tests.util.SampleTestObjectCreator;
import org.wso2.carbon.apimgt.rest.integration.tests.util.TestUtil;

import java.time.Instant;
import java.util.UUID;

public class APICommentsTestCaseIT {

    private Comment comment;
    private API api;

    @Test
    public void testCreateApi() throws AMIntegrationTestException {

        APICollectionApi apiCollectionApi = TestUtil.getPublisherApiClient("user1", TestUtil.getUser("user1"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APICollectionApi.class);
        api = SampleTestObjectCreator.ApiToCreate("api1-lifecycle", "1.0.0", "/apiLifecycle");
        api = apiCollectionApi.apisPost(api);
        Assert.assertNotNull(api.getId());
    }

    @Test(dependsOnMethods = "testCreateApi")
    public void testApisApiIdCommentsPost() throws AMIntegrationTestException {
        CommentIndividualApi commentIndividualApi = TestUtil.getStoreApiClient("user4", TestUtil.getUser
                ("user4"), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(CommentIndividualApi.class);
        String apiId = api.getId();
        Instant time = APIUtils.getCurrentUTCTime();
        comment = new Comment();
        comment.setApiId(apiId);
        comment.setCommentText("this is a sample comment");
        comment.setCategory("sample category");
        comment.setParentCommentId(UUID.randomUUID().toString());
        comment.setEntryPoint("APIStore");
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
        CommentIndividualApi commentIndividualApi = TestUtil.getStoreApiClient("user4", TestUtil.getUser
                ("user4"), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(CommentIndividualApi.class);
        String apiId = api.getId();
        comment.setCategory("Updated Category");
        comment.setCommentText("Updated comment");
        comment = commentIndividualApi.apisApiIdCommentsCommentIdPut(comment.getCommentId(), apiId, comment, "", "");
        Assert.assertNotNull(comment);
    }

    @Test(dependsOnMethods = "testApisApiIdCommentsCommentIdPut")
    public void testApisApiIdCommentsCommentIdGet() throws AMIntegrationTestException {
        CommentIndividualApi commentIndividualApi = TestUtil.getStoreApiClient("user4", TestUtil.getUser
                ("user4"), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(CommentIndividualApi.class);
        Comment resultComment = commentIndividualApi.apisApiIdCommentsCommentIdGet(comment.getCommentId(), api.getId(),null,null);
        Assert.assertEquals(comment.getCommentId(),resultComment.getCommentId());
    }

    @Test(dependsOnMethods = "testApisApiIdCommentsCommentIdPut")
    public void testApisApiIdCommentsGet() throws AMIntegrationTestException {
        CommentCollectionApi commentCollectionApi = TestUtil.getStoreApiClient("user4", TestUtil.getUser
                ("user4"), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(CommentCollectionApi.class);
        CommentList commentList = commentCollectionApi.apisApiIdCommentsGet(api.getId(),25,0);
        Assert.assertNotEquals(commentList.getCount(),0);
    }

    @Test(dependsOnMethods = "testApisApiIdCommentsCommentIdGet")
    public void testApisApiIdCommentsCommentIdDelete() throws AMIntegrationTestException {
        CommentIndividualApi commentIndividualApi = TestUtil.getStoreApiClient("user4", TestUtil.getUser
                ("user4"), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(CommentIndividualApi.class);
        commentIndividualApi.apisApiIdCommentsCommentIdDelete(comment.getCommentId(), api.getId(),"","");
        CommentCollectionApi commentCollectionApi = TestUtil.getStoreApiClient("user4", TestUtil.getUser
                ("user4"), AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(CommentCollectionApi.class);
        CommentList commentList = commentCollectionApi.apisApiIdCommentsGet(api.getId(),25,0);
        Assert.assertEquals(commentList.getCount().toString(),"0");
    }
}
