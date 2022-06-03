/*
 *
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationListDTO;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class APIM678ApplicationCreationTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(APIM678ApplicationCreationTestCase.class);
    private String applicationName = "NewApplication1";
    private static final String description = "NewApplicationCreation";
    private static final String appTier = APIMIntegrationConstants.APPLICATION_TIER.TEN_PER_MIN;
    private static Map<String, String> customAttributes = new HashMap<String, String>() {
        {
            put("billing_tier", "default_tier");

        }
    };
    private String applicationId;

    protected static final int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();

    private List<String> applicationsList = new ArrayList<String>();//list getting from the application creation response

    @Factory(dataProvider = "userModeDataProvider")
    public APIM678ApplicationCreationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @DataProvider(name = "createApplicationWithValidData")
    public static Object[][] createAppWithValidDataProvider() throws Exception {

        return new Object[][]{
                {"NewApplication1", appTier, description},
                {"NewApplication2", appTier, ""},
                {"NewApplication3", appTier, description},
                {"NewApplication4", appTier, ""}
        };
    }

    @DataProvider(name = "createApplicationWithInValidData")
    public static Object[][] createAppWithInValidDataProvider() throws Exception {
        return new Object[][]{
                {"", appTier, ""},//name is empty
                {"NewApplication5", "", description},
                {"", "", description},
        };
    }

    @DataProvider(name = "createApplicationWithCustomAttributes")
    public static Object[][] createApplicationWithCustomAttributes() throws Exception {
        return new Object[][]{
                {"NewApplication6", appTier, description, ApplicationDTO.TokenTypeEnum.JWT, customAttributes}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        log.info("Test Starting user mode:" + userMode);
        restAPIStore = new RestAPIStoreImpl(storeContext.getContextTenant().getTenantAdmin().getUserNameWithoutDomain(),
                storeContext.getContextTenant().getContextUser().getPassword(),
                storeContext.getContextTenant().getDomain(), storeURLHttps);

    }

    //create application with valid data
    @Test(dataProvider = "createApplicationWithValidData", description = "Create an Application")
    public void testApplicationCreation(String applicationName, String tier, String description)
            throws Exception {


        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                description, appTier,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();
        assertEquals(applicationResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response Code is mismatched in add application " + applicationName);
        assertNotNull(applicationResponse, "Error in Application Creation: "
                + applicationName);
        //add applications to a list (for get all applications and deletion purpose when test finished)
        applicationsList.add(applicationResponse.getData());
    }

    //TODO: Commented until fix: https://github.com/wso2/product-apim/issues/6012

    //Create Application with invalid data
//    @Test(groups = "webapp", dataProvider = "createApplicationWithInValidData",
//          description = "Create application for invalid data")
//    public void testApplicationCreationForInvalidData(String applicationName, String tier, String description)
//            throws Exception {
//        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
//                description, tier,
//                ApplicationDTO.TokenTypeEnum.JWT);
//
//        assertNull(applicationResponse , "Error in Application Creation with Invalid data: "
//         + applicationName);
//    }

    //Create already created application
    @Test(description = "Create already created application",
            dependsOnMethods = "testApplicationCreation")
    public void testAlreadyCreatedApplication() throws Exception {

        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                description, appTier,
                ApplicationDTO.TokenTypeEnum.JWT);
        assertNull(applicationResponse, "Create already created application: "
                + applicationName);
    }

    @Test(description = "Get all created applications", dependsOnMethods = "testApplicationCreation")
    public void getAllCreatedApplications() throws Exception {
        ApplicationListDTO applicationListDTO = restAPIStore.getAllApps();
        assertNotNull(applicationListDTO,
                "Error while get all applications");

    }

    //Update application
    @Test(description = "Update an application")
    public void testUpdateApplication() throws Exception {

        String applicationName = "testApplication";
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                description, appTier,
                ApplicationDTO.TokenTypeEnum.JWT);
        assertEquals(applicationResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response Code is mismatched in add application " + applicationName);
        applicationsList.add(applicationResponse.getData());

        String newAppName = "UpdateApplication";
        String newappDescription = "Application updated";
        String newAppTier = "20PerMin";

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeys(applicationId, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                        null, grantTypes);

        assertNotNull(applicationKeyDTO.getToken().getAccessToken());

        //Update AppTier
        HttpResponse updateTierResponse = restAPIStore.updateApplicationByID(applicationResponse.getData(),
                applicationName, description, newAppTier,
                ApplicationDTO.TokenTypeEnum.JWT);
        assertTrue(updateTierResponse.getData().contains(newAppTier), "Error while updating application tier" +
                applicationName);

        //Update AppName
        HttpResponse updateNameResponse = restAPIStore.updateApplicationByID(applicationResponse.getData(),
                newAppName, description, appTier,
                ApplicationDTO.TokenTypeEnum.JWT);
        assertTrue(updateNameResponse.getData().contains(newAppName), "Error while updating application name" +
                applicationName);

        ApplicationKeyDTO applicationKeyDTONew = restAPIStore
                .generateKeys(applicationId, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                        null, grantTypes);

        assertNotNull(applicationKeyDTONew.getToken().getAccessToken());

        //Update AppDescription
        HttpResponse updateDesResponse = restAPIStore.updateApplicationByID(applicationResponse.getData(),
                applicationName, newappDescription, newAppTier,
                ApplicationDTO.TokenTypeEnum.JWT);
        assertTrue(updateDesResponse.getData().contains(newappDescription), "Error while updating application " +
                "description" + applicationName);

    }

    //Remove a application
    @Test(description = "Remove application")
    public void testRemoveApplication() throws Exception {

        String applicationName = "RemoveMeApp";
        HttpResponse createApplicationResponse = restAPIStore.createApplication(applicationName,
                description, appTier,
                ApplicationDTO.TokenTypeEnum.JWT);
        assertEquals(createApplicationResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response Code is mismatched in add application " + applicationName);

        ApplicationDTO applicationResponse = restAPIStore.getApplicationById(createApplicationResponse.getData());

        //remove created application
        HttpResponse deleteApplicationResponse = restAPIStore.
                deleteApplication(new JSONObject(applicationResponse).getString("applicationId"));
        assertEquals(deleteApplicationResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response Code is mismatched get application " + applicationName);

        try {
            restAPIStore.getApplicationById(createApplicationResponse.getData());
        } catch (ApiException e) {
            assertEquals(e.getCode(), HttpStatus.SC_NOT_FOUND,
                    "Application is not deleted: " + applicationName);
        }
    }


    // Create application with custom attributes
    @Test(dataProvider = "createApplicationWithCustomAttributes",
            description = "Create an Application")
    public void testApplicationCreationWithCustomAttributes(String applicationName, String tier, String description,
                                                            ApplicationDTO.TokenTypeEnum token,
                                                            Map<String, String> applicationAttributes) throws Exception {
        HttpResponse applicationResponse = restAPIStore.createApplicationWithCustomAttribute(applicationName,
                description, appTier,
                token, applicationAttributes);
        assertEquals(applicationResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response Code is mismatched in add application with custom attributes " + applicationName);
        assertNotNull(applicationResponse, "Error in Application Creation: "
                + applicationName);
        applicationsList.add(applicationResponse.getData());
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        removeAllApps();
    }

    public void removeAllApps() throws Exception {
        //delete created applications
        for (String appId : applicationsList) {
            restAPIStore.deleteApplication(appId);
        }
    }
}
