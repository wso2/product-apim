package org.wso2.am.integration.tests.application;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.identity.oauth.stub.OAuthAdminServiceStub;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;

import java.rmi.RemoteException;

import static org.testng.Assert.assertTrue;

/**
 * Test whether changing call back url of an application of a certain user, update the call back url value of another
 * user's similarly named application in the database table IDN_OAUTH_CONSUMER_APPS
 * This is related to the public jira https://wso2.org/jira/browse/CAPIMGT-12
 */
public class CAPIMGT12CallBackURLOverwriteTestCase extends APIMIntegrationBaseTest {
    private APIStoreRestClient apiStore;
    private String publisherURLHttp;
    private String storeURLHttp;
    String consumerKeyUser1App;
    String consumerKeyUser2App;
    private static final String TEST_USER_1 = "TestUser1001";
    private static final String TEST_USER_2 = "TestUser2002";
    private static final String TEST_USER_1_PASSWORD = "12345";
    private static final String TEST_USER_2_PASSWORD = "12345";
    private static final String USER_1_TEST_APPLICATION = "TestApplication";
    private static final String USER_2_TEST_APPLICATION = "TestApplication";
    private static final String USER_1_TEST_APP_INITIAL_CBU = "www.user1-app-initial-callback-url.com";
    private static final String USER_2_TEST_APP_INITIAL_CBU = "www.user2-app-initial-callback-url.com";
    private static final String USER_2_TEST_APP_UPDATED_CBU = "www.user2-app-updated-callback-url.com";
    private static final Log log = LogFactory.getLog(CAPIMGT12CallBackURLOverwriteTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {
        super.init();
        try {
            userManagementClient.addUser(TEST_USER_1, TEST_USER_1_PASSWORD, new String[] { "admin" }, "manager");
            userManagementClient.addUser(TEST_USER_2, TEST_USER_2_PASSWORD, new String[] { "admin" }, "manager");
        } catch (UserAdminUserAdminException e) {
            log.error("Error while creating the test users");
            //Fail the test case.
            Assert.fail(e.getMessage());
        } catch (RemoteException e) {
            log.error("Error while creating the test users");
            //Fail the test case.
            Assert.fail(e.getMessage());
        }

        publisherURLHttp = publisherUrls.getWebAppURLHttp();
        storeURLHttp = storeUrls.getWebAppURLHttp();

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);
    }

    @Test(groups = { "wso2.am" }, description = "Application call back url update and overwriting test case")
    public void callBackUrlUpdateTestCase() throws Exception {
        addAppsAndGenerateKeys();
        //update the USER_2_TEST_APPLICATION's call back url
        String jsonParams = "{\"grant_types\":\"urn:ietf:params:oauth:grant-type:saml2-bearer,iwa:ntlm\"}";
        apiStore.updateClientApplication(USER_2_TEST_APPLICATION, "PRODUCTION", "ALL", String.valueOf(false),
                jsonParams, USER_2_TEST_APP_UPDATED_CBU);

        //query WSO2AM_DB-table IDN_OAUTH_CONSUMER_APPS to check both user's both app's call back urls
        OAuthAdminServiceStub oAuthAdminServiceStub = new OAuthAdminServiceStub(
                getKeyManagerURLHttps() + "services/OAuthAdminService");
        ServiceClient client = oAuthAdminServiceStub._getServiceClient();
        Options client_options = client.getOptions();
        HttpTransportProperties.Authenticator authenticator = new HttpTransportProperties.Authenticator();
        authenticator.setUsername("admin");
        authenticator.setPassword("admin");
        authenticator.setPreemptiveAuthentication(true);
        client_options.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, authenticator);
        client.setOptions(client_options);

        String finalCallbackUrlUser1App = oAuthAdminServiceStub.getOAuthApplicationData(consumerKeyUser1App)
                .getCallbackUrl();
        String finalCallbackUrlUser2App = oAuthAdminServiceStub.getOAuthApplicationData(consumerKeyUser2App)
                .getCallbackUrl();
        Assert.assertEquals(finalCallbackUrlUser1App, USER_1_TEST_APP_INITIAL_CBU,
                "Call back URL of the first user's application has been overwritten due to the update of the call"
                        + " back URL of second user's application.");
        Assert.assertNotEquals(finalCallbackUrlUser1App, finalCallbackUrlUser2App,
                "Call back URL of the first user's application has been overwritten due to the update of the call"
                        + " back URL of second user's application.");
    }

    /**
     * Add new application and Generate production keys for each the users (similar
     * names for both the applications)
     *
     * @throws Exception
     */
    public void addAppsAndGenerateKeys() throws Exception {
        //TEST_USER_1
        //Subscribe to API with a new application
        apiStore.login(TEST_USER_1, TEST_USER_1_PASSWORD);
        apiStore.addApplication(USER_1_TEST_APPLICATION, "Unlimited", "", "This-is-sample-application");
        //Generate production key
        APPKeyRequestGenerator generateAppKeyRequest1 = new APPKeyRequestGenerator(USER_1_TEST_APPLICATION);
        generateAppKeyRequest1.setCallbackUrl(USER_1_TEST_APP_INITIAL_CBU);
        String responseString1 = apiStore.generateApplicationKey(generateAppKeyRequest1).getData();
        JSONObject jsonObject1 = new JSONObject(responseString1);
        consumerKeyUser1App = ((JSONObject) ((JSONObject) jsonObject1.get("data")).get("key")).getString("consumerKey");

        //TEST_USER_2
        //Subscribe to API with a new application
        apiStore = new APIStoreRestClient(storeURLHttp);
        apiStore.login(TEST_USER_2, TEST_USER_2_PASSWORD);
        apiStore.addApplication(USER_2_TEST_APPLICATION, "Unlimited", "", "This-is-sample-application");
        //Generate production key
        APPKeyRequestGenerator generateAppKeyRequest2 = new APPKeyRequestGenerator(USER_2_TEST_APPLICATION);
        generateAppKeyRequest2.setCallbackUrl(USER_2_TEST_APP_INITIAL_CBU);
        String responseString2 = apiStore.generateApplicationKey(generateAppKeyRequest2).getData();
        JSONObject jsonObject2 = new JSONObject(responseString2);
        consumerKeyUser2App = ((JSONObject) ((JSONObject) jsonObject2.get("data")).get("key")).getString("consumerKey");
    }

    @AfterClass(alwaysRun = true) public void destroy() throws Exception {
        super.cleanUp();
    }
}
