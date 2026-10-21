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

package org.wso2.am.integration.cucumbertests.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Queries the broker's current non-durable AMQP topic subscriptions without creating or restarting consumers. */
final class BrokerSubscriptionReadiness {
    private static final String ENVELOPE = "<soapenv:Envelope "
            + "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
            + "xmlns:a=\"http://wso2.org/carbon/andes/admin/xsd\"><soapenv:Body>"
            + "<a:getSubscriptions><a:isDurable>false</a:isDurable><a:isActive>true</a:isActive>"
            + "<a:protocolType>AMQP</a:protocolType><a:destinationType>TOPIC</a:destinationType>"
            + "</a:getSubscriptions></soapenv:Body></soapenv:Envelope>";

    private BrokerSubscriptionReadiness() {
    }

    static Result inspect(String brokerBaseUrl, String topic, Set<String> consumerAddresses) {
        User actor = Identity.gatewayManagementAdmin();
        try {
            SimpleHTTPClient client = SimpleHTTPClient.getInstance();
            HttpResponse response = client.withSocketTimeout(10000, () -> client.sendSoapRequest(
                    brokerBaseUrl + "services/AndesAdminService", ENVELOPE, "urn:getSubscriptions",
                    actor.getUserName(), actor.getPassword()));
            Result result = inspectResponse(response == null ? 0 : response.getResponseCode(),
                    response == null ? null : response.getData(), topic, consumerAddresses);
            return new Result(result.ready(), "broker=" + brokerBaseUrl + " " + result.diagnostics());
        } catch (IOException e) {
            return new Result(false, "broker=" + brokerBaseUrl + " topic=" + topic + ": " + e.getMessage());
        }
    }

    static Result inspectResponse(int status, String body, String topic, Set<String> consumerAddresses) {
        if (status != 200) {
            return new Result(false, "topic=" + topic + " HTTP " + status + " " + abbreviate(body));
        }
        if (topic == null || topic.isBlank() || consumerAddresses == null || consumerAddresses.isEmpty()
                || consumerAddresses.stream().anyMatch(address -> address == null || address.isBlank())) {
            return new Result(false, "Missing expected topic or consumer broker-client addresses");
        }
        try {
            Document document = ReadinessXml.parse(body);
            NodeList responses = document.getElementsByTagNameNS("*", "getSubscriptionsResponse");
            if (responses.getLength() != 1 || document.getElementsByTagNameNS("*", "Fault").getLength() != 0) {
                return new Result(false, "topic=" + topic + " unexpected getSubscriptions response: "
                        + abbreviate(body));
            }
            NodeList subscriptions = ((Element) responses.item(0)).getElementsByTagNameNS("*", "return");
            List<String> activeOrigins = new ArrayList<>();
            List<String> consumerOrigins = new ArrayList<>();
            for (int i = 0; i < subscriptions.getLength(); i++) {
                Element subscription = (Element) subscriptions.item(i);
                // The shipped MBean merges active and inactive sets despite its isActive argument.
                if (!"true".equals(ReadinessXml.childText(subscription, "active"))
                        || !topic.equalsIgnoreCase(ReadinessXml.childText(subscription, "subscribedQueueOrTopicName"))
                        || !"AMQP".equals(ReadinessXml.childText(subscription, "protocolType"))
                        || !"TOPIC".equals(ReadinessXml.childText(subscription, "destinationType"))) {
                    continue;
                }
                String origin = ReadinessXml.childText(subscription, "originHostAddress");
                activeOrigins.add(origin);
                if (consumerAddresses.stream().anyMatch(address -> origin.startsWith("/" + address + ":")
                        || origin.startsWith("/[" + address + "]:"))) {
                    consumerOrigins.add(origin);
                }
            }
            return new Result(!consumerOrigins.isEmpty(), "topic=" + topic + " active origins=" + activeOrigins
                    + " expected consumer=" + consumerAddresses + " matched consumer=" + consumerOrigins);
        } catch (IOException e) {
            return new Result(false, "topic=" + topic + " invalid broker response: " + e.getMessage());
        }
    }

    private static String abbreviate(String value) {
        return value == null ? "no body" : value.substring(0, Math.min(value.length(), 1000));
    }

    record Result(boolean ready, String diagnostics) {
    }
}
