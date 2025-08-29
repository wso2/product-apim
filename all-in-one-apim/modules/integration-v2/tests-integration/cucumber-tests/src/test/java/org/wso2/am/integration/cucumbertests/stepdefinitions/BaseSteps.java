package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.Then;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.TestContext;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

public class BaseSteps {

    protected final TestContext context;

    public BaseSteps( TestContext context2) {

        this.context = context2;
    }

    @Then("The response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatusCode) {

        HttpResponse response = (HttpResponse) context.get("httpResponse");
        Assert.assertEquals(response.getResponseCode(), expectedStatusCode);
    }

    @Then("The response should contain {string}")
    public void responseShouldContainFieldValue(String expectedValue) {

        HttpResponse response = (HttpResponse) context.get("httpResponse");;
        Assert.assertTrue(response.getData().contains(expectedValue));
    }

    @Then("The response should not contain {string}")
    public void responseShouldNotContainFieldValue(String unexpectedValue) {

        HttpResponse response = (HttpResponse) context.get("httpResponse");
        Assert.assertFalse(response.getData().contains(unexpectedValue));
    }

    @Then("The response should contain the header {string} with value {string}")
    public void responseShouldContainHeaderWithValue(String headerName, String expectedValue) {

        HttpResponse response = (HttpResponse) context.get("httpResponse");
        Assert.assertTrue(response.getHeaders().containsKey(headerName), "Header " + headerName + " not found in response");
        Assert.assertEquals(response.getHeaders().get(headerName), expectedValue, "Header value mismatch for " + headerName);
    }

}
