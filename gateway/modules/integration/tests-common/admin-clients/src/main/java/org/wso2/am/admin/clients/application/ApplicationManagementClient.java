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
package org.wso2.am.admin.clients.application;

import org.apache.axis2.AxisFault;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.carbon.identity.application.common.model.xsd.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.stub.IdentityApplicationManagementServiceIdentityApplicationManagementException;
import org.wso2.carbon.identity.application.mgt.stub.IdentityApplicationManagementServiceStub;

import java.rmi.RemoteException;

public class ApplicationManagementClient {

    private String service = "IdentityApplicationManagementService";

    private IdentityApplicationManagementServiceStub identityApplicationManagementServiceStub;

    public ApplicationManagementClient(String backEndUrl, String sessionCookie) throws AxisFault {

        String endPoint = backEndUrl + service;
        identityApplicationManagementServiceStub = new IdentityApplicationManagementServiceStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, identityApplicationManagementServiceStub);
    }

    /**
     * Create admin client using username and password
     *
     * @param backEndUrl
     * @param userName
     * @param password
     * @throws AxisFault
     */
    public ApplicationManagementClient(String backEndUrl, String userName, String password)
            throws AxisFault {

        String endPoint = backEndUrl + service;
        identityApplicationManagementServiceStub = new IdentityApplicationManagementServiceStub(endPoint);
        AuthenticateStub.authenticateStub(userName, password, identityApplicationManagementServiceStub);
    }

    public ServiceProvider getApplication(String appname)
            throws RemoteException, IdentityApplicationManagementServiceIdentityApplicationManagementException {

        return identityApplicationManagementServiceStub.getApplication(appname);
    }

    public void updateApplication(ServiceProvider serviceProvider)
            throws RemoteException, IdentityApplicationManagementServiceIdentityApplicationManagementException {

        identityApplicationManagementServiceStub.updateApplication(serviceProvider);
    }

    public void createApplication(ServiceProvider serviceProvider)
            throws RemoteException, IdentityApplicationManagementServiceIdentityApplicationManagementException {
        identityApplicationManagementServiceStub.createApplication(serviceProvider);
    }

    public void deleteApplication(String applicationName)
            throws RemoteException, IdentityApplicationManagementServiceIdentityApplicationManagementException {
        identityApplicationManagementServiceStub.deleteApplication(applicationName);
    }
}
