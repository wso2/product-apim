/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.restapi;

import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.gateway.api.v1.dto.EndpointsDTO;
import org.wso2.am.integration.clients.gateway.api.v1.dto.LocalEntryDTO;
import org.wso2.am.integration.clients.gateway.api.v1.dto.SequencesDTO;
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

        APIRequest apiRequest = new APIRequest(name, context, new URL(url));
        apiRequest.setVersion(version);
        apiRequest.setProvider(providerName);

        // Create and publish API
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = serviceResponse.getData();
        MediationListDTO mediationListDTO = restAPIPublisher.retrieveMediationPolicies();
        List<MediationPolicyDTO> mediationPolicyDTOS = new ArrayList<>();
        if (mediationListDTO.getList() != null) {
            for (MediationInfoDTO mediationInfoDTO : mediationListDTO.getList()) {
                MediationPolicyDTO mediationPolicyDTO = new MediationPolicyDTO();
                MediationInfoDTO selectedMediationInfo = null;
                MediationInfoDTO.TypeEnum selectedMediationType = null;
                if ("log_in_message".equals(mediationInfoDTO.getName())) {
                    selectedMediationInfo = mediationInfoDTO;
                    selectedMediationType = MediationInfoDTO.TypeEnum.IN;
                }
                if ("log_out_message".equals(mediationInfoDTO.getName())) {
                    selectedMediationInfo = mediationInfoDTO;
                    selectedMediationType = MediationInfoDTO.TypeEnum.OUT;
                }
                if ("debug_json_fault".equals(mediationInfoDTO.getName())) {
                    selectedMediationInfo = mediationInfoDTO;
                    selectedMediationType = MediationInfoDTO.TypeEnum.FAULT;
                }
                if (selectedMediationInfo != null) {
                    mediationPolicyDTO.setId(mediationInfoDTO.getId());
                    mediationPolicyDTO.setName(mediationInfoDTO.getName());
                    mediationPolicyDTO.setType(selectedMediationType.getValue());
                    mediationPolicyDTO.setShared(true);
                    mediationPolicyDTOS.add(mediationPolicyDTO);
                }
            }
        }
        APIDTO api = restAPIPublisher.getAPIByID(apiId);
        api.setMediationPolicies(mediationPolicyDTOS);
        restAPIPublisher.updateAPI(api);
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);
        waitForAPIDeploymentSync(user.getUserName(), name, version, APIMIntegrationConstants.IS_API_EXISTS);
        RestAPIGatewayImpl restAPIGateway = new RestAPIGatewayImpl(user.getUserName(), user.getPassword(),
                user.getUserDomain());

        // Verify API artifact
        org.wso2.am.integration.clients.gateway.api.v1.dto.APIDTO apiDTO =
                restAPIGateway.retrieveAPI(name, version);
        Assert.assertNotNull(apiDTO);
        Assert.assertTrue(apiDTO.getApi().contains("GatewayRestAPITestCase"));
        Assert.assertTrue(apiDTO.getApi().contains(apiId));

        // Verify local entries
        LocalEntryDTO localEntryDTO = restAPIGateway.retrieveLocalEntries(name, version);
        Assert.assertNotNull(localEntryDTO);
        List<String> localEntries = new ArrayList<>();
        localEntries.addAll(localEntryDTO.getDeployedLocalEntries());
        localEntries.addAll(localEntryDTO.getNotdeployedLocalEntries());
        Assert.assertEquals(localEntries.size(), 2);
        Assert.assertTrue(localEntries.get(0).contains(apiId) || localEntries.get(1).contains(apiId));

        // Verify endpoints
        EndpointsDTO endpointsDTO = restAPIGateway.retrieveEndpoints(name, version);
        Assert.assertNotNull(endpointsDTO);
        List<String> endpoints = new ArrayList<>();
        endpoints.addAll(endpointsDTO.getDeployedEndpoints());
        endpoints.addAll(endpointsDTO.getNotdeployedEndpoints());
        Assert.assertEquals(endpoints.size(), 2);
        Assert.assertTrue(endpoints.get(0).contains("production"));
        Assert.assertTrue(endpoints.get(1).contains("sandbox"));
        Assert.assertTrue(endpoints.get(0).contains(url));
        Assert.assertTrue(endpoints.get(1).contains(url));

        // Verify sequences
        SequencesDTO sequencesDTO = restAPIGateway.retrieveSequences(name, version);
        Assert.assertNotNull(sequencesDTO);
        List<String> sequences = new ArrayList<>();
        sequences.addAll(sequencesDTO.getDeployedSequences());
        sequences.addAll(sequencesDTO.getNotdeployedSequences());
        Assert.assertEquals(sequences.size(), 3);
        for (String sequence : sequences) {
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