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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.apimgt.migration.client._110Specific.dto.SynapseDTO;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.utils.CarbonUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

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
        return APIConstants.API_DOC_LOCATION + RegistryConstants.PATH_SEPARATOR + apiName + '-' + apiVersion + '-'
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
            docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
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
            updateAPIAttributes(document);
            updateHandlers(document);
            updateResources(document);
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

    private static void updateAPIAttributes(Document document) {
        Element apiElement = document.getDocumentElement();

        String versionType = apiElement.getAttribute(Constants.SYNAPSE_API_ATTRIBUTE_VERSION_TYPE);

        if (Constants.SYNAPSE_API_VALUE_VERSION_TYPE_URL.equals(versionType)) {
            String context = apiElement.getAttribute(Constants.SYNAPSE_API_ATTRIBUTE_CONTEXT);
            String version = apiElement.getAttribute(Constants.SYNAPSE_API_ATTRIBUTE_VERSION);

            context = context + '/' + version;
            apiElement.setAttribute(Constants.SYNAPSE_API_ATTRIBUTE_CONTEXT, context);
            apiElement.setAttribute(Constants.SYNAPSE_API_ATTRIBUTE_VERSION_TYPE, Constants.SYNAPSE_API_VALUE_VERSION_TYPE_CONTEXT);
        }
    }


    private static void updateHandlers(Document document) {
        Element handlersElement = (Element) document.getElementsByTagNameNS(Constants.SYNAPSE_API_XMLNS, Constants.SYNAPSE_API_ELEMENT_HANDLERS).item(0);

        NodeList handlerNodes = handlersElement.getElementsByTagName(Constants.SYNAPSE_API_ELEMENT_HANDLER);

        for (int i = 0; i < handlerNodes.getLength(); ++i) {
            Element handler = (Element) handlerNodes.item(i);

            String className = handler.getAttribute(Constants.SYNAPSE_API_ATTRIBUTE_CLASS);

            if (Constants.SYNAPSE_API_VALUE_CORS_HANDLER.equals(className)) {
                handlersElement.removeChild(handler);
                break;
            }
        }

        // Find the inSequence
        Element inSequenceElement = (Element) document.getElementsByTagName(Constants.SYNAPSE_API_ELEMENT_INSEQUENCE).item(0);

        NodeList sendElements = inSequenceElement.getElementsByTagName(Constants.SYNAPSE_API_ELEMENT_SEND);

        Element corsHandler = document.createElementNS(Constants.SYNAPSE_API_XMLNS, Constants.SYNAPSE_API_ELEMENT_HANDLER);
        corsHandler.setAttribute(Constants.SYNAPSE_API_ATTRIBUTE_CLASS, Constants.SYNAPSE_API_VALUE_CORS_HANDLER);
        Element property = document.createElementNS(Constants.SYNAPSE_API_XMLNS, Constants.SYNAPSE_API_ELEMENT_PROPERTY);
        property.setAttribute(Constants.SYNAPSE_API_ATTRIBUTE_NAME, Constants.SYNAPSE_API_VALUE_INLINE);

        if (0 < sendElements.getLength()) {
            property.setAttribute(Constants.SYNAPSE_API_ATTRIBUTE_VALUE, Constants.SYNAPSE_API_VALUE_ENPOINT);
        }
        else {
            property.setAttribute(Constants.SYNAPSE_API_ATTRIBUTE_VALUE, Constants.SYNAPSE_API_VALUE_INLINE_UPPERCASE);
        }

        corsHandler.appendChild(property);

        handlersElement.insertBefore(corsHandler, handlersElement.getFirstChild());

    }

    private static void updateResources(Document document) throws APIMigrationException {
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
        Element outSequenceElement = (Element) resourceElement.getElementsByTagNameNS(Constants.SYNAPSE_API_XMLNS, Constants.SYNAPSE_API_ELEMENT_OUTSEQUENCE).item(0);

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

    public static String getResourceContent(Object content) {
        return new String((byte[]) content, Charset.defaultCharset());
    }

    public static Document buildDocument(String xmlContent, String fileName) throws APIMigrationException {
        Document doc = null;
        try {
            DocumentBuilder docBuilder = getDocumentBuilder(fileName);
            doc = docBuilder.parse(new InputSource(new ByteArrayInputStream(xmlContent.getBytes(Charset.defaultCharset()))));
            doc.getDocumentElement().normalize();
        } catch (SAXException e) {
            ResourceUtil.handleException("Error occurred while parsing the " + fileName + " xml document", e);
        } catch (IOException e) {
            ResourceUtil.handleException("Error occurred while reading the " + fileName + " xml document", e);
        }

        return doc;
    }


    public static Document buildDocument(InputStream inputStream, String fileName) throws APIMigrationException {
        Document doc = null;
        try {
            DocumentBuilder docBuilder = getDocumentBuilder(fileName);
            doc = docBuilder.parse(new InputSource(inputStream));
            doc.getDocumentElement().normalize();
        } catch (SAXException e) {
            ResourceUtil.handleException("Error occurred while parsing the " + fileName + " xml document", e);
        } catch (IOException e) {
            ResourceUtil.handleException("Error occurred while reading the " + fileName + " xml document", e);
        }

        return doc;
    }

    public static Document buildDocument(File file, String fileName) throws APIMigrationException {
        Document doc = null;
        try {
            DocumentBuilder docBuilder = getDocumentBuilder(fileName);
            doc = docBuilder.parse(file);
            doc.getDocumentElement().normalize();
        } catch (SAXException e) {
            ResourceUtil.handleException("Error occurred while parsing the " + fileName + " xml document", e);
        } catch (IOException e) {
            ResourceUtil.handleException("Error occurred while reading the " + fileName + " xml document", e);
        }

        return doc;
    }

    private static DocumentBuilder getDocumentBuilder(String fileName) throws APIMigrationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = null;
        try {
            docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            ResourceUtil.handleException("Error occurred while trying to build the " + fileName + " xml document", e);
        }

        return docBuilder;
    }

    public static void transformXMLDocument(Document document, File file) {
        document.getDocumentElement().normalize();
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, Charset.defaultCharset().toString());
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(document), new StreamResult(file));
        } catch (TransformerConfigurationException e) {
            log.error("Transformer configuration error encountered while transforming file " + file.getName(), e);
        } catch (TransformerException e) {
            log.error("Transformer error encountered while transforming file " + file.getName(), e);
        }
    }


    public static String getApiPath(int tenantID, String tenantDomain) {
        log.debug("Get api synapse files for tenant " + tenantID + '(' + tenantDomain + ')');
        String apiFilePath;
        if (tenantID != MultitenantConstants.SUPER_TENANT_ID) {
            apiFilePath = CarbonUtils.getCarbonTenantsDirPath() + File.separatorChar + tenantID +
                    File.separatorChar + "synapse-configs" + File.separatorChar + "default" + File.separatorChar + "api";
        } else {
            apiFilePath = CarbonUtils.getCarbonRepository() + "synapse-configs" + File.separatorChar +
                    "default" + File.separatorChar  +"api";
        }
        log.debug("Path of api folder " + apiFilePath);

        return apiFilePath;
    }


    public static List<SynapseDTO> getVersionedAPIs(String apiFilePath) {
        File apiFiles = new File(apiFilePath);
        File[] files = apiFiles.listFiles();
        List<SynapseDTO> versionedAPIs = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                try {
                    Document doc = buildDocument(file, file.getName());
                    Element rootElement = doc.getDocumentElement();

                    // Ensure that we skip internal apis such as '_TokenAPI_.xml' and apis
                    // that represent default versions
                    if (Constants.SYNAPSE_API_ROOT_ELEMENT.equals(rootElement.getNodeName()) &&
                            rootElement.hasAttribute(Constants.SYNAPSE_API_ATTRIBUTE_VERSION)) {
                        SynapseDTO synapseConfig = new SynapseDTO(doc, file);
                        versionedAPIs.add(synapseConfig);
                    }
                } catch (APIMigrationException e) {
                    log.error("Error when passing file " + file.getName(), e);
                }
            }
        }

        return versionedAPIs;
    }


}
