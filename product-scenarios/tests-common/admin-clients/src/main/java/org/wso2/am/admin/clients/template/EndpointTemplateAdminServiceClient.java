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
package org.wso2.am.admin.clients.template;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.carbon.mediation.templates.endpoint.stub.types.EndpointTemplateAdminServiceStub;
import org.wso2.carbon.mediation.templates.endpoint.stub.types.common.EndpointTemplateInfo;

import javax.activation.DataHandler;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.rmi.RemoteException;

/*
This class can be used to manage ESB Endpoint templates
*/
public class EndpointTemplateAdminServiceClient {
	private static final Log log = LogFactory.getLog(EndpointTemplateAdminServiceClient.class);

	private final String serviceName = "EndpointTemplateAdminService";
	private EndpointTemplateAdminServiceStub endpointTemplateAdminStub;

	public EndpointTemplateAdminServiceClient(String backEndUrl, String sessionCookie)
			throws AxisFault {
		String endPoint = backEndUrl + serviceName;
		endpointTemplateAdminStub = new EndpointTemplateAdminServiceStub(endPoint);
		AuthenticateStub.authenticateStub(sessionCookie, endpointTemplateAdminStub);
	}

	public EndpointTemplateAdminServiceClient(String backEndUrl, String userName, String password)
			throws AxisFault {
		String endPoint = backEndUrl + serviceName;
		endpointTemplateAdminStub = new EndpointTemplateAdminServiceStub(endPoint);
		AuthenticateStub.authenticateStub(userName, password, endpointTemplateAdminStub);
	}

	public void addEndpointTemplate(OMElement endpointTemplate) throws RemoteException {
		endpointTemplateAdminStub.addEndpointTemplate(endpointTemplate.toString());
	}

	public void addEndpointTemplate(DataHandler dh) throws IOException, XMLStreamException {
		XMLStreamReader parser =
				XMLInputFactory.newInstance().createXMLStreamReader(dh.getInputStream());
		//create the builder
		StAXOMBuilder builder = new StAXOMBuilder(parser);
		OMElement endpointTemplate = builder.getDocumentElement();
		endpointTemplateAdminStub.addEndpointTemplate(endpointTemplate.toString());
	}

	public void addDynamicEndpointTemplate(String key, OMElement endpointTemplate)
			throws RemoteException {
		endpointTemplateAdminStub.addDynamicEndpointTemplate(key, endpointTemplate.toString());
	}

	public void addDynamicEndpointTemplate(String key, DataHandler dh)
			throws IOException, XMLStreamException {
		XMLStreamReader parser =
				XMLInputFactory.newInstance().createXMLStreamReader(dh.getInputStream());
		//create the builder
		StAXOMBuilder builder = new StAXOMBuilder(parser);
		OMElement endpointTemplate = builder.getDocumentElement();
		endpointTemplateAdminStub.addDynamicEndpointTemplate(key, endpointTemplate.toString());
	}

	public void deleteEndpointTemplate(String templateName) throws RemoteException {
		endpointTemplateAdminStub.deleteEndpointTemplate(templateName);
	}

	public void deleteDynamicEndpointTemplate(String key) throws RemoteException {
		endpointTemplateAdminStub.deleteDynamicEndpointTemplate(key);
	}

	public int getDynamicEndpointTemplatesCount() throws RemoteException {
		return endpointTemplateAdminStub.getDynamicEndpointTemplatesCount();
	}

	public int getEndpointTemplatesCount() throws RemoteException {
		return endpointTemplateAdminStub.getEndpointTemplatesCount();
	}

	public String[] getEndpointTemplates() throws RemoteException {
		EndpointTemplateInfo[] info = endpointTemplateAdminStub.getEndpointTemplates(0, 200);
		if (info == null || info.length == 0) {
			return null;
		}
		String[] templates = new String[info.length];
		int i = 0;
		for (EndpointTemplateInfo tmpInfo : info) {
			templates[i++] = tmpInfo.getTemplateName();
		}
		return templates;
	}

}
