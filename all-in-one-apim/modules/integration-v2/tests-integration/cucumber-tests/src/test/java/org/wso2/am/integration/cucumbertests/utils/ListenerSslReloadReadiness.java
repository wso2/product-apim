/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.integration.cucumbertests.utils;

import org.wso2.am.testcontainers.ApimRuntime;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Readiness contract for the Gateway's dynamic SSL-profile reload on the passthrough HTTPS listener.
 *
 * <p>An uploaded mutual-SSL client certificate reaches the TLS handshake only when the listener reloads its SSL
 * profile. {@code PassThroughHttpListener.reloadDynamicSSLConfig} removes the {@code SSLProfiles} parameter before
 * {@code reloadSpecificEndPoints} checks that the listener is started, and nothing restores it on that path. A reload
 * that runs before the HTTPS listener has started therefore disables every later reload for the life of the server:
 * each one logs {@code reloading SSL Config..} and does nothing, so uploaded client certificates are never trusted
 * and mutual-SSL invocations fail with 401.</p>
 *
 * <p>A healthy reload is always followed by the listener restarting its endpoints ({@code Pass-through HTTPS Listener
 * started on}). The Gateway log is read from its most recent {@code Starting WSO2 Carbon}, because the file survives a
 * graceful restart. The listener is broken when a reload precedes the first HTTPS listener start, or when a reload
 * older than {@link #RELOAD_COMPLETION_MILLIS} has no listener start after it.</p>
 */
public final class ListenerSslReloadReadiness {

    private static final String SERVER_LOG = "wso2carbon.log";
    private static final Pattern LISTENER_RELOAD = Pattern.compile("PassThroughHttpListener reloading SSL Config");
    private static final Pattern HTTPS_LISTENER_STARTED = Pattern.compile("Pass-through HTTPS Listener started on");
    private static final Pattern LOG_TIMESTAMP = Pattern.compile(
            "\\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3})\\]");

    private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");

    /** A reload completes within milliseconds; one still unanswered after this long did nothing. */
    private static final long RELOAD_COMPLETION_MILLIS = 10000L;

    private ListenerSslReloadReadiness() {
    }

    /** The listener's state on the server's current start. */
    public enum State {
        /** The HTTPS listener has not started yet. */
        PENDING,
        /** Every reload so far was applied to a started listener. */
        READY,
        /** A reload ran before the listener started, or did not restart it; later reloads are no-ops. */
        BROKEN
    }

    public record Result(State state, String diagnostics) {
    }

    /** Polls the Gateway log until the listener has started, returning as soon as its state is decided. */
    public static Result await(ApimRuntime runtime, int timeoutSeconds) throws InterruptedException {
        return Utils.retryUntil(timeoutSeconds * 1000L, () -> inspect(runtime),
                result -> result.state() != State.PENDING);
    }

    /** Takes one snapshot of the Gateway log for the server's current start. */
    public static Result inspect(ApimRuntime runtime) {
        String log = runtime.readGatewayLogHistory(SERVER_LOG).text();
        List<String> events = new ArrayList<>();
        boolean started = false;
        String pendingReload = null;
        State state = State.PENDING;
        for (String line : log.split("\\R")) {
            if (HTTPS_LISTENER_STARTED.matcher(line).find()) {
                started = true;
                pendingReload = null;
                events.add(line.trim());
            } else if (LISTENER_RELOAD.matcher(line).find()) {
                events.add(line.trim());
                if (!started || pendingReload != null) {
                    state = State.BROKEN;
                }
                pendingReload = line;
            }
        }
        if (state != State.BROKEN && pendingReload != null && isOlderThan(pendingReload, log)) {
            state = State.BROKEN;
        }
        if (state != State.BROKEN && started) {
            state = State.READY;
        }
        return new Result(state, "httpsListenerStarted=" + started + " state=" + state + " events="
                + String.join(" | ", events.subList(Math.max(0, events.size() - 8), events.size())));
    }

    /** Whether the line's timestamp is more than the completion window older than the last timestamp in the log. */
    private static boolean isOlderThan(String line, String log) {
        String lineTime = lastTimestamp(line);
        String latest = lastTimestamp(log);
        if (lineTime == null || latest == null) {
            return false;
        }
        return epochMillis(latest) - epochMillis(lineTime) > RELOAD_COMPLETION_MILLIS;
    }

    private static long epochMillis(String timestamp) {
        return LocalDateTime.parse(timestamp, LOG_TIME_FORMAT).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private static String lastTimestamp(String value) {
        Matcher matcher = LOG_TIMESTAMP.matcher(value);
        String last = null;
        while (matcher.find()) {
            last = matcher.group(1);
        }
        return last;
    }

}
