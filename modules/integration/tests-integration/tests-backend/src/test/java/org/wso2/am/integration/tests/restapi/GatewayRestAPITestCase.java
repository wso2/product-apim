package org.wso2.am.integration.tests.restapi;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.gateway.api.v2.dto.APIArtifactDTO;
import org.wso2.am.integration.clients.gateway.api.v2.dto.EndpointsDTO;
import org.wso2.am.integration.clients.gateway.api.v2.dto.LocalEntryDTO;
import org.wso2.am.integration.clients.gateway.api.v2.dto.SequencesDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationPoliciesDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.OperationPolicyDTO;
import org.wso2.am.integration.test.impl.RestAPIGatewayImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class GatewayRestAPITestCase extends APIMIntegrationBaseTest {

    private String apiId;

    @Factory(dataProvider = "userModeDataProvider")
    public GatewayRestAPITestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
                new Object[]{TestUserMode.SUPER_TENANT_USER_STORE_USER},
                new Object[]{TestUserMode.SUPER_TENANT_EMAIL_USER},
                new Object[]{TestUserMode.TENANT_EMAIL_USER},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
    }

    @Test(groups = {"wso2.am"}, description = "Comment Rating Test case")
    public void testGatewayRestAPI() throws Exception {

        String name = "GatewayRestAPITestCase";
        String context = "GatewayRestAPITestCase";
        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";
        String providerName = user.getUserName();
        String version = "1.0.0";
        String apiData;

        APIRequest apiRequest = new APIRequest(name, context, new URL(url));
        apiRequest.setVersion(version);
        apiRequest.setProvider(providerName);

        //add api
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = serviceResponse.getData();

        Map<String, String> commonPolicyMap = restAPIPublisher.getAllCommonOperationPolicies();

        String policyName = "addHeader";
        Map<String, Object> requestAttributeMap = new HashMap<>();
        requestAttributeMap.put("headerName", "RequestTestHeader");
        requestAttributeMap.put("headerValue", "RequestTestValue");

        Map<String, Object> responseAttributeMap = new HashMap<>();
        responseAttributeMap.put("headerName", "ResponseTestHeader");
        responseAttributeMap.put("headerValue", "ResponseTestValue");

        Map<String, Object> faultAttributeMap = new HashMap<>();
        faultAttributeMap.put("headerName", "FaultTestHeader");
        faultAttributeMap.put("headerValue", "FaultTestValue");

        List<OperationPolicyDTO> requestOpList = getPolicyList(policyName, commonPolicyMap, requestAttributeMap);
        requestOpList.get(0).setPolicyVersion("v2");
        List<OperationPolicyDTO> responseOpList = getPolicyList(policyName, commonPolicyMap, responseAttributeMap);
        responseOpList.get(0).setPolicyVersion("v2");
        List<OperationPolicyDTO> faultOpList = getPolicyList(policyName, commonPolicyMap, faultAttributeMap);
        faultOpList.get(0).setPolicyVersion("v2");

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(requestOpList);
        apiOperationPoliciesDTO.setResponse(responseOpList);
        apiOperationPoliciesDTO.setFault(faultOpList);

        APIDTO api = restAPIPublisher.getAPIByID(apiId);
        api.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);

        restAPIPublisher.updateAPI(api);
        //publish the api
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), name, version, APIMIntegrationConstants.IS_API_EXISTS);
        RestAPIGatewayImpl restAPIGateway = new RestAPIGatewayImpl(user.getUserName(), user.getPassword(),
                user.getUserDomain());
        APIArtifactDTO apiArtifactDTO = restAPIGateway.retrieveAPI(name, version);
        Assert.assertNotNull(apiArtifactDTO);
        Assert.assertNotNull(apiArtifactDTO.getApi());
        Assert.assertTrue(apiArtifactDTO.getApi().contains("GatewayRestAPITestCase"));
        Assert.assertTrue(apiArtifactDTO.getApi().contains(apiId));
        // API Content Verified
        LocalEntryDTO localEntryDTO = restAPIGateway.retrieveLocalEntries(name, version);
        Assert.assertNotNull(localEntryDTO);
        Assert.assertNotNull(localEntryDTO.getLocalEntries());
        Assert.assertEquals(localEntryDTO.getLocalEntries().size(), 1);
        Assert.assertTrue(localEntryDTO.getLocalEntries().get(0).contains(apiId));
        // Local Entry Verified
        EndpointsDTO endpointsDTO = restAPIGateway.retrieveEndpoints(name, version);
        Assert.assertNotNull(endpointsDTO);
        Assert.assertNotNull(endpointsDTO.getEndpoints());
        Assert.assertEquals(endpointsDTO.getEndpoints().size(), 2);
        Assert.assertTrue(endpointsDTO.getEndpoints().get(0).contains("production"));
        Assert.assertTrue(endpointsDTO.getEndpoints().get(1).contains("sandbox"));
        Assert.assertTrue(endpointsDTO.getEndpoints().get(0).contains(url));
        Assert.assertTrue(endpointsDTO.getEndpoints().get(1).contains(url));
        // Endpoints Verified
        SequencesDTO sequencesDTO = restAPIGateway.retrieveSequences(name, version);
        Assert.assertNotNull(sequencesDTO);
        Assert.assertNotNull(sequencesDTO.getSequences());
        Assert.assertEquals(sequencesDTO.getSequences().size(), 3);
        for (String sequence : sequencesDTO.getSequences()) {
            if (sequence.contains("--In")) {
                Assert.assertTrue(sequence.contains("RequestTestHeader"));
            }
            if (sequence.contains("--Out")) {
                Assert.assertTrue(sequence.contains("ResponseTestHeader"));
            }
            if (sequence.contains("--Fault")) {
                Assert.assertTrue(sequence.contains("FaultTestHeader"));
            }
        }
    }

    public List<OperationPolicyDTO> getPolicyList(String policyName, Map<String, String> commonPolicyMap,
                                                  Map<String, Object> attributeMap) {

        List<OperationPolicyDTO> policyList = new ArrayList<>();
        OperationPolicyDTO policyDTO = new OperationPolicyDTO();
        policyDTO.setPolicyName(policyName);
        policyDTO.setPolicyId(commonPolicyMap.get(policyName));
        policyDTO.setParameters(attributeMap);
        policyList.add(policyDTO);

        return policyList;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }
}