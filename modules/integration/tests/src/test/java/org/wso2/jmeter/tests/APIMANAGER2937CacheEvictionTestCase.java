/*
*Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.jmeter.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;
import org.wso2.automation.tools.jmeter.JMeterTest;
import org.wso2.automation.tools.jmeter.JMeterTestManager;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.annotations.SetEnvironment;

import java.io.File;

public class APIMANAGER2937CacheEvictionTestCase {
    protected Log log = LogFactory.getLog(getClass());


    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_all})
    @Test(groups = "wso2.am")
    public void testCacheEviction() throws Exception {

        log.info("Starting CacheEviction Test.");
        JMeterTest publishScript = new JMeterTest(new File(ProductConstant.SYSTEM_TEST_RESOURCE_LOCATION + File.separator
                                                           + "artifacts" + File.separator + "AM" + File.separator + "scripts"
                                                           + File.separator +
                                                           "eviction_test_publish_and_subscribe_script.jmx"));

        JMeterTestManager manager = new JMeterTestManager();
        manager.runTest(publishScript);

        log.info("Finished creating API. Starting run the load.");
        Thread.sleep(1000);

        JMeterTest scriptGET = new JMeterTest(new File(ProductConstant.SYSTEM_TEST_RESOURCE_LOCATION + File.separator
                                                       + "artifacts" + File.separator + "AM" + File.separator + "scripts"
                                                       + File.separator + "eviction_test_run_load.jmx"));

        manager.runTest(scriptGET);

        log.info("Finished running load test");

    }    
}
