/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.wso2.am.integration.tests.api.lifecycle;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.logging.view.stub.LogViewerLogViewerException;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PluggableVersioningStrategyTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(PluggableVersioningStrategyTestCase.class);

    private static final String API_NAME = "PluggableVersionTestAPI";
    private static final String API_CONTEXT = "{version}/PluggableVersionTestAPI";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String INVOKABLE_API_CONTEXT = API_VERSION_1_0_0 + "/PluggableVersionTestAPI";
    private static final String API_TAGS = "Pluggable, Version, testTag3";
    private static final String API_DESCRIPTION = "This is test API to test pluggable version strategy";
    private static final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private static final String APPLICATION_NAME = "PluggableVersionTestApp";
    private String providerName;
    private APIIdentifier apiIdentifier;
    private APIPublisherRestClient apiPublisherClientUser;
    private APIStoreRestClient apiStoreClientUser;
    private APICreationRequestBean apiCreationRequestBean;
    private final static String API_GET_ENDPOINT_METHOD = "/customers/123";
    private final static String RESPONSE_GET = "<id>123</id><name>John</name></Customer>";


    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        String apiEndPointUrl = gatewayUrls.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiPublisherClientUser = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with  admin
        apiPublisherClientUser.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        //Login to API Store with  admin
        apiStoreClientUser.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);

        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiCreationRequestBean.setTier(TIER_GOLD);
        apiCreationRequestBean.setTiersCollection(TIER_GOLD);
    }

    @Test(groups = {"wso2.am"}, description = "This test method tests the pluggable versioning stratergy")
    public void testPluggableVersioningStratergy() throws APIManagerIntegrationTestException,
            IOException, LogViewerLogViewerException {

        //Create an application with gold tier
        apiStoreClientUser.addApplication(APPLICATION_NAME, TIER_GOLD, "", "");

        //Create publish and subscribe an API
        APIIdentifier apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiIdentifier.setTier(TIER_GOLD);
        createPublishAndSubscribeToAPI(
                apiIdentifier, apiCreationRequestBean, apiPublisherClientUser, apiStoreClientUser, APPLICATION_NAME);

        //Add request headers
        HashMap<String, String> requestHeadersGet = new HashMap<String, String>();
        requestHeadersGet.put("accept", "text/xml");

        //Get the  access token
        String accessToken = generateApplicationKeys(apiStoreClientUser, APPLICATION_NAME).getAccessToken();
        requestHeadersGet.put("Authorization", "Bearer " + accessToken);

        //Send GET Request
        HttpResponse httpResponse =
                HttpRequestUtil.doGet(gatewayWebAppUrl + INVOKABLE_API_CONTEXT + API_GET_ENDPOINT_METHOD,
                        requestHeadersGet);
        assertEquals(httpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");
        assertTrue(httpResponse.getData().contains(RESPONSE_GET), "Response Data not match for GET request." +
                " Expected value : " + RESPONSE_GET + " not contains in response data " + httpResponse.getData());
    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        //Remove application and the subscription
        if (apiStoreClientUser != null) {
            apiStoreClientUser.removeApplication(APPLICATION_NAME);
        }

        //delete the API
        if (apiPublisherClientUser != null) {
            apiPublisherClientUser.deleteAPI(API_NAME, API_VERSION_1_0_0, providerName);
        }
    }
}
