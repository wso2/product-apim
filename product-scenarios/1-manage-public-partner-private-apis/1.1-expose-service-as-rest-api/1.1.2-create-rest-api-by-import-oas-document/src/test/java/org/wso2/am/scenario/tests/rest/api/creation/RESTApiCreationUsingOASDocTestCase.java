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

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.yaml.snakeyaml.Yaml;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

public class RESTApiCreationUsingOASDocTestCase extends ScenarioTestBase {

    private APIPublisherRestClient apiPublisher;
    private String publisherURLHttp;
    private APIRequest apiRequest;
    private Properties infraProperties;
    private String import_definition_url = "swagger-url";
    private String import_definition_file = "swagger-file";
    private File swagger_file;
    private String swagger_url;
    private String type = "rest";

    String resourceLocation = System.getProperty("test.resource.location");

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {

        infraProperties = getDeploymentProperties();
        String authority = infraProperties.getProperty(CARBON_SERVER_URL);
        if (authority != null && authority.contains("/")) {
            authority = authority.split("/")[2];
        } else if (authority == null) {
            authority = "localhost";
        }
        publisherURLHttp = "http://" + authority + ":9763/";

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login("admin", "admin");
    }


    @Test(description = "1.1.2.1", dataProvider = "OASDocsWithJSONFiles", dataProviderClass = ScenarioDataProvider.class)
    public void testCreateApiUsingValidOASDocumentFromJsonFile(String file_path) throws Exception {

        swagger_file = new File(resourceLocation + File.separator + file_path);

        apiRequest = new APIRequest(import_definition_file, swagger_file.getAbsolutePath(), "", type);

        HttpResponse serviceResponse = apiPublisher.designAPIWithOAS(apiRequest);
        Assert.assertTrue(serviceResponse.getData().contains("imported"));

        String payload = readFromFile(swagger_file.getAbsolutePath());
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
    }


    @Test(description = "1.1.2.2", dataProvider = "OASDocsWithJsonURL", dataProviderClass = ScenarioDataProvider.class)
    public void testCreateApiUsingValidOASDocumentFromJsonURL(String url) throws Exception {
        swagger_url = url;

        apiRequest = new APIRequest(import_definition_url, "", swagger_url, type);

        HttpResponse serviceResponse = apiPublisher.designAPIWithOAS(apiRequest);
        Assert.assertTrue(serviceResponse.getData().contains("imported"));

        String payload = readFromUrl(swagger_url);
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

    }


    @Test(description = "1.1.2.3", dataProvider = "OASDocsWithYAMLFiles", dataProviderClass = ScenarioDataProvider.class)
    public void testCreateApiUsingValidOASDocumentFromYamlFile(String file_path) throws Exception {
        swagger_file = new File(resourceLocation + File.separator + file_path);

        apiRequest = new APIRequest(import_definition_file, swagger_file.getAbsolutePath(), "", type);

        HttpResponse serviceResponse = apiPublisher.designAPIWithOAS(apiRequest);
        Assert.assertTrue(serviceResponse.getData().contains("imported"));

        String payload = readFromFile(swagger_file.getAbsolutePath());

        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>) yaml.load(payload);
        JSONObject json = new JSONObject(map);
        String apiName = json.getJSONObject("info").get("title").toString();
        String context = json.get("basePath").toString();
        String version = json.getJSONObject("info").get("version").toString();
        String payloadJson = json.toString();

        apiRequest = new APIRequest(apiName, context, version);
        apiRequest.setSwagger(payloadJson);

        serviceResponse = apiPublisher.designAPI(apiRequest);
        String name = (new JSONObject(serviceResponse.getData())).getJSONObject("data").get("apiName").toString();
        Assert.assertEquals(name, apiName);

        serviceResponse = apiPublisher.deleteAPI(apiName, version, "admin");
        verifyResponse(serviceResponse);
    }


    @Test(description = "1.1.2.4", dataProvider = "OASDocsWithYamlURL", dataProviderClass = ScenarioDataProvider.class)
    public void testCreateApiUsingValidOASDocumentFromYamlURL(String url) throws Exception {
        swagger_url = url;

        apiRequest = new APIRequest(import_definition_url, "", swagger_url, type);

        HttpResponse serviceResponse = apiPublisher.designAPIWithOAS(apiRequest);
        Assert.assertTrue(serviceResponse.getData().contains("imported"));

        String payload = readFromUrl(swagger_url);

        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>) yaml.load(payload);
        JSONObject json = new JSONObject(map);
        String apiName = json.getJSONObject("info").get("title").toString();
        String context = json.get("basePath").toString();
        String version = json.getJSONObject("info").get("version").toString();
        String payloadJson = json.toString();

        apiRequest = new APIRequest(apiName, context, version);
        apiRequest.setSwagger(payloadJson);

        serviceResponse = apiPublisher.designAPI(apiRequest);
        String name = (new JSONObject(serviceResponse.getData())).getJSONObject("data").get("apiName").toString();
        Assert.assertEquals(name, apiName);

        serviceResponse = apiPublisher.deleteAPI(apiName, version, "admin");
        verifyResponse(serviceResponse);
    }


    public static String readFromUrl(String url) throws IOException {
        URL myUrl = new URL(url);
        HttpsURLConnection conn = (HttpsURLConnection) myUrl.openConnection();
        InputStream is = conn.getInputStream();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();
            int x;
            while ((x = br.read()) != -1) {
                sb.append((char) x);
            }
            String payloadText = sb.toString();
            return payloadText;
        } finally {
            is.close();
        }
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

}
