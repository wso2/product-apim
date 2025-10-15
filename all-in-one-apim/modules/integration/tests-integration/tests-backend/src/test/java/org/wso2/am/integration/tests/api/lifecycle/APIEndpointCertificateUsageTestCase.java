/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.api.lifecycle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIMetadataDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIMetadataListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * This class contains integration tests for endpoint Certificate usage endpoint.
 */
public class APIEndpointCertificateUsageTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(APIEndpointCertificateUsageTestCase.class);

    private final String API_NAME_BASE = "APIEndpointCertificateUsageTestCase";
    private final String API_CONTEXT_BASE = "APIEndpointCertificateUsageTestCase";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_ENDPOINT_URL_BASE_1 = "https://abc.test1.com/resource";
    private final String API_ENDPOINT_URL_BASE_2 = "https://abc.test2.com/resource";
    private final String CERT_ALIAS = "cert00001";
    private int apiCount;
    private ArrayList<String> allAPIs;
    private ArrayList<String> evenAPIs;
    private String selectedEndpointUrl;

    @Factory(dataProvider = "userModeDataProvider")
    public APIEndpointCertificateUsageTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_USER},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init(userMode);

        this.evenAPIs = new ArrayList<>();
        this.allAPIs = new ArrayList<>();

        String providerName = user.getUserName();

        Random random = new Random();
        int limit = random.ints(20, 30).findFirst().getAsInt();

        for (int i = 0; i < limit; i++) {
            String apiName = API_NAME_BASE + "_" + i;
            String apiContext = API_CONTEXT_BASE + "_" + i;
            String apiEndPointUrl = API_ENDPOINT_URL_BASE_1 + i;

            if (i % 2 == 0) {
                apiEndPointUrl = API_ENDPOINT_URL_BASE_2 + i;
                selectedEndpointUrl = apiEndPointUrl;
            }

            //Create the api creation request object
            APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(apiEndPointUrl));
            apiRequest.setVersion(API_VERSION_1_0_0);
            apiRequest.setProvider(providerName);
            apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
            apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

            //Add the API using the API publisher.
            HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
            String apiId = apiResponse.getData();

            allAPIs.add(apiId);
            if (i % 2 == 0) {
                evenAPIs.add(apiId);
            }

            // Wait until Solr and DB get updated
            Thread.sleep(1000);
        }

        apiCount = evenAPIs.size();

        String cert = getAMResourceLocation() + File.separator +
                "endpointCertificate" + File.separator + "endpoint" + ".cer";
        File file = new File(cert);
        restAPIPublisher.uploadEndpointCertificate(file, CERT_ALIAS, selectedEndpointUrl);

        // Wait until Solr and DB get updated
        Thread.sleep(5000);
    }

    @Test(groups = {"wso2.am"}, description = "Invoke API with invalid alias to get Endpoint Certificate usage")
    public void testGetCertificateUsageWithIncorrectAlias() throws ApiException {
        ApiResponse<APIMetadataListDTO> httpResponse = restAPIPublisher.getCertificateUsage("invalidcert", apiCount, 0);
        Assert.assertEquals(httpResponse.getStatusCode(), 200);

        APIMetadataListDTO apiMetadataListDTO = httpResponse.getData();
        List<APIMetadataDTO> apiMetadataDTOS = apiMetadataListDTO.getList();
        Assert.assertEquals(apiMetadataDTOS.size(), 0);
    }

    @Test(groups = {"wso2.am"}, description = "Invoke API to get Certificate usage", dependsOnMethods = {
            "testGetCertificateUsageWithIncorrectAlias"})
    public void testGetCertificateUsageByAlias() throws ApiException {
        ApiResponse<APIMetadataListDTO> httpResponse = restAPIPublisher.getCertificateUsage(CERT_ALIAS, apiCount, 0);
        Assert.assertEquals(httpResponse.getStatusCode(), 200);

        APIMetadataListDTO apiMetadataListDTO = httpResponse.getData();
        List<APIMetadataDTO> apiMetadataDTOS = apiMetadataListDTO.getList();
        Assert.assertEquals(apiMetadataDTOS.size(), apiCount);

        List<String> apiIds = apiMetadataDTOS
                .stream()
                .map(apiMetadataDTO -> apiMetadataDTO.getId())
                .collect(Collectors.toList());

        Assert.assertTrue(evenAPIs.containsAll(apiIds));
    }

    @Test(groups = {"wso2.am"}, description = "Invoke API with different offsets & limits to test correct behavior",
            dependsOnMethods = {"testGetCertificateUsageByAlias"})
    public void testPaginationOfGetCertificateUsageEndpoint() throws ApiException {
        List<APIMetadataDTO> apiMetadataDTOS;
        ApiResponse<APIMetadataListDTO> httpResponse;
        int offset;
        int limit;
        int expected;

        // limit + offset < apiCount & offset = 0
        limit = apiCount - 1;
        offset = 0;
        expected = limit;
        httpResponse = restAPIPublisher.getCertificateUsage(CERT_ALIAS, limit, offset);
        apiMetadataDTOS = httpResponse.getData().getList();
        Assert.assertEquals(apiMetadataDTOS.size(), expected);

        // limit + offset = apiCount & offset = 0
        limit = apiCount;
        offset = 0;
        expected = limit;
        httpResponse = restAPIPublisher.getCertificateUsage(CERT_ALIAS, limit, offset);
        apiMetadataDTOS = httpResponse.getData().getList();
        Assert.assertEquals(apiMetadataDTOS.size(), expected);

        // limit + offset > apiCount & offset = 0
        limit = apiCount + 5;
        offset = 0;
        expected = apiCount;
        httpResponse = restAPIPublisher.getCertificateUsage(CERT_ALIAS, limit, offset);
        apiMetadataDTOS = httpResponse.getData().getList();
        Assert.assertEquals(apiMetadataDTOS.size(), expected);

        // limit + offset < apiCount $ offset < apiCount
        limit = apiCount - 4;
        offset = 2 ;
        expected = apiCount - 4;
        httpResponse = restAPIPublisher.getCertificateUsage(CERT_ALIAS, limit, offset);
        apiMetadataDTOS = httpResponse.getData().getList();
        Assert.assertEquals(apiMetadataDTOS.size(), expected);

        // limit + offset = apiCount & offset < apiCount
        limit = apiCount - 3;
        offset = 3 ;
        expected = apiCount - 3;
        httpResponse = restAPIPublisher.getCertificateUsage(CERT_ALIAS, limit, offset);
        apiMetadataDTOS = httpResponse.getData().getList();
        Assert.assertEquals(apiMetadataDTOS.size(), expected);

        // limit + offset > apiCount & offset < apiCount
        limit = apiCount - 3;
        offset = 5;
        expected = apiCount - offset;
        httpResponse = restAPIPublisher.getCertificateUsage(CERT_ALIAS, limit, offset);
        apiMetadataDTOS = httpResponse.getData().getList();
        Assert.assertEquals(apiMetadataDTOS.size(), expected);

        // offset > apiCount
        limit = apiCount - 3;
        offset = apiCount + 2 ;
        expected = 0;
        httpResponse = restAPIPublisher.getCertificateUsage(CERT_ALIAS, limit, offset);
        apiMetadataDTOS = httpResponse.getData().getList();
        Assert.assertEquals(apiMetadataDTOS.size(), expected);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws ApiException {
        restAPIPublisher.deleteEndpointCertificate(CERT_ALIAS);
        for(String apiId:allAPIs) {
            restAPIPublisher.deleteAPI(apiId);
        }
    }

}
