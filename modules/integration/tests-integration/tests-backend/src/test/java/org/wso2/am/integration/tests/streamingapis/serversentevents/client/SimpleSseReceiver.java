/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.streamingapis.serversentevents.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Represents the test SSE Receiver (client).
 */
public class SimpleSseReceiver implements SseEventSource {

    private static final String DATA = "data:";
    private static final String REQUEST_IS_THROTTLED_SEGMENT = "request is throttled";
    private static final String MEDIA_TYPE_SSE = "text/event-stream";

    private final Log log = LogFactory.getLog(SimpleSseReceiver.class);
    private final WebTarget target;
    private String bearerToken;
    private AtomicInteger receivedDataEventsCount;
    private Consumer<Boolean> throttledResponseProcessor;

    public SimpleSseReceiver(WebTarget target, String bearerToken) {
        this.target = target;
        this.bearerToken = bearerToken;
        this.receivedDataEventsCount = new AtomicInteger(0);
    }

    public void registerThrottledResponseConsumer(Consumer<Boolean> consumer) {
        this.throttledResponseProcessor = consumer;
    }

    @Override
    public void register(Consumer<InboundSseEvent> consumer) {
        // Ignore
    }

    @Override
    public void register(Consumer<InboundSseEvent> consumer, Consumer<Throwable> consumer1) {
        // Ignore
    }

    @Override
    public void register(Consumer<InboundSseEvent> consumer, Consumer<Throwable> consumer1, Runnable runnable) {
        // Ignore
    }

    @Override
    public void open() {
        Invocation.Builder builder = target.request(MEDIA_TYPE_SSE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        Response response = builder.get();
        // A client can be told to stop reconnecting using the HTTP 204 No Content response code.
        if (response.getStatus() == 204) {
            return;
        }

        process(response);
    }

    private void process(Response response) {
        InputStream inputStream = (InputStream) response.getEntity();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while (line != null) {
                if (line.startsWith(DATA)) {
                    log.info("Received data - " + line);
                    receivedDataEventsCount.incrementAndGet();
                } else if (line.contains(REQUEST_IS_THROTTLED_SEGMENT)) {
                    processThrottledResponse();
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            log.error("Failed to read the response.", e);
        }
    }

    private void processThrottledResponse() {
        if (this.throttledResponseProcessor != null) {
            throttledResponseProcessor.accept(true);
        }
        log.info("Throttled out");
    }

    @Override
    public boolean isOpen() {
        return false; // Ignore
    }

    @Override
    public boolean close(long l, TimeUnit timeUnit) {
        return true;
    }

    public int getReceivedDataEventsCount() {
        return receivedDataEventsCount.get();
    }

    public void setReceivedDataEventsCount(int receivedDataEventsCount) {
        this.receivedDataEventsCount.set(receivedDataEventsCount);
    }
}
