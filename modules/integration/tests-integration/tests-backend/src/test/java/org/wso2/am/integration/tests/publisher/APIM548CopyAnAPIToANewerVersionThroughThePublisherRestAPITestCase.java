/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.tests.publisher;

import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * APIM2-548:Copy an API to a newer version through the publisher REST API
 */

public class APIM548CopyAnAPIToANewerVersionThroughThePublisherRestAPITestCase extends
        APIMIntegrationBaseTest {

    private final String apiNameTest = "APIM548PublisherTest";
    private final String apiOldVersion = "1.0.0";
    private final String apiNewVersion = "2.0.0";
    private APIPublisherRestClient apiPublisher;
    private String apiProviderName;
    private String apiProductionEndPointUrl;
    private String apiId;
    private String copyAPIId;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM548CopyAnAPIToANewerVersionThroughThePublisherRestAPITestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
//                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        String apiProductionEndpointPostfixUrl = "jaxrs_basic/services/customers/" +
                "customerservice/customers/123";

        String publisherURLHttp = publisherUrls.getWebAppURLHttp();

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);

        apiProductionEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() +
                apiProductionEndpointPostfixUrl;
        apiProviderName = publisherContext.getContextTenant().getContextUser().getUserName();

    }

    @Test(groups = {"wso2.am"}, description = "Copy An API To A Newer Version Through the " +
            "Publisher Rest API")
    public void testCopyAnAPIToANewerVersionThroughThePublisherRest() throws Exception {

        String apiContextTest = "apim548PublisherTestAPI";
        String apiDescription = "This is Test API Created by API Manager Integration Test";
        String apiTag = "tag548-1, tag548-2, tag548-3";
        String defaultVersion = "default_version";
        
        //Create an API
        APIRequest apiCreationRequestBean = new APIRequest(apiNameTest, apiContextTest,
                new URL(apiProductionEndPointUrl));
        apiCreationRequestBean.setVersion(apiOldVersion);
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTiersCollection("Gold,Bronze");
        apiCreationRequestBean.setDefault_version("default_version");
        apiCreationRequestBean.setDefault_version_checked("default_version");
        apiCreationRequestBean.setBusinessOwner("api548b");
        apiCreationRequestBean.setBusinessOwnerEmail("api548b@ee.com");
        apiCreationRequestBean.setTechnicalOwner("api548t");
        apiCreationRequestBean.setTechnicalOwnerEmail("api548t@ww.com");

        HttpResponse apiCreationResponse = restAPIPublisher.addAPI(apiCreationRequestBean );
        apiId = apiCreationResponse.getData();
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Response Code miss matched when creating the API");


        //Check availability of the API in publisher
        JSONObject jsonObject = null;
        Gson gson = new Gson();

        boolean available = false;
        int maxRetry = 10;
        int currentTry = 0;
        do {
            Thread.sleep(2000);
            APIListDTO apiResponsePublisher = restAPIPublisher
                    .getAllAPIs();
            if (apiResponsePublisher != null) {
                String json = gson.toJson(apiResponsePublisher);
                jsonObject = new JSONObject(json);
                available = true;
                break;
            }
            
            currentTry++;
        } while (currentTry <= maxRetry);

        assertTrue(available, "API not available");
        assertTrue(jsonObject.getString("list").contains(apiNameTest),
                apiNameTest + " is not visible in publisher");
        assertTrue(jsonObject.getString("list").contains(apiOldVersion),
                "Version of the " + apiNameTest + "is not a valid version");

        //Create a new copy of the API and validate the result
        HttpResponse copyResponse = restAPIPublisher.copyAPI(apiNewVersion, apiId, true);
        copyAPIId = copyResponse.getData();
        assertEquals(copyResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when copying the API");


        //Check if the New Version of the API is available in Publisher
 
        JSONObject allApiObject = null;
        available = false;
        maxRetry = 10;
        currentTry = 0;
        do {
            Thread.sleep(2000);
            APIListDTO allApiResponse = restAPIPublisher.getAllAPIs();
            if (allApiResponse != null) {
                String json = gson.toJson(allApiResponse);
                allApiObject = new JSONObject(json);
                available = true;
                break;
            }
            currentTry++;
        } while (currentTry <= maxRetry);
        
        assertTrue(available, "API not available");
        JSONArray jsonArray = allApiObject.getJSONArray("list");
        List<String> allApiList = new ArrayList<String>();


        for (int i = 0; i < jsonArray.length(); i++) {
            String version = jsonArray.getJSONObject(i).getString("version");
            allApiList.add(version);
        }

        assertTrue(allApiList.contains(apiOldVersion), "Error in API getting:" + apiNameTest +
                " through publisher rest api");
        assertTrue(allApiList.contains(apiNewVersion), "Error in API getting:" + apiNameTest +
                " through publisher rest api");
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteAPI(copyAPIId);
        super.cleanUp();
    }

}
