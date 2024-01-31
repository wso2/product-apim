/*
 * Copyright (c) 2023, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.am.integration.tests.other;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.io.Files;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.MockServerUtils;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotEquals;

/**
 * Test exporting and importing of SOAP to rest APIs (type: Pass through)
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class SOAPAPIImportExportTestCase extends APIManagerLifecycleBaseTest {
    private static final Log log = LogFactory.getLog(SoapToRestTestCase.class);
    private final String SOAPTOREST_API_NAME = "PhoneVerification";
    private final String API_CONTEXT = "phoneverify";
    private final String API_VERSION_1_0_0 = "1.0";
    private static final String SOAPTOREST_TEST_USER = "soaptorestuser1";
    private static final String SOAPTOREST_TEST_USER_PASSWORD = "soaptorestuser1";
    private static final String SOAPTOREST_ROLE = "soaptorestrole1";
    private String endpointHost = "http://localhost";
    private int endpointPort;
    private String wsdlDefinition;
    private String soapToRestAPIId;
    private WireMockServer wireMockServer;
    private String apiEndPointURL;
    private String publisherURLHttps;
    private String responseBody;
    private APIDTO apidto;
    private String importUrl;
    private String exportUrl;
    private File zipTempDir, apiZip;
    private String newSoapToRestAPIId;

    @Factory(dataProvider = "userModeDataProvider")
    public SOAPAPIImportExportTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        publisherURLHttps = publisherUrls.getWebAppURLHttps();
        userManagementClient.addUser(SOAPTOREST_TEST_USER, SOAPTOREST_TEST_USER_PASSWORD, new String[] {}, null);
        userManagementClient.addRole(SOAPTOREST_ROLE, new String[] { SOAPTOREST_TEST_USER }, new String[] {});
        wsdlDefinition = readFile(
                getAMResourceLocation() + File.separator + "soap" + File.separator + "phoneverify.wsdl");
        responseBody = readFile(
                getAMResourceLocation() + File.separator + "soap" + File.separator +
                        "checkPhoneNumberResponseBody" + ".xml");

        //Start wiremock server
        startWiremockServer();
        apiEndPointURL = endpointHost + ":" + endpointPort + "/phoneverify";

        File file = getTempFileWithContent(wsdlDefinition);
        restAPIPublisher.validateWsdlDefinition(null, file);

        ArrayList<String> environment = new ArrayList<>();
        environment.add(Constants.GATEWAY_ENVIRONMENT);

        ArrayList<String> policies = new ArrayList<String>();
        policies.add("Unlimited");

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", SOAPTOREST_API_NAME);
        additionalPropertiesObj.put("context", API_CONTEXT);
        additionalPropertiesObj.put("version", API_VERSION_1_0_0);

        JSONObject endpointObject = new JSONObject();
        endpointObject.put("type", "address");
        endpointObject.put("url", apiEndPointURL);

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "address");
        endpointConfig.put("sandbox_endpoints", endpointObject);
        endpointConfig.put("production_endpoints", endpointObject);

        additionalPropertiesObj.put("endpointConfig", endpointConfig);
        additionalPropertiesObj.put("policies", policies);

        // Create SOAP-TO-REST API
        apidto = restAPIPublisher.importWSDLSchemaDefinition(file, null, additionalPropertiesObj.toString(), "SOAP");
        soapToRestAPIId = apidto.getId();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(soapToRestAPIId);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode(),
                SOAPTOREST_API_NAME + " API creation is failed");
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(soapToRestAPIId, restAPIPublisher);
        // Publish API
        restAPIPublisher.changeAPILifeCycleStatus(soapToRestAPIId, Constants.PUBLISHED);
        waitForAPIDeploymentSync(user.getUserName(), SOAPTOREST_API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);

        // Initialize the required variables
        exportUrl =
                publisherURLHttps + APIMIntegrationConstants.REST_API_PUBLISHER_CONTEXT_FULL +
                        APIMIntegrationConstants.REST_API_PUBLISHER_EXPORT_API_RESOURCE;
        importUrl =
                publisherURLHttps + APIMIntegrationConstants.REST_API_PUBLISHER_CONTEXT_FULL +
                        APIMIntegrationConstants.REST_API_PUBLISHER_IMPORT_API_RESOURCE;
    }

    @Test(groups = { "wso2.am" }, description = "Test exporting Soap-To-REST APIs")
    public void testAPIExport() throws Exception {
        //construct export API url
        URL exportRequest = new URL(
                exportUrl + "?name=" + SOAPTOREST_API_NAME + "&version=" + API_VERSION_1_0_0 + "&providerName=" +
                        user.getUserName() + "&format=JSON");
        zipTempDir = Files.createTempDir();
        //set the export file name with tenant prefix
        String fileName = user.getUserDomain() + "_" + SOAPTOREST_API_NAME;
        apiZip = new File(zipTempDir.getAbsolutePath() + File.separator + fileName + ".zip");
        //save the exported API
        exportArtifact(exportRequest, apiZip, user.getUserName(), user.getPassword());
    }

    @Test(groups = { "wso2.am" }, description = "Test importing Soap-To-REST APIs", dependsOnMethods = "testAPIExport")
    public void testAPIImport() throws Exception {
        undeployAndDeleteAPIRevisionsUsingRest(soapToRestAPIId, restAPIPublisher);
        //delete exported API before import
        HttpResponse serviceResponse = restAPIPublisher.deleteAPI(soapToRestAPIId);
        assertEquals(serviceResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "API delete failed");
        //upload the exported zip
        importArtifact(importUrl, apiZip, user.getUserName(), user.getPassword().toCharArray());
        waitForAPIDeployment();
    }

    @Test(groups = {
            "wso2.am" }, description = "Check whether the WSDL url of the API is correctly set for the imported APIs "
            + "which were exported when they were in published status", dependsOnMethods = "testAPIImport")
    public void testAPIWSDLUrl() throws Exception {
        //get the imported API information
        newSoapToRestAPIId = getAPI(SOAPTOREST_API_NAME, API_VERSION_1_0_0, user.getUserName()).getId();
        HttpResponse getAPIResponse = restAPIPublisher.getAPI(newSoapToRestAPIId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);
        String state = apidto.getLifeCycleStatus();
        assertEquals(state, APILifeCycleState.PUBLISHED.getState().toUpperCase(),
                "Imported API not in Published state");
        String wsdlUrl = apidto.getWsdlUrl();
        String expectedRegistryWsdlUri = null;
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            expectedRegistryWsdlUri =
                    "/registry/resource/_system/governance/apimgt/applicationdata/provider/admin/" +
                            "PhoneVerification/1.0/admin--PhoneVerification1.0.wsdl";
        } else if (TestUserMode.TENANT_ADMIN.equals(userMode)) {
            expectedRegistryWsdlUri = "/t/wso2.com/registry/resource/_system/governance/apimgt/applicationdata" +
                    "/provider/" + "admin-AT-wso2.com/PhoneVerification/1.0/admin-AT-wso2.com--PhoneVerification1.0" +
                    ".wsdl";
        }

        assertEquals(wsdlUrl, expectedRegistryWsdlUri, "WSDL URI set to the imported API is incorrect");
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
        restAPIPublisher.deleteAPI(newSoapToRestAPIId);
        userManagementClient.deleteRole(SOAPTOREST_ROLE);
        userManagementClient.deleteUser(SOAPTOREST_TEST_USER);
        boolean deleteStatus;
        deleteStatus = apiZip.delete();
        Assert.assertTrue(deleteStatus, "temp file deletion not successful");
        FileUtils.deleteDirectory(zipTempDir);
        Assert.assertTrue(deleteStatus, "temp directory deletion not successful");
        super.cleanUp();
    }

    private void startWiremockServer() {
        endpointPort = MockServerUtils.getAvailablePort(MockServerUtils.LOCALHOST, true);
        assertNotEquals(endpointPort, -1,
                "No available port in the range " + MockServerUtils.httpsPortLowerRange + "-" +
                        MockServerUtils.httpsPortUpperRange + " was found");
        wireMockServer = new WireMockServer(options().port(endpointPort));
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/phoneverify/wsdl")).willReturn(
                aResponse().withStatus(200).withHeader("Content-Type", "text/xml").withBody(wsdlDefinition)));
        wireMockServer.stubFor(WireMock.post(urlEqualTo("/phoneverify"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/xml").withBody(responseBody)));
        wireMockServer.start();
    }

    private File getTempFileWithContent(String schema) throws Exception {
        File temp = File.createTempFile("phoneverify", ".wsdl");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(schema);
        out.close();
        return temp;
    }

    /**
     * Save file from a given URL
     *
     * @param exportRequest URL of the file location
     * @param fileName      expected File to be saved
     * @throws URISyntaxException throws if URL is malformed
     * @throws IOException        throws if connection issues occurred
     */
    private void exportArtifact(URL exportRequest, File fileName, String username, String password)
            throws URISyntaxException, IOException {
        CloseableHttpResponse response = exportAPIRequest(exportRequest, username, password);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            FileOutputStream outStream = new FileOutputStream(fileName);
            try {
                entity.writeTo(outStream);
            } finally {
                outStream.close();
            }
        }

        assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK, "Response code is not as expected");
        Assert.assertTrue(fileName.exists(), "File save was not successful");
    }

    private CloseableHttpResponse exportAPIRequest(URL exportRequest, String username, String password)
            throws IOException, URISyntaxException {
        CloseableHttpClient client = HTTPSClientUtils.getHttpsClient();
        HttpGet get = new HttpGet(exportRequest.toURI());
        get.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                "Basic " + encodeCredentials(username, password.toCharArray()));
        CloseableHttpResponse response = client.execute(get);
        return response;
    }

    /**
     * get the base64 encoded username and password
     *
     * @param user username
     * @param pass password
     * @return encoded basic auth, as string
     */
    private static String encodeCredentials(String user, char[] pass) {
        StringBuilder builder = new StringBuilder(user).append(':').append(pass);
        String cred = builder.toString();
        byte[] encodedBytes = Base64.encodeBase64(cred.getBytes());
        return new String(encodedBytes);
    }

    /**
     * Upload a file to the given URL
     *
     * @param importUrl URL to be file upload
     * @param fileName  Name of the file to be upload
     * @throws IOException throws if connection issues occurred
     */
    private static void importArtifact(String importUrl, File fileName, String user, char[] pass) throws IOException {
        //open import API url connection and deploy the exported API
        URL url = new URL(importUrl);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        });
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        FileBody fileBody = new FileBody(fileName);
        MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.STRICT);
        multipartEntity.addPart("file", fileBody);

        connection.setRequestProperty("Content-Type", multipartEntity.getContentType().getValue());
        connection.setRequestProperty(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                "Basic " + encodeCredentials(user, pass));
        OutputStream out = connection.getOutputStream();
        try {
            multipartEntity.writeTo(out);
        } finally {
            out.close();
        }
        int status = connection.getResponseCode();
        BufferedReader read = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String temp;
        StringBuilder response = new StringBuilder();
        while ((temp = read.readLine()) != null) {
            response.append(temp);
        }
        assertEquals(status, HttpStatus.SC_OK, "Response code is not as expected : " + response);
    }

    /**
     * Get the API related to the given name, version and provider
     */
    private APIDTO getAPI(String apiName, String apiVersion, String provider) throws Exception {
        //get the imported file information
        int retry = 10;
        String apiId = "";
        APIListDTO apiListDTO = restAPIPublisher.getAllAPIs();
        log.info("Get All APIS after import: " + apiListDTO.toString());
        if (apiListDTO == null) {
            Thread.sleep(3000);
            for (int i = 0; i < retry; i++) {
                apiListDTO = restAPIPublisher.getAllAPIs();
                if (apiListDTO == null) {
                    retry++;
                    Thread.sleep(3000);
                    log.info("Waiting for the imported APIs");
                } else {
                    break;
                }
            }
        }
        assertNotNull(apiListDTO, "No APIs found in API Publisher");
        for (APIInfoDTO apiInfoDTO : apiListDTO.getList()) {
            if (apiName.equals(apiInfoDTO.getName()) && apiVersion.equals(apiInfoDTO.getVersion()) && provider.equals(
                    apiInfoDTO.getProvider())) {
                apiId = apiInfoDTO.getId();
                log.info("API Object after Import: " + apiInfoDTO.toString());
            }
        }
        log.info("API ID after import: " + apiId);
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        assertEquals(response.getResponseCode(), HTTP_RESPONSE_CODE_OK, "API get failed");
        Gson g = new Gson();
        APIDTO apiObj = g.fromJson(response.getData(), APIDTO.class);
        return apiObj;
    }
}
