package org.wso2.am.integration.cucumbertests.runners.common;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.Test;

@CucumberOptions(
        features = "src/test/resources/features/common/migrated_tenant_user_initialization.feature",
        glue = "org.wso2.am.integration.cucumbertests.stepdefinitions",
        plugin = {"pretty", "html:target/cucumber-report/testrunner.html"}
)

@Test(groups = {"migrationTest"})
public class MigratedTenantUserInitilization extends AbstractTestNGCucumberTests {
}
