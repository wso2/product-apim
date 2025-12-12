package org.wso2.am.integration.cucumbertests.runners.migration;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.*;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.Tenant;
import org.wso2.carbon.automation.engine.context.beans.User;

@CucumberOptions(
        features = "src/test/resources/features/migration/migrated_api_revisioning.feature",
        glue = "org.wso2.am.integration.cucumbertests.stepdefinitions",
        plugin = {"pretty", "html:target/cucumber-report/createAPI.html"}
)

@Test(groups = {"migrationTest"})
public class MigratedAPIRevisionRunner extends AbstractTestNGCucumberTests {
    private String testUserDomain;
    private String testUserKey;

    private void setTestUserDomain(String testUserDomain) {
        this.testUserDomain = testUserDomain;
    }

    private void setTestUserKey(String testUserKey) {
        this.testUserKey = testUserKey;
    }

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        Tenant tenant = Utils.getTenantFromContext(testUserDomain);
        User user  = testUserKey.equals(Constants.ADMIN_USER_KEY)
                ? tenant.getTenantAdmin()
                : tenant.getTenantUser(testUserKey);
        tenant.setContextUser(user);
        TestContext.set("currentTenant", tenant);
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() {
        TestContext.remove("currentTenant");
    }

    @Factory(dataProvider = "userModeDataProvider")
    public static Object[] factory(String tenantDomain, String userKey) {
        MigratedAPIRevisionRunner runner = new MigratedAPIRevisionRunner();
        runner.setTestUserDomain(tenantDomain);
        runner.setTestUserKey(userKey);
        return new Object[]{ runner };
    }

    @DataProvider
    public Object[][] userModeDataProvider() {
        return new Object[][]{
                {"carbon.super", "admin"},
                {"adpsample.com", "admin"},
        };
    }

//    @Override
//    @DataProvider(parallel = true)
//    public Object[][] scenarios() {
//        return super.scenarios();
//    }
}
