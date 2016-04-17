/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * This test case will test Whether a notification email is sent to existing subscribers when a new api is created
 */
public class NotificationTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(NotificationTestCase.class);
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private static final String API_NAME = "NOTIFICATION_TEST_API";
    private static final String API_CONTEXT = "NOTIFICATION_TEST_API";
    private static final String API_VERSION = "1.0.0";
    private static final String NEW_API_VERSION = "2.0.0";
    private static final String APP_NAME = "NOTIFICATION_TEST_APP";
    private final String STORE_USERNAME ="sam";
    private final String STORE_PASSWORD ="admin";
    private String gatewaySessionCookie;
    private String storeURLHttp;

    private ResourceAdminServiceClient resourceAdminServiceClient;
    private static final String ADAPTER_CONFIG_XML = "output-event-adapters.xml";
    private final String TENANT_CONFIG_LOCATION = "/_system/config/apimgt/applicationdata/tenant-conf.json";

    @Factory(dataProvider = "userModeDataProvider")
    public NotificationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        String publisherURLHttp = getPublisherURLHttp();
        storeURLHttp = getStoreURLHttp();
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);
        gatewaySessionCookie = createSession(gatewayContextMgt);

        resourceAdminServiceClient =
                new ResourceAdminServiceClient(gatewayContextMgt.getContextUrls().getBackEndUrl(),
                        createSession(gatewayContextMgt));

        String tenantConfSrcLocation = readFile(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "notification" + File.separator + "tenant-conf.json");

        resourceAdminServiceClient.updateTextContent(TENANT_CONFIG_LOCATION, tenantConfSrcLocation);

        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
        String artifactsLocation = TestConfigurationProvider.getResourceLocation() +
                File.separator + "artifacts" + File.separator + "AM" + File.separator +
                "configFiles" + File.separator + "notification" + File.separator;

        String apimConfigArtifactLocation = artifactsLocation + ADAPTER_CONFIG_XML;
        String apimRepositoryConfigLocation = carbonHome + File.separator + "repository" +
                File.separator + "conf" + File.separator + ADAPTER_CONFIG_XML;

        File apimConfSourceFile = new File(apimConfigArtifactLocation);
        File apimConfTargetFile = new File(apimRepositoryConfigLocation);

        ServerConfigurationManager serverManager = new ServerConfigurationManager(gatewayContextWrk);

        serverManager.applyConfigurationWithoutRestart(apimConfSourceFile, apimConfTargetFile, true);
        log.info("api-manager.xml configuration file copy from :" + apimConfigArtifactLocation +
                " to :" + apimRepositoryConfigLocation);

        serverManager.restartGracefully();
        super.init();
    }

    @Test(groups = {"wso2.am"}, description = "Testing the scopes Notification Feature")
    public void notificationTestCase() throws Exception {

        // Adding API
        String url = getGatewayURLNhttp() + "response";
        String description = "This is a test API created by API manager integration test";

        apiPublisher.login(user.getUserName(), user.getPassword());
        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(url));
        apiRequest.setDescription(description);
        apiRequest.setVersion(API_VERSION);
        apiRequest.setProvider(user.getUserName());
        apiPublisher.addAPI(apiRequest);

        //publishing API
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(API_NAME, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        // add a new store user
        AutomationContext storeContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_STORE_INSTANCE, TestUserMode.SUPER_TENANT_ADMIN);
        HttpResponse storeSignUpResponse = signUp("sam", "admin", "sam", "siva", "sambaheerathan@gmail.com");
        log.info("Sign Up User: " + STORE_USERNAME);
        JSONObject signUpJsonObject = new JSONObject(storeSignUpResponse.getData());
        assertFalse(signUpJsonObject.getBoolean("error"), "Error in user sign up Response");
        assertFalse(signUpJsonObject.getBoolean("showWorkflowTip"), "Error in sign up Response");

        //login with new user
        HttpResponse loginResponse = apiStore.login(STORE_USERNAME, STORE_PASSWORD);
        JSONObject loginJsonObject = new JSONObject(loginResponse.getData());
        assertFalse(loginJsonObject.getBoolean("error"), "Error in Login Request: User Name : " + STORE_USERNAME);

        // For Admin user
        // create new application and subscribing
        apiStore.login(STORE_USERNAME, STORE_PASSWORD);
        apiStore.addApplication(APP_NAME, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "some_url", "NewApp");

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(API_NAME, user.getUserName());
        subscriptionRequest.setApplicationName(APP_NAME);
        apiStore.subscribe(subscriptionRequest);

        //Create a new Version
        HttpResponse newVersionResponse=apiPublisher.copyAPI(user.getUserName(), API_NAME, API_VERSION, NEW_API_VERSION, "");
        assertEquals(newVersionResponse.getResponseCode(),Response.Status.OK.getStatusCode(),"Response Code Mismatch");

        //TODO add a check to see if email is sent
    }

    /**
     * Read the file content and return the content as String.
     *
     * @param fileLocation - Location of the file.
     * @return String - content of the file.
     * @throws APIManagerIntegrationTestException - exception throws when reading the file.
     */
    protected String readFile(String fileLocation) throws APIManagerIntegrationTestException {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(new File(fileLocation)));
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (IOException ioE) {
            throw new APIManagerIntegrationTestException("IOException when reading the file from:" + fileLocation, ioE);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    log.warn("Error when closing the buffer reade which used to reed the file:" + fileLocation +
                            ". Error:" + e.getMessage());
                }
            }
        }
    }


    /**
     * API Store sign up
     * @param userName - store user name
     * @param password -store password
     * @param firstName - user first name
     * @param lastName - user's last name
     * @param email - user's email
     * @return
     * @throws APIManagerIntegrationTestException
     *
     */
    public HttpResponse signUp(String userName, String password, String firstName, String lastName, String email) throws
            APIManagerIntegrationTestException {
        try {
            Map<String, String> requestHeaders = new HashMap<String, String>();
            requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

            return HttpRequestUtil.doPost(new URL(storeURLHttp + "store/site/blocks/user/sign-up/ajax/user-add.jag"),
                    "action=addUser&username=" + userName + "&password=" + password + "&allFieldsValues=" + firstName +
                            "|" + lastName + "||||" + email, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Error in user sign up. Error: " + e.getMessage(), e);
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        if (apiStore != null) {
            apiStore.removeApplication(APP_NAME);
        }

        if (apiPublisher != null) {
            apiPublisher.deleteAPI(API_NAME, API_VERSION, user.getUserName());
        }

        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }
}

