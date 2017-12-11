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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.logging.view.stub.LogViewerLogViewerException;

import java.io.IOException;
import java.util.HashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PluggableVersioningStrategyTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(PluggableVersioningStrategyTestCase.class);

    private final String INVOKABLE_API_CONTEXT = API_VERSION_1_0_0 + "/api";
    private APIPublisherRestClient apiPublisherClientUser;
    private APIStoreRestClient apiStoreClientUser;
    private final static String API_GET_ENDPOINT_METHOD = "/customers/123";
    private final static String RESPONSE_GET = "<id>123</id><name>John</name></Customer>";


    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        apiPublisherClientUser = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with  admin
        apiPublisherClientUser.login(user.getUserName(), user.getPassword());
        //Login to API Store with  admin
        apiStoreClientUser.login(user.getUserName(), user.getPassword());
    }

    @Test(groups = {"webapp"}, description = "This test method tests the pluggable versioning stratergy")
    public void testPluggableVersioningStratergy() throws Exception,
                                                          IOException, LogViewerLogViewerException {

        //Add request headers
        HashMap<String, String> requestHeadersGet = new HashMap<String, String>();
        requestHeadersGet.put("accept", "text/xml");

        //Get the  access token
        String accessToken = System.getProperty(APPLICATION_NAME + "-accessToken");
        requestHeadersGet.put("Authorization", "Bearer " + accessToken);

        //Send GET Request
        HttpResponse httpResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(INVOKABLE_API_CONTEXT) + API_GET_ENDPOINT_METHOD,
                                      requestHeadersGet);
        assertEquals(httpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");
        assertTrue(httpResponse.getData().contains(RESPONSE_GET), "Response Data not match for GET request." +
                                                                  " Expected value : " + RESPONSE_GET + " not contains in response data " + httpResponse.getData());
    }

}
