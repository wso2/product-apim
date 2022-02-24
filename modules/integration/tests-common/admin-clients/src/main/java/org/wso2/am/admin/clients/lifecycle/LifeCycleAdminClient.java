/*
* Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.admin.clients.lifecycle;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.carbon.governance.lcm.stub.LifeCycleManagementServiceExceptionException;
import org.wso2.carbon.governance.lcm.stub.LifeCycleManagementServiceStub;

import java.rmi.RemoteException;

public class LifeCycleAdminClient {

    private static final Log log = LogFactory.getLog(LifeCycleAdminClient.class);
    private final String serviceName = "LifeCycleManagementService";
    private LifeCycleManagementServiceStub lifeCycleServiceStub;

    public LifeCycleAdminClient(String backEndUrl, String sessionCookie) throws AxisFault {
        String endPoint = backEndUrl + serviceName;
        lifeCycleServiceStub = new LifeCycleManagementServiceStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, lifeCycleServiceStub);
    }

    public LifeCycleAdminClient(String backEndUrl, String userName, String password) throws AxisFault {
        String endPoint = backEndUrl + serviceName;
        lifeCycleServiceStub = new LifeCycleManagementServiceStub(endPoint);
        AuthenticateStub.authenticateStub(userName, password, lifeCycleServiceStub);
    }

    public void createLifecycle(String configuration) throws Exception {
        lifeCycleServiceStub.createLifecycle(configuration);
    }

    public void deleteLifecycle(String name) throws Exception {
        lifeCycleServiceStub.deleteLifecycle(name);
    }

    public String getLifecycleConfiguration(String lifeCycleName)
            throws LifeCycleManagementServiceExceptionException, RemoteException {
        return lifeCycleServiceStub.getLifecycleConfiguration(lifeCycleName);
    }

    public boolean editLifeCycle(String oldLifeCycleName,
            String lifeCycleConfiguration)
            throws LifeCycleManagementServiceExceptionException, RemoteException {
        return lifeCycleServiceStub.updateLifecycle(oldLifeCycleName, lifeCycleConfiguration);
    }
}
