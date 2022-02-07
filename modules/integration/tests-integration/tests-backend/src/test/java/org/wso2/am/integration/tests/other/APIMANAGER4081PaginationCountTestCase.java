/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.other;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIInfoDTO;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;

import javax.xml.xpath.XPathExpressionException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Related to Patch Automation  https://wso2.org/jira/browse/APIMANAGER-4081
 * This test class tests the outcome of the paginated search result
 */
public class APIMANAGER4081PaginationCountTestCase extends APIMIntegrationBaseTest {

    private String tenantDomain = "paginationtest.com";
    private int numberOfAPIs = 5;
    private int numberOfPublisherAPIs = 15;
    private List<String> createdAPIs = new ArrayList<>();
    private static final Log log = LogFactory.getLog(APIMANAGER4081PaginationCountTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() {

        try {
            super.init();
            tenantManagementServiceClient.addTenant(tenantDomain,
                    keyManagerContext.getContextTenant().getTenantAdmin().getPassword(),
                    keyManagerContext.getContextTenant().getTenantAdmin().getUserName(), "demo");

        } catch (XPathExpressionException e) {
            fail("Error occurred while retrieving context. Pagination count test case failed.");
        } catch (APIManagerIntegrationTestException e) {
            fail("Error occurred while adding tenant. Pagination count test case failed.");
        } catch (RemoteException e) {
            fail("Error occurred while creating session. Pagination count test case failed.");
        } catch (TenantMgtAdminServiceExceptionException e) {
            fail("Error while getting tenant management service. Pagination count test case failed.");
        }
    }

/*
* This test follow the below integration scenario.
*
*
1.1.Create API
1.2. Publish API
1.3. assert # of APIs in the devportal /assert latest version

1.4 create new version
1.5.assert # of APIs in the devportal (this should be same as above no, since api is not yet published.)

1.6. Publish the new version
1.7. assert for the latest version on the devportal

1.8.Create an older version than the latest(version in between step 1.1 and 1.4)
1.9. assert for the latest version on the devportal(this should be same as above step 1.7, since api is not yet published.)

1.10. publish
1.11. assert for the latest version on the devportal(this should be same as above step 1.7, since the created version is older than the latest)

1.12. delete latest version created in step 1.4
1.13 assert latest version (this should be the odd version created in step 1.8)

1.14. create a later version
1.15 [optional] assert for the latest version on the devportal(this should be same as above , since api is not yet published

1.16. publish
1.17. assert for the latest version on the devportal (new version should be the latest)

2.0. assert for pagination"
*
*/
    @Test(groups = {"wso2.am"}, description = "Pagination test case")
    public void testPaginationWithMultipleVersions() throws Exception {

        Map<Integer, String[]> versionsOfAPIs = new HashMap<>();
        String[] api1Versions = {"1.0", "2.0.1", "1.0.2", "3.0"};
        String[] api2Versions = {"1.0.1", "2.0.0", "1.0.3", "2.1.0"};
        String[] api3Versions = {"1.0.0b", "2.0.1c", "2.0.1a", "2.0.1d"};
        String[] api4Versions = {"1.0.0-SNAPSHOT", "1.1.0-SNAPSHOT", "1.0.1-SNAPSHOT", "2.0.0-SNAPSHOT"};
        String[] api5Versions = {"1.0.0.wso2v1", "2.0.0.wso2v1", "1.1.3.wso2v1", "2.0.0.wso2v2"};
        versionsOfAPIs.put(1, api1Versions);
        versionsOfAPIs.put(2, api2Versions);
        versionsOfAPIs.put(3, api3Versions);
        versionsOfAPIs.put(4, api4Versions);
        versionsOfAPIs.put(5, api5Versions);

        int counter = 0;
        while (counter < versionsOfAPIs.size()) {
            counter ++;
            String[] apiVersionsArray = versionsOfAPIs.get(counter);
            int numberOfVersions = apiVersionsArray.length;
            String apiId = "";

            for (int i = 0; i < numberOfVersions; i++) {
                String APIName = "PaginationTestVersionedAPI" + Integer.toString(counter);
                //put the name of the API in the array so that we can refer it when deleting
                String APIContext = "paginationTestVersioned" + Integer.toString(counter);
                String tags = "pagination";
                String providerName = publisherContext.getContextTenant().getTenantAdmin().getUserName();
                String url = "https://localhost:9443/test";
                String description = "This is test API create by API manager integration test";
                String APIVersion = apiVersionsArray[i];
                org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO devportalAPIs =
                        new org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO();

                //Wait till CommonConfigDeployer finishes adding the default set of policies to the database after tenant admin
                //login, if not api creation fails since Unlimited resource tier is not available in database.
                waitForAPIDeployment();

                APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
                apiRequest.setTags(tags);
                apiRequest.setDescription(description);
                apiRequest.setVersion(APIVersion);
                apiRequest.setSandbox(url);
                apiRequest.setResourceMethod("GET");
                if (i < 1) {
                    APIDTO createdAPI = restAPIPublisher.addAPI(apiRequest, "v2");// 1.1
                    if (createdAPI != null) {
                        apiId = createdAPI.getId();
                        createdAPIs.add(createdAPI.getId());
                    } else {
                        fail("API creation has failed for APIName=" + APIName + " APIVersion=" + APIVersion);
                    }
                } else {
                    apiId = restAPIPublisher.createNewAPIVersion(APIVersion, apiId, false);//1.4 1.8 1.14
                    //1.9
                    if (i == 2) {
                        devportalAPIs = restAPIStore.getAllAPIs();
                        if (devportalAPIs.getList() != null) {
                            for (APIInfoDTO apiInfoDTO : devportalAPIs.getList()) {
                                if (APIName.equals(apiInfoDTO.getName()) && providerName
                                        .equals(apiInfoDTO.getProvider())) {
                                    assertEquals(apiInfoDTO.getVersion(), versionsOfAPIs.get(counter)[i - 1]); //1.9
                                }
                            }
                        }
                    }
                    if (i > 1) {
                        createdAPIs.add(apiId);
                    }
                }
                //periodically check if the API is available in publisher
                int publisherAPIsCount = (3 * (counter - 1)) + (i + 1);
                publisherAPIsCount = (i == 3) ? publisherAPIsCount - 1 : publisherAPIsCount;

                boolean isAPIAvailableInPublisher = false;
                for (int j = 0; j < 24; j++) {
                    APIListDTO publisherAPIs = restAPIPublisher.getAllAPIs();
                    if ( null != publisherAPIs) {
                        if (null != publisherAPIs.getCount()) {
                            if ((publisherAPIs.getCount() == publisherAPIsCount)) {
                                isAPIAvailableInPublisher = true;
                                break;
                            }
                        }
                    }
                    Thread.sleep(5000);
                }
                if (!isAPIAvailableInPublisher) {
                    fail(" Versioned API apiId=" + apiId + " APIName = " + APIName + "APIVersion" + APIVersion +
                            "is not visible properly in Publisher.");
                }
                //1.2 1.6
                log.info("publishing API apiId=" + apiId + " APIName = " + APIName + "APIVersion" + APIVersion);
                HttpResponse lifecycleChangeResponse =
                        restAPIPublisher.changeAPILifeCycleStatusToPublish(apiId, false);
                assertNotNull(lifecycleChangeResponse,
                        "Failed to publish the API " + APIName + ":" + APIVersion);
                //Check whether the APIs are available for testing
                boolean apisAvailableForTesting = false;
                APIListDTO publisherAPIs = new APIListDTO();

                //wait for latest versin to be available
                waitForAPIDeployment();

                //periodically check if the APIs are available for searching (give some time for solr indexing)
                for (int j = 0; j < 10; j++) {
                    devportalAPIs = restAPIStore.getAllAPIs();
                    publisherAPIs = restAPIPublisher.getAllAPIs();
                    if (null != publisherAPIs && null != devportalAPIs) {
                        if ((publisherAPIs.getCount() == publisherAPIsCount)
                                && devportalAPIs.getCount() == counter) {
                            apisAvailableForTesting = true;
                            break;
                        }
                    }
                    Thread.sleep(5000);
                }

                if (!apisAvailableForTesting) {
                    fail(" Versioned APIs are not visible properly in Devportal or Publisher");
                }
                // assert no of APIs in the devportal
                assertTrue(devportalAPIs.getCount() == counter); //1.3 1.5

                //assert for correct latest version
                String latestVersionInDevportal = "";
                String latestVersionApiId = "";

                if (devportalAPIs.getList() != null) {
                    for (APIInfoDTO apiInfoDTO : devportalAPIs.getList()) {
                        if (APIName.equals(apiInfoDTO.getName()) && providerName
                                .equals(apiInfoDTO.getProvider())) {
                            latestVersionInDevportal = apiInfoDTO.getVersion();
                            latestVersionApiId = apiInfoDTO.getId();
                        }
                    }
                }
                log.info("apiVersionsArray = " + apiVersionsArray + " i= " + i + "apiVersionsArray[i] = " + apiVersionsArray[i]
                        + " latestVersionInDevportal= " + latestVersionInDevportal);
                switch (i) { //1.3
                    case 0:
                    case 1: //1.7
                    case 3: // 1.15
                        assertEquals(latestVersionInDevportal, apiVersionsArray[i], " count = " + counter + " i=" + i);
                        break;
                    case 2: // 1.11
                        assertEquals(latestVersionInDevportal, apiVersionsArray[i - 1], " count = " + counter + " i=" + i); //1.11
                        break;
                    default:
                        break;
                }
                // delete the version created in step 1.4 (only when i == 2)
                if (i == 2 && StringUtils.isNotEmpty(latestVersionApiId)) {
                    restAPIPublisher.deleteAPI(latestVersionApiId); // 1.12
                    waitForAPIDeployment();

                    // assert for the latest version, which should be the version created in step 1.8
                    devportalAPIs = restAPIStore.getAllAPIs();
                    if (devportalAPIs != null) {
                        for (APIInfoDTO apiInfoDTO : devportalAPIs.getList()) {
                            if (APIName.equals(apiInfoDTO.getName()) && providerName
                                    .equals(apiInfoDTO.getProvider())) {
                                latestVersionInDevportal = apiInfoDTO.getVersion();
                            }
                        }
                    }
                    assertEquals(latestVersionInDevportal, versionsOfAPIs.get(counter)[i]); //1.13
                }

            }

        }
        //Checking Dev Portal pagination
        //fetch the first page, this page should have 4 results
        getPaginatedAPIsFromPublisherAndVerify(0, 4, 4);
        //fetch the 2nd page, this page should have 4 results
        getPaginatedAPIsFromPublisherAndVerify(4, 4, 4);
        //fetch the 3nd page, this page should have 4 results
        getPaginatedAPIsFromPublisherAndVerify(8, 4, 4);
        //fetch the 4nd page, this page should have 3 results
        getPaginatedAPIsFromPublisherAndVerify(12, 4, 3);

        //Checking Publisher pagination
        //fetch the first page, this page should have 2 results
        getPaginatedAPIsFromDevportalAndVerify(0, 2, 2);
        //fetch the 2nd page, this page should have 2 results
        getPaginatedAPIsFromDevportalAndVerify(2, 2, 2);
        //fetch the 3nd page, this page should have 1 results
        getPaginatedAPIsFromDevportalAndVerify(4, 3, 1);

    }

    /**
     * Request paginated APIs from publisher and verify counts
     *
     * @param offset starting index
     * @param limit number of APIs to return
     * @param expect expected returned number of APIs
     * @throws ApiException when failed to fetch the APIs
     */
    private void getPaginatedAPIsFromPublisherAndVerify(int offset, int limit, int expect) throws ApiException {
        APIListDTO publisherAPIs = restAPIPublisher.getAPIs(offset, limit);
        assertEquals(publisherAPIs.getCount().intValue(), expect, "Expected " + expect + " of APIs in the page with " +
                "offset:" + offset + " but was " + publisherAPIs.getCount());
        assertNotNull(publisherAPIs.getPagination(), "pagination element cannot be null in get APIs response");
        assertEquals(publisherAPIs.getPagination().getTotal().intValue(), numberOfPublisherAPIs, "Expected " + numberOfPublisherAPIs + " as " +
                "total number of APIs in the system but was " + publisherAPIs.getPagination().getTotal());
    }

    /**
     * Request paginated APIs from store and verify counts
     *
     * @param offset starting index
     * @param limit number of APIs to return
     * @param expect expected returned number of APIs
     * @throws org.wso2.am.integration.clients.store.api.ApiException when failed to fetch the APIs
     */
    private void getPaginatedAPIsFromDevportalAndVerify(int offset, int limit, int expect) throws org.wso2.am.integration.clients.store.api.ApiException {
        org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO storeAPIs = restAPIStore.getAPIs(offset, limit);
        assertEquals(storeAPIs.getCount().intValue(), expect, "Expected " + expect + " of APIs in the page with " +
                "offset:" + offset + " but was " + storeAPIs.getCount());
        assertNotNull(storeAPIs.getPagination(), "pagination element cannot be null in get APIs response");
        assertEquals(storeAPIs.getPagination().getTotal().intValue(), numberOfAPIs, "Expected " + numberOfAPIs + " as total number " +
                "of APIs in the system but was " + storeAPIs.getPagination().getTotal());
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (restAPIPublisher != null) {
            for (String id : createdAPIs) {
                log.info("Delete API from createdAPIs: id" + id);
                restAPIPublisher.deleteAPI(id);
            }
        }
        super.cleanUp();
    }
}
