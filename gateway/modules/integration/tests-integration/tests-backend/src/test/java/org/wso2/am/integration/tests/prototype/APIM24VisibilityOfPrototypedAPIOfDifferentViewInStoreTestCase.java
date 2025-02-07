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
//TODO change as prototype
package org.wso2.am.integration.tests.prototype;

import com.google.gson.Gson;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.TagListDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Create an API as a Prototyped API and check the visibility in store from different views
 * APIM-24 ,APIM-25, APIM-26, APIM-27
 */
public class APIM24VisibilityOfPrototypedAPIOfDifferentViewInStoreTestCase extends
        APIMIntegrationBaseTest {

    private final String apiName = "APIM24PrototypedAPI";
    private final String apiVersion = "1.0.0";
    private final String apiTags = "pizza, order, pizza-menu";
    private final String superUser = "carbon.super";
    private String apiEndPointUrl;
    private String apiId;
    private APIIdentifier apiIdentifier;
    private APIListDTO apis;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        apiEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + "pizzashack-api-1.0.0/api/";
    }

    @Test(groups = {"wso2.am"}, description = "Open already Saved API in design stage and Deploy" +
            " it as a prototype and check the visibility of prototyped API In store")
    public void testOpenAlreadySavedAPIAndDeployedAsAPrototyped() throws Exception {

        String apiContext = "apim24prototypedApi";
        String apiDescription = "Pizza API:Allows to manage pizza orders" +
                " (create, update, retrieve orders)";

        String apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();
        apiIdentifier = new APIIdentifier(apiProvider, apiName, apiVersion);

        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(apiEndPointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setProvider(apiProvider);
        apiRequest.setTags(apiTags);

        //Adding the API to the publisher
        apiId = restAPIPublisher.addAPI(apiRequest).getData();

        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.DEPLOY_AS_PROTOTYPE.getAction());

        HttpResponse response = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);
        String endPointString = "{\n" +
                "  \"production_endpoints\": {\n" +
                "    \"template_not_supported\": false,\n" +
                "    \"config\": null,\n" +
                "    \"url\": \"" + apiEndPointUrl + "\"\n" +
                "  },\n" +
                "  \"sandbox_endpoints\": {\n" +
                "    \"url\": \"" + apiEndPointUrl + "\",\n" +
                "    \"config\": null,\n" +
                "    \"template_not_supported\": false\n" +
                "  },\n" +
                "  \"endpoint_type\": \"http\",\n" +
                "  \"implementation_status\": \"prototyped\"\n" +
                "}";

        JSONParser parser = new JSONParser();
        JSONObject endpoint = (JSONObject) parser.parse(endPointString);
        apidto.setEndpointConfig(endpoint);

        restAPIPublisher.updateAPI(apidto);

        HttpResponse response2 = restAPIPublisher.getAPI(apiId);
        Gson g2 = new Gson();
        APIDTO apidto2 = g2.fromJson(response2.getData(), APIDTO.class);

        JSONObject endPointConfigUpdated = (JSONObject) apidto.getEndpointConfig();
        String implementation_status = (String) endPointConfigUpdated.get("implementation_status");
        String lcStatus = apidto2.getLifeCycleStatus();

        assertEquals(implementation_status, "prototyped", "Endpoint implementation is not prototyped");
        assertEquals(lcStatus, "PROTOTYPED", "Lifecycle status is not PROTOTYPED");
        Thread.sleep(20000);
        apis = restAPIStore.getPrototypedAPIs(superUser);
        assertTrue((apis.getList().size() > 0), apiName + " is not visible as Prototyped API");
    }

    @Test(groups = {"wso2.am"}, description = "API deployed as a prototype and check the" +
            " visibility in general store",
            dependsOnMethods = "testOpenAlreadySavedAPIAndDeployedAsAPrototyped")
    public void testPrototypedAPIVisibilityInGeneralAPI() {
        assertTrue(APIMTestCaseUtils.isAPIAvailableInStore(apiIdentifier, apis),
                apiName + "is not in general Store.");
    }


    @Test(groups = {"wso2.am"}, description = "API deployed as a prototype and check the " +
            "tags of API visibility in Tag list in store",
            dependsOnMethods = "testPrototypedAPIVisibilityInGeneralAPI")
    public void testTagsOfPrototypedAPIVisibilityInTagList() throws org.wso2.am.integration.clients.store.api.ApiException {

        TagListDTO allTags = restAPIStore.getAllTags();
        assertFalse(allTags.getList().contains(apiTags),
                "Tags of" + apiName + " PizzaAPI are visible in Tag List.");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException, JSONException, ApiException {
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.DEMOTE_TO_CREATE.getAction());
        restAPIPublisher.deleteAPI(apiId);
    }

}