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

package org.wso2.carbon.am.jmeter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.extensions.jmeter.JMeterTest;
import org.wso2.carbon.automation.extensions.jmeter.JMeterTestManager;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;

import java.io.File;

/**
 * Jmeter Testcase to test the performance of Application update and deletion when large number of Applications are
 * created in the database.
 */
public class APIMANAGER4488ApplicationUpdatePerformanceTestCase {
    protected Log log = LogFactory.getLog(getClass());

    @Test(groups = "wso2.am", description = "Application update performance test")
    public void testApplicationUpdate() throws Exception {
        log.info("Starting Application Update/Delete Performance TestCase");
        JMeterTest script = new JMeterTest(new File(
                TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" + File.separator + "AM"
                        + File.separator + "scripts" + File.separator
                        + "APIMANAGER4488ApplicationUpdatePerformanceTestCase.jmx"));

        JMeterTestManager manager = new JMeterTestManager();
        manager.runTest(script);
        log.info("Successfully completed Application Update/Delete Performance TestCase");
    }
}
