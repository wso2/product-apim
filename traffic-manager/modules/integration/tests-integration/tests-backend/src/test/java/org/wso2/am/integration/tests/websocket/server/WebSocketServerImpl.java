package org.wso2.am.integration.tests.websocket.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;

@WebSocket
public class WebSocketServerImpl {
    private final Log log = LogFactory.getLog(WebSocketServerImpl.class);

    @OnWebSocketMessage
    public void onText(Session session, String message) throws IOException {
        log.info("Server received message:" + message);
        if (session.isOpen()) {
            String response = message.toUpperCase();
            session.getRemote().sendString(response);
        }
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        log.info(session.getRemoteAddress().getHostName() + " connected!");
    }

    @OnWebSocketClose
    public void onClose(Session session, int status, String reason) {
        log.info(session.getRemoteAddress().getHostName() + " closed!");
    }

}
