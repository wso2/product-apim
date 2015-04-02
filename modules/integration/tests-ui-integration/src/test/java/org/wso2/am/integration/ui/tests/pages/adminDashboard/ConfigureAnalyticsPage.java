package org.wso2.am.integration.ui.tests.pages.adminDashboard;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.WebDriver;
import org.wso2.am.integration.ui.tests.pages.PageHandler;
import org.wso2.am.integration.ui.tests.util.APIMTestConstants;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * UI Page class of admin-dashboard Configure Analytics page of API Manager
 */
public class ConfigureAnalyticsPage extends PageHandler {

    private static final Log log = LogFactory.getLog(ConfigureAnalyticsPage.class);

    public ConfigureAnalyticsPage(WebDriver driver) {
        super(driver);
        // Check that we're on the right page.
        if (!driver.getCurrentUrl().contains(APIMTestConstants.ADMIN_DASHBOARD_CONFIGURE_ANALYTICS_PAGE_URL_VERIFICATION)) {
            throw new IllegalStateException(driver.getCurrentUrl() + ":    This is not the Publisher home page");
        }
        log.info("Page load : Admin Dashboard Analytics Page");
    }

    /**
     * Add configurations. To run this the BAM server should be running
     */
    public String addConfigurations(String eventReceiverURL, String eventReceiverUsername, String eventReceiverPassword,
                                  String analyzerURL, String analyzerUsername, String analyzerPassword, String statDsURL,
                                  String statDsClassName, String statDsUsername, String statDsPassword)
            throws IOException {
        //Enable stats
        clickElementById("admin.dashboard.enable.stats.checkbox");
        //Clear existing values in text boxes
        clearTextBoxById("admin.dashboard.event.receiver.url");
        clearTextBoxById("admin.dashboard.event.receiver.username");
        clearTextBoxById("admin.dashboard.event.receiver.password");
        clearTextBoxById("admin.dashboard.data.analyzer.url");
        clearTextBoxById("admin.dashboard.data.analyzer.username");
        clearTextBoxById("admin.dashboard.data.analyzer.password");
        clearTextBoxById("admin.dashboard.stat.ds.url");
        clearTextBoxById("admin.dashboard.stat.ds.class");
        clearTextBoxById("admin.dashboard.stat.ds.username");
        clearTextBoxById("admin.dashboard.stat.ds.password");

        //Add config phase
        fillTextBoxById("admin.dashboard.event.receiver.url", eventReceiverURL);
        fillTextBoxById("admin.dashboard.event.receiver.username", eventReceiverUsername);
        fillTextBoxById("admin.dashboard.event.receiver.password", eventReceiverPassword);
        clickElementById("admin.dashboard.add.event.receiver.config.button");
        fillTextBoxById("admin.dashboard.data.analyzer.url", analyzerURL);
        fillTextBoxById("admin.dashboard.data.analyzer.username", analyzerUsername);
        fillTextBoxById("admin.dashboard.data.analyzer.password", analyzerPassword);
        fillTextBoxById("admin.dashboard.stat.ds.url", statDsURL);
        fillTextBoxById("admin.dashboard.stat.ds.class", statDsClassName);
        fillTextBoxById("admin.dashboard.stat.ds.username", statDsUsername);
        fillTextBoxById("admin.dashboard.stat.ds.password", statDsPassword);
       // clickElementByLinkText("admin.dashboard.stat.more.options");

        clickElementByXpath("admin.dashboard.save.config.button.xpath");
        log.info("Configuration Saved : Finish ");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error("Error while saving analytics configurations");
        }

        return getTextOfElementByClassName("admin.dashboard.save.config.message");
    }

    /**
     * Logout from publisher
     */
    public void logOut() throws IOException {
        clickElementById("admin.dashboard.usermenu.id");
        clickElementByCssSelector("admin.dashboard.logout.button.css");
    }
}
