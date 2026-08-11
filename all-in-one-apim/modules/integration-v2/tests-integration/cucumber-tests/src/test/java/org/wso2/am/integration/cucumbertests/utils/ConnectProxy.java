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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A tiny in-process HTTP {@code CONNECT} proxy used to let a real browser (Playwright) reach the mapped
 * container ports WITHOUT rewriting any URL. A browser configured with this proxy delegates name resolution to
 * it: for {@code https://<host>:<port>/...} it sends {@code CONNECT <host>:<port>}, and this proxy looks the
 * {@code host:port} up in a fixed table (the servers' INTERNAL addresses → their host-mapped addresses) and
 * tunnels raw TCP to the mapped port. TLS is end-to-end through the tunnel (the proxy never sees plaintext), so
 * the servers' committed certs still validate and — crucially — the servers' own {@code redirect_uri}
 * (built from {@code localhost:9443}) stays consistent between the authorize leg and the callback exchange.
 *
 * <p>This is what makes the console SSO cookie obtainable that a headless HTTP walk could not get: the real
 * browser scopes JSESSIONID per path and runs the SPA's JS, so the PKCE {@code code_verifier} survives the
 * federated round-trip and {@code login_callback.jsp}'s server-side token exchange succeeds.
 *
 * <p>Only mapped hosts are tunnelled; an unmapped {@code CONNECT} target gets a {@code 502} so a stray request
 * (e.g. telemetry) cannot escape to the real internet. Bound to loopback only.
 */
public final class ConnectProxy implements AutoCloseable {

    private static final Log logger = LogFactory.getLog(ConnectProxy.class);

    /** internal "host:port" (as the browser will CONNECT to) → the host-mapped address to tunnel to. */
    private final Map<String, InetSocketAddress> routes;
    private final ServerSocket serverSocket;
    private final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "connect-proxy");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean running = true;

    /**
     * @param routes internal {@code host:port} → mapped {@code InetSocketAddress}. Keys are exactly what the
     *               browser will present in the {@code CONNECT} line, e.g. {@code localhost:9443},
     *               {@code wso2am:9443}, {@code wso2is:9443}.
     */
    public ConnectProxy(Map<String, InetSocketAddress> routes) {
        this.routes = Map.copyOf(routes);
        try {
            this.serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        } catch (IOException e) {
            throw new IllegalStateException("Could not start CONNECT proxy", e);
        }
        pool.submit(this::acceptLoop);
        logger.info("CONNECT proxy listening on 127.0.0.1:" + getPort() + " routes=" + routes);
    }

    /** The loopback port the browser's {@code --proxy-server} must point at. */
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                pool.submit(() -> handle(client));
            } catch (IOException e) {
                if (running) {
                    logger.warn("CONNECT proxy accept failed: " + e.getMessage());
                }
            }
        }
    }

    private void handle(Socket client) {
        try {
            client.setTcpNoDelay(true);
            InputStream in = client.getInputStream();
            String requestLine = readRequestLineAndDrainHeaders(in);
            if (requestLine == null || !requestLine.toUpperCase().startsWith("CONNECT ")) {
                writeStatus(client, "400 Bad Request");
                client.close();
                return;
            }
            // "CONNECT host:port HTTP/1.1"
            String target = requestLine.split("\\s+")[1];
            InetSocketAddress mapped = routes.get(target);
            if (mapped == null) {
                logger.warn("CONNECT proxy: no route for '" + target + "' — refusing (502)");
                writeStatus(client, "502 Bad Gateway");
                client.close();
                return;
            }
            Socket upstream = new Socket();
            upstream.setTcpNoDelay(true);
            upstream.connect(mapped, 15000);
            writeStatus(client, "200 Connection Established");
            // Raw bidirectional tunnel: TLS flows end-to-end, the proxy only shuffles bytes.
            pool.submit(() -> pipe(client, upstream));
            pipe(upstream, client);
        } catch (IOException e) {
            logger.warn("CONNECT proxy tunnel failed: " + e.getMessage());
            closeQuietly(client);
        }
    }

    /**
     * Reads the CONNECT request line and consumes the request headers up to (and including) the terminating
     * blank line, byte-by-byte on the RAW stream so the bytes that follow (the TLS ClientHello) are left
     * untouched in the socket for the tunnel. Returns the request line, or {@code null} on EOF.
     */
    private static String readRequestLineAndDrainHeaders(InputStream in) throws IOException {
        StringBuilder all = new StringBuilder();
        int c;
        int consecutiveNl = 0;
        while ((c = in.read()) != -1) {
            all.append((char) c);
            if (c == '\n') {
                consecutiveNl++;
                if (consecutiveNl == 2) {
                    break; // end of headers (\r\n\r\n → two '\n' with only '\r' between)
                }
            } else if (c != '\r') {
                consecutiveNl = 0;
            }
        }
        if (all.length() == 0) {
            return null;
        }
        int firstNl = all.indexOf("\n");
        return (firstNl < 0 ? all.toString() : all.substring(0, firstNl)).trim();
    }

    private static void writeStatus(Socket client, String status) throws IOException {
        OutputStream out = client.getOutputStream();
        out.write(("HTTP/1.1 " + status + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    private static void pipe(Socket from, Socket to) {
        byte[] buf = new byte[16384];
        try {
            InputStream in = from.getInputStream();
            OutputStream out = to.getOutputStream();
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                out.flush();
            }
        } catch (IOException ignored) {
            // peer closed / reset — normal at connection end
        } finally {
            closeQuietly(from);
            closeQuietly(to);
        }
    }

    private static void closeQuietly(Socket s) {
        try {
            s.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void close() {
        running = false;
        closeQuietly0(serverSocket);
        pool.shutdownNow();
        logger.info("CONNECT proxy stopped");
    }

    private static void closeQuietly0(ServerSocket s) {
        try {
            s.close();
        } catch (IOException ignored) {
        }
    }
}
