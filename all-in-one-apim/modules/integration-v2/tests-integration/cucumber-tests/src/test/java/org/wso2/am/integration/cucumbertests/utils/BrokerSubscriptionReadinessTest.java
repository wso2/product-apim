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

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Set;

/** Regression checks for the live subscription contract shared by key-manager and throttle-data readiness. */
public class BrokerSubscriptionReadinessTest {
    private static final String GATEWAY = "192.168.42.6";

    @Test
    public void acceptsOnlyAnActiveGatewaySubscriptionToTheRequestedTopic() {
        var result = check(200, envelope(subscription("true", "keymanager", "AMQP", "TOPIC", "/" + GATEWAY + ":51590/1")));
        Assert.assertTrue(result.ready(), result.diagnostics());
        Assert.assertTrue(result.diagnostics().contains("matched consumer=[/" + GATEWAY));
    }

    @DataProvider
    public Object[][] nonMatchingSubscriptions() {
        return new Object[][] {
                {"false", "keymanager", "AMQP", "TOPIC", "/" + GATEWAY + ":51590/1"},
                {"", "keymanager", "AMQP", "TOPIC", "/" + GATEWAY + ":51590/1"},
                {"true", "throttledata", "AMQP", "TOPIC", "/" + GATEWAY + ":51590/1"},
                {"true", "keymanager", "MQTT", "TOPIC", "/" + GATEWAY + ":51590/1"},
                {"true", "keymanager", "AMQP", "QUEUE", "/" + GATEWAY + ":51590/1"},
                {"true", "keymanager", "AMQP", "TOPIC", "/192.168.42.4:51590/1"},
                {"true", "keymanager", "AMQP", "TOPIC", "/192.168.42.60:51590/1"},
                {"true", "keymanager", "AMQP", "TOPIC", ""}
        };
    }

    @Test(dataProvider = "nonMatchingSubscriptions")
    public void rejectsInactiveWrongTopicProtocolDestinationAndOrigin(String active, String topic, String protocol,
                                                                    String destination, String origin) {
        var result = check(200, envelope(subscription(active, topic, protocol, destination, origin)));
        Assert.assertFalse(result.ready(), result.diagnostics());
    }

    @Test
    public void controlPlaneSubscriptionCannotHideAnInactiveGateway() {
        String cp = subscription("true", "keymanager", "AMQP", "TOPIC", "/192.168.42.4:40000/1");
        String gateway = subscription("false", "keymanager", "AMQP", "TOPIC", "/" + GATEWAY + ":51590/1");
        Assert.assertFalse(check(200, envelope(cp + gateway)).ready());
    }

    @Test
    public void gatewayNotificationsCannotHideADisconnectedTrafficManagerPolicyConsumer() {
        String tmAddress = "192.168.42.5";
        String gateway = subscription("true", "notification", "AMQP", "TOPIC", "/" + GATEWAY + ":51590/1");
        String inactiveTm = subscription("false", "notification", "AMQP", "TOPIC", "/" + tmAddress + ":40000/1");
        var disconnected = BrokerSubscriptionReadiness.inspectResponse(200, envelope(gateway + inactiveTm),
                "notification", Set.of(tmAddress));
        Assert.assertFalse(disconnected.ready(), disconnected.diagnostics());

        String activeTm = subscription("true", "notification", "AMQP", "TOPIC", "/" + tmAddress + ":40001/1");
        var connected = BrokerSubscriptionReadiness.inspectResponse(200, envelope(gateway + inactiveTm + activeTm),
                "notification", Set.of(tmAddress));
        Assert.assertTrue(connected.ready(), connected.diagnostics());
    }

    @Test
    public void laterDisconnectionCannotReuseAnEarlierHealthySample() {
        Assert.assertTrue(check(200, envelope(subscription("true", "keymanager", "AMQP", "TOPIC",
                "/" + GATEWAY + ":51590/1"))).ready());
        Assert.assertFalse(check(200, envelope(subscription("false", "keymanager", "AMQP", "TOPIC",
                "/" + GATEWAY + ":51590/1"))).ready());
    }

    @DataProvider
    public Object[][] failedStatuses() {
        return new Object[][] {{0}, {401}, {403}, {500}};
    }

    @Test(dataProvider = "failedStatuses")
    public void failedHttpCannotBeReadyEvenWithAHealthyLookingBody(int status) {
        Assert.assertFalse(check(status, envelope(subscription("true", "keymanager", "AMQP", "TOPIC",
                "/" + GATEWAY + ":51590/1"))).ready());
    }

    @DataProvider
    public Object[][] invalidBodies() {
        return new Object[][] {
                {null}, {""}, {"not XML"}, {"<unexpected/>"},
                {"<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><s:Fault/>"
                        + "</s:Body></s:Envelope>"},
                {"<!DOCTYPE root [<!ENTITY value 'unexpected'>]><root>&value;</root>"},
                {envelope("").replace("</a:getSubscriptionsResponse>",
                        "</a:getSubscriptionsResponse><a:getSubscriptionsResponse xmlns:a=\"http://wso2.org/carbon/andes/admin/xsd\"/>")}
        };
    }

    @Test(dataProvider = "invalidBodies")
    public void malformedFaultAndUnexpectedSoapResponsesFailClosed(String body) {
        Assert.assertFalse(check(200, body).ready());
    }

    @Test
    public void noSubscriptionsOrNoExpectedGatewayCannotBeReady() {
        Assert.assertFalse(check(200, envelope("")).ready());
        Assert.assertFalse(BrokerSubscriptionReadiness.inspectResponse(200, envelope(""), "keyManager", Set.of())
                .ready());
    }

    @Test
    public void acceptsTheAllInOneLoopbackAndBracketedIpv6Addresses() {
        for (String address : Set.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1")) {
            String origin = address.contains(":") ? "/[" + address + "]:51590/1" : "/" + address + ":51590/1";
            var result = BrokerSubscriptionReadiness.inspectResponse(200,
                    envelope(subscription("true", "keymanager", "AMQP", "TOPIC", origin)),
                    "keyManager", Set.of(address));
            Assert.assertTrue(result.ready(), result.diagnostics());
        }
    }

    private static BrokerSubscriptionReadiness.Result check(int status, String body) {
        return BrokerSubscriptionReadiness.inspectResponse(status, body, "keyManager", Set.of(GATEWAY));
    }

    static String envelope(String subscriptions) {
        return "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body>"
                + "<a:getSubscriptionsResponse xmlns:a=\"http://wso2.org/carbon/andes/admin/xsd\">"
                + subscriptions + "</a:getSubscriptionsResponse></s:Body></s:Envelope>";
    }

    private static String subscription(String active, String topic, String protocol, String destination,
                                       String origin) {
        return "<a:return><a:active>" + active + "</a:active><a:subscribedQueueOrTopicName>" + topic
                + "</a:subscribedQueueOrTopicName><a:protocolType>" + protocol + "</a:protocolType>"
                + "<a:destinationType>" + destination + "</a:destinationType><a:originHostAddress>" + origin
                + "</a:originHostAddress></a:return>";
    }
}
