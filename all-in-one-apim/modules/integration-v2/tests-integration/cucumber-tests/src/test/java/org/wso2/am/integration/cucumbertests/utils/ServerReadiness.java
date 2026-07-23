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

package org.wso2.am.integration.cucumbertests.utils;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

/**
 * Shared APIM readiness gate: polls the gateway server-startup health-check endpoint until it returns
 * 200 or the startup window elapses. Extracted so both the Cucumber {@code "I wait for the APIM server
 * to be ready"} step and the parallel-lane {@code BlockLifecycleListener} drive the exact same poll.
 */
public final class ServerReadiness {

    private static final Log logger = LogFactory.getLog(ServerReadiness.class);

    private ServerReadiness() {
    }

    /**
     * Polls {@code baseUrl}'s gateway health-check endpoint until it returns 200 or
     * {@link Constants#SERVER_STARTUP_WAIT_TIME} elapses.
     *
     * @param baseUrl the servlet base URL of the APIM node (e.g. the container's servlet-https URL)
     * @return {@code true} if the server became ready within the startup window, {@code false} otherwise
     */
    public static boolean awaitReady(String baseUrl) {
        return awaitReady(baseUrl, Constants.SERVER_STARTUP_WAIT_TIME);
    }

    /**
     * Polls {@code baseUrl}'s gateway health-check endpoint until it returns 200 or {@code timeoutMillis}
     * elapses. The gate keys on an HTTP 200 from the health-check, not merely on the port being open, so a
     * container that listens but never serves 200 (a partial boot) correctly times out to {@code false}.
     *
     * @param baseUrl       the servlet base URL of the APIM node (e.g. the container's servlet-https URL)
     * @param timeoutMillis how long to keep polling before giving up
     * @return {@code true} if the health-check returned 200 within the window, {@code false} otherwise
     */
    public static boolean awaitReady(String baseUrl, long timeoutMillis) {

        String url = Utils.getGatewayHealthCheckURL(baseUrl);
        long deadline = System.currentTimeMillis() + timeoutMillis;

        while (System.currentTimeMillis() < deadline) {
            HttpResponse response = null;
            try {
                response = SimpleHTTPClient.getInstance().doGet(url, null);
            } catch (IOException ignored) {
                // server not accepting connections yet
            }
            if (response != null && response.getResponseCode() == 200) {
                return true;
            }
            try {
                logger.info("Waiting for APIM server to be ready...");
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * Polls an external WSO2 Identity Server's OIDC discovery document until it returns 200 or
     * {@link Constants#SERVER_STARTUP_WAIT_TIME} elapses. Used by the external-KM block after starting the
     * {@code IdentityServerContainer} to gate KM registration on IS actually serving OAuth endpoints — the same
     * 200-gated poll shape as {@link #awaitReady} but against IS's {@code .well-known/openid-configuration}
     * rather than the APIM gateway health-check.
     *
     * @param isBaseUrl the host-mapped IS management HTTPS base URL (e.g. {@code https://localhost:32771/})
     * @return {@code true} if the discovery document returned 200 within the window, {@code false} otherwise
     */
    public static boolean awaitIdentityServerReady(String isBaseUrl) {

        String url = isBaseUrl + "oauth2/token/.well-known/openid-configuration";
        long deadline = System.currentTimeMillis() + Constants.SERVER_STARTUP_WAIT_TIME;

        while (System.currentTimeMillis() < deadline) {
            HttpResponse response = null;
            try {
                response = SimpleHTTPClient.getInstance().doGet(url, null);
            } catch (IOException ignored) {
                // IS not accepting connections / serving OAuth endpoints yet
            }
            if (response != null && response.getResponseCode() == 200) {
                return true;
            }
            try {
                logger.info("Waiting for the external Identity Server to be ready...");
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * Awaits a server restart by tracking the health-check through a DOWN then UP transition. A graceful
     * restart ({@code restartGracefully}) returns BEFORE the JVM halts (it drains in-flight requests first),
     * so the server is briefly still serving 200 after the call — a naive {@link #awaitReady} would return
     * immediately on the not-yet-restarted server. This first waits until the health-check stops returning 200
     * (the server going down), then waits until it returns 200 again (back up). Each phase is bounded by
     * {@code SERVER_STARTUP_WAIT_TIME}.
     *
     * @param baseUrl the servlet base URL of the APIM node
     * @return {@code true} if the server was observed going down and then becoming ready again
     */
    public static boolean awaitRestart(String baseUrl) {
        if (!awaitUnready(baseUrl, Constants.SERVER_STARTUP_WAIT_TIME)) {
            logger.error("APIM server did not go down after the restart request; restart may not have taken effect");
            return false;
        }
        logger.info("APIM server went down for restart; waiting for it to come back up...");
        return awaitReady(baseUrl, Constants.SERVER_STARTUP_WAIT_TIME);
    }

    /** Polls the health-check until it is NOT 200 (or the port is closed), i.e. the server has gone down. */
    private static boolean awaitUnready(String baseUrl, long timeoutMillis) {
        String url = Utils.getGatewayHealthCheckURL(baseUrl);
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            try {
                HttpResponse response = SimpleHTTPClient.getInstance().doGet(url, null);
                if (response == null || response.getResponseCode() != 200) {
                    return true;
                }
            } catch (Exception e) {
                // Connection refused / reset — the server is down.
                return true;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
