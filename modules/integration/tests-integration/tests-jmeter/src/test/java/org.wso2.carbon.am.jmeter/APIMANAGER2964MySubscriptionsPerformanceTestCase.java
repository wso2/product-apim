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
import org.testng.annotations.Test;
import org.wso2.carbon.automation.extensions.jmeter.JMeterTest;
import org.wso2.carbon.automation.extensions.jmeter.JMeterTestManager;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;

import java.io.File;

public class APIMANAGER2964MySubscriptionsPerformanceTestCase  {

    protected Log log = LogFactory.getLog(APIMANAGER2964MySubscriptionsPerformanceTestCase.class);

    @Test(groups = "wso2.am", description = "Login to api manager as user2")
    public void testListServices() throws Exception {

        log.info("Starting APIMANAGER2964MySubscriptionsPerformanceTestCase");

        JMeterTest script =
                new JMeterTest(new File(TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts"
                        + File.separator + "AM" + File.separator + "scripts"
                        + File.separator + "APIMANAGER2964MySubscriptionScript.jmx"));

        JMeterTestManager manager = new JMeterTestManager();
        manager.runTest(script);
        log.info("Successfully completed APIMANAGER2964MySubscriptionsPerformanceTestCase");
    }
}