package org.wso2.am.integration.tests.sequence;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.parser.JSONParser;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.MediationPolicyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataHandler;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class DefaultEndpointTestCase extends APIManagerLifecycleBaseTest {

    private final String API_NAME = "AddDynamicEndpointAndInvokeAPITest";
    private final String API_CONTEXT = "AddDynamicEndpointAndInvokeAPI";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "AddDynamicEndpointAndInvokeAPI";
    private final String RESOURCE_PATH_SEPARATOR = "/";
    private String apiId;
    private String applicationID;
    private String subscriptionId1;
    private APIDTO apiDto;
    private String accessToken;
    private String apiEndPointUrl;
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationID = applicationResponse.getData();
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the default endpoint")
    public void testAPIInvocationAfterAddingDynamicEndpoint() throws Exception {
        APIRequest apiRequest;
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));

        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget("/");

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO);
        apiRequest.setOperationsDTOS(operationsDTOS);

        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = apiResponse.getData();

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);

        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);

        HttpResponse subscription = restAPIStore.createSubscription(apiId, applicationID, APIMIntegrationConstants.API_TIER.UNLIMITED);
        subscriptionId1 = subscription.getData();

        ArrayList<String> grantTypes = new ArrayList<>();
        //get access token
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationID, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        accessToken = applicationKeyDTO.getToken().getAccessToken();

        ResourceAdminServiceClient resourceAdminServiceClient =
                new ResourceAdminServiceClient(publisherContext.getContextUrls().getBackEndUrl(), "admin", "admin");

        boolean isResourceAdded = resourceAdminServiceClient.addResource(
                "/_system/governance/apimgt/applicationdata/provider" +
                        RESOURCE_PATH_SEPARATOR + "admin" + RESOURCE_PATH_SEPARATOR + API_NAME + RESOURCE_PATH_SEPARATOR + API_VERSION_1_0_0 + RESOURCE_PATH_SEPARATOR +
                        //"/admin/AddNewMediationAndInvokeAPITest/1.0.0/" +
                        "in/default_endpoint.xml",
                "application/xml",
                "xml files",
                new DataHandler(new URL("file:///" + getAMResourceLocation() + RESOURCE_PATH_SEPARATOR + "sequence"
                        + RESOURCE_PATH_SEPARATOR + "default_endpoint.xml")));

        assertTrue(isResourceAdded, "Adding Mediation Sequence File failed");

        List<MediationPolicyDTO> mediationPolicies = new ArrayList<MediationPolicyDTO>();
        MediationPolicyDTO mediationPolicyDTO = new MediationPolicyDTO();
        mediationPolicyDTO.setName("default_endpoint");
        mediationPolicyDTO.setType("in");
        mediationPolicies.add(mediationPolicyDTO);
        apiRequest.setMediationPolicies(mediationPolicies);

        String endPointString = "{\n" +
                "  \"production_endpoints\": {\n" +
                "    \"template_not_supported\": false,\n" +
                "    \"config\": null,\n" +
                "    \"url\": \"" + apiEndPointUrl + "\"\n" +
                "  },\n" +
                "  \"sandbox_endpoints\": {\n" +
                "    \"url\": \"" + apiEndPointUrl + "\",\n" +
                "    \"config\": null,\n" +
                "    \"template_not_supported\": false\n" +
                "  },\n" +
                "  \"endpoint_type\": \"default\"\n" +
                "}";
        JSONParser parser = new JSONParser();
        apiRequest.setEndpoint((org.json.simple.JSONObject) parser.parse(endPointString));

        restAPIPublisher.updateAPI(apiRequest, apiId);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0));
        request.setHeader("Authorization", "Bearer " + accessToken);
        org.apache.http.HttpResponse response = client.execute(request);

        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");

        assertEquals(response.getHeaders("Content-Type")[0].getValue(), "application/xml");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationID);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }

}