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

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
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
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * Related to Patch Automation  https://wso2.org/jira/browse/APIMANAGER-4081
 * This test class tests the outcome of the paginated search result
 */
public class APIMANAGER4081PaginationCountTestCase extends APIMIntegrationBaseTest {

    private String tenantDomain = "paginationtest.com";
    private int numberOfAPIs = 5;
    private List<APIDTO> createdAPIs = new ArrayList<>();

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

    @Test(groups = {"wso2.am"}, description = "Pagination test case")
    public void testPagination() throws Exception {

        //create 5 APIs and publish them; these APIs should be paginated as 2,2,1
        try {
            for (int i = 0; i < numberOfAPIs; i++) {
                String APIName = "PaginationTestAPI" + Integer.toString(i);
                //put the name of the API in the array so that we can refer it when deleting
                String APIContext = "paginationTest" + Integer.toString(i);
                String tags = "pagination";
                String url = "https://localhost:9443/test";
                String description = "This is test API create by API manager integration test";
                String APIVersion = "1.0.0";
                String providerName = publisherContext.getContextTenant().getTenantAdmin().getUserName()
                        + "@" + tenantDomain;

                //Wait till CommonConfigDeployer finishes adding the default set of policies to the database after tenant admin
                //login, if not api creation fails since Unlimited resource tier is not available in database.
                waitForAPIDeployment();

                APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
                apiRequest.setTags(tags);
                apiRequest.setDescription(description);
                apiRequest.setVersion(APIVersion);
                apiRequest.setSandbox(url);
                apiRequest.setResourceMethod("GET");
                APIDTO createdAPI = restAPIPublisher.addAPI(apiRequest, "v2");
                createdAPIs.add(createdAPI);
                //update the lifecycle of the API to Published state, so that is it visible in store
                HttpResponse lifecycleChangeResponse =
                        restAPIPublisher.changeAPILifeCycleStatusToPublish(createdAPI.getId(), false);
                assertNotNull(lifecycleChangeResponse,
                        "Failed to publish the API " + createdAPI.getName() + ":" + createdAPI.getVersion());
            }

            //periodically check (for 1 min) if the APIs are available for searching (give some time for solr indexing)
            boolean apisAvailableForTesting = false;
            for (int i = 0; i < 12; i++) {
                if (restAPIPublisher.getAllAPIs().getCount() == numberOfAPIs
                        && restAPIStore.getAllAPIs().getCount() == numberOfAPIs) {
                    apisAvailableForTesting = true;
                    break;
                }
                Thread.sleep(5000);
            }

            if (!apisAvailableForTesting) {
                fail(numberOfAPIs + " APIs are not visible properly Store or Publisher");
            }
        } catch (XPathExpressionException e) {
            fail("Error occurred when retrieving context to add APIs. Pagination count test case failed.");
        } catch (APIManagerIntegrationTestException e) {
            fail("Error occurred while log-in to add APIs. Pagination count test case failed.");
        } catch (MalformedURLException e) {
            fail("Invalid service URL to add APIs. Pagination count test case failed.");
        }

        //Checking Dev Portal pagination
        //fetch the first page, this page should have 2 results
        getPaginatedAPIsFromStoreAndVerify(0, 2,2);
        //fetch the 2nd page, this page should have 2 results
        getPaginatedAPIsFromStoreAndVerify(2,2, 2);
        //fetch the 3nd page, this page should have 1 results
        getPaginatedAPIsFromStoreAndVerify(4,3, 1);

        //Checking Publisher pagination
        //fetch the first page, this page should have 2 results
        getPaginatedAPIsFromPublisherAndVerify(0, 2,2);
        //fetch the 2nd page, this page should have 2 results
        getPaginatedAPIsFromPublisherAndVerify(2,2, 2);
        //fetch the 3nd page, this page should have 1 results
        getPaginatedAPIsFromPublisherAndVerify(4,3, 1);

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
        assertEquals(publisherAPIs.getPagination().getTotal().intValue(), numberOfAPIs, "Expected " + numberOfAPIs + " as " +
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
    private void getPaginatedAPIsFromStoreAndVerify(int offset, int limit, int expect) throws org.wso2.am.integration.clients.store.api.ApiException {
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
            //take the names of the newly added APIs from the saved array and delete them
            for (int j = 0; j < numberOfAPIs; j++) {
                String APIVersion = "1.0.0";
                restAPIPublisher.deleteAPI(createdAPIs.get(j).getId());
            }
        }
        super.cleanUp();
    }
}
