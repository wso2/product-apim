/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.wso2.carbon.apimgt.migration.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.apimgt.migration.client.MigrationDBCreator;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.utils.CarbonUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

public class ResourceUtil {

    private static final Log log = LogFactory.getLog(ResourceUtil.class);

    /**
     * location for the swagger 1.2 resources
     *
     * @param apiName     api name
     * @param apiVersion  api version
     * @param apiProvider api provider
     * @return swagger v1.2 location
     */
    public static String getSwagger12ResourceLocation(String apiName, String apiVersion, String apiProvider) {
        return APIConstants.API_DOC_LOCATION + RegistryConstants.PATH_SEPARATOR + apiName + "-" + apiVersion + "-"
                + apiProvider + RegistryConstants.PATH_SEPARATOR + APIConstants.API_DOC_1_2_LOCATION;
    }


    /**
     * location for the swagger v2.0 resources
     *
     * @param apiName     name of the API
     * @param apiVersion  version of the API
     * @param apiProvider provider name of the API
     * @return swagger v2.0 resource location as a string
     */
    public static String getSwagger2ResourceLocation(String apiName, String apiVersion, String apiProvider) {
        return APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR + apiProvider
                + RegistryConstants.PATH_SEPARATOR + apiName + RegistryConstants.PATH_SEPARATOR + apiVersion
                + RegistryConstants.PATH_SEPARATOR + "swagger.json";
    }

    /**
     * location for the rxt of the api
     *
     * @param apiName     name of the API
     * @param apiVersion  version of the API
     * @param apiProvider provider name of the API
     * @return rxt location for the api as a string
     */
    public static String getRxtResourceLocation(String apiName, String apiVersion, String apiProvider) {
        return APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR + apiProvider
                + RegistryConstants.PATH_SEPARATOR + apiName + RegistryConstants.PATH_SEPARATOR + apiVersion
                + RegistryConstants.PATH_SEPARATOR + "api";
    }



    /**
     * This method picks the query according to the users database
     *
     * @param migrateVersion migrate version
     * @return exact query to execute
     * @throws SQLException
     * @throws APIMigrationException
     * @throws IOException
     */
    public static String pickQueryFromResources(String migrateVersion, String queryType) throws SQLException, APIMigrationException,
            IOException {

        String queryTobeExecuted;
        try {
            String databaseType = MigrationDBCreator.getDatabaseType(APIMgtDBUtil.getConnection());

            String resourcePath;

            if (Constants.VERSION_1_9.equalsIgnoreCase(migrateVersion)) {
                //pick from 18to19Migration/sql-scripts
                resourcePath = CarbonUtils.getCarbonHome() + File.separator + "migration-scripts" + File.separator + "18-19-migration" + File.separator;
            }
            else if (Constants.VERSION_1_10.equalsIgnoreCase(migrateVersion)) {
                //pick from 19to110Migration/sql-scripts
                resourcePath = CarbonUtils.getCarbonHome() + File.separator + "migration-scripts" + File.separator + "19-110-migration" + File.separator;
            } else {
                throw new APIMigrationException("No query picked up for the given migrate version. Please check the migrate version.");
            }

            if (Constants.CONSTRAINT.equals(queryType)) {
                resourcePath = CarbonUtils.getCarbonHome() + File.separator + "migration-scripts" + File.separator + "18-19-migration" + File.separator;
                queryTobeExecuted = IOUtils.toString(new FileInputStream(new File(resourcePath + "drop-fk.sql")), "UTF-8");
            } else {
                queryTobeExecuted = resourcePath + databaseType + ".sql";
            }

        } catch (IOException e) {
            throw new APIMigrationException("Error occurred while accessing the sql from resources. " + e);
        } catch (Exception e) {
            //getDatabaseType inherited from DBCreator, which throws generic exception
            throw new APIMigrationException("Error occurred while searching for database type " + e);
        }

        return queryTobeExecuted;
    }

    /**
     * To handle exceptions
     *
     * @param msg error message
     * @throws APIMigrationException
     */
    public static void handleException(String msg, Throwable e) throws APIMigrationException {
        log.error(msg, e);
        throw new APIMigrationException(msg, e);
    }

    /**
     * To copy a new sequence to existing ones
     *
     * @param sequenceDirectoryFilePath sequence directory
     * @param sequenceName              sequence name
     * @throws APIMigrationException
     */
    public static void copyNewSequenceToExistingSequences(String sequenceDirectoryFilePath, String sequenceName)
            throws APIMigrationException {
        try {
            String namespace = "http://ws.apache.org/ns/synapse";
            String filePath = sequenceDirectoryFilePath + sequenceName + ".xml";

            File sequenceFile = new File(filePath);

            if (!sequenceFile.exists()) {
                log.debug("Sequence file " + sequenceName + ".xml does not exist");
                return;
            }

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(filePath);
            Node sequence = doc.getElementsByTagName("sequence").item(0);
            Element corsSequence = doc.createElementNS(namespace, "sequence");
            corsSequence.setAttribute("key", "_cors_request_handler_");
            boolean available = false;
            for (int i = 0; i < sequence.getChildNodes().getLength(); i++) {
                Node tempNode = sequence.getChildNodes().item(i);
                if (tempNode.getNodeType() == Node.ELEMENT_NODE &&"sequence".equals(tempNode.getLocalName()) &&
                    "_cors_request_handler_".equals(tempNode.getAttributes().getNamedItem("key").getTextContent())) {
                    available = true;
                    break;
                }
            }
            if (!available) {
                if ("_throttle_out_handler_".equals(sequenceName)) {
                    sequence.appendChild(corsSequence);
                } else if ("_auth_failure_handler_".equals(sequenceName)) {
                    sequence.appendChild(corsSequence);
                } else {
                    sequence.insertBefore(corsSequence, doc.getElementsByTagName("send").item(0));
                }
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(new File(filePath));
                transformer.transform(source, result);
            }
        } catch (ParserConfigurationException e) {
            handleException("Could not initiate Document Builder.", e);
        } catch (TransformerConfigurationException e) {
            handleException("Could not initiate TransformerFactory Builder.", e);
        } catch (TransformerException e) {
            handleException("Could not transform the source.", e);
        } catch (SAXException e) {
            handleException("SAX exception occurred while parsing the file.", e);
        } catch (IOException e) {
            handleException("IO Exception occurred. Please check the file.", e);
        }
    }

    /**
     * To update synapse API
     *
     * @param document       XML document object
     * @param file       synapse file
     * @throws APIMigrationException
     */
    public static void updateSynapseAPI(Document document, File file) throws APIMigrationException {
        try {
            updateAPIAttributes(document, file);
            updateHandlers(document, file);
            updateResources(document, file);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            handleException("Could not initiate TransformerFactory Builder.", e);
        } catch (TransformerException e) {
            handleException("Could not transform the source.", e);
        }
    }

    private static void updateAPIAttributes(Document document, File file) {
        Element apiElement = document.getDocumentElement();

        String versionType = apiElement.getAttribute(Constants.SYNAPSE_API_ATTRIBUTE_VERSION_TYPE);

        if (versionType.equals(Constants.SYNAPSE_API_VALUE_VERSION_TYPE_URL)) {
            String context = apiElement.getAttribute(Constants.SYNAPSE_API_ATTRIBUTE_CONTEXT);
            String version = apiElement.getAttribute(Constants.SYNAPSE_API_ATTRIBUTE_VERSION);

            context = context + "/" + version;
            apiElement.setAttribute(Constants.SYNAPSE_API_ATTRIBUTE_CONTEXT, context);
            apiElement.setAttribute(Constants.SYNAPSE_API_ATTRIBUTE_VERSION_TYPE, Constants.SYNAPSE_API_VALUE_VERSION_TYPE_CONTEXT);
        }
    }


    private static void updateHandlers(Document document, File file) {
        Element handlersElement = (Element) document.getElementsByTagNameNS(Constants.SYNAPSE_API_XMLNS, 
                                                                            Constants.SYNAPSE_API_ELEMENT_HANDLERS).item(0);

        if (handlersElement != null) {
            NodeList handlerNodes = handlersElement.getElementsByTagName(Constants.SYNAPSE_API_ELEMENT_HANDLER);
    
            for (int i = 0; i < handlerNodes.getLength(); ++i) {
                Element handler = (Element) handlerNodes.item(i);
    
                String className = handler.getAttribute(Constants.SYNAPSE_API_ATTRIBUTE_CLASS);
    
                if (className.equals(Constants.SYNAPSE_API_VALUE_CORS_HANDLER)) {
                    handlersElement.removeChild(handler);
                    break;
                }
            }
    
            // Find the inSequence
            Element inSequenceElement = (Element) document.getElementsByTagName
                    (Constants.SYNAPSE_API_ELEMENT_INSEQUENCE).item(0);
            
            NodeList sendElements = null;            
            if (inSequenceElement != null) {    
                sendElements = inSequenceElement.getElementsByTagName(Constants.SYNAPSE_API_ELEMENT_SEND);
            }
    
            Element corsHandler = document.createElementNS(Constants.SYNAPSE_API_XMLNS, 
                                                           Constants.SYNAPSE_API_ELEMENT_HANDLER);
            corsHandler.setAttribute(Constants.SYNAPSE_API_ATTRIBUTE_CLASS, Constants.SYNAPSE_API_VALUE_CORS_HANDLER);
            Element property = document.createElementNS(Constants.SYNAPSE_API_XMLNS, 
                                                        Constants.SYNAPSE_API_ELEMENT_PROPERTY);
            property.setAttribute(Constants.SYNAPSE_API_ATTRIBUTE_NAME, Constants.SYNAPSE_API_VALUE_INLINE);
    
            if (sendElements != null && 0 < sendElements.getLength()) {
                property.setAttribute(Constants.SYNAPSE_API_ATTRIBUTE_VALUE, Constants.SYNAPSE_API_VALUE_ENPOINT);
            }
            else {
                property.setAttribute(Constants.SYNAPSE_API_ATTRIBUTE_VALUE, 
                                      Constants.SYNAPSE_API_VALUE_INLINE_UPPERCASE);
            }
    
            corsHandler.appendChild(property);
    
            handlersElement.insertBefore(corsHandler, handlersElement.getFirstChild());
        }

    }

    private static void updateResources(Document document, File file) throws APIMigrationException {
        NodeList resourceNodes = document.getElementsByTagName("resource");
        for (int i = 0; i < resourceNodes.getLength(); i++) {
            Element resourceElement = (Element) resourceNodes.item(i);

            updateInSequence(resourceElement, document);

            updateOutSequence(resourceElement, document);
        }
    }

    private static void updateInSequence(Element resourceElement, Document doc) {
        // Find the inSequence
        Element inSequenceElement = (Element) resourceElement.getElementsByTagName(Constants.SYNAPSE_API_ELEMENT_INSEQUENCE).item(0);

        // Find the property element in the inSequence
        NodeList properties = inSequenceElement.getElementsByTagName(Constants.SYNAPSE_API_ELEMENT_PROPERTY);

        boolean isBackEndRequestTimeSet = false;

        for (int i = 0; i < properties.getLength(); ++i) {
            Element propertyElement = (Element) properties.item(i);

            if (propertyElement.hasAttribute(Constants.SYNAPSE_API_ATTRIBUTE_NAME)) {
                if (Constants.SYNAPSE_API_VALUE_BACKEND_REQUEST_TIME.equals(propertyElement.getAttribute(Constants.SYNAPSE_API_ATTRIBUTE_NAME))) {
                    isBackEndRequestTimeSet = true;
                    break;
                }
            }
        }

        if (!isBackEndRequestTimeSet) {
            NodeList filters = inSequenceElement.getElementsByTagName(Constants.SYNAPSE_API_ELEMENT_FILTER);

            for (int j = 0; j < filters.getLength(); ++j) {
                Element filterElement = (Element) filters.item(j);

                if (Constants.SYNAPSE_API_VALUE_AM_KEY_TYPE.equals(filterElement.getAttribute(Constants.SYNAPSE_API_ATTRIBUTE_SOURCE))) {
                    // Only one <then> element can exist in filter mediator
                    Element thenElement = (Element) filterElement.getElementsByTagNameNS(Constants.SYNAPSE_API_XMLNS, Constants.SYNAPSE_API_ELEMENT_THEN).item(0);

                    // At least one <send> element must exist as a child of <then> element
                    Element sendElement = (Element) thenElement.getElementsByTagName(Constants.SYNAPSE_API_ELEMENT_SEND).item(0);

                    Element propertyElement = doc.createElementNS(Constants.SYNAPSE_API_XMLNS, Constants.SYNAPSE_API_ELEMENT_PROPERTY);
                    propertyElement.setAttribute(Constants.SYNAPSE_API_ATTRIBUTE_NAME, Constants.SYNAPSE_API_VALUE_BACKEND_REQUEST_TIME);
                    propertyElement.setAttribute(Constants.SYNAPSE_API_ATTRIBUTE_EXPRESSION, Constants.SYNAPSE_API_VALUE_EXPRESSION);

                    thenElement.insertBefore(propertyElement, sendElement);
                }
            }
        }
    }


    private static void updateOutSequence(Element resourceElement, Document doc) {
        // Find the outSequence
        Element outSequenceElement = (Element) resourceElement.getElementsByTagNameNS(Constants.SYNAPSE_API_XMLNS, 
                                                                     Constants.SYNAPSE_API_ELEMENT_OUTSEQUENCE).item(0);
        if (outSequenceElement != null) {

            NodeList classNodes = outSequenceElement.getElementsByTagName(Constants.SYNAPSE_API_ELEMENT_CLASS);
    
            boolean isResponseHandlerSet = false;
    
            for (int i = 0; i < classNodes.getLength(); ++i) {
                Element classElement = (Element) classNodes.item(i);
    
                if (Constants.SYNAPSE_API_VALUE_RESPONSE_HANDLER.equals(classElement.getAttribute(Constants.SYNAPSE_API_ATTRIBUTE_NAME))) {
                    isResponseHandlerSet = true;
                    break;
                }
            }
    
            if (!isResponseHandlerSet) {
                // There must be at least one <send> element for an outSequence
                Element sendElement = (Element) outSequenceElement.getElementsByTagName(Constants.SYNAPSE_API_ELEMENT_SEND).item(0);
    
                Element classElement = doc.createElementNS(Constants.SYNAPSE_API_XMLNS, Constants.SYNAPSE_API_ELEMENT_CLASS);
                classElement.setAttribute(Constants.SYNAPSE_API_ATTRIBUTE_NAME, Constants.SYNAPSE_API_VALUE_RESPONSE_HANDLER);
                classElement.removeAttribute(Constants.SYNAPSE_API_ATTRIBUTE_XMLNS);
    
                outSequenceElement.insertBefore(classElement, sendElement);
            }
        }
    }
}
