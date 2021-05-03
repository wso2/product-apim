/*
 *
 *   Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.prototype;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;

import com.google.gson.Gson;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WorkflowResponseDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

public class PrototypedAPITestcase extends APIMIntegrationBaseTest {

    private final String apiVersion = "1.0.0";
    private String apiProvider;
    private String apiName;
    private String apiEndPointUrl;
    private String apiID;
    private APIIdentifier apiIdentifier;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws APIManagerIntegrationTestException, XPathExpressionException {

        super.init();

        String apiPrototypeEndpointPostfixUrl = "am/sample/pizzashack/v1/api/menu";
        apiEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiPrototypeEndpointPostfixUrl;

        restAPIPublisher = new RestAPIPublisherImpl(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword(),
                publisherContext.getContextTenant().getDomain(), publisherURLHttps);

        restAPIStore = new RestAPIStoreImpl(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword(),
                storeContext.getContextTenant().getDomain(), storeURLHttps);

        apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();
    }

    @Test(groups = {"wso2.am"}, description = "Create an API with a prototype endpoint and invoke")
    public void testPrototypedAPIEndpoint() throws Exception {

        apiName = "APIMPrototypedEndpointAPI1";
        String apiContext = "pizzashack-prototype";
        String apiTags = "pizza, order, pizza-menu";
        String apiDescription = "Pizza API:Allows to manage pizza orders " +
                "(create, update, retrieve orders)";

        apiIdentifier = new APIIdentifier(apiProvider, apiName, apiVersion);
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(apiEndPointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setDescription(apiDescription);
        apiRequest.setTags(apiTags);
        apiRequest.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());

        HttpResponse addAPIResponse = restAPIPublisher.addAPI(apiRequest);
        assertEquals(addAPIResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Invalid Response Code");
        apiID = addAPIResponse.getData();

        //Deployed API as a Prototyped API & check the status
        WorkflowResponseDTO lcChangeResponse = restAPIPublisher.changeAPILifeCycleStatus(apiID,
                APILifeCycleAction.DEPLOY_AS_PROTOTYPE.getAction());

        HttpResponse response = restAPIPublisher.getAPI(apiID);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);
        String endPointString = "{\"implementation_status\":\"prototyped\",\"endpoint_type\":\"http\"," +
                "\"production_endpoints\":{\"config\":null," +
                "\"url\":\"" + apiEndPointUrl + "\"}," +
                "\"sandbox_endpoints\":{\"config\":null,\"url\":\"" + apiEndPointUrl + "\"}}";

        JSONParser parser = new JSONParser();
        JSONObject endpoint = (JSONObject) parser.parse(endPointString);
        apidto.setEndpointConfig(endpoint);

        restAPIPublisher.updateAPI(apidto);

        assertTrue(lcChangeResponse.getLifecycleState().getState().equals("Prototyped"),
                apiName + "  status not updated as Prototyped");
        Thread.sleep(15000);

        // Create a revision and Deploy the API
        createAPIRevisionAndDeployUsingRest(apiID, restAPIPublisher);

        //Check whether Prototype API is available in publisher
        APIListDTO getAllAPIsResponse = restAPIPublisher.getAllAPIs();
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, getAllAPIsResponse),
                "Implemented" + apiName + " Api is not visible in API Publisher.");
        Thread.sleep(15000);

        //Check whether Prototype API is available under the Prototyped API
        org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO prototypedAPIs = restAPIStore
                .getPrototypedAPIs(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        assertTrue(APIMTestCaseUtils.isAPIAvailableInStore(apiIdentifier, prototypedAPIs),
                apiName + " is not visible as Prototyped API");

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "application/json");

        //Invoke the Prototype endpoint and validate
        HttpResponse response1 = HTTPSClientUtils
                .doGet(getAPIInvocationURLHttps(apiContext, apiVersion) +
                        "", requestHeaders);
        Assert.assertEquals(response1.getResponseCode(), 200);
        Assert.assertTrue(response1.getData().contains("BBQ Chicken Bacon"));
    }

    @Test(groups = {"wso2.am"}, description = "Create an API with a prototype endpoint, demote to created and invoke")
    public void testDemotedPrototypedEndpointAPItoCreated() throws Exception {

        apiName = "APIMPrototypedEndpointAPI2";
        String apiContext = "pizzashack-prototype2";
        String apiTags = "pizza, order, pizza-menu";
        String apiDescription = "Pizza API:Allows to manage pizza orders " +
                "(create, update, retrieve orders)";

        apiIdentifier = new APIIdentifier(apiProvider, apiName, apiVersion);
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(apiEndPointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setDescription(apiDescription);
        apiRequest.setTags(apiTags);
        apiRequest.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());

        HttpResponse addAPIResponse = restAPIPublisher.addAPI(apiRequest);
        assertEquals(addAPIResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Invalid Response Code");
        apiID = addAPIResponse.getData();

        //Deployed API as a Prototyped API & check the status
        WorkflowResponseDTO lcChangeResponse = restAPIPublisher.changeAPILifeCycleStatus(apiID,
                APILifeCycleAction.DEPLOY_AS_PROTOTYPE.getAction());

        HttpResponse response = restAPIPublisher.getAPI(apiID);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);
        String endPointString = "{\"implementation_status\":\"prototyped\",\"endpoint_type\":\"http\"," +
                "\"production_endpoints\":{\"config\":null," +
                "\"url\":\"" + apiEndPointUrl + "\"}," +
                "\"sandbox_endpoints\":{\"config\":null,\"url\":\"" + apiEndPointUrl + "\"}}";

        JSONParser parser = new JSONParser();
        JSONObject endpoint = (JSONObject) parser.parse(endPointString);
        apidto.setEndpointConfig(endpoint);

        //Update the API with Prototype endpoint
        restAPIPublisher.updateAPI(apidto);

        assertTrue(lcChangeResponse.getLifecycleState().getState().equals("Prototyped"),
                apiName + "  status not updated as Prototyped");
        Thread.sleep(15000);

        // Create a revision and Deploy the API
        createAPIRevisionAndDeployUsingRest(apiID, restAPIPublisher);

        //Check whether Prototype API is available in publisher
        APIListDTO getAllAPIsResponse = restAPIPublisher.getAllAPIs();
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, getAllAPIsResponse),
                "Implemented" + apiName + " Api is not visible in API Publisher.");
        Thread.sleep(15000);

        //Check whether Prototype API is available under the Prototyped API
        org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO prototypedAPIs = restAPIStore
                .getPrototypedAPIs(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        assertTrue(APIMTestCaseUtils.isAPIAvailableInStore(apiIdentifier, prototypedAPIs),
                apiName + " is not visible as Prototyped API");

        //Change the status PROTOTYPED to CREATED
        restAPIPublisher.changeAPILifeCycleStatus(apiID, APILifeCycleAction.DEMOTE_TO_CREATE.getAction());
        assertTrue(APILifeCycleState.CREATED.getState().equals(restAPIPublisher.getLifecycleStatus(apiID).getData()),
                apiName + "status not updated as CREATED");
        //Wait for the changes to be applied after demoting to Created.
        Thread.sleep(15000);

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "application/json");

        //Invoke the Prototype endpoint
        HttpResponse response2 = HTTPSClientUtils
                .doGet(getAPIInvocationURLHttps(apiContext, apiVersion) +
                        "", requestHeaders);

        Assert.assertEquals(response2.getResponseCode(), 401, "User was able to invoke the API demoted to CREATED from PROTOTYPE");
    }

}
