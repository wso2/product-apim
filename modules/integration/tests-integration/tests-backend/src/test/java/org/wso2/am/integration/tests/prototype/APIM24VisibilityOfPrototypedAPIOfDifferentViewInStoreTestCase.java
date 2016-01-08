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
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;

import java.net.URL;
import java.util.List;


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
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private APIIdentifier apiIdentifierStore;
    private APIIdentifier apiIdentifierPublisher;
    private String apiProvider;
    private String apiEndPointUrl;


    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        apiEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + "pizzashack-api-1.0.0/api/";

        apiStore = new APIStoreRestClient(storeUrls.getWebAppURLHttp());
        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());


        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();

        apiIdentifierPublisher = new APIIdentifier(apiProvider, apiName, apiVersion);
        apiIdentifierStore = new APIIdentifier(apiProvider, apiName, apiVersion);
    }

    @Test(groups = {"wso2.am"}, description = "Open already Saved API in design stage and Deploy" +
            " it as a prototype and check the visibility of prototyped API In store")
    public void testOpenAlreadySavedAPIAndDeployedAsAPrototyped() throws Exception {

        String apiContext = "apim24prototypedApi";
        String apiDescription = "Pizza API:Allows to manage pizza orders" +
                " (create, update, retrieve orders)";

        APIDesignBean apiDesignBean = new APIDesignBean(apiName, apiContext, apiVersion,
                apiDescription, apiTags);

        HttpResponse apiDesignResponse = apiPublisher.designAPI(apiDesignBean);
        assertEquals(apiDesignResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                apiName + "is not Designed as expected");
        assertTrue(apiDesignResponse.getData().contains("\"error\" : false"),
                apiName + "is not created as expected");

        APIImplementationBean apiImplementationBean =
                new APIImplementationBean(apiName, apiVersion, apiProvider, new URL(apiEndPointUrl));
        apiImplementationBean.setSwagger(apiDesignBean.getSwagger());

        HttpResponse apiImplementationResponse = apiPublisher.implement(apiImplementationBean);
        assertEquals(apiImplementationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                apiName + "is not Implemented as expected");
        assertTrue(apiImplementationResponse.getData().contains("\"error\" : false"),
                apiName + "is not created as expected");

        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, apiProvider,
                APILifeCycleState.PROTOTYPED);
        HttpResponse creationResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        assertTrue(creationResponse.getData().contains("PROTOTYPED"),
                apiName + " status not updated as Prototyped");

        //Check whether Prototype API is available in general store
        List<APIIdentifier> implementedAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiPublisher.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierPublisher, implementedAPIList),
                apiName + " is not visible in API Publisher.");

        Thread.sleep(20000);

        //Check whether Prototype API is available under the Prototyped API
        HttpResponse apiResponse = apiStore.getPrototypedAPI(superUser);
        assertEquals(apiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Invalid Response Code");
        assertTrue(apiResponse.getData().contains(apiName),
                apiName + " is not visible as Prototyped API");
    }

    @Test(groups = {"wso2.am"}, description = "API deployed as a prototype and check the" +
            " visibility in general store",
            dependsOnMethods = "testOpenAlreadySavedAPIAndDeployedAsAPrototyped")
    public void testPrototypedAPIVisibilityInGeneralAPI() throws
            APIManagerIntegrationTestException {

        List<APIIdentifier> publishedAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiStore.getAllPublishedAPIs());
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierStore, publishedAPIList),
                apiName + "is not in general Store.");
    }

    @Test(groups = {"wso2.am"}, description = "API deployed as a prototype and check the " +
            "visibility in Recently Added list in store",
            dependsOnMethods = "testOpenAlreadySavedAPIAndDeployedAsAPrototyped")
    public void testPrototypedAPIVisibilityInRecentlyAddedList() throws
            APIManagerIntegrationTestException {

        String apiLimit = "5";

        List<APIIdentifier> recentlyAddedAPIIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse
                        (apiStore.getRecentlyAddedAPIs(superUser, apiLimit));
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierStore, recentlyAddedAPIIList),
                apiName + "is visible in Recently added API List.");
    }

    @Test(groups = {"wso2.am"}, description = "API deployed as a prototype and check the " +
            "tags of API visibility in Tag list in store",
            dependsOnMethods = "testOpenAlreadySavedAPIAndDeployedAsAPrototyped")
    public void testTagsOfPrototypedAPIVisibilityInTagList() throws
            APIManagerIntegrationTestException {

        HttpResponse tagResponse = apiStore.getAllTags();
        assertFalse(tagResponse.getData().contains(apiTags),
                "Tags of" + apiName + " PizzaAPI are visible in Tag List.");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException, JSONException {
        apiPublisher.deleteAPI(apiName, apiVersion, apiProvider);

    }

}