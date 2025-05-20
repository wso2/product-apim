package org.wso2.am.integration.cucumbertests.stepdefinitions;

import com.google.gson.Gson;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.clients.store.api.v1.dto.*;
import org.wso2.am.integration.cucumbertests.TestContext;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class StoreStepDefinitions {

    RestAPIStoreImpl store;
    String baseUrl;
    String createdAppId;
    String createdSubscriptionId;

    private final TestContext context;

    public StoreStepDefinitions(TestContext context) {
        this.context = context;
        baseUrl = this.context.get("baseUrl").toString();
    }


    @When("I initialize the Store REST API client with username {string}, password {string} and tenant {string}")
    public void i_initialize_store_client(String username, String password, String tenantDomain) {
        String containerName = context.get("label").toString();
        store = new RestAPIStoreImpl(username, password, tenantDomain, baseUrl, containerName);
        context.set("store",store);
        context.set("username",username);
        context.set("password",password);
        context.set("tenant",tenantDomain);
    }

    @When("I create an application with the following details")
    public void i_create_an_application_with_the_following_details(DataTable dataTable) throws Exception {
        Map<String, String> appDetails = dataTable.asMap(String.class, String.class);

        String appName = appDetails.get("name");
        String throttlingTier = appDetails.get("throttlingPolicy");
        String callbackUrl = appDetails.getOrDefault("callbackUrl", "");
        String description = appDetails.getOrDefault("description", "");

        ApplicationDTO response = store.addApplication(appName,throttlingTier,callbackUrl,description);
        createdAppId = response.getApplicationId();
        context.set("createdAppId",createdAppId);
    }

    @When("I update the application with id {string} with the following details")
    public void i_update_the_application(String appId,DataTable dataTable) throws Exception {
        Map<String, String> appDetails = dataTable.asMap(String.class, String.class);
        String actualAppId = resolveFromContext(appId);
        ApplicationDTO appDto = store.getApplicationById(actualAppId);

        if (appDetails.containsKey("name")) {appDto.setName(appDetails.get("name"));};
        if (appDetails.containsKey("throttlingPolicy")) {appDto.setThrottlingPolicy(appDetails.get("throttlingPolicy"));};
        if (appDetails.containsKey("description")) {appDto.setDescription(appDetails.get("description"));};

        System.out.println(appDto);
        ApplicationDTO response = store.applicationsApi.applicationsApplicationIdPut(actualAppId,appDto,"");
    }

    @When("I delete the application with id {string}")
    public void i_delete_application(String appId) throws Exception {
        String actualAppId = resolveFromContext(appId);
        store.deleteApplication(actualAppId);
    }

    @When("I subscribe to API {string} using application {string} with throttling policy {string}")
    public void i_subscribe_to_api(String apiId, String applicationId, String throttlingPolicy) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        String actualAppId = resolveFromContext(applicationId);
        HttpResponse response = store.createSubscription(actualApiId,actualAppId,throttlingPolicy);
        createdSubscriptionId = response.getData();
        context.set("createdSubscriptionId",createdSubscriptionId);
    }

    @Then("I should be able to retrieve the application with id {string}")
    public void i_should_be_able_to_retrieve_application(String appId) throws Exception {
        String actualAppId = resolveFromContext(appId);
        ApplicationDTO appDTO=store.getApplicationById(actualAppId);
        Assert.assertEquals(appDTO.getApplicationId(),actualAppId);
    }

    @Then("I should be able to retrieve the subscription for Api {string} by Application {string}")
    public void i_should_be_able_to_retrieve_subscription(String apiId, String appId) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        String actualAppId = resolveFromContext(appId);
        SubscriptionListDTO subDTO = store.getSubscription(actualApiId,actualAppId,null,null);
        Assert.assertNotNull(subDTO);
    }

    @When("I generate client credentials for application id {string} with key type {string}")
    public void i_generate_client_credentials(String appId, String keyType) throws Exception {
        String actualAppId = resolveFromContext(appId);
        List<String> grantTypes = Collections.singletonList("client_credentials");
        List<String> scopes = Collections.emptyList(); // Add any if needed
        ApplicationKeyDTO keyDTO = store.generateKeys(actualAppId, "-1", "https://localhost/callback",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.valueOf(keyType), new ArrayList<>(scopes), grantTypes);

        context.set("clientId", keyDTO.getConsumerKey());
        context.set("clientSecret", keyDTO.getConsumerSecret());
        Assert.assertNotNull(context.get("clientId"));
        Assert.assertNotNull(context.get("clientSecret"));
    }

    @When("I request an access token using grant type {string} without any scope")
    public void i_request_access_token(String grantType) throws Exception {
        String clientId = (String) context.get("clientId");
        String clientSecret = (String) context.get("clientSecret");
        String tokenEndpoint = baseUrl+"oauth2/token";

        String authHeader = Base64.encodeBase64String((clientId + ":" + clientSecret).getBytes());
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + authHeader);
        headers.put("Content-Type", "application/x-www-form-urlencoded");

        String payload = "grant_type=" + grantType + "&username="+ context.get("username") +"&password="+ context.get("password");
        HttpResponse response = doTokenPost(tokenEndpoint, payload, headers);
        System.out.println(response);
        String accessToken = new Gson().fromJson(response.getData(), Map.class).get("access_token").toString();
        Assert.assertNotNull(accessToken,"AccessToken is not generated");
        context.set("generatedAccessToken", accessToken);
    }

    @When("I request an access token using grant type {string} with scope {string}")
    public void i_request_access_token_with_scope(String grantType, String scope) throws Exception {
        String clientId = (String) context.get("clientId");
        String clientSecret = (String) context.get("clientSecret");
        URL tokenEndpoint = new URL(baseUrl + "oauth2/token");

        String authHeader = Base64.encodeBase64String((clientId + ":" + clientSecret).getBytes());
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + authHeader);
        headers.put("Content-Type", "application/x-www-form-urlencoded");

        String payload = "grant_type=" + grantType + "&username="+ context.get("username") +"&password="+ context.get("password")+ "&scope=" + scope;
        System.out.println(payload);
        JSONObject accessTokenGenerationResponseScope = new JSONObject(
                store.generateUserAccessKey(clientId,clientSecret,payload, tokenEndpoint)
                        .getData());

        Assert.assertNotNull(accessTokenGenerationResponseScope);
        System.out.println(accessTokenGenerationResponseScope);
        Assert.assertTrue(accessTokenGenerationResponseScope.getString("scope").contains(scope));
        String accessToken = accessTokenGenerationResponseScope.getString("access_token");
        context.set("generatedAccessToken", accessToken);
    }


    @Then("the access token should be available")
    public void access_token_should_be_available() {
        String token = (String) context.get("generatedAccessToken");
        Assert.assertNotNull(token, "Expected access token to be generated, but it was null");
    }

    @When("I generate an API key for application {string}")
    public void generate_api_key_for_app(String appId) throws Exception {
        String actualAppId = resolveFromContext(appId);
        RestAPIStoreImpl store = (RestAPIStoreImpl) context.get("store");
        APIKeyDTO apiKeyDTO = store.generateAPIKeys(actualAppId, "PRODUCTION", -1, null, null);
        Assert.assertNotNull(apiKeyDTO, "API Key generation failed");
        context.set("generatedApiKey", apiKeyDTO.getApikey());
    }

    @Then("I should be able to list all applications")
    public void i_should_list_all_applications() throws Exception {
        ApplicationListDTO apps = store.getAllApps();
        context.set("allApplications", apps);
    }

    @Then("I should be able to list all subscriptions for application id {string}")
    public void i_should_list_subscriptions(String appId) throws Exception {
        String actualAppId = resolveFromContext(appId);
        SubscriptionListDTO subscriptions = store.getAllSubscriptionsOfApplication(actualAppId);
        context.set("appSubscriptions", subscriptions);
    }

    @Then("I should be able to get the list of published APIs")
    public void i_should_get_published_apis() throws Exception {
        APIListDTO apis = store.getAllPublishedAPIs();
        context.set("publishedApis", apis);
    }

    @When("I add a rating of {int} to API {string} for tenant {string}")
    public void i_add_rating_to_api(Integer rating, String apiId, String tenantDomain) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        HttpResponse response = store.addRating(actualApiId, rating, tenantDomain);
        context.set("ratingResponse", response);
    }

    @When("I remove the rating for API {string} in tenant {string}")
    public void i_remove_rating(String apiId, String tenantDomain) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        HttpResponse response = store.removeRating(actualApiId, tenantDomain);
        context.set("ratingRemovalResponse", response);
    }

    @When("I add a comment {string} to API {string} in category {string}")
    public void i_add_comment_to_api(String comment, String apiId, String category) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        HttpResponse response = store.addComment(actualApiId, comment, category, null);
        context.set("addedComment", response);
    }

    @Then("I should be able to get comments for API {string} in tenant {string}")
    public void i_should_get_comments(String apiId, String tenantDomain) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        HttpResponse response = store.getComments(actualApiId, tenantDomain, false, 10, 0);
        context.set("commentsResponse", response);
    }

    @Then("I should be able to get all tags")
    public void i_should_get_all_tags() throws Exception {
        TagListDTO tags = store.getAllTags();
        context.set("apiTags", tags);
    }

    @Then("I should be able to search APIs with query {string}")
    public void i_should_be_able_to_search_apis(String query) throws Exception {
        SearchResultListDTO results = store.searchAPIs(query);
        context.set("searchResults", results);
    }

    @Then("I should be able to get the API with id {string}")
    public void i_should_get_api(String apiId) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        APIDTO api = store.getAPI(actualApiId);
        context.set("apiDetails", api);
    }

    @When("I remove the comment with id {string} from API {string}")
    public void i_remove_comment(String commentId, String apiId) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        String actualCommentId = resolveFromContext(commentId);
        HttpResponse response = store.removeComment(actualCommentId,actualApiId);
        context.set("commentRemoval", response);
    }

    @When("I edit comment with id {string} on API {string} to {string}")
    public void i_edit_comment(String commentId, String apiId, String newContent) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        String actualCommentId = resolveFromContext(commentId);
        HttpResponse response = store.editComment(actualCommentId, actualApiId, newContent, "general");
        context.set("editedComment", response);
    }

    @Then("I clean up Application of Id {string}")
    public void i_clean_up_application(String appId) throws Exception {
        String actualAppId = resolveFromContext(appId);
        store.deleteApplication(actualAppId);
    }

    private String resolveFromContext(String input) {
        if (input.startsWith("<") && input.endsWith(">")) {
            return (String) context.get(input.substring(1, input.length() - 1));
        }
        return input;
    }

    private HttpResponse doTokenPost(String tokenUrl, String payload, Map<String, String> headers) throws IOException, IOException {
        URL url = new URL(tokenUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        headers.forEach(conn::setRequestProperty);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = payload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int statusCode = conn.getResponseCode();
        InputStream is = (statusCode >= 200 && statusCode < 300) ? conn.getInputStream() : conn.getErrorStream();
        String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        return new HttpResponse(response, statusCode);
    }

}
