package org.wso2.am.integration.tests.publisher;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.apimgt.api.model.policy.SubscriptionPolicy;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.util.List;

public class APIMGetAllSubscriptionThrottlingPolicies extends APIMIntegrationBaseTest {
    @Factory(dataProvider = "userModeDataProvider")
    public APIMGetAllSubscriptionThrottlingPolicies
            (TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
//                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
    }

    @Test(groups = {"wso2.am"}, description = "Get all the subscription throttling tiers from the publisher " +
            "rest API ")
    public void testGetAllSubscriptionThrottlingTiers() throws Exception {
        List<SubscriptionPolicy> subscriptionPolicyList = restAPIPublisher.getSubscriptionPolicies("");
    }
}
