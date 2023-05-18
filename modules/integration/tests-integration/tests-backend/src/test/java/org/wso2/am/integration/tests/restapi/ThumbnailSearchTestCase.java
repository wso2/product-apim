/*
 *Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.am.integration.tests.restapi;

import com.google.gson.internal.LinkedTreeMap;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SearchResultListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.net.URL;

public class ThumbnailSearchTestCase extends APIMIntegrationBaseTest {
    private String testApiId;

    @BeforeClass(alwaysRun = true) public void setEnvironment() throws Exception {

        super.init();
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();

        APIPublisherRestClient apiPublisher = new APIPublisherRestClient(publisherURLHttp);

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

    }

    @Test(groups = { "wso2.am" },
            description = "Test whether the API thumbnail information is reflected in the API search results.")
    public void testHasThumbnailVariableWhenSearch() throws Exception {

        //prepare API to create and publish
        String imageFile = "thumbnail.png";
        String apiContext = "testContext";
        String url = "https://localhost:9443/test";
        String apiName = "TestAPI";
        String apiVersion = "1.0.0";
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(url));
        String description = "This is test API create by API manager integration test.";
        apiRequest.setDescription(description);
        apiRequest.setVersion(apiVersion);
        apiRequest.setSandbox(url);
        apiRequest.setResourceMethod("GET");
        apiRequest.setProvider(user.getUserName());

        //set API visibility to public hence this API should be visible to all tenants
        apiRequest.setVisibility("public");
        HttpResponse response = restAPIPublisher.addAPI(apiRequest);
        testApiId = response.getData();
        int responseCode = response.getResponseCode();

        // Assert api creation.
        Assert.assertEquals(responseCode, HttpStatus.SC_CREATED);

        HttpResponse lifecycleResponse = restAPIPublisher.
                changeAPILifeCycleStatusToPublish(testApiId, false);

        // Assert successful lifecycle change
        Assert.assertEquals(lifecycleResponse.getResponseCode(), HttpStatus.SC_OK);

        String imagePath = (new File(System.getProperty("user.dir"))).getParent() + RESTAPITestConstants.PATH_SUBSTRING
                + imageFile;
        File image = new File(imagePath);

        //upload the thumbnail image
        restAPIPublisher.updateAPIThumbnail(testApiId, image);

        //search for the API
        SearchResultListDTO searchResultListDTO = restAPIPublisher.searchAPIs("test");

        //check whether the hasThumbnail variable is set to true
        assert searchResultListDTO.getList() != null;
        LinkedTreeMap searchResultDTO = (LinkedTreeMap) searchResultListDTO.getList().get(0);
        Assert.assertEquals(searchResultDTO.get("hasThumbnail").toString(), "true");
    }

    @AfterClass(alwaysRun = true) public void destroy() throws Exception {
        if (testApiId != null) {
            restAPIPublisher.deleteAPI(testApiId);
        }
        super.cleanUp();
    }
}



