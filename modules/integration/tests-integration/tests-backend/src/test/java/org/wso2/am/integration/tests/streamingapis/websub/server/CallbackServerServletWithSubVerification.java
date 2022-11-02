/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.streamingapis.websub.server;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@WebServlet(urlPatterns = "/receiver")
public class CallbackServerServletWithSubVerification extends HttpServlet {

    private final Log log = LogFactory.getLog(CallbackServerServletWithSubVerification.class);

    private AtomicInteger callbacksReceived = new AtomicInteger(0);

    private String message;

    private String signature;
    private String linkHeader;
    private String hubMode;
    private String hubTopic;
    private String hubChallenge;

    public String getHubMode() {
        return hubMode;
    }

    public void setHubMode(String hubMode) {
        this.hubMode = hubMode;
    }

    public String getHubTopic() {
        return hubTopic;
    }

    public void setHubTopic(String hubTopic) {
        this.hubTopic = hubTopic;
    }

    public String getHubChallenge() {
        return hubChallenge;
    }

    public void setHubChallenge(String hubChallenge) {
        this.hubChallenge = hubChallenge;
    }

    public String getLinkHeader() {
        return linkHeader;
    }

    public void setLinkHeader(String linkHeader) {
        this.linkHeader = linkHeader;
    }

    public int getCallbacksReceived() {
        return callbacksReceived.get();
    }

    public void setCallbacksReceived(int callbacksReceived) {
        this.callbacksReceived.set(callbacksReceived);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("Callback Received");
        message = IOUtils.toString(req.getReader());
        signature = req.getHeader("x-hub-signature");
        setLinkHeader(req.getHeader("link"));
        callbacksReceived.incrementAndGet();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        log.info("Subscription callback Received");
        setHubMode(req.getParameter("hub.mode"));
        setHubTopic(req.getParameter("hub.topic"));
        setHubChallenge(req.getParameter("hub.challenge"));
        if (getHubMode().equals("subscribe")) {
            resp.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(getHubChallenge());
            resp.getWriter().flush();
        }
    }

    public String getLastReceivedMessage() {
        return message;
    }

    public String getLastReceivedSignature() {
        return signature;
    }

}
