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
                {" App 1", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "New App Description",
                        "Application name cannot contain leading or trailing white spaces"},
                {"App 2 ", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "New App Description",
                        "Application name cannot contain leading or trailing white spaces"},
//                todo fix the error message when the fix to remove x["application"] is working
                {"App !@#$%^", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "",
                        "Invalid inputs [\"application\"]"},
                {" ", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "Application Name is empty."},
                {"App 3", "", "New App Description", "Specified application tier does not exist."},
                {"App 4", "TierAbc", "New App Description", "Specified application tier does not exist."},
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

    @DataProvider(name = "MissingMandatoryApplicationValuesDataProvider")
    public static Object[][] missingMandatoryApplicationValuesDataProvider() {
        String urlPrefix = "{{backendURL}}store/site/blocks/application/application-add/ajax/application-add.jag?" +
                "action=addApplication";
        return new Object[][]{
                {"", urlPrefix + "&tier=" + APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED +
                        "&callbackUrl=&description=description", "Missing parameters."},
                {"application-missingMandatory1", urlPrefix + "&tier="
                        + APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED +
                        "&callbackUrl=&application=application-missingMandatory1", "Missing parameters."},
                {"application-missingMandatory2", urlPrefix + "&callbackUrl=&description=description" +
                        "&application=application-missingMandatory2", "Specified application tier does not exist."}
        };
    }
}
