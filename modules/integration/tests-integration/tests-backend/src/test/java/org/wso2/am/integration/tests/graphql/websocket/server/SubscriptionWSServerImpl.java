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
package org.wso2.am.integration.tests.graphql.websocket.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;

@WebSocket
public class SubscriptionWSServerImpl {
    private final Log log = LogFactory.getLog(org.wso2.am.integration.tests.websocket.server.WebSocketServerImpl.class);


    @OnWebSocketMessage
    public void onText(Session session, String message) throws IOException {
        log.info("Server received message:" + message);
        String response = message;
        if (session.isOpen()) {
            JSONParser jsonParser = new JSONParser();
            try {
                JSONObject clientMessage = (JSONObject) jsonParser.parse(message);
                if (clientMessage.containsKey("type")) {
                    String messageType = (String) clientMessage.get("type");
                    if ("connection_init".equals(messageType)) {
                        response = "{\"type\":\"connection_ack\"}";
                    } else if ("start".equals(messageType)) {
                        response = "{\"type\":\"data\",\"id\":\"1\",\"payload\":{\"data\":"
                                + "{\"liftStatusChange\":{\"name\":\"Astra Express\"}}}}";
                    }
                } else {
                    response = "Invalid message type";
                }
                session.getRemote().sendString(response);
            } catch (ParseException e) {
                log.error("Invalid json message received to GraphQL Subscription backend: " + message);
            }
        }
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        session.getUpgradeResponse().setAcceptedSubProtocol("graphql-ws");
        log.info(session.getRemoteAddress().getHostName() + " connected!");
    }

    @OnWebSocketClose
    public void onClose(Session session, int status, String reason) {
        log.info(session.getRemoteAddress().getHostName() + " closed!");
    }


}

