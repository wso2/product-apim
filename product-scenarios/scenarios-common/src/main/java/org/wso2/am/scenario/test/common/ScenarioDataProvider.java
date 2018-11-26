package org.wso2.am.scenario.test.common;

import org.testng.annotations.DataProvider;

public class ScenarioDataProvider {

    @DataProvider(name = "apiNames")
    public static Object[][] ApiDataProvide() {
        return new Object[][]{

                {"PhoneVerification"}, {"123567890"}, {"电话验证"},{"eñe"}, {"Pho_ne-verify?api."}, {"PhoneVerification123"}

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
}
