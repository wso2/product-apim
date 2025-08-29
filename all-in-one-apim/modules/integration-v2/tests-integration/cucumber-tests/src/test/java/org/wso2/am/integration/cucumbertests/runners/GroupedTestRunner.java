package org.wso2.am.integration.cucumbertests.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(
        features = "src/test/resources/features/tenant_user_initialisation.feature",
        glue = "org.wso2.am.integration.cucumbertests.stepdefinitions",
        plugin = {"pretty", "html:target/cucumber-report/groupedtestrunner.html"}
)

@Test(groups = "groupTest")
public class GroupedTestRunner extends AbstractTestNGCucumberTests{
}
