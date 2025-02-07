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

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.UserManagementUtils;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import javax.mail.Message;
import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.wso2.am.integration.test.utils.base.APIMIntegrationConstants.SUPER_TENANT_DOMAIN;


/**
 * This test case will test Whether a notification email is sent to existing subscribers when a new api is created
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class NotificationTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(NotificationTestCase.class);
    private static final String API_NAME = "NOTIFICATION_TEST_API";
    private static final String API_CONTEXT = "NOTIFICATION_TEST_API";
    private static final String API_VERSION = "1.0.0";
    private static final String NEW_API_VERSION = "2.0.0";
    private static final String APP_NAME = "NOTIFICATION_TEST_APP";
    private final String STORE_USERNAME = "api_store";
    private final String STORE_PASSWORD = "admin";

    private static final String EMAIL_USERNAME = "APIM";
    private static final String EMAIL_PASSWORD = "APIM+123";
    private static final String USER_EMAIL_ADDRESS = "apim@gmail.com";
    private static final int SMTP_TEST_PORT = 3025;

    private static final String FIRST_NAME = "John";
    private static final String ORGANIZATION = "Test";

    private GreenMail greenMail;
    private String apiId;
    private String newApiId;
    private String applicationId;
    private String subscriptionId;
    private RestAPIStoreImpl restAPIStoreClient;

    @Factory(dataProvider = "userModeDataProvider")
    public NotificationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        storeURLHttp = "https://localhost:9943/";

        //Setting greenMail server
        ServerSetup setup = new ServerSetup(SMTP_TEST_PORT, "localhost", "smtp");
        greenMail = new GreenMail(setup);
        //Creating user in greenMail server
        greenMail.setUser(USER_EMAIL_ADDRESS, EMAIL_USERNAME, EMAIL_PASSWORD);
        try {
            greenMail.start();
        } catch (IllegalStateException e) {
            log.warn("There was a problem starting GreenMail server. Retrying in 10 seconds");
            Thread.sleep(10000);
            greenMail.start();
        }
        log.info("green mail server started");

    }

    @Test(groups = {"wso2.am"}, description = "Testing Notification Feature")
    public void notificationTestCase() throws Exception {

        // Adding API
        String url = getGatewayURLNhttp() + "response";
        String description = "This is a test API created by API manager integration test";

        APIRequest apiRequest;
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(url));
        apiRequest.setProvider(user.getUserName());
        apiRequest.setDescription(description);
        apiRequest.setVersion(API_VERSION);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        //Add the API using the API publisher.
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = apiResponse.getData();

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);

        //publishing API
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);

        UserManagementUtils.signupUser(STORE_USERNAME, STORE_PASSWORD, FIRST_NAME, ORGANIZATION, USER_EMAIL_ADDRESS);

        restAPIStoreClient = new
                RestAPIStoreImpl(STORE_USERNAME, STORE_PASSWORD, SUPER_TENANT_DOMAIN, storeURLHttp);

        // create new application and subscribing
        HttpResponse applicationResponse = restAPIStoreClient.createApplication(APP_NAME,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);

        applicationId = applicationResponse.getData();

        //Subscribe the API to the Application
        HttpResponse responseStore = restAPIStoreClient
                .createSubscription(apiId, applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);

        subscriptionId = responseStore.getData();

        //Create a new Version
        HttpResponse newVersionResponse = restAPIPublisher.copyAPI(NEW_API_VERSION, apiId, null);
        assertEquals(newVersionResponse.getResponseCode(), Response.Status.OK.getStatusCode(), "Response Code Mismatch");

        newApiId = newVersionResponse.getData();

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(newApiId, restAPIPublisher);
        waitForAPIDeployment();

        //Publisher new version
        HttpResponse newVersionPublishResponse = restAPIPublisher
                .changeAPILifeCycleStatus(newApiId, APILifeCycleAction.PUBLISH.getAction(), null);
        assertEquals(newVersionPublishResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code Mismatch");

        // checking whether message is received by greenmail
        greenMail.waitForIncomingEmail(50000, 1);
        Message[] messages = greenMail.getReceivedMessages();

        if (messages.length > 0) {
            assertTrue(true, "Email received BY Greenmail server");
        } else {
            assertTrue(false, "Email NOT received BY Greenmail server");
        }
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


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStoreClient.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(newApiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteAPI(newApiId);
        greenMail.stop();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
        };
    }
}

