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

import org.testng.annotations.AfterClass;
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
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.net.URL;
import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Create an API as a Prototyped API and check the visibility in store from different views
 */
public class APIM23VisibilityOfPrototypedAPIInStoreTestCase extends APIMIntegrationBaseTest {

    private final String apiName = "APIM23PrototypedAPI";
    private final String apiVersion = "1.0.0";
    private RestAPIPublisherImpl restAPIPublisher;
    private String apiProvider;
    private String apiEndPointUrl;
    private String apiID;
    private APIIdentifier apiIdentifier;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws APIManagerIntegrationTestException,
            XPathExpressionException {
        super.init();

        String apiPrototypeEndpointPostfixUrl = "pizzashack-api-1.0.0/api/";
        apiEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiPrototypeEndpointPostfixUrl;

        restAPIPublisher = new RestAPIPublisherImpl(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword(),
                publisherContext.getContextTenant().getDomain(),publisherURLHttps);

        restAPIStore = new RestAPIStoreImpl(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword(),
                storeContext.getContextTenant().getDomain(), storeURLHttps);

        apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();

        apiIdentifier = new APIIdentifier(apiProvider, apiName, apiVersion);
    }

    @Test(groups = {"wso2.am"}, description = "Create an API & deployed as a prototype and check " +
            "the visibility in prototype API In store")
    public void testVisibilityInPrototypedAPI() throws Exception {

        String apiContext = "apim23pizzashack";
        String apiTags = "pizza, order, pizza-menu";
        String apiDescription = "Pizza API:Allows to manage pizza orders " +
                "(create, update, retrieve orders)";

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

        assertTrue(lcChangeResponse.getLifecycleState().getState().equals("Prototyped"),
                apiName + "  status not updated as Prototyped");
        Thread.sleep(15000);
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
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        restAPIPublisher.deleteAPI(apiID);
        super.cleanUp();
    }
}
