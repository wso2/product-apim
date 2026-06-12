/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.cucumbertests.utils.listeners;

import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlSuite;
import org.wso2.am.integration.test.utils.Constants;

import java.util.List;

/**
 * Alters TestNG XmlSuites to enable/disable parallel execution of test groups at runtime
 * based on environment variables.
 *
 */
public class ParallelToggleAlterSuiteListener implements IAlterSuiteListener {

    @Override
    public void alter(List<XmlSuite> suites) {

        boolean isParallel = Boolean.parseBoolean(System.getenv(Constants.APIM_TEST_CONTAINERS_PARALLEL_ENABLED));
        int threadCount = Integer.parseInt(System.getenv(Constants.PARALLEL_THREAD_COUNT));

        // Set parallel mode to TEST if parallel test execution is enabled
        XmlSuite.ParallelMode parallelMode = isParallel ?
                XmlSuite.ParallelMode.TESTS : XmlSuite.ParallelMode.NONE;

        for (XmlSuite suite : suites) {
            suite.setParallel(parallelMode);  // override <suite parallel="...">
            suite.setThreadCount(isParallel ? threadCount : 1);    // override thread-count
        }
    }
}
