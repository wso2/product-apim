package org.wso2.am.integration.tests.listener;

import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.util.ArrayList;
import java.util.List;

public class APIMAlterSuiteListener implements IAlterSuiteListener {
    @Override
    public void alter(List<XmlSuite> list) {
        String testsToRunCommaSeparated = System.getenv("PRODUCT_APIM_TESTS");
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
