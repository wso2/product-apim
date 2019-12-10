package org.wso2.am.integration.tests.header;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;

public class CORSAccessControlAllowCredentialsHeaderSetup extends APIManagerLifecycleBaseTest {

    private ServerConfigurationManager serverConfigurationManager;

    @BeforeTest(alwaysRun = true)
    public void setEnvironment() throws Exception {
        superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                TestUserMode.SUPER_TENANT_ADMIN);
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);

        serverConfigurationManager.applyConfiguration(new File(
                getAMResourceLocation() + File.separator + "configFiles" + File.separator + "corsACACTest"
                        + File.separator + "deployment.toml"));

    }

    @AfterTest(alwaysRun = true)
    public void restore() throws Exception {
        superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                TestUserMode.SUPER_TENANT_ADMIN);
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(
                getAMResourceLocation() + File.separator + "configFiles" + File.separator + "corsACACTest"
                        + File.separator + "original" + File.separator + "deployment.toml"));
    }
}
