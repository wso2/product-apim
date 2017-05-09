/*
 *
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.tests.other;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.exceptions.AutomationUtilException;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Disables advanced throttling, enable monetization, create an API as a Prototyped API and browses the store and check
 * if it loads properly
 */
public class APIMANAGER5750MultipleVersionAPISTestCase extends APIMIntegrationBaseTest {

    String apiName = "VersionCheckAPI";
    String apiVersion = "1.0.0";
    String apiContext = "versionCheck";
    String providerName = null;


    private String tenantConfigBeforeTestCase;
    ServerConfigurationManager serverConfigurationManager;
    private static final Log log = LogFactory.getLog(APIMANAGER5750MultipleVersionAPISTestCase.class);
    private ResourceAdminServiceClient resourceAdminServiceClient;
    private final String TENANT_CONFIG_LOCATION = "/_system/config/apimgt/applicationdata/tenant-conf.json";

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws APIManagerIntegrationTestException,
            XPathExpressionException, IOException, TenantMgtAdminServiceExceptionException,
            AutomationUtilException {
        super.init();
    }

    @Test(groups = {"wso2.am"}, description = "Create a new API version & check if it appears in the API store after" +
            " changing configurations")
    public void testVisibilityOfMultipleVersionsVisibleEnabled() throws Exception {

        tenantManagementServiceClient.addTenant("test.com", "wso2@123", "admin", "Gold");

        String endpointUrl = getAPIInvocationURLHttp("response");
        String newVersion = "2.0.0";
        providerName = "admin@test.com";

        try{
            APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));

            apiRequest.setVersion(apiVersion);
            apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
            apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
            apiRequest.setProvider(providerName);

            //Add the API - V1
            APIPublisherRestClient apiPublisherRestClient2 = new APIPublisherRestClient(publisherURLHttp);
            apiPublisherRestClient2.login(providerName, "wso2@123");
            HttpResponse addAPIResponse = apiPublisherRestClient2.addAPI(apiRequest);
            verifyResponse(addAPIResponse);

            //Publish the PAI - V1
            APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, providerName,
                    APILifeCycleState.PUBLISHED);
            updateRequest.setVersion(apiVersion);
            addAPIResponse = apiPublisherRestClient2.changeAPILifeCycleStatus(updateRequest);
            verifyResponse(addAPIResponse);

            //Copy API V1, and create V2
            apiPublisherRestClient2.copyAPI(providerName, apiName, apiVersion, newVersion, "false");
            HttpResponse copyAPiResponse = apiPublisherRestClient2.getAPI(apiName, providerName, newVersion);
            verifyResponse(copyAPiResponse);

            JSONObject response = new JSONObject(copyAPiResponse.getData());
            String version = response.getJSONObject("api").get("version").toString();
            Assert.assertEquals(version, newVersion);

             //publish the api V2
            APILifeCycleStateRequest updateRequest2 = new APILifeCycleStateRequest(apiName, providerName,
                    APILifeCycleState.PUBLISHED);
            updateRequest2.setVersion(newVersion);
            copyAPiResponse = apiPublisherRestClient2.changeAPILifeCycleStatus(updateRequest2);
            verifyResponse(copyAPiResponse);

            APIStoreRestClient apiStoreRestClient = new APIStoreRestClient(storeURLHttp);
            apiStoreRestClient.login(providerName, "wso2@123");

            //Check if API V1 is visible
            APIIdentifier apiIdentifierAPI1Version = new APIIdentifier(providerName, apiName, apiVersion);
            List<APIIdentifier> apiStoreAPIIdentifierList =
                    APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreRestClient.getAllPublishedAPIs("test.com"));
            assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAPI1Version, apiStoreAPIIdentifierList),
                    "New version Api is  not visible in API Store after publish new version.");

            apiStoreRestClient.logout();

            //Enabling multiple versions by editing tenant-conf.json in super tenant registry
            resourceAdminServiceClient =
                    new ResourceAdminServiceClient(gatewayContextMgt.getContextUrls().getBackEndUrl(),
                            providerName,"wso2@123");
            String tenantConfContent = FileUtils.readFileToString(new File(getAMResourceLocation() + File.separator
                    + "configFiles" + File.separator + "displayMultipleVersions" + File.separator + "tenant-conf.json"), "UTF-8");
            tenantConfigBeforeTestCase = resourceAdminServiceClient.getTextContent(TENANT_CONFIG_LOCATION);
            resourceAdminServiceClient.updateTextContent(TENANT_CONFIG_LOCATION, tenantConfContent);

            apiStoreRestClient.login(providerName, "wso2@123");
            //Check if API V1 is now visible
            APIIdentifier apiIdentifierAPI1Version2 = new APIIdentifier(providerName, apiName, apiVersion);
            List<APIIdentifier> apiStoreAPIIdentifierList2 =
                    APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreRestClient.getAllPublishedAPIs("test.com"));
            assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAPI1Version2, apiStoreAPIIdentifierList2),
                    "Old version  is  visible in API Store after publish new version.");
        }  catch (IOException e) {
            log.error("Failed to get  tenant store", e);
            assertTrue(false, "Failed to get  tenant store due to " + e.getMessage());
        }
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        resourceAdminServiceClient.updateTextContent(TENANT_CONFIG_LOCATION, tenantConfigBeforeTestCase);
        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        apiPublisherRestClient.login(providerName, "wso2@123");
        apiPublisherRestClient.deleteAPI(apiName, apiVersion, providerName);
        super.cleanUp();
    }

}