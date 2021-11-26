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

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

public class SubscriptionServerCreator implements WebSocketCreator {

    private SubscriptionWSServerImpl subscriptionWSServer;

    public SubscriptionServerCreator() {
        this.subscriptionWSServer = new SubscriptionWSServerImpl();
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest servletUpgradeRequest,
                                  ServletUpgradeResponse servletUpgradeResponse) {

        for (String subProtocol : servletUpgradeRequest.getSubProtocols()) {
            if ("graphql-ws".equals(subProtocol)) {
                servletUpgradeResponse.setAcceptedSubProtocol(subProtocol);
                return subscriptionWSServer;
            }
        }
        return null;
    }
}
