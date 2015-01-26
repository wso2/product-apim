/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.am.tests.rest;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.am.tests.APIManagerIntegrationTest;
import org.wso2.carbon.automation.core.utils.HttpRequestUtil;
import org.wso2.carbon.automation.core.utils.HttpResponse;
import org.wso2.carbon.automation.utils.httpclient.HttpURLConnectionClient;

import java.io.*;
import java.net.URL;

/**
 * Related to Patch Automation  https://wso2.org/jira/browse/APIMANAGER-769
 * This test class test the Rest URI template patterns like uri-template="/view/*"
 */
public class URLMappingRESTTestCase extends APIManagerIntegrationTest {

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        loadESBConfigurationFromClasspath("artifacts" + File.separator + "AM"
                + File.separator + "synapseconfigs" + File.separator + "rest"
                + File.separator + "url-mapping-synapse.xml");
    }

    @Test(groups = {"wso2.am"}, description = "Sending a Message Via REST to test uri template fix")
    public void testRESTURITemplate() throws Exception {
        // Before apply this patch uri template not recognize localhost:8280/stockquote/test/ and localhost:8280/stockquote/test
        //maps to same resource. It will return correct response only if request hits localhost:8280/stockquote/test
        //after fixing issue both will work.
        HttpResponse response = HttpRequestUtil.sendGetRequest(getApiInvocationURLHttp("stockquote") + "/test/", null);
//        HttpResponse response = HttpRequestUtil.sendGetRequest(getApiInvocationURLHttp("stockquote") + "/test/", null);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatch");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanup();
    }
}

