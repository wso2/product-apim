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
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.am.admin.clients.webapp.WebAppAdminClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.test.utils.webapp.WebAppDeploymentUtil;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/*
APIM678- Create an application through the store rest api
APIM679- Create an already created application through the store rest api
APIM681- Get all created applications through the store rest api
APIM682- Update an application through the store rest api
APIM684- Remove an existing application through the store rest api
 */

public class APIM678ApplicationCreationTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(APIM678ApplicationCreationTestCase.class);
    private APIStoreRestClient apiStore;
    private String applicationName = "NewApplication1";
    private static final String description = "NewApplicationCreation";
    private static final String appTier= APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED;

    private final String webApp = "jaxrs_basic";
    private static final String callBackUrl = "http://myserver.com";
    private List<String> applicationsList = new ArrayList<String>();//list getting from the application creation response
    List<String> allAppsList = new ArrayList<String>(); //List getting from the getAllApplications() response

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
                {"NewApplication1", appTier, callBackUrl, description},
                {"NewApplication2", appTier, callBackUrl, ""},
                {"NewApplication3", appTier, "", description},
                {"NewApplication4", appTier, "", ""}
        };
    }

    @DataProvider(name = "createApplicationWithInValidData")
    public static Object[][] createAppWithInValidDataProvider() throws Exception {
        return new Object[][]{
                {"", appTier, "", ""},//name is empty
                {"NewApplication5", "", callBackUrl, description},
                {"", "", callBackUrl, description},
        };
    }


    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        String fileFormat = ".war";
        super.init(userMode);
        log.info("Test Starting user mode:" + userMode);

        //copy  .war file
        String path = TestConfigurationProvider.getResourceLocation() + File.separator +
                "artifacts" + File.separator + "AM" + File.separator + "lifecycletest" + File.separator;

        String sourcePath = path + webApp + fileFormat;

        String sessionId = createSession(gatewayContextWrk);
        WebAppAdminClient webAppAdminClient = new WebAppAdminClient(gatewayContextWrk.getContextUrls().
                getBackEndUrl(), sessionId);
        webAppAdminClient.uploadWarFile(sourcePath);
        boolean isWebAppDeployed = WebAppDeploymentUtil.isWebApplicationDeployed
                (gatewayContextWrk.getContextUrls().getBackEndUrl(), sessionId, webApp);
        assertTrue(isWebAppDeployed, "Web APP is not deployed: " + webApp);

        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiStore = new APIStoreRestClient(storeURLHttp);

        HttpResponse loginResponse = apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
        assertEquals(loginResponse.getResponseCode(), Response.Status.OK.getStatusCode(), "Response code is Mismatced in Login Response");
        JSONObject loginJsonObject = new JSONObject(loginResponse.getData());
        assertFalse(loginJsonObject.getBoolean("error"), "Response data error in Login Request");

        log.info("Login User: " + storeContext.getContextTenant().getContextUser().getUserName());

        //Add default application to the list
        applicationsList.add("DefaultApplication");

    }

    //create application with valid data
    @Test(dataProvider = "createApplicationWithValidData", description = "Create an Application")
    public void testApplicationCreation(String applicationName, String tier, String callBackUrl, String description)
            throws Exception {

        HttpResponse addApplicationResponse = apiStore.addApplication(applicationName, appTier, callBackUrl, description);
        assertEquals(addApplicationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code is mismatched in add application: " + applicationName);
        JSONObject addApplicationJsonObject = new JSONObject(addApplicationResponse.getData());
        assertFalse(addApplicationJsonObject.getBoolean("error"), "Error in Application Creation: " + applicationName);
        assertEquals(addApplicationJsonObject.get("status"), "APPROVED", "Error in Application Creation: "
                + applicationName);

        //add applications to a list (for get all applications and deletion purpose when test finished)
        applicationsList.add(applicationName);
    }

    //Create Application with invalid data
    @Test(dataProvider = "createApplicationWithInValidData", description = "Create application for invalid data")
    public void testApplicationCreationForInvalidData(String applicationName, String tier, String callBackUrl,
                                                      String description) throws Exception {
        HttpResponse addApplicationResponse = apiStore.addApplication(applicationName, appTier, callBackUrl, description);
        JSONObject addApplicationJsonObject = new JSONObject(addApplicationResponse.getData());
        //Error when application name string empty (JIRA 4040)
        //assertTrue(addApplicationJsonObject.getBoolean("error"), "Error in Application Creation with Invalid data: "
        // + applicationName);

        //Verify application is exists
        HttpResponse getAllApplicationsResponse = apiStore.getAllApplications();
        JSONObject allApplicationsJsonObject = new JSONObject(getAllApplicationsResponse.getData());
//        assertFalse(allApplicationsJsonObject.getBoolean("error"), "Error in App verification");
    }

    //Create already created application
    @Test(description = "Create already created application", dependsOnMethods = "testApplicationCreation")
    public void testAlreadyCreatedApplication() throws Exception {

        HttpResponse alreadyCreatedApplicationResponse = apiStore.addApplication(applicationName, appTier, callBackUrl,
                description);
        assertEquals(alreadyCreatedApplicationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Invalid Response Code in application creation");
        JSONObject alreadyCreatedAppJsonObject = new JSONObject(alreadyCreatedApplicationResponse.getData());
        assertTrue(alreadyCreatedAppJsonObject.getBoolean("error"), "Error in already created application : " +
                applicationName);
        assertTrue(alreadyCreatedAppJsonObject.getString("message").contains(" A duplicate application already exists " +
                "by the name - " + applicationName), "Error in Response Message : " + applicationName);
    }

    @Test(description = "Get all created applications", dependsOnMethods = "testApplicationCreation")
    public void getAllCreatedApplications() throws Exception {

        HttpResponse getAllApplicationsResponse = apiStore.getAllApplications();
        JSONObject getAllApplicationsJsonObject = new JSONObject(getAllApplicationsResponse.getData());
        assertFalse(getAllApplicationsJsonObject.getBoolean("error"), "Error in Get All Applications Response");
        JSONArray getApplicationsJsonArray = getAllApplicationsJsonObject.getJSONArray("applications");
        //Verify application count
        assertTrue(getApplicationsJsonArray.length() >= applicationsList.size(), "Error in Application Count");
        //put in to a list from the response got from getAllApplication()

        String appName;
        for (int allAppsIndex = 0; allAppsIndex < getApplicationsJsonArray.length(); allAppsIndex++) {
            appName = getApplicationsJsonArray.getJSONObject(allAppsIndex).getString("name");
            allAppsList.add(appName);

        }
        log.info("Get All Applications Response List: " + allAppsList);
        log.info("All Apps from Created App List: " + applicationsList);

        //verify application name
        for (int applicationCount = 0; applicationCount < allAppsList.size(); applicationCount++) {
            String applicationName = applicationsList.get(applicationCount);
            assertTrue(allAppsList.contains(applicationName), "Error in getting Applications: " + applicationName);
        }

    }

    //Update application
    @Test(description = "Update an application", dependsOnMethods = "testApplicationCreation")
    public void testUpdateApplication() throws Exception {

        //get default application and update fields
        HttpResponse getAllAppResponse = apiStore.getAllApplications();
        assertEquals(getAllAppResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code mismatched in Get All Applications");
        JSONObject getAllAppJsonObject = new JSONObject(getAllAppResponse.getData());
        assertFalse(getAllAppJsonObject.getBoolean("error"), "Error in get All Applications Response");
        JSONArray getAllApplicationsJsonArray = getAllAppJsonObject.getJSONArray("applications");

        JSONObject updateJsonObject = null;

        for (int j = 0; j < applicationsList.size(); j++) {
            for (int i = 0; i < getAllApplicationsJsonArray.length(); i++) {
                if (applicationsList.get(j).equals(getAllApplicationsJsonArray.getJSONObject(i).getString("name"))) {
                    //get an element to update
                    updateJsonObject = getAllApplicationsJsonArray.getJSONObject(j);
                    break;
                }
            }
        }

        String oldAppName = updateJsonObject.getString("name");
        String appDescription = updateJsonObject.getString("description");
        String appCallbackUrl = updateJsonObject.getString("callbackUrl");
        String appTier = updateJsonObject.getString("tier");
        String newApppName = "UpdateApplication";

        //Update name
        HttpResponse updateNameResponse = apiStore.updateApplication(oldAppName, newApppName, appCallbackUrl,
                appDescription, appTier);
        assertEquals(updateNameResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                " Response Code is mismatched");
        JSONObject updateNameJson = new JSONObject(updateNameResponse.getData());
        assertFalse(updateNameJson.getBoolean("error"), "Error in Application Name Update");


        //Update
        //verify the app name
        HttpResponse verifyAppName = apiStore.getAllApplications();
        assertEquals(verifyAppName.getResponseCode(), Response.Status.OK.getStatusCode());
        JSONObject verifyAppNameJsonObject = new JSONObject(verifyAppName.getData());
        log.info(verifyAppNameJsonObject);
        JSONArray verifyAppJsonArray = verifyAppNameJsonObject.getJSONArray("applications");
        boolean isUpdatedAppAvailable = false;

        String updatedAppName = "";
        for (int appsIndex = 0; appsIndex < verifyAppJsonArray.length(); appsIndex++) {
            updatedAppName = verifyAppJsonArray.getJSONObject(appsIndex).getString("name");
            if (updatedAppName.equals(newApppName)) {
                isUpdatedAppAvailable = true;
                break;
            }
        }
        assertTrue(isUpdatedAppAvailable, "Error in update Application name");

        //update description fields
        String updatedDescription = "updatednewdescription";
        HttpResponse updateDescriptionResponse = apiStore.updateApplication(updatedAppName, updatedAppName,
                appCallbackUrl, updatedDescription, APIMIntegrationConstants.APPLICATION_TIER.LARGE);
        assertEquals(updateDescriptionResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code Invalid in Description update Response");
        JSONObject updateDescriptionJsonObject = new JSONObject(updateDescriptionResponse.getData());
        assertFalse(updateDescriptionJsonObject.getBoolean("error"), "Error in Description Update Response");

        //verify the app description
        HttpResponse verifyAppDesResponse = apiStore.getAllApplications();
        assertEquals(verifyAppName.getResponseCode(), Response.Status.OK.getStatusCode());
        JSONObject verifyAppDesJsonObject = new JSONObject(verifyAppDesResponse.getData());

        JSONArray verifyAppDesJsonArray = verifyAppDesJsonObject.getJSONArray("applications");
        boolean isUpdatedDescriptionAvailable = false;
        for (int applicationsIndex = 0; applicationsIndex < verifyAppDesJsonArray.length(); applicationsIndex++) {
            if (verifyAppDesJsonArray.getJSONObject(applicationsIndex).getString("name").contains(updatedAppName)) {
                isUpdatedDescriptionAvailable = true;
                assertTrue(updatedDescription.contains
                                (verifyAppDesJsonArray.getJSONObject(applicationsIndex).getString("description")),
                        "Error in Description update Response");
                break;
            }
        }
        assertTrue(isUpdatedDescriptionAvailable, "Error in Application Description Update Response");

        //update tier field
        String newTier = APIMIntegrationConstants.APPLICATION_TIER.LARGE;
        HttpResponse updateTierResponse = apiStore.updateApplication(updatedAppName, updatedAppName,
                "http://myserverupdated.com", updatedDescription, newTier);
        assertEquals(updateTierResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code Mismatched in update Tier Response");
        JSONObject updateTierJsonObject = new JSONObject(updateTierResponse.getData());
        assertFalse(updateTierJsonObject.getBoolean("error"), "Error in Update Tier Response");

        //verify tier update
        HttpResponse verifyAppTierResponse = apiStore.getAllApplications();
        assertEquals(verifyAppName.getResponseCode(), Response.Status.OK.getStatusCode());
        JSONObject verifyAppTierJsonObject = new JSONObject(verifyAppTierResponse.getData());

        JSONArray verifyAppTierJsonArray = verifyAppDesJsonObject.getJSONArray("applications");
        boolean isTierUpdated = false;
        for (int appIndex = 0; appIndex < verifyAppTierJsonArray.length(); appIndex++) {
            if (updatedAppName.contains(verifyAppTierJsonArray.getJSONObject(appIndex).getString("name"))) {
                isTierUpdated = true;
                assertTrue(verifyAppTierJsonArray.getJSONObject(appIndex).getString("tier").contains(newTier),
                        "Error in tier Update Response");
                break;
            }
        }
        assertTrue(isTierUpdated, "Error in Tier Update Response");
    }


    //Remove a application
    @Test(description = "Remove application")
    public void testRemoveApplication() throws Exception {

        String applicationName = "RemoveMeApp";
        HttpResponse createApplicationResponse = apiStore.addApplication(applicationName, appTier, callBackUrl,
                description);
        JSONObject createAppJsonObject = new JSONObject(createApplicationResponse.getData());
        assertFalse(createAppJsonObject.getBoolean("error"), "Error in Create an Application: " + applicationName);
        assertEquals(createAppJsonObject.get("status"), "APPROVED", "Error in Application Creation: " + applicationName);

        //verify the application is exists
        HttpResponse getAllAppResponse = apiStore.getAllApplications();
        JSONObject getAllAppJsonObject = new JSONObject(getAllAppResponse.getData());
        assertFalse(getAllAppJsonObject.getBoolean("error"), "Error in Get All Applications Response");
        JSONArray getAllAppJsonArray = getAllAppJsonObject.getJSONArray("applications");
        List<String> appNameList = new ArrayList<String>();
        for (int nameArrayIndex = 0; nameArrayIndex < getAllAppJsonArray.length(); nameArrayIndex++) {
            String appName = getAllAppJsonArray.getJSONObject(nameArrayIndex).getString("name");
            appNameList.add(appName);
        }
        boolean isApplicationExist = false;
        for (int applicationListIndex = 0; applicationListIndex < appNameList.size(); applicationListIndex++) {
            if (appNameList.get(applicationListIndex).equals(applicationName)) {
                isApplicationExist = true;
                break;
            }
        }
        assertTrue(isApplicationExist, "Error : Application is Not Exists : " + applicationName);

        //remove created application
        HttpResponse removeAppResponse = apiStore.removeApplication(applicationName);
        JSONObject removeAppJsonObject = new JSONObject(removeAppResponse.getData());
        assertFalse(removeAppJsonObject.getBoolean("error"), "Error in Remove Application Response: " + applicationName);

        //Verify the application--(application should not exists)
        HttpResponse getAllAppsResponse = apiStore.getAllApplications();
        JSONObject getAllAppsJsonObject = new JSONObject(getAllAppsResponse.getData());
        JSONArray getAllAppsJsonArray = getAllAppsJsonObject.getJSONArray("applications");

        List<String> allAppsList = new ArrayList<String>();
        boolean isRemoveAppExist = false;
        for (int getAllAppsIndex = 0; getAllAppsIndex < getAllAppsJsonArray.length(); getAllAppsIndex++) {
            String appName = getAllAppsJsonArray.getJSONObject(getAllAppsIndex).getString("name");
            allAppsList.add(appName);
            if (appName.equals(applicationName)) {
                isRemoveAppExist = true;
                break;
            }
        }
        assertFalse(isRemoveAppExist, "Error in Remove App Verification : " + applicationName);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        //delete created applications
        HttpResponse getAllAppResponse = apiStore.getAllApplications();
        assertEquals(getAllAppResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error in Get All App Response code");
        JSONObject getAllAppJsonObject = new JSONObject(getAllAppResponse.getData());
        assertFalse(getAllAppJsonObject.getBoolean("error"), "Error in Response");
        JSONArray getAllAppJsonArray = getAllAppJsonObject.getJSONArray("applications");
        List<String> allAppResponseList = new ArrayList<String>();
        for (int i = 0; i < getAllAppJsonArray.length(); i++) {
            allAppResponseList.add(getAllAppJsonArray.getJSONObject(i).getString("name"));
        }


    }

}
