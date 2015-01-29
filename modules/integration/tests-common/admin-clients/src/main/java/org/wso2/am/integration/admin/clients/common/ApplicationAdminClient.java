/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.admin.clients.common;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.admin.clients.utils.AuthenticateStubUtil;
import org.wso2.carbon.application.mgt.stub.ApplicationAdminExceptionException;
import org.wso2.carbon.application.mgt.stub.ApplicationAdminStub;
import org.wso2.carbon.application.mgt.stub.types.carbon.ApplicationMetadata;

import java.rmi.RemoteException;

public class ApplicationAdminClient {
    private static final Log log = LogFactory.getLog(ApplicationAdminClient.class);
    private ApplicationAdminStub applicationAdminStub;
    private final String serviceName = "ApplicationAdmin";

    public ApplicationAdminClient(String backEndUrl, String sessionCookie) throws AxisFault {
        String endPoint = backEndUrl + serviceName;
        applicationAdminStub = new ApplicationAdminStub(endPoint);
        AuthenticateStubUtil.authenticateStub(sessionCookie, applicationAdminStub);
    }

    public ApplicationAdminClient(String backEndUrl, String userName, String password) throws AxisFault {
        String endPoint = backEndUrl + serviceName;
        applicationAdminStub = new ApplicationAdminStub(endPoint);
        AuthenticateStubUtil.authenticateStub(userName, password, applicationAdminStub);
    }

    public void deleteApplication(String appName)
            throws ApplicationAdminExceptionException, RemoteException {
        applicationAdminStub.deleteApplication(appName);
        log.info("Application Deleted");
    }

    public String[] listAllApplications()
            throws ApplicationAdminExceptionException,
            RemoteException {
        String[] appList;
        appList = applicationAdminStub.listAllApplications();
        return appList;
    }

    public ApplicationMetadata getMetaData(String appName)
            throws ApplicationAdminExceptionException, RemoteException {
        ApplicationMetadata appList;
        appList = applicationAdminStub.getAppData(appName);
        return appList;
    }

    public String[] deleteMatchingApplication(String appName)
            throws ApplicationAdminExceptionException, RemoteException {
        String[] appList;
        appList = applicationAdminStub.listAllApplications();
        for (String anAppList : appList) {
            if (appName.contains(anAppList)) {
                applicationAdminStub.deleteApplication(anAppList);
            }
        }
        return appList;
    }

    public ServiceClient getServiceClient() {
        ServiceClient serviceClient;
        serviceClient = applicationAdminStub._getServiceClient();
        return serviceClient;
    }
}
