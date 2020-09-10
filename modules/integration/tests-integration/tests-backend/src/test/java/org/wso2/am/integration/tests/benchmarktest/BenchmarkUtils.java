package org.wso2.am.integration.tests.benchmarktest;

import static io.restassured.RestAssured.given;
import static junit.framework.Assert.assertFalse;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.json.simple.parser.ParseException;
import org.testng.Assert;
import org.wso2.carbon.automation.engine.frameworkutils.enums.OperatingSystems;
import org.wso2.carbon.base.ServerConfiguration;

public class BenchmarkUtils {

    int statusCode;
    String publisherConsumerSecret;
    String publisherConsumerKey;
    String apiUUID;
    String applicationID;
    String accessToken;
    private static final String CARBON_XML_HOSTNAME = "HostName";
    private static final int PORT_OFFSET = 500;
    private static int apimport = 9443 + PORT_OFFSET;
    private static int gatewayport = 8243 + PORT_OFFSET;
    private static final String APIM_HOST = "192.168.1.5";
    public static String APIM_URL = "https://" + APIM_HOST + ':' + apimport;
    private static String TOKEN_URL = "https://" + APIM_HOST + ':' +gatewayport;
    private static String TARGET_URL = "https://" + APIM_HOST + ':' +gatewayport;
    private final String SUPER_TENANT_USERNAME = "admin";
    private final String SUPER_TENANT_PASSWORD = "admin";
    private static final String APIM_HOME = System.getProperty("carbon.home");;
    private static final String IN_FILE_PATH = APIM_HOME+"/repository/logs/correlation.log";
    private static final String OUT_FILE_PATH = "logs/";
    static String[] excludedLines = {"select um_id, um_domain_name, um_email, um_created_date, um_active from um_tenant order by um_id|jdbc:h2:./repository/database/wso2shared_db",
                                     "reg_path, reg_user_id, reg_logged_time, reg_action, reg_action_data from reg_log where reg_logged_time>? and reg_logged_time<? and reg_tenant_id=? order by reg_logged_time desc|jdbc:h2:./repository/database/"};
    private String apimHost;

    public void generateConsumerCredentialsAndAccessToken() {
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
        Assert.assertEquals(statusCode /*actual value*/,  HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
        publisherConsumerKey = jsonResponse.getString("clientId");
        publisherConsumerSecret = jsonResponse.getString("clientSecret");

        generateAccessToken("apim:api_create apim:api_delete apim:api_view apim:api_publish apim:subscribe");
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
//            Assert.assertEquals(statusCode /*actual value*/,  HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
        accessToken = jsonResponse.getString("access_token");
        return accessToken;
    }

    public String createRestAPI(String apiName, String apiContext, String activityID) {
        Response response =
            given()
                   .header("Authorization","Bearer "+ accessToken)
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
        Assert.assertEquals(statusCode /*actual value*/,  HttpStatus.SC_CREATED /*expected value*/, "Incorrect status code returned");
        apiUUID = jsonResponse.getString("id");
        return apiUUID;
    }

    public void deleteRestAPI(String apiID) {
        Response response =
            given()
                   .header("Authorization","Bearer "+ accessToken)
                   .header("Content-Type", "application/json").
                       when().
                       delete(APIM_URL+"/api/am/publisher/v1/apis/"+apiID);
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
//        Assert.assertEquals(statusCode /*actual value*/,  HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned when deleting API");
    }

    public void publishAPI(String apiID, String activityID) {
        Response response =
            given()
                   .header("Authorization","Bearer "+ accessToken)
                   .header("Content-Type", "application/json")
                   .header("activityid", activityID)
                   .queryParam("apiId",apiID)
                   .queryParam("action","Publish").
                       when().
                       post(APIM_URL+"/api/am/publisher/v1/apis/change-lifecycle");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/,  HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
    }

    public String createAnApplication(String appName, String activityID) {
        Response response =
            given()
                   .header("Authorization","Bearer "+accessToken)
                   .header("Content-Type", "application/json")
                   .header("activityid", activityID).
                       when().
                       body("{\"name\":\""+appName+"\",\"throttlingPolicy\":\"Unlimited\",\"description\":\"test\"," +
                            "\"tokenType\":\"JWT\",\"groups\":null,\"attributes\":{}}").
                       post(APIM_URL+"/api/am/store/v1/applications");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/,  HttpStatus.SC_CREATED /*expected value*/, "Incorrect status code returned");
        applicationID = jsonResponse.getString("applicationId");
        return applicationID;
    }

    public void addSubscription(String apiID, String appID,String activityID) {
        Response response =
            given()
                   .header("Authorization","Bearer "+ accessToken)
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
        Assert.assertEquals(statusCode /*actual value*/,  HttpStatus.SC_CREATED /*expected value*/, "Incorrect status code returned");
    }

    public String generateApplicationToken(String appID, String activityID) {
        Response response =
            given()
                   .header("Authorization","Bearer "+ accessToken)
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
        Assert.assertEquals(statusCode /*actual value*/,  HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
        String accessToken = jsonResponse.getString("token.accessToken");
        return accessToken;
    }

    public void deleteApplication(String appID) {
        Response response =
            given()
                   .header("Authorization","Bearer "+ accessToken)
                   .header("Content-Type", "application/json").
                       when().
                       delete(APIM_URL+"/api/am/store/v1/applications/"+appID);
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/,  HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned when deleting application");
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
        Assert.assertEquals(statusCode /*actual value*/,  HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
    }

    public void retrieveAllApisFromPublisher(int limit, String activityID) {
        Response response =
            given().log().all()
                   .header("Authorization","Bearer "+ accessToken)
                   .header("Content-Type", "application/json")
                   .header("activityid", activityID).
                       when().
                       get(APIM_URL+"/api/am/publisher/v1/apis?limit="+limit+"&offset=0");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        response.then().log().all();
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/,  HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
    }

    public void retrieveAnApiFromPublisher(String activityID, String apiID) {
        Response response =
            given().log().all()
                   .header("Authorization","Bearer "+ accessToken)
                   .header("Content-Type", "application/json")
                   .header("activityid", activityID).
                       when().
                       get(APIM_URL+"/api/am/publisher/v1/apis/"+apiID);
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        response.then().log().all();
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/,  HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
    }

    public void retrieveAnApiFromStore(String activityID, String apiID) {
        Response response =
            given().log().all()
                   .header("Authorization","Bearer "+ accessToken)
                   .header("Content-Type", "application/json")
                   .header("activityid", activityID).
                       when().
                       get(APIM_URL+"/api/am/store/v1/apis/"+apiID);
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        response.then().log().all();
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/,  HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
    }

    public int getDevPortalApiCount() {
        Response response =
            given()
                   .header("Authorization","Bearer "+ accessToken)
                   .header("Content-Type", "application/json").
                       when().
                       get(APIM_URL+"/api/am/store/v1/apis?limit=100&offset=0");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/,  HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
        int count = Integer.parseInt(jsonResponse.getString("count"));
        return count;
    }

    public int getPublisherApiCount() {
        Response response =
            given()
                .header("Authorization","Bearer "+ accessToken)
                .header("Content-Type", "application/json").
                    when().
                    get(APIM_URL+"/api/am/publisher/v1/apis?limit=100&offset=0");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/,  HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
        int count = Integer.parseInt(jsonResponse.getString("count"));
        return count;
    }

    public void retrieveAllApisFromStore(int limit, String activityID) {
        Response response =
            given().log().all()
                   .header("Authorization","Bearer "+ accessToken)
                   .header("Content-Type", "application/json")
                   .header("activityid", activityID).
                       when().
                       get(APIM_URL+"/api/am/store/v1/apis?limit="+limit+"&offset=0");
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        response.then().log().all();
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/,  HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
    }

    public void retrieveAnAPI(String apiID) {
        Response response =
            given().log().all()
                   .header("Authorization","Bearer "+ accessToken)
                   .header("Content-Type", "application/json").
                       when().
                       get(APIM_URL+"/api/am/publisher/v1/apis/"+apiID);
        String responseBody = response.getBody().asString();
        JsonPath jsonResponse = new JsonPath(responseBody);
        response.then().log().all();
        statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode /*actual value*/,  HttpStatus.SC_OK /*expected value*/, "Incorrect status code returned");
    }

    public static String setActivityID(){
        Date date = new Date();
        String CorellationID = "log_test_"+ (new Timestamp(date.getTime()))+"_";
        return CorellationID;
    }

    public static int extractCountsFromLog(String logFile, String testType , LocalTime startTime)
        throws InterruptedException {
        String logAttribute = null;

        Thread.sleep(4000);
        if (testType=="http"){
            logAttribute = "http-in-request";
        } else {
            logAttribute = testType;
        }
        int noOfLinesExecuted = 0;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(IN_FILE_PATH));
//             lines executed are logged in to a file starting with test method name,

             Writer writer = new BufferedWriter(
                 new OutputStreamWriter(new FileOutputStream(OUT_FILE_PATH + logFile +"_"+testType+ ".log"), "utf-8"))
        ) {
            String readLine;
            while ((readLine = bufferedReader.readLine()) != null) {
                String logLine = readLine.toLowerCase();
//                 lines executed will be captured with "jdbc"
                if (logLine.contains("|"+logAttribute+"|") ){
                    String regex = "(\\d{2}:\\d{2}:\\d{2})";
                    Matcher m = Pattern.compile(regex).matcher(logLine);
                    if (m.find()) {
                        LocalTime logtime = LocalTime.parse(m.group(1));
                        if (logtime.isAfter(startTime)) {
                            logLine.substring(logLine.indexOf("|"+logAttribute+"|") + 3, logLine.length());
                            if (!logLine.contains(excludedLines[0]) && !logLine.contains(excludedLines[1])) {
                                noOfLinesExecuted++;
                                writer.write(logLine);
                                writer.write(System.lineSeparator());
                            }
                        }
                    }
                }
                }

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("NO. of statements executed : "+noOfLinesExecuted);
        return noOfLinesExecuted;
    }

    public static LocalTime getCurrentTimeStamp() throws InterruptedException {
        Thread.sleep( 15000);
        LocalDateTime ldt = LocalDateTime.now();
        LocalTime currentTime = LocalTime.parse(DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH).format(ldt));
        Thread.sleep(1000);
        return currentTime;
    }

    public static void validateBenchmark(int benchmark, int actualCount) {
        boolean exceedsLimit = false;
//        Validate if No. of statements executed exceeds the benchmark
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
        String resourceLocation = getSystemResourceLocation() + "benchmark-values" + File.separator + "benchmark-values-"+testType+".json";

        JSONParser parser = new JSONParser();
        JSONArray a = (JSONArray) parser.parse(new FileReader(resourceLocation));

        for (Object o : a)
        {
            JSONObject values = (JSONObject) o;
             benchmarkValue = (String) values.get(scenario);
        }
        return Integer.parseInt(benchmarkValue);
    }

    public String getApimURL() throws IOException {
        String carbonLog = APIM_HOME + "/repository/logs/wso2carbon.log";
        String url = null;
        String host = null;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(carbonLog));
//             lines executed are logged in to a file starting with test method name,
        {
            String readLine;
            while ((readLine = bufferedReader.readLine()) != null) {
                String logLine = readLine.toLowerCase();
                if (logLine.contains("api publisher default context :")) {

                    url = logLine.substring(logLine.indexOf("https://") + 8 , logLine.length());
                    apimHost = url.substring(0, url.indexOf(':'));
                    System.out.println("url is "+apimHost);
                }
            }
        }
        return apimHost;
    }

    public static void writeResultsToFile(String fileName, String testName, int actual, int benchmark) throws IOException {
        String outputFile = OUT_FILE_PATH + "Results_"+fileName+ ".log";
        File f = new File(outputFile);
        if(!f.exists()){
            f.createNewFile();
        }
            System.out.println(testName +" ===== Actual count is "+actual+ "   Benchmark is  " + benchmark);

        BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
        bw.append(testName +" ===== Actual count is "+actual+ "   Benchmark is  " + benchmark);
        bw.newLine();
        bw.close();
    }


}