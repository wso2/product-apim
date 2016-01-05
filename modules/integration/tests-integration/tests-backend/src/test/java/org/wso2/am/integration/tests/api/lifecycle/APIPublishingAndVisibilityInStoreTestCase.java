/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.api.lifecycle;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.xpath.XPathExpressionException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Publish a API and check its visibility in the API Store.
 */
public class APIPublishingAndVisibilityInStoreTestCase extends APIManagerLifecycleBaseTest {

    private final String API_NAME = "APIPublishingAndVisibilityInStoreTest";
    private final String API_CONTEXT = "APIPublishingAndVisibilityInStore";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "APIPublishingAndVisibilityInStoreTestCase";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private APIIdentifier apiIdentifier;
    private String providerName;
    private APICreationRequestBean apiCreationRequestBean;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, XPathExpressionException, MalformedURLException {
        super.init();
        apiEndPointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                        new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(user.getUserName(), user.getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(user.getUserName(), user.getPassword());
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiStoreClientUser1
                .addApplication(APPLICATION_NAME, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
    }


    @Test(groups = {"wso2.am"}, description = "Create a API and  check its availability in Publisher.")
    public void testAPICreation() throws APIManagerIntegrationTestException {
        //Create APi
        HttpResponse createAPIResponse = apiPublisherClientUser1.addAPI(apiCreationRequestBean);
        assertEquals(createAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Create API Response Code is invalid." + getAPIIdentifierString(apiIdentifier));
        assertEquals(getValueFromJSON(createAPIResponse, "error"), "false",
                "Error in API Creation in " + getAPIIdentifierString(apiIdentifier) +
                        "Response Data:" + createAPIResponse.getData());
        //Verify the API in API Publisher
        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientUser1.getApi(API_NAME, providerName, API_VERSION_1_0_0));
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiPublisherAPIIdentifierList),
                "Added Api is not available in APi Publisher. " + getAPIIdentifierString(apiIdentifier));
    }


    @Test(groups = {"wso2.am"}, description = "Check the visibility of API in Store before the API publish. " +
            "it should not be available in store.", dependsOnMethods = "testAPICreation")
    public void testVisibilityOfAPIInStoreBeforePublishing() throws APIManagerIntegrationTestException {
        //Verify the API in API Store : API should not be available in the store.
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientUser1.getAPI());
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList),
                "Api is visible in API Store before publish." + getAPIIdentifierString(apiIdentifier));
    }


    @Test(groups = {"wso2.am"}, description = "Test the API publishing action. " +
            "Response HTTP message should contains API status change from  CREATED to PUBLISHED",
            dependsOnMethods = "testVisibilityOfAPIInStoreBeforePublishing")
    public void testAPIPublishing() throws APIManagerIntegrationTestException, XPathExpressionException {
        //Publish the API
        APILifeCycleStateRequest publishUpdateRequest =
                new APILifeCycleStateRequest(API_NAME, providerName, APILifeCycleState.PUBLISHED);
        waitForAPIDeploymentSync(apiCreationRequestBean.getProvider(),apiCreationRequestBean.getName(),
                API_VERSION_1_0_0, APIMIntegrationConstants.IS_API_EXISTS);
        publishUpdateRequest.setVersion(API_VERSION_1_0_0);
        HttpResponse publishAPIResponse =
                apiPublisherClientUser1.changeAPILifeCycleStatusToPublish(apiIdentifier, false);
        assertEquals(publishAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API publish Response code is invalid " + getAPIIdentifierString(apiIdentifier));
        assertTrue(verifyAPIStatusChange(publishAPIResponse, APILifeCycleState.CREATED, APILifeCycleState.PUBLISHED),
                "API status Change is invalid in" + getAPIIdentifierString(apiIdentifier) +
                        "Response Data:" + publishAPIResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in the store after API publish.",
            dependsOnMethods = "testAPIPublishing")
    public void testVisibilityOfAPIInStoreAfterPublishing() throws APIManagerIntegrationTestException {
        //Verify the API in API Store : API should not be available in the store.
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientUser1.getAPI());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList),
                "Api is not visible in API Store after publish. " + getAPIIdentifierString(apiIdentifier));

    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
        apiStoreClientUser1.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);
    }


}
