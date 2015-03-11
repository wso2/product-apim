/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.am.jmeter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.carbon.automation.extensions.jmeter.JMeterTest;
import org.wso2.carbon.automation.extensions.jmeter.JMeterTestManager;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;

import java.io.File;

public class APIMANAGER2937CacheEvictionTestCase extends AMIntegrationBaseTest {

    protected Log log = LogFactory.getLog(getClass());

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
    }

    @Test(groups = "wso2.am")
    public void testCacheEviction() throws Exception {

        log.info("Starting CacheEviction Test.");
        JMeterTest publishScript = new JMeterTest(new File(TestConfigurationProvider.getResourceLocation() +
                File.separator + "artifacts" + File.separator + "AM" + File.separator + "scripts"
                + File.separator + "eviction_test_publish_and_subscribe_script.jmx"));

        JMeterTestManager manager = new JMeterTestManager();
        manager.runTest(publishScript);

        log.info("Finished creating API. Starting run the load.");
        Thread.sleep(1000);

        JMeterTest scriptGET = new JMeterTest(new File(TestConfigurationProvider.getResourceLocation() + File.separator
                + "artifacts" + File.separator + "AM" + File.separator + "scripts"
                + File.separator + "eviction_test_run_load.jmx"));

        manager.runTest(scriptGET);

        log.info("Finished running load test");
    }
}
