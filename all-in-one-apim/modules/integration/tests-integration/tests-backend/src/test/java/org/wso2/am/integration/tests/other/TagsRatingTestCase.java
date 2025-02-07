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

package org.wso2.am.integration.tests.other;

import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class TagsRatingTestCase
        extends APIMIntegrationBaseTest {

    private String apiId;

    @Factory(dataProvider = "userModeDataProvider")
    public TagsRatingTestCase(TestUserMode userMode) {
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

    @Test(groups = {"wso2.am"}, description = "Comment Rating Test case")
    public void testTagsTestCase() throws Exception {
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
    }

    @Test(dependsOnMethods = {"testTagsTestCase"}, groups = { "wso2.am" }, description = "Test and Verify Added Rating Test Case")
    public void testRatingTest() throws Exception {

        //Test and verify added rating
        Integer rating = 4;
        HttpResponse ratingAddResponse = restAPIStore
                .addRating(apiId, 4, gatewayContextWrk.getContextTenant().getDomain());
        assertEquals(ratingAddResponse.getResponseCode(), Response.Status.OK.getStatusCode(), "Error adding rating");
        Gson getRatingsGson = new Gson();
        RatingDTO ratingDTO = getRatingsGson.fromJson(ratingAddResponse.getData(), RatingDTO.class);
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
