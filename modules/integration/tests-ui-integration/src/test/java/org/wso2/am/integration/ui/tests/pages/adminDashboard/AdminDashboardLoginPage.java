package org.wso2.am.integration.ui.tests.pages.adminDashboard;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.WebDriver;
import org.wso2.am.integration.ui.tests.pages.PageHandler;
import org.wso2.am.integration.ui.tests.util.APIMTestConstants;

import java.io.IOException;

/**
 * UI Page class of Admin Portal Login page of API Manager
 */
public class AdminDashboardLoginPage extends PageHandler {

    private static final Log log = LogFactory.getLog(AdminDashboardLoginPage.class);
    private WebDriver driver;

    public AdminDashboardLoginPage(WebDriver driver) {
        super(driver);
        this.driver = driver;
        // Check that were on the right page.
        if (!(driver.getCurrentUrl().contains(APIMTestConstants.ADMIN_DASHBOARD_LOGIN_PAGE_URL_VERIFICATION))) {
            throw new IllegalStateException("This is not the Admin Portal login page");
        }
    }

    /**
     * Login to Publisher. after login this page will return to Publisher home page.
     *
     * @param userName Login username
     * @param password Login password
     * @return instance of a PublisherHomePage
     * @throws Exception
     */
    public ConfigureAnalyticsPage getConfigureAnalyticsPage(String userName, CharSequence password)
            throws IOException {

        fillTextBoxById("admin.dashboard.login.username.id", userName);
        fillTextBoxById("admin.dashboard.login.password.id", password);
        clickElementById("admin.dashboard.login.button.id");
        log.info("login as " + userName + " to Admin Portal Page");
        waitUntilElementVisibilityByLinkText("admin.dashboard.menu.configure.analytics", APIMTestConstants.WAIT_TIME_VISIBILITY_ELEMENT_SECONDS);
        clickElementByLinkText("admin.dashboard.menu.configure.analytics");
        return new ConfigureAnalyticsPage(driver);

    }
}
