/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.listener;

import org.apache.commons.lang.StringUtils;
import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.util.ArrayList;
import java.util.List;

public class APIMAlterSuiteListener implements IAlterSuiteListener {
    @Override
    public void alter(List<XmlSuite> list) {
        String testsToRunCommaSeparated = System.getenv("PRODUCT_APIM_TESTS");
        if (StringUtils.isBlank(testsToRunCommaSeparated)) {
            return;
        }
        String[] enabledTests = testsToRunCommaSeparated.split(",");
        for (XmlSuite suite: list) {
            if ("ApiManager-features-test-suite".equals(suite.getName())) {
                List<XmlTest> newXMLTests = new ArrayList<>();
                for(XmlTest test: suite.getTests()) {
                    for (String enabledTest: enabledTests) {
                        if (enabledTest.equals(test.getName())) {
                            newXMLTests.add(test);
                        }
                    }
                }
                suite.setTests(newXMLTests);
            }
        }
    }
}
