/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.samples.utils.Clients;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.wso2.carbon.apimgt.samples.utils.stubs.AuthenticateStub;
import org.wso2.carbon.server.admin.stub.ServerAdminException;
import org.wso2.carbon.server.admin.stub.ServerAdminStub;

import java.rmi.RemoteException;

public class ServerAdminServiceClient {

    private final String serviceName = "ServerAdmin";
    private ServerAdminStub serviceAdminStub;
    private String endPoint;
    private ServiceClient serviceClient;

    public ServerAdminServiceClient(String backEndUrl, String username, String password) throws AxisFault {
        this.endPoint = backEndUrl + serviceName;
        serviceAdminStub = new ServerAdminStub(endPoint);
        AuthenticateStub.authenticateStub(username, password, serviceAdminStub);
        serviceClient = serviceAdminStub._getServiceClient();
    }

    public boolean restartServer() throws RemoteException, ServerAdminException {
        return serviceAdminStub.restart();
    }

    public boolean isAlive() throws RemoteException {
        return serviceAdminStub.isAlive();
    }
}
