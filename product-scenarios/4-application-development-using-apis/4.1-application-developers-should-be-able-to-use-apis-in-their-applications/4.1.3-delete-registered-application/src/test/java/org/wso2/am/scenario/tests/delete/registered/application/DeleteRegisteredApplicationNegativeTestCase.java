package org.wso2.am.scenario.tests.delete.registered.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import static org.osgi.service.application.ApplicationDescriptor.APPLICATION_DESCRIPTION;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class DeleteRegisteredApplicationNegativeTestCase extends ScenarioTestBase {
    private final Log log = LogFactory.getLog(DeleteRegisteredApplicationNegativeTestCase.class);
    private final String APPLICATION_NAME = "DeleteUnownedApplication";
    private final String SUBSCRIBER2_USERNAME = "deleteAppSubscriber2";
    private final String SUBSCRIBER2_PW = "deleteAppSubscriber2";
    private String applicationId = null;
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";


    @Factory(dataProvider = "userModeDataProvider")
    public DeleteRegisteredApplicationNegativeTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                    ADMIN_USERNAME, ADMIN_PW);
            createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, ADMIN_USERNAME, ADMIN_PW);
            createUserWithSubscriberRole(SUBSCRIBER2_USERNAME, SUBSCRIBER2_PW, ADMIN_USERNAME, ADMIN_PW);
        }

        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            // create user in wso2.com tenant
            addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, ADMIN_USERNAME, ADMIN_PW);
            if (isActivated(ScenarioTestConstants.TENANT_WSO2)) {
                //Add and activate wso2.com tenant
                createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                        TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
                createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, TENANT_ADMIN_USERNAME,
                        TENANT_ADMIN_PW);
                createUserWithSubscriberRole(SUBSCRIBER2_USERNAME, SUBSCRIBER2_PW, TENANT_ADMIN_USERNAME,
                        TENANT_ADMIN_PW);
            }
        }
        super.init(userMode);
    }

    @Test(description = "4.1.3.5")
    public void testUnownedDeleteApplication() throws Exception {
        applicationId = createApplication(APPLICATION_NAME);
        RestAPIStoreImpl restAPIStoreNew = new RestAPIStoreImpl(
                SUBSCRIBER2_USERNAME, SUBSCRIBER2_PW, storeContext.getContextTenant().getDomain(), storeURLHttps);
        HttpResponse response = restAPIStoreNew.deleteApplication(applicationId);
        assertNull(response, "Different owners application deleted.");
    }

    @Test(description = "4.1.3.7", dependsOnMethods = {"testUnownedDeleteApplication"})
    public void testDeleteApplicationWithSameName() throws Exception {
        RestAPIStoreImpl restAPIStoreNew = new RestAPIStoreImpl(
                SUBSCRIBER2_USERNAME, SUBSCRIBER2_PW, storeContext.getContextTenant().getDomain(), storeURLHttps);
        HttpResponse applicationResponse = restAPIStoreNew.createApplication(APPLICATION_NAME, APPLICATION_DESCRIPTION,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        assertNotNull(applicationResponse.getData());
        assertFalse(applicationId.equals(applicationResponse.getData()));
        assertNull(restAPIStoreNew.deleteApplication(applicationId));
        ApplicationDTO applicationResponseOld = restAPIStore.getApplicationById(applicationId);
        assertTrue(applicationResponseOld.getApplicationId().contains(applicationId));
        restAPIStoreNew.deleteApplication(applicationResponse.getData());
        restAPIStore.deleteApplication(applicationId);
    }

    private String createApplication(String applicationName) throws Exception {
        String APPLICATION_DESCRIPTION = "ApplicationDescription";
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName, APPLICATION_DESCRIPTION,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        return applicationResponse.getData();
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
            deleteUser(SUBSCRIBER2_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deleteUser(SUBSCRIBER2_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deactivateAndDeleteTenant(ScenarioTestConstants.TENANT_WSO2);
            deactivateAndDeleteTenant(ScenarioTestConstants.TENANT_WSO2);
        }
    }

    @DataProvider
    public static Object[][] userModeDataProvider() throws Exception {
        setup();
        // return the relevant parameters for each test run
        // 1) Super tenant API creator
        // 2) Tenant API creator
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_USER},
                new Object[]{TestUserMode.TENANT_USER},
        };
    }
}
