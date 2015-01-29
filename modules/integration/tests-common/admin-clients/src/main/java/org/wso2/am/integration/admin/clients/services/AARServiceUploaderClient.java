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
package org.wso2.am.integration.admin.clients.services;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.admin.clients.utils.AuthenticateStub;
import org.wso2.carbon.aarservices.stub.ExceptionException;
import org.wso2.carbon.aarservices.stub.ServiceUploaderStub;
import org.wso2.carbon.aarservices.stub.types.carbon.AARServiceData;

import javax.activation.DataHandler;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

public class AARServiceUploaderClient {
	private static final Log log = LogFactory.getLog(AARServiceUploaderClient.class);

	private ServiceUploaderStub serviceUploaderStub;
	private final String serviceName = "ServiceUploader";

	public AARServiceUploaderClient(String backEndUrl, String sessionCookie) throws AxisFault {

		String endPoint = backEndUrl + serviceName;
		try {
			serviceUploaderStub = new ServiceUploaderStub(endPoint);
			AuthenticateStub.authenticateStub(sessionCookie, serviceUploaderStub);
		} catch (AxisFault axisFault) {
			log.error("ServiceUploaderStub Initialization fail " + axisFault.getMessage());
			throw new AxisFault(
					"ServiceUploaderStub Initialization fail " + axisFault.getMessage());
		}
	}

	public AARServiceUploaderClient(String backEndUrl, String userName, String password)
			throws AxisFault {
		String endPoint = backEndUrl + serviceName;
		try {
			serviceUploaderStub = new ServiceUploaderStub(endPoint);
			AuthenticateStub.authenticateStub(userName, password, serviceUploaderStub);
		} catch (AxisFault axisFault) {
			log.error("ServiceUploaderStub Initialization fail " + axisFault.getMessage());
			throw new AxisFault(
					"ServiceUploaderStub Initialization fail " + axisFault.getMessage());
		}
	}

	public void uploadAARFile(String fileName, String filePath,
	                          String serviceHierarchy)
			throws ExceptionException, RemoteException, MalformedURLException {
		AARServiceData aarServiceData;

		aarServiceData = new AARServiceData();
		aarServiceData.setFileName(fileName);
		aarServiceData.setDataHandler(createDataHandler(filePath));
		aarServiceData.setServiceHierarchy(serviceHierarchy);
		serviceUploaderStub.uploadService(new AARServiceData[] { aarServiceData });
	}

	private DataHandler createDataHandler(String filePath) throws MalformedURLException {
		URL url = null;
		try {
			url = new URL("file://" + filePath);
		} catch (MalformedURLException e) {
			log.error("File path URL is invalid" + e);
			throw new MalformedURLException("File path URL is invalid" + e);
		}
		DataHandler dh = new DataHandler(url);
		return dh;
	}
}
