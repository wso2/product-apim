/*
 *  Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.admin.clients.application;

import org.apache.axis2.AxisFault;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.carbon.application.mgt.stub.upload.CarbonAppUploaderStub;
import org.wso2.carbon.application.mgt.stub.upload.types.carbon.UploadedFileItem;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import java.rmi.RemoteException;

public class CarbonApplicationUploaderClient {

    private String service = "CarbonAppUploader";

    private CarbonAppUploaderStub carbonAppUploaderStub;

    public CarbonApplicationUploaderClient(String backEndUrl, String sessionCookie) throws AxisFault {

        String endPoint = backEndUrl + service;
        carbonAppUploaderStub = new CarbonAppUploaderStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, carbonAppUploaderStub);
    }

    public void uploadCarbonApp(String filePath) throws RemoteException {

        UploadedFileItem fileItem = new UploadedFileItem();
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        if (fileName.lastIndexOf('\\') >= 0) {
            fileName = fileName.substring(fileName.lastIndexOf('\\') + 1);
        }
        fileItem.setFileName(fileName);
        fileItem.setFileType("jar");  // CAR files are internally treated with fileType jar
        FileDataSource fileDataSource = new FileDataSource(filePath);
        fileItem.setDataHandler(new DataHandler(fileDataSource));
        carbonAppUploaderStub.uploadApp(new UploadedFileItem[] { fileItem });
    }
}