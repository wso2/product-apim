package org.wso2.am.integration.tests.login;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.api.dto.RoleAliasDTO;
import org.wso2.am.integration.clients.admin.api.dto.RoleAliasListDTO;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.util.ArrayList;
import java.util.Collections;

public class TenantUserLoginTestCase extends APIManagerLifecycleBaseTest {

    private final String TENANT_DOMAIN = "tenant.com";
    private final String TENANT_ADMIN_USERNAME = "tenantAdmin";
    private final String TENANT_ADMIN_PASSWORD = "tenantAdminPassword";
    private final String TENANT_USER_USERNAME = "tenantUser";
    private final String TENANT_USER_PASSWORD = "tenantUserPassword";
    private final String TENANT_ADMIN_USER = TENANT_ADMIN_USERNAME + "@" + TENANT_DOMAIN;
    private final String TENANT_PUBLISHER_ROLE = "tenantPublisherRole";
    private UserManagementClient tenantUserManagementClient = null;
    private RestAPIAdminImpl restAPIAdminClient;
    private RestAPIPublisherImpl restAPIPublisherClient;
    private final String[] userRoles = { TENANT_PUBLISHER_ROLE };

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        tenantUserManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                TENANT_ADMIN_USER, TENANT_ADMIN_PASSWORD);
        // Create the tenant
        tenantManagementServiceClient.addTenant(TENANT_DOMAIN, TENANT_ADMIN_PASSWORD, TENANT_ADMIN_USERNAME, "demo");
        restAPIAdminClient = new RestAPIAdminImpl(TENANT_ADMIN_USERNAME, TENANT_ADMIN_PASSWORD, TENANT_DOMAIN,
                publisherURLHttps);
    }

    @Test(groups = {"wso2.am"}, description = "Login with tenant user with custom role with role mapping")
    public void testApiInvocationForTenantUserWithScopeAssignedRole() throws Exception {
        // Adding new role
        tenantUserManagementClient.addRole(TENANT_PUBLISHER_ROLE, new String[]{}, new String[]{});
        tenantUserManagementClient.addUser(TENANT_USER_USERNAME, TENANT_USER_PASSWORD, userRoles, TENANT_USER_USERNAME);
        // Adding role alias from the admin portal
        RoleAliasDTO roleAliasDTO = new RoleAliasDTO();
        roleAliasDTO.setRole(APIMIntegrationConstants.APIM_INTERNAL_ROLE.PUBLISHER);
        roleAliasDTO.setAliases(new ArrayList<>(Collections.singletonList(TENANT_PUBLISHER_ROLE)));
        RoleAliasListDTO roleAliasListDTO = new RoleAliasListDTO();
        roleAliasListDTO.setCount(1);
        roleAliasListDTO.setList(new ArrayList<>(Collections.singletonList(roleAliasDTO)));
        restAPIAdminClient.putRoleAliases(roleAliasListDTO);
        // Initiate API rest clients with tenant user
        restAPIPublisherClient = new RestAPIPublisherImpl(TENANT_USER_USERNAME, TENANT_USER_PASSWORD, TENANT_DOMAIN,
                publisherURLHttps);
        // Test API invocation with the initiated rest clients
        restAPIPublisherClient.getAllAPIs();
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (tenantUserManagementClient != null) {
            tenantUserManagementClient.deleteUser(TENANT_USER_USERNAME);
            tenantUserManagementClient.deleteRole(TENANT_PUBLISHER_ROLE);
        }
        tenantManagementServiceClient.deleteTenant(TENANT_DOMAIN);
        super.cleanUp();
    }
}
