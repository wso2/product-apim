/*
 *   Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.am.integration.tests.graphql.websocket.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@WebSocket
public class SubscriptionWSClientImpl {

    private Session session;
    private final Log log = LogFactory.getLog(SubscriptionWSClientImpl.class);
    private String responseMessage;

    private final CountDownLatch latch = new CountDownLatch(1);

    @OnWebSocketMessage
    public void onText(Session session, String message) {
        this.setResponseMessage(message);
        log.info("Client received message:" + message);
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        log.info("Connected to server");
        this.session = session;
        latch.countDown();
    }

    public void sendMessage(String str) throws APIManagerIntegrationTestException {
        try {
            if (session != null) {
                session.getRemote().sendString(str);
            } else {
                throw new APIManagerIntegrationTestException("Client session is null");
            }
        } catch (IOException e) {
            log.error("Error while sending message from client: ", e);
        }
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }
}
