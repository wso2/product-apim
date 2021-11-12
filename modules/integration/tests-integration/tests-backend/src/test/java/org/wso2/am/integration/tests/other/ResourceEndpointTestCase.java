package org.wso2.am.integration.tests.other;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.*;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.impl.ApiProductTestHelper;
import org.wso2.am.integration.test.impl.ApiTestHelper;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class ResourceEndpointTestCase extends APIManagerLifecycleBaseTest {
    private final Log log = LogFactory.getLog(ResourceEndpointTestCase.class);

    private String resourceEndpointName = "Sample Resource Endpoint";
    private String resourceEndpointUrl = "https://run.mocky.io/v3/c07d060c-7b04-40cc-a418-6dc603b48ca5";
    private String apiId;
    private String resourceEndpointId;
    private String apiRevisionUUID;
    private ApiProductTestHelper apiProductTestHelper;

    @Factory(dataProvider = "userModeDataProvider")
    public ResourceEndpointTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] {
                new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        apiProductTestHelper = new ApiProductTestHelper(restAPIPublisher, restAPIStore);

        String APIName = "ResourceEndpointTestAPI";
        String APIContext = "resource-endpoints";
        String endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api/add";
        String description = "This is resource-endpoint test API created by API manager integration test";
        String providerName = user.getUserName();
        String APIVersion = "1.0.0";

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(endpointUrl));
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setProvider(providerName);

        //add api
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = serviceResponse.getData();

        //publish the api
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);
    }

    @Test(groups = { "wso2.am" }, description = "Test add resource endpoint")
    public void testAddResourceEndpoint() throws Exception {
        ResourceEndpointDTO resourceEndpointDTO = new ResourceEndpointDTO();
        Map<String, String> securityConfig = new HashMap<>();
        securityConfig.put("enabled", "true");
        securityConfig.put("type", "BASIC");
        securityConfig.put("username", "user123");
        securityConfig.put("password", "password123");

        Map<String, String> generalConfig = new HashMap<>();
        securityConfig.put("actionDuration", "30000");
        securityConfig.put("actionSelect", "fault");

        resourceEndpointDTO.setEndpointType(ResourceEndpointDTO.EndpointTypeEnum.HTTP);
        resourceEndpointDTO.setName(resourceEndpointName);
        resourceEndpointDTO.setUrl("https://run.mocky.io/v3/c07d060c-7b04-40cc-a418-6dc603b48ca5");
        resourceEndpointDTO.securityConfig(securityConfig);
        resourceEndpointDTO.setGeneralConfig(generalConfig);
        ResourceEndpointDTO addedResourceEndpointDTO = restAPIPublisher.addResourceEndpoint(apiId, resourceEndpointDTO);
        resourceEndpointId = addedResourceEndpointDTO.getId();
        Assert.assertNotNull(resourceEndpointId, "Resource Endpoint ID cannot be null");
    }

    @Test(groups = { "wso2.am" }, description = "Test get and update resource endpoint",
            dependsOnMethods = "testAddResourceEndpoint")
    public void testGetAndUpdateResourceEndpoint() throws Exception {
        ResourceEndpointDTO resourceEndpointDTO = restAPIPublisher.getResourceEndpoint(apiId, resourceEndpointId);
        Assert.assertEquals(resourceEndpointDTO.getName(), resourceEndpointName);
        Assert.assertEquals(resourceEndpointDTO.getUrl(), resourceEndpointUrl);

        String urlToUpdate = "https://run.mocky.io/v3/d7b75389-bbb3-4127-be13-ec9a8476ca62";
        resourceEndpointDTO.setUrl(urlToUpdate);
        ResourceEndpointDTO updatedResourceEndpointDTO = restAPIPublisher
                .updateResourceEndpoint(apiId, resourceEndpointId, resourceEndpointDTO);
        Assert.assertEquals(updatedResourceEndpointDTO.getName(), resourceEndpointName,
                "Resource endpoint name does not match with the expected name");
        Assert.assertEquals(updatedResourceEndpointDTO.getUrl(), urlToUpdate,
                "Resource endpoint url does not match with the expected url");

    }

    @Test(groups = { "wso2.am" }, description = "Test get all resource endpoints of API",
            dependsOnMethods = "testGetAndUpdateResourceEndpoint")
    public void testGetAllResourceEndpointsOfAPI() throws Exception {
        ResourceEndpointListDTO resourceEndpointListDTO = restAPIPublisher.testGetAllResourceEndpointsOfAPI(apiId);
        Assert.assertEquals(resourceEndpointListDTO.getList().size(), 1,
                "APIs resource endpoint count does not match the expected count");
    }

    @Test(groups = { "wso2.am" }, description = "Delete resource endpoint",
            dependsOnMethods = "testGetAllResourceEndpointsOfAPI")
    public void testAttachEndpointToAPIOperationPolicy() throws Exception {
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(response.getData(), APIDTO.class);

        List<APIOperationsDTO> operationsDTOList = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        OperationPolicyDTO operationPolicyDTO = new OperationPolicyDTO();
        APIOperationPoliciesDTO operationPoliciesDTO = new APIOperationPoliciesDTO();

        Map<String, String> parameters = new HashMap<>();
        parameters.put("endpointId", resourceEndpointId);
        operationPolicyDTO.setPolicyType(OperationPolicyDTO.PolicyTypeEnum.CHANGE_ENDPOINT);
        operationPolicyDTO.setParameters(parameters);

        List<OperationPolicyDTO> inPolicies = new ArrayList<>();
        inPolicies.add(operationPolicyDTO);
        operationPoliciesDTO.setIn(inPolicies);

        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget("/resource");
        apiOperationsDTO.setOperationPolicies(operationPoliciesDTO);
        operationsDTOList.add(apiOperationsDTO);
        apidto.setOperations(operationsDTOList);

        restAPIPublisher.updateAPI(apidto);

        ResourceEndpointDTO endpointDTO = restAPIPublisher.getResourceEndpoint(apiId, resourceEndpointId);
        Assert.assertEquals(endpointDTO.getUsageCount().intValue(), 1);
    }

    @Test
    (groups = {"wso2.am" }, description = "Delete resource endpoint", dependsOnMethods =
            "testAttachEndpointToAPIOperationPolicy")
    public void testIncludeOperationWithOperationPolicyInAPIProduct() throws Exception {
        List<APIDTO> apisToBeUsed = new ArrayList<>();
        HttpResponse apiResponse = restAPIPublisher.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apiDTO = gson.fromJson(apiResponse.getData(), APIDTO.class);
        apisToBeUsed.add(apiDTO);
        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        APIProductDTO createdAPIProductDTO = apiProductTestHelper
                .createAPIProductInPublisher(user.getUserName(), "TestProduct", "/op-policy-test", apisToBeUsed,
                        policies);
        OperationPolicyDTO operationPolicy = createdAPIProductDTO.getApis().get(0).getOperations().get(0)
                .getOperationPolicies().getIn().get(0);
        Assert.assertNotNull(operationPolicy);
    }

    @Test(groups = { "wso2.am" }, description = "Create Revision of API that has a CHANGE_ENDPOINT policy",
            dependsOnMethods = "testAttachEndpointToAPIOperationPolicy")
    public void testCreateAndDeployRevisionOfAPIWithChangeEndpointPolicy() throws Exception {
        APIRevisionDTO apiRevisionDTO = restAPIPublisher.addAPIRevision(apiId);
        Assert.assertNotNull(apiRevisionDTO);

        apiRevisionUUID = apiRevisionDTO.getId();
        HttpResponse response = restAPIPublisher.getAPI(apiRevisionUUID);
        APIDTO apidto = new Gson().fromJson(response.getData(), APIDTO.class);
        List<APIOperationsDTO> operationsDTOList = apidto.getOperations();

        for (APIOperationsDTO operation : operationsDTOList) {
            String verb = operation.getVerb();
            String urlPattern = operation.getTarget();
            if ("GET".equals(verb) && "/resource".equals(urlPattern)) {
                APIOperationPoliciesDTO operationPolicies = operation.getOperationPolicies();
                Assert.assertEquals(operationPolicies.getIn().get(0).getPolicyType(),
                        OperationPolicyDTO.PolicyTypeEnum.CHANGE_ENDPOINT,
                        "Change Endpoint policy is not attached to the API product resource");
            }
        }

        //Deploy Revision
        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionDeployRequest.setVhost("localhost");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionsDeployResponse = restAPIPublisher.deployAPIRevision(apiId, apiRevisionUUID,
                apiRevisionDeployRequestList,"API");
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to deploy API Revision:" + apiRevisionsDeployResponse.getData());

        //Undeploy Revision
        List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionUnDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionUnDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionUndeployRequestList.add(apiRevisionUnDeployRequest);
        HttpResponse apiRevisionsUnDeployResponse = restAPIPublisher.undeployAPIRevision(apiId, apiRevisionUUID,
                apiRevisionUndeployRequestList);
        assertEquals(apiRevisionsUnDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to Undeploy API Revision:" + apiRevisionsUnDeployResponse.getData());
    }

    @Test(groups = { "wso2.am" }, description = "Restore Revision of API that has a CHANGE_ENDPOINT policy",
            dependsOnMethods = "testCreateAndDeployRevisionOfAPIWithChangeEndpointPolicy")
    public void testRestoreRevisionOfAPIWithChangeEndpointPolicy() throws Exception {
        //Remove change_endpoint policy mapping from current API
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(response.getData(), APIDTO.class);

        List<APIOperationsDTO> operationsDTOList = apidto.getOperations();
        operationsDTOList.get(0).setOperationPolicies(new APIOperationPoliciesDTO());
        restAPIPublisher.updateAPI(apidto);

        HttpResponse updatedAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO updatedAPIDTO = new Gson().fromJson(updatedAPIResponse.getData(), APIDTO.class);
        Assert.assertEquals(updatedAPIDTO.getOperations().get(0).getOperationPolicies().getIn().size(), 0);
        Assert.assertEquals(updatedAPIDTO.getOperations().get(0).getOperationPolicies().getOut().size(), 0);

        //Restore revision
        restAPIPublisher.restoreAPIRevision(apiId, apiRevisionUUID);
        HttpResponse restoredAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO restoredAPIDTO = new Gson().fromJson(restoredAPIResponse.getData(), APIDTO.class);
        Assert.assertEquals(restoredAPIDTO.getOperations().get(0).getOperationPolicies().getIn().size(), 1);
        Assert.assertEquals(restoredAPIDTO.getOperations().get(0).getOperationPolicies().getIn().get(0).getPolicyType(),
                OperationPolicyDTO.PolicyTypeEnum.CHANGE_ENDPOINT);
    }
}
