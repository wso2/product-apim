/*
 *  Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.other;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.io.File;
import java.rmi.RemoteException;

public class CarbonAppUploadTestCase extends APIMIntegrationBaseTest {

    private static final String CAR_FILE_NAME = "carapp_1.0.0.car";

    @Factory(dataProvider = "userModeDataProvider")
    public CarbonAppUploadTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN } };
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init(userMode);
    }

    @Test(groups = { "wso2.am" }, description = "Upload a Carbon Application via a .car file")
    public void testUploadCarbonApp() throws Exception {

        String carFilePath = getAMResourceLocation() + File.separator + "carbonapps" + File.separator + CAR_FILE_NAME;
        File carFile = new File(carFilePath);
        Assert.assertTrue(carFile.exists(), "Source CAR file not found at: " + carFilePath);
        try {
            carbonApplicationUploaderClient.uploadCarbonApp(carFilePath);
        } catch (RemoteException e) {
            Assert.fail("CAR file upload failed. Server responded with: " + e.getMessage());
        }
    }
}
