package org.wso2.am.scenario.test.common;

import org.testng.annotations.DataProvider;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;

public class ScenarioDataProvider {

    // TODO: Add {"电话验证"} to apiNames data set once fix done.

    @DataProvider(name = "apiNames")
    public static Object[][] ApiDataProvide() {
        return new Object[][]{
                {"PhoneVerification"}, {"123567890"}, {"eñe"}, {"Pho_ne-verify?api."}
        };
    }

    @DataProvider(name = "OASDocsWithJSONFiles")
    public static Object[][] OASDocJsonFilesProvide() {
        return new Object[][]{
                {"swaggerFiles/OAS2Document.json"}, {"swaggerFiles/OAS3Document.json"}
        };
    }

    @DataProvider(name = "OASDocsWithYAMLFiles")
    public static Object[][] OASDocYamlFilesProvide() {
        return new Object[][]{
                {"swaggerFiles/OAS2Document.yaml"}, {"swaggerFiles/OAS3Document.yaml"}
        };
    }

    @DataProvider(name = "OASDocsWithJsonURL")
    public static Object[][] OASDocJsonURLProvide() {
        return new Object[][]{
                {"https://raw.githubusercontent.com/wso2/product-apim/product-scenarios/product-scenarios/1-manage-public-partner-private-apis/1.1-expose-service-as-rest-api/1.1.2-create-rest-api-by-import-oas-document/src/test/resources/swaggerFiles/OAS2Document.json"},
                {"https://raw.githubusercontent.com/wso2/product-apim/product-scenarios/product-scenarios/1-manage-public-partner-private-apis/1.1-expose-service-as-rest-api/1.1.2-create-rest-api-by-import-oas-document/src/test/resources/swaggerFiles/OAS3Document.json"}
        };
    }

    @DataProvider(name = "OASDocsWithYamlURL")
    public static Object[][] OASDocYamlURLProvide() {
        return new Object[][]{
                {"https://raw.githubusercontent.com/wso2/product-apim/product-scenarios/product-scenarios/1-manage-public-partner-private-apis/1.1-expose-service-as-rest-api/1.1.2-create-rest-api-by-import-oas-document/src/test/resources/swaggerFiles/OAS2Document.yaml"},
                {"https://raw.githubusercontent.com/wso2/product-apim/product-scenarios/product-scenarios/1-manage-public-partner-private-apis/1.1-expose-service-as-rest-api/1.1.2-create-rest-api-by-import-oas-document/src/test/resources/swaggerFiles/OAS3Document.yaml"}
        };
    }

    @DataProvider(name = "ValidMandatoryApplicationValuesDataProvider")
    public static Object[][] validMandatoryApplicationValuesDataProvider() {
        return new Object[][]{
                {"Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "New App Description "},
                {"Application_-.", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, " New app description 123"},
                {"1234", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, " "},
                {"Mix App-1234_.", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "New app description 123 !@#$%"}
        };
    }

    @DataProvider(name = "InvalidMandatoryApplicationValuesDataProvider")
    public static Object[][] invalidMandatoryApplicationValuesDataProvider() {
        return new Object[][]{
                {" App 1", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "New App Description"},
                {"App 2 ", "", "New App Description"},
                {"App !@#$%^", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ""},
                {" ", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ""},
                {"App", "", ""},
                {"", "TierAbc", ""},
                {"", "", ""}
        };
    }

    @DataProvider(name = "MandatoryAndOptionalApplicationValuesDataProvider")
    public static Object[][] mandatoryAndOptionalApplicationValuesDataProvider() {
        return new Object[][]{
                {"App - Token 1", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                        "New App Description", "JWT"},
                {"App - Token 2", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                        "New App Description", "DEFAULT"},
                {"App - Token 3", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                        "", "OAuth"}
        };
    }
}
