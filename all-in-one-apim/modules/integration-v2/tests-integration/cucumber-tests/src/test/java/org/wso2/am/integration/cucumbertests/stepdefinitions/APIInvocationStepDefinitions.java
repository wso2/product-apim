package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;
import org.wso2.am.integration.clients.store.api.v1.dto.APIDTO;
import org.wso2.am.integration.cucumbertests.TestContext;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.HashMap;
import java.util.Map;

public class APIInvocationStepDefinitions {

    private final TestContext context;
    String baseGatewayUrl;
    String apiUrl;

    public APIInvocationStepDefinitions(TestContext context) {
        this.context = context;
        baseGatewayUrl= this.context.get("baseGatewayUrl").toString();
    }


    @When("I invoke API of ID {string} with path {string} and method POST using access token {string} to add customer with name {string}")
    public void i_invoke_post_api_to_add_customer(String apiId, String path, String accessToken, String customerName) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        String actualAccessToken = resolveFromContext(accessToken);

        RestAPIStoreImpl store = (RestAPIStoreImpl) context.get("store");
        APIDTO apiDto = store.getAPI(actualApiId);
        String apiContext = apiDto.getContext();
        apiUrl = baseGatewayUrl + apiContext + path;

        // Construct XML payload
        String xmlBody = String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                        "<customer>\n" +
                        "    <name>%s</name>\n" +
                        "</customer>", customerName);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + actualAccessToken);
        headers.put("Content-Type", "text/xml");

        HttpResponse response = HTTPSClientUtils.doPost(apiUrl, headers, xmlBody);
        context.set("invokeAPIResponse", response);
    }


    @When("I invoke API of ID {string} with path {string} and method GET using access token {string}")
    public void i_invoke_api_with_accessToken(String apiId, String path, String accessToken) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        String actualAccessToken = resolveFromContext(accessToken);

        RestAPIStoreImpl store = (RestAPIStoreImpl) context.get("store");
        APIDTO apiDto = store.getAPI(actualApiId);
        String apiContext = apiDto.getContext();
        apiUrl = baseGatewayUrl + apiContext + path;

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + actualAccessToken);

        HttpResponse response = HTTPSClientUtils.doGet(apiUrl, headers);
        context.set("invokeAPIResponse", response);
    }

    @When("I invoke API of ID {string} with path {string} and method GET using access token {string} and header {string}")
    public void invoke_api_with_custom_auth_header(String apiId, String path, String accessToken, String headerName) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        String token = resolveFromContext(accessToken);
        RestAPIStoreImpl store = (RestAPIStoreImpl) context.get("store");
        APIDTO apiDto = store.getAPI(actualApiId);
        String baseGatewayUrl = context.get("baseGatewayUrl").toString();
        String apiUrl = baseGatewayUrl + apiDto.getContext() + path;

        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put(headerName, "Bearer " + token);

        HttpResponse response = HTTPSClientUtils.doGet(apiUrl, headers);
        context.set("invokeAPIResponse", response);
    }

    @When("I invoke API of ID {string} with path {string} and method GET using API key {string} and header {string}")
    public void invoke_api_with_api_key_header(String apiId, String path, String apiKey, String headerName) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        String key = resolveFromContext(apiKey);
        RestAPIStoreImpl store = (RestAPIStoreImpl) context.get("store");
        APIDTO apiDto = store.getAPI(actualApiId);
        String baseGatewayUrl = context.get("baseGatewayUrl").toString();
        String apiUrl = baseGatewayUrl + apiDto.getContext() + path;

        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put(headerName, key);

        HttpResponse response = HTTPSClientUtils.doGet(apiUrl, headers);
        context.set("invokeAPIResponse", response);
    }


    @Then("the API response status should be {int}")
    public void the_api_response_status_should_be(int expectedStatus) {
        HttpResponse response = (HttpResponse) context.get("invokeAPIResponse");
        Assert.assertEquals(response.getResponseCode(), expectedStatus, response.toString());
    }

    private String resolveFromContext(String input) {
        if (input.startsWith("<") && input.endsWith(">")) {
            return (String) context.get(input.substring(1, input.length() - 1));
        }
        return input;
    }
}
