package org.wso2.am.scenario.test.common;

import org.testng.annotations.DataProvider;

public class ScenarioDataProvider {

    @DataProvider(name = "apiNames")
    public static Object[][] ApiDataProvide() {
        return new Object[][]{

                {"PhoneVerification"}, {"123567890"}, {"电话验证"},{"eñe"}, {"Pho_ne-verify?api."}, {"PhoneVerification123"}

        };
    }
}
