package org.wso2.am.scenario.tests.delete.registered.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class DeleteRegisteredApplicationNegativeTestCase extends ScenarioTestBase {
    private APIStoreRestClient apiStore;
    private static final Log log = LogFactory.getLog(DeleteRegisteredApplicationNegativeTestCase.class);
    private static final String ADMIN_LOGIN_USERNAME = "admin";
    private static final String ADMIN_LOGIN_PW = "admin";
    private static final String APPLICATION_NAME = "DeleteUnownedApplication";
    private static final String SUBSCRIBER_USERNAME = "deleteAppSubscriber1";
    private static final String SUBSCRIBER_PW = "deleteAppSubscriber1";
    private static final String SUBSCRIBER2_USERNAME = "deleteAppSubscriber2";
    private static final String SUBSCRIBER2_PW = "deleteAppSubscriber2";

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        createUserWithSubscriberRole(SUBSCRIBER_USERNAME, SUBSCRIBER_PW, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        createUserWithSubscriberRole(SUBSCRIBER2_USERNAME, SUBSCRIBER2_PW, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);

        apiStore = new APIStoreRestClient(storeURL);
        apiStore.login(SUBSCRIBER_USERNAME, SUBSCRIBER_PW);
    }

    @Test(description = "4.1.3.5")
    public void testUnownedDeleteApplication() throws Exception {
        HttpResponse addApplicationResponse = apiStore.addApplication(APPLICATION_NAME,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "Description");
        verifyResponse(addApplicationResponse);
        assertEquals(new JSONObject(addApplicationResponse.getData()).get("status"), "APPROVED",
                "Application creation failed for application: DeleteUnownedApplication");

        apiStore.login(SUBSCRIBER2_USERNAME, SUBSCRIBER2_PW);
        HttpResponse deleteResponse = apiStore.removeApplication(APPLICATION_NAME);
        log.info("Delete unowned application Response Code : " + deleteResponse.getResponseCode());
        log.info("Delete unowned application Response Message : " + deleteResponse.getData());
        JSONObject responseData = new JSONObject(deleteResponse.getData());
        Assert.assertTrue(responseData.getBoolean("error"), "Error message received not received when" +
                "deleting unowned application: " + deleteResponse.getData());
        
        HttpResponse getApplicationsResponse = apiStore.getAllApplications();
        log.info("Verify application does not exist in store response code : " +
                getApplicationsResponse.getResponseCode());
        log.info("Verify application does not exist in store response message : " +
                getApplicationsResponse.getData());
        assertFalse(getApplicationsResponse.getData().contains(APPLICATION_NAME),
                "Application still available in store: DeleteUnownedApplication");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.login(SUBSCRIBER_USERNAME, SUBSCRIBER_PW);
        apiStore.removeApplication(APPLICATION_NAME);

        deleteUser(SUBSCRIBER_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        deleteUser(SUBSCRIBER2_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
    }
}
