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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.AsyncAPISpecificationValidationResponseDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.testng.Assert.*;

public class SolaceDeveloperPortalTestCase extends APIManagerLifecycleBaseTest {

    private final Log log = LogFactory.getLog(SolaceDeveloperPortalTestCase.class);

    protected static final int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_CREATED = Response.Status.CREATED.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_NO_CONTENT = Response.Status.NO_CONTENT.getStatusCode();

    // Solace broker config related values
    protected static final String SOLACE_USER_NAME = "testUser";
    protected static final String SOLACE_PASSWORD = "testPassword";
    protected static final String SOLACE_ORGANIZATION = "TestWSO2";
    protected static final String SOLACE_DEVELOPER_USER_NAME = "devPortTestEnv";
    protected static final String SOLACE_ENVIRONMENT = "devportalEnv";
    protected static final String SOLACE_BASE_URL = "http://localhost:9960/";

    private ServerConfigurationManager serverConfigurationManager;
    private static WireMockServer solaceWireMockServer;
    private ArrayList<String> grantTypes;

    // Test artifact constants
    private String newSubApplicationId;
    private SubscriptionDTO subscriptionDTO1;
    private SubscriptionDTO subscriptionDTO2;
    private String solaceApiId;
    private final String solaceApiName = "SolaceSampleAPI";
    private final String solaceApiContext = "SolaceSampleAPI";
    private final String solaceApiVersion = "1.0";
    private final String solaceApiProductName = SOLACE_ENVIRONMENT + "-" + solaceApiName + "-" + solaceApiContext + "-"
            + solaceApiVersion;
    private String solaceApiId2;
    private final String solaceApiName2 = "SolaceSampleAPI2";
    private final String solaceApiContext2 = "SolaceSampleAPI2";
    private final String solaceApiVersion2 = "1.0";
    private final String solaceApiProductName2 = SOLACE_ENVIRONMENT + "-" + solaceApiName2 + "-" + solaceApiContext2 + "-"
            + solaceApiVersion;
    private final String solaceGatewayVendor = "solace";


    @Factory(dataProvider = "userModeDataProvider")
    public SolaceDeveloperPortalTestCase(TestUserMode userMode) {
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
                getAMResourceLocation() + File.separator + "solace" + File.separator + "devportal_tests" +
                        File.separator + "deployment.toml"));
        String tenantDomain = storeContext.getContextTenant().getDomain();
        String userName = keyManagerContext.getContextTenant().getTenantAdmin().getUserName();
        String password = keyManagerContext.getContextTenant().getTenantAdmin().getPassword();
        grantTypes = new ArrayList<>();
        String publisherURLHttps = publisherUrls.getWebAppURLHttps();

        // Create new Application
        HttpResponse applicationResponse = restAPIStore.createApplication("SolaceNewSubApp",
                "New Subscription for API Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        newSubApplicationId = applicationResponse.getData();

        // Load request/response body
        String solaceDefinitionPath = FrameworkPathUtil.getSystemResourceLocation() + "solace"
                + File.separator + "APIMaintenance.yml";
        String solaceDefinition = readFile(solaceDefinitionPath);

        // Start wiremock server
        startSolaceWiremockServer();
    }

    public void startSolaceWiremockServer() {
        int wireMockPort = 9960;
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

        // Get created APIProduct in Solace broker not found
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apiProducts/" + solaceApiProductName)
                        .inScenario("Get APIProduct1")
                        .whenScenarioStateIs(STARTED)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NOT_FOUND)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo("Cause Success")
        );
        // Get created APIProduct in Solace broker success
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apiProducts/" + solaceApiProductName)
                        .inScenario("Get APIProduct1")
                        .whenScenarioStateIs("Cause Success")
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_OK)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo("Cause Success")
        );

        // Get registered API in solace broker not found
        String apiNameForRegistration = solaceApiName + "-" + solaceApiVersion;
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apis/" + apiNameForRegistration)
                        .inScenario("Get API1")
                        .whenScenarioStateIs(STARTED)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NOT_FOUND)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo("Cause Success")

        );
        // Get registered API in solace broker success
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apis/" + apiNameForRegistration)
                        .inScenario("Get API1")
                        .whenScenarioStateIs("Cause Success")
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NOT_FOUND)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo("Cause Success")
        );

        // Register API in Solace broker
        solaceWireMockServer.stubFor(
                WireMock.post("/" + SOLACE_ORGANIZATION + "/apis/" + apiNameForRegistration)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_CREATED)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
        );


        // Get created APIProduct2 in Solace broker not found
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apiProducts/" + solaceApiProductName2)
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
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apiProducts/" + solaceApiProductName2)
                        .inScenario("Get APIProduct2")
                        .whenScenarioStateIs("Cause Success")
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_OK)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo("Cause Success")
        );

        // Get registered API2 in solace broker not found
        String apiNameForRegistration2 = solaceApiName2 + "-" + solaceApiVersion2;
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apis/" + apiNameForRegistration2)
                        .inScenario("Get API2")
                        .whenScenarioStateIs(STARTED)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NOT_FOUND)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo("Cause Success")

        );
        // Get registered API in solace broker success
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/apis/" + apiNameForRegistration2)
                        .inScenario("Get API2")
                        .whenScenarioStateIs("Cause Success")
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NOT_FOUND)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo("Cause Success")
        );

        // Register API in Solace broker
        solaceWireMockServer.stubFor(
                WireMock.post("/" + SOLACE_ORGANIZATION + "/apis/" + apiNameForRegistration)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_CREATED)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
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

        // Create Applications in Solace broker
        solaceWireMockServer.stubFor(
                WireMock.post("/" + SOLACE_ORGANIZATION + "/developers/" + SOLACE_DEVELOPER_USER_NAME + "/apps/")
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_CREATED)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
        );

        // Get created Application in Solace broker not found
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/developers/" + SOLACE_DEVELOPER_USER_NAME + "/apps/" + newSubApplicationId)
                        .inScenario("Get newSubApplicationId")
                        .whenScenarioStateIs(STARTED)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NOT_FOUND)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo("Cause Success")
        );
        // Get created Application in Solace broker success
        solaceWireMockServer.stubFor(
                WireMock.get("/" + SOLACE_ORGANIZATION + "/developers/" + SOLACE_DEVELOPER_USER_NAME + "/apps/" + newSubApplicationId)
                        .inScenario("Get newSubApplicationId")
                        .whenScenarioStateIs("Cause Success")
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_OK)
                                .withBody("{\n" +
                                        "    \"name\": \"41756660-9948-4b6a-8040-571122e711dd\",\n" +
                                        "    \"displayName\": \"TestApp\",\n" +
                                        "    \"apiProducts\": [\n" +
                                        "        \"devportalEnv-SolaceSampleAPI-SolaceSampleAPI-1.0\",\n" +
                                        "        \"devportalEnv-SolaceSampleAPI-SolaceSampleAPI-1.0\"\n" +
                                        "    ]\n" +
                                        "}")
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
                        .willSetStateTo("Cause Success")
        );

        // Update created Application in Solace broker success
        solaceWireMockServer.stubFor(
                WireMock.put("/" + SOLACE_ORGANIZATION +
                                "/developers/" + SOLACE_DEVELOPER_USER_NAME + "/apps/" + newSubApplicationId)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_OK)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
        );

        // Delete created application
        solaceWireMockServer.stubFor(
                WireMock.delete("/" + SOLACE_ORGANIZATION +
                                "/developers/" + SOLACE_DEVELOPER_USER_NAME + "/apps/" + newSubApplicationId)
                        .withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((toEncode).getBytes())))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                                .withStatus(HTTP_RESPONSE_CODE_NO_CONTENT)
                                .withHeader("Content-Type", "application/json", "charset=utf-8"))
        );

        // Start mock server
        solaceWireMockServer.start();
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

    @Test(groups = {"wso2.am"}, description = "Show the published Solace API details in the Developer Portal")
    public void showSolaceApiInDeveloperPortal() throws Exception {
        log.info("showSolaceApiInDeveloperPortal initiated");

        // Set environment
        ArrayList<String> environment = new ArrayList<>();
        environment.add(Constants.GATEWAY_ENVIRONMENT);
        environment.add(SOLACE_ENVIRONMENT);

        // Create additional properties object
        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("provider", user.getUserName());
        additionalPropertiesObj.put("name", solaceApiName);
        additionalPropertiesObj.put("context", solaceApiContext);
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
        solaceApiId = solaceApiDto.getId();
        assertEquals(solaceApiDto.getName(), solaceApiName);
        assertEquals(solaceApiDto.getContext(), "/" + solaceApiContext);

        // Assert Solace specific API properties
        assertEquals(solaceApiDto.getGatewayVendor(), solaceGatewayVendor);
        List<String> asyncProtocolsForAssertion = new ArrayList<String>();
        asyncProtocolsForAssertion.add("mqtt");
        asyncProtocolsForAssertion.add("http");
        assertEquals(solaceApiDto.getAsyncTransportProtocols(), asyncProtocolsForAssertion);

        // Deploy the revision and publish API
        createSolaceAPIRevisionAndDeployToSolaceBroker(solaceApiId, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatusToPublish(solaceApiId, false);
        waitForAPIDeploymentSync(user.getUserName(), solaceApiName, solaceApiVersion,
                APIMIntegrationConstants.IS_API_EXISTS);

        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO storeAPI = restAPIStore.getAPI(solaceApiId);
        // Assert that Solace API is properly retrieved in store
        assertEquals(storeAPI.getName(), solaceApiName);
        assertEquals(storeAPI.getContext(), "/" + solaceApiContext + "/" + solaceApiVersion);
        assertEquals(storeAPI.getGatewayVendor(), solaceGatewayVendor);
        Assert.assertNotNull(storeAPI.getAsyncTransportProtocols());
    }

    @Test(groups = {"wso2.am"}, description = "Create a new subscription for Solace API",
            dependsOnMethods = "showSolaceApiInDeveloperPortal")
    public void testSolaceAPINewSubscriptionCreation() throws Exception {

        log.info("testSolaceAPINewSubscriptionCreation initiated");

        // Add subscription
        subscriptionDTO1 = restAPIStore.subscribeToAPI(solaceApiId, newSubApplicationId,
                APIMIntegrationConstants.API_TIER.ASYNC_UNLIMITED);

        ApplicationDTO newSubApplication = restAPIStore.getApplicationById(newSubApplicationId);
        assertNotNull(newSubApplication.getSubscriptionCount());
        assertEquals(newSubApplication.getSubscriptionCount().intValue(),1);
    }

    @Test(groups = {"wso2.am"}, description = "Create a new application and generate Keys for Solace subscription",
            dependsOnMethods = "testSolaceAPINewSubscriptionCreation")
    public void testGenerateKeysForSolaceSubscriptions() throws Exception {

        log.info("testGenerateKeysForSolaceSubscriptions initiated");

        // Generate Keys for Solace subscription
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(newSubApplicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        Assert.assertNotNull("Access Token not found ", accessToken);
    }

    @Test(groups = {"wso2.am"}, description = "Create a new subscription with an application already has Solace subscriptions",
            dependsOnMethods = "testGenerateKeysForSolaceSubscriptions")
    public void testAddSolaceSubscriptionToApplicationWithExistingSolaceSubscriptions() throws Exception {

        log.info("testAddSolaceSubscriptionToApplicationWithExistingSolaceSubscriptions initiated");

        // Create additional properties object
        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("provider", user.getUserName());
        additionalPropertiesObj.put("name", solaceApiName2);
        additionalPropertiesObj.put("context", solaceApiContext2);
        additionalPropertiesObj.put("version", solaceApiVersion2);
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
        solaceApiId2 = solaceApiDto.getId();
        assertEquals(solaceApiDto.getGatewayVendor(), solaceGatewayVendor);

        // Deploy the revision and publish API
        createSolaceAPIRevisionAndDeployToSolaceBroker(solaceApiId2, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatusToPublish(solaceApiId2, false);
        waitForAPIDeploymentSync(user.getUserName(), solaceApiName2, solaceApiVersion2,
                APIMIntegrationConstants.IS_API_EXISTS);

        // Add subscription
        subscriptionDTO2 = restAPIStore.subscribeToAPI(solaceApiId2, newSubApplicationId,
                APIMIntegrationConstants.API_TIER.ASYNC_UNLIMITED);

        // Assert new Subscription count
        ApplicationDTO newSubApplication = restAPIStore.getApplicationById(newSubApplicationId);
        assertEquals(newSubApplication.getSubscriptionCount().intValue(),2);
    }

    @Test(groups = {"wso2.am"}, description = "Remove created subscription from an application that already has Solace subscriptions",
            dependsOnMethods = "testAddSolaceSubscriptionToApplicationWithExistingSolaceSubscriptions")
    public void testRemoveSolaceSubscriptionFromApplicationWithExistingSolaceSubscriptions() throws Exception {

        log.info("testAddSolaceSubscriptionToApplicationWithExistingSolaceSubscriptions initiated");

        // Remove subscription
        restAPIStore.removeSubscription(subscriptionDTO2);

        // Assert new Subscription count
        ApplicationDTO newSubApplication = restAPIStore.getApplicationById(newSubApplicationId);
        assertEquals(newSubApplication.getSubscriptionCount().intValue(),1);
    }

    @Test(groups = {"wso2.am"}, description = "Remove all the Solace subscriptions attached to an application and delete" +
            " that from solace broker ", dependsOnMethods = "testRemoveSolaceSubscriptionFromApplicationWithExistingSolaceSubscriptions")
    public void testRemoveAllSolaceSubscriptionsFromApplication() throws Exception {

        log.info("testRemoveAllSolaceSubscriptionsFromApplication initiated");

        // Remove subscription
        restAPIStore.removeSubscription(subscriptionDTO1);

        // Assert new Subscription count
        ApplicationDTO newSubApplication = restAPIStore.getApplicationById(newSubApplicationId);
        assertEquals(newSubApplication.getSubscriptionCount().intValue(),0);
    }

    @Test(groups = {"wso2.am"}, description = "Delete an application that has a solace subscription attached to it"
            , dependsOnMethods = "testRemoveAllSolaceSubscriptionsFromApplication")
    public void testDeleteApplicationWithSolaceSubscription() throws Exception {

        log.info("testDeleteApplicationWithSolaceSubscription initiated");

        // Add subscription
        subscriptionDTO2 = restAPIStore.subscribeToAPI(solaceApiId2, newSubApplicationId,
                APIMIntegrationConstants.API_TIER.ASYNC_UNLIMITED);

        // Assert app deletion
        HttpResponse appDeleteResponse = restAPIStore.deleteApplication(newSubApplicationId);
        assertEquals(appDeleteResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK);
    }

    @AfterClass(alwaysRun = true)
    void destroy() throws Exception {
        restAPIPublisher.deleteAPI(solaceApiId);
        restAPIPublisher.deleteAPI(solaceApiId2);
        solaceWireMockServer.stop();
    }
}
