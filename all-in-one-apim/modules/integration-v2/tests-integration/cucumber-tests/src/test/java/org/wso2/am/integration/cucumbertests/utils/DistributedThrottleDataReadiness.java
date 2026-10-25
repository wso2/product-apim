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
import org.wso2.am.testcontainers.DistributedApimTomlBuilder.Component;
import org.wso2.am.testcontainers.DistributedDynamicApimContainer;

import java.io.IOException;
import java.util.regex.Pattern;

/** Read-only readiness of the distinct CP event-hub and TM throttling paths. */
final class DistributedThrottleDataReadiness {

    private static final String PUBLISHER_DIRECTORY = "repository/deployment/server/eventpublishers/";

    private DistributedThrottleDataReadiness() {
    }

    static ThrottleDataReadiness.Result inspect(DistributedDynamicApimContainer runtime, String cpLog,
                                                String tmLog, String gatewayLog) {
        var gatewayAddresses = runtime.getGatewayBrokerClientAddresses();
        BrokerSubscriptionReadiness.Result cpConsumer = BrokerSubscriptionReadiness.inspect(
                runtime.getServletHttpsUrl(), "throttleData", gatewayAddresses);
        BrokerSubscriptionReadiness.Result tmConsumer = BrokerSubscriptionReadiness.inspect(
                runtime.getTrafficManagerManagementHttpsUrl(), "throttleData", gatewayAddresses);
        BrokerSubscriptionReadiness.Result policyConsumer = BrokerSubscriptionReadiness.inspect(
                runtime.getServletHttpsUrl(), "notification", runtime.getTrafficManagerBrokerClientAddresses());
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
        return new ThrottleDataReadiness.Result(registryHealthy && blockingPublisher.ready() && policyConsumer.ready(),
                throttlePublisher.ready() && throttlePublisherV11.ready(),
                cpConsumer.ready() && tmConsumer.ready(), false,
                "distributed throttle-data: registryHealthy=" + registryHealthy + " gatewayAddresses=" + gatewayAddresses
                        + "\nCP blocking publisher: " + blockingPublisher.diagnostics()
                        + "\nTM primary publisher: " + throttlePublisher.diagnostics()
                        + "\nTM stream 1.1 publisher: " + throttlePublisherV11.diagnostics()
                        + "\nCP Traffic Manager policy subscription: " + policyConsumer.diagnostics()
                        + "\nCP Gateway subscription: " + cpConsumer.diagnostics()
                        + "\nTM Gateway subscription: " + tmConsumer.diagnostics());
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
        return ReadinessXml.parse(xml);
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

    private record Check(boolean ready, String diagnostics) {
    }
}
