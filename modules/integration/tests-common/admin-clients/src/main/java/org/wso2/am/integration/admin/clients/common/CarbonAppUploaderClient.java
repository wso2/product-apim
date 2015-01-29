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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.admin.clients.utils.AuthenticateStubUtil;
import org.wso2.carbon.application.mgt.stub.upload.CarbonAppUploader;
import org.wso2.carbon.application.mgt.stub.upload.CarbonAppUploaderStub;
import org.wso2.carbon.application.mgt.stub.upload.types.carbon.UploadedFileItem;

import javax.activation.DataHandler;
import java.rmi.RemoteException;

public class CarbonAppUploaderClient {
    private static final Log log = LogFactory.getLog(CarbonAppUploader.class);
    private CarbonAppUploaderStub carbonAppUploaderStub;
    private final String serviceName = "CarbonAppUploader";

    public CarbonAppUploaderClient(String backendUrl, String sessionCookie) throws AxisFault {
        String endPoint = backendUrl + serviceName;
        carbonAppUploaderStub = new CarbonAppUploaderStub(endPoint);
        AuthenticateStubUtil.authenticateStub(sessionCookie, carbonAppUploaderStub);
    }

    public CarbonAppUploaderClient(String backendUrl, String userName, String password) throws AxisFault {
        String endPoint = backendUrl + serviceName;
        carbonAppUploaderStub = new CarbonAppUploaderStub(endPoint);
        AuthenticateStubUtil.authenticateStub(userName, password, carbonAppUploaderStub);
    }

    public void uploadCarbonAppArtifact(String fileName, DataHandler dh)
            throws RemoteException {
        UploadedFileItem[] carbonAppArray = new UploadedFileItem[1];
        UploadedFileItem carbonApp = new UploadedFileItem();
        carbonApp.setFileName(fileName);
        carbonApp.setDataHandler(dh);
        carbonApp.setFileType("jar");
        carbonAppArray[0] = carbonApp;
        carbonAppUploaderStub.uploadApp(carbonAppArray);
    }
}

