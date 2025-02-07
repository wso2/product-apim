/*
 *
 *   Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.tests.publisher;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.testng.Assert.assertEquals;

/**
 * Get linter custom rules through the publisher REST API
 * APIM-534 / APIM-542
 */

public class GetLinterCustomRulesThroughThePublisherRestAPITestCase extends
        APIMIntegrationBaseTest {

    private static final Log log = LogFactory.
            getLog(GetLinterCustomRulesThroughThePublisherRestAPITestCase.class);
    private JSONObject tenantConfig;
    JSONParser jsonParser = new JSONParser();
    String linterCustomRulesKey = "LinterCustomRules";

    @Factory(dataProvider = "userModeDataProvider")
    public GetLinterCustomRulesThroughThePublisherRestAPITestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        String tenantConfContent = FileUtils.readFileToString(new File(getAMResourceLocation() + File.separator
                + "configFiles" + File.separator + "linterCustomRulesTest" + File.separator + "tenant-conf.json"), "UTF-8");
        tenantConfig = (JSONObject) jsonParser.parse(tenantConfContent);
        restAPIAdmin.updateTenantConfig(tenantConfig);

    }

    @Test(description = "Test get linter rules through publisher REST API")
    public void testGetLinterCustomRulesThroughThePublisherRestAPI() throws Exception {

        String linterCustomRulesActual = restAPIPublisher.getLinterCustomRules();
        String linterCustomRulesExpected = tenantConfig.get(linterCustomRulesKey).toString();

        assertEquals(linterCustomRulesActual, linterCustomRulesExpected);
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {

        super.cleanUp();
    }

}
