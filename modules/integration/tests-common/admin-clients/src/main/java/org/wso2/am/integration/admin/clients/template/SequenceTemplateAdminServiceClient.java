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
package org.wso2.am.integration.admin.clients.template;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.admin.clients.utils.AuthenticateStub;
import org.wso2.carbon.mediation.templates.stub.types.TemplateAdminServiceStub;
import org.wso2.carbon.mediation.templates.stub.types.common.TemplateInfo;

import javax.activation.DataHandler;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.rmi.RemoteException;

public class SequenceTemplateAdminServiceClient {
	private static final Log log = LogFactory.getLog(SequenceTemplateAdminServiceClient.class);

	private final String serviceName = "TemplateAdminService";
	private TemplateAdminServiceStub templateAdminStub;

	public SequenceTemplateAdminServiceClient(String backEndUrl, String sessionCookie)
			throws AxisFault {
		String endPoint = backEndUrl + serviceName;
		templateAdminStub = new TemplateAdminServiceStub(endPoint);
		AuthenticateStub.authenticateStub(sessionCookie, templateAdminStub);
	}

	public SequenceTemplateAdminServiceClient(String backEndUrl, String userName, String password)
			throws AxisFault {
		String endPoint = backEndUrl + serviceName;
		templateAdminStub = new TemplateAdminServiceStub(endPoint);
		AuthenticateStub.authenticateStub(userName, password, templateAdminStub);
	}

	public void addSequenceTemplate(OMElement template) throws RemoteException {
		templateAdminStub.addTemplate(template);
	}

	public void addSequenceTemplate(DataHandler dh) throws IOException, XMLStreamException {
		XMLStreamReader parser =
				XMLInputFactory.newInstance().createXMLStreamReader(dh.getInputStream());
		//create the builder
		StAXOMBuilder builder = new StAXOMBuilder(parser);
		OMElement template = builder.getDocumentElement();
		templateAdminStub.addTemplate(template);
	}

	public void addDynamicSequenceTemplate(String key, OMElement template) throws RemoteException {
		templateAdminStub.addDynamicTemplate(key, template);
	}

	public void addDynamicSequenceTemplate(String key, DataHandler dh)
			throws IOException, XMLStreamException {
		XMLStreamReader parser =
				XMLInputFactory.newInstance().createXMLStreamReader(dh.getInputStream());
		//create the builder
		StAXOMBuilder builder = new StAXOMBuilder(parser);
		OMElement template = builder.getDocumentElement();
		templateAdminStub.addDynamicTemplate(key, template);
	}

	public void deleteTemplate(String templateName) throws RemoteException {
		templateAdminStub.deleteTemplate(templateName);
	}

	public void deleteDynamicTemplate(String key) throws RemoteException {
		templateAdminStub.deleteDynamicTemplate(key);
	}

	public int getTemplatesCount() throws RemoteException {
		return templateAdminStub.getTemplatesCount();
	}

	public int getDynamicTemplateCount() throws RemoteException {
		return templateAdminStub.getDynamicTemplateCount();
	}

	public String[] getSequenceTemplates() throws RemoteException {
		TemplateInfo[] info = templateAdminStub.getTemplates(0, 200);
		if (info == null || info.length == 0) {
			return null;
		}
		String[] templates = new String[info.length];
		int i = 0;
		for (TemplateInfo tmpInfo : info) {
			templates[i++] = tmpInfo.getName();
		}
		return templates;
	}
}
