/*
 *Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.scenario.test.common.clients;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Stub;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.integration.common.admin.client.utils.AuthenticateStubUtil;
import org.wso2.carbon.um.ws.api.stub.RemoteUserStoreManagerServiceStub;
import org.wso2.carbon.um.ws.api.stub.RemoteUserStoreManagerServiceUserStoreExceptionException;

import java.rmi.RemoteException;

public class UserMgtClient {

    private final Log log = LogFactory.getLog(UserMgtClient.class);
    private final String serviceName = "RemoteUserStoreManagerService";
    private RemoteUserStoreManagerServiceStub remoteUserStoreManagerServiceStub;

    public UserMgtClient(String backendURL, String sessionCookie) throws AxisFault {
        String endPoint = backendURL + serviceName;
        remoteUserStoreManagerServiceStub = new RemoteUserStoreManagerServiceStub(endPoint);
        AuthenticateStubUtil.authenticateStub(sessionCookie, remoteUserStoreManagerServiceStub);
    }

    public UserMgtClient(String backendURL, String userName, String password)
            throws AxisFault {
        String endPoint = backendURL + serviceName;
        remoteUserStoreManagerServiceStub = new RemoteUserStoreManagerServiceStub(endPoint);
        AuthenticateStubUtil.authenticateStub(userName, password, remoteUserStoreManagerServiceStub);
    }

    public Stub getServiceStub() {
        return this.remoteUserStoreManagerServiceStub;
    }

    public boolean isExistingUser(String username)
            throws RemoteUserStoreManagerServiceUserStoreExceptionException, RemoteException {
        return remoteUserStoreManagerServiceStub.isExistingUser(username);
    }

    public void addUser(String username, String password, String[] roleList)
            throws RemoteUserStoreManagerServiceUserStoreExceptionException, RemoteException {
        remoteUserStoreManagerServiceStub.addUser(username, password, roleList, null, null, false);
    }

    public void deleteUser(String username) throws RemoteUserStoreManagerServiceUserStoreExceptionException, RemoteException {
        remoteUserStoreManagerServiceStub.deleteUser(username);
    }
}
