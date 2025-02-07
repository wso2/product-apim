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
package org.wso2.am.admin.clients.tasks;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.carbon.task.stub.TaskAdminStub;
import org.wso2.carbon.task.stub.TaskManagementException;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TaskAdminClient {
	private static final Log log = LogFactory.getLog(TaskAdminClient.class);

	private final String serviceName = "TaskAdmin";
	private TaskAdminStub taskAdminStub;

	public TaskAdminClient(String backEndUrl, String sessionCookie) throws AxisFault {
		String endPoint = backEndUrl + serviceName;
		taskAdminStub = new TaskAdminStub(endPoint);
		AuthenticateStub.authenticateStub(sessionCookie, taskAdminStub);
	}

	public TaskAdminClient(String backEndUrl, String userName, String password) throws AxisFault {
		String endPoint = backEndUrl + serviceName;
		taskAdminStub = new TaskAdminStub(endPoint);
		AuthenticateStub.authenticateStub(userName, password, taskAdminStub);
	}

	public void addTask(DataHandler dh)
			throws TaskManagementException, IOException, XMLStreamException {

		XMLStreamReader parser =
				XMLInputFactory.newInstance().createXMLStreamReader(dh.getInputStream());
		//create the builder
		StAXOMBuilder builder = new StAXOMBuilder(parser);
		OMElement scheduleTaskElem = builder.getDocumentElement();
		// scheduleTaskElem.setText("test");
		taskAdminStub.addTaskDescription(scheduleTaskElem);
	}

	public void addTask(OMElement scheduleTaskElem)
			throws TaskManagementException, RemoteException {
		taskAdminStub.addTaskDescription(scheduleTaskElem);
	}

	public void deleteTask(String name, String group)
			throws TaskManagementException, RemoteException {
		taskAdminStub.deleteTaskDescription(name, group);

	}

	public OMElement getScheduleTask(String name, String group)
			throws TaskManagementException, RemoteException {
		return taskAdminStub.getTaskDescription(name, group);

	}

	public OMElement getAllTaskDescriptions() throws RemoteException, TaskManagementException {
		//TODO:With mediation 4.4.3 released version, getAllTaskDescriptions expects a parameter. Have to fix this properly later. 
		return taskAdminStub.getAllTaskDescriptions(null);
	}

	public List<String> getScheduleTaskList() throws RemoteException, TaskManagementException {
		ArrayList<String> taskList = new ArrayList();
		OMElement tasksOme = getAllTaskDescriptions().getFirstElement();
		if (tasksOme != null) {
			Iterator<OMElement> itr = tasksOme.getChildrenWithName(new QName("task"));
			while (itr.hasNext()) {
				OMElement taskOme = itr.next();
				taskList.add(taskOme.getAttributeValue(new QName("name")));
			}
		}
		return taskList;
	}

}
