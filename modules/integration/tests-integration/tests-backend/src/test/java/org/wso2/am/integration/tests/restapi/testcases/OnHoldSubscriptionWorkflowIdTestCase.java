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

package org.wso2.am.integration.tests.restapi.testcases;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.parser.JSONParser;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.tests.restapi.RESTAPITestConstants;
import org.wso2.am.integration.tests.restapi.utils.RESTAPITestUtil;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.io.*;

import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Test case for the fix for APIMANAGER-5898 - When the status is ON_HOLD, the publisher rest API should return the
 * external workflow id in the response to GET /api/am/publisher/v0.16//subscriptions/{subscriptionId1}
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE})
public class OnHoldSubscriptionWorkflowIdTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(OnHoldSubscriptionWorkflowIdTestCase.class);
    private org.json.simple.JSONObject originalTenantConf;
    private static final String UTF_8 = "UTF-8";
    private static final String TENANT_CONFIG_PATH = "artifacts/AM/configFiles/tenantConf/tenant-conf.json";
    private static final String WORKFLOW_CONFIG_PATH
            = "artifacts/AM/configFiles/APIM5898/tenant-workflow-conf.json";


    @Factory(dataProvider = "userModeDataProvider")
    public OnHoldSubscriptionWorkflowIdTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{ TestUserMode.SUPER_TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        originalTenantConf =  (org.json.simple.JSONObject) new JSONParser().parse(restAPIAdmin.getTenantConfig());
        InputStream tenantConfigStream = getClass().getClassLoader().getResourceAsStream(TENANT_CONFIG_PATH);
        assertNotNull("Tenant config stream can not be null", tenantConfigStream);

        org.json.simple.JSONObject tenantJsonObject = (org.json.simple.JSONObject) new JSONParser().parse(
                new InputStreamReader(tenantConfigStream, UTF_8));

        InputStream wfStream = getClass().getClassLoader().getResourceAsStream(WORKFLOW_CONFIG_PATH);
        assertNotNull("Workflow stream can not be null", wfStream);
        org.json.simple.JSONObject wfJsonObj = (org.json.simple.JSONObject) new JSONParser().parse(
                new InputStreamReader(wfStream, UTF_8));

        tenantJsonObject.put("Workflows",wfJsonObj);
        restAPIAdmin.updateTenantConfig(tenantJsonObject);
    }

    @Test(groups = {"wso2.am"}, description = "Returning workflow external ref. id from subscriptions rest api in "
            + "publisher when subscription is in ON_HOLD status")
    public void testSubscription() {

        String gatewayURL = getGatewayURLNhttp();
        String keyManagerURL = getKeyManagerURLHttp();

        //file name of the JSON data file related to multiple API Subscription test case
        String dataFilePath = (new File(System.getProperty("user.dir"))).getParent() +
                RESTAPITestConstants.PATH_SUBSTRING + "APIM5898.txt";
        boolean testSuccessStatus = new RESTAPITestUtil().testRestAPI(dataFilePath, gatewayURL, keyManagerURL);
        assertTrue(testSuccessStatus);
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        restAPIAdmin.updateTenantConfig(originalTenantConf);
        super.cleanUp();
    }


    /**
     * Read the file content and return the content as String.
     *
     * @param fileLocation - Location of the file.
     * @return String - content of the file.
     * @throws APIManagerIntegrationTestException - exception throws when reading the file.
     */
    protected String readFile(String fileLocation) throws APIManagerIntegrationTestException {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(new File(fileLocation)));
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (IOException ioE) {
            throw new APIManagerIntegrationTestException("IOException when reading the file from:" + fileLocation, ioE);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    log.warn("Error when closing the buffer reade which used to reed the file:" + fileLocation +
                            ". Error:" + e.getMessage());
                }
            }
        }
    }

}
