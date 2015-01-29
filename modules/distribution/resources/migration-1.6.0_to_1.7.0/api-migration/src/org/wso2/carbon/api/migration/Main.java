/*
 *  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.api.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 
 * Application to modify the Google Analytics Tracking handler element in each
 * api in the synapse-config folder.
 * 
 */
public class Main {

	private static File[] apis;
	private static String apimHome;

	public static void main(String[] args) {
		if (args.length > 0) {
			apimHome = args[0];
			modifySuperUserAPIs();
			modifyTenantAPIs();

		} else {
			System.out.println("Missing API manager home argument");			
		}
	}

	/**
	 * modify apis related to carbon.super
	 */
	private static void modifySuperUserAPIs() {
		System.out.println("Modify carbon user apis..");
		String configPath = apimHome + File.separator + "repository"
				+ File.separator + "deployment" + File.separator + "server"
				+ File.separator + "synapse-configs" + File.separator
				+ "default" + File.separator + "api";	
		modifySynapseConfigs(configPath);

	}

	/**
	 * modify apis related to tenants
	 */
	private static void modifyTenantAPIs() {
		System.out.println("Modify tenant apis..");
		String tenantLocation = apimHome + File.separator + "repository"
				+ File.separator + "tenants";
		File tenantsDir = new File(tenantLocation);
		if (tenantsDir.exists()) {
			File[] tenants = tenantsDir.listFiles();
			for (File file : tenants) {
				String configLocation = file.getAbsolutePath() + File.separator
						+ "synapse-configs" + File.separator + "default"
						+ File.separator + "api";
					modifySynapseConfigs(configLocation);				
			}
		}
	}

	/**
	 * modify the apis in the sysnpase-config folder
	 * 
	 * @param configPath
	 *            path to synapse-config api foldermodifySuperUserAPIs
	 * @throws Exception 
	 */
	private static void modifySynapseConfigs(String configPath) {

		File configFolder = new File(configPath);
		if (configFolder.isDirectory()) {
			apis = configFolder.listFiles();
			for (int i = 0; i < apis.length; i++) {
				// take only the apis created by the user
				if (apis[i].getName().contains("_v")) {
					try {
						modifyGoogleAnalyticsTrackingHandler(apis[i]);
					} catch (IOException e) {
						System.out
								.println("Error while accessing api configuration file. "
										+ e);
					} catch (Exception e) {
						System.out.println("Error while migrating. " + e);
					}
				}
			}
		} else {
			System.err
					.println("API Manager home is not set properly. Please check the build.xml "
							+ "file");			
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
	private static void modifyGoogleAnalyticsTrackingHandler(File api)
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
