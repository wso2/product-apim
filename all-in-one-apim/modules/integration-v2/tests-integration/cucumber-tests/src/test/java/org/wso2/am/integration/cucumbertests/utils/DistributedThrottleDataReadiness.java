/*
 * Copyright (c) 2026, WSO2 LLC. (http://wso2.com) All Rights Reserved.
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
package org.wso2.am.integration.cucumbertests.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.testcontainers.DistributedApimTomlBuilder.Component;
import org.wso2.am.testcontainers.DistributedDynamicApimContainer;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

/** Read-only readiness of the distinct CP event-hub and TM throttling paths. */
final class DistributedThrottleDataReadiness {

    private static final String PUBLISHER_DIRECTORY = "repository/deployment/server/eventpublishers/";
    private static final String SUBSCRIPTIONS_ENVELOPE = "<soapenv:Envelope "
            + "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
            + "xmlns:a=\"http://wso2.org/carbon/andes/admin/xsd\"><soapenv:Body>"
            + "<a:getSubscriptions><a:isDurable>false</a:isDurable><a:isActive>true</a:isActive>"
            + "<a:protocolType>AMQP</a:protocolType><a:destinationType>TOPIC</a:destinationType>"
            + "</a:getSubscriptions></soapenv:Body></soapenv:Envelope>";

    private DistributedThrottleDataReadiness() {
    }

    static ThrottleDataReadiness.Result inspect(DistributedDynamicApimContainer runtime, String cpLog,
                                                String tmLog, String gatewayLog) {
        String gatewayAddress = runtime.getGatewayBrokerClientAddress();
        User actor = Identity.resolveActor("admin");
        Check cpConsumer = inspectBroker(runtime.getServletHttpsUrl(), actor, gatewayAddress);
        Check tmConsumer = inspectBroker(runtime.getTrafficManagerManagementHttpsUrl(), actor, gatewayAddress);
        Check blockingPublisher = inspectPublisher(runtime, Component.CP, "blockingEventJMSPublisher.xml",
                "blockingEventJMSPublisher", "org.wso2.blocking.request.stream", "1.0.0", cpLog);
        Check throttlePublisher = inspectPublisher(runtime, Component.TM, "jmsEventPublisher.xml",
                "jmsEventPublisher", "org.wso2.throttle.globalThrottle.stream", "1.0.0", tmLog);
        Check throttlePublisherV11 = inspectPublisher(runtime, Component.TM, "jmsEventPublisher_1.10.0.xml",
                "jmsEventPublisher-1.0.0", "org.wso2.throttle.globalThrottle.stream", "1.1.0", tmLog);

        // A successful later probe must not hide a corrupt initial start. No recovery is performed here.
        boolean registryHealthy = !cpLog.contains("REG_RESOURCE_FK_BY_PATH_ID")
                && !tmLog.contains("REG_RESOURCE_FK_BY_PATH_ID")
                && !gatewayLog.contains("REG_RESOURCE_FK_BY_PATH_ID");
        return new ThrottleDataReadiness.Result(registryHealthy && blockingPublisher.ready(),
                throttlePublisher.ready() && throttlePublisherV11.ready(),
                cpConsumer.ready() && tmConsumer.ready(), false,
                "distributed throttle-data: registryHealthy=" + registryHealthy + " gatewayAddress=" + gatewayAddress
                        + "\nCP blocking publisher: " + blockingPublisher.diagnostics()
                        + "\nTM primary publisher: " + throttlePublisher.diagnostics()
                        + "\nTM stream 1.1 publisher: " + throttlePublisherV11.diagnostics()
                        + "\nCP Gateway subscription: " + cpConsumer.diagnostics()
                        + "\nTM Gateway subscription: " + tmConsumer.diagnostics());
    }

    private static Check inspectBroker(String baseUrl, User actor, String gatewayAddress) {
        try {
            SimpleHTTPClient client = SimpleHTTPClient.getInstance();
            HttpResponse response = client.withSocketTimeout(10000, () -> client.sendSoapRequest(
                    baseUrl + "services/AndesAdminService", SUBSCRIPTIONS_ENVELOPE, "urn:getSubscriptions",
                    actor.getUserName(), actor.getPassword()));
            if (response == null || response.getResponseCode() != 200) {
                return new Check(false, response == null ? "no response" : "HTTP " + response.getResponseCode()
                        + " " + abbreviate(response.getData()));
            }
            Document document = parseXml(response.getData());
            if (document.getElementsByTagNameNS("*", "getSubscriptionsResponse").getLength() != 1
                    || document.getElementsByTagNameNS("*", "Fault").getLength() != 0) {
                return new Check(false, "unexpected getSubscriptions response: " + abbreviate(response.getData()));
            }
            NodeList subscriptions = document.getElementsByTagNameNS("*", "return");
            List<String> activeOrigins = new ArrayList<>();
            for (int i = 0; i < subscriptions.getLength(); i++) {
                Element subscription = (Element) subscriptions.item(i);
                // The shipped MBean ignores its isActive argument and merges active/inactive sets.
                if (!"true".equals(childText(subscription, "active"))
                        || !"throttledata".equalsIgnoreCase(childText(subscription, "subscribedQueueOrTopicName"))
                        || !"AMQP".equals(childText(subscription, "protocolType"))
                        || !"TOPIC".equals(childText(subscription, "destinationType"))) {
                    continue;
                }
                activeOrigins.add(childText(subscription, "originHostAddress"));
            }
            boolean ready = activeOrigins.stream().anyMatch(origin -> origin.startsWith("/" + gatewayAddress + ":")
                    || origin.startsWith("/[" + gatewayAddress + "]:"));
            return new Check(ready, "active throttledata origins=" + activeOrigins
                    + ", expected Gateway=" + gatewayAddress);
        } catch (IOException e) {
            return new Check(false, e.getMessage());
        }
    }

    private static Check inspectPublisher(DistributedDynamicApimContainer runtime, Component component,
                                           String fileName, String name, String streamName, String streamVersion,
                                           String currentLog) {
        try {
            Element publisher = parseXml(runtime.readComponentFile(component, PUBLISHER_DIRECTORY + fileName))
                    .getDocumentElement();
            String processing = publisher.getAttribute("processing");
            NodeList streams = publisher.getElementsByTagNameNS("*", "from");
            boolean configured = "eventPublisher".equals(publisher.getLocalName())
                    && name.equals(publisher.getAttribute("name"))
                    && (processing.isEmpty() || "enable".equals(processing)) && streams.getLength() == 1;
            if (configured) {
                Element stream = (Element) streams.item(0);
                configured = streamName.equals(stream.getAttribute("streamName"))
                        && streamVersion.equals(stream.getAttribute("version"));
            }
            NodeList adapters = publisher.getElementsByTagNameNS("*", "to");
            if (configured && adapters.getLength() == 1) {
                Element adapter = (Element) adapters.item(0);
                String jndi = component == Component.CP ? "jndi-cp.properties" : "jndi.properties";
                configured = "jms".equals(adapter.getAttribute("eventAdapterType"))
                        && "topic".equals(propertyText(adapter, "transport.jms.DestinationType"))
                        && "throttleData".equals(propertyText(adapter, "transport.jms.Destination"))
                        && ("repository/conf/" + jndi).equals(propertyText(adapter, "java.naming.provider.url"));
            } else {
                configured = false;
            }
            Pattern active = Pattern.compile("Event Publisher configuration successfully deployed and in active "
                    + "state\\s*:\\s*" + Pattern.quote(name) + "(?![\\w.-])");
            int deployed = lastMatch(currentLog, active);
            int undeployed = lastMatch(currentLog, Pattern.compile("Event Publisher undeployed successfully\\s*:\\s*"
                    + Pattern.quote(fileName) + "(?![\\w.-])"));
            boolean ready = configured && deployed >= 0 && deployed > undeployed;
            return new Check(ready, name + " enabledConfiguration=" + configured + " deployed=" + (deployed >= 0)
                    + " deploymentCurrent=" + (deployed > undeployed));
        } catch (IOException e) {
            return new Check(false, component + "/" + fileName + ": " + e.getMessage());
        }
    }

    private static int lastMatch(String text, Pattern pattern) {
        var matcher = pattern.matcher(text);
        int result = -1;
        while (matcher.find()) {
            result = matcher.start();
        }
        return result;
    }

    static Document parseXml(String xml) throws IOException {
        try {
            // The test classpath includes legacy Xerces, which lacks the JAXP external-access properties.
            DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IOException("Could not parse throttle-data readiness XML: " + e.getMessage(), e);
        }
    }

    private static String childText(Element parent, String name) {
        NodeList matches = parent.getElementsByTagNameNS("*", name);
        return matches.getLength() == 1 ? matches.item(0).getTextContent().trim() : "";
    }

    private static String propertyText(Element adapter, String name) {
        NodeList properties = adapter.getElementsByTagNameNS("*", "property");
        for (int i = 0; i < properties.getLength(); i++) {
            Element property = (Element) properties.item(i);
            if (name.equals(property.getAttribute("name"))) {
                return property.getTextContent().trim();
            }
        }
        return "";
    }

    private static String abbreviate(String value) {
        return value == null ? "no body" : value.substring(0, Math.min(value.length(), 1000));
    }

    private record Check(boolean ready, String diagnostics) {
    }
}
