/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.admin.clients.logging;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.wso2.carbon.logging.remote.config.stub.RemoteLoggingConfigStub;
import org.wso2.carbon.logging.remote.config.stub.types.carbon.RemoteServerLoggerData;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * Client for interacting with the RemoteLoggingConfig Axis2 admin service.
 * Used in integration tests to configure remote server logging (HTTP appenders).
 */
public class RemoteLoggingConfigClient {

    private static final String SERVICE_NAME = "RemoteLoggingConfig";

    /*
     * Axis2 namespaces used by the RemoteLoggingConfig service.
     * The operation wrapper uses the standard Axis2 RPC namespace; the data type fields
     * use the logging-service data namespace from the service WSDL.
     */
    private static final String AXIS2_NS = "http://org.apache.axis2/xsd";
    private static final String DATA_NS  = "http://data.service.logging.carbon.wso2.org/xsd";

    private final RemoteLoggingConfigStub stub;

    public RemoteLoggingConfigClient(String backendURL, String userName, String password) throws Exception {
        stub = new RemoteLoggingConfigStub(backendURL + SERVICE_NAME);
        CarbonUtils.setBasicAccessSecurityHeaders(userName, password, stub._getServiceClient());
    }

    /**
     * Adds a remote server logging configuration.
     *
     * <p>Uses raw Axis2 OM rather than the generated stub to work around a JVM-dependent
     * method-overloading issue: the service exposes two public overloads of this operation
     * and Axis2's schema generator picks whichever {@code Class.getMethods()} returns first.
     * The raw-OM call always includes both {@code data} and {@code args1} so the request is
     * accepted regardless of which overload was registered.</p>
     */
    public void addRemoteServerConfig(RemoteServerLoggerData data) throws Exception {
        sendConfigOp("addRemoteServerConfig", data, false);
    }

    /**
     * Resets a remote server logging configuration to its default local-file state.
     *
     * <p>See {@link #addRemoteServerConfig} for the reason this uses raw Axis2 OM.</p>
     */
    public void resetRemoteServerConfig(RemoteServerLoggerData data) throws Exception {
        sendConfigOp("resetRemoteServerConfig", data, false);
    }

    /** Returns all currently configured remote server logging entries. */
    public RemoteServerLoggerData[] getRemoteServerConfigs() throws Exception {
        return stub.getRemoteServerConfigs();
    }

    /** Returns the remote server configuration for the given log type. */
    public RemoteServerLoggerData getRemoteServerConfig(String logType) throws Exception {
        return stub.getRemoteServerConfig(logType);
    }

    /** Synchronises all persisted remote server configs with log4j2.properties. */
    public void syncRemoteServerConfigs() throws Exception {
        stub.syncRemoteServerConfigs();
    }

    /**
     * Sends an {@code addRemoteServerConfig} or {@code resetRemoteServerConfig} SOAP operation
     * using raw Axis2 OM and the Robust In-Only MEP ({@code sendRobust}). Always includes both
     * the {@code data} element and an {@code args1} boolean so the request is accepted whether
     * the server registered the one- or two-parameter overload. Using {@code sendRobust} instead
     * of {@code fireAndForget} means the call blocks until the server finishes processing and
     * any server-side fault is propagated to the caller as an {@link org.apache.axis2.AxisFault}.
     */
    private void sendConfigOp(String opName, RemoteServerLoggerData data, boolean isPeriodicalSyncRequest)
            throws Exception {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace opNs   = fac.createOMNamespace(AXIS2_NS, "ns");
        OMNamespace dataNs = fac.createOMNamespace(DATA_NS,  "ax2");

        OMElement op = fac.createOMElement(opName, opNs);
        op.addChild(buildDataElement(fac, opNs, dataNs, data));

        OMElement args1 = fac.createOMElement("args1", opNs);
        args1.setText(Boolean.toString(isPeriodicalSyncRequest));
        op.addChild(args1);

        stub._getServiceClient().sendRobust(op);
    }

    private static OMElement buildDataElement(OMFactory fac, OMNamespace opNs, OMNamespace dataNs,
                                              RemoteServerLoggerData data) {
        OMElement dataEl = fac.createOMElement("data", opNs);
        appendTextChild(fac, dataNs, dataEl, "connectTimeoutMillis", data.getConnectTimeoutMillis());
        appendTextChild(fac, dataNs, dataEl, "keystoreLocation",     data.getKeystoreLocation());
        appendTextChild(fac, dataNs, dataEl, "keystorePassword",     data.getKeystorePassword());
        appendTextChild(fac, dataNs, dataEl, "logType",              data.getLogType());
        appendTextChild(fac, dataNs, dataEl, "password",             data.getPassword());
        appendTextChild(fac, dataNs, dataEl, "truststoreLocation",   data.getTruststoreLocation());
        appendTextChild(fac, dataNs, dataEl, "truststorePassword",   data.getTruststorePassword());
        appendTextChild(fac, dataNs, dataEl, "url",                  data.getUrl());
        appendTextChild(fac, dataNs, dataEl, "username",             data.getUsername());
        appendTextChild(fac, dataNs, dataEl, "verifyHostname",       data.getVerifyHostname());
        return dataEl;
    }

    private static void appendTextChild(OMFactory f, OMNamespace ns, OMElement parent,
                                        String localName, String value) {
        if (value == null) {
            return;
        }
        OMElement child = f.createOMElement(localName, ns);
        child.setText(value);
        parent.addChild(child);
    }

    private static void appendTextChild(OMFactory f, OMNamespace ns, OMElement parent,
                                        String localName, boolean value) {
        OMElement child = f.createOMElement(localName, ns);
        child.setText(Boolean.toString(value));
        parent.addChild(child);
    }
}
