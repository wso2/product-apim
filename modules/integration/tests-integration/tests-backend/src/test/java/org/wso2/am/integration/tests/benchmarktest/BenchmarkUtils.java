package org.wso2.am.integration.tests.benchmarktest;

import static io.restassured.RestAssured.given;
import static junit.framework.Assert.assertFalse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.Date;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;

public class BenchmarkUtils {
    int statusCode;
    String publisherConsumerSecret;
    String publisherConsumerKey;
    String apiUUID;
    String applicationID;
    String accessToken;
    private final String APIM_URL = "https://localhost:9443";
    private final String TOKEN_URL = "https://localhost:8243";
    private final String TARGET_URL = "https://localhost:8243";
    private final String SUPER_TENANT_USERNAME = "admin";
    private final String SUPER_TENANT_PASSWORD = "admin";
    private static final String APIM_HOME = "/Users/nironsmac/Documents/Migration/packs/wso2am-3.2.0-SNAPSHOT";
    private static final String IN_FILE_PATH = APIM_HOME+"/repository/logs/correlation.log";
    private static final String OUT_FILE_PATH = "logs/";

    public void generateConsumerCredentials() {
        Response response =
            given()
                .auth()
                .preemptive()
                .basic(SUPER_TENANT_USERNAME, SUPER_TENANT_PASSWORD)
                .header("Content-Type", "application/json").
                    when().
                    body("{\n" +
                         "  \"callbackUrl\": \"www.google.lk\",\n" +
                         "  \"clientName\": \"rest_api_publisher\",\n" +
                         "  \"owner\": \"admin\",\n" +
                         "  \"grantType\": \"password refresh_token\",\n" +
                         "  \"saasApp\": true\n" +
                         "}").
                    post(APIM_URL+"/client-registration/v0.17/register");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, 200 /*expected value*/, "Incorrect status code returned");
        publisherConsumerKey = jsonResponse.getString("clientId");
        publisherConsumerSecret = jsonResponse.getString("clientSecret");
    }

    public String generateAccessToken(String scope) {
        Response response =
            given()
                .auth()
                .preemptive()
                .basic(publisherConsumerKey, publisherConsumerSecret)
                .formParam("grant_type","password")
                .formParam("username",SUPER_TENANT_USERNAME)
                .formParam("password",SUPER_TENANT_PASSWORD)
                .formParam("scope",scope).
                    when().
                    post(TOKEN_URL+"/token");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
//            Assert.assertEquals(statusCode /*actual value*/, 200 /*expected value*/, "Incorrect status code returned");
        String accessToken = jsonResponse.getString("access_token");
        return accessToken;
    }

    public String createRestAPI(String apiName, String apiContext, String activityID) {
        Response response =
            given()
                   .header("Authorization","Bearer "+ generateAccessToken("apim:api_create"))
                   .header("Content-Type", "application/json")
                   .header("activityid", activityID).
                       when().
                       body("{\"name\":\""+apiName+"\",\"version\":\"v1.0\",\"context\":\""+apiContext+"\"," +
                            "\"policies\":[\"Gold\"],\"endpointConfig\":{\"endpoint_type\":\"http\"," +
                            "\"sandbox_endpoints\":{\"url\":\"https://jsonplaceholder.typicode.com/\"}," +
                            "\"production_endpoints\":{\"url\":\"https://jsonplaceholder.typicode.com/\"}}," +
                            "\"gatewayEnvironments\":[\"Production and Sandbox\"]}").
                       post(APIM_URL+"/api/am/publisher/v1/apis");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, 201 /*expected value*/, "Incorrect status code returned");
        apiUUID = jsonResponse.getString("id");
        return apiUUID;
    }

    public void deleteRestAPI(String apiID) {
        Response response =
            given()
                   .header("Authorization","Bearer "+ generateAccessToken("apim:api_delete"))
                   .header("Content-Type", "application/json").
                       when().
                       delete(APIM_URL+"/api/am/publisher/v1/apis/"+apiID);
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, 200 /*expected value*/, "Incorrect status code returned when deleting API");
    }

    public void publishAPI(String apiID, String activityID) {
        Response response =
            given()
                   .header("Authorization","Bearer "+ generateAccessToken("apim:api_publish"))
                   .header("Content-Type", "application/json")
                   .header("activityid", activityID)
                   .queryParam("apiId",apiID)
                   .queryParam("action","Publish").
                       when().
                       post(APIM_URL+"/api/am/publisher/v1/apis/change-lifecycle");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, 200 /*expected value*/, "Incorrect status code returned");
    }

    public String createAnApplication(String appName, String activityID) {
        Response response =
            given()
                   .header("Authorization","Bearer "+ generateAccessToken("apim:subscribe"))
                   .header("Content-Type", "application/json")
                   .header("activityid", activityID).
                       when().
                       body("{\"name\":\""+appName+"\",\"throttlingPolicy\":\"Unlimited\",\"description\":\"test\"," +
                            "\"tokenType\":\"JWT\",\"groups\":null,\"attributes\":{}}").
                       post(APIM_URL+"/api/am/store/v1/applications");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, 201 /*expected value*/, "Incorrect status code returned");
        applicationID = jsonResponse.getString("applicationId");
        return applicationID;
    }

    public void addSubscription(String apiID, String appID,String activityID) {
        Response response =
            given()
                   .header("Authorization","Bearer "+ generateAccessToken("apim:subscribe"))
                   .header("Content-Type", "application/json")
                   .header("activityid", activityID).
                       when().
                       body("{\"throttlingPolicy\":\"Gold\",\n" +
                            "    \"apiId\": \""+apiID+"\",\n" +
                            "    \"applicationId\": \""+appID+"\"}").
                       post(APIM_URL+"/api/am/store/v1/subscriptions");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, 201 /*expected value*/, "Incorrect status code returned");
    }

    public String generateApplicationToken(String appID, String activityID) {
        Response response =
            given()
                   .header("Authorization","Bearer "+ generateAccessToken("apim:subscribe"))
                   .header("Content-Type", "application/json")
                   .header("activityid", activityID).
                       when().
                       body("{\n" +
                            "  \"keyType\": \"PRODUCTION\",\n" +
                            "  \"grantTypesToBeSupported\": [\n" +
                            "    \"refresh_token\",\"urn:ietf:params:oauth:grant-type:saml2-bearer\"," +
                            "\"password\",\"client_credentials\",\"iwa:ntlm\"," +
                            "\"urn:ietf:params:oauth:grant-type:jwt-bearer\"\n" +
                            "  ],\n" +
                            "  \"callbackUrl\": \"string\",  \"scopes\": [\"am_application_scope\",\"default\"]," +
                            "\"validityTime\": 3600,\"clientId\": \"\",\"clientSecret\": \"\"," +
                            "\"additionalProperties\": \"\"\n" +
                            "}").
                       post(APIM_URL+"/api/am/store/v1/applications/"+appID+"/generate-keys");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, 200 /*expected value*/, "Incorrect status code returned");
        String accessToken = jsonResponse.getString("token.accessToken");
        return accessToken;
    }

    public void deleteApplication(String appID) {
        Response response =
            given()
                   .header("Authorization","Bearer "+ generateAccessToken("apim:subscribe"))
                   .header("Content-Type", "application/json").
                       when().
                       delete(APIM_URL+"/api/am/store/v1/applications/"+appID);
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, 200 /*expected value*/, "Incorrect status code returned when deleting application");
    }

    public void invokeAPI(String context, String token, String activityID) {
        Response response =
            given()
                   .header("Authorization","Bearer "+ token)
                   .header("activityid", activityID).
                       when().
                       get(TARGET_URL+"/"+context+"/v1.0/posts/1");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, 200 /*expected value*/, "Incorrect status code returned");
    }

    public void retrieveAllApis(int limit, String activityID) {
        Response response =
            given().log().all()
                   .header("Authorization","Bearer "+ generateAccessToken("apim:api_view"))
                   .header("Content-Type", "application/json")
                   .header("activityid", activityID).
                       when().
                       get(APIM_URL+"/api/am/publisher/v1/apis?limit="+limit+"&offset=0");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        response.then().log().all();
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, 200 /*expected value*/, "Incorrect status code returned");
    }

    public int getDevPortalApiCount() {
        Response response =
            given()
                   .header("Authorization","Bearer "+ generateAccessToken("apim:api_view"))
                   .header("Content-Type", "application/json").
                       when().
                       get(APIM_URL+"/api/am/store/v1/apis?limit=100&offset=0");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, 200 /*expected value*/, "Incorrect status code returned");
        int count = Integer.parseInt(jsonResponse.getString("count"));
        return count;
    }

    public int getPublisherApiCount() {
        Response response =
            given()
                .header("Authorization","Bearer "+ generateAccessToken("apim:api_view"))
                .header("Content-Type", "application/json").
                    when().
                    get(APIM_URL+"/api/am/publisher/v1/apis?limit=100&offset=0");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, 200 /*expected value*/, "Incorrect status code returned");
        int count = Integer.parseInt(jsonResponse.getString("count"));
        return count;
    }

    public void retrieveAllApisFromStore(int limit, String activityID) {
        Response response =
            given().log().all()
                   .header("Authorization","Bearer "+ generateAccessToken("apim:api_view"))
                   .header("Content-Type", "application/json")
                   .header("activityid", activityID).
                       when().
                       get(APIM_URL+"/api/am/store/v1/apis?limit="+limit+"&offset=0");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        response.then().log().all();
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, 200 /*expected value*/, "Incorrect status code returned");
    }

    public void retrieveAnAPI(String apiID) {
        Response response =
            given().log().all()
                   .header("Authorization","Bearer "+ generateAccessToken("apim:api_view"))
                   .header("Content-Type", "application/json").
                       when().
                       get(APIM_URL+"/api/am/publisher/v1/apis/"+apiID);
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        response.then().log().all();
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, 200 /*expected value*/, "Incorrect status code returned");
    }

    public static String setActivityID(){
        Date date = new Date();
        String CorellationID = "jdbc_log_test"+ (new Timestamp(date.getTime()));
        return CorellationID;
    }

    public static int extractSqlQueries(String logFile, String corellationID, String logAttribute) {
        int noOfSqlExecuted = 0;

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(IN_FILE_PATH));
//             SQL lines executed are logged in to a file starting with test method name,
             Writer writer = new BufferedWriter(
                 new OutputStreamWriter(new FileOutputStream(OUT_FILE_PATH + logFile + ".log"), "utf-8"))
        ) {
            String readLine;
            while ((readLine = bufferedReader.readLine()) != null) {
                String logLine = readLine.toLowerCase();
//                SQL lines executed will be captured with "jdbc" and corellationID statement
                if (logLine.contains("|"+corellationID+"|") && logLine.contains("|"+logAttribute+"|") ) {
                    noOfSqlExecuted++;
                    writer.write(logLine);
                    writer.write(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("NO. of SQLS : "+noOfSqlExecuted);
        return noOfSqlExecuted;
    }

    public static int extractAPICalls(String logFile, String corellationID, String logAttribute) {
        int noOfSqlExecuted = 0;

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(IN_FILE_PATH));
//             SQL lines executed are logged in to a file starting with test method name,
             Writer writer = new BufferedWriter(
                 new OutputStreamWriter(new FileOutputStream(OUT_FILE_PATH + logFile + ".log"), "utf-8"))
        ) {
            String readLine;
            while ((readLine = bufferedReader.readLine()) != null) {
                String logLine = readLine.toLowerCase();
//                SQL lines executed will be captured with "jdbc" and corellationID statement
                if (logLine.contains("|"+corellationID+"|") && logLine.contains("|"+logAttribute+"|") ) {
                    noOfSqlExecuted++;
                    writer.write(logLine);
                    writer.write(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("NO. of SQLS : "+noOfSqlExecuted);
        return noOfSqlExecuted;
    }

    public static void validateBenchmark(int benchmark, int actualCount) {
        boolean exceedsLimit = false;
//        Validate if No. of sql statements executed exceeds the benchmark
        if (actualCount > benchmark) {
            exceedsLimit = true;
        }
        assertFalse(
            "Exceeds limit! SQL queries " + actualCount + " were Executed, but the Benchmark value is " + benchmark,
            exceedsLimit);
    }

}
