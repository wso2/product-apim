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

package org.wso2.am.integration.tests.login;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationThrottlePolicyListDTO;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationListDTO;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceStub;
import org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * test case to test login using email user name. test is done for publisher,
 * store and admin-dashboard. modified api manager configurations can be found in
 * configFiles/emailusernametest location
 */
public class EmailUserNameLoginTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(EmailUserNameLoginTestCase.class);
    private ServerConfigurationManager serverConfigurationManager ;
    private UserManagementClient userManagementClient = null;
    private final String INTERNAL_ROLE_SUBSCRIBER = "Internal/subscriber";
    private final String INTERNAL_ROLE_PUBLISHER = "Internal/publisher";
    private final String INTERNAL_ROLE_CREATOR = "Internal/creator";
    private APIListDTO apiListDTO = null;
    private final String TENANT_ADMIN_USERNAME = "emailuser@email.com";
    private final String PASSWORD = "emailuser@email.com";
    private final String TENANT_DOMAIN = "emailuserdomain.com";
    private final String TENANT_USER_USERNAME = "user1@email.com";


    @BeforeTest(alwaysRun = true)
    public void loadConfiguration() throws Exception {

        superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                TestUserMode.SUPER_TENANT_ADMIN);

        String apiManagerXml =
                getAMResourceLocation() + File.separator + "configFiles" + File.separator + "emailusernametest" +
                        File.separator + "deployment.toml";
        try {
            serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
            serverConfigurationManager.applyConfiguration(new File(apiManagerXml));
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Error while changing server configuration", e);
        }
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init();
    }

    @Test(groups = {"wso2.am"}, description = "Email username login test case")
    public void LoginWithEmailUserNameTestCase()
            throws APIManagerIntegrationTestException, org.wso2.am.integration.clients.store.api.ApiException,
            XPathExpressionException, org.wso2.am.integration.clients.admin.ApiException {

        boolean isSuccessful = false;
        try {
            isSuccessful = createTenantWithEmailUserName(TENANT_ADMIN_USERNAME, PASSWORD,
                    TENANT_DOMAIN, publisherContext.getContextUrls().getBackEndUrl());
        } catch (XPathExpressionException | RemoteException | TenantMgtAdminServiceExceptionException e) {
           throw new APIManagerIntegrationTestException(e.getMessage(), e);
        }
        assertEquals(isSuccessful, true);

        try {
            HTTPSClientUtils.doGet(getAPIInvocationURLHttps("/t/emailuserdomain.com/services"), Collections.emptyMap());
        } catch (IOException ignored) {
            log.error(ignored);
        }
        restAPIPublisher = new RestAPIPublisherImpl(TENANT_ADMIN_USERNAME, PASSWORD, TENANT_DOMAIN, "https://localhost:9943/");
        try {
            apiListDTO = restAPIPublisher.apIsApi.getAllAPIs(null, null, TENANT_DOMAIN, null, null, null, null, null);
        } catch (ApiException e) {
            throw new APIManagerIntegrationTestException("Login to Publisher with email username failed due to " +
                    e.getMessage(), e);
        }
        // check for store login with email user name
        restAPIStore = new RestAPIStoreImpl(TENANT_ADMIN_USERNAME, PASSWORD, TENANT_DOMAIN,"https://localhost:9943/"
        );
        ApplicationListDTO responseData = restAPIStore.getAllApps();
        assertNotNull(responseData, "Login to Store with email username failed");

        // check for Admin Portal login with email user name
        restAPIAdmin = new RestAPIAdminImpl(TENANT_ADMIN_USERNAME, PASSWORD, TENANT_DOMAIN, "https://localhost:9943/");
        ApplicationThrottlePolicyListDTO listDTO =
                restAPIAdmin.applicationPolicyCollectionApi.throttlingPoliciesApplicationGet(null, null, null);
        assertNotNull(listDTO, "Login to Admin portal with email username failed");
    }

    @Test(groups = {"wso2.am"}, description = "Login with email username for tenant user",
            dependsOnMethods = "LoginWithEmailUserNameTestCase")
    public void LoginWithTenantUserEmailUserNameTestCase() throws Exception {
        String fullUserName = TENANT_ADMIN_USERNAME + "@" + TENANT_DOMAIN;

        userManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                fullUserName, PASSWORD);
        //add tenant user
        userManagementClient.addUser(TENANT_USER_USERNAME, PASSWORD,
                new String[]{INTERNAL_ROLE_SUBSCRIBER, INTERNAL_ROLE_PUBLISHER, INTERNAL_ROLE_CREATOR}, TENANT_USER_USERNAME);

        restAPIPublisher = new RestAPIPublisherImpl(TENANT_USER_USERNAME, PASSWORD,
                TENANT_DOMAIN, publisherURLHttps);

        try {
            apiListDTO = restAPIPublisher.apIsApi.getAllAPIs(null, null, "emailuserdomain.com",
                    null, null, null, null, null);
        } catch (ApiException e) {
            throw new APIManagerIntegrationTestException("Login to Publisher with email username failed due to " +
                    e.getMessage(), e);
        }

        // check for store login with email user name
        restAPIStore = new RestAPIStoreImpl(TENANT_USER_USERNAME, PASSWORD, TENANT_DOMAIN, storeURLHttps);
        ApplicationListDTO responseData = restAPIStore.getAllApps();
        assertNotNull(responseData, "Login to Store with email username failed");

        // check for Admin Portal login with email user name
        restAPIAdmin = new RestAPIAdminImpl(TENANT_USER_USERNAME, PASSWORD, TENANT_DOMAIN, adminURLHttps);
        ApplicationThrottlePolicyListDTO listDTO =
                restAPIAdmin.applicationPolicyCollectionApi.throttlingPoliciesApplicationGet(null, null, null);
        assertNotNull(listDTO, "Login to Admin portal with email username failed");
    }

    /**
     * create a new tenant with email address as the user name.
     *
     * @param userNameWithEmail tenant name with email
     * @param pwd               tenant password
     * @param domainName        tenant domain name
     * @param backendUrl        apim backend url
     * @return boolean whether tenant creation was successful or not
     */
    private boolean createTenantWithEmailUserName(String userNameWithEmail, String pwd,
                                                  String domainName, String backendUrl)
            throws XPathExpressionException, RemoteException, TenantMgtAdminServiceExceptionException {
        boolean isSuccess = false;

        String endPoint = backendUrl + "TenantMgtAdminService";
        TenantMgtAdminServiceStub tenantMgtAdminServiceStub =
                new TenantMgtAdminServiceStub(
                        endPoint);
        AuthenticateStub.authenticateStub(publisherContext.getSuperTenant().getContextUser().getUserName(),
                publisherContext.getSuperTenant().getContextUser().getUserName(), tenantMgtAdminServiceStub);

        Date date = new Date();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);

        TenantInfoBean tenantInfoBean = new TenantInfoBean();
        tenantInfoBean.setActive(true);
        tenantInfoBean.setEmail(userNameWithEmail);
        tenantInfoBean.setAdminPassword(pwd);
        tenantInfoBean.setAdmin(userNameWithEmail);
        tenantInfoBean.setTenantDomain(domainName);
        tenantInfoBean.setCreatedDate(calendar);
        tenantInfoBean.setFirstname("emailuser first name");
        tenantInfoBean.setLastname("email user last name");
        tenantInfoBean.setSuccessKey("true");
        tenantInfoBean.setUsagePlan("demo");

        TenantInfoBean tenantInfoBeanGet;

        tenantInfoBeanGet = tenantMgtAdminServiceStub.getTenant(domainName);

        if (!tenantInfoBeanGet.getActive() && tenantInfoBeanGet.getTenantId() != 0) {
            tenantMgtAdminServiceStub.activateTenant(domainName);
            log.info("Tenant domain " + domainName + " Activated successfully");

        } else if (!tenantInfoBeanGet.getActive() && tenantInfoBeanGet.getTenantId() == 0) {
            tenantMgtAdminServiceStub.addTenant(tenantInfoBean);
            tenantMgtAdminServiceStub.activateTenant(domainName);
            log.info("Tenant domain " + domainName +
                    " created and activated successfully");
            log.info("Tenant domain " + domainName + " created and activated successfully");
            isSuccess = true;
        } else {
            log.info("Tenant domain " + domainName + " already registered");
        }
        return isSuccess;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }

    @AfterTest(alwaysRun = true)
    public void restoreConfiguration() throws Exception {
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
        serverConfigurationManager.restoreToLastConfiguration();
    }

}
