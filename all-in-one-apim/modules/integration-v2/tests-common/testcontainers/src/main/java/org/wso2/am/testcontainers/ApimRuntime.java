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
 */

package org.wso2.am.testcontainers;

import org.testcontainers.containers.Container;

import java.io.IOException;

/**
 * Block-facing APIM runtime contract.
 *
 * <p>The all-in-one runtime is one Testcontainers container today. The distributed runtime will be a
 * composition of CP, TM, Gateway, and database resources. Code outside the concrete runtime implementations
 * must depend on this contract so changing topology does not change the v2 block API.</p>
 */
public interface ApimRuntime {

    void start();

    void stop();

    String getServletHttpsUrl();

    String getServletHttpUrl();

    /**
     * URL that a Gateway-hosted backend endpoint-security flow can use to obtain an OAuth token.
     *
     * <p>This is intentionally different from the block-facing servlet URL in a distributed topology:
     * the URL is consumed from inside the Gateway container, so it must resolve from the Gateway's network
     * namespace to the component serving the token endpoint.</p>
     */
    String getBackendOAuthTokenUrl();

    String getGatewayHttpsUrl();

    /** Gateway management HTTPS base URL (the endpoint serving gateway artifact APIs). */
    String getGatewayManagementHttpsUrl();

    String getGatewayHttpUrl();

    String getGatewayWsUrl();

    String getGatewayWssUrl();

    String getWebSubEventReceiverUrl();

    String getGatewayClientIp();

    String getContainerTomlPath();

    ApimRuntime withServerFile(String hostPath, String serverRelativePath);

    ApimRuntime withCoverage();

    ApimRuntime withExternalKmTrust();

    ApimRuntime withExternalIsNotificationAlias();

    ApimRuntime withSolaceJwksAlias();

    String getCoverageDumpHost();

    int getCoverageDumpPort();

    void createSecondaryUserStoreH2Schema(String dbRelativePath) throws IOException, InterruptedException;

    String getContainerLog4j2Path();

    String getContainerLogFilePath(String fileName);

    /**
     * Read a log file from the component that serves Gateway traffic.
     *
     * <p>For an all-in-one runtime this is the same container. A distributed runtime must resolve this
     * against the Gateway component rather than the control plane, because deployment/Synapse messages are
     * emitted by the Gateway after it receives the deployment event.</p>
     */
    String readGatewayLogFile(String fileName);

    String readContainerFile(String containerPath);

    void writeContainerFile(String containerPath, String content);

    Container.ExecResult execInContainer(String... command) throws IOException, InterruptedException;

    String getContainerId();
}
