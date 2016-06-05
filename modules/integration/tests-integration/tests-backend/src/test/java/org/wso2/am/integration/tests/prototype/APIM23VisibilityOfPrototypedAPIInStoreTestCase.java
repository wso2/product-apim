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

import org.json.JSONException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIDesignBean;
import org.wso2.am.integration.test.utils.bean.APIImplementationBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;
import java.net.URL;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Create an API as a Prototyped API and check the visibility in store from different views
 */
public class APIM23VisibilityOfPrototypedAPIInStoreTestCase extends APIMIntegrationBaseTest {

    private final String apiName = "APIM23PrototypedAPI";
    private final String apiVersion = "1.0.0";
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private APIIdentifier apiIdentifierPublisher;
    private String apiProvider;
    private String apiEndPointUrl;


    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws APIManagerIntegrationTestException,
            XPathExpressionException {
        super.init();

        String apiPrototypeEndpointPostfixUrl = "pizzashack-api-1.0.0/api/";
        apiEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiPrototypeEndpointPostfixUrl;

        apiStore = new APIStoreRestClient(storeUrls.getWebAppURLHttp());
        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());


        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();

        apiIdentifierPublisher = new APIIdentifier(apiProvider, apiName, apiVersion);

    }

    @Test(groups = {"wso2.am"}, description = "Create an API & deployed as a prototype and check " +
            "the visibility in prototype API In store")
    public void testVisibilityInPrototypedAPI() throws Exception {

        String apiContext = "apim23pizzashack";
        String apiTags = "pizza, order, pizza-menu";
        String apiDescription = "Pizza API:Allows to manage pizza orders " +
                "(create, update, retrieve orders)";
        String superUser = "carbon.super";

        APIDesignBean apiDesignBean = new APIDesignBean(apiName, apiContext, apiVersion,
                apiDescription, apiTags);

        HttpResponse apiDesignResponse = apiPublisher.designAPI(apiDesignBean);
        assertEquals(apiDesignResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Invalid Response Code");
        assertTrue(apiDesignResponse.getData().contains("\"error\" : false"),
                apiName + "is not created as expected");

        APIImplementationBean apiImplementationBean = new APIImplementationBean(apiName,
                apiVersion, apiProvider, new URL(apiEndPointUrl));
        apiImplementationBean.setSwagger(apiDesignBean.getSwagger());

        HttpResponse apiImplementationResponse = apiPublisher.implement(apiImplementationBean);
        assertEquals(apiImplementationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Invalid Response Code");
        assertTrue(apiImplementationResponse.getData().contains("\"error\" : false"),
                apiName + "is not created as expected");

        //Deployed API as a Prototyped API & check the status
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, apiProvider,
                APILifeCycleState.PROTOTYPED);
        HttpResponse creationResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        assertTrue(creationResponse.getData().contains("PROTOTYPED"),
                apiName + "  status not updated as Prototyped");

        //Check whether Prototype API is available in publisher
        List<APIIdentifier> implementedAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiPublisher.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierPublisher, implementedAPIList),
                "Implemented" + apiName + " Api is visible in API Publisher.");

        Thread.sleep(15000);

        //Check whether Prototype API is available under the Prototyped API
        HttpResponse apiResponse = apiStore.getPrototypedAPI(superUser);
        assertEquals(apiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Invalid Response Code");
        assertTrue(apiResponse.getData().contains(apiName),
                apiName + " is not visible as Prototyped API");
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        apiPublisher.deleteAPI(apiName, apiVersion, apiProvider);
        super.cleanUp();
    }

}