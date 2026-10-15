/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.After;
import io.cucumber.java.en.When;
import org.testng.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.testcontainers.ApimRuntime;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/** Installs and removes legacy-style static Synapse API routes used by gateway parity scenarios. */
public class StaticSynapseRouteSteps {

    private static final String SYNAPSE_NAMESPACE = "http://ws.apache.org/ns/synapse";
    private static final String CONFIG_NAMESPACE = "http://org.apache.synapse/xsd";
    private static final String LOCATION_HEADER_ROUTE_NAME = "v2-location-header-static-route";
    private static final String ROUTE_CONTEXT_KEY = "staticSynapseRouteContext";
    private static final String ROUTE_NAME_KEY = "staticSynapseRouteName";

    @When("I install the legacy static URL-mapping Synapse route at context {string}")
    public void installLegacyStaticUrlMappingRoute(String context) throws Exception {

        String routeName = urlMappingRouteName(context);
        installRoute(context, routeName, urlMappingRouteXml(context, routeName));
    }

    @When("I install the legacy static Location-header Synapse route at context {string}")
    public void installLegacyStaticLocationHeaderRoute(String context) throws Exception {

        String routeName = locationHeaderRouteName(context);
        installRoute(context, routeName, locationHeaderRouteXml(context, routeName));
    }

    private static void installRoute(String context, String routeName, String routeXml) throws Exception {

        Object runtimeValue = TestContext.get("blockApimContainer");
        Assert.assertTrue(runtimeValue instanceof ApimRuntime,
                "No APIM runtime is available for the static Synapse route installation");
        ApimRuntime runtime = (ApimRuntime) runtimeValue;
        User admin = Identity.actingTenantAdmin();
        String serviceUrl = runtime.getGatewayManagementHttpsUrl() + "services/ConfigServiceAdmin";
        String configuration = getConfiguration(serviceUrl, admin);
        Document document = parse(configuration);
        Assert.assertNull(findRoute(document, context),
                "Refusing to replace an existing Synapse API at context " + context);

        // Register before the update so the @After hook can remove the route even if the SOAP call fails late.
        TestContext.set(ROUTE_CONTEXT_KEY, context);
        TestContext.set(ROUTE_NAME_KEY, routeName);
        Element route = parse(routeXml).getDocumentElement();
        document.getDocumentElement().appendChild(document.importNode(route, true));
        Assert.assertTrue(updateConfiguration(serviceUrl, admin, serialize(document)),
                "ConfigServiceAdmin rejected static Synapse route " + routeName + " at " + context);
    }

    @After("@static-synapse-route")
    public void removeLegacyStaticSynapseRoute() throws Exception {

        Object routeContextValue = TestContext.get(ROUTE_CONTEXT_KEY);
        Object routeNameValue = TestContext.get(ROUTE_NAME_KEY);
        if (!(routeContextValue instanceof String routeContext) || !(routeNameValue instanceof String routeName)) {
            return;
        }
        try {
            Object runtimeValue = TestContext.get("blockApimContainer");
            Assert.assertTrue(runtimeValue instanceof ApimRuntime,
                    "No APIM runtime is available to remove the static Synapse route");
            ApimRuntime runtime = (ApimRuntime) runtimeValue;
            User admin = Identity.actingTenantAdmin();
            String serviceUrl = runtime.getGatewayManagementHttpsUrl() + "services/ConfigServiceAdmin";
            Document document = parse(getConfiguration(serviceUrl, admin));
            Element route = findRoute(document, routeContext);
            if (route != null && routeName.equals(route.getAttribute("name"))) {
                route.getParentNode().removeChild(route);
                Assert.assertTrue(updateConfiguration(serviceUrl, admin, serialize(document)),
                        "ConfigServiceAdmin rejected cleanup of the static route at " + routeContext);
            }
        } finally {
            TestContext.remove(ROUTE_CONTEXT_KEY);
            TestContext.remove(ROUTE_NAME_KEY);
        }
    }

    private static Element findRoute(Document document, String context) {

        var routes = document.getElementsByTagNameNS(SYNAPSE_NAMESPACE, "api");
        for (int index = 0; index < routes.getLength(); index++) {
            Element route = (Element) routes.item(index);
            if (context.equals(route.getAttribute("context"))) {
                return route;
            }
        }
        return null;
    }

    private static String urlMappingRouteName(String context) {

        if (context.startsWith("/t/")) {
            int tenantDomainEnd = context.indexOf('/', "/t/".length());
            Assert.assertTrue(tenantDomainEnd > "/t/".length(),
                    "Tenant static route context has no tenant domain: " + context);
            return "admin-AT-" + context.substring("/t/".length(), tenantDomainEnd) + "--urlmapping";
        }
        return "admin--urlmapping";
    }

    private static String locationHeaderRouteName(String context) {

        if (context.startsWith("/t/")) {
            int tenantDomainEnd = context.indexOf('/', "/t/".length());
            Assert.assertTrue(tenantDomainEnd > "/t/".length(),
                    "Tenant static route context has no tenant domain: " + context);
            return "admin-AT-" + context.substring("/t/".length(), tenantDomainEnd) + "--"
                    + LOCATION_HEADER_ROUTE_NAME;
        }
        return LOCATION_HEADER_ROUTE_NAME;
    }

    private static String urlMappingRouteXml(String context, String routeName) {

        return "<api xmlns=\"" + SYNAPSE_NAMESPACE + "\" name=\"" + routeName
                + "\" context=\"" + context + "\" version=\"1.0.0\" version-type=\"context\">"
                + "<resource methods=\"GET\" uri-template=\"/test\">"
                + "<inSequence><header name=\"To\" action=\"remove\"/>"
                + "<property name=\"RESPONSE\" value=\"true\"/>"
                + "<property name=\"NO_ENTITY_BODY\" scope=\"axis2\" action=\"remove\"/>"
                + "<payloadFactory><format><getQuoteResponse xmlns=\"http://services.samples\">"
                + "<return xmlns=\"http://services.samples/xsd\"><symbol>IBM</symbol></return>"
                + "</getQuoteResponse></format></payloadFactory><send/></inSequence>"
                + "<outSequence><property name=\"Content-Type\" value=\"application/xml\" scope=\"transport\"/>"
                + "<send/></outSequence></resource></api>";
    }

    private static String locationHeaderRouteXml(String context, String routeName) {

        return "<api xmlns=\"" + SYNAPSE_NAMESPACE + "\" name=\"" + routeName
                + "\" context=\"" + context + "\">"
                + "<resource methods=\"GET\" url-mapping=\"/*\"><inSequence>"
                + "<property name=\"NO_ENTITY_BODY\" action=\"remove\" scope=\"axis2\"/>"
                + "<payloadFactory><format><response xmlns=\"\"><value>Received Request</value></response></format>"
                + "</payloadFactory><property name=\"RESPONSE\" value=\"true\" scope=\"default\" type=\"STRING\"/>"
                + "<property name=\"HTTP_SC\" value=\"200\" scope=\"axis2\"/>"
                + "<property name=\"Location\" value=\"http://google.lk/abc/domain\" scope=\"transport\"/>"
                + "<header name=\"To\" action=\"remove\"/><send/></inSequence></resource></api>";
    }

    private static String getConfiguration(String serviceUrl, User admin) throws Exception {

        String response = sendSoap(serviceUrl, admin, "urn:getConfiguration",
                "<ns:getConfiguration xmlns:ns=\"" + CONFIG_NAMESPACE + "\"/>");
        Document document = parse(response);
        var result = document.getElementsByTagNameNS("*", "return");
        Assert.assertTrue(result.getLength() > 0, "ConfigServiceAdmin returned no configuration");
        return result.item(0).getTextContent();
    }

    private static boolean updateConfiguration(String serviceUrl, User admin, String configuration) throws Exception {

        String response = sendSoap(serviceUrl, admin, "urn:updateConfiguration",
                "<ns:updateConfiguration xmlns:ns=\"" + CONFIG_NAMESPACE + "\">" + configuration
                        + "</ns:updateConfiguration>");
        Document document = parse(response);
        var result = document.getElementsByTagNameNS("*", "return");
        return result.getLength() > 0 && Boolean.parseBoolean(result.item(0).getTextContent().trim());
    }

    private static String sendSoap(String serviceUrl, User admin, String soapAction, String operation) throws Exception {

        String envelope = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soapenv:Header/><soapenv:Body>" + operation + "</soapenv:Body></soapenv:Envelope>";
        String credentials = admin.getUserName() + ":" + admin.getPassword();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8)));
        headers.put("SOAPAction", soapAction);
        HttpResponse response = SimpleHTTPClient.getInstance().doPost(serviceUrl, headers, envelope,
                "text/xml;charset=UTF-8");
        Assert.assertNotNull(response, "ConfigServiceAdmin " + soapAction + " returned no HTTP response");
        Assert.assertEquals(response.getResponseCode(), 200,
                "ConfigServiceAdmin " + soapAction + " failed: " + response.getData());
        Document responseDocument = parse(response.getData());
        Assert.assertEquals(responseDocument.getElementsByTagNameNS("*", "Fault").getLength(), 0,
                "ConfigServiceAdmin returned a SOAP fault: " + response.getData());
        return response.getData();
    }

    private static Document parse(String xml) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private static String serialize(Document document) throws Exception {

        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        var transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }
}
