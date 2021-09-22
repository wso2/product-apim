package org.wso2.am.integration.tests.application;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.ApiResponse;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIThrottlingTier;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.ws.rs.core.Response;
import java.rmi.RemoteException;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Test whether changing call back url of an application of a certain user, update the call back url value of another
 * user's similarly named application in the database table IDN_OAUTH_CONSUMER_APPS
 * This is related to the public jira https://wso2.org/jira/browse/CAPIMGT-12
 */
public class CAPIMGT12CallBackURLOverwriteTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(CAPIMGT12CallBackURLOverwriteTestCase.class);
    private static final String TEST_USER_1 = "TestUser1001";
    private static final String TEST_USER_2 = "TestUser2002";
    private static final String TEST_USER_1_PASSWORD = "12345";
    private static final String TEST_USER_2_PASSWORD = "12345";
    private static final String TEST_APPLICATION = "TestApplication";
    private static final String USER_1_TEST_APP_INITIAL_CBU = "www.user1-app-initial-callback-url.com";
    private static final String USER_2_TEST_APP_INITIAL_CBU = "www.user2-app-initial-callback-url.com";
    private static final String USER_2_TEST_APP_UPDATED_CBU = "www.user2-app-updated-callback-url.com";

    private ArrayList<String> grantTypes = new ArrayList<>();
    private RestAPIStoreImpl user1ApiStore;
    private RestAPIStoreImpl user2ApiStore;
    private String user1ApplicationId;
    private String user2ApplicationId;

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

        storeURLHttp = storeUrls.getWebAppURLHttp();

        user1ApiStore = new RestAPIStoreImpl(TEST_USER_1, TEST_USER_1_PASSWORD,
                MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, storeURLHttps);
        user2ApiStore = new RestAPIStoreImpl(TEST_USER_2, TEST_USER_2_PASSWORD,
                MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, storeURLHttps);

        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
    }

    @Test(groups = {
            "wso2.am" }, description = "Application call back url update and overwriting test case")
    public void callBackUrlUpdateTestCase() throws Exception {
        addAppsAndGenerateKeys();
        //update the USER_2_TEST_APPLICATION's call back url
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.SAML2);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.NTLM);

        ApplicationKeyDTO applicationKeyDTO = new ApplicationKeyDTO();
        applicationKeyDTO.setKeyType(ApplicationKeyDTO.KeyTypeEnum.PRODUCTION);
        applicationKeyDTO.setCallbackUrl(USER_2_TEST_APP_UPDATED_CBU);
        applicationKeyDTO.setSupportedGrantTypes(grantTypes);

        ApiResponse<ApplicationKeyDTO> updateResponse = user2ApiStore
                .updateKeys(user2ApplicationId, ApplicationKeyDTO.KeyTypeEnum.PRODUCTION.toString(), applicationKeyDTO);
        assertEquals(updateResponse.getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when adding an application");

        ApiResponse<ApplicationKeyDTO> getKeysForUser1Response = user1ApiStore
                .getApplicationKeysByKeyType(user1ApplicationId, ApplicationKeyDTO.KeyTypeEnum.PRODUCTION.getValue());
        Assert.assertEquals(getKeysForUser1Response.getStatusCode(), Response.Status.OK.getStatusCode(),
                "Error when re-generating consumer secret for application: " + user1ApplicationId);
        Assert.assertNotNull(getKeysForUser1Response.getData().getConsumerKey(),
                "Error in generating keys for application: " + TEST_APPLICATION);

        ApiResponse<ApplicationKeyDTO> getKeysForUser2Response = user2ApiStore
                .getApplicationKeysByKeyType(user2ApplicationId, ApplicationKeyDTO.KeyTypeEnum.PRODUCTION.getValue());
        Assert.assertEquals(getKeysForUser2Response.getStatusCode(), Response.Status.OK.getStatusCode(),
                "Error when re-generating consumer secret for application: " + user2ApplicationId);
        Assert.assertNotNull(getKeysForUser2Response.getData().getConsumerKey(),
                "Error in generating keys for application: " + TEST_APPLICATION);

        String finalCallbackUrlUser1App = getKeysForUser1Response.getData().getCallbackUrl();
        String finalCallbackUrlUser2App = getKeysForUser2Response.getData().getCallbackUrl();
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
        HttpResponse applicationResponse = user1ApiStore
                .createApplication(TEST_APPLICATION, "Test Application", APIThrottlingTier.UNLIMITED.getState(),
                        ApplicationDTO.TokenTypeEnum.JWT);
        assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        user1ApplicationId = applicationResponse.getData();
        //Generate production key
        //generate keys for the subscription
        ApplicationKeyDTO applicationKeyDTO = user1ApiStore
                .generateKeys(user1ApplicationId, "3600", USER_1_TEST_APP_INITIAL_CBU,
                        ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        assertNotNull(applicationKeyDTO.getToken().getAccessToken());

        //TEST_USER_2
        //Subscribe to API with a new application

        applicationResponse = user2ApiStore
                .createApplication(TEST_APPLICATION, "Test Application", APIThrottlingTier.UNLIMITED.getState(),
                        ApplicationDTO.TokenTypeEnum.JWT);
        assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        user2ApplicationId = applicationResponse.getData();

        //Generate production key
        applicationKeyDTO = user2ApiStore.generateKeys(user2ApplicationId, "3600", USER_2_TEST_APP_INITIAL_CBU,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        assertNotNull(applicationKeyDTO.getToken().getAccessToken());
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        user1ApiStore.deleteApplication(user1ApplicationId);
        user2ApiStore.deleteApplication(user2ApplicationId);
        userManagementClient.deleteUser(TEST_USER_1);
        userManagementClient.deleteUser(TEST_USER_2);
    }
}
