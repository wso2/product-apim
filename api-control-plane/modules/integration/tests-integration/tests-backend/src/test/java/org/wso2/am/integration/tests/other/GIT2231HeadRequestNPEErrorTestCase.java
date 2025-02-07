/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.io.File;

public class GIT2231HeadRequestNPEErrorTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(GIT2231HeadRequestNPEErrorTestCase.class);
    private static final String apiResourceUrl = "git2231headapi/1.0.0/testing";

    @Factory(dataProvider = "userModeDataProvider")
    public GIT2231HeadRequestNPEErrorTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        String gatewaySessionCookie = createSession(gatewayContextMgt);
        if (userMode != TestUserMode.SUPER_TENANT_ADMIN) {
            loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM"
                    + File.separator + "synapseconfigs" + File.separator + "rest"
                    + File.separator + "git2231_head_api_tenant.xml", gatewayContextMgt, gatewaySessionCookie);
        }
    }

    @Test(groups = "wso2.am", description = "Check whether the Location header is correct")
    public void testAPIWithLocationHeader() throws Exception {
        String apiInvocationUrl;
        if (userMode != TestUserMode.SUPER_TENANT_ADMIN) {
            apiInvocationUrl = getAPIInvocationURLHttp("t/" + user.getUserDomain() + "/" + apiResourceUrl);
        } else {
            apiInvocationUrl = getAPIInvocationURLHttp(apiResourceUrl);
        }
        HttpClient httpclient = HttpClients.createDefault();
        HttpUriRequest head = new HttpHead(apiInvocationUrl);

        HttpResponse httpResponse = httpclient.execute(head);
        Assert.assertEquals(httpResponse.getStatusLine().getStatusCode(), 204);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN}
        };
    }

}
