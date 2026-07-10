/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.wso2.am.integration.tests.websocket.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal embedded HTTP CONNECT proxy for WS proxy profile integration tests.
 *
 * Always relays to 127.0.0.1 on the port extracted from the CONNECT line,
 * ignoring the hostname. This allows tests to use fake hostnames (e.g.
 * "proxied.ws.local") as target_hosts regex patterns without requiring real
 * DNS entries, while still reaching the local Jetty WS echo backend.
 *
 * Supports optional Proxy-Authorization enforcement for testing authenticated
 * proxy profiles. Records every CONNECT target for assertion in tests.
 */
public class TestHttpConnectProxy {

    private static final Log log = LogFactory.getLog(TestHttpConnectProxy.class);

    private final boolean requireAuth;
    private final String expectedUsername;
    private final String expectedPassword;

    private final AtomicInteger connectCount = new AtomicInteger(0);
    private final List<String> connectTargets = new CopyOnWriteArrayList<>();

    private ServerSocket serverSocket;
    private ExecutorService executor;
    private volatile boolean running;

    /** Creates an unauthenticated proxy. */
    public TestHttpConnectProxy() {
        this(false, null, null);
    }

    /** Creates a proxy that requires Proxy-Authorization Basic with the given credentials. */
    public TestHttpConnectProxy(String username, String password) {
        this(true, username, password);
    }

    private TestHttpConnectProxy(boolean requireAuth, String username, String password) {
        this.requireAuth = requireAuth;
        this.expectedUsername = username;
        this.expectedPassword = password;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(0);
        executor = Executors.newCachedThreadPool();
        running = true;
        executor.submit(this::acceptLoop);
        log.info("TestHttpConnectProxy started on port " + getPort()
                + (requireAuth ? " (auth required)" : " (anonymous)"));
    }

    public void stop() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }
        executor.shutdownNow();
        log.info("TestHttpConnectProxy on port " + getPort() + " stopped");
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    /** Total number of CONNECT requests received since start or last reset. */
    public int getConnectCount() {
        return connectCount.get();
    }

    /** Ordered list of "host:port" strings from every CONNECT request received. */
    public List<String> getConnectTargets() {
        return Collections.unmodifiableList(connectTargets);
    }

    public void resetCounters() {
        connectCount.set(0);
        connectTargets.clear();
    }

    // -----------------------------------------------------------------------

    private void acceptLoop() {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                executor.submit(() -> handleClient(client));
            } catch (IOException e) {
                if (running) {
                    log.warn("Proxy accept error", e);
                }
            }
        }
    }

    private void handleClient(Socket client) {
        try {
            InputStream clientIn = client.getInputStream();
            OutputStream clientOut = client.getOutputStream();

            String rawHeaders = readHeaders(clientIn);
            if (rawHeaders == null) {
                closeQuietly(client);
                return;
            }

            String[] lines = rawHeaders.split("\r\n");
            if (lines.length == 0 || !lines[0].startsWith("CONNECT ")) {
                writeResponse(clientOut, "HTTP/1.1 405 Method Not Allowed\r\n\r\n");
                closeQuietly(client);
                return;
            }

            // Request line: CONNECT host:port HTTP/1.1
            String target = lines[0].split(" ")[1];
            connectTargets.add(target);
            connectCount.incrementAndGet();
            log.info("Proxy CONNECT " + target
                    + (requireAuth ? " [auth-required proxy]" : " [anonymous proxy]"));

            if (requireAuth) {
                String authHeader = extractHeader(lines, "Proxy-Authorization");
                if (!isCredentialValid(authHeader)) {
                    writeResponse(clientOut,
                            "HTTP/1.1 407 Proxy Authentication Required\r\n"
                            + "Proxy-Authenticate: Basic realm=\"TestProxy\"\r\n\r\n");
                    closeQuietly(client);
                    return;
                }
            }

            int backendPort = parsePort(target);
            Socket backend;
            try {
                backend = new Socket("127.0.0.1", backendPort);
            } catch (IOException e) {
                writeResponse(clientOut, "HTTP/1.1 502 Bad Gateway\r\n\r\n");
                closeQuietly(client);
                return;
            }

            writeResponse(clientOut, "HTTP/1.1 200 Connection Established\r\n\r\n");

            // Relay threads own both sockets from this point — do not close in this method.
            InputStream backendIn = backend.getInputStream();
            OutputStream backendOut = backend.getOutputStream();

            Thread t1 = new Thread(() -> {
                relay(clientIn, backendOut);
                closeQuietly(client);
                closeQuietly(backend);
            });
            Thread t2 = new Thread(() -> {
                relay(backendIn, clientOut);
                closeQuietly(client);
                closeQuietly(backend);
            });
            t1.setDaemon(true);
            t2.setDaemon(true);
            t1.start();
            t2.start();
            // Intentional: return without closing. Relay threads are responsible.
        } catch (Exception e) {
            log.debug("Proxy client handler error", e);
            closeQuietly(client);
        }
    }

    /**
     * Reads HTTP headers from the stream until the terminal CRLF CRLF sequence.
     * Returns null if the stream closes before a complete header block is received.
     */
    private String readHeaders(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            sb.append((char) b);
            int len = sb.length();
            if (len >= 4
                    && sb.charAt(len - 4) == '\r' && sb.charAt(len - 3) == '\n'
                    && sb.charAt(len - 2) == '\r' && sb.charAt(len - 1) == '\n') {
                return sb.toString();
            }
        }
        return null;
    }

    private String extractHeader(String[] lines, String name) {
        String prefix = name.toLowerCase() + ":";
        for (String line : lines) {
            if (line.toLowerCase().startsWith(prefix)) {
                return line.substring(name.length() + 1).trim();
            }
        }
        return null;
    }

    private boolean isCredentialValid(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return false;
        }
        try {
            String decoded = new String(
                    Base64.getDecoder().decode(authHeader.substring(6)), StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            if (colon < 0) {
                return false;
            }
            return expectedUsername.equals(decoded.substring(0, colon))
                    && expectedPassword.equals(decoded.substring(colon + 1));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private int parsePort(String hostPort) {
        int colon = hostPort.lastIndexOf(':');
        if (colon >= 0) {
            try {
                return Integer.parseInt(hostPort.substring(colon + 1));
            } catch (NumberFormatException ignored) {
            }
        }
        return 80;
    }

    private void relay(InputStream src, OutputStream dst) {
        byte[] buf = new byte[8192];
        try {
            int n;
            while ((n = src.read(buf)) != -1) {
                dst.write(buf, 0, n);
                dst.flush();
            }
        } catch (IOException ignored) {
        }
    }

    private void writeResponse(OutputStream out, String response) throws IOException {
        out.write(response.getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    private void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
