package org.wso2.am.integration.cucumbertests.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = "src/test/resources/features/customHeaderTest",
        glue = "org.wso2.am.integration.cucumbertests.stepdefinitions",
        plugin = {"pretty", "html:target/cucumber-report/customHeader.html"}
)

public class CustomHeaderTestRunner extends AbstractTestNGCucumberTests{
}
