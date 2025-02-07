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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
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
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Get all the APIs created through the publisher REST API
 * APIM-534 / APIM-542
 */

public class APIM534GetAllTheAPIsCreatedThroughThePublisherRestAPITestCase extends
        APIMIntegrationBaseTest {

    private static final Log log = LogFactory.
            getLog(APIM534GetAllTheAPIsCreatedThroughThePublisherRestAPITestCase.class);
    private static final String apiNameTest1 = "APIM534PublisherTest1";
    private static final String apiNameTest2 = "APIM534PublisherTest2";
    private static final String apiNameTest3 = "APIM534PublisherTest3";
    private static final String apiVersion = "1.0.0";
    private static String apiProviderName;
    private static String apiTest1EndPointUrl;
    private static String apiTest2EndPointUrl;
    private static String apiTest3EndPointUrl;
    private static String id;
    List<String> idList = new ArrayList<String>();

    @Factory(dataProvider = "userModeDataProvider")
    public APIM534GetAllTheAPIsCreatedThroughThePublisherRestAPITestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                //                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @DataProvider(name = "createAPI")
    public static Object[][] createAppWithValidDataProvider() throws Exception {

        return new Object[][]{
                {apiNameTest1, "apim534PublisherTest1API", apiVersion, apiProviderName,
                        new URL(apiTest1EndPointUrl)},
                {apiNameTest2, "apim534PublisherTest2API", apiVersion, apiProviderName,
                        new URL(apiTest2EndPointUrl)},
                {apiNameTest3, "apim534PublisherTest3API", apiVersion, apiProviderName,
                        new URL(apiTest3EndPointUrl)}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        String apiTest1EndpointPostfixUrl = "jaxrs_basic/services/customers/" +
                "customerservice/customers/123";
        String apiTest2EndpointPostfixUrl = "name-check1/";
        String apiTest3EndpointPostfixUrl = "pizzashack-api-1.0.0/api/";

        String publisherURLHttp = publisherUrls.getWebAppURLHttp();

        apiTest1EndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiTest1EndpointPostfixUrl;
        apiTest2EndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiTest2EndpointPostfixUrl;
        apiTest3EndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiTest3EndpointPostfixUrl;

        apiProviderName = publisherContext.getContextTenant().getContextUser().getUserName();
    }

    @Test(dataProvider = "createAPI", description = "Create an API using valid data and get all " +
            "the API through Publisher Rest API")
    public void testGetAllTheAPICreatedThroughThePublisherRestAPI
            (String apiName, String context, String version, String provider,
             URL endpointUrl) throws Exception {


        APIRequest apiRequest = new APIRequest(apiName, context, endpointUrl);
        apiRequest.setVersion(version);
        HttpResponse apiCreationResponse = restAPIPublisher.addAPI(apiRequest);//(apiCreationRequestBean);
        id = apiCreationResponse.getData(); 
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Response Code miss matched when creating the API");
        assertNotNull(id, "APIs are not created as expected");
        idList.add(id);
        
        HttpResponse resp = restAPIPublisher.getAPI(id);
        assertEquals(resp.getResponseCode(), Response.Status.OK.getStatusCode(), "API does not exist");
        //Create a API list with newly added APIs
        List<String> apiList = Arrays.asList(apiNameTest1, apiNameTest2, apiNameTest3);
        log.info("My API List :" + apiList);

        //Check the availability of API in Publisher
        Thread.sleep(5000);
        APIListDTO response = restAPIPublisher.getAllAPIs();
        Gson gson = new Gson();
        String json = gson.toJson(response);
        JSONObject jsonObject = new JSONObject(json);
        JSONArray jsonArray = jsonObject.getJSONArray("list");
        List<String> allApiList = new ArrayList<String>();


        for (int i = 0; i < jsonArray.length(); i++) {
            String name = jsonArray.getJSONObject(i).getString("name");
            allApiList.add(name);
        }
        log.info("All API List :" + allApiList);

    }

    @Test(groups = {"wso2.am"}, description = "Validate if an API exists through the publisher " +
            "REST API", dependsOnMethods = "testGetAllTheAPICreatedThroughThePublisherRestAPI")
    public void testCheckIfAnAPIExistsThroughThePublisherRestAPI() throws Exception {

        //Trying to Create an API with an existing API Name
        APIRequest apiRequest = new APIRequest(apiNameTest1, "apim534PublisherTest1API", new URL(apiTest1EndPointUrl));
        apiRequest.setVersion(apiVersion);
        HttpResponse response = restAPIPublisher.addAPI(apiRequest);
        //response is null if addAPI fails with same api name
        assertNull("Added same api again", response);

    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        for (String id : idList) {
            restAPIPublisher.deleteAPI(id);
        }
        super.cleanUp();
    }


}