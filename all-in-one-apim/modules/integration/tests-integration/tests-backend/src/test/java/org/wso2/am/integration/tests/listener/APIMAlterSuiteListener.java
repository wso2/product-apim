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
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.util.ArrayList;
import java.util.List;

public class APIMAlterSuiteListener implements IAlterSuiteListener {
    @Override
    public void alter(List<XmlSuite> list) {
        String testsToRunCommaSeparated = System.getenv("PRODUCT_APIM_TESTS");
        String testClassesToRunCommaSeparated = System.getenv("PRODUCT_APIM_TEST_CLASSES");
        String testGroupsToRunCommaSeparated = System.getenv("PRODUCT_APIM_TEST_GROUPS");
        String isReleaseBuild = System.getProperty("performRelease");
        if (!StringUtils.isBlank(isReleaseBuild) && "true".equals(isReleaseBuild)) {
            list.clear();
        } else {
            if (StringUtils.isBlank(testsToRunCommaSeparated) && StringUtils.isBlank(testClassesToRunCommaSeparated)
                    && StringUtils.isBlank(testGroupsToRunCommaSeparated)) {
                return;
            }
            String[] enabledTests = new String[]{};
            String[] enabledTestClasses = new String[]{};
            String[] enableTestGroups = new String[]{};
            if (!StringUtils.isBlank(testsToRunCommaSeparated)) {
                enabledTests = testsToRunCommaSeparated.split(",");
            }
            if (!StringUtils.isBlank(testClassesToRunCommaSeparated)) {
                enabledTestClasses = testClassesToRunCommaSeparated.split(",");
            }
            if (!StringUtils.isBlank(testGroupsToRunCommaSeparated)) {
                enableTestGroups = testGroupsToRunCommaSeparated.split(",");
            }
            for (XmlSuite suite : list) {
                if ("ApiManager-features-test-suite".equals(suite.getName())) {
                    List<XmlTest> newXMLTests = new ArrayList<>();
                    for (XmlTest xmlTest : suite.getTests()) {
                        // process PRODUCT_APIM_TESTS to select xml tests to run
                        boolean xmlTestAdded = false;
                        for (String enabledTest : enabledTests) {
                            if (enabledTest.trim().equals(xmlTest.getName().trim())) {
                                xmlTestAdded = true;
                                newXMLTests.add(xmlTest);
                            }
                        }
                        // Process PRODUCT_APIM_TEST_GROUPS to select xml tests to run.
                        for (String enabledTestGroup : enableTestGroups) {
                            if (enabledTestGroup.trim().equals(getTestGroup(xmlTest))) {
                                xmlTestAdded = true;
                                newXMLTests.add(xmlTest);
                            }
                        }

                        List<XmlClass> selectedXmlClassList = new ArrayList<>();
                        // process PRODUCT_APIM_TEST_CLASSES to select xml test classes to run
                        for (String enabledTestClass : enabledTestClasses) {
                            List<XmlClass> xmlClassList = xmlTest.getClasses();
                            for (int i = 0; i < xmlClassList.size(); i++) {
                                if (enabledTestClass.trim().equals(xmlClassList.get(i).getName().trim())) {
                                    // add first XML test class of the XML test always if any of the test xml class is
                                    // selected.
                                    if (i != 0) {
                                        selectedXmlClassList.add(xmlClassList.get(0));
                                    }
                                    selectedXmlClassList.add(xmlClassList.get(i));
                                }
                            }
                        }
                        // if any xml class is selected in the particular xml test, we need to run the xml test if it is
                        //  not added to the suite to run
                        if (selectedXmlClassList.size() > 0) {
                            xmlTest.setClasses(selectedXmlClassList);
                            // when the xml is not added already from the PRODUCT_APIM_TESTS list, need to add them.
                            if (!xmlTestAdded) {
                                newXMLTests.add(xmlTest);
                            }
                        }
                    }
                    suite.setTests(newXMLTests);
                }
            }
        }
    }

    private String getTestGroup(XmlTest xmlTest) {

        return xmlTest.getParameter("group");
    }
}
