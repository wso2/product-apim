package org.wso2.am.scenario.test.common;

import org.testng.annotations.DataProvider;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;

public class ScenarioDataProvider {

    // TODO: Add {"电话验证"} to apiNames data set once fix done.

    @DataProvider(name = "apiNames")
    public static Object[][] ApiDataProvide() {
        return new Object[][]{
                {"PhoneVerification"}, {"123567890"}, {"eñe"}, {"Pho_ne-verify?api."}, {"PhoneVerification123567890"}
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
                {"http://localhost:8083/swaggerFiles/OAS2Document.json"}, {"http://localhost:8083/swaggerFiles/OAS3Document.json"}};
    }

    @DataProvider(name = "OASDocsWithYamlURL")
    public static Object[][] OASDocYamlURLProvide() {
        return new Object[][]{
                {"http://localhost:8083/swaggerFiles/OAS2Document.yaml"}, {"http://localhost:8083/swaggerFiles/OAS3Document.yaml"}};
    }

    @DataProvider(name = "InvalidMandatoryApplicationValuesDataProvider")
    public static Object[][] invalidMandatoryApplicationValuesDataProvider() {
        return new Object[][]{
                {" App 1 ", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                        "Application name cannot contain leading or trailing white spaces"},
                {"App !@#$%^", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "Invalid inputs"},
                {" ", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "Application Name is empty."},
                {"App 3", "", "Specified application tier does not exist."},
                {"App 4", "TierAbc", "Specified application tier does not exist."},
        };
    }

    @DataProvider(name = "OptionalApplicationValuesDataProvider")
    public static Object[][] optionalApplicationValuesDataProvider() {
        return new Object[][]{
                {"JWT"},
                {"DEFAULT"},
                {"OAuth"}
        };
    }

    @DataProvider(name = "ApiStateAndValidRoleDataProvider")
    public static Object[][] apiStateAndValidRoleDataProvider() {
        return new Object[][]{
                { "internal/publisher", APILifeCycleState.CREATED.toString()},
                { "internal/publisher", APILifeCycleState.PROTOTYPED.toString()},
                { "internal/publisher", APILifeCycleState.BLOCKED.toString()},
                { "admin", APILifeCycleState.CREATED.toString()},
                { "admin", APILifeCycleState.PROTOTYPED.toString()},
                { "admin", APILifeCycleState.BLOCKED.toString()}
        };
    }

    @DataProvider(name = "ApiStateAndInvalidRoleDataProvider")
    public static Object[][] apiStateAndInvalidRoleDataProvider() {
        return new Object[][]{
                { "internal/creator", APILifeCycleState.CREATED.toString()},
                { "internal/creator", APILifeCycleState.PROTOTYPED.toString()},
                { "internal/creator", APILifeCycleState.BLOCKED.toString()},
                { "internal/subscriber", APILifeCycleState.CREATED.toString()},
                { "internal/subscriber", APILifeCycleState.PROTOTYPED.toString()},
                { "internal/subscriber", APILifeCycleState.BLOCKED.toString()}
        };
    }


    @DataProvider(name = "ApiInvalidPermissionDataProvider")
    public static Object[][] apiInvalidPermissionDataProvider() {

        String loginPermission = "/permission/admin/login";
        String apiSubscriberPermission = "/permission/admin/manage/api/subscribe";
        String apiCreatorPermission = "/permission/admin/manage/api/create";

        String[] creatorPermissionList = new String[] { loginPermission, apiCreatorPermission };
        String[] subscribePermissionList = new String[] { loginPermission, apiSubscriberPermission };

        return new Object[][]{
                { creatorPermissionList },
                { subscribePermissionList }
        };
    }

    @DataProvider(name = "ValidRoleDataProvider")
    public static Object[][] validRoleDataProvider() {
        return new Object[][]{
                { "internal/publisher"},
                { "admin"}
        };
    }

    @DataProvider(name = "ValidPermissionDataProvider")
    public static Object[][] validPermissionDataProvider() {

        String loginPermission = "/permission/admin/login";
        String apiAdminPermission = "/permission/admin";
        String apiPublisherPermission = "/permission/admin/manage/api/publish";

        String[] publisherPermissionList = new String[] { loginPermission, apiPublisherPermission };
        String[] apiAdminPermissionList = new String[] { apiAdminPermission };

        return new Object[][]{
                { publisherPermissionList },
                { apiAdminPermissionList }
        };
    }

    @DataProvider(name = "MissingMandatoryApplicationValuesDataProvider")
    public static Object[][] missingMandatoryApplicationValuesDataProvider() {
        String urlPrefix = "{{backendURL}}/site/blocks/application/application-add/ajax/application-add.jag?" +
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

    @DataProvider(name = "DeleteAPIInLifeCycleStateDataProvider")
    public static Object[][] deleteAPIInLifeCycleStateDataProvider() {
        return new Object[][]{
                {APILifeCycleState.CREATED},
                {APILifeCycleState.PUBLISHED},
                {APILifeCycleState.PROTOTYPED},
                {APILifeCycleState.BLOCKED},
                {APILifeCycleState.DEPRECATED},
                {APILifeCycleState.RETIRED},
        };
    }

    @DataProvider(name = "DeleteAPIAfterSubscribingDataProvider")
    public static Object[][] deleteAPIAfterSubscribingDataProvider() {
        return new Object[][]{
                {APILifeCycleState.PUBLISHED},
                {APILifeCycleState.BLOCKED},
                {APILifeCycleState.DEPRECATED},
        };
    }

    @DataProvider(name = "UserTypeDataProvider")
    public static Object[][] userTypeDataProvider() {
        return new Object[][]{
                { "normalUser" , "admin"},
                { "normalUser" , "nonAdmin"},
                { "tenantUser" , "admin"},
                { "tenantUser" , "nonAdmin"}
        };
    }

    @DataProvider(name = "RoleUpdatingDataProvider")
    public static Object[][] roleUpdatingDataProvider() {
        return new Object[][]{
                {"admin", ScenarioTestConstants.CREATOR_ROLE},
                {ScenarioTestConstants.PUBLISHER_ROLE, ScenarioTestConstants.CREATOR_ROLE},
        };
    }

    @DataProvider(name = "PermissionUpdatingDataProvider")
    public static Object[][] permissionUpdatingDataProvider() {

        String loginPermission = "/permission/admin/login";
        String creatorPermission = "/permission/admin/manage/api/create";
        String adminPermission = "/permission/admin";
        String publisherPermission = "/permission/admin/manage/api/publish";

        String[] creatorPermissionList = new String[] {loginPermission, creatorPermission};
        String[] publisherPermissionList = new String[]{loginPermission, publisherPermission};
        String[] adminPermissionList = new String[]{adminPermission};

        return new Object[][]{
                {adminPermissionList, creatorPermissionList},
                {publisherPermissionList, creatorPermissionList}
        };
    }

    @DataProvider(name = "thumbUrlProvider")
    public static Object[][] thumbUrlDataProvider() {

        return new Object[][]{
                {"http://localhost:8083/thumbnail/petstoreapi.jpg"}
        };
    }

    @DataProvider(name = "AuthorizationHeadersDataProvider")
    public static Object[][] authorizationHeadersDataProvider() {
            return new Object[][]{
                    {"Bearer "},
                    {"Basic WXS4DASADDDSDS5; Bearer "}
            };
    }

    @DataProvider(name = "IncorrectFormattedAuthorizationHeadersDataProvider")
    public static Object[][] incorrectFormattedAuthorizationHeadersDataProvider() {
        return new Object[][]{
                {"Bearer" , "tokenVal"},
                {"Bearer" , ""},
                {"Basic su18eaodd" , ""},
                {"Basic su18eaodd; Bearer", "tokenVal"},
                {"",""},
                {"Bearer " , "tokenDuplicated"}
        };
    }

    @DataProvider(name = "OASDocsWithInvalidURL")
    public static Object[][] OASDocInvalidURLProvide() {

        return new Object[][]{
                {"localhost:8083/swaggerFiles/OAS2Document.yaml"}, {"http:localhost:8083/swaggerFiles/OAS3Document.yaml"}
                , {"http:localhost:8083/swaggerFiles/xxx.jpg"}};
    }


    @DataProvider(name = "APITags")
    public static Object[][] APITagsDataProvider() {
        return new Object[][]{
                {""},
                {"newTag1,newTag2"}
        };
    }
}

