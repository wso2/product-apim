/*
*Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.am.integration.ui;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.api.clients.user.mgt.UserManagementClient;
import org.wso2.carbon.automation.api.clients.utils.AuthenticateStub;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceStub;
import org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class APIDocsDownloadTestCase extends AMIntegrationUiTestBase {
	private WebDriver driver;
	WebDriverWait wait;
	private static final Log
			log = LogFactory.getLog(APIDocsDownloadTestCase.class);
	String resourceFile = ProductConstant.getResourceLocations(
			ProductConstant.AM_SERVER_NAME + File.separator +
			"configFiles/downloaddoctest/sampleDownloadFile");

	@BeforeClass(alwaysRun = true)
	public void setUp() throws Exception {
		super.init();
		FirefoxProfile firefoxProfile = new FirefoxProfile();
		firefoxProfile.setPreference("browser.download.folderList", 2);
		firefoxProfile.setPreference("browser.download.manager.showWhenStarting", false);
		firefoxProfile
				.setPreference("browser.download.dir", System.getProperty("download.location"));
		firefoxProfile.setPreference("browser.helperApps.alwaysAsk.force", false);
		firefoxProfile.setPreference("browser.helperApps.neverAsk.saveToDisk",
		                             "text/csv, application/pdf, application/x-msexcel,application/excel," +
		                             "application/x-excel,application/excel,application/x-excel,application/excel, " +
		                             "application/vnd.ms- excel,application/x-excel,application/x-msexcel,image/png," +
		                             "image/jpeg,text/html,text/plain,application/msword,application/xml," +
		                             "application/excel,text/x-c,application/force-download");
		driver = new FirefoxDriver(firefoxProfile);
		wait = new WebDriverWait(driver, 60);
	}

	@Test(groups = "wso2.am", description = "Download API Doc from publisher in restricted by role")
	public void DownloadFromStoreRestrictedRolesVisibilitySuperTenantUserTestCase()
			throws Exception {
		UserManagementClient userManagementClient =
				new UserManagementClient(amServer.getBackEndUrl(), "admin", "admin");
		userManagementClient
				.addUser("admin123", "admin123", new String[] { "Internal/subscriber" },
				         "manager");
		userManagementClient.addRole("man", new String[] { "admin123" }, null);
		createAPIWithDocument("test1", "Restricted By Roles", "man", "admin", "admin",
		                      MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
		Thread.sleep(2000);
		publisherLogout();
		downloadDocFromStore("admin123", "admin123", "carbon.super", "test1");
	}

	@Test(groups = "wso2.am", description = "Download API Doc from publisher in visible to my domain " +
	                                        "from tenant", dependsOnMethods = {
			"DownloadFromStoreRestrictedRolesVisibilitySuperTenantUserTestCase" })
	public void DownloadFromPublisherTenantUserTestCase()
			throws TenantMgtAdminServiceExceptionException, InterruptedException, IOException,
			       NoSuchAlgorithmException {
		boolean isSuccessful =
				createTenantWithEmailUserName("abc", "wso2carbon",
				                              "dom.com", amServer.getBackEndUrl());
		if (isSuccessful) {
			createAPIWithDocument("test", "Visible to my domain", null, "abc", "wso2carbon",
			                      "dom.com");
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Open")));
			driver.findElement(By.linkText("Open")).click();
			File file = new File(
					System.getProperty("download.location") + File.separator +
					"sampleDownloadFile");
			while (!file.exists()) {
				Thread.sleep(1000);
			}
			publisherLogout();
			Assert.assertTrue(IOUtils.contentEquals(new FileInputStream(file), new FileInputStream(resourceFile)));
			file.delete();
		}
	}

	@Test(groups = "wso2.am", description = "Download API Doc from store in Super Tenant when visible" +
	                                        " " +
	                                        "set to my domain", dependsOnMethods = {
			"DownloadFromPublisherTenantUserTestCase" })
	public void DownloadFromStoreTenantUserTestCase() throws Exception {
		downloadDocFromStore("abc", "wso2carbon", "dom.com", "test");
	}

	@Test(groups = "wso2.am", description = "Download API Doc from publisher in restricted by role",
			dependsOnMethods = "DownloadFromStoreTenantUserTestCase")
	public void DownloadFromStoreRestrictedRolesVisibilityTenantUserTestCase()
			throws Exception {
		UserManagementClient userManagementClient =
				new UserManagementClient(amServer.getBackEndUrl(), "abc@dom.com", "wso2carbon");
		userManagementClient
				.addUser("admin123", "wso2carbon", new String[] { },
				         "manager");
		userManagementClient.addRole("man", new String[] { "admin123" },
		                             new String[] { "/permission/admin/manage/api/subscribe",
		                                            "/permission/admin/login" });
		createAPIWithDocument("test1", "Restricted By Roles", "man", "abc", "wso2carbon",
		                      "dom.com");
		Thread.sleep(2000);
		publisherLogout();
		downloadDocFromStore("admin123", "wso2carbon", "dom.com", "test1");
	}

	@AfterClass(alwaysRun = true)
	public void tearDown() throws Exception {
		driver.quit();
		super.cleanup();
	}

	/**
	 * @param username username to login to store
	 * @param password password of the user
	 * @param tenant   domain of the login user
	 * @param apiName  api name of the published api
	 * @throws java.io.IOException                    if downloaded/uploaded file couldn't found
	 * @throws java.security.NoSuchAlgorithmException if md5 hash algorithm not in environment
	 */
	private void downloadDocFromStore(String username, String password, String tenant,
	                                  String apiName) throws IOException, NoSuchAlgorithmException,
	                                                         InterruptedException {
		try {
			driver.get(getStoreURL(ProductConstant.AM_SERVER_NAME) + "?tenant=" + tenant);
		} catch (Exception e) {
			log.error("Couldn't retrieve store url of api manager");
		}
		wait.until(ExpectedConditions
				           .visibilityOfElementLocated(By.id("login-link")));
		WebElement login = driver.findElement(By.id("login-link"));
		if (login != null) {
			login.click();
			driver.findElement(By.id("username")).clear();
			driver.findElement(By.id("username")).sendKeys(username + "@" + tenant);
			driver.findElement(By.id("password")).clear();
			driver.findElement(By.id("password")).sendKeys(password);
			driver.findElement(By.id("loginBtn")).click();
		}
		Thread.sleep(1000);
		String loginButton;
		if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenant)) {
			loginButton = username + "@" + tenant;
		} else {
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText(username)));
			loginButton = username;
		}
		if (wait.until(ExpectedConditions
				               .visibilityOfElementLocated(By.linkText(loginButton))).isDisplayed()) {
			while (true) {
				driver.findElement(By.linkText("APIs")).click();
				Thread.sleep(3000);
				boolean apiElement = elementIsVisible(By.linkText(apiName + "-1.0.0"));
				if (apiElement) {
					break;
				}
			}
		}
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText(apiName + "-1.0.0")));
		driver.findElement(By.linkText(apiName + "-1.0.0")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Documentation")));
		driver.findElement(By.linkText("Documentation")).click();
		driver.findElement(By.linkText("How To")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Download")));
		driver.findElement(By.linkText("Download")).click();
		File file = new File(
				System.getProperty("download.location") + File.separator + "sampleDownloadFile");
		while (!file.exists()) {
			Thread.sleep(1000);
		}
		wait.until(ExpectedConditions
				           .visibilityOfElementLocated(By.linkText(loginButton)));
		driver.findElement(By.linkText(loginButton)).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("logout-link")));
		driver.findElement(By.id("logout-link")).click();

		Assert.assertTrue(IOUtils.contentEquals(new FileInputStream(file), new FileInputStream(resourceFile)));
		file.delete();
	}

	/**
	 * To Create api
	 *
	 * @param apiName    api name want to publish
	 * @param visibility visibility of api(public,restricted,visible to my domain)
	 * @param role       if roles restricted what roles restricted
	 * @param username   username to login to publisher
	 * @param password   password of the user
	 * @param domain     domain of the login user
	 * @throws InterruptedException if thread sleep during click is interrupted
	 */
	public void createAPIWithDocument(String apiName, String visibility, String role,
	                                  String username, String password, String domain) throws InterruptedException {
		try {
			driver.get(getPublisherURL(ProductConstant.AM_SERVER_NAME));
		} catch (Exception ex) {
			log.error("couldn't retrieve publisher url", ex);
		}
		if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(domain)) {
			driver.findElement(By.id("username")).sendKeys(username + "@" + domain);
		} else {
			driver.findElement(By.id("username")).sendKeys(username);
		}
		driver.findElement(By.id("pass")).sendKeys(password);
		driver.findElement(By.id("loginButton")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Add")));
		driver.findElement(By.linkText("Add")).click();
		driver.findElement(By.id("name")).clear();
		driver.findElement(By.id("name")).sendKeys(apiName);
		if ("Visible to my domain".equals(visibility)) {
			new Select(driver.findElement(By.id("visibility"))).selectByVisibleText(
					"Visible to my domain");
		} else if ("Restricted By Roles".equals(visibility) && role != null) {
			new Select(driver.findElement(By.id("visibility"))).selectByVisibleText(
					"Restricted by roles");
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("roles")));
			driver.findElement(By.id("roles")).sendKeys(role);
		}
		driver.findElement(By.id("context")).clear();
		driver.findElement(By.id("context")).sendKeys(apiName);
		driver.findElement(By.id("version")).clear();
		driver.findElement(By.id("version")).sendKeys("1.0.0");
		driver.findElement(By.id("go_to_implement")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Yes")));
		driver.findElement(By.linkText("Yes")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(
				"jsonform-0-elt-production_endpoints")));
		driver.findElement(By.id("jsonform-0-elt-production_endpoints")).sendKeys("http");
		driver.findElement(By.id("go_to_manage")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("publish_api")));
		driver.findElement(By.xpath("//button[@type='button']")).click();
		driver.findElement(By.xpath("//input[@value='Gold']")).click();
		driver.findElement(By.id("publish_api")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Browse")));
		driver.findElement(By.linkText("Browse")).click();
		driver.findElement(By.linkText(apiName)).click();

		Thread.sleep(2000);

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("lifecyclesLink")));
		driver.findElement(By.id("lifecyclesLink")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("editStatus")));
		Select status = new Select(driver.findElement(By.id("editStatus")));
		if (!"PUBLISHED".equals(status.getFirstSelectedOption().getText())) {
			status.selectByVisibleText("PUBLISHED");
		}

		driver.findElement(By.id("updateStateButton")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("docsLink")));
		driver.findElement(By.id("docsLink")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Add New Document")));
		driver.findElement(By.linkText("Add New Document")).click();
		driver.findElement(By.id("docName")).clear();
		driver.findElement(By.id("docName")).sendKeys("how to");
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("optionsRadios8")));
		driver.findElement(By.id("optionsRadios8")).click();
		driver.findElement(By.xpath("//div[@id='newDoc']/div/div[3]/div/div/div/label[3]")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("sourceFile")));
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("docLocation")));
		driver.findElement(By.id("docLocation")).sendKeys(resourceFile);
		driver.findElement(By.id("saveDocBtn")).click();
	}

	/**
	 * this method used to logout from the publisher
	 */

	private void publisherLogout() {
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("userMenu")));
		driver.findElement(By.id("userMenu")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(
				"button.btn.btn-danger")));
		driver.findElement(By.cssSelector("button.btn.btn-danger")).click();
	}

	/**
	 * @param userName   username to domain admin
	 * @param pwd        password of domain admin
	 * @param domainName domain name
	 * @param backendUrl backendUrl of the server
	 * @return if tenant created successfully returns true else false
	 * @throws org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException
	 */
	private boolean createTenantWithEmailUserName(String userName, String pwd,
	                                              String domainName, String backendUrl)
			throws TenantMgtAdminServiceExceptionException {
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
			tenantInfoBean.setEmail("abc@fds.com");
			tenantInfoBean.setAdminPassword(pwd);
			tenantInfoBean.setAdmin(userName);
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

			} else if (!tenantInfoBeanGet.getActive()) {
				tenantMgtAdminServiceStub.addTenant(tenantInfoBean);
				tenantMgtAdminServiceStub.activateTenant(domainName);
				log.info("Tenant domain " + domainName + " created and activated successfully");
				isSuccess = true;
			} else {
				log.info("Tenant domain " + domainName + " already registered");
			}
		} catch (RemoteException e) {
			//we do not need to throw exception because if exception occurred test cases will automatically fail.
			log.error("RemoteException thrown while adding user/tenants : ", e);

		} catch (TenantMgtAdminServiceExceptionException e) {
			//we do not need to throw exception because if exception occurred test cases will automatically fail.
			log.error("Error connecting to the TenantMgtAdminService : ", e);
		}

		return isSuccess;
	}

	/**
	 * @param elementToFind for visibility
	 * @return true if element is visible
	 */
	public boolean elementIsVisible(By elementToFind) {
		try {
			driver.findElement(elementToFind);
			return true;
		} catch (NoSuchElementException ex) {
			return false;
		}
	}
}