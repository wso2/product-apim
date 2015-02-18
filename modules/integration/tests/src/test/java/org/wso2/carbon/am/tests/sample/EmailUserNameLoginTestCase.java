/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.am.tests.sample;

import java.io.File;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.am.tests.APIManagerIntegrationTest;
import org.wso2.carbon.am.tests.util.APIPublisherRestClient;
import org.wso2.carbon.am.tests.util.APIStoreRestClient;
import org.wso2.carbon.am.tests.util.WorkFlowAdminRestClient;
import org.wso2.carbon.automation.api.clients.utils.AuthenticateStub;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.utils.HttpResponse;
import org.wso2.carbon.automation.core.utils.serverutils.ServerConfigurationManager;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceStub;
import org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean;

/**
 * test case to test login using email user name. test is done for publisher,
 * store and admin-dashboard. modified api manager configurations can be found in
 * configFiles/emailusernametest location
 */
public class EmailUserNameLoginTestCase extends APIManagerIntegrationTest {

	private APIPublisherRestClient apiPublisher;
	private APIStoreRestClient apiStore;
	private WorkFlowAdminRestClient workflowAdmin;
	private ServerConfigurationManager serverConfigurationManager;
	private String publisherURLHttp;
	private String storeURLHttp;
	private String workflowAdminURLHTTP;
	private String password;
	private String domainName;
	private String userNameWithEmail;
	private String fullUserName;

	@Override
	@BeforeClass(alwaysRun = true)
	public void init() throws Exception {
		super.init(0);

		if (isBuilderEnabled()) {
			publisherURLHttp = getServerURLHttp();
			storeURLHttp = getServerURLHttp();
			workflowAdminURLHTTP = getServerURLHttp();

			String apimanagerxml =
					ProductConstant.getResourceLocations(ProductConstant.AM_SERVER_NAME) +
					File.separator +
					"configFiles/emailusernametest/" +
					"api-manager.xml";

			String usermgtxml =
					ProductConstant.getResourceLocations(ProductConstant.AM_SERVER_NAME) +
					File.separator +
					"configFiles/emailusernametest/" +
					"user-mgt.xml";

			String carbonxml =
					ProductConstant.getResourceLocations(ProductConstant.AM_SERVER_NAME) +
					File.separator +
					"configFiles/emailusernametest/" +
					"carbon.xml";

			serverConfigurationManager = new ServerConfigurationManager(amServer.getBackEndUrl());
			serverConfigurationManager.applyConfiguration(new File(apimanagerxml));
			serverConfigurationManager.applyConfiguration(new File(usermgtxml));
			serverConfigurationManager.applyConfiguration(new File(carbonxml));

			super.init(0);

		} else {
			publisherURLHttp = getPublisherServerURLHttp();
			storeURLHttp = getStoreServerURLHttp();
			workflowAdminURLHTTP = getPublisherServerURLHttp();

		}
		apiPublisher = new APIPublisherRestClient(publisherURLHttp);
		apiStore = new APIStoreRestClient(storeURLHttp);
		workflowAdmin = new WorkFlowAdminRestClient(workflowAdminURLHTTP);

	}

	@Test(groups = { "wso2.am" }, description = "Email username login test case")
	public void LoginWithEmailUserNameTestCase() throws Exception {

		userNameWithEmail = "tenant@wso2.com";
		password = "admin123";
		domainName = "tenant.com";
		fullUserName = userNameWithEmail + "@" + domainName;
		boolean isSuccessful =
				createTenantWithEmailUserName(userNameWithEmail, password,
				                              domainName, amServer.getBackEndUrl());
		Assert.assertEquals(isSuccessful, true);

		HttpResponse login = null;

		// check for publisher login with email user name
		login = apiPublisher.login(fullUserName, password);
		Assert.assertEquals(login.getResponseCode(), 200,
				"Login to Publisher with email username failed");
		// check for store login with email user name
		login = apiStore.login(fullUserName, password);
		Assert.assertEquals(login.getResponseCode(), 200,
				"Login to Store with email username failed");
		// check for admin dashboard login with email user name
		login = workflowAdmin.login(fullUserName, password);
		Assert.assertEquals(login.getResponseCode(), 200,
				"Login to Admin dashboard Login to Publisher with email username failed");
	}

	/**
	 * create a new tenant with email address as the user name. 
	 * @param userNameWithEmail
	 * @param pwd
	 * @param domainName
	 * @param backendUrl
	 * @return
	 */
	private boolean createTenantWithEmailUserName(String userNameWithEmail, String pwd,
	                                              String domainName, String backendUrl) {
		boolean isSuccess = false;
		try {
			String endPoint = backendUrl + "TenantMgtAdminService";
			TenantMgtAdminServiceStub tenantMgtAdminServiceStub =
					new TenantMgtAdminServiceStub(
					                              endPoint);
			AuthenticateStub.authenticateStub("admin", "admin", tenantMgtAdminServiceStub);

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
			tenantInfoBean.setFirstname("admin");
			tenantInfoBean.setLastname("admin" + "wso2automation");
			tenantInfoBean.setSuccessKey("true");
			tenantInfoBean.setUsagePlan("demo");

			TenantInfoBean tenantInfoBeanGet;

			tenantInfoBeanGet = tenantMgtAdminServiceStub.getTenant(domainName);

			if (!tenantInfoBeanGet.getActive() && tenantInfoBeanGet.getTenantId() != 0) {
				tenantMgtAdminServiceStub.activateTenant(domainName);
				System.out.println("Tenant domain " + domainName + " Activated successfully");
				log.info("Tenant domain " + domainName + " Activated successfully");

			} else if (!tenantInfoBeanGet.getActive() && tenantInfoBeanGet.getTenantId() == 0) {
				tenantMgtAdminServiceStub.addTenant(tenantInfoBean);
				tenantMgtAdminServiceStub.activateTenant(domainName);
				System.out.println("Tenant domain " + domainName +
						" created and activated successfully");
				log.info("Tenant domain " + domainName + " created and activated successfully");
				isSuccess = true;
			} else {
				System.out.println("Tenant domain " + domainName + " already registered");
				log.info("Tenant domain " + domainName + " already registered");
			}
		} catch (RemoteException e) {
			log.error("RemoteException thrown while adding user/tenants : ", e);

		} catch (TenantMgtAdminServiceExceptionException e) {
			log.error("Error connecting to the TenantMgtAdminService : ", e);
		}

		return isSuccess;
	}

	@AfterClass(alwaysRun = true)
	public void destroy() throws Exception {
        serverConfigurationManager.restoreToLastConfiguration();
		super.cleanup();
	}

}
