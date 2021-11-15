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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.wso2.am.integration.tests.solace;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.AsyncAPISpecificationValidationResponseDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionRequest;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

public class SolacePublisherPortalTestCase extends APIManagerLifecycleBaseTest {
    private final Log log = LogFactory.getLog(SolacePublisherPortalTestCase.class);

    protected static final int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_CREATED = Response.Status.CREATED.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_NO_CONTENT = Response.Status.NO_CONTENT.getStatusCode();

    private final String KEYSTORE_FILE_PATH_CLIENT =
            TestConfigurationProvider.getResourceLocation() + File.separator + "keystores" + File.separator + "products"
                    + File.separator + "wso2carbon.jks";
    private final String TRUSTSTORE_FILE_PATH_CLIENT =
            TestConfigurationProvider.getResourceLocation() + File.separator + "keystores" + File.separator + "products"
                    + File.separator + "client-truststore.jks";

    // Solace broker config related values
    protected static final String SOLACE_USER_NAME = "testUser";
    protected static final String SOLACE_PASSWORD = "testPassword";
    protected static final String SOLACE_ORGANIZATION = "TestWSO2";
    protected static final String SOLACE_DEVELOPER_USER_NAME = "publisherTestEnv";
    protected static final String SOLACE_ENVIRONMENT = "publisherEnv";
    protected static final String SOLACE_BASE_URL = "http://localhost:9955/";

    private ServerConfigurationManager serverConfigurationManager;
    private String endpointHost = "http://localhost";
    private static WireMockServer solaceWireMockServer;

    // Test artifact constants
    private String solaceApi1Id;
    private final String solaceApi1Name = "SolaceSampleAPI";
    private final String solaceApi1Context = "SolaceSampleAPI";
    private final String solaceApiVersion = "1.0";
    private final String solaceApiProduct1Name = SOLACE_ENVIRONMENT + "-" + solaceApi1Name + "-" + solaceApi1Context + "-"
            + solaceApiVersion;
    private final String solaceGatewayVendor = "solace";
    private String solaceApi2Id;
    private final String solaceApi2Name = "SolaceLifeCycleAPI";
    private final String solaceApi2Context = "SolaceLifeCycleAPI";
    private final String solaceApiProduct2Name = SOLACE_ENVIRONMENT + "-" + solaceApi2Name + "-" + solaceApi2Context + "-"
            + solaceApiVersion;
    private String solaceApi3Id;
    private final String solaceApi3Name = "SolaceDeleteAPI";
    private final String solaceApi3Context = "SolaceDeleteAPI";
    private final String solaceApiProduct3Name = SOLACE_ENVIRONMENT + "-" + solaceApi3Name + "-" + solaceApi3Context + "-"
            + solaceApiVersion;

    @Factory(dataProvider = "userModeDataProvider")
    public SolacePublisherPortalTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        log.info("SolaceAPICreateTestCase initiated");

        super.init(userMode);

        serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
        serverConfigurationManager.applyConfiguration(new File(
                getAMResourceLocation() + File.separator + "solace" + File.separator + "publisher_tests" +
                        File.separator + "deployment.toml"));
        String tenantDomain = storeContext.getContextTenant().getDomain();
        String userName = keyManagerContext.getContextTenant().getTenantAdmin().getUserName();
        String password = keyManagerContext.getContextTenant().getTenantAdmin().getPassword();
        ArrayList<String> grantTypes = new ArrayList<>();
        String publisherURLHttps = publisherUrls.getWebAppURLHttps();

        // Setting the system properties to call the etcd endpoint
        System.setProperty("javax.net.ssl.keyStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.keyStore", KEYSTORE_FILE_PATH_CLIENT);
        System.setProperty("javax.net.ssl.keyStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_FILE_PATH_CLIENT);
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");

        // Load request/response body
        String solaceDefinitionPath = FrameworkPathUtil.getSystemResourceLocation() + "solace"
                + File.separator + "APIMaintenance.yml";
        String solaceDefinition = readFile(solaceDefinitionPath);

        // Start wiremock server
        startSolaceWiremockServer();

    }

    public void startSolaceWiremockServer() {
        int wireMockPort = 9955;
        solaceWireMockServer = new WireMockServer(options().port(wireMockPort));

        String toEncode = SOLACE_USER_NAME + ":" + SOLACE_PASSWORD;
        // GET Solace developer
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/developers/" + SOLACE_DEVELOPER_USER_NAME)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withBody("{\n" +
                                        "    \"email\": \"dev-1@service-co.com\",\n" +
                                        "    \"firstName\": \"Dev-1\",\n" +
                                        "    \"lastName\": \"Developer-1\",\n" +
                                        "    \"userName\": \"dev-1\"\n" +
                                        "}")
                                .withStatus(HTTP_RESPONSE_CODE_OK)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
        );

        //Get environment registered in Solace broker
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/environments/" + SOLACE_ENVIRONMENT)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withBody("{\n" +
                                        "    \"description\": \"development api gateway broker\",\n" +
                                        "    \"name\": \"devEnv\",\n" +
                                        "    \"serviceId\": \"1j7v403y2q9f\",\n" +
                                        "    \"serviceName\": \"dev-broker\",\n" +
                                        "    \"creationState\": \"completed\",\n" +
                                        "    \"datacenterId\": \"aws-eu-central-1a\",\n" +
                                        "    \"datacenterProvider\": \"aws\",\n" +
                                        "    \"msgVpnName\": \"dev-broker\",\n" +
                                        "    \"serviceClassDisplayedAttributes\": {\n" +
                                        "        \"High Availability\": \"HA Group\",\n" +
                                        "        \"Network Speed\": \"450 Mbps\",\n" +
                                        "        \"Storage\": \"25 GB\",\n" +
                                        "        \"Message Broker Tenancy\": \"Dedicated\",\n" +
                                        "        \"Queues\": \"250\",\n" +
                                        "        \"Clients\": \"250\",\n" +
                                        "        \"Network Usage\": \"50 GB per month\"\n" +
                                        "    },\n" +
                                        "    \"serviceClassId\": \"enterprise-250-nano\",\n" +
                                        "    \"serviceTypeId\": \"enterprise\",\n" +
                                        "    \"messagingProtocols\": [\n" +
                                        "        {\n" +
                                        "            \"compressed\": \"no\",\n" +
                                        "            \"secure\": \"no\",\n" +
                                        "            \"protocol\": {\n" +
                                        "                \"name\": \"mqtt\",\n" +
                                        "                \"version\": \"3.1.1\"\n" +
                                        "            },\n" +
                                        "            \"transport\": \"TCP\",\n" +
                                        "            \"uri\": \"tcp://mr1j7v403y2qa9.messaging.solace.cloud:1883\"\n" +
                                        "        },\n" +
                                        "        {\n" +
                                        "            \"compressed\": \"no\",\n" +
                                        "            \"secure\": \"yes\",\n" +
                                        "            \"protocol\": {\n" +
                                        "                \"name\": \"secure-mqtt\",\n" +
                                        "                \"version\": \"3.1.1\"\n" +
                                        "            },\n" +
                                        "            \"transport\": \"SSL\",\n" +
                                        "            \"uri\": \"ssl://mr1j7v403y2qa9.messaging.solace.cloud:8883\"\n" +
                                        "        },\n" +
                                        "        {\n" +
                                        "            \"compressed\": \"no\",\n" +
                                        "            \"secure\": \"no\",\n" +
                                        "            \"protocol\": {\n" +
                                        "                \"name\": \"ws-mqtt\",\n" +
                                        "                \"version\": \"3.1.1\"\n" +
                                        "            },\n" +
                                        "            \"transport\": \"WS\",\n" +
                                        "            \"uri\": \"ws://mr1j7v403y2qa9.messaging.solace.cloud:8000\"\n" +
                                        "        },\n" +
                                        "        {\n" +
                                        "            \"compressed\": \"no\",\n" +
                                        "            \"secure\": \"yes\",\n" +
                                        "            \"protocol\": {\n" +
                                        "                \"name\": \"wss-mqtt\",\n" +
                                        "                \"version\": \"3.1.1\"\n" +
                                        "            },\n" +
                                        "            \"transport\": \"WSS\",\n" +
                                        "            \"uri\": \"wss://mr1j7v403y2qa9.messaging.solace.cloud:8443\"\n" +
                                        "        },\n" +
                                        "        {\n" +
                                        "            \"compressed\": \"no\",\n" +
                                        "            \"secure\": \"no\",\n" +
                                        "            \"protocol\": {\n" +
                                        "                \"name\": \"jms\",\n" +
                                        "                \"version\": \"1.1\"\n" +
                                        "            },\n" +
                                        "            \"transport\": \"TCP\",\n" +
                                        "            \"uri\": \"smf://mr1j7v403y2qa9.messaging.solace.cloud:55555\"\n" +
                                        "        },\n" +
                                        "        {\n" +
                                        "            \"compressed\": \"no\",\n" +
                                        "            \"secure\": \"yes\",\n" +
                                        "            \"protocol\": {\n" +
                                        "                \"name\": \"secure-jms\",\n" +
                                        "                \"version\": \"1.1\"\n" +
                                        "            },\n" +
                                        "            \"transport\": \"TLS\",\n" +
                                        "            \"uri\": \"smfs://mr1j7v403y2qa9.messaging.solace.cloud:55443\"\n" +
                                        "        },\n" +
                                        "        {\n" +
                                        "            \"compressed\": \"no\",\n" +
                                        "            \"secure\": \"no\",\n" +
                                        "            \"protocol\": {\n" +
                                        "                \"name\": \"http\",\n" +
                                        "                \"version\": \"1.1\"\n" +
                                        "            },\n" +
                                        "            \"transport\": \"HTTP\",\n" +
                                        "            \"uri\": \"http://mr1j7v403y2qa9.messaging.solace.cloud:9000\"\n" +
                                        "        },\n" +
                                        "        {\n" +
                                        "            \"compressed\": \"no\",\n" +
                                        "            \"secure\": \"yes\",\n" +
                                        "            \"protocol\": {\n" +
                                        "                \"name\": \"https\",\n" +
                                        "                \"version\": \"1.1\"\n" +
                                        "            },\n" +
                                        "            \"transport\": \"HTTPS\",\n" +
                                        "            \"uri\": \"https://mr1j7v403y2qa9.messaging.solace.cloud:9443\"\n" +
                                        "        },\n" +
                                        "        {\n" +
                                        "            \"compressed\": \"no\",\n" +
                                        "            \"secure\": \"no\",\n" +
                                        "            \"protocol\": {\n" +
                                        "                \"name\": \"amqp\",\n" +
                                        "                \"version\": \"1.0.0\"\n" +
                                        "            },\n" +
                                        "            \"transport\": \"AMQP\",\n" +
                                        "            \"uri\": \"amqp://mr1j7v403y2qa9.messaging.solace.cloud:5672\"\n" +
                                        "        },\n" +
                                        "        {\n" +
                                        "            \"compressed\": \"no\",\n" +
                                        "            \"secure\": \"yes\",\n" +
                                        "            \"protocol\": {\n" +
                                        "                \"name\": \"amqps\",\n" +
                                        "                \"version\": \"1.0.0\"\n" +
                                        "            },\n" +
                                        "            \"transport\": \"AMQPS\",\n" +
                                        "            \"uri\": \"amqps://mr1j7v403y2qa9.messaging.solace.cloud:5671\"\n" +
                                        "        },\n" +
                                        "        {\n" +
                                        "            \"compressed\": \"no\",\n" +
                                        "            \"secure\": \"no\",\n" +
                                        "            \"protocol\": {\n" +
                                        "                \"name\": \"smf\",\n" +
                                        "                \"version\": \"smf\"\n" +
                                        "            },\n" +
                                        "            \"transport\": \"TCP\",\n" +
                                        "            \"uri\": \"tcp://mr1j7v403y2qa9.messaging.solace.cloud:55555\"\n" +
                                        "        },\n" +
                                        "        {\n" +
                                        "            \"compressed\": \"yes\",\n" +
                                        "            \"secure\": \"no\",\n" +
                                        "            \"protocol\": {\n" +
                                        "                \"name\": \"compressed-smf\",\n" +
                                        "                \"version\": \"smf\"\n" +
                                        "            },\n" +
                                        "            \"transport\": \"TCP\",\n" +
                                        "            \"uri\": \"tcp://mr1j7v403y2qa9.messaging.solace.cloud:55003\"\n" +
                                        "        },\n" +
                                        "        {\n" +
                                        "            \"compressed\": \"no\",\n" +
                                        "            \"secure\": \"yes\",\n" +
                                        "            \"protocol\": {\n" +
                                        "                \"name\": \"smfs\",\n" +
                                        "                \"version\": \"smfs\"\n" +
                                        "            },\n" +
                                        "            \"transport\": \"TLS\",\n" +
                                        "            \"uri\": \"tcps://mr1j7v403y2qa9.messaging.solace.cloud:55443\"\n" +
                                        "        }\n" +
                                        "    ]\n" +
                                        "}")
                                .withStatus(HTTP_RESPONSE_CODE_OK)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
        );

        // Get created APIProduct1 in Solace broker not found
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apiProducts/" + solaceApiProduct1Name)
                        .inScenario("Get APIProduct1")
                        .whenScenarioStateIs(STARTED)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NOT_FOUND)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo("Cause Success")
        );
        // Get created APIProduct1 in Solace broker success
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apiProducts/" + solaceApiProduct1Name)
                        .inScenario("Get APIProduct1")
                        .whenScenarioStateIs("Cause Success")
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_OK)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo(STARTED)
        );

        // Get created APIProduct2 in Solace broker not found
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apiProducts/" + solaceApiProduct2Name)
                        .inScenario("Get APIProduct2")
                        .whenScenarioStateIs(STARTED)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NOT_FOUND)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo("Cause Success")
        );
        // Get created APIProduct2 in Solace broker success
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apiProducts/" + solaceApiProduct2Name)
                        .inScenario("Get APIProduct2")
                        .whenScenarioStateIs("Cause Success")
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_OK)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo(STARTED)
        );

        // Get created APIProduct3 in Solace broker not found
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apiProducts/" + solaceApiProduct3Name)
                        .inScenario("Get APIProduct3")
                        .whenScenarioStateIs(STARTED)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NOT_FOUND)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo("Cause Success")
        );
        // Get created APIProduct3 in Solace broker success
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apiProducts/" + solaceApiProduct2Name)
                        .inScenario("Get APIProduct3")
                        .whenScenarioStateIs("Cause Success")
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_OK)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo(STARTED)
        );


        // Get registered API1 in solace broker not found
        String api1NameForRegistration = solaceApi1Name + "-" + solaceApiVersion;
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apis/" + api1NameForRegistration)
                        .inScenario("Get API1")
                        .whenScenarioStateIs(STARTED)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NOT_FOUND)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo("Cause Success")

        );
        // Get registered API1 in solace broker success
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apis/" + api1NameForRegistration)
                        .inScenario("Get API1")
                        .whenScenarioStateIs("Cause Success")
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NOT_FOUND)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo(STARTED)
        );

        // Get registered API2 in solace broker not found
        String api2NameForRegistration = solaceApi2Name + "-" + solaceApiVersion;
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apis/" + api2NameForRegistration)
                        .inScenario("Get API2")
                        .whenScenarioStateIs(STARTED)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NOT_FOUND)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo("Cause Success")
        );
        // Get registered API2 in solace broker success
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apis/" + api2NameForRegistration)
                        .inScenario("Get API2")
                        .whenScenarioStateIs("Cause Success")
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NOT_FOUND)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo(STARTED)
        );

        // Get registered API3 in solace broker not found
        String api3NameForRegistration = solaceApi3Name + "-" + solaceApiVersion;
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apis/" + api3NameForRegistration)
                        .inScenario("Get API3")
                        .whenScenarioStateIs(STARTED)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NOT_FOUND)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
        );
        // Get registered API3 in solace broker success
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apis/" + api3NameForRegistration)
                        .inScenario("Get API3")
                        .whenScenarioStateIs("Cause Success")
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NOT_FOUND)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo(STARTED)
        );

        // Create APIProducts in Solace broker
        solaceWireMockServer.stubFor(
                WireMock.post("/" + SOLACE_ORGANIZATION + "/apiProducts/" )
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_CREATED)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
        );

        // Register API1 in Solace broker
        solaceWireMockServer.stubFor(
                WireMock.post("/" + SOLACE_ORGANIZATION + "/apis/" + api1NameForRegistration)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_CREATED)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
        );

        // Register API2 in Solace broker
        solaceWireMockServer.stubFor(
                WireMock.post("/" + SOLACE_ORGANIZATION + "/apis/" + api2NameForRegistration)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_CREATED)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
        );

        // Register API3 in Solace broker
        solaceWireMockServer.stubFor(
                WireMock.post("/" + SOLACE_ORGANIZATION + "/apis/" + api2NameForRegistration)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_CREATED)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
        );


        // Delete API1 from Solace broker
        solaceWireMockServer.stubFor(
                WireMock.delete("/" + SOLACE_ORGANIZATION + "/apis/" + api1NameForRegistration)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NO_CONTENT)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
        );

        // Delete API2 from Solace broker
        solaceWireMockServer.stubFor(
                WireMock.delete("/" + SOLACE_ORGANIZATION + "/apis/" + api2NameForRegistration)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NO_CONTENT)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
        );

        // Delete API3 from Solace broker
        solaceWireMockServer.stubFor(
                WireMock.delete("/" + SOLACE_ORGANIZATION + "/apis/" + api2NameForRegistration)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NO_CONTENT)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
        );

        // Delete APIProduct1 from Solace broker
        solaceWireMockServer.stubFor(
                WireMock.delete("/" + SOLACE_ORGANIZATION + "/apiProducts/" + solaceApiProduct1Name)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NO_CONTENT)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
        );

        // Delete APIProduct2 from Solace broker
        solaceWireMockServer.stubFor(
                WireMock.delete("/" + SOLACE_ORGANIZATION + "/apiProducts/" + solaceApiProduct2Name)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NO_CONTENT)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
        );

        // Delete APIProduct3 from Solace broker
        solaceWireMockServer.stubFor(
                WireMock.delete("/" + SOLACE_ORGANIZATION + "/apiProducts/" + solaceApiProduct3Name)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NO_CONTENT)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
        );

        // Start mock server
        solaceWireMockServer.start();
    }

    @Test(groups = {"wso2.am"}, description = "Importing Solace Async API definition and create API")
    public void testSolaceDefinitionImport() throws Exception {

        log.info("testWsdlDefinitionImport initiated");

        // Set environment
        ArrayList<String> environment = new ArrayList<>();
        environment.add(Constants.GATEWAY_ENVIRONMENT);
        environment.add(SOLACE_ENVIRONMENT);

        // Create additional properties object
        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("provider", user.getUserName());
        additionalPropertiesObj.put("name", solaceApi1Name);
        additionalPropertiesObj.put("context", solaceApi1Context);
        additionalPropertiesObj.put("version", solaceApiVersion);
        additionalPropertiesObj.put("gatewayVendor", solaceGatewayVendor);
        additionalPropertiesObj.put("type", "WEBSUB");

        String solaceDefinitionPath = FrameworkPathUtil.getSystemResourceLocation() + "solace"
                + File.separator + "APIMaintenance.yml";
        File file = new File(solaceDefinitionPath);

        // Validate Async Definition of the Solace Specification
        AsyncAPISpecificationValidationResponseDTO asyncAPISpecificationValidationResponseDTO = restAPIPublisher.
                validateAsyncAPISchemaDefinition(null, file);
        assertTrue(asyncAPISpecificationValidationResponseDTO.isIsValid());

        // Create API by importing the Solace  definition
        APIDTO solaceApiDto = restAPIPublisher
                .importAsyncAPISchemaDefinition(file, null, additionalPropertiesObj.toString());

        // Make sure API is created properly
        solaceApi1Id = solaceApiDto.getId();
        assertEquals(solaceApiDto.getName(), solaceApi1Name);
        assertEquals(solaceApiDto.getContext(), "/" + solaceApi1Context);

        // Assert Solace specific API properties
        assertEquals(solaceApiDto.getGatewayVendor(), solaceGatewayVendor);
        List<String> asyncProtocolsForAssertion = new ArrayList<String>();
        asyncProtocolsForAssertion.add("mqtt");
        asyncProtocolsForAssertion.add("http");
        assertEquals(solaceApiDto.getAsyncTransportProtocols(), asyncProtocolsForAssertion);
    }

    @Test(groups = {"wso2.am"}, description = "Deploy created Solace API into Solace broker",
            dependsOnMethods = "testSolaceDefinitionImport")
    public void testSolaceAPIDeployToSolaceBroker() throws Exception {

        log.info("testSolaceAPIDeployToSolaceBroker initiated");

        // Create revision and deploy to Solace broker
        createSolaceAPIRevisionAndDeployToSolaceBroker(solaceApi1Id, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatusToPublish(solaceApi1Id, false);
        waitForAPIDeploymentSync(user.getUserName(), solaceApi1Name, solaceApiVersion,
                APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(groups = {"wso2.am"}, description = "Undeploy deployed Solace API from Solace broker",
            dependsOnMethods = "testSolaceAPIDeployToSolaceBroker")
    public void testSolaceAPIUndeployFromSolaceBroker() throws Exception {

        log.info("testSolaceAPIUndeployFromSolaceBroker initiated");

        //Undeploy from Solace broker
        undeployAndDeleteSolaceAPIRevisionsFromSolaceBroker(solaceApi1Id, restAPIPublisher);
        waitForAPIUnDeploymentSync(user.getUserName(), solaceApi1Name, solaceApiVersion,
                APIMIntegrationConstants.IS_API_EXISTS);
        // Assert that related artifacts are not found in solace broker
        HttpClient httpClient = HttpClients.createDefault();
        String apiNameForRegistration = solaceApi1Name + "-" + solaceApiVersion;
        HttpGet httpGet = new HttpGet(SOLACE_BASE_URL + "/" + SOLACE_ORGANIZATION + "/apis/" + apiNameForRegistration);
        String toEncode = SOLACE_USER_NAME + ":" + SOLACE_PASSWORD;
        httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes()));

        org.apache.http.HttpResponse response = httpClient.execute(httpGet);
        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Invocation fails for GET request");
    }

    @Test(groups = {"wso2.am"}, description = "Deploy Solace API to solace broker and change life cycle as Retired and " +
            "assert undeployment at Solace side", dependsOnMethods = "testSolaceAPIUndeployFromSolaceBroker")
    public void testSolaceAPIRetireLifeCycleAndUndeployFromSolaceBroker() throws Exception {

        log.info("testSolaceAPIRetireLifeCycleAndUndeployFromSolaceBroker initiated");

        // Set environment
        ArrayList<String> environment = new ArrayList<>();
        environment.add(Constants.GATEWAY_ENVIRONMENT);
        environment.add(SOLACE_ENVIRONMENT);

        // Create additional properties object
        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("provider", user.getUserName());
        additionalPropertiesObj.put("name", solaceApi2Name);
        additionalPropertiesObj.put("context", solaceApi2Context);
        additionalPropertiesObj.put("version", solaceApiVersion);
        additionalPropertiesObj.put("gatewayVendor", solaceGatewayVendor);
        additionalPropertiesObj.put("type", "WEBSUB");

        String solaceDefinitionPath = FrameworkPathUtil.getSystemResourceLocation() + "solace"
                + File.separator + "APIMaintenance.yml";
        File file = new File(solaceDefinitionPath);

        // Validate Async Definition of the Solace Specification
        AsyncAPISpecificationValidationResponseDTO asyncAPISpecificationValidationResponseDTO = restAPIPublisher.
                validateAsyncAPISchemaDefinition(null, file);
        assertTrue(asyncAPISpecificationValidationResponseDTO.isIsValid());

        // Create API by importing the Solace  definition
        APIDTO solaceApiDto = restAPIPublisher
                .importAsyncAPISchemaDefinition(file, null, additionalPropertiesObj.toString());

        // Make sure API is created properly
        solaceApi2Id = solaceApiDto.getId();
        assertEquals(solaceApiDto.getName(), solaceApi2Name);
        assertEquals(solaceApiDto.getContext(), "/" + solaceApi2Context);

        // Assert Solace specific API properties
        assertEquals(solaceApiDto.getGatewayVendor(), solaceGatewayVendor);
        List<String> asyncProtocolsForAssertion = new ArrayList<String>();
        asyncProtocolsForAssertion.add("mqtt");
        asyncProtocolsForAssertion.add("http");
        assertEquals(solaceApiDto.getAsyncTransportProtocols(), asyncProtocolsForAssertion);

        // Deploy API to Solace broker and publish API
        createSolaceAPIRevisionAndDeployToSolaceBroker(solaceApi2Id, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatusToPublish(solaceApi2Id, false);
        waitForAPIDeploymentSync(user.getUserName(), solaceApi2Name, solaceApiVersion,
                APIMIntegrationConstants.IS_API_EXISTS);

        //Change lifeCycle to DEPRECATED and then RETIRED
        HttpResponse blockAPIActionResponse = restAPIPublisher
                .changeAPILifeCycleStatus(solaceApi2Id, APILifeCycleAction.DEPRECATE.getAction(), null);
        assertEquals(blockAPIActionResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(APILifeCycleState.DEPRECATED.getState().equals(blockAPIActionResponse.getData()),
                "API status Change is invalid when retire an API :" + solaceApi2Name + " with API ID ("
                        + solaceApi2Id + ")" + " Response Code:" + blockAPIActionResponse.getResponseCode());

        HttpResponse retiredAPIActionResponse = restAPIPublisher
                .changeAPILifeCycleStatus(solaceApi2Id, APILifeCycleAction.RETIRE.getAction(), null);
        assertEquals(retiredAPIActionResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(APILifeCycleState.RETIRED.getState().equals(retiredAPIActionResponse.getData()),
                "API status Change is invalid when retire an API :" + solaceApi2Name + " with API ID ("
                        + solaceApi2Id + ")" + " Response Code:" + retiredAPIActionResponse.getResponseCode());

        // Assert that related artifacts are not found in solace broker
        HttpClient httpClient = HttpClients.createDefault();
        String apiNameForRegistration = solaceApi2Name + "-" + solaceApiVersion;
        HttpGet httpGet = new HttpGet(SOLACE_BASE_URL + "/" + SOLACE_ORGANIZATION + "/apis/" + apiNameForRegistration);
        String toEncode = SOLACE_USER_NAME + ":" + SOLACE_PASSWORD;
        httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes()));
        org.apache.http.HttpResponse response = httpClient.execute(httpGet);
        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Invocation fails for GET request");
    }

    @Test(groups = {"wso2.am"}, description = "Deploy Solace API to solace broker and delete API from publisher " +
            "assert undeployment at Solace side",
            dependsOnMethods = "testSolaceAPIRetireLifeCycleAndUndeployFromSolaceBroker")
    public void testSolaceAPIDeleteAndUndeployFromSolaceBroker() throws Exception {
        log.info("testSolaceAPIDeleteAndUndeployFromSolaceBroker initiated");

        // Set environment
        ArrayList<String> environment = new ArrayList<>();
        environment.add(Constants.GATEWAY_ENVIRONMENT);
        environment.add(SOLACE_ENVIRONMENT);

        // Create additional properties object
        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("provider", user.getUserName());
        additionalPropertiesObj.put("name", solaceApi3Name);
        additionalPropertiesObj.put("context", solaceApi3Context);
        additionalPropertiesObj.put("version", solaceApiVersion);
        additionalPropertiesObj.put("gatewayVendor", solaceGatewayVendor);
        additionalPropertiesObj.put("type", "WEBSUB");

        String solaceDefinitionPath = FrameworkPathUtil.getSystemResourceLocation() + "solace"
                + File.separator + "APIMaintenance.yml";
        File file = new File(solaceDefinitionPath);

        // Validate Async Definition of the Solace Specification
        AsyncAPISpecificationValidationResponseDTO asyncAPISpecificationValidationResponseDTO = restAPIPublisher.
                validateAsyncAPISchemaDefinition(null, file);
        assertTrue(asyncAPISpecificationValidationResponseDTO.isIsValid());

        // Create API by importing the Solace  definition
        APIDTO solaceApiDto = restAPIPublisher
                .importAsyncAPISchemaDefinition(file, null, additionalPropertiesObj.toString());

        // Make sure API is created properly
        solaceApi3Id = solaceApiDto.getId();
        assertEquals(solaceApiDto.getName(), solaceApi3Name);
        assertEquals(solaceApiDto.getContext(), "/" + solaceApi3Context);

        // Assert Solace specific API properties
        assertEquals(solaceApiDto.getGatewayVendor(), solaceGatewayVendor);
        List<String> asyncProtocolsForAssertion = new ArrayList<String>();
        asyncProtocolsForAssertion.add("mqtt");
        asyncProtocolsForAssertion.add("http");
        assertEquals(solaceApiDto.getAsyncTransportProtocols(), asyncProtocolsForAssertion);

        // Deploy API to Solace broker and publish API
        createSolaceAPIRevisionAndDeployToSolaceBroker(solaceApi3Id, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatusToPublish(solaceApi3Id, false);
        waitForAPIDeploymentSync(user.getUserName(), solaceApi3Name, solaceApiVersion,
                APIMIntegrationConstants.IS_API_EXISTS);

        // Delete API from publisher portal
        HttpResponse apiDeleteResponse = restAPIPublisher.deleteAPI(solaceApi3Id);
        assertEquals(apiDeleteResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to delete API :" + apiDeleteResponse.getData());

        // Assert that related artifacts are not found in solace broker
        HttpClient httpClient = HttpClients.createDefault();
        String apiNameForRegistration = solaceApi3Name + "-" + solaceApiVersion;
        HttpGet httpGet = new HttpGet(SOLACE_BASE_URL + "/" + SOLACE_ORGANIZATION + "/apis/" + apiNameForRegistration);
        String toEncode = SOLACE_USER_NAME + ":" + SOLACE_PASSWORD;
        httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes()));

        org.apache.http.HttpResponse response = httpClient.execute(httpGet);
        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                    "Invocation fails for GET request");
    }

    /**
     * Create API Revision and Deploy to Solace broker using REST API.
     *
     * @param apiId          - API UUID
     * @param restAPIPublisher -  Instance of APIPublisherRestClient
     */
    protected String createSolaceAPIRevisionAndDeployToSolaceBroker(String apiId, RestAPIPublisherImpl restAPIPublisher)
            throws ApiException, JSONException, APIManagerIntegrationTestException {
        int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
        int HTTP_RESPONSE_CODE_CREATED = Response.Status.CREATED.getStatusCode();
        String revisionUUID = null;

        //Add the API Revision using the API publisher.
        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(apiId);
        apiRevisionRequest.setDescription("Solace API Test Revision 1");

        HttpResponse apiRevisionResponse = restAPIPublisher.addAPIRevision(apiRevisionRequest);
        assertEquals(apiRevisionResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Create API Response Code is invalid." + apiRevisionResponse.getData());

        // Retrieve Revision Info
        HttpResponse apiRevisionsGetResponse = restAPIPublisher.getAPIRevisions(apiId,null);
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to retrieve revisions" + apiRevisionsGetResponse.getData());
        List<JSONObject> revisionList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(apiRevisionsGetResponse.getData());
        JSONArray arrayList = jsonObject.getJSONArray("list");
        for (int i = 0, l = arrayList.length(); i < l; i++) {
            revisionList.add(arrayList.getJSONObject(i));
        }
        for (JSONObject revision :revisionList) {
            revisionUUID = revision.getString("id");
        }

        // Deploy Revision to Solace broker
        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName(SOLACE_ENVIRONMENT);
        apiRevisionDeployRequest.setVhost("localhost");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionsDeployResponse = restAPIPublisher.deployAPIRevision(apiId, revisionUUID,
                apiRevisionDeployRequestList, "API");
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to deploy API Revisions:" +apiRevisionsDeployResponse.getData());
        return  revisionUUID;
    }

    /**
     * Undeploy and Delete API Revisions using REST API from Solace broker.
     *
     * @param apiId          - API UUID
     * @param restAPIPublisher -  Instance of APIPublisherRestClient
     */
    protected String undeployAndDeleteSolaceAPIRevisionsFromSolaceBroker(String apiId, RestAPIPublisherImpl restAPIPublisher)
            throws ApiException, JSONException, XPathExpressionException, APIManagerIntegrationTestException {
        int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
        int HTTP_RESPONSE_CODE_CREATED = Response.Status.CREATED.getStatusCode();
        String revisionUUID = null;

        // Get Deployed Revisions
        HttpResponse apiRevisionsGetResponse = restAPIPublisher.getAPIRevisions(apiId,"deployed:true");
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to retrieve revisions" + apiRevisionsGetResponse.getData());
        List<JSONObject> revisionList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(apiRevisionsGetResponse.getData());

        JSONArray arrayList = jsonObject.getJSONArray("list");
        for (int i = 0, l = arrayList.length(); i < l; i++) {
            revisionList.add(arrayList.getJSONObject(i));
        }
        for (JSONObject revision :revisionList) {
            revisionUUID = revision.getString("id");
        }
        if (revisionUUID == null) {
            return null;
        }

        // Undeploy Revisions
        List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionUnDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionUnDeployRequest.setName(SOLACE_ENVIRONMENT);
        apiRevisionUnDeployRequest.setVhost(null);
        apiRevisionUnDeployRequest.setDisplayOnDevportal(true);
        apiRevisionUndeployRequestList.add(apiRevisionUnDeployRequest);
        HttpResponse apiRevisionsUnDeployResponse = restAPIPublisher.undeployAPIRevision(apiId, revisionUUID,
                apiRevisionUndeployRequestList);
        assertEquals(apiRevisionsUnDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to Undeploy API Revisions:" + apiRevisionsUnDeployResponse.getData());

        // Get Revisions
        HttpResponse apiRevisionsFullGetResponse = restAPIPublisher.getAPIRevisions(apiId,null);
        assertEquals(apiRevisionsFullGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to retrieve revisions" + apiRevisionsFullGetResponse.getData());
        List<JSONObject> revisionFullList = new ArrayList<>();
        JSONObject jsonFullObject = new JSONObject(apiRevisionsFullGetResponse.getData());

        JSONArray arrayFullList = jsonFullObject.getJSONArray("list");
        for (int i = 0, l = arrayFullList.length(); i < l; i++) {
            revisionFullList.add(arrayFullList.getJSONObject(i));
        }
        for (JSONObject revision :revisionFullList) {
            revisionUUID = revision.getString("id");
            HttpResponse apiRevisionsDeleteResponse = restAPIPublisher.deleteAPIRevision(apiId, revisionUUID);
            assertEquals(apiRevisionsDeleteResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                    "Unable to delete API Revisions:" + apiRevisionsDeleteResponse.getData());
        }
        //Waiting for API un-deployment
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apiDto = g.fromJson(response.getData(), APIDTO.class);
        waitForAPIDeploymentSync(user.getUserName(), apiDto.getName(), apiDto.getVersion(),
                APIMIntegrationConstants.IS_API_NOT_EXISTS);

        return  revisionUUID;
    }

    @AfterClass(alwaysRun = true)
    void destroy() throws Exception {
        restAPIPublisher.deleteAPI(solaceApi1Id);
        restAPIPublisher.deleteAPI(solaceApi2Id);
        solaceWireMockServer.stop();
    }

}

