/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.esb.integration.common.clients.mediation;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.event.stub.internal.TopicManagerAdminServiceStub;
import org.wso2.carbon.event.stub.internal.xsd.TopicNode;

public class TopicAdminClient {

	private static final Log log = LogFactory.getLog(TopicAdminClient.class);

	String backendUrl = null;
	String SessionCookie = null;
	ConfigurationContext configurationContext = null;

	public TopicAdminClient(String backendUrl, String sessionCookie,
	                        ConfigurationContext configurationContext) {

		this.backendUrl = backendUrl
		                  + "TopicManagerAdminService.TopicManagerAdminServiceHttpsSoap12Endpoint";
		this.SessionCookie = sessionCookie;
		this.configurationContext = configurationContext;

	}

	public void addTopic(String topic) throws Exception {

		TopicManagerAdminServiceStub stub = new TopicManagerAdminServiceStub(configurationContext,
		                                                                     backendUrl);
		configureCookie(stub._getServiceClient());
		stub.addTopic(topic);
	}

	public TopicNode getAllTopics() throws Exception {
		TopicManagerAdminServiceStub stub = new TopicManagerAdminServiceStub(configurationContext,
		                                                                     backendUrl);
		configureCookie(stub._getServiceClient());
		return stub.getAllTopics();
	}

	public void removeTopic(String topic) throws Exception {

		TopicManagerAdminServiceStub stub = new TopicManagerAdminServiceStub(configurationContext,
		                                                                     backendUrl);
		configureCookie(stub._getServiceClient());
		stub.removeTopic(topic);
	}

	private void configureCookie(ServiceClient client) throws AxisFault {
		if (SessionCookie != null) {
			Options option = client.getOptions();
			option.setManageSession(true);
			option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING,
			                   SessionCookie);
		}
	}

}
