/*
 *
 *   Copyright (c) 2025, WSO2 LLc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 LLc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.tests.organization;

import java.rmi.RemoteException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.um.ws.api.stub.ClaimValue;
import org.wso2.carbon.um.ws.api.stub.RemoteUserStoreManagerServiceUserStoreExceptionException;
import org.wso2.carbon.user.core.UserStoreException;

public class ConsumerOrganizationVisibilityTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(ConsumerOrganizationVisibilityTestCase.class);
    private final String DEFAULT_PROFILE = "default";
    String enduserPassword = "password@123";
    String users[] = {"orgadmin", "orgpublisher", "org"};
    
    String orgId = "123-456-789";
    String orrgName = "Super";
    String subOrg1Id = "org1";
    String subOrg1Name = "123-456-001";
    String subOrg2Id = "org2";
    String subOrg2Name = "123-456-002";
    
    String orgAdmin = "orgadmin";
    String orgPublisher = "orgpublisher";
    String orgDevUser = "orgdevuser";
    String subOrg1DevUser = "suborg1devuser";
    String subOrg2DevUser = "suborg2devuser";
    
    @Factory(dataProvider = "userModeDataProvider")
    public ConsumerOrganizationVisibilityTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN } };
    }
    
    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        remoteClaimMetaDataMgtAdminClient.addOrganizationLocalClaim();
        // Parent Org
        addUser(orgAdmin, orgId, orrgName);
        addUser(orgPublisher, orgId, orrgName);
        addUser(orgDevUser, orgId, orrgName);
        // Suborg 1 user
        addUser(subOrg1DevUser, subOrg1Id, subOrg1Name);
        // Suborg 2 user
        addUser(subOrg2DevUser, subOrg2Id, subOrg2Name);
    }
    
    @Test(groups = {"wso2.am"}, description = "Add organization")
    public void testAddOrganization() throws Exception {
        
    }
    
    private void addUser(String username, String organizationId, String organization)
            throws UserStoreException, RemoteException, RemoteUserStoreManagerServiceUserStoreExceptionException {

        remoteUserStoreManagerServiceClient.addUser(username, enduserPassword, new String[] {}, new ClaimValue[] {},
                DEFAULT_PROFILE, false);
        remoteUserStoreManagerServiceClient.setUserClaimValue(username, "http://wso2.org/claims/givenname",
                "first name".concat(username), DEFAULT_PROFILE);
        remoteUserStoreManagerServiceClient.setUserClaimValue(username, "http://wso2.org/claims/lastname",
                "last name".concat(username), DEFAULT_PROFILE);
        remoteUserStoreManagerServiceClient.setUserClaimValue(username, "http://wso2.org/claims/organization",
                organization, DEFAULT_PROFILE);
        remoteUserStoreManagerServiceClient.setUserClaimValue(username, "http://wso2.org/claims/organizationId",
                organizationId, DEFAULT_PROFILE);

    }
}
