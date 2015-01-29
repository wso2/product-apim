/*
*Copyright (c) 2015​, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.wso2.am.integration.test.extensions.user.mgt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.NodeList;
import org.wso2.am.integration.admin.clients.AuthenticatorClient;
import org.wso2.am.integration.admin.clients.TenantManagementServiceClient;
import org.wso2.am.integration.admin.clients.UserManagementClient;
import org.wso2.am.integration.test.utils.AutomationXpathConstants;
import org.wso2.am.integration.test.utils.user.mgt.LoginLogoutClient;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.engine.FrameworkConstants;
import org.wso2.carbon.automation.engine.configurations.AutomationConfiguration;
import org.wso2.carbon.automation.engine.configurations.UrlGenerationUtil;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import javax.xml.xpath.XPathExpressionException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for adding tenants and users
 * defined under userManagement entry in automation.xml to servers.
 */
public class UserPopulator {

    private static final Log log = LogFactory.getLog(UserPopulator.class);

    private AutomationContext automationContext;
    private List<String> tenantList;
    private List<String> rolesList;
    private List<RemovableData> removableDataList = new ArrayList<RemovableData>();

    public UserPopulator(String productGroupName, String instanceName)
            throws XPathExpressionException {
        this.automationContext = new AutomationContext(productGroupName, instanceName,
                                                       TestUserMode.SUPER_TENANT_ADMIN);
        this.tenantList = getTenantList();
        this.rolesList = getRolesList();
    }

    /**
     * Populate Tenants, Users and Roles
     *
     * @throws Exception
     */
    public void populateUsers() throws Exception {

        // login as carbon super to add tenants
        LoginLogoutClient loginLogoutUtil = new LoginLogoutClient(automationContext);
        String sessionCookie = loginLogoutUtil.login();
        String backendURL = automationContext.getContextUrls().getBackEndUrl();
        TenantManagementServiceClient tenantManagementServiceClient =
                new TenantManagementServiceClient(backendURL, sessionCookie);

        for (String tenant : tenantList) {
            RemovableData removableData = new RemovableData();
            removableData.setTenant(tenant);

            // add tenant, if the tenant is not the Super tenant
            String tenantType = AutomationXpathConstants.SUPER_TENANT;
            if (!tenant.equals(FrameworkConstants.SUPER_TENANT_DOMAIN_NAME)) {
                tenantType = AutomationXpathConstants.TENANTS;
                String tenantAdminUserName = getTenantAdminUsername(tenantType, tenant);
                char[] tenantAdminPassword = getTenantAdminPassword(tenantType, tenant);

//				if (!tenantManagementServiceClient.getTenant(tenant).getActive()) {
                tenantManagementServiceClient
                        .addTenant(tenant, String.valueOf(tenantAdminPassword), tenantAdminUserName,
                                   FrameworkConstants.TENANT_USAGE_PLAN_DEMO);
                log.info("Added new tenant : " + tenant);

                // if new tenant added -> need to remove from the system at the end of the test
                removableData.setNewTenant(true);
//				}

                // login as newly added tenant
                sessionCookie = login(tenantAdminUserName, tenant, tenantAdminPassword, backendURL,
                                      UrlGenerationUtil.getManagerHost(
                                              automationContext.getInstance()));

            }

            removableData.setTenantType(tenantType);
            UserManagementClient userManagementClient =
                    new UserManagementClient(backendURL, sessionCookie);

            // add roles to the tenant
            addRoles(userManagementClient, removableData);

            // populate users of the current tenant and add roles
            addTenantUsers(tenantType, tenant, userManagementClient, removableData);

            // collect RemovableData
            removableDataList.add(removableData);
        }
    }

    private void addRoles(UserManagementClient userManagementClient, RemovableData removableData)
            throws Exception {

        for (String role : rolesList) {
            if (!userManagementClient.roleNameExists(role)) {
                List<String> permissions = getPermissionList(role);
                userManagementClient
                        .addRole(role, null, permissions.toArray(new String[permissions.size()]));
                log.info("Added role " + role + " with permissions");

                // if new role added for existing tenant -> need to remove from the system at the
                // end of the test
                if (!removableData.isNewTenant()) {
                    removableData.setNewRole(role);
                }
            }
        }
    }

    private void addTenantUsers(String tenantType, String tenant,
                                UserManagementClient userManagementClient,
                                RemovableData removableData) throws Exception {

        List<String> userList = getUserList(tenant);
        for (String tenantUser : userList) {
            String tenantUserUsername = getTenantUserUsername(tenantType, tenant, tenantUser);
            boolean isTenantUserExist = userManagementClient.getUserList().contains(
                    tenantUserUsername);

            if (!isTenantUserExist) {
                String[] rolesToBeAdded = new String[]{FrameworkConstants.ADMIN_ROLE};
                List<String> userRoles = new ArrayList<String>();
                NodeList roleList = automationContext.getConfigurationNodeList(
                        String.format(AutomationXpathConstants.TENANT_USER_ROLES, tenantType,
                                      tenant, tenantUser));

                if (roleList != null && roleList.item(0) != null) {
                    roleList = roleList.item(0).getChildNodes();
                    for (int i = 0; i < roleList.getLength(); i++) {
                        String role = roleList.item(i).getTextContent();
                        if (userManagementClient.roleNameExists(role)) {
                            userRoles.add(role);
                        } else {
                            log.warn("Role is not exist : " + role);
                        }
                    }
                    if (userRoles.size() > 0) {
                        rolesToBeAdded = userRoles.toArray(new String[userRoles.size()]);
                    }
                }

                userManagementClient
                        .addUser(tenantUserUsername, String.valueOf(getTenantUserPassword(tenantType, tenant, tenantUser)),
                                 rolesToBeAdded, null);
                log.info("User - " + tenantUser + " created in tenant domain of " + " " + tenant);

                // if new user added for existing tenant -> need to remove from the system at the
                // end of the test
                if (!removableData.isNewTenant()) {
                    removableData.setNewUser(tenantUserUsername);
                }

            } else {
                log.info(tenantUser + " is already in " + tenant);
            }

        }
    }

    /**
     * Delete Tenants, Users and Roles
     *
     * @throws Exception
     */
    public void deleteUsers() throws Exception {
        String backendURL = automationContext.getContextUrls().getBackEndUrl();
        for (RemovableData removableData : removableDataList) {
            if (removableData.isNewTenant()) {
                LoginLogoutClient loginLogoutUtil = new LoginLogoutClient(automationContext);
                String sessionCookie = loginLogoutUtil.login();

                // remove tenant
                TenantManagementServiceClient tenantManagementServiceClient =
                        new TenantManagementServiceClient(backendURL, sessionCookie);
                tenantManagementServiceClient.deleteTenant(removableData.getTenant());

                log.info("Tenant was deleted successfully - " + removableData.getTenant());
            } else {
                String sessionCookie = login(
                        getTenantAdminUsername(removableData.getTenantType(),
                                               removableData.getTenant()),
                        removableData.getTenant(),
                        getTenantAdminPassword(removableData.getTenantType(),
                                               removableData.getTenant()),
                        backendURL,
                        UrlGenerationUtil.getManagerHost(automationContext.getInstance()));

                UserManagementClient userManagementClient =
                        new UserManagementClient(backendURL, sessionCookie);

                for (String user : removableData.getNewUsers()) {
                    // remove users
                    boolean isTenantUserExist = userManagementClient.getUserList().contains(user);
                    if (isTenantUserExist) {
                        userManagementClient.deleteUser(user);
                        log.info("User was deleted successfully - " + user);
                    }
                }

                for (String role : removableData.getNewRoles()) {
                    // remove roles
                    if (userManagementClient.roleNameExists(role)) {
                        userManagementClient.deleteRole(role);
                        log.info("Role was deleted successfully - " + role);
                    }
                }
            }
        }
    }

    private String getTenantAdminUsername(String tenantType, String tenant)
            throws XPathExpressionException {
        return automationContext
                .getConfigurationValue(
                        String.format(AutomationXpathConstants.ADMIN_USER_USERNAME, tenantType,
                                      tenant));
    }

    private char[] getTenantAdminPassword(String tenantType, String tenant)
            throws XPathExpressionException {
        return automationContext.getConfigurationValue(
                String.format(AutomationXpathConstants.ADMIN_USER_PASSWORD, tenantType, tenant)).toCharArray();
    }

    private String getTenantUserUsername(String tenantType, String tenant, String tenantUser)
            throws XPathExpressionException {
        return automationContext.getConfigurationValue(
                String.format(AutomationXpathConstants.TENANT_USER_USERNAME, tenantType, tenant,
                              tenantUser));
    }

    private char[] getTenantUserPassword(String tenantType, String tenant, String tenantUser)
            throws XPathExpressionException {
        return automationContext.getConfigurationValue(
                String.format(AutomationXpathConstants.TENANT_USER_PASSWORD, tenantType, tenant,
                              tenantUser)).toCharArray();
    }

    private String login(String userName, String domain, char[] password, String backendUrl,
                         String hostName) throws
                                          RemoteException,
                                          LoginAuthenticationExceptionException,
                                          XPathExpressionException {
        AuthenticatorClient loginClient = new AuthenticatorClient(backendUrl);
        if (!domain.equals(AutomationConfiguration
                                   .getConfigurationValue(
                                           ExtensionCommonConstants.SUPER_TENANT_DOMAIN_NAME))) {
            userName += "@" + domain;
        }
        return loginClient.login(userName, String.valueOf(password), hostName);
    }

    private List<String> getTenantList() throws XPathExpressionException {
        List<String> tenantList = new ArrayList<String>();
        // add carbon.super
        tenantList.add(FrameworkConstants.SUPER_TENANT_DOMAIN_NAME);

        // add other tenants
        NodeList tenantNodeList =
                automationContext.getConfigurationNodeList(AutomationXpathConstants.TENANTS_NODE)
                        .item(0)
                        .getChildNodes();
        for (int i = 0; i < tenantNodeList.getLength(); i++) {
            tenantList.add(
                    tenantNodeList.item(i).getAttributes()
                            .getNamedItem(AutomationXpathConstants.DOMAIN).getNodeValue()
            );
        }
        return tenantList;
    }

    private List<String> getUserList(String tenantDomain) throws XPathExpressionException {
        List<String> userList = new ArrayList<String>();

        // set tenant type
        String tenantType = AutomationXpathConstants.TENANTS;
        if (tenantDomain.equals(FrameworkConstants.SUPER_TENANT_DOMAIN_NAME)) {
            tenantType = AutomationXpathConstants.SUPER_TENANT;
        }

        NodeList userNodeList = automationContext
                .getConfigurationNodeList(
                        String.format(AutomationXpathConstants.USER_NODE, tenantType,
                                      tenantDomain));

        for (int i = 0; i < userNodeList.getLength(); i++) {
            userList.add(userNodeList.item(i).getAttributes().getNamedItem("key").getNodeValue());
        }
        return userList;
    }

    private List<String> getRolesList() throws XPathExpressionException {
        List<String> roleList = new ArrayList<String>();

        NodeList roleNodeList =
                automationContext.getConfigurationNodeList(AutomationXpathConstants.ROLES_NODE);
        if (roleNodeList != null && roleNodeList.item(0) != null) {
            roleNodeList = roleNodeList.item(0).getChildNodes();
            for (int i = 0; i < roleNodeList.getLength(); i++) {
                roleList.add(roleNodeList.item(i).getAttributes()
                                     .getNamedItem(AutomationXpathConstants.NAME)
                                     .getNodeValue());
            }
        }
        return roleList;
    }

    private List<String> getPermissionList(String role) throws XPathExpressionException {
        List<String> permissionList = new ArrayList<String>();

        NodeList permissionNodeList = automationContext
                .getConfigurationNodeList(
                        String.format(AutomationXpathConstants.PERMISSIONS_NODE, role));
        if (permissionNodeList != null && permissionNodeList.item(0) != null) {
            permissionNodeList = permissionNodeList.item(0).getChildNodes();
            for (int i = 0; i < permissionNodeList.getLength(); i++) {
                permissionList.add(permissionNodeList.item(i).getTextContent());
            }
        }

        return permissionList;
    }

    /**
     * Class to store data to be removed at the end of the test execution
     */
    private class RemovableData {

        private String tenant;
        private String tenantType;
        private boolean isNewTenant = false;

        private List<String> newRoles = new ArrayList<String>();
        private List<String> newUsers = new ArrayList<String>();

        public String getTenant() {
            return tenant;
        }

        public void setTenant(String tenant) {
            this.tenant = tenant;
        }

        public String getTenantType() {
            return tenantType;
        }

        public void setTenantType(String tenantType) {
            this.tenantType = tenantType;
        }

        public boolean isNewTenant() {
            return isNewTenant;
        }

        public void setNewTenant(boolean isNewTenant) {
            this.isNewTenant = isNewTenant;
        }

        public List<String> getNewRoles() {
            return newRoles;
        }

        public void setNewRole(String role) {
            this.newRoles.add(role);
        }

        public List<String> getNewUsers() {
            return newUsers;
        }

        public void setNewUser(String user) {
            this.newUsers.add(user);
        }

    }

}
