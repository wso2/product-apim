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

import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.util.HashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * TestCase used to test the functionality of the Script Mediator when a null value is sent in the JSON response.
 */
public class ScriptMediatorTestCase extends APIMIntegrationBaseTest {

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init();

        String gatewaySessionCookie = createSession(gatewayContextMgt);

        //Load the back-end dummy API
        loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM"
                + File.separator + "synapseconfigs" + File.separator + "scriptmediator"
                + File.separator + "script_mediator_api.xml", gatewayContextMgt, gatewaySessionCookie);
    }

    @Test(groups = "wso2.am", description = "Check whether the script mediator works when a null object is returned " +
                                                                                                        "in the json")
    public void testScriptMediatorWithNullObject() throws Exception {

        String endpoint = getGatewayURLNhttp() + "script/test";

        //Access the deployed API.
        HttpResponse response = HttpRequestUtil.doGet(endpoint, new HashMap<String, String>());

        assertNotNull(response, "Received null response from script mediator endpoint");

        JSONObject jsonResponse = new JSONObject(response.getData());

        assertEquals(jsonResponse.getString("name"), "testName", "Did not find the expected string. Probably the " +
                "backend failed to respond");

        assertEquals(jsonResponse.getString("checkNull"), "null", "Expected 'null' but received " +
                jsonResponse.getString("checkNull"));

    }
}
