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

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

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
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

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
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiNameTest, apiContextTest,
                        apiOldVersion, apiProviderName,
                        new URL(apiProductionEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTiersCollection("Gold,Bronze");
        apiCreationRequestBean.setDefaultVersion("default_version");
        apiCreationRequestBean.setDefaultVersionChecked("default_version");
        apiCreationRequestBean.setBizOwner("api548b");
        apiCreationRequestBean.setBizOwnerMail("api548b@ee.com");
        apiCreationRequestBean.setTechOwner("api548t");
        apiCreationRequestBean.setTechOwnerMail("api548t@ww.com");

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        JSONObject apiResponse = new JSONObject(apiCreationResponse.getData());
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        assertFalse(apiResponse.getBoolean("error"), apiNameTest + "is not created as expected");

        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = apiPublisher.getAllAPIs();
        JSONObject jsonObject = new JSONObject(apiResponsePublisher.getData());
        assertFalse(jsonObject.getBoolean("error"), apiNameTest + " is not visible in publisher");
        assertTrue(jsonObject.getString("apis").contains(apiNameTest),
                apiNameTest + " is not visible in publisher");
        assertTrue(jsonObject.getString("apis").contains(apiOldVersion),
                "Version of the " + apiNameTest + "is not a valid version");

        //Create a new copy of the API and validate the result
        JSONObject jsonObjectCopy = new JSONObject(apiPublisher.copyAPI
                (apiProviderName, apiNameTest, apiOldVersion, apiNewVersion, defaultVersion).getData());
        assertFalse(jsonObjectCopy.getBoolean("error"), " New copy of the " + apiNameTest +
                " is not created as expected");

        //Check if the New Version of the API is available in Publisher
        HttpResponse allApiResponse = apiPublisher.getAllAPIs();
        JSONObject allApiObject = new JSONObject(allApiResponse.getData());
        JSONArray jsonArray = allApiObject.getJSONArray("apis");
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
        apiPublisher.deleteAPI(apiNameTest, apiOldVersion, apiProviderName);
        apiPublisher.deleteAPI(apiNameTest, apiNewVersion, apiProviderName);
    }

}
