/*
 *
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.other;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import static org.testng.Assert.assertTrue;

/**
 * <p>Test case to partial key clean up and Key generation with PostgreSQL
 * as the DB</p>
 * Need to configure pgsql with <code>carbon_db</code> and <code>am_db</code> databases
 * with following credentials to run this test. Also add the pgsql driver.
 * <p><code>user: wso2carbon</code></p>
 * <p><code>password: wso2carbon</code></p>
 */
public class APIMANAGER5327KeyGenerationWithPGSQLTestCase extends APIMIntegrationBaseTest {
    private APIStoreRestClient apiStore;
    private URL cleanUpURLHttp;
    private String password;
    private ServerConfigurationManager serverConfigurationManager;
    private HttpResponse storeLoginResponse;
    private String userName;
    private static final String UNLIMITED_TIER = "Unlimited";
    private static final String APPLICATION_NAME = "APIMANAGER5327";
    private static final String DATA_SOURCES = "master-datasources.xml";

    @BeforeClass
    public void init() {
        try {
            super.init();
            String storeURLHttp = storeUrls.getWebAppURLHttp();
            cleanUpURLHttp = new URL(new URL(storeURLHttp),
                    "store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag");
            userName = storeContext.getContextTenant().getTenantAdmin().getUserName();
            password = storeContext.getContextTenant().getContextUser().getPassword();
            apiStore = new APIStoreRestClient(storeURLHttp);
            String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);

            //update database to pgsql
            String masterDataSources =
                    getAMResourceLocation() + File.separator + "configFiles" + File.separator + "tokengenerationpgsql"
                            + File.separator + DATA_SOURCES;
            File sourceDataSource = new File(masterDataSources);
            File targetDataSource = new File(carbonHome + File.separator + "repository" +
                    File.separator + "conf" + File.separator + "datasources" + File.separator + DATA_SOURCES);
            serverConfigurationManager = new ServerConfigurationManager(storeContext);
            serverConfigurationManager.applyConfiguration(sourceDataSource, targetDataSource, true, true);
        } catch (APIManagerIntegrationTestException e) {
            assertTrue(false, "Error occurred while initializing testcase: " + e.getCause());
        } catch (Exception e) {
            assertTrue(false, "Error occurred while configuring the server instance: " + e.getCause());
        }
    }

    @Test(description = "Create new application and generate keys for it")
    public void generateKeyForApplication() throws Exception {
        storeLoginResponse = apiStore.login(userName, password);

        //create new application and generate keys
        apiStore.addApplication(APPLICATION_NAME, UNLIMITED_TIER, "", "test");
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(APPLICATION_NAME);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();

        JSONObject response = new JSONObject(responseString);
        String error = response.getString("error");
        if ("true".equals(error)) {
            throw new Exception("Unable to generate the keys.");
        }
    }

    @Test(dependsOnMethods = { "generateKeyForApplication" },
          description = "Try to execute application key cleanup and test if pgsql error occurs")
    public void cleanupKeys() throws Exception {
        Map<String, String> cleanupRequestHeaders = new HashMap<String, String>();
        cleanupRequestHeaders.put("Cookie", storeLoginResponse.getHeaders().get("Set-Cookie"));
        String requestData =
                "action=cleanUpApplicationRegistration&applicationName=" + APPLICATION_NAME + "&keyType=PRODUCTION";
        HttpResponse httpResponse = HttpRequestUtil.doPost(cleanUpURLHttp, requestData, cleanupRequestHeaders);

        JSONObject response = new JSONObject(httpResponse.getData());
        String error = response.getString("error");
        Assert.assertNotEquals("true", error, "Error occurred while partial key cleanup");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
        serverConfigurationManager.restoreToLastConfiguration();
    }

}
