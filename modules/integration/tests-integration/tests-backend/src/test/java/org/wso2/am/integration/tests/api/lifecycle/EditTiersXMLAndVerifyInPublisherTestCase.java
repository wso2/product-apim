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
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;

import java.io.File;
import java.rmi.RemoteException;

import static org.testng.Assert.*;

/**
 * Change the tiers.xml file  with new tier added and check the new tier availability in publisher.
 */
public class EditTiersXMLAndVerifyInPublisherTestCase extends APIManagerLifecycleBaseTest {


    private APIIdentifier apiIdentifier;
    private final static String APPLICATION_NAME = "EditTiersXMLAndVerifyInPublisherTestCase";
    private String artifactsLocation;
    private String originalTiersXML;
    private String newTiersXML;
    private ResourceAdminServiceClient resourceAdminServiceClient;
    private static final String TIER_XML_REG_CONFIG_LOCATION = "/_system/governance/apimgt/applicationdata/tiers.xml";

    private static final String TIER_PERMISSION_PAGE_TIER_GOLD = "<td>Gold</td>";
    private static final String TIER_PERMISSION_PAGE_TIER_PLATINUM = "<td>Platinum</td>";

    private static final String TIER_MANAGE_PAGE_TIER_GOLD = "{ \"value\": \"Gold\", \"text\": \"Gold\" }";
    private static final String TIER_MANAGE_PAGE_TIER_PLATINUM = "{ \"value\": \"Platinum\", \"text\": \"Platinum\" }";

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        apiIdentifier = new APIIdentifier(USER_NAME1, API1_NAME, API_VERSION_1_0_0);
        apiStoreClientUser1.addApplication(APPLICATION_NAME, "", "", "");
        artifactsLocation =
                TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" + File.separator + "AM" +
                        File.separator + "configFiles" + File.separator + "lifecycletest" + File.separator + "tiers.xml";
        resourceAdminServiceClient = new ResourceAdminServiceClient(contextUrls.getBackEndUrl(), sessionCookie);
        originalTiersXML = resourceAdminServiceClient.getTextContent(TIER_XML_REG_CONFIG_LOCATION);

        newTiersXML = readFile(artifactsLocation);


    }


    @Test(groups = {"wso2.am"}, description = "test availability of tiers in Permission Page before change tiers XML")
    public void testAvailabilityOfTiersInPermissionPageBeforeChangeTiersXML() throws APIManagerIntegrationTestException {
        //Create a API
        APIIdentifier apiIdentifier = new APIIdentifier(USER_NAME1, API1_NAME, API_VERSION_1_0_0);
        createAndPublishAPI(apiIdentifier, API1_CONTEXT, apiPublisherClientUser1, false);

        HttpResponse tierPermissionPageHttpResponse = apiPublisherClientUser1.getTierPermissionsPage();
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
                apiPublisherClientUser1.getAPIManagePage(API1_NAME, USER_NAME1, API_VERSION_1_0_0);
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

        HttpResponse tierPermissionPageHttpResponse = apiPublisherClientUser1.getTierPermissionsPage();
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
                apiPublisherClientUser1.getAPIManagePage(API1_NAME, USER_NAME1, API_VERSION_1_0_0);
        assertEquals(tierManagePageHttpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke to get Tier Permission Page");
        assertTrue(tierManagePageHttpResponse.getData().contains(TIER_MANAGE_PAGE_TIER_GOLD),
                "default tier Gold is not available in Tier Permission page before  add new tear in tiers.xml");
        assertTrue(tierManagePageHttpResponse.getData().contains(TIER_MANAGE_PAGE_TIER_PLATINUM),
                "new tier Platinum available in Tier Permission page before  add new tear in tiers.xml");
    }


    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        //restore the original tiers.xml content.
        deleteAPI(apiIdentifier, apiPublisherClientUser1);
        resourceAdminServiceClient.updateTextContent(TIER_XML_REG_CONFIG_LOCATION, originalTiersXML);


    }


}
