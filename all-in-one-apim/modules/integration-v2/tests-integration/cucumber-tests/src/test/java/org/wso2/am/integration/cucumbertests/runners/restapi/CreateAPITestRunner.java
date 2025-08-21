package org.wso2.am.integration.cucumbertests.runners.restapi;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = "src/test/resources/features/restapi/publisher/create_an_api_through_the_publisher_rest_api_test.feature",
        glue = "org.wso2.am.integration.cucumbertests.stepdefinitions",
        plugin = {"pretty", "html:target/cucumber-report/createAPI.html"}
)

public class CreateAPITestRunner extends AbstractTestNGCucumberTests{
}
