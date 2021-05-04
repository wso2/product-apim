/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.soap;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

public class SOAPPassThroughTestCase extends APIMIntegrationBaseTest {
    protected static final int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_CREATED = Response.Status.CREATED.getStatusCode();
    private final String API_NAME = "SOAPPassThroughTestAPI";
    private final String API_CONTEXT = "soappassthroughtestapi";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "SOAPPassThroughAPITestApplication";
    private String apiId;
    private String applicationId;
    private String accessToken;
    private String wsdlDefinition;
    private String requestBody;
    private String responseBody;
    private String endpointHost = "http://localhost";
    private int endpointPort;
    private int lowerPortLimit = 9950;
    private int upperPortLimit = 9999;
    private WireMockServer wireMockServer;
    private String apiEndPointURL;
    private String wsdlURL;

    @Factory(dataProvider = "userModeDataProvider")
    public SOAPPassThroughTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
                new Object[]{TestUserMode.SUPER_TENANT_EMAIL_USER},
                new Object[]{TestUserMode.TENANT_EMAIL_USER},
                new Object[]{TestUserMode.SUPER_TENANT_USER},
                new Object[]{TestUserMode.TENANT_USER}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        // Load request/response body
        wsdlDefinition = readFile(getAMResourceLocation() + File.separator + "soap" + File.separator +
                "phoneverify.wsdl");
        requestBody = readFile(getAMResourceLocation() + File.separator + "soap" + File.separator +
                "checkPhoneNumberRequestBody.xml");
        responseBody = readFile(getAMResourceLocation() + File.separator + "soap" + File.separator +
                "checkPhoneNumberResponseBody.xml");

        // Start wiremock server
        startWiremockServer();
        apiEndPointURL = endpointHost + ":" + endpointPort + "/phoneverify";
        wsdlURL = endpointHost + ":" + endpointPort + "/phoneverify/wsdl";

        // Create additionalProperties object
        JSONObject additionalProperties = new JSONObject();
        additionalProperties.put("name", API_NAME);
        additionalProperties.put("context", API_CONTEXT);
        additionalProperties.put("version", API_VERSION_1_0_0);
        ArrayList<String> policies = new ArrayList<>();
        policies.add("Unlimited");
        additionalProperties.put("policies", policies);
        JSONObject endpoint = new JSONObject();
        endpoint.put("url", apiEndPointURL);
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("sandbox_endpoints", endpoint);
        endpointConfig.put("production_endpoints", endpoint);
        additionalProperties.put("endpointConfig", endpointConfig);

        // Create SOAP API
        APIDTO apidto = restAPIPublisher
                .importWSDLDefinition(null, wsdlURL, additionalProperties.toString(), "SOAP");
        apiId = apidto.getId();
        HttpResponse apiResponse = restAPIPublisher.getAPI(apiId);
        assertEquals(apiResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Unable to create SOAP API");

        // Create Revision
        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(apiId);
        apiRevisionRequest.setDescription("Revision 1");
        HttpResponse apiRevisionResponse = restAPIPublisher.addAPIRevision(apiRevisionRequest);
        assertEquals(apiRevisionResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to create API Revision" + apiRevisionResponse.getData());
        JSONObject revisionResponseData = new JSONObject(apiRevisionResponse.getData());
        String revisionUUID = revisionResponseData.getString("id");

        // Deploy Revision
        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionDeployRequest.setVhost("localhost");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionDeployResponse = restAPIPublisher.deployAPIRevision(apiId, revisionUUID,
                apiRevisionDeployRequestList);
        assertEquals(apiRevisionDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to deploy API Revisions: " + apiRevisionDeployResponse.getData());
        waitForAPIDeployment();

        // Change lifecycle stage from CREATED to PUBLISHED
        HttpResponse apiLifecycleChangeResponse = restAPIPublisher.changeAPILifeCycleStatus(apiId,
                APILifeCycleAction.PUBLISH.getAction(), null);
        assertEquals(apiLifecycleChangeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to change lifecycle stage to PUBLISHED: " + apiLifecycleChangeResponse.getData());
        waitForAPIDeployment();

        // Create application and subscribe to API
        ApplicationDTO applicationDTO = restAPIStore.addApplication(APPLICATION_NAME,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
        applicationId = applicationDTO.getApplicationId();
        restAPIStore.subscribeToAPI(apiId, applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);

        // Generate access token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add("client_credentials");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        assertNotNull(applicationKeyDTO.getToken(), "Unable to get access token");
        accessToken = applicationKeyDTO.getToken().getAccessToken();
    }

    @Test(groups = {"wso2.am"}, description = "Test retrieval of WSDL Definition from Publisher")
    public void testGetWSDLDefinitionFromPublisher() throws ApiException {
        HttpResponse getWSDLDefinitionResponse = restAPIPublisher.getWSDLDefinition(apiId);
        assertEquals(getWSDLDefinitionResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to retrieve WSDL Definition from Publisher");
    }

    @Test(groups = {"wso2.am"}, description = "Test retrieval of WSDL Definition from Devportal")
    public void testGetWSDLDefinitionFromDevportal() throws org.wso2.am.integration.clients.store.api.ApiException {
        HttpResponse getWSDLDefinitionResponse = restAPIStore.getWSDLDefinition(apiId);
        assertEquals(getWSDLDefinitionResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to retrieve WSDL Definition from Devportal");
    }

    @Test(groups = {"wso2.am"}, description = "Test invoking Check Phone Number method")
    public void testInvokeCheckPhoneNumber() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/xml");
        headers.put("accept", "text/xml");
        headers.put("Authorization", "Bearer " + accessToken);
        HttpResponse invokeAPIResponse = HttpRequestUtil.doPost(new URL(getAPIInvocationURLHttp(API_CONTEXT,
                API_VERSION_1_0_0)), requestBody, headers);
        assertEquals(invokeAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to invoke Pass Through SOAP API");
    }

    private void startWiremockServer() {
        endpointPort = getAvailablePort();
        assertNotEquals(endpointPort, -1, "No available port in the range " + lowerPortLimit + "-" +
                upperPortLimit + " was found");
        wireMockServer = new WireMockServer(options().port(endpointPort));
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/phoneverify/wsdl")).willReturn(aResponse()
                .withStatus(200).withHeader("Content-Type", "text/xml").withBody(wsdlDefinition)));
        wireMockServer.stubFor(WireMock.post(urlEqualTo("/phoneverify")).willReturn(aResponse()
                .withStatus(200).withHeader("Content-Type", "text/xml").withBody(responseBody)));
        wireMockServer.start();
    }

    /**
     * Find a free port to start backend WebSocket server in given port range
     *
     * @return Available Port Number
     */
    private int getAvailablePort() {
        while (lowerPortLimit < upperPortLimit) {
            if (isPortFree(lowerPortLimit)) {
                return lowerPortLimit;
            }
            lowerPortLimit++;
        }
        return -1;
    }

    /**
     * Check whether give port is available
     *
     * @param port Port Number
     * @return status
     */
    private boolean isPortFree(int port) {
        Socket s = null;
        try {
            s = new Socket(endpointHost, port);
            // something is using the port and has responded.
            return false;
        } catch (IOException e) {
            // port available
            return true;
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to close connection ", e);
                }
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiId);
        wireMockServer.stop();
    }
}
