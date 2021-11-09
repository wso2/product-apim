package org.wso2.am.integration.tests.solace;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.testng.Assert.*;

public class SolaceAPICreateTestCase extends APIManagerLifecycleBaseTest {
    private final Log log = LogFactory.getLog(SolaceAPIImportTestcase.class);

    protected static final int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_CREATED = Response.Status.CREATED.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();

    private final String KEYSTORE_FILE_PATH_CLIENT =
            TestConfigurationProvider.getResourceLocation() + File.separator + "keystores" + File.separator + "products"
                    + File.separator + "wso2carbon.jks";
    private final String TRUSTSTORE_FILE_PATH_CLIENT =
            TestConfigurationProvider.getResourceLocation() + File.separator + "keystores" + File.separator + "products"
                    + File.separator + "client-truststore.jks";

    // Solace broker config related values
    protected static final String SOLACE_USER_NAME = "wso2";
    protected static final String SOLACE_PASSWORD = "hzxVWwFQs2EEK5kK";
    protected static final String SOLACE_ORGANIZATION = "WSO2";
    protected static final String SOLACE_DEVELOPER_USER_NAME = "dev-1";
    protected static final String SOLACE_ENVIRONMENT = "devEnv";
    protected static final String SOLACE_ENVIRONMENT_DISPLAY_NAME = "Development Environment";
    protected static final String SOLACE_ENVIRONMENT_TYPE = "hybrid";
    protected static final String SOLACE_ENVIRONMENT_PROVIDER = "solace";
    protected static final String SOLACE_ENVIRONMENT_DESCRIPTION = "development api gateway broker";

    private ServerConfigurationManager serverConfigurationManager;
    private String apiEndPointUrl;
    private int endpointPort;
    private String endpointHost = "http://localhost";
    private int wireMockPort = 9955;
    private ArrayList<String> grantTypes;
    private String publisherURLHttps;
    private String userName;
    private String password;
    private String tenantDomain;
    private String solaceApi1Id;
    private String solaceDefinition;
    private String solaceDefinitionPath;
    private String solaceApi1Name = "SolaceSampleAPI";
    private String solaceApi1Context = "SolaceSampleAPI";
    private String solaceApiVersion = "1.0";
    private String solaceGatewayVendor = "solace";


    @Factory(dataProvider = "userModeDataProvider")
    public SolaceAPICreateTestCase(TestUserMode userMode) {
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
                getAMResourceLocation() + File.separator + "solace" + File.separator + "deployment.toml"));
        tenantDomain = storeContext.getContextTenant().getDomain();
        userName = keyManagerContext.getContextTenant().getTenantAdmin().getUserName();
        password = keyManagerContext.getContextTenant().getTenantAdmin().getPassword();
        grantTypes = new ArrayList<>();
        publisherURLHttps = publisherUrls.getWebAppURLHttps();

        // Setting the system properties to call the etcd endpoint
        System.setProperty("javax.net.ssl.keyStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.keyStore", KEYSTORE_FILE_PATH_CLIENT);
        System.setProperty("javax.net.ssl.keyStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_FILE_PATH_CLIENT);
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");

        // Load request/response body
        solaceDefinitionPath = FrameworkPathUtil.getSystemResourceLocation() + "solace"
                + File.separator + "APIMaintenance.yml";
        solaceDefinition = readFile(solaceDefinitionPath);

        // Start wiremock server
        SolaceMockServer.startSolaceWiremockServer();

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

    @Test(groups = {"wso2.am"}, description = "Importing Solace Async API definition and create API",
            dependsOnMethods = "testSolaceDefinitionImport")
    public void testSolaceAPIDeployToSolaceBroker() throws Exception {

        log.info("testSolaceAPIDeployToSolaceBroker initiated");

        createSolaceAPIRevisionAndDeployToSolaceBroker(solaceApi1Id, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatusToPublish(solaceApi1Id, false);
        waitForAPIDeploymentSync(user.getUserName(), solaceApi1Name, solaceApiVersion,
                APIMIntegrationConstants.IS_API_EXISTS);
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
        SolaceMockServer.stopSolaceWireMockServer();
    }

}

