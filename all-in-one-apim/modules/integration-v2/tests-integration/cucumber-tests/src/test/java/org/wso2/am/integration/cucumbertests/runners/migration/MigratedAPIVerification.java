package org.wso2.am.integration.cucumbertests.runners.migration;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = "src/test/resources/features/migration/migrated_api_verification.feature",
        glue = "org.wso2.am.integration.cucumbertests.stepdefinitions",
        plugin = {"pretty", "html:target/cucumber-report/createAPI.html"}
)

public class MigratedAPIVerification extends AbstractTestNGCucumberTests{
}
