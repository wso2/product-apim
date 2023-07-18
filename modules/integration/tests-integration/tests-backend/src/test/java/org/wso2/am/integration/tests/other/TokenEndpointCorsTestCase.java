/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *    WSO2 LLC. licenses this file to you under the Apache License,
 *    Version 2.0 (the "License"); you may not use this file except
 *    in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.tests.api.lifecycle.AddEndPointSecurityPerTypeTestCase;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

public class TokenEndpointCorsTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(AddEndPointSecurityPerTypeTestCase.class);
    private final String API_NAME = "TokenEndpointCorsAPI";
    private final String API_CONTEXT = "TokenEndpointCorsAPI";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "AddEndPointSecurityPerTypeTestCase";
    private String providerName;
    private APICreationRequestBean apiCreationRequestBean;
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String apiID;
    ArrayList<String> apiIds = new ArrayList<>();
    String tokenEndpointURL;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                new URL(apiEndPointUrl));
        APIDTO apidto = createAndPublishAPI(apiCreationRequestBean, restAPIPublisher, false);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        apiID = apidto.getId();
        tokenEndpointURL = keyManagerHTTPSURL + "oauth2/token";
    }


    @Test(groups = {"wso2.am"}, description = "Test CORS for token endpoint")
    public void testCORSforTokenEndpoint() throws Exception {

        Map<String, String> requestHeader = new HashMap<>();
        requestHeader.put("Origin", "http://wso2.is");
        HttpResponse response1 = HTTPSClientUtils.doOptions(tokenEndpointURL, requestHeader);
        log.info(requestHeader.toString());
        log.info(response1.getHeaders().toString());
        log.info(response1.getResponseMessage());
        Assert.assertEquals(response1.getHeaders().get("Access-Control-Allow-Origin"), "http://wso2.is");
        Assert.assertEquals(response1.getResponseCode(), 200);

        requestHeader.put("Origin", "http://wso3.is");
        HttpResponse response2 = HTTPSClientUtils.doOptions(tokenEndpointURL, requestHeader);
        log.info(requestHeader.toString());
        log.info(response2.getHeaders().toString());
        log.info(response2.getResponseMessage());
        Assert.assertEquals(response2.getResponseCode(), 403);
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        for (String apiId: apiIds) {
            undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
            restAPIPublisher.deleteAPI(apiId);
        }
        super.cleanUp();
    }

}
