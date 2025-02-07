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

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.ws.rs.core.Response;
import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Change the status of an API to PROTOTYPED, PUBLISHED, DEPRECATED and RETIRED through the publisher REST api
 * APIM2-574 /APIM2-587
 */

public class APIM574ChangeTheStatusOfAnAPIToPrototypedThroughThePublisherRestAPITestCase
        extends APIMIntegrationBaseTest {

    private final String apiNameTest = "APIM574PublisherTest";
    private final String apiVersion = "1.0.0";
    private String apiProvider;
    private String apiEndPointUrl;
    private String apiId;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM574ChangeTheStatusOfAnAPIToPrototypedThroughThePublisherRestAPITestCase
            (TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        String gatewayUrl;
        if (gatewayContextWrk.getContextTenant().getDomain().equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp();
        } else {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "t/" +
                    gatewayContextWrk.getContextTenant().getDomain() + "/";
        }
        apiEndPointUrl = gatewayUrl + "jaxrs_basic/services/customers/customerservice";
        apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();

    }

    @Test(groups = {"wso2.am"}, description = "Create an API throgh the publisher rest API ")
    public void testCreateAnAPIThroughThePublisherRest() throws Exception {

        String apiContext = "apim574PublisherTestAPI";
        String apiDescription = "This is Test API Created by API Manager Integration Test";
        String apiTags = "tag574-1, tag574-2, tag587-3";
        //Create an API
        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiNameTest, apiContext, apiVersion,
                apiProvider, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(apiTags);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTiersCollection("Gold,Bronze");
        apiCreationRequestBean.setDefaultVersion("default_version");
        apiCreationRequestBean.setDefaultVersionChecked("default_version");
        apiCreationRequestBean.setBizOwner("api574b");
        apiCreationRequestBean.setBizOwnerMail("api574b@ee.com");
        apiCreationRequestBean.setTechOwner("api574t");
        apiCreationRequestBean.setTechOwnerMail("api574t@ww.com");
        APIDTO apiCreationResponse = restAPIPublisher.addAPI(apiCreationRequestBean);
        String status = apiCreationResponse.getLifeCycleStatus();
        apiId = apiCreationResponse.getId();
        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = restAPIPublisher.getAPI(apiId);
        assertEquals(apiResponsePublisher.getResponseCode(), Response.Status.OK.getStatusCode(), apiNameTest +
                " is not visible in publisher");
        assertTrue(apiNameTest.equals(apiCreationResponse.getName()), apiNameTest + " is not visible in publisher");
        assertTrue(APILifeCycleState.CREATED.getState().equalsIgnoreCase(status), "Status of the " + apiNameTest +
                "is not a valid status");
    }

    @Test(groups = {"wso2.am"}, description = "Change the status of the API to PROTOTYPED through" +
            " the publisher rest API ", dependsOnMethods = "testCreateAnAPIThroughThePublisherRest")
    public void testChangeTheStatusOfTheAPIToPrototyped() throws Exception {

        //Change the status of the API from CREATED to PROTOTYPED
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.DEPLOY_AS_PROTOTYPE.getAction());
        assertTrue(APILifeCycleState.PROTOTYPED.getState().equals(restAPIPublisher.getLifecycleStatus(apiId).getData()),
                apiNameTest + "  status not updated as Prototyped");
        //Check whether Prototype API is available in publisher
        HttpResponse prototypedApiResponse = restAPIPublisher.getAPI(apiId);
        assertEquals(prototypedApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(), apiNameTest +
                " is not visible in publisher");
        assertTrue(prototypedApiResponse.getData().contains(apiNameTest), apiNameTest + " is not visible in " +
                "publisher");
    }

    @Test(groups = {"wso2.am"}, description = "Change the status of the API to CREATED through" +
            " the publisher rest API ", dependsOnMethods = "testChangeTheStatusOfTheAPIToPrototyped")
    public void testChangeTheStatusOfTheAPIToCreated() throws Exception {
        //Change the status PROTOTYPED to CREATED
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.DEMOTE_TO_CREATE.getAction());
        assertTrue(APILifeCycleState.CREATED.getState().equals(restAPIPublisher.getLifecycleStatus(apiId).getData()),
                apiNameTest + "status not updated as Published");
        //Check whether published API is available in publisher
        HttpResponse publishedApiResponse = restAPIPublisher.getAPI(apiId);
        assertEquals(publishedApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(), apiNameTest +
                " is not visible in publisher");
        assertTrue(publishedApiResponse.getData().contains(apiNameTest), apiNameTest + " is not visible in " +
                "publisher");
    }

    @Test(groups = {"wso2.am"}, description = "Change the status of the API to PUBLISHED through" +
            " the publisher rest API ", dependsOnMethods = "testChangeTheStatusOfTheAPIToCreated")
    public void testChangeTheStatusOfTheAPIToPublished() throws Exception {

        //Change the status CREATED to PUBLISHED
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction());
        assertTrue(APILifeCycleState.PUBLISHED.getState().equals(restAPIPublisher.getLifecycleStatus(apiId).getData()),
                apiNameTest + "status not updated as Published");
        //Check whether published API is available in publisher
        HttpResponse publishedApiResponse = restAPIPublisher.getAPI(apiId);
        assertEquals(publishedApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(), apiNameTest +
                " is not visible in publisher");
        assertTrue(publishedApiResponse.getData().contains(apiNameTest), apiNameTest + " is not visible in " +
                "publisher");
    }

    @Test(groups = {"wso2.am"}, description = "Change the status of the API to DEPRECATED through" +
            " the publisher rest API ", dependsOnMethods = "testChangeTheStatusOfTheAPIToPublished")
    public void testChangeTheStatusOfTheAPIToDeprecated() throws Exception {

        //Change the status PUBLISHED to DEPRECATED
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.DEPRECATE.getAction());
        assertTrue(APILifeCycleState.DEPRECATED.getState().equals(restAPIPublisher.getLifecycleStatus(apiId).getData()),
                apiNameTest + "  status not updated as Deprecate");
        //Check whether published API is available in publisher
        HttpResponse deprecatedApiResponse = restAPIPublisher.getAPI(apiId);
        assertEquals(deprecatedApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(), apiNameTest +
                " is not visible in publisher");
        assertTrue(deprecatedApiResponse.getData().contains(apiNameTest), apiNameTest + " is not visible in " +
                "publisher");
    }

    @Test(groups = {"wso2.am"}, description = "Change the status of the API to RETIRED through" +
            " the publisher rest API ", dependsOnMethods = "testChangeTheStatusOfTheAPIToDeprecated")
    public void testChangeTheStatusOfTheAPIToRetired() throws Exception {

        //Change the status DEPRECATED to RETIRED
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.RETIRE.getAction());
        assertTrue(APILifeCycleState.RETIRED.getState().equals(restAPIPublisher.getLifecycleStatus(apiId).getData()),
                apiNameTest + "status not updated as Retired");
        //Check whether published API is available in publisher
        HttpResponse retiredApiResponse = restAPIPublisher.getAPI(apiId);
        assertEquals(retiredApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(), apiNameTest +
                " is not visible in publisher");
        assertTrue(retiredApiResponse.getData().contains(apiNameTest), apiNameTest + " is not visible in " +
                "publisher");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
    }
}
