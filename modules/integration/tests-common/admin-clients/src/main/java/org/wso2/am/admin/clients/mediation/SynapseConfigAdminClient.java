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

    public SynapseConfigAdminClient(String backEndUrl, String userName, String password) throws AxisFault {
        String endPoint = backEndUrl + serviceName;
        configServiceAdminStub = new ConfigServiceAdminStub(endPoint);
        AuthenticateStub.authenticateStub(userName, password, configServiceAdminStub);
    }

    /**
     * Activating service
     *
     * @param serviceName - service name need to be activated
     * @throws RemoteException for the activate() method call in ConfigServiceAdminStub
     */
    public void activateService(String serviceName) throws RemoteException {
        configServiceAdminStub.activate(serviceName);
    }

    /**
     * Adding more configuration to the existing service
     *
     * @param serviceName - service name
     * @throws RemoteException for the addExistingConfiguration() method call in ConfigServiceAdminStub
     */
    public void addExistingConfiguration(String serviceName) throws RemoteException {
        configServiceAdminStub.addExistingConfiguration(serviceName);
    }

    /**
     * Create synapse configuration
     *
     * @param serviceName - service name
     * @param description - service description
     * @throws RemoteException for the create() method call in ConfigServiceAdminStub
     */
    public void create(String serviceName, String description) throws RemoteException {
        configServiceAdminStub.create(serviceName, description);
    }

    /**
     * Deleting synapse configuration
     *
     * @param serviceName - service name
     * @throws RemoteException for the deleteConfiguration() method call in ConfigServiceAdminStub
     */
    public void deleteConfiguration(String serviceName) throws RemoteException {
        configServiceAdminStub.deleteConfiguration(serviceName);
    }

    /**
     * Get current synapse configuration
     *
     * @return String - synapse configuration
     * @throws RemoteException for the getConfiguration() method call in ConfigServiceAdminStub
     */
    public String getConfiguration() throws RemoteException {
        return configServiceAdminStub.getConfiguration();
    }

    /**
     * Get current configuration List
     *
     * @return configuration list
     * @throws RemoteException for the getConfigurationList() method call in ConfigServiceAdminStub
     */
    public ConfigurationInformation[] getConfigurationList() throws RemoteException {
        return configServiceAdminStub.getConfigurationList();
    }

    /**
     * Save Synapse configuration
     *
     * @throws RemoteException for the saveConfigurationToDisk() method call in ConfigServiceAdminStub
     */
    public void saveConfigurationToDisk() throws RemoteException {
        configServiceAdminStub.saveConfigurationToDisk();
    }

    /**
     * Update Synapse configuration using a String that contains the new configuration.
     *
     * @param configuration - synapse configuration
     * @return boolean - return true if update success else return false.
     * @throws RemoteException    for the updateConfiguration() method call in ConfigServiceAdminStub
     * @throws XMLStreamException for the updateConfiguration() method call in ConfigServiceAdminStub
     */

    public boolean updateConfiguration(String configuration) throws XMLStreamException, RemoteException {
        return configServiceAdminStub.updateConfiguration(createOMElement(configuration));
    }


    /**
     * Update Synapse configuration using a File object that contains the new configuration.
     *
     * @param file - File that contains the synapse configuration
     * @return boolean - true if update process success, else  returns false
     * @throws IOException                  for the updateConfiguration() method call in ConfigServiceAdminStub and
     *                                      parse() method call in DocumentBuilder
     * @throws SAXException                 for the parse() method call in DocumentBuilder
     * @throws ParserConfigurationException for the newDocumentBuilder() method call in DocumentBuilderFactory
     * @throws TransformerException         for the getStringFromDocument() method call
     * @throws XMLStreamException           for the updateConfiguration() method call in ConfigServiceAdminStub
     */
    public boolean updateConfiguration(File file) throws IOException, SAXException, ParserConfigurationException,
            TransformerException, XMLStreamException {
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
     * update synapse configuration using a OMElement object that contains the new configuration.
     *
     * @param configuration - synapse configuration
     * @return configuration update status
     * @throws RemoteException for the updateConfiguration() method call in ConfigServiceAdminStub
     */
    public boolean updateConfiguration(OMElement configuration) throws RemoteException {
        return configServiceAdminStub.updateConfiguration(configuration);
    }

    /**
     * Validate synapse configuration using a OMElement
     *
     * @param configuration - synapse configuration
     * @return validation error array
     * @throws RemoteException for the validateConfiguration() method call in ConfigServiceAdminStub
     */
    public ValidationError[] validateConfiguration(OMElement configuration) throws RemoteException {
        return configServiceAdminStub.validateConfiguration(configuration);
    }

    /**
     * Create OMElement using String this is in ML format
     *
     * @param xml - String that is in XML format to create the OMElement
     * @return OMElement - newly created OMElement
     * @throws XMLStreamException - for the newInstance() method call in XMLInputFactory
     */
    private static OMElement createOMElement(String xml) throws XMLStreamException {
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
        StAXOMBuilder builder = new StAXOMBuilder(reader);
        return builder.getDocumentElement();

    }

    /**
     * Get the String value form Document object
     *
     * @param doc - Document object the need to  retrieve the string value
     * @return String -  String value of the Document object provided.
     * @throws TransformerException for the newTransformer()  and transform() method calls in Transformer
     */
    private String getStringFromDocument(Document doc) throws TransformerException {
        DOMSource domSource = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(domSource, result);
        String stringFromDoc = writer.toString();
        try {
            writer.close();
        } catch (IOException e) {
            log.warn("Exception when closing the StringWriter.", e);
        }
        return stringFromDoc;
    }


}