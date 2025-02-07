/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WorkflowResponseDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import static org.testng.Assert.assertNotNull;

public class LocationHeaderTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(LocationHeaderTestCase.class);
    private String applicationId;
    private String apiId;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init();
    }

    @Test(groups = "wso2.am", description = "Check whether the Location header is correct")
    public void testAPIWithLocationHeader() throws Exception {

        String endpointUrl = getAPIInvocationURLHttp("locationheader");
        HttpClient httpclient = new DefaultHttpClient();
        HttpUriRequest get = new HttpGet(endpointUrl);
        org.apache.http.HttpResponse httpResponse = httpclient.execute(get);
        Header locationHeader = httpResponse.getFirstHeader("Location");

        Assert.assertFalse(locationHeader.getValue().endsWith("//abc/domain"),
                "Location header contains additional / character");
        Assert.assertTrue(locationHeader.getValue().endsWith("/abc/domain"),
                "Unexpected Location header. Expected to end with "
                        + "/abc/domain but received " + locationHeader);

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        super.cleanUp();
    }
}
