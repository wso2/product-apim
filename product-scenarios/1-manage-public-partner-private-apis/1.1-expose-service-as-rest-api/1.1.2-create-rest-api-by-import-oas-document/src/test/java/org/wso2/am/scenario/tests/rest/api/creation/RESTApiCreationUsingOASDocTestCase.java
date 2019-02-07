/*
* Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.wso2.am.scenario.tests.rest.api.creation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.httpserver.SimpleHTTPServer;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;

import static org.apache.axis2.transport.http.HTTPConstants.USER_AGENT;

public class RESTApiCreationUsingOASDocTestCase extends ScenarioTestBase {

    private APIPublisherRestClient apiPublisher;
    private APIRequest apiRequest;
    private String import_definition_url = "swagger-url";
    private String import_definition_file = "swagger-file";
    private File swagger_file;
    private String swagger_url;
    private String type = "rest";
    private String apiName;
    private String apiVersion;
    private String apiContext;

    String resourceLocation = System.getProperty("test.resource.location");

    private static final Log log = LogFactory.getLog(RESTApiCreationUsingOASDocTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {

        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiPublisher.login("admin", "admin");
    }

    @Test(description = "1.1.2.1")
    public void createApiWithValidOAS2DocumentAsJSONFile() throws Exception {
        swagger_file = new File(resourceLocation + File.separator + "swaggerFiles/OAS2Document.json");

        //Import api definition from swagger file
        apiRequest = new APIRequest(import_definition_file, swagger_file.getAbsolutePath(), "", type);
        HttpResponse designResponse1 = apiPublisher.designAPIWithOAS(apiRequest);
        Assert.assertTrue(designResponse1.getData().contains("imported"));

        //Save the API with imported values
        String payload = readFromFile(swagger_file.getAbsolutePath());
        JSONObject json = new JSONObject(payload);
        apiName = json.getJSONObject("info").get("title").toString();
        apiContext = json.get("basePath").toString();
        apiVersion = json.getJSONObject("info").get("version").toString();

        apiRequest = new APIRequest(apiName, apiContext, apiVersion);
        apiRequest.setSwagger(payload);
        HttpResponse designResponse2 = apiPublisher.designAPI(apiRequest);
        verifyResponse(designResponse2);

        //Verify the API created with correct name, version and resources
        String name = (new JSONObject(designResponse2.getData())).getJSONObject("data").get("apiName").toString();
        String version = (new JSONObject(designResponse2.getData())).getJSONObject("data").get("version").toString();
        Assert.assertEquals(name, apiName, "Api name was not imported correctly");
        Assert.assertEquals(version, apiVersion, "Api version was not imported correctly");

        HttpResponse getResponse = apiPublisher.getAPI(apiName, "admin", apiVersion);
        String resource = (new JSONObject(getResponse.getData())).getJSONObject("api").get("resources").toString();
        Assert.assertTrue(resource != "null", "API resource was not imported correctly");

        //Assert resources
        assertGETResource(((JSONArray) (new JSONObject(getResponse.getData())).getJSONObject("api").
                get("templates")).get(0));

    }

    @Test(description = "1.1.2.2", dependsOnMethods = "createApiWithValidOAS2DocumentAsJSONFile")
    public void createApiWithValidOAS3DocumentAsJSONFile() throws Exception {

        swagger_file = new File(resourceLocation + File.separator + "swaggerFiles/OAS3Document.json");

        //Import api definition from swagger file
        apiRequest = new APIRequest(import_definition_file, swagger_file.getAbsolutePath(), "", type);

        HttpResponse designResponse1 = apiPublisher.designAPIWithOAS(apiRequest);
        Assert.assertTrue(designResponse1.getData().contains("imported"));

        //Save the API with imported values
        String payload = readFromFile(swagger_file.getAbsolutePath());
        JSONObject json = new JSONObject(payload);
        String apiName = json.getJSONObject("info").get("title").toString();
        String apiContext = json.get("basePath").toString();
        String apiVersion = json.getJSONObject("info").get("version").toString();

        apiRequest = new APIRequest(apiName, apiContext, apiVersion);
        apiRequest.setSwagger(payload);
        HttpResponse designResponse2 = apiPublisher.designAPI(apiRequest);
        verifyResponse(designResponse2);

        //Verify the API created with correct name, version and resources
        String name = (new JSONObject(designResponse2.getData())).getJSONObject("data").get("apiName").toString();
        String version = (new JSONObject(designResponse2.getData())).getJSONObject("data").get("version").toString();
        Assert.assertEquals(name, apiName, "Api name was not imported correctly");
        Assert.assertEquals(version, apiVersion, "Api version was not imported correctly");

        HttpResponse getResponse = apiPublisher.getAPI(apiName, "admin", apiVersion);
        String resource = (new JSONObject(getResponse.getData())).getJSONObject("api").get("resources").toString();
        Assert.assertTrue(resource != "null", "API resource was not imported correctly");

        //Assert resources
        assertPOSTResource(((JSONArray) (new JSONObject(getResponse.getData())).getJSONObject("api").
                get("templates")).get(0));
        assertGETResource(((JSONArray) (new JSONObject(getResponse.getData())).getJSONObject("api").
                get("templates")).get(1));

    }

    @Test(description = "1.1.2.3", dependsOnMethods = "createApiWithValidOAS3DocumentAsJSONFile")
    public void createApiWithValidOAS2DocumentAsYAMLFile() throws Exception {

        swagger_file = new File(resourceLocation + File.separator + "swaggerFiles/OAS2Document.yaml");

        //Import api definition from swagger file
        apiRequest = new APIRequest(import_definition_file, swagger_file.getAbsolutePath(), "", type);

        HttpResponse designResponse1 = apiPublisher.designAPIWithOAS(apiRequest);
        Assert.assertTrue(designResponse1.getData().contains("imported"));

        //Save the API with imported values
        String payload = readFromFile(swagger_file.getAbsolutePath());

        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>) yaml.load(payload);
        JSONObject json = new JSONObject(map);
        String apiName = json.getJSONObject("info").get("title").toString();
        String apiContext = json.get("basePath").toString();
        String apiVersion = json.getJSONObject("info").get("version").toString();
        String payloadJson = json.toString();

        apiRequest = new APIRequest(apiName, apiContext, apiVersion);
        apiRequest.setSwagger(payloadJson);
        HttpResponse designResponse2 = apiPublisher.designAPI(apiRequest);
        verifyResponse(designResponse2);

        //Verify the API created with correct name, version and resources
        String name = (new JSONObject(designResponse2.getData())).getJSONObject("data").get("apiName").toString();
        String version = (new JSONObject(designResponse2.getData())).getJSONObject("data").get("version").toString();
        Assert.assertEquals(name, apiName, "Api name was not imported correctly");
        Assert.assertEquals(version, apiVersion, "Api version was not imported correctly");

        HttpResponse getResponse = apiPublisher.getAPI(apiName, "admin", apiVersion);
        String resource = (new JSONObject(getResponse.getData())).getJSONObject("api").get("resources").toString();
        Assert.assertTrue(resource != "null", "API resource was not imported correctly");

        //Assert resources
        assertGETResource(((JSONArray) (new JSONObject(getResponse.getData())).getJSONObject("api").
                get("templates")).get(0));
    }

    @Test(description = "1.1.2.4", dependsOnMethods = "createApiWithValidOAS2DocumentAsYAMLFile")
    public void createApiWithValidOAS3DocumentAsYAMLFile() throws Exception {

        swagger_file = new File(resourceLocation + File.separator + "swaggerFiles/OAS3Document.yaml");

        //Import api definition from swagger file
        apiRequest = new APIRequest(import_definition_file, swagger_file.getAbsolutePath(), "", type);

        HttpResponse designResponse1 = apiPublisher.designAPIWithOAS(apiRequest);
        Assert.assertTrue(designResponse1.getData().contains("imported"));

        //Save the API with imported values
        String payload = readFromFile(swagger_file.getAbsolutePath());

        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>) yaml.load(payload);
        JSONObject json = new JSONObject(map);
        String apiName = json.getJSONObject("info").get("title").toString();
        String apiContext = json.get("basePath").toString();
        String apiVersion = json.getJSONObject("info").get("version").toString();
        String payloadJson = json.toString();

        apiRequest = new APIRequest(apiName, apiContext, apiVersion);
        apiRequest.setSwagger(payloadJson);
        HttpResponse designResponse2 = apiPublisher.designAPI(apiRequest);
        verifyResponse(designResponse2);

        //Verify the API created with correct name, version and resources
        String name = (new JSONObject(designResponse2.getData())).getJSONObject("data").get("apiName").toString();
        String version = (new JSONObject(designResponse2.getData())).getJSONObject("data").get("version").toString();
        Assert.assertEquals(name, apiName, "Api name was not imported correctly");
        Assert.assertEquals(version, apiVersion, "Api version was not imported correctly");

        HttpResponse getResponse = apiPublisher.getAPI(apiName, "admin", apiVersion);
        String resource = (new JSONObject(getResponse.getData())).getJSONObject("api").get("resources").toString();
        Assert.assertTrue(resource != "null", "API resource was not imported correctly");

        //Assert resources
        assertPOSTResource(((JSONArray) (new JSONObject(getResponse.getData())).getJSONObject("api").
                get("templates")).get(0));
        assertGETResource(((JSONArray) (new JSONObject(getResponse.getData())).getJSONObject("api").
                get("templates")).get(1));
    }

    private void assertGETResource(Object resource) throws JSONException {

        if (resource instanceof JSONArray) {
            JSONArray resourceAttributes = (JSONArray) resource;
            String path = resourceAttributes.get(0).toString();
            Assert.assertEquals(path, "/pets/{petId}", "API resource path not imported correctly");

            String httpMethod = resourceAttributes.get(1).toString();
            Assert.assertEquals(httpMethod, "GET", "API resource's httpMethod not imported correctly");

            String authType = resourceAttributes.get(2).toString();
            Assert.assertEquals(authType, "Any", "API resource's authType not imported correctly");
        }
    }

    private void assertPOSTResource(Object resource) throws JSONException {

        if (resource instanceof JSONArray) {
            JSONArray resourceAttributes = (JSONArray) resource;
            String path = resourceAttributes.get(0).toString();
            Assert.assertEquals(path, "/pets", "API resource path not imported correctly");

            String httpMethod = resourceAttributes.get(1).toString();
            Assert.assertEquals(httpMethod, "POST", "API resource's httpMethod not imported correctly");

            String authType = resourceAttributes.get(2).toString();
            Assert.assertEquals(authType, "Any", "API resource's authType not imported correctly");
        }
    }

    //TODO: Commented due to the SSL certification issue for read from url still occurs
    @Test(description = "1.1.2.2", dataProvider = "OASDocsWithJsonURL", dataProviderClass = ScenarioDataProvider.class)
    public void testCreateApiUsingValidOASDocumentFromJsonURL(String url) throws Exception {

        swagger_url = url;

        apiRequest = new APIRequest(import_definition_url, "", swagger_url, type);

        HttpResponse serviceResponse = apiPublisher.designAPIWithOAS(apiRequest);
        Assert.assertTrue(serviceResponse.getData().contains("imported"));

        new Thread(new SimpleHTTPServer()).start();

        String payload = doGet(swagger_url);
        JSONObject json = new JSONObject(payload);
        String apiName = json.getJSONObject("info").get("title").toString();
        String context = json.get("basePath").toString();
        String version = json.getJSONObject("info").get("version").toString();

        apiRequest = new APIRequest(apiName, context, version);
        apiRequest.setSwagger(payload);

        serviceResponse = apiPublisher.designAPI(apiRequest);
        String name = (new JSONObject(serviceResponse.getData())).getJSONObject("data").get("apiName").toString();
        Assert.assertEquals(name, apiName);

        serviceResponse = apiPublisher.deleteAPI(apiName, version, "admin");
        verifyResponse(serviceResponse);

        Thread.sleep(1000); // To avoid connection failure in the next iteration.

    }

    //TODO: Commented due to the SSL certification issue for read from url still occurs

//    @Test(description = "1.1.2.4", dataProvider = "OASDocsWithYamlURL", dataProviderClass = ScenarioDataProvider.class)
//    public void testCreateApiUsingValidOASDocumentFromYamlURL(String url) throws Exception {
//        swagger_url = url;
//
//        apiRequest = new APIRequest(import_definition_url, "", swagger_url, type);
//
//        HttpResponse serviceResponse = apiPublisher.designAPIWithOAS(apiRequest);
//        Assert.assertTrue(serviceResponse.getData().contains("imported"));
//
//        String payload = readFromUrl(swagger_url);
//
//        Yaml yaml = new Yaml();
//        Map<String, Object> map = (Map<String, Object>) yaml.load(payload);
//        JSONObject json = new JSONObject(map);
//        String apiName = json.getJSONObject("info").get("title").toString();
//        String context = json.get("basePath").toString();
//        String version = json.getJSONObject("info").get("version").toString();
//        String payloadJson = json.toString();
//
//        apiRequest = new APIRequest(apiName, context, version);
//        apiRequest.setSwagger(payloadJson);
//
//        serviceResponse = apiPublisher.designAPI(apiRequest);
//        String name = (new JSONObject(serviceResponse.getData())).getJSONObject("data").get("apiName").toString();
//        Assert.assertEquals(name, apiName);
//
//        serviceResponse = apiPublisher.deleteAPI(apiName, version, "admin");
//        verifyResponse(serviceResponse);
//    }
//
//    public static String readFromUrl(String url) throws IOException {
//        URL myUrl = new URL(url);
//        HttpsURLConnection conn = (HttpsURLConnection) myUrl.openConnection();
//        InputStream is = conn.getInputStream();
//        try {
//            BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
//            StringBuilder sb = new StringBuilder();
//            int x;
//            while ((x = br.read()) != -1) {
//                sb.append((char) x);
//            }
//            String payloadText = sb.toString();
//            return payloadText;
//        } finally {
//            is.close();
//        }
//    }

    @AfterClass(alwaysRun = true)
    public void RemoveAPI() throws APIManagerIntegrationTestException {
        //clean the data
        apiPublisher.deleteAPI("PetApiSample_OAS2_JSON", apiVersion, "admin");
        apiPublisher.deleteAPI("PetApiSample_OAS2_YAML", apiVersion, "admin");
        apiPublisher.deleteAPI("PetApiSample_OAS3_JSON", apiVersion, "admin");
        apiPublisher.deleteAPI("PetApiSample_OAS3_YAML", apiVersion, "admin");
    }

    public static String readFromFile(String file_name) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(file_name));
        StringBuilder sb = new StringBuilder();
        int x;
        while ((x = br.read()) != -1) {
            sb.append((char) x);
        }
        String payloadText = sb.toString();
        return payloadText;
    }

    public String doGet(String url) {

        StringBuffer response = new StringBuffer();
        URL obj = null;
        try {
            obj = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        BufferedReader in = null;
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("GET");

            con.setRequestProperty("User-Agent", USER_AGENT);
            int responseCode = 0;
            responseCode = con.getResponseCode();
            System.out.println("GET Response Code :: " + responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                in = new BufferedReader(new InputStreamReader(
                        con.getInputStream()));
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

            }
        } catch (IOException e) {
            log.error("Error in reading url : " + url, e);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return response.toString();
    }
}

