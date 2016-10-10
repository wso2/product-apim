package org.wso2.am.integration.tests.websocket.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.wso2.am.integration.tests.websocket.WebSocketAPIPublishTestCase;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@WebSocket
public class ToUpperClientSocket {

    private Session session;
    private final Log log = LogFactory.getLog(WebSocketAPIPublishTestCase.class);
    private String responseMessage;

    CountDownLatch latch = new CountDownLatch(1);

    @OnWebSocketMessage
    public void onText(Session session, String message) throws IOException {
        this.setResponseMessage(message);
        log.info("Message received from server:" + message);
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        log.info("Connected to server");
        this.session = session;
        latch.countDown();
    }

    public void sendMessage(String str) {
        try {
            session.getRemote().sendString(str);
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
