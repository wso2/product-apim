/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.admin.clients.mediation;


import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.carbon.mediation.configadmin.stub.ConfigServiceAdminStub;
import org.wso2.carbon.mediation.configadmin.stub.types.carbon.ConfigurationInformation;
import org.wso2.carbon.mediation.configadmin.stub.types.carbon.ValidationError;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.rmi.RemoteException;

/**
 * This class exposing ConfigServiceAdmin operations to the test cases.
 */
public class SynapseConfigAdminClient {

    private static final Log log = LogFactory.getLog(SynapseConfigAdminClient.class);

    private ConfigServiceAdminStub configServiceAdminStub;
    private final String serviceName = "ConfigServiceAdmin";

    public SynapseConfigAdminClient(String backEndUrl, String sessionCookie) throws AxisFault {

        String endPoint = backEndUrl + serviceName;
        configServiceAdminStub = new ConfigServiceAdminStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, configServiceAdminStub);
    }

    public SynapseConfigAdminClient(String backEndUrl, String userName, String password)
            throws AxisFault {

        String endPoint = backEndUrl + serviceName;
        configServiceAdminStub = new ConfigServiceAdminStub(endPoint);
        AuthenticateStub.authenticateStub(userName, password, configServiceAdminStub);
    }

    /**
     * Activating service
     *
     * @param serviceName - service name need to be activated
     * @throws java.rmi.RemoteException throwable exception
     */
    public void activateService(String serviceName) throws RemoteException {
        configServiceAdminStub.activate(serviceName);
    }

    /**
     * Adding more configuration to the existing service
     *
     * @param serviceName - service name
     * @throws java.rmi.RemoteException throwable exception
     */
    public void addExistingConfiguration(String serviceName) throws RemoteException {
        configServiceAdminStub.addExistingConfiguration(serviceName);
    }

    /**
     * @param serviceName - service name
     * @param description - service description
     * @throws java.rmi.RemoteException throwable exception
     */
    public void create(String serviceName, String description) throws RemoteException {
        configServiceAdminStub.create(serviceName, description);
    }

    /**
     * Deleting synapse configuration
     *
     * @param serviceName - service name
     * @throws java.rmi.RemoteException throwable exception
     */
    public void deleteConfiguration(String serviceName) throws RemoteException {
        configServiceAdminStub.deleteConfiguration(serviceName);
    }

    /**
     * Get current synapse configuration
     *
     * @return synapse configuration
     * @throws java.rmi.RemoteException throwable exception
     */
    public String getConfiguration() throws RemoteException {
        return configServiceAdminStub.getConfiguration();
    }

    /**
     * @return configuration list
     * @throws java.rmi.RemoteException throwable exception
     */
    public ConfigurationInformation[] getConfigurationList() throws RemoteException {
        return configServiceAdminStub.getConfigurationList();
    }

    /**
     * save synapse configuration
     *
     * @throws java.rmi.RemoteException throwable exception
     */
    public void saveConfigurationToDisk() throws RemoteException {
        configServiceAdminStub.saveConfigurationToDisk();
    }

    /**
     * update synapse configuration
     *
     * @param configuration - synapse configuration
     * @return configuration update status
     * @throws java.rmi.RemoteException            throwable exception
     * @throws javax.servlet.ServletException      throwable exception
     * @throws javax.xml.stream.XMLStreamException throwable exception
     */
    public boolean updateConfiguration(String configuration)
            throws XMLStreamException, ServletException, RemoteException {
        return configServiceAdminStub.updateConfiguration(createOMElement(configuration));
    }

    /**
     * Uploads synapse config bu using a file
     *
     * @param file -File that contains the synapse configuration
     * @return
     */
    public boolean updateConfiguration(File file)
            throws IOException, SAXException, ParserConfigurationException, TransformerException,
            XMLStreamException, ServletException {
        boolean success = false;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        if (file.exists()) {
            Document doc = docBuilder.parse(file);
            String fileContent = getStringFromDocument(doc);
            success = configServiceAdminStub.updateConfiguration(createOMElement(fileContent));
        }
        return success;
    }

    /**
     * update synapse configuration
     *
     * @param configuration - synapse configuration
     * @return configuration update status
     * @throws java.rmi.RemoteException            throwable exception
     * @throws javax.servlet.ServletException      throwable exception
     * @throws javax.xml.stream.XMLStreamException throwable exception
     */
    public boolean updateConfiguration(OMElement configuration)
            throws XMLStreamException, ServletException, RemoteException {
        return configServiceAdminStub.updateConfiguration(configuration);
    }

    /**
     * Validate configuration
     *
     * @param configuration - synapse configuration
     * @return validation error array
     * @throws java.rmi.RemoteException throwable exception
     */
    public ValidationError[] validateConfiguration(OMElement configuration) throws RemoteException {
        return configServiceAdminStub.validateConfiguration(configuration);
    }

    private static OMElement createOMElement(String xml)
            throws ServletException, XMLStreamException {
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
        StAXOMBuilder builder = new StAXOMBuilder(reader);
        return builder.getDocumentElement();

    }


    private String getStringFromDocument(Document doc) throws TransformerException {
        DOMSource domSource = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(domSource, result);
        return writer.toString();
    }


}