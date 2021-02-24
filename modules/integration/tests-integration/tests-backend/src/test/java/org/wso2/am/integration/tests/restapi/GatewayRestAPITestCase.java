package org.wso2.am.integration.tests.restapi;

import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.gateway.api.v2.dto.APIArtifactDTO;
import org.wso2.am.integration.clients.gateway.api.v2.dto.EndpointsDTO;
import org.wso2.am.integration.clients.gateway.api.v2.dto.LocalEntryDTO;
import org.wso2.am.integration.clients.gateway.api.v2.dto.SequencesDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.MediationInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.MediationListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.MediationPolicyDTO;
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
import java.util.List;

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
        MediationListDTO mediationListDTO = restAPIPublisher.retrieveMediationPolicies();
        MediationInfoDTO inMediationInfoDTO, outMediationInfoDTO, faultMediationInfoDTO;
        List<MediationPolicyDTO> mediationPolicyDTOS = new ArrayList<>();
        if (mediationListDTO.getList() != null) {
            for (MediationInfoDTO mediationInfoDTO : mediationListDTO.getList()) {
                MediationPolicyDTO mediationPolicyDTO = new MediationPolicyDTO();
                MediationInfoDTO selectedMediationInfo = null;
                if (mediationInfoDTO.getType().equals(MediationInfoDTO.TypeEnum.IN) &&
                        "log_in_message".equals(mediationInfoDTO.getName())) {
                    selectedMediationInfo = mediationInfoDTO;
                }
                if (mediationInfoDTO.getType().equals(MediationInfoDTO.TypeEnum.OUT) &&
                        "log_out_message".equals(mediationInfoDTO.getName())) {
                    selectedMediationInfo = mediationInfoDTO;
                }
                if (mediationInfoDTO.getType().equals(MediationInfoDTO.TypeEnum.FAULT) &&
                        "debug_json_fault".equals(mediationInfoDTO.getName())) {
                    selectedMediationInfo = mediationInfoDTO;
                }
                if (selectedMediationInfo != null) {
                    mediationPolicyDTO.setId(mediationInfoDTO.getId());
                    mediationPolicyDTO.setName(mediationInfoDTO.getName());
                    mediationPolicyDTO.setType(mediationInfoDTO.getType().getValue());
                    mediationPolicyDTO.setShared(true);
                    mediationPolicyDTOS.add(mediationPolicyDTO);
                }
            }
        }
        APIDTO api = restAPIPublisher.getAPIByID(apiId);
        api.setMediationPolicies(mediationPolicyDTOS);
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
                Assert.assertTrue(sequence.contains("IN_MESSAGE"));
            }
            if (sequence.contains("--Out")) {
                Assert.assertTrue(sequence.contains("OUT_MESSAGE"));
            }
            if (sequence.contains("--Fault")) {
                Assert.assertTrue(sequence.contains("ERROR_MESSAGE"));
                Assert.assertTrue(sequence.contains("Correlation_Id"));
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }
}