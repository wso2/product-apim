/*
 *Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.am.integration.tests.benchmarktest;

import static io.restassured.RestAssured.given;
import static junit.framework.Assert.assertFalse;

import io.restassured.RestAssured;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.json.simple.parser.ParseException;
import org.testng.Assert;
import org.wso2.carbon.automation.engine.frameworkutils.enums.OperatingSystems;

public class BenchmarkUtils {

    private static final int PORT_OFFSET = 500;
    private static final String APIM_URL_SYSTEM_PROPERTY = "apim.url";
    private static final String APIM_HOME = System.getProperty("carbon.home");
    private static final String IN_FILE_PATH = APIM_HOME + "/repository/logs/correlation.log";
    private static final String OUT_FILE_PATH = "logs/benchmark-tests/";
    public static String apimUrl;
    static String[] excludedLines = {"select um_id, um_domain_name, um_email, um_created_date, um_active from um_tenant order by um_id|jdbc:h2:./repository/database/wso2shared_db",
            "select reg_path, reg_user_id, reg_logged_time, reg_action, reg_action_data from reg_log where reg_logged_time>? and reg_logged_time<? and reg_tenant_id=? order by reg_logged_time desc|jdbc:h2:./repository/database/"};
    public static int apimPort = 9443 + PORT_OFFSET;
    private static int gatewayport = 8243 + PORT_OFFSET;
    private static String gateway_Url;
    private static final String RESTFUL_API_VERSION = "v1";
    private static final String SUPER_TENANT = "superTenant";
    private static final String HTTP_PROTOCOL = "https://";
    private static int statusCode;
    private static String publisherConsumerSecret;
    private static String publisherConsumerKey;
    private static String apiUUID;
    private static String applicationID;
    private static String accessToken;
    public static String tenant;

    public static String setActivityID() {

        Date date = new Date();
        String CorellationID = "benchmark_correlationID_" + (new Timestamp(date.getTime())) + "_";
        return CorellationID;
    }

    public static int extractCountsFromLog(String logFile, String testType, LocalTime startTime)
            throws InterruptedException {
        String tenantName;
        if(tenant!=SUPER_TENANT){
            tenantName = tenant.substring(0, tenant.indexOf('.'));
        }
        else {  tenantName=tenant;
        }
        String logAttribute = null;
        File directory = new File(OUT_FILE_PATH);
        if (!directory.exists()) {
            directory.mkdir();
        }
        Thread.sleep(4000);
        if (testType == "http") {
            logAttribute = "http-in-request";
        } else {
            logAttribute = testType;
        }
        int noOfLinesExecuted = 0;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(IN_FILE_PATH));
    // lines executed are logged in to a file starting with test method name,
             Writer writer = new BufferedWriter(
                     new OutputStreamWriter(new FileOutputStream(OUT_FILE_PATH + logFile + "_" + testType +"_"+tenantName+".log"), "utf-8"))
        ) {
            String readLine;
            while ((readLine = bufferedReader.readLine()) != null) {
                String logLine = readLine.toLowerCase();
    // lines executed will be captured with log Attribute
                if (logLine.contains("|" + logAttribute + "|")) {
                    String regex = "(\\d{2}:\\d{2}:\\d{2})";
                    Matcher matcher = Pattern.compile(regex).matcher(logLine);
                    if (matcher.find()) {
                        LocalTime logtime = LocalTime.parse(matcher.group(1));
                        if (logtime.isAfter(startTime)) {
                            logLine.substring(logLine.indexOf("|" + logAttribute + "|") + 3, logLine.length());
                            if (!logLine.contains(excludedLines[0]) && !logLine.contains(excludedLines[1])) {
                                noOfLinesExecuted++;
                                writer.write(logLine);
                                writer.write(System.lineSeparator());
                                if (logLine.contains("slotdeletionexecutor")) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return noOfLinesExecuted;
    }

    public static LocalTime getCurrentTimeStamp() throws InterruptedException {

        Thread.sleep(15000);
        LocalDateTime ldt = LocalDateTime.now();
        LocalTime currentTime = LocalTime.parse(DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH).format(ldt));
        Thread.sleep(1000);
        return currentTime;
    }

    public static void validateBenchmark(int benchmark, int actualCount) {

        boolean exceedsLimit = false;

        benchmark = (int) (benchmark + (benchmark * 0.01));
    // Validate if No. of statements executed exceeds the benchmark
        if (actualCount > benchmark) {
            exceedsLimit = true;
        }
        assertFalse(
                "Exceeds limit! " + actualCount + " were Executed, but the Benchmark value is " + benchmark,
                exceedsLimit);
    }

    public static String getSystemResourceLocation() {

        String resourceLocation;
        if (System.getProperty("os.name").toLowerCase().contains(OperatingSystems.WINDOWS.name().toLowerCase())) {
            resourceLocation = System.getProperty("framework.resource.location").replace("/", "\\");
        } else {
            resourceLocation = System.getProperty("framework.resource.location").replace("/", "/");
        }

        return resourceLocation;
    }

    public static int getBenchmark(String testType, String scenario) throws IOException, ParseException {

        String benchmarkValue = null;
        String resourceLocation;
        String tenantName;
        if(tenant!=SUPER_TENANT){
            tenantName = "tenant";
        }
        else {  tenantName=tenant;
        }
        resourceLocation = getSystemResourceLocation() + "benchmark-values" + File.separator + "benchmark-values-" + testType +"-"+ tenantName +".json";
        JSONParser parser = new JSONParser();
        JSONArray a = (JSONArray) parser.parse(new FileReader(resourceLocation));

        for (Object o : a) {
            JSONObject values = (JSONObject) o;
            benchmarkValue = (String) values.get(scenario);
        }
        return Integer.parseInt(benchmarkValue);
    }

    public static void writeResultsToFile(String fileName, String testName, int actual, int benchmark) throws IOException {
        String tenantName;
        if(tenant!=SUPER_TENANT){
            tenantName = tenant.substring(0, tenant.indexOf('.'));
        }
        else {  tenantName= tenant;
        }
        String outputFile = OUT_FILE_PATH + "Results_" + fileName + ".log";
        File f = new File(outputFile);
        if (!f.exists()) {
            f.createNewFile();
        }
        BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
        bw.append(testName +"  :  "+ tenantName +"  :  Actual count is " + actual + "   Benchmark is  " + benchmark);
        bw.newLine();
        bw.close();
    }

    public static void generateConsumerCredentialsAndAccessToken(String userName, String password) throws IOException {
        setTenancy(userName);
        if(System.getProperty(APIM_URL_SYSTEM_PROPERTY) == null){
            System.setProperty(APIM_URL_SYSTEM_PROPERTY, getApimURL());
        }
        apimUrl = HTTP_PROTOCOL + System.getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + apimPort;
        Response response =
                given()
                        .auth()
                        .preemptive()
                        .basic(userName, password)
                        .header("Content-Type", "application/json").
                        when().
                        body("{\n" +
                                "  \"callbackUrl\": \"www.google.lk\",\n" +
                                "  \"clientName\": \"rest_api_publisher\",\n" +
                                "  \"owner\": \""+userName+"\",\n" +
                                "  \"grantType\": \"password refresh_token\",\n" +
                                "  \"saasApp\": true\n" +
                                "}").
                        post(apimUrl + "/client-registration/v0.17/register");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
        publisherConsumerKey = jsonResponse.getString("clientId");
        publisherConsumerSecret = jsonResponse.getString("clientSecret");

        generateAccessToken("apim:api_create apim:api_delete apim:api_view apim:api_publish apim:subscribe",userName,password);
    }

    public static void setTenancy(String userName){
    if (userName.contains("@")){
    tenant = userName.substring(userName.lastIndexOf("@") + 1);
    } else {
    tenant = SUPER_TENANT;
    }
    }

    public static String generateAccessToken(String scope, String userName, String password) {

        gateway_Url = HTTP_PROTOCOL + System.getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + gatewayport;
        Response response =
                given()
                        .auth()
                        .preemptive()
                        .basic(publisherConsumerKey, publisherConsumerSecret)
                        .formParam("grant_type", "password")
                        .formParam("username", userName)
                        .formParam("password", password)
                        .formParam("scope", scope).
                        when().
                        post(gateway_Url + "/token");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
    Assert.assertEquals(statusCode /*actual value*/,  HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
        accessToken = jsonResponse.getString("access_token");
        return accessToken;
    }

    public static String createRestAPI(String apiName, String apiContext, String activityID) {

        apimUrl = HTTP_PROTOCOL + System.getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + apimPort;
        Response response =
                given()
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .header("activityid", activityID).
                        when().
                        body("{\"name\":\"" + apiName + "\",\"version\":\"v1.0\",\"context\":\"" + apiContext + "\"," +
                                "\"policies\":[\"Gold\"],\"endpointConfig\":{\"endpoint_type\":\"http\"," +
                                "\"sandbox_endpoints\":{\"url\":\"https://jsonplaceholder.typicode.com/\"}," +
                                "\"production_endpoints\":{\"url\":\"https://jsonplaceholder.typicode.com/\"}}," +
                                "\"gatewayEnvironments\":[\"Production and Sandbox\"]}").
                        post(apimUrl + "/api/am/publisher/" + RESTFUL_API_VERSION + "/apis");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, HttpStatus.SC_CREATED /*expected value*/, "Incorrect status code returned");
        apiUUID = jsonResponse.getString("id");
        return apiUUID;
    }

    public static void deleteRestAPI(String apiID) {

        apimUrl = HTTP_PROTOCOL + System.getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + apimPort;
        Response response =
                given()
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json").
                        when().
                        delete(apimUrl + "/api/am/publisher/" + RESTFUL_API_VERSION + "/apis/" + apiID);
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
    }

    public static void publishAPI(String apiID, String activityID) {

        apimUrl = HTTP_PROTOCOL + System.getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + apimPort;
        Response response =
                given()
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .header("activityid", activityID)
                        .queryParam("action", "Publish")
                        .queryParam("apiId", apiID).
                        when().
                        post(apimUrl + "/api/am/publisher/" + RESTFUL_API_VERSION + "/apis/change-lifecycle");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
    }

    public static String createAnApplication(String appName, String activityID) {

        apimUrl = HTTP_PROTOCOL + System.getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + apimPort;
        Response response =
                given()
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .header("activityid", activityID).
                        when().
                        body("{\"name\":\"" + appName + "\",\"throttlingPolicy\":\"Unlimited\",\"description\":\"test\"," +
                                "\"tokenType\":\"JWT\",\"groups\":null,\"attributes\":{}}").
                        post(apimUrl + "/api/am/store/" + RESTFUL_API_VERSION + "/applications");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, HttpStatus.SC_CREATED /*expected value*/, "Incorrect status code returned");
        applicationID = jsonResponse.getString("applicationId");
        return applicationID;
    }

    public static void addSubscription(String apiID, String appID, String activityID) {

        apimUrl = HTTP_PROTOCOL + System.getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + apimPort;
        Response response =
                given()
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .header("activityid", activityID).
                        when().
                        body("{\"throttlingPolicy\":\"Gold\",\n" +
                                "    \"apiId\": \"" + apiID + "\",\n" +
                                "    \"applicationId\": \"" + appID + "\"}").
                        post(apimUrl + "/api/am/store/" + RESTFUL_API_VERSION + "/subscriptions");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, HttpStatus.SC_CREATED /*expected value*/, "Incorrect status code returned");
    }

    public static String generateApplicationToken(String appID, String activityID) {

        apimUrl = HTTP_PROTOCOL + System.getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + apimPort;
        Response response =
                given()
                        .header("Authorization", "Bearer " + accessToken)
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
                        post(apimUrl + "/api/am/store/" + RESTFUL_API_VERSION + "/applications/" + appID + "/generate-keys");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
        String accessToken = jsonResponse.getString("token.accessToken");
        return accessToken;
    }

    public static void deleteApplication(String appID) {

        apimUrl = HTTP_PROTOCOL + System.getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + apimPort;
        Response response =
                given()
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json").
                        when().
                        delete(apimUrl + "/api/am/store/" + RESTFUL_API_VERSION + "/applications/" + appID);
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned when deleting application");
    }

    public static void invokeAPI(String context, String token, String activityID) {

        gateway_Url = HTTP_PROTOCOL + System.getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + gatewayport;
        String urlPath = gateway_Url + "/" + context + "/v1.0/posts/1";
        if (tenant!= SUPER_TENANT){
            urlPath = gateway_Url + "/t/"+tenant +"/"+ context + "/v1.0/posts/1";
        }
        System.out.println("Tenant is :   "+urlPath);
        Response response =
                given()
                        .header("Authorization", "Bearer " + token)
                        .header("activityid", activityID).
                        when().
                        get(urlPath);
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
    }

    public static void retrieveAllApisFromPublisher(int limit, String activityID) {

        apimUrl = HTTP_PROTOCOL + System.getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + apimPort;
        Response response =
                given()
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .header("activityid", activityID).
                        when().
                        get(apimUrl + "/api/am/publisher/" + RESTFUL_API_VERSION + "/apis?limit=" + limit + "&offset=0");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
    }

    public static void retrieveApiFromPublisher(String activityID, String apiID) {

        apimUrl = HTTP_PROTOCOL + System.getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + apimPort;
        Response response =
                given()
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .header("activityid", activityID).
                        when().
                        get(apimUrl + "/api/am/publisher/" + RESTFUL_API_VERSION + "/apis/" + apiID);
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
    }

    public static void retrieveApiFromStore(String activityID, String apiID) {

        apimUrl = HTTP_PROTOCOL + System.getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + apimPort;
        Response response =
                given()
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .header("activityid", activityID).
                        when().
                        get(apimUrl + "/api/am/store/" + RESTFUL_API_VERSION + "/apis/" + apiID);
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
    }

    public static int getDevPortalApiCount() {

        apimUrl = HTTP_PROTOCOL + System.getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + apimPort;
        Response response =
                given()
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json").
                        when().
                        get(apimUrl + "/api/am/store/" + RESTFUL_API_VERSION + "/apis?limit=100&offset=0");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
        int count = Integer.parseInt(jsonResponse.getString("count"));
        return count;
    }

    public static int getPublisherApiCount() {

        apimUrl = HTTP_PROTOCOL + System.getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + apimPort;
        Response response =
                given()
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json").
                        when().
                        get(apimUrl + "/api/am/publisher/" + RESTFUL_API_VERSION + "/apis?limit=100&offset=0");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
        int count = Integer.parseInt(jsonResponse.getString("count"));
        return count;
    }

    public static void retrieveAllApisFromStore(int limit, String activityID) {

        apimUrl = HTTP_PROTOCOL + System.getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + apimPort;
        Response response =
                given()
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .header("activityid", activityID).
                        when().
                        get(apimUrl + "/api/am/store/" + RESTFUL_API_VERSION + "/apis?limit=" + limit + "&offset=0");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
    }

    public static void retrieveAnAPI(String apiID) {

        apimUrl = HTTP_PROTOCOL + System.getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + apimPort;
        gateway_Url = HTTP_PROTOCOL + System.getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + gatewayport;
        Response response =
                given()
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json").
                        when().
                        get(apimUrl + "/api/am/publisher/" + RESTFUL_API_VERSION + "/apis/" + apiID);
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/, HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
    }

    public static String getApimURL() throws IOException {

        String carbonLog = APIM_HOME + "/repository/logs/wso2carbon.log";
        String url = null;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(carbonLog));
    // lines executed are logged in to a file starting with test method name,
        String apimHost = null;
        {
            String readLine;
            while ((readLine = bufferedReader.readLine()) != null) {
                String logLine = readLine.toLowerCase();
                if (logLine.contains("api publisher default context :")) {

                    url = logLine.substring(logLine.indexOf(HTTP_PROTOCOL) + 8, logLine.length());
                    apimHost = url.substring(0, url.indexOf(':'));
                }
            }
        }
        return apimHost;
    }

    public static void validateBenchmarkResults(String testName, String testType, LocalTime startTime, int benchmark) throws InterruptedException, IOException {

        int actualCount = extractCountsFromLog(testName, testType, startTime);
        writeResultsToFile(testType, testName, actualCount, benchmark);
        validateBenchmark(benchmark, actualCount);
    }

    public static void enableRestassuredHttpLogs(boolean isEnabled) {

        if (isEnabled == true) {
            RestAssured.useRelaxedHTTPSValidation();
        }
    }

}