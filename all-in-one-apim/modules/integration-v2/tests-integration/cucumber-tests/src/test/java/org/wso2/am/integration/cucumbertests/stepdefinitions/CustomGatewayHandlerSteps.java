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

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Assertions and isolated runtime configuration for the custom-handler scenarios. */
public class CustomGatewayHandlerSteps {

    private static final String HANDLER_CLASS = "org.test.apim.coustom.handler.CustomAPIAuthenticationHandler";
    private static final String SYNAPSE_NAMESPACE = "http://ws.apache.org/ns/synapse";
    private static final String SYNAPSE_CONFIG_NAMESPACE = "http://org.apache.synapse/xsd";
    private static final long SOAP_CONFIG_TIMEOUT_MILLIS = 60_000L;

    @When("I attach the custom gateway handler to API context {string} through ConfigServiceAdmin")
    public void attachCustomGatewayHandlerThroughConfigServiceAdmin(String apiContext) throws Exception {

        String resolvedApiContext = Utils.resolveContextPlaceholders(apiContext);
        Object runtimeValue = TestContext.get("blockApimContainer");
        Assert.assertTrue(runtimeValue instanceof ApimRuntime,
                "No APIM runtime is available for the isolated ConfigServiceAdmin update");
        ApimRuntime runtime = (ApimRuntime) runtimeValue;
        User admin = Identity.gatewayManagementAdmin();
        String serviceUrl = runtime.getGatewayManagementHttpsUrl() + "services/ConfigServiceAdmin";

        String currentConfiguration = getConfiguration(serviceUrl, admin);
        org.w3c.dom.Document document = parseConfiguration(currentConfiguration);
        org.w3c.dom.Element targetApi = findApiByContext(document, resolvedApiContext);
        org.w3c.dom.Element handlers = directChild(targetApi, "handlers");
        Assert.assertNotNull(handlers, "Target API has no handlers element in live Synapse configuration: "
                + resolvedApiContext);
        Assert.assertEquals(countHandler(handlers), 0,
                "The runtime-SOAP API unexpectedly already has the custom handler: " + resolvedApiContext);

        org.w3c.dom.Element handler = document.createElementNS(SYNAPSE_NAMESPACE, "handler");
        handler.setAttribute("class", HANDLER_CLASS);
        handlers.insertBefore(handler, handlers.getFirstChild());
        Assert.assertTrue(updateConfiguration(serviceUrl, admin, serialize(document)),
                "ConfigServiceAdmin rejected the scoped handler update for API context " + resolvedApiContext);

        long deadline = System.currentTimeMillis() + SOAP_CONFIG_TIMEOUT_MILLIS;
        String lastConfiguration = null;
        while (System.currentTimeMillis() < deadline) {
            lastConfiguration = getConfiguration(serviceUrl, admin);
            org.w3c.dom.Document observed = parseConfiguration(lastConfiguration);
            org.w3c.dom.Element observedApi = findApiByContext(observed, resolvedApiContext);
            if (countHandler(directChild(observedApi, "handlers")) == 1) {
                return;
            }
            Thread.sleep(500);
        }
        Assert.fail("ConfigServiceAdmin update did not converge for API context " + resolvedApiContext
                + " within " + SOAP_CONFIG_TIMEOUT_MILLIS + "ms. Last configuration length: "
                + (lastConfiguration == null ? "null" : lastConfiguration.length()));
    }

    private static String getConfiguration(String serviceUrl, User admin) throws Exception {

        String response = sendConfigAdminSoap(serviceUrl, admin, "urn:getConfiguration",
                "<ns:getConfiguration xmlns:ns=\"" + SYNAPSE_CONFIG_NAMESPACE + "\"/>");
        org.w3c.dom.Document document = parseConfiguration(response);
        org.w3c.dom.Element result = firstElementByLocalName(document, "return");
        Assert.assertNotNull(result, "ConfigServiceAdmin getConfiguration returned no result element");
        return result.getTextContent();
    }

    private static boolean updateConfiguration(String serviceUrl, User admin, String configuration) throws Exception {

        String operation = "<ns:updateConfiguration xmlns:ns=\"" + SYNAPSE_CONFIG_NAMESPACE + "\">"
                + configuration + "</ns:updateConfiguration>";
        String response = sendConfigAdminSoap(serviceUrl, admin, "urn:updateConfiguration", operation);
        org.w3c.dom.Document document = parseConfiguration(response);
        org.w3c.dom.Element result = firstElementByLocalName(document, "return");
        return result != null && Boolean.parseBoolean(result.getTextContent().trim());
    }

    private static String sendConfigAdminSoap(String serviceUrl, User admin, String soapAction, String operation)
            throws Exception {

        String envelope = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soapenv:Header/><soapenv:Body>" + operation + "</soapenv:Body></soapenv:Envelope>";
        String credentials = admin.getUserName() + ":" + admin.getPassword();
        String authorization = "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", authorization);
        headers.put("SOAPAction", soapAction);
        HttpResponse response = SimpleHTTPClient.getInstance().doPost(serviceUrl, headers, envelope,
                "text/xml;charset=UTF-8");
        Assert.assertNotNull(response, "ConfigServiceAdmin " + soapAction + " returned no HTTP response");
        Assert.assertEquals(response.getResponseCode(), 200, "ConfigServiceAdmin " + soapAction
                + " failed: " + response.getData());
        org.w3c.dom.Document responseDocument = parseConfiguration(response.getData());
        Assert.assertNull(firstElementByLocalName(responseDocument, "Fault"),
                "ConfigServiceAdmin " + soapAction + " returned a SOAP fault: " + response.getData());
        return response.getData();
    }

    @Then("The custom handler response body should be exactly {string}")
    public void customHandlerResponseBodyShouldBeExactly(String expected) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertNotNull(response, "No gateway response is available for the custom-handler assertion");
        Assert.assertEquals(response.getData(), Utils.resolveContextPlaceholders(expected),
                "The backend did not receive the response marker set by the per-API custom handler");
    }

    private static org.w3c.dom.Document parseConfiguration(String xml) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private static String serialize(org.w3c.dom.Document document) throws Exception {

        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        var transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    private static org.w3c.dom.Element findApiByContext(org.w3c.dom.Document document, String context) {

        String expected = normalizeContext(context);
        List<org.w3c.dom.Element> matches = new ArrayList<>();
        var apis = document.getElementsByTagNameNS(SYNAPSE_NAMESPACE, "api");
        for (int i = 0; i < apis.getLength(); i++) {
            org.w3c.dom.Element api = (org.w3c.dom.Element) apis.item(i);
            String apiContext = normalizeContext(api.getAttribute("context"));
            if (expected.equals(apiContext) || isVersionedContext(expected, apiContext)) {
                matches.add(api);
            }
        }
        Assert.assertEquals(matches.size(), 1, "Expected exactly one live Synapse API for context " + context
                + "; found " + matches.size() + ". Available contexts: " + apiContexts(apis));
        return matches.get(0);
    }

    private static boolean isVersionedContext(String expected, String actual) {

        String versionPrefix = expected + "/";
        if (!actual.startsWith(versionPrefix)) {
            return false;
        }
        String version = actual.substring(versionPrefix.length());
        return !version.isBlank() && !version.contains("/");
    }

    private static List<String> apiContexts(org.w3c.dom.NodeList apis) {

        List<String> contexts = new ArrayList<>();
        for (int i = 0; i < apis.getLength(); i++) {
            org.w3c.dom.Element api = (org.w3c.dom.Element) apis.item(i);
            contexts.add(api.getAttribute("context"));
        }
        return contexts;
    }

    private static String normalizeContext(String context) {

        String normalized = context == null ? "" : context.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }


    private static org.w3c.dom.Element firstElementByLocalName(org.w3c.dom.Document document, String localName) {

        var elements = document.getElementsByTagNameNS("*", localName);
        return elements.getLength() == 0 ? null : (org.w3c.dom.Element) elements.item(0);
    }

    private static org.w3c.dom.Element directChild(org.w3c.dom.Element parent, String localName) {

        if (parent == null) {
            return null;
        }
        for (org.w3c.dom.Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof org.w3c.dom.Element element && localName.equals(element.getLocalName())) {
                return element;
            }
        }
        return null;
    }

    private static int countHandler(org.w3c.dom.Element handlers) {

        if (handlers == null) {
            return 0;
        }
        int count = 0;
        for (org.w3c.dom.Node child = handlers.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof org.w3c.dom.Element element && "handler".equals(element.getLocalName())
                    && HANDLER_CLASS.equals(element.getAttribute("class"))) {
                count++;
            }
        }
        return count;
    }
}
