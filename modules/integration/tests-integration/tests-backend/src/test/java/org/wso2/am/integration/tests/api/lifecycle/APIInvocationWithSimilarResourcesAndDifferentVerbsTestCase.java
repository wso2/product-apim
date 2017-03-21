/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.bean.ApplicationKeyBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Change the Auth type of the Resource and invoke the APi
 */
public class APIInvocationWithSimilarResourcesAndDifferentVerbsTestCase extends APIManagerLifecycleBaseTest {

    private static final String API_NAME = "MultiVerbSimilarResourceAPI";
    private static final String API_CONTEXT = "multiVerbSimilarResourceAPI";
    private static final String API_END_POINT_POSTFIX_URL = "multiVSR/";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String RESPONSE_GET = "<response><value>Received GET /comp/cartes*</value></response>";
    private static final String RESPONSE_POST = "<response><value>Received POST /comp/cartes/op*</value></response>";
    private static final String API_ENDPOINT_RESOURCE = "/comp/cartes/op/123";
    private String APPLICATION_NAME = "MultiVerbSimilarResourceApp";
    private APIPublisherRestClient apiPublisherClientUser1;
    private String apiEndPointUrl;
    private APIStoreRestClient apiStoreClientUser1;
    private String providerName;
    private APIIdentifier apiIdentifier;
    private HashMap<String, String> requestHeaders;


    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        apiEndPointUrl = getGatewayURLNhttp() + API_END_POINT_POSTFIX_URL;
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
        requestHeaders = new HashMap<String, String>();

        //Load the back-end dummy API
        String gatewaySessionCookie = createSession(gatewayContextMgt);
        loadSynapseConfigurationFromClasspath(
                "artifacts" + File.separator + "AM" + File.separator + "synapseconfigs" + File.separator + "rest"
                        + File.separator + "dummy-api-multiResourceSameVerb.xml", gatewayContextMgt, gatewaySessionCookie);
    }

    @Test(groups = {"wso2.am"}, description = "Invoke all resources and verbs that are valid")
    public void testInvokeAllResources() throws Exception {
        //Create application
        apiStoreClientUser1.addApplication(APPLICATION_NAME, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiCreationRequestBean.setVisibility("public");
        List<APIResourceBean> apiResourceBeansList = new ArrayList<APIResourceBean>();
        APIResourceBean apiResourceBeanGET = new APIResourceBean("GET", "Application & Application User", "Unlimited",
                "/comp/cartes*");
        APIResourceBean apiResourceBeanPOST = new APIResourceBean("POST", "Application & Application User", "Unlimited",
                "/comp/cartes/op*");
        apiResourceBeansList.add(apiResourceBeanGET);
        apiResourceBeansList.add(apiResourceBeanPOST);
        apiCreationRequestBean.setResourceBeanList(apiResourceBeansList);
        //Create publish and subscribe a API
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        createPublishAndSubscribeToAPI(apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1,
                apiStoreClientUser1, APPLICATION_NAME);
        //get the  access token
        ApplicationKeyBean applicationKeyBean = generateApplicationKeys(apiStoreClientUser1, APPLICATION_NAME);
        String accessToken = applicationKeyBean.getAccessToken();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        //Send GET request
        HttpResponse httpResponseGet =
                HttpRequestUtil.doGet(getGatewayURLNhttp() + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_ENDPOINT_RESOURCE,
                        requestHeaders);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request for " +
                API_ENDPOINT_RESOURCE);
        assertTrue(httpResponseGet.getData().contains(RESPONSE_GET), "Response Data not match for GET request for" +
                API_ENDPOINT_RESOURCE + " Expected value :\"" + RESPONSE_GET + "\" not contains" +
                " in response data:\"" + httpResponseGet.getData() + "\"");

        //Send POST request
        HttpResponse httpResponsePost = HttpRequestUtil
                .doPost(new URL(getGatewayURLNhttp() + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_ENDPOINT_RESOURCE), "",
                        requestHeaders);
        assertEquals(httpResponsePost.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for POST request for " + API_ENDPOINT_RESOURCE);
        assertTrue(httpResponsePost.getData().contains(RESPONSE_POST), "Response Data not match for POST request for" +
                " auth type Application & Application User. Expected value :\"" + RESPONSE_GET + "\" not contains" +
                " in response data:\"" + httpResponsePost.getData() + "\"");

    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
        apiStoreClientUser1.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);
    }

}
