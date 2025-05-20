package org.wso2.am.integration.cucumbertests.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = "src/test/resources/features/api_lifecycle_test.feature",
        glue = "org.wso2.am.integration.cucumbertests.stepdefinitions",
        plugin = {"pretty", "html:target/cucumber-report/testrunner.html"}
)
public class testrunner extends AbstractTestNGCucumberTests {
}

