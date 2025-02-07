/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.apimgt.samples.utils;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.apimgt.samples.utils.stubs.AuthenticateStub;
import org.wso2.carbon.user.mgt.stub.UserAdminStub;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;

import java.rmi.RemoteException;

public class UserManagementUtils {

    private static UserAdminStub userAdminStub;
    private static final String serviceName = "UserAdmin";

    public static void addUser(String userName, String password, String backendURL, String[] roleList,
            String adminUsername, String adminPassword) throws RemoteException, UserAdminUserAdminException {

        String endPoint = backendURL + serviceName;
        userAdminStub = new UserAdminStub(endPoint);
        AuthenticateStub.authenticateStub(adminUsername, adminPassword, userAdminStub);
        userAdminStub.addUser(userName, password, roleList, null, null);
    }

}
