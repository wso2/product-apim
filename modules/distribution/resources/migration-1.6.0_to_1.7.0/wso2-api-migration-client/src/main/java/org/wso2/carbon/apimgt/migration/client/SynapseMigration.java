package org.wso2.carbon.apimgt.migration.client;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SynapseMigration {
    private static final Log log = LogFactory.getLog(SynapseMigration.class);
    /**
     * modify the apis in the sysnpase-config folder
     *
     * @param configPath
     *            path to synapse-config api foldermodifySuperUserAPIs
     * @throws Exception
     */
    public void modifySynapseConfigs(String configPath) {
        File configFolder = new File(configPath);
        if (configFolder.isDirectory()) {
            File[] apis = configFolder.listFiles();
            for (int i = 0; i < apis.length; i++) {
                // take only the apis created by the user
                if (apis[i].getName().contains("_v")) {
                    try {
                        modifyGoogleAnalyticsTrackingHandler(apis[i]);
                    } catch (IOException e) {
                        log.error("Error while accessing api configuration file.", e);
                    } catch (Exception e) {
                        log.error("Error while migrating.", e);
                    }
                }
            }
        } else {
            log.error("Provided synapse api path " + configPath + " is not a valid directory");
        }
    }

    /**
     * method to add property element to the existing google analytics tracking
     * handler
     *
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws XPathExpressionException
     * @throws TransformerException
     */
    private void modifyGoogleAnalyticsTrackingHandler(File api)
            throws ParserConfigurationException, SAXException, IOException,
            XPathExpressionException, TransformerException {

        FileInputStream file = new FileInputStream(api);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory
                .newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();

        Document xmlDocument = builder.parse(file);

        // current handler element
        String expression = "//handler[@class='org.wso2.carbon.apimgt.usage.publisher."
                + "APIMgtGoogleAnalyticsTrackingHandler']";
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(
                xmlDocument, XPathConstants.NODESET);
        Element oldHandlerElem = (Element) nodeList.item(0);

		/*
		 * Replace
		 * <handler class="org.wso2.carbon.apimgt.usage.publisher.APIMgtGoogleAnalyticsTrackingHandler"/>
		 * with
		 * <handler class="org.wso2.carbon.apimgt.usage.publisher.APIMgtGoogleAnalyticsTrackingHandler" >
		 *   <property name="configKey" value="gov:/apimgt/statistics/ga-config.xml"/>
		 * </handler>
		 */

        if (oldHandlerElem != null) {
            // new handler to replace the old one
            Element newHandlerElem = xmlDocument.createElement("handler");
            newHandlerElem.setAttribute("class",
                    "org.wso2.carbon.apimgt.usage.publisher."
                            + "APIMgtGoogleAnalyticsTrackingHandler");
            // child element for the handler
            Element propertyElem = xmlDocument.createElement("property");
            propertyElem.setAttribute("name", "configKey");
            propertyElem.setAttribute("value",
                    "gov:/apimgt/statistics/ga-config.xml");

            newHandlerElem.appendChild(propertyElem);

            Element root = xmlDocument.getDocumentElement();
            NodeList handlersNodelist = root.getElementsByTagName("handlers");
            Element handlers = (Element) handlersNodelist.item(0);
            // replace old handler with the new one
            handlers.replaceChild(newHandlerElem, oldHandlerElem);

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory
                    .newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(xmlDocument);
            StreamResult result = new StreamResult(api);
            transformer.transform(source, result);
            System.out.println("Updated api: " + api.getName());
        }

    }
}
