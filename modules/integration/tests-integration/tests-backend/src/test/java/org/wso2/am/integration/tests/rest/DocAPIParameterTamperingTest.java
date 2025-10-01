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

package org.wso2.am.integration.tests.rest;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

/**
 * Test class is used to test the stack trace exposure of Doc API when the
 * parameters are tampered.
 */
public class DocAPIParameterTamperingTest extends APIMIntegrationBaseTest {

    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private static final Log log = LogFactory.getLog(DocAPIParameterTamperingTest.class);

    @Factory(dataProvider = "userModeDataProvider")
    public DocAPIParameterTamperingTest(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
    }

    @Test(groups = {"wso2.am"}, description = "Test whether the response expose the stack trace")
    public void testParameterTampaeredResponseOfDocAPI() {

        boolean expectedExceptionOccured = false;
        try {
            restAPIPublisher.getDocumentContent(";alert(1)",
                    "daf732d3-bda2-46da-b381-2c39d901ea61");
            Assert.fail("ApiException Exception expected to be thrown");
        } catch (ApiException e) {
            log.error(e);
            if (e.getCode() == 401) {
                expectedExceptionOccured = true;
            }
        }
        Assert.assertTrue("Expected ApiException to be thrown with a response code 401. But it was not thrown " +
                "as that.", expectedExceptionOccured);
    }
}
