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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
 * APIM2-570:Check if an older version of the API exists through the publisher REST API
 */

public class APIM570CheckIfAnOlderVersionOfTheAPIExistsThroughThePublisherRestAPITestCase
        extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.
            getLog(APIM570CheckIfAnOlderVersionOfTheAPIExistsThroughThePublisherRestAPITestCase.class);
    private static final String apiNameTest = "APIM570PublisherTest";
    private static final String apiVersion1 = "1.0.0";
    private static final String apiVersion2 = "2.0.0";
    private static final String apiVersion3 = "3.0.0";
    private static final String apiDefaultVersion = "default_version";
    private APIPublisherRestClient apiPublisher;
    private static String apiProviderName;
    private String apiProductionEndPointUrl;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM570CheckIfAnOlderVersionOfTheAPIExistsThroughThePublisherRestAPITestCase
            (TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
//                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }


    @DataProvider(name = "copyAPI")
    public static Object[][] copyanApiWithValidDataProvider() throws Exception {

        return new Object[][]{
                {apiProviderName, apiNameTest, apiVersion1, apiVersion2, apiDefaultVersion},
                {apiProviderName, apiNameTest, apiVersion1, apiVersion3, apiDefaultVersion},
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

    @Test(groups = {"wso2.am"}, description = "Create an API using valid data and get the API")
    public void testCreateAnApiUsingValidDataAndGetThroughThePublisherRest() throws Exception {

        String apiContextTest = "apim570PublisherTestAPI";
        String apiDescription = "This is Test API Created by API Manager Integration Test";
        String apiTag = "tag570-1, tag570-2, tag570-3";

        //Create an API
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiNameTest, apiContextTest, apiVersion1, apiProviderName,
                        new URL(apiProductionEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTiersCollection("Gold,Bronze");
        apiCreationRequestBean.setDefaultVersion("default_version");
        apiCreationRequestBean.setDefaultVersionChecked("default_version");
        apiCreationRequestBean.setBizOwner("api570b");
        apiCreationRequestBean.setBizOwnerMail("api570b@ee.com");
        apiCreationRequestBean.setTechOwner("api570t");
        apiCreationRequestBean.setTechOwnerMail("api570t@ww.com");

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
        assertTrue(jsonObject.getString("apis").contains(apiVersion1),
                "Version of the " + apiNameTest + "is not a valid version");

    }

    @Test(dataProvider = "copyAPI", description = "Copy an API with the multiple version check " +
            "if the older version exist through Publisher Rest API",
            dependsOnMethods = "testCreateAnApiUsingValidDataAndGetThroughThePublisherRest")
    public void testCheckIfAnOlderVersionOfTheAPIExistsThroughThePublisherRestAPI
            (String provider, String apiName, String oldVersion, String newVersion,
             String defaultVersion) throws Exception {

        //Create two new copies of the API and validate the result
        JSONObject jsonObjectCopy = new JSONObject(apiPublisher.copyAPI
                (provider, apiName, oldVersion, newVersion, defaultVersion).getData());
        log.info("API Name: " + apiName + " Old Version: " + oldVersion +
                " New Version: " + newVersion);
        assertFalse(jsonObjectCopy.getBoolean("error"), " New copy of the " + apiNameTest +
                " is not created as expected");


        //Check availability of the APIs with the copies
        HttpResponse allApiResponse = apiPublisher.getAllAPIs();
        JSONObject allApiObject = new JSONObject(allApiResponse.getData());
        JSONArray jsonArray = allApiObject.getJSONArray("apis");
        List<String> allApiList = new ArrayList<String>();

        for (int i = 0; i < jsonArray.length(); i++) {
            String name = jsonArray.getJSONObject(i).toString();
            allApiList.add(name);
            log.info("API List :" + allApiList);

        }

    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        apiPublisher.deleteAPI(apiNameTest, apiVersion1, apiProviderName);
        apiPublisher.deleteAPI(apiNameTest, apiVersion2, apiProviderName);
        apiPublisher.deleteAPI(apiNameTest, apiVersion3, apiProviderName);
    }

}
