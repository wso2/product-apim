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

package org.wso2.am.integration.tests.api.lifecycle;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import static org.testng.Assert.*;

/**
 * Change the tiers.xml file  with new tier added and check the new tier availability in publisher.
 */
public class EditTiersXMLAndVerifyInPublisherTestCase extends APIManagerLifecycleBaseTest {
    private static final String API_NAME = "APILifeCycleTestAPI1";
    private static final String API_CONTEXT = "testAPI1";
    private static final String API_TAGS = "youtube, video, media";
    private static final String API_END_POINT_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String TIER_XML_REG_CONFIG_LOCATION =
            "/_system/governance/apimgt/applicationdata/tiers.xml";
    private static final String TIER_PERMISSION_PAGE_TIER_GOLD = "<td>Gold</td>";
    private static final String TIER_PERMISSION_PAGE_TIER_PLATINUM = "<td>Platinum</td>";
    private static final String TIER_MANAGE_PAGE_TIER_GOLD = "{ \"value\": \"Gold\", \"text\": \"Gold\" }";
    private static final String TIER_MANAGE_PAGE_TIER_PLATINUM = "{ \"value\": \"Platinum\", \"text\": \"Platinum\" }";
    private String providerName;
    private APICreationRequestBean apiCreationRequestBean;
    private APIIdentifier apiIdentifier;
    private String originalTiersXML;
    private String newTiersXML;
    private ResourceAdminServiceClient resourceAdminServiceClient;
    private APIPublisherRestClient apiPublisherClientUser1;


    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, XPathExpressionException,
            RemoteException, ResourceAdminServiceExceptionException, MalformedURLException {
        super.init();
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                        new URL(API_END_POINT_URL));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        APIStoreRestClient apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        String artifactsLocation =
                TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" +
                        File.separator + "AM" + File.separator + "lifecycletest" + File.separator + "tiers.xml";
        resourceAdminServiceClient =
                new ResourceAdminServiceClient(gatewayContext.getContextUrls().getBackEndUrl(),
                        createSession(gatewayContext));
        originalTiersXML = resourceAdminServiceClient.getTextContent(TIER_XML_REG_CONFIG_LOCATION);
        newTiersXML = readFile(artifactsLocation);
    }


    @Test(groups = {"wso2.am"}, description = "test availability of tiers in Permission Page before change tiers XML")
    public void testAvailabilityOfTiersInPermissionPageBeforeChangeTiersXML() throws APIManagerIntegrationTestException {
        //Create a API
        APIIdentifier apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        createAndPublishAPI(apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1, false);

        HttpResponse tierPermissionPageHttpResponse =
                apiPublisherClientUser1.getTierPermissionsPage();
        assertEquals(tierPermissionPageHttpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke to get Tier Permission Page");
        assertTrue(tierPermissionPageHttpResponse.getData().contains(TIER_PERMISSION_PAGE_TIER_GOLD),
                "default tier Gold is not available in Tier Permission page before  add new tear in tiers.xml");
        assertFalse(tierPermissionPageHttpResponse.getData().contains(TIER_PERMISSION_PAGE_TIER_PLATINUM),
                "new tier Platinum available in Tier Permission page before  add new tear in tiers.xml");
    }


    @Test(groups = {"wso2.am"}, description = "Test availability of tiers in API Manage Page before change tiers XML",
            dependsOnMethods = "testAvailabilityOfTiersInPermissionPageBeforeChangeTiersXML")
    public void testAvailabilityOfTiersInAPIManagePageBeforeChangeTiersXML() throws APIManagerIntegrationTestException {

        HttpResponse tierManagePageHttpResponse =
                apiPublisherClientUser1.getAPIManagePage(API_NAME, providerName, API_VERSION_1_0_0);
        assertEquals(tierManagePageHttpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke to get Tier Permission Page");
        assertTrue(tierManagePageHttpResponse.getData().contains(TIER_MANAGE_PAGE_TIER_GOLD),
                "default tier  Gold is not available in Tier Permission page before  add new tear in tiers.xml");
        assertFalse(tierManagePageHttpResponse.getData().contains(TIER_MANAGE_PAGE_TIER_PLATINUM),
                "new tier Platinum available in Tier Permission page before  add new tear in tiers.xml");
    }


    @Test(groups = {"wso2.am"}, description = "test availability of tiers in Permission Page after change tiers XML",
            dependsOnMethods = "testAvailabilityOfTiersInAPIManagePageBeforeChangeTiersXML")
    public void testAvailabilityOfTiersInPermissionPageAfterChangeTiersXML() throws RemoteException,
            ResourceAdminServiceExceptionException, APIManagerIntegrationTestException {
        //Changing the Tier XML
        resourceAdminServiceClient.updateTextContent(TIER_XML_REG_CONFIG_LOCATION, newTiersXML);
        HttpResponse tierPermissionPageHttpResponse =
                apiPublisherClientUser1.getTierPermissionsPage();
        assertEquals(tierPermissionPageHttpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke to get Tier Permission Page");
        assertTrue(tierPermissionPageHttpResponse.getData().contains(TIER_PERMISSION_PAGE_TIER_GOLD),
                "default tier Gold is not available in Tier Permission page before  add new tear in tiers.xml");
        assertTrue(tierPermissionPageHttpResponse.getData().contains(TIER_PERMISSION_PAGE_TIER_PLATINUM),
                "new tier Platinum  is not available in Tier Permission page before  add new tear in tiers.xml");

    }


    @Test(groups = {"wso2.am"}, description = "Test availability of tiers in API Manage Page after change tiers XML",
            dependsOnMethods = "testAvailabilityOfTiersInPermissionPageAfterChangeTiersXML")
    public void testAvailabilityOfTiersInAPIManagePageAfterChangeTiersXML() throws APIManagerIntegrationTestException {
        HttpResponse tierManagePageHttpResponse =
                apiPublisherClientUser1.getAPIManagePage(API_NAME, providerName, API_VERSION_1_0_0);
        assertEquals(tierManagePageHttpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke to get Tier Permission Page");
        assertTrue(tierManagePageHttpResponse.getData().contains(TIER_MANAGE_PAGE_TIER_GOLD),
                "default tier Gold is not available in Tier Permission page before  add new tear in tiers.xml");
        assertTrue(tierManagePageHttpResponse.getData().contains(TIER_MANAGE_PAGE_TIER_PLATINUM),
                "new tier Platinum available in Tier Permission page before  add new tear in tiers.xml");
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        deleteAPI(apiIdentifier, apiPublisherClientUser1);
        //restore the original tiers.xml content.
        resourceAdminServiceClient.updateTextContent(TIER_XML_REG_CONFIG_LOCATION, originalTiersXML);
    }


}
