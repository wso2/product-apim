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
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
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
                resourcePath = CarbonUtils.getCarbonHome() + "/migration-scripts/18-19-migration/";
            } else {
                throw new APIMigrationException("No query picked up for the given migrate version. Please check the migrate version.");
            }

            if (Constants.CONSTRAINT.equals(queryType)) {
                resourcePath = CarbonUtils.getCarbonHome() + "/migration-scripts/18-19-migration/";
                //queryTobeExecuted = resourcePath + "drop-fk.sql";
                queryTobeExecuted = IOUtils.toString(new FileInputStream(new File(resourcePath + "drop-fk.sql")), "UTF-8");
            } else {
                queryTobeExecuted = resourcePath + databaseType + ".sql";
                //queryTobeExecuted = IOUtils.toString(new FileInputStream(new File(resourcePath + databaseType + ".sql")), "UTF-8");
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
     * @param filePath       file path
     * @param implementation new impl
     * @throws APIMigrationException
     */
    public static void updateSynapseAPI(File filePath, String implementation) throws APIMigrationException {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = null;
            doc = docBuilder.parse(filePath.getAbsolutePath());
            Node handlers = doc.getElementsByTagName("handlers").item(0);
            Element corsHandler = doc.createElement("handler");
            corsHandler.setAttribute("class", "org.wso2.carbon.apimgt.gateway.handlers.security.CORSRequestHandler");
            Element property = doc.createElement("property");
            property.setAttribute("name", "inline");
            property.setAttribute("value", implementation);
            corsHandler.appendChild(property);
            NodeList handlerNodes = doc.getElementsByTagName("handler");
            for (int i = 0; i < handlerNodes.getLength(); i++) {
                Node tempNode = handlerNodes.item(i);
                if ("org.wso2.carbon.apimgt.gateway.handlers.security.CORSRequestHandler"
                        .equals(tempNode.getAttributes().getNamedItem("class").getTextContent())) {
                    handlers.removeChild(tempNode);
                }
                handlers.insertBefore(corsHandler, handlerNodes.item(0));
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(filePath);
            transformer.transform(source, result);
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
}
