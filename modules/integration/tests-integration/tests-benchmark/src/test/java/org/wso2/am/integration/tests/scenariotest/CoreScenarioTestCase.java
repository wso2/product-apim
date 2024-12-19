package org.wso2.am.integration.tests.scenariotest;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.backend.service.AbstractSSLServer;
import org.wso2.am.integration.backend.service.SSLServerSendImmediateResponse;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.benchmarktest.BenchmarkUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.*;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class CoreScenarioTestCase extends APIMIntegrationBaseTest {
    protected static final String TIER_UNLIMITED = "Unlimited";
    private static final String JDBC_METRIC = "jdbc";
    private static final String EXTERNAL_API_METRIC = "http";
    private final String API_END_POINT_POSTFIX_URL = "am/sample/pizzashack/v1/api/menu";
    private final String API_VERSION_1_0_0 = "1.0.0";
    BenchmarkUtils benchmarkUtils = new BenchmarkUtils();
    List<String> idList = new ArrayList<String>();
    List<String> apiIdList = new ArrayList<>();
    private String apiUUID;
    private String applicationID;
    private String testName;
    private String context;
    private LocalTime startTime;
    private String providerName;
    private String apiEndPointUrl;
    private String scenario;
    String Content1MB;

    {
        try {
            Content1MB = readThisFile("../src/test/java/org/wso2/am/integration/tests/scenariotest/1MB.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    String Content2KB;

    {
        try {
            Content2KB = readThisFile("../src/test/java/org/wso2/am/integration/tests/scenariotest/2KB.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Factory(dataProvider = "userModeDataProvider")
    public CoreScenarioTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
//                new Object[]{TestUserMode.TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        apiEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
    }

    @Test
    public void invokeCreatedApis(Method method)
            throws Exception {

        scenario = "INVOKE_API";
        AbstractSSLServer server = new SSLServerSendImmediateResponse();

        ArrayList grantTypes = new ArrayList();
        Map<String, String> requestHeaders;

        apiUUID = createAnApi("TestAPI", "test");
        apiIdList.add(apiUUID);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        restAPIPublisher.changeAPILifeCycleStatus(apiUUID, APILifeCycleAction.PUBLISH.getAction(), null);
        createAPIRevisionAndDeployUsingRest(apiUUID, restAPIPublisher);
        waitForAPIDeployment();
        HttpResponse applicationResponse = restAPIStore.createApplication("Application_" + testName,
                "Test Application For Benchmark",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationID = applicationResponse.getData();
        restAPIStore.subscribeToAPI(apiUUID, applicationID, TIER_UNLIMITED);
        ApplicationKeyDTO apiKeyDTO = restAPIStore
                .generateKeys(applicationID, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null,
                        grantTypes);
        String accessToken = apiKeyDTO.getToken().getAccessToken();
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("activityID", System.getProperty("testName"));



        //Tests
        SimpleNonBlockingClient client2kb = new SimpleNonBlockingClient("localhost",8743,accessToken);
        SimpleNonBlockingClient client1mb = new SimpleNonBlockingClient("localhost",8743,accessToken);
        NonBlockingClientSendLessContent client2kbLessContent = new NonBlockingClientSendLessContent("localhost",8743,accessToken);
        NonBlockingClientSendLessContent client1mbLessContent = new NonBlockingClientSendLessContent("localhost",8743,accessToken);

        //Client send the full request content
        server.run(8100,Content2KB, 200);
        client2kb.run(Content2KB,RequestMethod.POST);
        client1mb.run(Content1MB,RequestMethod.POST);
        server.stop();

        server.run(8100,Content2KB, 300);
        client2kb.run(Content2KB,RequestMethod.POST);
        client1mb.run(Content1MB,RequestMethod.POST);
        server.stop();

        server.run(8100,Content2KB, 400);
        client2kb.run(Content2KB,RequestMethod.POST);
        client1mb.run(Content1MB,RequestMethod.POST);
        server.stop();

        server.run(8100,Content2KB, 503);
        client2kb.run(Content2KB,RequestMethod.POST);
        client1mb.run(Content1MB,RequestMethod.POST);
        server.stop();

        //Client sends less content than mentioned in the content-length header and server sends immediate response without payload

        server.run(8100,"", 200);
        client2kbLessContent.run(Content2KB, RequestMethod.POST);
        client2kbLessContent.run(Content2KB,RequestMethod.POST);
        client1mbLessContent.run(Content1MB,RequestMethod.POST);
        server.stop();

        server.run(8100,"", 300);
        client2kbLessContent.run(Content2KB,RequestMethod.POST);
        client1mbLessContent.run(Content1MB,RequestMethod.POST);
        server.stop();

        server.run(8100,"", 400);
        client2kbLessContent.run(Content2KB,RequestMethod.POST);
        client1mbLessContent.run(Content1MB,RequestMethod.POST);
        server.stop();

        server.run(8100,"", 503);
        client2kbLessContent.run(Content2KB,RequestMethod.POST);
        client1mbLessContent.run(Content1MB,RequestMethod.POST);
        server.stop();

        //Client sends less content than mentioned in the content-length header

        server.run(8100,Content2KB, 200);
        client2kbLessContent.run(Content2KB,RequestMethod.POST);
        client1mbLessContent.run(Content1MB,RequestMethod.POST);
        server.stop();

        server.run(8100,Content2KB, 300);
        client2kbLessContent.run(Content2KB,RequestMethod.POST);
        client1mbLessContent.run(Content1MB,RequestMethod.POST);
        server.stop();

        server.run(8100,Content2KB, 400);
        client2kbLessContent.run(Content2KB,RequestMethod.POST);
        client1mbLessContent.run(Content1MB,RequestMethod.POST);
        server.stop();

        server.run(8100,Content2KB, 503);
        client2kbLessContent.run(Content2KB,RequestMethod.POST);
        client1mbLessContent.run(Content1MB,RequestMethod.POST);
        server.stop();

        //

        restAPIStore.deleteApplication(applicationID);
    }

    public String createAnApi(String apiName, String context)
            throws APIManagerIntegrationTestException, ApiException, MalformedURLException {
        //Create the api creation request object
        APIRequest apiRequest;
        apiRequest = new APIRequest(apiName, context, new URL("https://localhost:8100/"));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(providerName);

        APIOperationsDTO apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb("GET");
        apiOperationsDTO1.setTarget("/*");
        apiOperationsDTO1.setAuthType("Application & Application User");
        apiOperationsDTO1.setThrottlingPolicy("Unlimited");

        APIOperationsDTO apiOperationsDTO2 = new APIOperationsDTO();
        apiOperationsDTO2.setVerb("POST");
        apiOperationsDTO2.setTarget("/*");
        apiOperationsDTO2.setAuthType("Application & Application User");
        apiOperationsDTO2.setThrottlingPolicy("Unlimited");


        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        operationsDTOS.add(apiOperationsDTO2);
        apiRequest.setOperationsDTOS(operationsDTOS);
        apiRequest.setVisibility("public");


        //Add the API using the API publisher.
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiUUID = apiResponse.getData();
        assertEquals(apiResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Create API Response Code is invalid." + apiUUID);
        idList.add(apiUUID);
        return apiUUID;
    }

    public String readThisFile(String fileLocation) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileLocation));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            String everything = sb.toString();
            return everything;
        } finally {
            br.close();
        }
    }

}